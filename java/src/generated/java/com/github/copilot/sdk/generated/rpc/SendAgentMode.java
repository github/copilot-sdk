/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SendAgentMode {
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code plan} variant. */
    PLAN("plan"),
    /** The {@code autopilot} variant. */
    AUTOPILOT("autopilot"),
    /** The {@code shell} variant. */
    SHELL("shell");

    private final String value;
    SendAgentMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SendAgentMode fromValue(String value) {
        for (SendAgentMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SendAgentMode value: " + value);
    }
}
