/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * MCP server, tool name, and arguments to invoke from an MCP App view.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionMcpAppsCallToolRequest {

    /** MCP server hosting the tool */
    @JsonProperty("serverName")
    private final String serverName;

    /** MCP tool name */
    @JsonProperty("toolName")
    private final String toolName;

    /** **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing. */
    @JsonProperty("originServerName")
    private final String originServerName;

    /** Tool arguments */
    @JsonProperty("arguments")
    private Map<String, Object> arguments;

    /** MCP tools/call request metadata, forwarded separately from tool arguments. Host-owned progress tokens and protocol metadata take precedence. */
    @JsonProperty("_meta")
    private Map<String, Object> meta;

    /**
     * Creates a request with its required inputs.
     *
     * @param serverName MCP server hosting the tool
     * @param toolName MCP tool name
     * @param originServerName **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
     */
    public SessionMcpAppsCallToolRequest(String serverName, String toolName, String originServerName) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.toolName = Objects.requireNonNull(toolName, "toolName");
        this.originServerName = Objects.requireNonNull(originServerName, "originServerName");
    }

    /**
     * Returns the {@code serverName} property.
     *
     * @return MCP server hosting the tool
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the {@code toolName} property.
     *
     * @return MCP tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns the {@code originServerName} property.
     *
     * @return **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
     */
    public String getOriginServerName() {
        return originServerName;
    }

    /**
     * Returns the {@code arguments} property.
     *
     * @return Tool arguments
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Returns the {@code _meta} property.
     *
     * @return MCP tools/call request metadata, forwarded separately from tool arguments. Host-owned progress tokens and protocol metadata take precedence.
     */
    public Map<String, Object> getMeta() {
        return meta;
    }

    /**
     * Sets the {@code arguments} property.
     *
     * @param value Tool arguments
     * @return this request
     */
    public SessionMcpAppsCallToolRequest setArguments(Map<String, Object> value) {
        this.arguments = value;
        return this;
    }

    /**
     * Sets the {@code _meta} property.
     *
     * @param value MCP tools/call request metadata, forwarded separately from tool arguments. Host-owned progress tokens and protocol metadata take precedence.
     * @return this request
     */
    public SessionMcpAppsCallToolRequest setMeta(Map<String, Object> value) {
        this.meta = value;
        return this;
    }
}
