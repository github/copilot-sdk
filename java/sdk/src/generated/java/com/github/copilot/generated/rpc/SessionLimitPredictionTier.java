/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Semantic usage tier used for a recommended cap or additional headroom.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionLimitPredictionTier {
    /** The {@code recommended} variant. */
    RECOMMENDED("recommended"),
    /** The {@code additional_headroom} variant. */
    ADDITIONAL_HEADROOM("additional_headroom"),
    /** The {@code generous_headroom} variant. */
    GENEROUS_HEADROOM("generous_headroom"),
    /** The {@code maximum_headroom} variant. */
    MAXIMUM_HEADROOM("maximum_headroom");

    private final String value;
    SessionLimitPredictionTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionLimitPredictionTier fromValue(String value) {
        for (SessionLimitPredictionTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionLimitPredictionTier value: " + value);
    }
}
