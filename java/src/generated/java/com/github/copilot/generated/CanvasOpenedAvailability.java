/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Runtime-controlled routing state for the instance. "ready" when the provider connection is live; "stale" when the provider has gone away and the instance is awaiting rebinding.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CanvasOpenedAvailability {
    /** The {@code ready} variant. */
    READY("ready"),
    /** The {@code stale} variant. */
    STALE("stale");

    private final String value;
    CanvasOpenedAvailability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CanvasOpenedAvailability fromValue(String value) {
        for (CanvasOpenedAvailability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CanvasOpenedAvailability value: " + value);
    }
}
