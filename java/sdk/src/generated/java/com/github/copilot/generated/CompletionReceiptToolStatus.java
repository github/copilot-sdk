/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Structured terminal status from a tool completion event.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CompletionReceiptToolStatus {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code failure} variant. */
    FAILURE("failure"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout"),
    /** The {@code rejected} variant. */
    REJECTED("rejected"),
    /** The {@code denied} variant. */
    DENIED("denied");

    private final String value;
    CompletionReceiptToolStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CompletionReceiptToolStatus fromValue(String value) {
        for (CompletionReceiptToolStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CompletionReceiptToolStatus value: " + value);
    }
}
