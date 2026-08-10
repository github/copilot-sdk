/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Client population used for the prediction baseline.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionLimitPredictionClientType {
    /** The {@code cli-interactive} variant. */
    CLI_INTERACTIVE("cli-interactive"),
    /** The {@code cli-prompt} variant. */
    CLI_PROMPT("cli-prompt");

    private final String value;
    SessionLimitPredictionClientType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionLimitPredictionClientType fromValue(String value) {
        for (SessionLimitPredictionClientType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionLimitPredictionClientType value: " + value);
    }
}
