/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;

/**
 * Tests for {@link CopilotClientMode} and Empty mode validation.
 */
public class CopilotClientModeTest {

    @Test
    void testDefaultModeIsCopilotCli() {
        var opts = new CopilotClientOptions();
        assertEquals(CopilotClientMode.COPILOT_CLI, opts.getMode());
    }

    @Test
    void testSetModeEmpty() {
        var opts = new CopilotClientOptions();
        opts.setMode(CopilotClientMode.EMPTY);
        assertEquals(CopilotClientMode.EMPTY, opts.getMode());
    }

    @Test
    void testEmptyModeRequiresCopilotHome() {
        var opts = new CopilotClientOptions().setMode(CopilotClientMode.EMPTY).setAutoStart(false);
        // Empty mode without copilotHome should throw
        var ex = assertThrows(IllegalArgumentException.class, () -> new CopilotClient(opts));
        assertTrue(ex.getMessage().contains("Empty mode"));
    }

    @Test
    void testEmptyModeWithCopilotHome() {
        var opts = new CopilotClientOptions().setMode(CopilotClientMode.EMPTY).setCopilotHome("/tmp/copilot-home")
                .setAutoStart(false);
        // Should not throw - copilotHome is set
        var client = new CopilotClient(opts);
        assertEquals(ConnectionState.DISCONNECTED, client.getState());
        client.close();
    }

    @Test
    void testCopilotClientModeEnumValues() {
        assertEquals(2, CopilotClientMode.values().length);
        assertEquals(CopilotClientMode.EMPTY, CopilotClientMode.valueOf("EMPTY"));
        assertEquals(CopilotClientMode.COPILOT_CLI, CopilotClientMode.valueOf("COPILOT_CLI"));
    }

    @Test
    void testEnumSerializationNames() {
        // CopilotClientMode is a plain enum; verify the values exist
        assertNotNull(CopilotClientMode.EMPTY);
        assertNotNull(CopilotClientMode.COPILOT_CLI);
    }
}
