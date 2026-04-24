/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DiscoveredMcpServerType {
    /** The {@code stdio} variant. */
    STDIO("stdio"),
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code sse} variant. */
    SSE("sse"),
    /** The {@code memory} variant. */
    MEMORY("memory");

    private final String value;
    DiscoveredMcpServerType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DiscoveredMcpServerType fromValue(String value) {
        for (DiscoveredMcpServerType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DiscoveredMcpServerType value: " + value);
    }
}
