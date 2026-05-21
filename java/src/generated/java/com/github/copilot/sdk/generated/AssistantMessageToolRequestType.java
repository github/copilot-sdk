/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AssistantMessageToolRequestType {
    /** The {@code function} variant. */
    FUNCTION("function"),
    /** The {@code custom} variant. */
    CUSTOM("custom");

    private final String value;
    AssistantMessageToolRequestType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AssistantMessageToolRequestType fromValue(String value) {
        for (AssistantMessageToolRequestType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AssistantMessageToolRequestType value: " + value);
    }
}
