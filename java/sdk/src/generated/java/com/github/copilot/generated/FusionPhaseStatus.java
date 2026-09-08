/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Durable outcome status of a HydraFusion phase.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionPhaseStatus {
    /** The {@code succeeded} variant. */
    SUCCEEDED("succeeded"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled");

    private final String value;
    FusionPhaseStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionPhaseStatus fromValue(String value) {
        for (FusionPhaseStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionPhaseStatus value: " + value);
    }
}
