/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Canonical availability treatment for an account row.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AccountHealth {
    /** The {@code available} variant. */
    AVAILABLE("available"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable"),
    /** The {@code warning} variant. */
    WARNING("warning"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    AccountHealth(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AccountHealth fromValue(String value) {
        for (AccountHealth v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AccountHealth value: " + value);
    }
}
