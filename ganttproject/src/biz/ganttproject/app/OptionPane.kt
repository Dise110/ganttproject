/*
Copyright 2019 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.app

import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Pane

interface I18N {
  fun formatText(key: String, args: Array<Any> = arrayOf()): String
  fun hasKey(key: String): Boolean
}

/**
 * Input data of a single option: its key for i18n purposes, user data for the handler and flag indicating if option
 * is initially selected.
 */
data class OptionElementData<T>(val i18nKey: String, val userData: T, val isSelected: Boolean = false, val customContent: Node? = null)

/**
 * This is a helper class which builds a UI widget consisting of a few mutually exclusive options where user is supposed
 * to choose one.
 *
 * The widget can be used inside other widgets or shown in a dialog. It thus becomes slightly more adavnced alternative
 * to JOptionPane class.
 */
class OptionPaneBuilder<T> {
  val i18n = DefaultStringSupplier()
  val titleString = i18n.create("title")
  val titleHelpString = i18n.create("titleHelp")

  /**
   * The root key in i18n key hierarchy for this widget. Builder automatically appends suffixes to this root key:
   * - title
   * - titleHelp
   */
  var i18nRootKey: String = ""
    set(value) {
      this.i18n.rootKey = value
    }

  /**
   * Style class added to the widget
   */
  var styleClass = ""

  /**
   * Stylesheet which is associated with the widget
   */
  var styleSheets: MutableList<String> = mutableListOf()

  /**
   * Graphic node shown in the left side of the widget
   */
  var graphic: Node? = null

  /**
   * The list of option elements.
   */
  var elements: List<OptionElementData<T>> = listOf()

  fun buildPane(optionHandler: (T) -> Unit): Pane {
    return DialogPane().also {
      this.buildDialogPane(it, optionHandler)
    }
  }

  fun showDialog(optionHandler: (T) -> Unit) {
    Platform.runLater {
      Dialog<Unit>().apply {
        buildDialogPane(dialogPane, optionHandler)
        show()
      }
    }
  }

  private fun buildDialogPane(pane: DialogPane, optionHandler: (T) -> Unit) {
    val vbox = VBoxBuilder()
    vbox.addTitle(this.titleString.value)
    vbox.add(Label().apply {
      this.textProperty().bind(this@OptionPaneBuilder.titleHelpString)
      this.styleClass.add("help")
    })

    val lockGroup = ToggleGroup()
    this.elements.forEach {
      val btn = RadioButton(this.i18n.formatText("${i18nRootKey}.${it.i18nKey}")).also { btn ->
        btn.styleClass.add("btn-option")
        btn.userData = it.userData
        btn.toggleGroup = lockGroup
        btn.isSelected = it.isSelected
      }
      vbox.add(btn)

      if (this.i18n.hasKey("${i18nRootKey}.${it.i18nKey}.help")) {
        val helpText = this.i18n.formatText("${i18nRootKey}.${it.i18nKey}.help")
        vbox.add(Label(helpText).apply { this.styleClass.add("option-help") })
      }
      it.customContent?.let { vbox.add(it) }
    }

    val builder = this
    pane.apply {
      styleClass.add(builder.styleClass)
      stylesheets.addAll(builder.styleSheets)
      builder.graphic?.let {
        graphic = it
      }

      content = vbox.vbox
      buttonTypes.add(ButtonType.OK)
      lookupButton(ButtonType.OK).apply {
        styleClass.add("btn-attention")
        addEventHandler(ActionEvent.ACTION) { evt ->
          val userData = lockGroup.selectedToggle.userData as T
          optionHandler(userData)
        }
      }
    }

  }
}
