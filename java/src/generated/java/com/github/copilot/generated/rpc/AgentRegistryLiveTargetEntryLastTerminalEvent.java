/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistryLiveTargetEntryLastTerminalEvent {
    /** The {@code turn_end} variant. */
    TURN_END("turn_end"),
    /** The {@code abort} variant. */
    ABORT("abort");

    private final String value;
    AgentRegistryLiveTargetEntryLastTerminalEvent(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistryLiveTargetEntryLastTerminalEvent fromValue(String value) {
        for (AgentRegistryLiveTargetEntryLastTerminalEvent v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistryLiveTargetEntryLastTerminalEvent value: " + value);
    }
}
