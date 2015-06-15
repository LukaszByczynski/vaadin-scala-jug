package org.jug.vaadinscala.todo

import scala.beans.BeanProperty

case class Todo(
    @BeanProperty var id: Int = 0,
    @BeanProperty var content: Option[String] = None
  )