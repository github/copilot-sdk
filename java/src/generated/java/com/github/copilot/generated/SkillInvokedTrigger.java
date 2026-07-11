/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * What triggered the skill invocation: `user-invoked` (explicit user action, such as via a slash command or UI affordance), `agent-invoked` (agent requested the skill), or `context-load` (loaded as part of another context, such as preloading skills configured on a custom agent or subagent)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillInvokedTrigger {
    /** The {@code user-invoked} variant. */
    USER_INVOKED("user-invoked"),
    /** The {@code agent-invoked} variant. */
    AGENT_INVOKED("agent-invoked"),
    /** The {@code context-load} variant. */
    CONTEXT_LOAD("context-load");

    private final String value;
    SkillInvokedTrigger(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillInvokedTrigger fromValue(String value) {
        for (SkillInvokedTrigger v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillInvokedTrigger value: " + value);
    }
}
