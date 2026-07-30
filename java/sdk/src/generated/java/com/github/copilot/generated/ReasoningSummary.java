/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ReasoningSummary {
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code concise} variant. */
    CONCISE("concise"),
    /** The {@code detailed} variant. */
    DETAILED("detailed");

    private final String value;
    ReasoningSummary(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ReasoningSummary fromValue(String value) {
        for (ReasoningSummary v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ReasoningSummary value: " + value);
    }
}
