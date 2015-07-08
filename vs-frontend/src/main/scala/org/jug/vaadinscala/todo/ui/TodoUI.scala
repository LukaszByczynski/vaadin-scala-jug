package org.jug.vaadinscala.todo.ui

import javax.inject.Inject

import com.vaadin.annotations.{Push, Theme}
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.UI
import org.jug.vaadinscala.todo.ui.views.TodoView

@SpringUI
@Push
@Theme("test")
class TodoUI extends UI {

  @Inject private var todoView: TodoView = _

  override def init(vaadinRequest: VaadinRequest): Unit = {
    setContent(todoView)
  }
}