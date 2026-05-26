/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkspacesWorkspaceDetailsHostType {
    /** The {@code github} variant. */
    GITHUB("github"),
    /** The {@code ado} variant. */
    ADO("ado");

    private final String value;
    WorkspacesWorkspaceDetailsHostType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkspacesWorkspaceDetailsHostType fromValue(String value) {
        for (WorkspacesWorkspaceDetailsHostType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkspacesWorkspaceDetailsHostType value: " + value);
    }
}
