/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Scalar type a required value must conform to
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpPlanScalarValueType {
    /** The {@code string} variant. */
    STRING("string"),
    /** The {@code number} variant. */
    NUMBER("number"),
    /** The {@code boolean} variant. */
    BOOLEAN("boolean"),
    /** The {@code path} variant. */
    PATH("path");

    private final String value;
    McpPlanScalarValueType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpPlanScalarValueType fromValue(String value) {
        for (McpPlanScalarValueType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpPlanScalarValueType value: " + value);
    }
}
