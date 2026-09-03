/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * How a durable phase checkpoint contributes its exact message to canonical root history.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionProjectionMode {
    /** The {@code append} variant. */
    APPEND("append"),
    /** The {@code staged} variant. */
    STAGED("staged"),
    /** The {@code none} variant. */
    NONE("none");

    private final String value;
    FusionProjectionMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionProjectionMode fromValue(String value) {
        for (FusionProjectionMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionProjectionMode value: " + value);
    }
}
