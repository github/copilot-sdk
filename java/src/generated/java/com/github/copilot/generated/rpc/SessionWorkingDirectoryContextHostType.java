/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Hosting platform type of the repository
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionWorkingDirectoryContextHostType {
    /** The {@code github} variant. */
    GITHUB("github"),
    /** The {@code ado} variant. */
    ADO("ado");

    private final String value;
    SessionWorkingDirectoryContextHostType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionWorkingDirectoryContextHostType fromValue(String value) {
        for (SessionWorkingDirectoryContextHostType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionWorkingDirectoryContextHostType value: " + value);
    }
}
