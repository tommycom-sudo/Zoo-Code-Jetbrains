// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.zoocode.jetbrains.selection

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

class SelectionPopupConfigurable : Configurable {
    private val showPopupOnSelection = JBCheckBox(
        "Show Zoo Code actions when text is selected",
    )

    private val panel: JPanel = FormBuilder.createFormBuilder()
        .addComponent(showPopupOnSelection)
        .addComponentToRightColumn(
            JBLabel(
                "When enabled, selecting non-empty code shows a compact " +
                    "\"Add to conversation\" action near the selection.",
            ).apply {
                foreground = JBColor.GRAY
                border = JBUI.Borders.emptyLeft(24)
            },
        )
        .addComponentFillVertically(JPanel(), 0)
        .panel
        .apply {
            border = JBUI.Borders.empty(8)
        }

    override fun getDisplayName(): String = "Zoo Code"

    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean {
        return showPopupOnSelection.isSelected !=
            SelectionPopupSettings.getInstance().state.showPopupOnSelection
    }

    override fun apply() {
        SelectionPopupSettings.getInstance().state.showPopupOnSelection =
            showPopupOnSelection.isSelected

        ProjectManager.getInstance().openProjects.forEach { project ->
            project.getService(ZooCodeSelectionPopupService::class.java)
                .onSettingsChanged()
        }
    }

    override fun reset() {
        showPopupOnSelection.isSelected =
            SelectionPopupSettings.getInstance().state.showPopupOnSelection
    }

    override fun disposeUIResources() = Unit
}
