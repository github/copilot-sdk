/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Final bounded result of one logical model operation after its internal retry loop settles
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallFinalResult {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code http_400} variant. */
    HTTP_400("http_400"),
    /** The {@code http_413} variant. */
    HTTP_413("http_413"),
    /** The {@code http_429} variant. */
    HTTP_429("http_429"),
    /** The {@code http_4xx} variant. */
    HTTP_4XX("http_4xx"),
    /** The {@code http_5xx} variant. */
    HTTP_5XX("http_5xx"),
    /** The {@code transport_error} variant. */
    TRANSPORT_ERROR("transport_error"),
    /** The {@code other_error} variant. */
    OTHER_ERROR("other_error");

    private final String value;
    ModelCallFinalResult(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallFinalResult fromValue(String value) {
        for (ModelCallFinalResult v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallFinalResult value: " + value);
    }
}
