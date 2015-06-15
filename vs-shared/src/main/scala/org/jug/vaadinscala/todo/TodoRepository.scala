package org.jug.vaadinscala.todo

import scala.concurrent.Future

trait TodoRepository {

  def findOne(id: Int): Future[Option[Todo]]

  def save(todo: Todo): Future[Todo]
}
