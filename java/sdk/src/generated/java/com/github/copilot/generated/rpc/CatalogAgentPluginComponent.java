/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * A kind of component an Agent Plugin contributes
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAgentPluginComponent {
    /** The {@code agent} variant. */
    AGENT("agent"),
    /** The {@code command} variant. */
    COMMAND("command"),
    /** The {@code skill} variant. */
    SKILL("skill"),
    /** The {@code hook} variant. */
    HOOK("hook"),
    /** The {@code mcp-server} variant. */
    MCP_SERVER("mcp-server"),
    /** The {@code lsp-server} variant. */
    LSP_SERVER("lsp-server"),
    /** The {@code extension} variant. */
    EXTENSION("extension"),
    /** The {@code canvas} variant. */
    CANVAS("canvas");

    private final String value;
    CatalogAgentPluginComponent(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAgentPluginComponent fromValue(String value) {
        for (CatalogAgentPluginComponent v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAgentPluginComponent value: " + value);
    }
}
