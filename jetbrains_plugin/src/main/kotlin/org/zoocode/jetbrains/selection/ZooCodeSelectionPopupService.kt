// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.zoocode.jetbrains.selection

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import org.zoocode.jetbrains.extensions.plugin.zoo.ZooCodeContextMenuProvider
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.IdentityHashMap
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class ZooCodeSelectionPopupService(private val project: Project) : Disposable {
    private val log = Logger.getInstance(ZooCodeSelectionPopupService::class.java)
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val attachments = IdentityHashMap<Editor, EditorAttachment>()
    private var popup: JBPopup? = null
    private var popupEditor: Editor? = null
    private var initialized = false

    fun initialize() {
        if (initialized) {
            return
        }
        initialized = true

        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    attach(event.editor)
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    detach(event.editor)
                }
            },
            this,
        )

        EditorFactory.getInstance().allEditors.forEach(::attach)
    }

    fun addSelection(editor: Editor) {
        if (!editor.selectionModel.hasSelection()) {
            return
        }

        val added = ZooCodeContextMenuProvider.addSelectionToContext(project, editor)
        if (!added) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Zoo Code JetBrains")
                .createNotification(
                    "Zoo Code",
                    "Open the Zoo Code chat before adding selected code to the conversation.",
                    NotificationType.WARNING,
                )
                .notify(project)
        }
        dismissPopup()
    }

    fun onSettingsChanged() {
        if (!SelectionPopupSettings.getInstance().isPopupEnabled) {
            dismissPopup()
            alarm.cancelAllRequests()
        }
    }

    fun dismissPopup() {
        val current = popup ?: return
        popup = null
        popupEditor = null
        if (!current.isDisposed) {
            current.cancel()
        }
    }

    private fun attach(editor: Editor) {
        if (editor.project != project ||
            editor.editorKind != EditorKind.MAIN_EDITOR ||
            attachments.containsKey(editor)
        ) {
            return
        }

        val parent = Disposer.newDisposable(
            "ZooCodeSelection-${System.identityHashCode(editor)}",
        )
        val attachment = object :
            EditorAttachment,
            SelectionListener,
            CaretListener,
            DocumentListener,
            EditorMouseListener,
            VisibleAreaListener {
            override fun selectionChanged(event: SelectionEvent) {
                onSelectionChanged(editor)
            }

            override fun caretPositionChanged(event: CaretEvent) {
                if (!editor.selectionModel.hasSelection()) {
                    dismissPopup()
                    alarm.cancelAllRequests()
                }
            }

            override fun documentChanged(event: DocumentEvent) {
                dismissPopup()
                alarm.cancelAllRequests()
            }

            override fun mousePressed(event: EditorMouseEvent) {
                dismissPopup()
                alarm.cancelAllRequests()
            }

            override fun mouseReleased(event: EditorMouseEvent) {
                if (event.mouseEvent.button == MouseEvent.BUTTON1) {
                    schedulePopup(editor)
                }
            }

            override fun visibleAreaChanged(event: VisibleAreaEvent) {
                dismissPopup()
            }

            override fun dispose() {
                editor.selectionModel.removeSelectionListener(this)
                editor.caretModel.removeCaretListener(this)
                editor.document.removeDocumentListener(this)
                editor.removeEditorMouseListener(this)
                editor.scrollingModel.removeVisibleAreaListener(this)
                Disposer.dispose(parent)
            }
        }

        editor.selectionModel.addSelectionListener(attachment)
        editor.caretModel.addCaretListener(attachment)
        editor.document.addDocumentListener(attachment)
        editor.addEditorMouseListener(attachment)
        editor.scrollingModel.addVisibleAreaListener(attachment)
        attachments[editor] = attachment
    }

    private fun detach(editor: Editor) {
        val attachment = attachments.remove(editor) ?: return
        if (popupEditor == editor) {
            dismissPopup()
        }
        runCatching(attachment::dispose)
            .onFailure { log.debug("Failed to detach Zoo Code selection listener", it) }
    }

    private fun onSelectionChanged(editor: Editor) {
        if (!editor.selectionModel.hasSelection()) {
            dismissPopup()
            alarm.cancelAllRequests()
            return
        }

        if (popupEditor != null) {
            dismissPopup()
        }
        schedulePopup(editor)
    }

    private fun schedulePopup(editor: Editor) {
        if (!SelectionPopupSettings.getInstance().isPopupEnabled ||
            editor.isDisposed ||
            editor.project != project
        ) {
            return
        }

        alarm.cancelAllRequests()
        alarm.addRequest({ showPopupIfSelectionStillPresent(editor) }, SHOW_DELAY_MS)
    }

    private fun showPopupIfSelectionStillPresent(editor: Editor) {
        if (!SelectionPopupSettings.getInstance().isPopupEnabled ||
            editor.isDisposed ||
            !editor.selectionModel.hasSelection() ||
            FileDocumentManager.getInstance().getFile(editor.document) == null
        ) {
            return
        }

        dismissPopup()

        val end = editor.offsetToVisualPosition(editor.selectionModel.selectionEnd)
        val editorPoint = editor.visualPositionToXY(end)
        val screenPoint = Point(editorPoint.x + 6, editorPoint.y + editor.lineHeight)
        SwingUtilities.convertPointToScreen(screenPoint, editor.contentComponent)

        val currentPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(createPopupContent(editor), null)
            .setProject(project)
            .setRequestFocus(false)
            .setFocusable(false)
            .setResizable(false)
            .setMovable(false)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelCallback {
                popup = null
                popupEditor = null
                true
            }
            .createPopup()

        popup = currentPopup
        popupEditor = editor
        currentPopup.showInScreenCoordinates(editor.contentComponent, screenPoint)
    }

    private fun createPopupContent(editor: Editor): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            JBUI.Borders.empty(3, 8),
        )

        val button = JButton(
            "添加到对话",
            com.intellij.icons.AllIcons.General.Add,
        ).apply {
            isOpaque = false
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(2, 4)
            toolTipText = "Add selected code to the Zoo Code conversation"
            addActionListener { addSelection(editor) }
        }

        val brand = JBLabel("Zoo Code").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(font.size2D - 1f)
        }
        panel.add(brand, BorderLayout.WEST)
        panel.add(
            JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
                isOpaque = false
                add(button)
            },
            BorderLayout.CENTER,
        )
        panel.preferredSize = Dimension(165, 34)
        return panel
    }

    override fun dispose() {
        alarm.cancelAllRequests()
        dismissPopup()
        attachments.keys.toList().forEach(::detach)
    }

    private interface EditorAttachment : Disposable

    companion object {
        private const val SHOW_DELAY_MS = 180

        fun getInstance(project: Project): ZooCodeSelectionPopupService =
            project.getService(ZooCodeSelectionPopupService::class.java)
    }
}
