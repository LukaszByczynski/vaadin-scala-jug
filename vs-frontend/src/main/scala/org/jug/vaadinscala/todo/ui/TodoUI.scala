package org.jug.vaadinscala.todo.ui

import com.vaadin.annotations.Theme
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.UI
import org.vaadin.addons.rinne._

@SpringUI
@Theme("chameleon")
class TodoUI extends UI {
  override def init(vaadinRequest: VaadinRequest): Unit = {
    setContent(
      new VVerticalLayout {
        sizeFull()

        componentSet += new VLabel {
          value = "Hello JUG!"
        }
      }
    )
  }
}
