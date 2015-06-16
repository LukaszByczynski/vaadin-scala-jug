package org.jug.vaadinscala.todo.async

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.vaadin.ui.UI
import org.jboss.netty.util.{HashedWheelTimer, TimerTask}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

object Async {

  //  private implicit val context = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

  implicit private[async] var timeout = 10.seconds

  def default[T](future: Future[T])(success: (T => Unit), failure: Throwable => Unit = (_) => {}) = {
    task(future)(success, failure)
  }

  def task[T](future: Future[T])(success: T => Unit, failure: Throwable => Unit = (_) => {}) {
    import scala.concurrent.ExecutionContext.Implicits.global

    // remember current UI
    val ui = UI.getCurrent

    val uuid = UUID.randomUUID()

    future.withTimeout.onComplete {
      case result =>
        ui.access(
          new Runnable {
            override def run(): Unit = {
              result match {
                case Success(r) =>
                  success.apply(r)
                case Failure(t) =>
                  failure.apply(t)
              }
            }
          }
        )
    }
  }

  def awaitResult[T](future: Future[T])(success: T => Unit, failure: Throwable => Unit = (_) => {}) {
    Try(Await.result(future, timeout)) match {
      case Success(result) =>
        success.apply(result)
      case Failure(e) =>
        failure.apply(e)
    }
  }

  implicit class FutureExtensions[T](fut: Future[T]) {
    def withTimeout(implicit after: FiniteDuration, executor: ExecutionContext): Future[T] = {
      val prom = Promise[T]()
      val timeout = TimeoutScheduler.scheduleTimeout(prom, after)
      val combinedFut = Future.firstCompletedOf(List(fut, prom.future))
      fut onComplete {
        case result => timeout.cancel()
      }
      combinedFut
    }
  }

  private object TimeoutScheduler {
    val timer = new HashedWheelTimer(1, TimeUnit.SECONDS)

    def scheduleTimeout(promise: Promise[_], after: Duration) = {
      timer.newTimeout(
        new TimerTask {
          override def run(timeout: org.jboss.netty.util.Timeout): Unit = {
            promise.failure(new TimeoutException("Operation timed out after " + after.toSeconds + " seconds"))
          }
        }, after.toSeconds, TimeUnit.SECONDS
      )
    }
  }

}
