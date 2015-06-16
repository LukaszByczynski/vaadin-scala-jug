package org.jug.vaadinscala.todo.persist.todo

import org.jug.vaadinscala.todo.Todo
import org.springframework.stereotype.Component

import scala.slick.driver.PostgresDriver.simple._

@Component
class DBTodoRepository {

  private val dbTodos = TableQuery[DBTodo]

  private lazy val _insertInvoker = (dbTodos returning dbTodos.map(_.id.get)).insertInvoker

  private lazy val _findOneQuery = Compiled(
    (id: Column[Int]) => dbTodos.filter(_.id === id).map(
      t => (t.content)
    )
  )

  private lazy val _findIdsQuery = Compiled(
    (filter: Column[Option[String]]) => dbTodos.map(
      _.id.get
    )
  )

  private lazy val _findByIdsQuery = (ids: Seq[Int]) => dbTodos.filter(_.id inSetBind ids).map(
    t => (t.id.get, t.content)
  )

  private lazy val _updateQuery = Compiled(
    (id: Column[Int]) => dbTodos.filter(_.id === id).map(
      t => t.content
    )
  )

  def save(e: Todo)(implicit session: Session): Todo = {
    if (e.id == 0) {
      e.id = _insertInvoker +=(
        None,
        e.content
        )
    } else {
      _updateQuery(e.id).update((e.content))
    }
    e
  }

  def findOne(id: Int)(implicit session: Session): Option[Todo] = {
    _findOneQuery(id).mapResult {
      case content => Todo(id, content)
    }.firstOption
  }

  def findByIds(ids: Seq[Int])(implicit session: Session): Seq[Todo] = {
    val result = _findByIdsQuery(ids).mapResult {
      case (id, content) => (id, Todo(id, content))
    }.toMap
    // order return values by input ids (it's hard to do that in db)
    ids.map(result.get(_).get).toSeq
  }

  def findIds(filter: Option[String])(implicit session: Session): Seq[Int] = {
    _findIdsQuery(filter.map(_.trim)).list
  }

}