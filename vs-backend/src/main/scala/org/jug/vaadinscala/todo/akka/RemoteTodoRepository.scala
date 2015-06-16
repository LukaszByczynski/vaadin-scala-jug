package org.jug.vaadinscala.todo.akka

import javax.inject.Inject

import org.jug.vaadinscala.todo.persist.DBProvider
import org.jug.vaadinscala.todo.persist.todo.DBTodoRepository
import org.jug.vaadinscala.todo.{PersistFailureException, Todo, TodoRepository}
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import scala.concurrent.{Future, Promise}

@Component
@Scope("prototype")
class RemoteTodoRepository extends TodoRepository {

  @Inject protected var dbProvider: DBProvider = _

  @Inject protected var dbTodoRepository: DBTodoRepository = _

  protected def safeQuery[T](query: => T): Future[T] = try {
    Promise.successful(query).future
  } catch {
    case t: Throwable =>
      Promise.failed(PersistFailureException(t.getMessage)).future
  }

  override def findOne(id: Int): Future[Option[Todo]] = dbProvider().withSession {
    implicit session => safeQuery(dbTodoRepository.findOne(id))
  }

  override def save(todo: Todo): Future[Todo] = dbProvider().withTransaction {
    implicit session => safeQuery(dbTodoRepository.save(todo))
  }

  override def findIds(filter: Option[String]): Future[Seq[Int]] = dbProvider().withSession {
    implicit session => safeQuery(dbTodoRepository.findIds(filter))
  }

  override def findByIds(ids: Seq[Int]): Future[Seq[Todo]] = dbProvider().withSession {
    implicit session => safeQuery(dbTodoRepository.findByIds(ids))
  }
}
