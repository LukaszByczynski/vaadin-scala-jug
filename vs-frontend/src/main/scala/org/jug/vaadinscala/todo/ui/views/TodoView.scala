package org.jug.vaadinscala.todo.ui.views

import javax.annotation.PostConstruct
import javax.inject.Inject

import com.vaadin.data.util.BeanItemContainer
import com.vaadin.ui.{UI, Alignment}
import com.vaadin.ui.themes.ChameleonTheme
import org.jug.vaadinscala.todo.Todo
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.vaadin.addons.rinne._
import org.vaadin.addons.rinne.converters.Converters

@Component
@Scope("prototype")
class TodoView extends VVerticalLayout {

  @Inject var todoEditorView: TodoEditorView = _

  val todoContainer = new BeanItemContainer[Todo](classOf[Todo])

  var idGen = 1

  @PostConstruct
  def init() {
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

          clickListeners += addTodo()
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

  def addTodo(): Unit = {
    UI.getCurrent.addWindow(
      new VWindow {
        width = 50.percent
        height = 50.percent
        modal = true

        caption = "Add Todo"
        content = todoEditorView

        todoEditorView.bind(Todo())
        todoEditorView.onCancelClick = () => { close() }
        todoEditorView.onSaveClick = (item) => {
          item.id = idGen
          idGen += 1
          todoContainer.addBean(item)
          close()
        }
      }
    )
  }
}
