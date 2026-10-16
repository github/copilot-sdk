/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Category for an MCP diagnostic record.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpDiagnosticKind {
    /** The {@code lifecycle} variant. */
    LIFECYCLE("lifecycle"),
    /** The {@code protocol} variant. */
    PROTOCOL("protocol"),
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code stderr} variant. */
    STDERR("stderr");

    private final String value;
    McpDiagnosticKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpDiagnosticKind fromValue(String value) {
        for (McpDiagnosticKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpDiagnosticKind value: " + value);
    }
}
