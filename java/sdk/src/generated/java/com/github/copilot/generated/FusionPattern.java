/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Validated HydraFusion execution pattern.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionPattern {
    /** The {@code single} variant. */
    SINGLE("single"),
    /** The {@code cascade} variant. */
    CASCADE("cascade"),
    /** The {@code critique} variant. */
    CRITIQUE("critique");

    private final String value;
    FusionPattern(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionPattern fromValue(String value) {
        for (FusionPattern v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionPattern value: " + value);
    }
}
