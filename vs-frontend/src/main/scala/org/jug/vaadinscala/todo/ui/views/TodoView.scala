package org.jug.vaadinscala.todo.ui.views

import com.vaadin.ui.Alignment
import com.vaadin.ui.themes.ChameleonTheme
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.vaadin.addons.rinne._

@Component
@Scope("prototype")
class TodoView extends VVerticalLayout {
  sizeFull()
  margin = true
  spacing = true

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
    },
    ratio = 1
  )
}
