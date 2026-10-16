/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Acquisition state, independent of the service's business access/capacity verdict.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderQuotaAcquisitionStatus {
    /** The {@code succeeded} variant. */
    SUCCEEDED("succeeded"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    ProviderQuotaAcquisitionStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderQuotaAcquisitionStatus fromValue(String value) {
        for (ProviderQuotaAcquisitionStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderQuotaAcquisitionStatus value: " + value);
    }
}
