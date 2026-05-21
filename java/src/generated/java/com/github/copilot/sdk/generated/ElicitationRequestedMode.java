/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ElicitationRequestedMode {
    /** The {@code form} variant. */
    FORM("form"),
    /** The {@code url} variant. */
    URL("url");

    private final String value;
    ElicitationRequestedMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ElicitationRequestedMode fromValue(String value) {
        for (ElicitationRequestedMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ElicitationRequestedMode value: " + value);
    }
}
