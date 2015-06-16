package org.jug.vaadinscala.todo.akka

import akka.actor._

import scala.util.{Failure, Try}

class PerRequestSelectionActor(path: String) extends Actor {

  override def receive: Receive = {
    case msg =>
      Try(context.actorSelection(path).forward(msg)) match {
        case Failure(f) =>
          println(s"Something goes wrong with forwarded message: $f")
        case _ =>
      }
  }
}
