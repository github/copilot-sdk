/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Root JSON Schema type for a built-in tool input.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum BuiltinToolInputSchemaType {
    /** The {@code object} variant. */
    OBJECT("object");

    private final String value;
    BuiltinToolInputSchemaType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static BuiltinToolInputSchemaType fromValue(String value) {
        for (BuiltinToolInputSchemaType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown BuiltinToolInputSchemaType value: " + value);
    }
}
