/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Baseline fallback level used to create the prediction.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionLimitPredictionSource {
    /** The {@code model} variant. */
    MODEL("model"),
    /** The {@code family} variant. */
    FAMILY("family"),
    /** The {@code global} variant. */
    GLOBAL("global");

    private final String value;
    SessionLimitPredictionSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionLimitPredictionSource fromValue(String value) {
        for (SessionLimitPredictionSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionLimitPredictionSource value: " + value);
    }
}
