/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Media type naming the plugin format of an Agent Plugin candidate. Values other than `application/vnd.github.copilot-plugin` appear only when the caller requires the agent-plugin-formats capability.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAgentPluginMediaType {
    /** The {@code application/vnd.github.copilot-plugin} variant. */
    APPLICATION_VND_GITHUB_COPILOT_PLUGIN("application/vnd.github.copilot-plugin"),
    /** The {@code application/vnd.anthropic.claude-plugin+json} variant. */
    APPLICATION_VND_ANTHROPIC_CLAUDE_PLUGIN_JSON("application/vnd.anthropic.claude-plugin+json"),
    /** The {@code application/agent-plugin+json} variant. */
    APPLICATION_AGENT_PLUGIN_JSON("application/agent-plugin+json");

    private final String value;
    CatalogAgentPluginMediaType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAgentPluginMediaType fromValue(String value) {
        for (CatalogAgentPluginMediaType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAgentPluginMediaType value: " + value);
    }
}
