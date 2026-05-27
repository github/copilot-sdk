/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum OptionsUpdateToolFilterPrecedence {
    /** The {@code available} variant. */
    AVAILABLE("available"),
    /** The {@code excluded} variant. */
    EXCLUDED("excluded");

    private final String value;
    OptionsUpdateToolFilterPrecedence(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static OptionsUpdateToolFilterPrecedence fromValue(String value) {
        for (OptionsUpdateToolFilterPrecedence v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown OptionsUpdateToolFilterPrecedence value: " + value);
    }
}
