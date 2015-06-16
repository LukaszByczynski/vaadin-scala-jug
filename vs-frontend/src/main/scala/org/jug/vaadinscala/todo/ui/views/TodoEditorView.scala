package org.jug.vaadinscala.todo.ui.views

import javax.annotation.PostConstruct

import com.vaadin.data.fieldgroup.FieldGroup
import com.vaadin.data.util.BeanItem
import com.vaadin.ui.Alignment
import com.vaadin.ui.themes.ValoTheme
import org.jug.vaadinscala.todo.Todo
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.vaadin.addons.rinne._
import org.vaadin.addons.rinne.converters.Converters

@Component
@Scope("prototype")
class TodoEditorView extends VVerticalLayout {

  private val fieldGroup = new FieldGroup()
  fieldGroup.setBuffered(false)

  private var beanItem: Option[BeanItem[Todo]] = None

  var onCancelClick = () => {}
  var onSaveClick = (item: Todo) => {}

  @PostConstruct
  def init(): Unit = {
    sizeFull()
    margin = true
    spacing = true

    add(
      new VFormLayout {
        sizeFull()

        componentSet += new VTextArea {
          maxLength = 255
          fieldGroup.bind(this, "content")
          sizeFull()
          setConverter(Converters.optionToString)
        }
      },
      alignment = Alignment.BOTTOM_CENTER,
      ratio = 1
    )

    add(
      new VHorizontalLayout {
        width = 100.percent
        spacing = true

        add(new VLabel, ratio = 1)

        add(
          new VButton {
            caption = "Save"
            styleName = ValoTheme.BUTTON_PRIMARY

            clickListeners += {
              beanItem.foreach(b => onSaveClick(b.getBean))
            }
          },
          alignment = Alignment.BOTTOM_RIGHT
        )

        add(
          new VButton {
            caption = "Cancel"
          },
          alignment = Alignment.BOTTOM_RIGHT
        )
      },
      alignment = Alignment.BOTTOM_CENTER
    )
  }

  def bind(item: Todo): Unit = {
    beanItem = Some(new BeanItem[Todo](item))
    fieldGroup.setItemDataSource(beanItem.get)
  }
}