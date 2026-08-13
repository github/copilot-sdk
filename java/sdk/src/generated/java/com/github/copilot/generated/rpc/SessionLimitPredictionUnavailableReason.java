/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Reason a prediction could not be computed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionLimitPredictionUnavailableReason {
    /** The {@code auto_unresolved} variant. */
    AUTO_UNRESOLVED("auto_unresolved"),
    /** The {@code no_model} variant. */
    NO_MODEL("no_model");

    private final String value;
    SessionLimitPredictionUnavailableReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionLimitPredictionUnavailableReason fromValue(String value) {
        for (SessionLimitPredictionUnavailableReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionLimitPredictionUnavailableReason value: " + value);
    }
}
