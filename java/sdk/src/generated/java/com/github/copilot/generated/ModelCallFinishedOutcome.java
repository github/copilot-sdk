/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Final outcome of one logical model dispatch after response acceptance processing
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallFinishedOutcome {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code error} variant. */
    ERROR("error"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code rejected} variant. */
    REJECTED("rejected");

    private final String value;
    ModelCallFinishedOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallFinishedOutcome fromValue(String value) {
        for (ModelCallFinishedOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallFinishedOutcome value: " + value);
    }
}
