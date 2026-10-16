/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Session-scoped diagnostic threshold. Capture is disabled by default and is never persisted with the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DiagnosticLogLevel {
    /** The {@code off} variant. */
    OFF("off"),
    /** The {@code error} variant. */
    ERROR("error"),
    /** The {@code warning} variant. */
    WARNING("warning"),
    /** The {@code info} variant. */
    INFO("info"),
    /** The {@code debug} variant. */
    DEBUG("debug"),
    /** The {@code trace} variant. */
    TRACE("trace");

    private final String value;
    DiagnosticLogLevel(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DiagnosticLogLevel fromValue(String value) {
        for (DiagnosticLogLevel v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DiagnosticLogLevel value: " + value);
    }
}
