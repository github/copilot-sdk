/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract base class for MCP (Model Context Protocol) server configurations.
 * <p>
 * Use one of the concrete subclasses to configure MCP servers:
 * <ul>
 * <li>{@link McpStdioServerConfig} — for local/stdio-based MCP servers</li>
 * <li>{@link McpHttpServerConfig} — for remote HTTP/SSE-based MCP servers</li>
 * </ul>
 *
 * @see McpStdioServerConfig
 * @see McpHttpServerConfig
 * @see SessionConfig#setMcpServers(java.util.Map)
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = McpStdioServerConfig.class)
@JsonSubTypes({@JsonSubTypes.Type(value = McpStdioServerConfig.class, name = "stdio"),
        @JsonSubTypes.Type(value = McpStdioServerConfig.class, name = "local"),
        @JsonSubTypes.Type(value = McpHttpServerConfig.class, name = "http"),
        @JsonSubTypes.Type(value = McpHttpServerConfig.class, name = "sse")})
public abstract class McpServerConfig {

    @JsonProperty("tools")
    private List<String> tools;

    @JsonProperty("timeout")
    private Integer timeout;

    /**
     * Gets the list of tools to include from this server.
     * <p>
     * An empty list means none; use {@code "*"} to include all tools.
     *
     * @return the list of tool names, or {@code null} if not set
     */
    public List<String> getTools() {
        return tools == null ? null : Collections.unmodifiableList(tools);
    }

    /**
     * Sets the list of tools to include from this server.
     * <p>
     * An empty list means none; use {@code "*"} to include all tools.
     *
     * @param tools
     *            the list of tool names, or {@code null}
     * @return this config for method chaining
     */
    public McpServerConfig setTools(List<String> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * Gets the optional timeout in milliseconds for tool calls to this server.
     *
     * @return the timeout in milliseconds, or {@code null} for the default
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Sets an optional timeout in milliseconds for tool calls to this server.
     *
     * @param timeout
     *            the timeout in milliseconds, or {@code null} for the default
     * @return this config for method chaining
     */
    public McpServerConfig setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }
}
