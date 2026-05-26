/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UIAutoModeSwitchResponse {
    /** The {@code yes} variant. */
    YES("yes"),
    /** The {@code yes_always} variant. */
    YES_ALWAYS("yes_always"),
    /** The {@code no} variant. */
    NO("no");

    private final String value;
    UIAutoModeSwitchResponse(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UIAutoModeSwitchResponse fromValue(String value) {
        for (UIAutoModeSwitchResponse v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UIAutoModeSwitchResponse value: " + value);
    }
}
