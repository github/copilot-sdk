/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.AgentMode;

/**
 * Unit tests for {@link AgentMode} serialization, deserialization, and
 * unknown-value behavior.
 */
public class AgentModeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(AgentMode.class)
    void jsonRoundTrip_allValues(AgentMode mode) throws Exception {
        String json = mapper.writeValueAsString(mode);
        AgentMode deserialized = mapper.readValue(json, AgentMode.class);
        assertEquals(mode, deserialized);
    }

    @Test
    void getValue_returnsExpectedStrings() {
        assertEquals("interactive", AgentMode.INTERACTIVE.getValue());
        assertEquals("plan", AgentMode.PLAN.getValue());
        assertEquals("autopilot", AgentMode.AUTOPILOT.getValue());
        assertEquals("shell", AgentMode.SHELL.getValue());
    }

    @Test
    void fromValue_knownValues_returnsCorrectEnum() {
        assertEquals(AgentMode.INTERACTIVE, AgentMode.fromValue("interactive"));
        assertEquals(AgentMode.PLAN, AgentMode.fromValue("plan"));
        assertEquals(AgentMode.AUTOPILOT, AgentMode.fromValue("autopilot"));
        assertEquals(AgentMode.SHELL, AgentMode.fromValue("shell"));
    }

    @Test
    void fromValue_null_returnsNull() {
        assertNull(AgentMode.fromValue(null));
    }

    @Test
    void fromValue_unknownValue_throwsWithConsistentMessage() {
        var ex = assertThrows(IllegalArgumentException.class, () -> AgentMode.fromValue("unknown"));
        assertEquals("Unknown AgentMode value: unknown", ex.getMessage());
    }

    @Test
    void jsonDeserialize_unknownValue_throws() {
        String json = "\"not-a-mode\"";
        assertThrows(Exception.class, () -> mapper.readValue(json, AgentMode.class));
    }

    @Test
    void jsonSerialize_writesStringValue() throws Exception {
        String json = mapper.writeValueAsString(AgentMode.AUTOPILOT);
        assertEquals("\"autopilot\"", json);
    }
}
