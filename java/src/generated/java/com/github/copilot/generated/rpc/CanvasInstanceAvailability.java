/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Runtime-controlled routing state for an open canvas instance.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CanvasInstanceAvailability {
    /** The {@code ready} variant. */
    READY("ready"),
    /** The {@code stale} variant. */
    STALE("stale");

    private final String value;
    CanvasInstanceAvailability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CanvasInstanceAvailability fromValue(String value) {
        for (CanvasInstanceAvailability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CanvasInstanceAvailability value: " + value);
    }
}
