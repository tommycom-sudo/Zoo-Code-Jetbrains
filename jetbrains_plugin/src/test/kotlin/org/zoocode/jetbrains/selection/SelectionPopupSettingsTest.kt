// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.zoocode.jetbrains.selection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionPopupSettingsTest {
    @Test
    fun `selection popup is enabled by default`() {
        assertTrue(SelectionPopupSettings().isPopupEnabled)
    }

    @Test
    fun `selection popup setting is restored from persisted state`() {
        val settings = SelectionPopupSettings()
        settings.loadState(
            SelectionPopupSettings.State(showPopupOnSelection = false),
        )

        assertFalse(settings.isPopupEnabled)
    }
}
