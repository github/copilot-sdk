/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * How the runtime fell back to an uncompressed request body after a compressed body was rejected
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallRequestBodyCompressionFallback {
    /** The {@code retried} variant. */
    RETRIED("retried"),
    /** The {@code disabled} variant. */
    DISABLED("disabled");

    private final String value;
    ModelCallRequestBodyCompressionFallback(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallRequestBodyCompressionFallback fromValue(String value) {
        for (ModelCallRequestBodyCompressionFallback v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallRequestBodyCompressionFallback value: " + value);
    }
}
