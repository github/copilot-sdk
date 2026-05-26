/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.AgentInfo;

/**
 * Unit tests for {@link AgentInfo} getters, setters, and fluent chaining.
 */
class AgentInfoTest {

    @Test
    void defaultValuesAreNull() {
        var agent = new AgentInfo();
        assertNull(agent.getName());
        assertNull(agent.getDisplayName());
        assertNull(agent.getDescription());
    }

    @Test
    void nameGetterSetter() {
        var agent = new AgentInfo();
        agent.setName("coder");
        assertEquals("coder", agent.getName());
    }

    @Test
    void displayNameGetterSetter() {
        var agent = new AgentInfo();
        agent.setDisplayName("Code Assistant");
        assertEquals("Code Assistant", agent.getDisplayName());
    }

    @Test
    void descriptionGetterSetter() {
        var agent = new AgentInfo();
        agent.setDescription("Helps with coding tasks");
        assertEquals("Helps with coding tasks", agent.getDescription());
    }

    @Test
    void fluentChainingReturnsThis() {
        var agent = new AgentInfo().setName("coder").setDisplayName("Code Assistant")
                .setDescription("Helps with coding tasks");

        assertEquals("coder", agent.getName());
        assertEquals("Code Assistant", agent.getDisplayName());
        assertEquals("Helps with coding tasks", agent.getDescription());
    }

    @Test
    void fluentChainingReturnsSameInstance() {
        var agent = new AgentInfo();
        assertSame(agent, agent.setName("test"));
        assertSame(agent, agent.setDisplayName("Test"));
        assertSame(agent, agent.setDescription("A test agent"));
    }
}
