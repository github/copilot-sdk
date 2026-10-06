/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Effective MCP configuration entry. Configuration enablement is distinct from the optional live observation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpConfiguredServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Whether this configured server is enabled after session configuration and policy filtering. */
    @JsonProperty("enabled") Boolean enabled,
    /** Configuration provenance: user, workspace, plugin, builtin, or managed. */
    @JsonProperty("source") McpServerSource source,
    /** Plugin name that provided this server, when source is plugin. */
    @JsonProperty("sourcePlugin") String sourcePlugin,
    /** Plugin version that provided this server, when source is plugin. */
    @JsonProperty("sourcePluginVersion") String sourcePluginVersion,
    /** Human-readable display name supplied by configuration. */
    @JsonProperty("displayName") String displayName,
    /** Observed state from an already materialized matching server. Omitted when no live graph has this configured server; it never determines configuration enablement. */
    @JsonProperty("live") McpConfiguredServerState live
) {
}
