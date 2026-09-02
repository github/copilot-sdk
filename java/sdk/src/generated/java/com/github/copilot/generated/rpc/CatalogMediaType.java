/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Media type a catalog card is interpreted as
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogMediaType {
    /** The {@code application/mcp-server-card+json} variant. */
    APPLICATION_MCP_SERVER_CARD_JSON("application/mcp-server-card+json"),
    /** The {@code application/mcp-server+json} variant. */
    APPLICATION_MCP_SERVER_JSON("application/mcp-server+json"),
    /** The {@code application/ai-skill} variant. */
    APPLICATION_AI_SKILL("application/ai-skill");

    private final String value;
    CatalogMediaType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogMediaType fromValue(String value) {
        for (CatalogMediaType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogMediaType value: " + value);
    }
}
