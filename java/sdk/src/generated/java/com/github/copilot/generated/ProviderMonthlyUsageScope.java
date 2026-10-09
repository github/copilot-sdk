/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Identity scope of a monthly usage reading.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderMonthlyUsageScope {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    ProviderMonthlyUsageScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderMonthlyUsageScope fromValue(String value) {
        for (ProviderMonthlyUsageScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderMonthlyUsageScope value: " + value);
    }
}
