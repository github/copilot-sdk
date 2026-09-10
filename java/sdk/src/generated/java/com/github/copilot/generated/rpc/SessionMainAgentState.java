/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether the main agent is executing, blocked on interactive input, or idle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionMainAgentState {
    /** The {@code working} variant. */
    WORKING("working"),
    /** The {@code waiting} variant. */
    WAITING("waiting"),
    /** The {@code idle} variant. */
    IDLE("idle");

    private final String value;
    SessionMainAgentState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionMainAgentState fromValue(String value) {
        for (SessionMainAgentState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionMainAgentState value: " + value);
    }
}
