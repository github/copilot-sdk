/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Hosting platform type of the repository (github or ado)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkingDirectoryContextHostType {
    /** The {@code github} variant. */
    GITHUB("github"),
    /** The {@code ado} variant. */
    ADO("ado");

    private final String value;
    WorkingDirectoryContextHostType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkingDirectoryContextHostType fromValue(String value) {
        for (WorkingDirectoryContextHostType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkingDirectoryContextHostType value: " + value);
    }
}
