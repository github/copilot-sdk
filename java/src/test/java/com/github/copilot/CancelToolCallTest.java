/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.AbortSignal;

/**
 * Unit tests for {@link CopilotSession#cancelToolCall(String)}.
 * <p>
 * Uses reflection to inject {@link AbortSignal} instances directly into the
 * session's active-tool-signal tracking map, allowing the cancellation logic to
 * be verified in isolation without requiring the full E2E test harness.
 */
class CancelToolCallTest {

    /**
     * Injects two signals into a session, calls cancelToolCall for one, and
     * verifies that only the targeted signal is aborted while the other remains
     * unaffected.
     */
    @Test
    void cancelToolCallFiresOnlyTargetedSignal() throws Exception {
        var session = new CopilotSession("sess-cancel-test", null);

        AbortSignal signalA = new AbortSignal();
        AbortSignal signalB = new AbortSignal();

        Map<String, AbortSignal> map = getActiveToolSignals(session);
        map.put("call-A", signalA);
        map.put("call-B", signalB);

        boolean result = session.cancelToolCall("call-A");

        assertTrue(result, "cancelToolCall should return true for a known toolCallId");
        assertTrue(signalA.isAborted(), "signal A should be aborted after cancelToolCall(call-A)");
        assertFalse(signalB.isAborted(), "signal B must NOT be aborted — only the targeted signal fires");
    }

    /**
     * Verifies that cancelToolCall returns false for an unknown tool call ID,
     * without affecting any in-flight signals.
     */
    @Test
    void cancelToolCallReturnsFalseForUnknownId() throws Exception {
        var session = new CopilotSession("sess-cancel-unknown", null);

        AbortSignal signal = new AbortSignal();
        Map<String, AbortSignal> map = getActiveToolSignals(session);
        map.put("call-exists", signal);

        boolean result = session.cancelToolCall("call-does-not-exist");

        assertFalse(result, "cancelToolCall should return false for an unknown toolCallId");
        assertFalse(signal.isAborted(), "existing signal must not be affected");
    }

    /**
     * Verifies that a cancelled signal is removed from the tracking map so it
     * cannot be double-fired.
     */
    @Test
    void cancelToolCallRemovesSignalFromMap() throws Exception {
        var session = new CopilotSession("sess-cancel-cleanup", null);

        AbortSignal signal = new AbortSignal();
        Map<String, AbortSignal> map = getActiveToolSignals(session);
        map.put("call-X", signal);

        session.cancelToolCall("call-X");

        assertFalse(map.containsKey("call-X"), "signal should be removed from the map after cancellation");
        // second call must return false since the entry is gone
        assertFalse(session.cancelToolCall("call-X"), "second cancelToolCall for same id should return false");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AbortSignal> getActiveToolSignals(CopilotSession session) throws Exception {
        Field f = CopilotSession.class.getDeclaredField("activeToolSignals");
        f.setAccessible(true);
        return (Map<String, AbortSignal>) f.get(session);
    }
}
