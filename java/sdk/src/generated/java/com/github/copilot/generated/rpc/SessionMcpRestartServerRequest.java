/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Server name and optional replacement configuration for an individual MCP server restart. Omit `config` for a config-free restart-by-name of an already-configured server.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionMcpRestartServerRequest {

    /** Name of the MCP server to restart */
    @JsonProperty("serverName")
    private final String serverName;

    /** Replacement MCP server configuration (stdio process or remote HTTP/SSE). Omit to restart the server with its already-registered configuration (config-free restart-by-name). */
    @JsonProperty("config")
    private Object config;

    /** Exact receipt identity for an explicit owned restart; configuration overrides are refused. */
    @JsonProperty("expectedInstallationId")
    private String expectedInstallationId;

    /**
     * Creates a request with its required inputs.
     *
     * @param serverName Name of the MCP server to restart
     */
    public SessionMcpRestartServerRequest(String serverName) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
    }

    /**
     * Returns the {@code serverName} property.
     *
     * @return Name of the MCP server to restart
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the {@code config} property.
     *
     * @return Replacement MCP server configuration (stdio process or remote HTTP/SSE). Omit to restart the server with its already-registered configuration (config-free restart-by-name).
     */
    public Object getConfig() {
        return config;
    }

    /**
     * Returns the {@code expectedInstallationId} property.
     *
     * @return Exact receipt identity for an explicit owned restart; configuration overrides are refused.
     */
    public String getExpectedInstallationId() {
        return expectedInstallationId;
    }

    /**
     * Sets the {@code config} property.
     *
     * @param value Replacement MCP server configuration (stdio process or remote HTTP/SSE). Omit to restart the server with its already-registered configuration (config-free restart-by-name).
     * @return this request
     */
    public SessionMcpRestartServerRequest setConfig(Object value) {
        this.config = value;
        return this;
    }

    /**
     * Sets the {@code expectedInstallationId} property.
     *
     * @param value Exact receipt identity for an explicit owned restart; configuration overrides are refused.
     * @return this request
     */
    public SessionMcpRestartServerRequest setExpectedInstallationId(String value) {
        this.expectedInstallationId = value;
        return this;
    }
}
