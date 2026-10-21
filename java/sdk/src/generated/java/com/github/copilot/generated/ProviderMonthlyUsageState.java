/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Availability of a service's monthly consumption reading.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderMonthlyUsageState {
    /** The {@code available} variant. */
    AVAILABLE("available"),
    /** The {@code no_policy} variant. */
    NO_POLICY("no_policy"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    ProviderMonthlyUsageState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderMonthlyUsageState fromValue(String value) {
        for (ProviderMonthlyUsageState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderMonthlyUsageState value: " + value);
    }
}
