// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.zoocode.jetbrains.ipc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket

class NodeSocketTest {
    @Test
    fun `active socket treats peer disconnect as unexpected`() {
        withSocketPair { nodeSocket ->
            assertFalse(nodeSocket.isPeerDisconnectExpected())
        }
    }

    @Test
    fun `expected shutdown treats peer disconnect as normal`() {
        withSocketPair { nodeSocket ->
            nodeSocket.expectClose()

            assertTrue(nodeSocket.isPeerDisconnectExpected())
        }
    }

    @Test
    fun `dispose marks peer disconnect as expected`() {
        withSocketPair { nodeSocket ->
            nodeSocket.dispose()

            assertTrue(nodeSocket.isPeerDisconnectExpected())
        }
    }

    private fun withSocketPair(test: (NodeSocket) -> Unit) {
        ServerSocket(0).use { server ->
            Socket("127.0.0.1", server.localPort).use { client ->
                server.accept().use { peer ->
                    val nodeSocket = NodeSocket(client, "test")
                    try {
                        test(nodeSocket)
                    } finally {
                        nodeSocket.dispose()
                        peer.close()
                    }
                }
            }
        }
    }
}
