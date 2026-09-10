/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Authority or runtime mechanism responsible for sub-agent model selection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SubagentModelSelectionSource {
    /** The {@code explicit_override} variant. */
    EXPLICIT_OVERRIDE("explicit_override"),
    /** The {@code configured_required} variant. */
    CONFIGURED_REQUIRED("configured_required"),
    /** The {@code configured_preference} variant. */
    CONFIGURED_PREFERENCE("configured_preference"),
    /** The {@code complementary_default} variant. */
    COMPLEMENTARY_DEFAULT("complementary_default"),
    /** The {@code session_inheritance} variant. */
    SESSION_INHERITANCE("session_inheritance"),
    /** The {@code agent_definition_default} variant. */
    AGENT_DEFINITION_DEFAULT("agent_definition_default"),
    /** The {@code runtime_policy} variant. */
    RUNTIME_POLICY("runtime_policy");

    private final String value;
    SubagentModelSelectionSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SubagentModelSelectionSource fromValue(String value) {
        for (SubagentModelSelectionSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SubagentModelSelectionSource value: " + value);
    }
}
