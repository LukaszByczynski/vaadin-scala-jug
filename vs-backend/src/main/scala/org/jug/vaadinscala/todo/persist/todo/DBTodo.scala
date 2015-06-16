package org.jug.vaadinscala.todo.persist.todo

import scala.slick.driver.PostgresDriver.simple._

class DBTodo(tag: Tag) extends Table[(
  Option[Int],
    Option[String]
  )](tag, "todo") {

  def * = (id, content)

  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)

  def content = column[Option[String]]("content")

}
