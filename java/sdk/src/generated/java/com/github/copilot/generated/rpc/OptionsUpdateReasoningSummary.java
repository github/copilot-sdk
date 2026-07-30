/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Reasoning summary mode for supported model clients.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum OptionsUpdateReasoningSummary {
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code concise} variant. */
    CONCISE("concise"),
    /** The {@code detailed} variant. */
    DETAILED("detailed");

    private final String value;
    OptionsUpdateReasoningSummary(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static OptionsUpdateReasoningSummary fromValue(String value) {
        for (OptionsUpdateReasoningSummary v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown OptionsUpdateReasoningSummary value: " + value);
    }
}
