package org.jug.vaadinscala.todo

case class PersistFailureException(msg: String) extends RuntimeException(msg)