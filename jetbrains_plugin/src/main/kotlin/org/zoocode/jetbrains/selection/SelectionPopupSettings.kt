// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.zoocode.jetbrains.selection

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "ZooCodeSelectionPopupSettings",
    storages = [Storage("zoo-code-selection.xml")],
)
class SelectionPopupSettings : PersistentStateComponent<SelectionPopupSettings.State> {
    data class State(
        var showPopupOnSelection: Boolean = true,
    )

    private var currentState = State()

    override fun getState(): State = currentState

    override fun loadState(state: State) {
        currentState = state
    }

    val isPopupEnabled: Boolean
        get() = currentState.showPopupOnSelection

    companion object {
        fun getInstance(): SelectionPopupSettings =
            ApplicationManager.getApplication().getService(SelectionPopupSettings::class.java)
    }
}
