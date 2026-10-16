/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Severity of an emitted diagnostic record.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DiagnosticSeverity {
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
    DiagnosticSeverity(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DiagnosticSeverity fromValue(String value) {
        for (DiagnosticSeverity v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DiagnosticSeverity value: " + value);
    }
}
