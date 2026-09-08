/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * A wire feature a caller can require of the catalog surface, negotiated per request. A grant means the runtime understands the feature's contract, not that the deployment has enabled the operation; typed unavailable results report availability separately.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogCapability {
    /** The {@code mcp-server-card} variant. */
    MCP_SERVER_CARD("mcp-server-card"),
    /** The {@code legacy-mcp-server-card} variant. */
    LEGACY_MCP_SERVER_CARD("legacy-mcp-server-card"),
    /** The {@code ai-skill-discovery} variant. */
    AI_SKILL_DISCOVERY("ai-skill-discovery"),
    /** The {@code mcp-install-planning} variant. */
    MCP_INSTALL_PLANNING("mcp-install-planning"),
    /** The {@code multiple-transport-choice} variant. */
    MULTIPLE_TRANSPORT_CHOICE("multiple-transport-choice");

    private final String value;
    CatalogCapability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogCapability fromValue(String value) {
        for (CatalogCapability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogCapability value: " + value);
    }
}
