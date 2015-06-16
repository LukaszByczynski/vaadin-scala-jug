package org.jug.vaadinscala.todo.akka

import javax.annotation.PreDestroy

class TypedActorProxy[T <: AnyRef](creator: () => T, disposer: T => Unit) {

  private var _actor: Option[T] = None

  def apply(): T = {
    _actor match {
      case Some(actor) => actor
      case None =>
        synchronized {
          _actor match {
            case Some(actor) => actor
            case None =>
              _actor = Some(creator())
              _actor.get
          }
        }
    }
  }

  @PreDestroy
  def dispose(): Unit = {
    _actor.foreach(disposer)
    _actor = None
  }
}

