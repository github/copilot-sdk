/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Content-Encoding applied to the request body sent on the wire
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallRequestBodyEncoding {
    /** The {@code identity} variant. */
    IDENTITY("identity"),
    /** The {@code gzip} variant. */
    GZIP("gzip"),
    /** The {@code zstd} variant. */
    ZSTD("zstd");

    private final String value;
    ModelCallRequestBodyEncoding(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallRequestBodyEncoding fromValue(String value) {
        for (ModelCallRequestBodyEncoding v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallRequestBodyEncoding value: " + value);
    }
}
