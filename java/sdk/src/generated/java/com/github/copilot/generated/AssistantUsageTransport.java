/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Transport used for a successful model call
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AssistantUsageTransport {
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code websocket} variant. */
    WEBSOCKET("websocket");

    private final String value;
    AssistantUsageTransport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AssistantUsageTransport fromValue(String value) {
        for (AssistantUsageTransport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AssistantUsageTransport value: " + value);
    }
}
