/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * The user's auto-mode-switch choice
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutoModeSwitchResponse {
    /** The {@code yes} variant. */
    YES("yes"),
    /** The {@code yes_always} variant. */
    YES_ALWAYS("yes_always"),
    /** The {@code no} variant. */
    NO("no");

    private final String value;
    AutoModeSwitchResponse(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutoModeSwitchResponse fromValue(String value) {
        for (AutoModeSwitchResponse v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutoModeSwitchResponse value: " + value);
    }
}
