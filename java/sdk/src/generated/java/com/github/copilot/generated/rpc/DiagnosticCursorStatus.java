/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether the supplied diagnostic cursor remained within the retained buffer window.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DiagnosticCursorStatus {
    /** The {@code ok} variant. */
    OK("ok"),
    /** The {@code expired} variant. */
    EXPIRED("expired");

    private final String value;
    DiagnosticCursorStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DiagnosticCursorStatus fromValue(String value) {
        for (DiagnosticCursorStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DiagnosticCursorStatus value: " + value);
    }
}
