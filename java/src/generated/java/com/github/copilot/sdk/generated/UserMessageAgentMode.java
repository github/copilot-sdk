/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * The agent mode that was active when this message was sent
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UserMessageAgentMode {
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code plan} variant. */
    PLAN("plan"),
    /** The {@code autopilot} variant. */
    AUTOPILOT("autopilot"),
    /** The {@code shell} variant. */
    SHELL("shell");

    private final String value;
    UserMessageAgentMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UserMessageAgentMode fromValue(String value) {
        for (UserMessageAgentMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UserMessageAgentMode value: " + value);
    }
}
