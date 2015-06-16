package org.jug.vaadinscala.todo.ui.views

import javax.annotation.PostConstruct
import javax.inject.Inject

import com.vaadin.ui.themes.ValoTheme
import com.vaadin.ui.{Alignment, Notification, UI}
import org.jug.vaadinscala.todo.akka.TypedActorProxy
import org.jug.vaadinscala.todo.async.Async
import org.jug.vaadinscala.todo.container.RemoteTodoContainer
import org.jug.vaadinscala.todo.{Todo, TodoRepository}
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.vaadin.addons.rinne._
import org.vaadin.addons.rinne.converters.Converters

import scala.concurrent.Future

@Component
@Scope("prototype")
class TodoView extends VVerticalLayout {

  @Inject var todoEditorView: TodoEditorView = _

  @Inject var todoRepository: TypedActorProxy[TodoRepository] = _

  private val searchField = new VTextField {
    width = 90.percent
    prompt = "Search..."

    valueChangeListeners += {
      remoteTodoContainer.refresh()
    }
  }

  lazy val remoteTodoContainer: RemoteTodoContainer = new RemoteTodoContainer(todoRepository) {
    /** Gets ids of the items managed by container */
    override protected def findIds(): Future[Seq[Int]] = {
      todoRepository().findIds(optimizeSearch(searchField.value))
    }

    def optimizeSearch(value: Option[String]): Option[String] = value.map(_.trim) match {
      case Some(text) =>
        if (text.length > 0)
          Some(text)
        else
          None
      case _ => None
    }
  }

  @PostConstruct
  def init() {
    sizeFull()
    margin = true
    spacing = true

    add(
      new VLabel {
        sizeUndefined()
        value = "Simple Todo Application"
        styleName = ValoTheme.LABEL_H1
      },
      alignment = Alignment.BOTTOM_CENTER
    )

    componentSet += new VHorizontalLayout {
      width = 100.percent
      spacing = true

      add(
        searchField,
        alignment = Alignment.MIDDLE_LEFT,
        ratio = 1
      )

      add(
        new VButton {
          caption = "Add TODO"
          styleName = ValoTheme.BUTTON_PRIMARY

          clickListeners += addTodo()
        },
        alignment = Alignment.MIDDLE_RIGHT
      )
    }

    add(
      new VTable {
        sizeFull()

        dataSource = remoteTodoContainer

        visibleColumns = Seq("id", "content")
        columnHeaders = Seq("#", "Todo")
        setColumnExpandRatio("content", 1)

        setConverter("content", Converters.optionToString)
      },
      ratio = 1
    )
  }

  def addTodo(): Unit = {
    todoEditorView.enabled = true
    UI.getCurrent.addWindow(
      new VWindow {
        width = 50.percent
        height = 50.percent
        modal = true

        caption = "Add Todo"
        content = todoEditorView

        todoEditorView.bind(Todo())
        todoEditorView.onCancelClick = () => {
          close()
        }
        todoEditorView.onSaveClick = (item) => {
          todoEditorView.enabled = false
          Async.default(todoRepository().save(item))(
            success => {
              close()
              remoteTodoContainer.refresh()
            },
            failure => {
              Notification.show(failure.toString, Notification.Type.ERROR_MESSAGE)
              todoEditorView.enabled = true
            }
          )
        }
      }
    )
  }
}
