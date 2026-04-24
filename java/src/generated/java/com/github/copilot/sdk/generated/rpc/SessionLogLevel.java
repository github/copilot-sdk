/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionLogLevel {
    /** The {@code info} variant. */
    INFO("info"),
    /** The {@code warning} variant. */
    WARNING("warning"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    SessionLogLevel(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionLogLevel fromValue(String value) {
        for (SessionLogLevel v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionLogLevel value: " + value);
    }
}
