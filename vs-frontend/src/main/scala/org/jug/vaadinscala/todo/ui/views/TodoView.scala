package org.jug.vaadinscala.todo.ui.views

import com.vaadin.data.util.BeanItemContainer
import com.vaadin.ui.Alignment
import com.vaadin.ui.themes.ChameleonTheme
import org.jug.vaadinscala.todo.Todo
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.vaadin.addons.rinne._
import org.vaadin.addons.rinne.converters.Converters

@Component
@Scope("prototype")
class TodoView extends VVerticalLayout {
  sizeFull()
  margin = true
  spacing = true

  val todoContainer = new BeanItemContainer[Todo](classOf[Todo])
  todoContainer.addBean(Todo(1, Some("Test")))
  todoContainer.addBean(Todo(2, Some("Poznan Jug")))

  add(
    new VLabel {
      sizeUndefined()
      value = "Simple Todo Application"
      styleName = ChameleonTheme.LABEL_H1
    },
    alignment = Alignment.BOTTOM_CENTER
  )

  componentSet += new VHorizontalLayout {
    width = 100.percent
    spacing = true

    add(
      new VTextField {
        width = 90.percent
        prompt = "Search..."
        enabled = false
      },
      alignment = Alignment.MIDDLE_LEFT,
      ratio = 1
    )

    add(
      new VButton {
        caption = "Add TODO"
        styleName = ChameleonTheme.BUTTON_DEFAULT
      },
      alignment = Alignment.MIDDLE_RIGHT
    )
  }

  add(
    new VTable {
      sizeFull()

      dataSource = todoContainer

      visibleColumns = Seq("id", "content")
      columnHeaders = Seq("#", "Todo")
      setColumnExpandRatio("content", 1)

      setConverter("content", Converters.optionToString)
    },
    ratio = 1
  )
}
