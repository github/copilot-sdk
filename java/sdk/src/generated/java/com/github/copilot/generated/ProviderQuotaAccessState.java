/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Access policy reported by the quota service.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderQuotaAccessState {
    /** The {@code allowed} variant. */
    ALLOWED("allowed"),
    /** The {@code denied} variant. */
    DENIED("denied"),
    /** The {@code not_required} variant. */
    NOT_REQUIRED("not_required"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable");

    private final String value;
    ProviderQuotaAccessState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderQuotaAccessState fromValue(String value) {
        for (ProviderQuotaAccessState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderQuotaAccessState value: " + value);
    }
}
