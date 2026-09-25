/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * MCP server status entry, including config source/plugin source and any connection error.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured */
    @JsonProperty("status") McpServerStatus status,
    /** Configuration source: user, workspace, plugin, builtin, or managed */
    @JsonProperty("source") McpServerSource source,
    /** Plugin name that provided this server, when source is plugin. */
    @JsonProperty("sourcePlugin") String sourcePlugin,
    /** Plugin version that provided this server, when source is plugin. */
    @JsonProperty("sourcePluginVersion") String sourcePluginVersion,
    /** Human-readable display name supplied by a managed server catalog. */
    @JsonProperty("displayName") String displayName,
    /** Error message if the server failed to connect */
    @JsonProperty("error") String error,
    /** Server-advertised metadata for a connected server. Omitted when no live connection metadata is available, including while pending or when failed, disabled, stopped, or not configured. */
    @JsonProperty("serverMetadata") McpServerMetadata serverMetadata,
    /** Owned installation this entry's live configuration came from. Absent for manual, workspace, plugin, builtin and same-name servers, and on runtimes without owned installations. */
    @JsonProperty("owned") McpServerOwnership owned
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param name Server name (config key)
     * @param status Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured
     * @param source Configuration source: user, workspace, plugin, builtin, or managed
     * @param sourcePlugin Plugin name that provided this server, when source is plugin.
     * @param sourcePluginVersion Plugin version that provided this server, when source is plugin.
     * @param displayName Human-readable display name supplied by a managed server catalog.
     * @param error Error message if the server failed to connect
     * @param serverMetadata Server-advertised metadata for a connected server. Omitted when no live connection metadata is available, including while pending or when failed, disabled, stopped, or not configured.
     */
    public McpServer(
        String name,
        McpServerStatus status,
        McpServerSource source,
        String sourcePlugin,
        String sourcePluginVersion,
        String displayName,
        String error,
        McpServerMetadata serverMetadata
    ) {
        this(name, status, source, sourcePlugin, sourcePluginVersion, displayName, error, serverMetadata, null);
    }
}
