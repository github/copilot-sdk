/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Timing and purpose of a provider quota observation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderQuotaObservationKind {
    /** The {@code account_snapshot} variant. */
    ACCOUNT_SNAPSHOT("account_snapshot"),
    /** The {@code admission_state} variant. */
    ADMISSION_STATE("admission_state");

    private final String value;
    ProviderQuotaObservationKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderQuotaObservationKind fromValue(String value) {
        for (ProviderQuotaObservationKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderQuotaObservationKind value: " + value);
    }
}
