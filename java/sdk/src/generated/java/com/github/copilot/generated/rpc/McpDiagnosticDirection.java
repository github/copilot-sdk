/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Direction of an observed MCP protocol frame.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpDiagnosticDirection {
    /** The {@code client-to-server} variant. */
    CLIENT_TO_SERVER("client-to-server"),
    /** The {@code server-to-client} variant. */
    SERVER_TO_CLIENT("server-to-client");

    private final String value;
    McpDiagnosticDirection(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpDiagnosticDirection fromValue(String value) {
        for (McpDiagnosticDirection v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpDiagnosticDirection value: " + value);
    }
}
