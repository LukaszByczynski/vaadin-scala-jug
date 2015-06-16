package org.jug.vaadinscala.todo

import scala.concurrent.Future

trait TodoRepository {

  def findIds(filter: Option[String]): Future[Seq[Int]]

  def findByIds(ids: Seq[Int]): Future[Seq[Todo]]

  def findOne(id: Int): Future[Option[Todo]]

  def save(todo: Todo): Future[Todo]
}
