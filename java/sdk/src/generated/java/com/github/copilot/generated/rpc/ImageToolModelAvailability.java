/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Account availability of the runtime image model. Discovery errors are returned as errors, not converted to unavailable.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ImageToolModelAvailability {
    /** The {@code available} variant. */
    AVAILABLE("available"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable");

    private final String value;
    ImageToolModelAvailability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ImageToolModelAvailability fromValue(String value) {
        for (ImageToolModelAvailability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ImageToolModelAvailability value: " + value);
    }
}
