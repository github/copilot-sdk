/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Output verbosity level for supported models
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum Verbosity {
    /** The {@code low} variant. */
    LOW("low"),
    /** The {@code medium} variant. */
    MEDIUM("medium"),
    /** The {@code high} variant. */
    HIGH("high");

    private final String value;
    Verbosity(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static Verbosity fromValue(String value) {
        for (Verbosity v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown Verbosity value: " + value);
    }
}
