/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Entry type
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionFsReaddirWithTypesEntryType {
    /** The {@code file} variant. */
    FILE("file"),
    /** The {@code directory} variant. */
    DIRECTORY("directory");

    private final String value;
    SessionFsReaddirWithTypesEntryType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionFsReaddirWithTypesEntryType fromValue(String value) {
        for (SessionFsReaddirWithTypesEntryType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionFsReaddirWithTypesEntryType value: " + value);
    }
}
