/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Bounded product-state failure classified before telemetry projection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallProductStateError {
    /** The {@code input_item_connection_mismatch} variant. */
    INPUT_ITEM_CONNECTION_MISMATCH("input_item_connection_mismatch");

    private final String value;
    ModelCallProductStateError(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallProductStateError fromValue(String value) {
        for (ModelCallProductStateError v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallProductStateError value: " + value);
    }
}
