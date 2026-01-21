/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for resuming an existing Copilot session.
 * <p>
 * This class provides options for configuring a resumed session, including tool
 * registration, provider configuration, and streaming. All setter methods
 * return {@code this} for method chaining.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var config = new ResumeSessionConfig().setStreaming(true).setTools(List.of(myTool));
 *
 * var session = client.resumeSession(sessionId, config).get();
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotClient#resumeSession(String,
 *      ResumeSessionConfig)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeSessionConfig {

    private List<ToolDefinition> tools;
    private ProviderConfig provider;
    private PermissionHandler onPermissionRequest;
    private boolean streaming;
    private Map<String, Object> mcpServers;
    private List<CustomAgentConfig> customAgents;

    /**
     * Gets the custom tools for this session.
     *
     * @return the list of tool definitions
     */
    public List<ToolDefinition> getTools() {
        return tools;
    }

    /**
     * Sets custom tools that the assistant can invoke during the session.
     *
     * @param tools
     *            the list of tool definitions
     * @return this config for method chaining
     * @see ToolDefinition
     */
    public ResumeSessionConfig setTools(List<ToolDefinition> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * Gets the custom API provider configuration.
     *
     * @return the provider configuration
     */
    public ProviderConfig getProvider() {
        return provider;
    }

    /**
     * Sets a custom API provider for BYOK scenarios.
     *
     * @param provider
     *            the provider configuration
     * @return this config for method chaining
     * @see ProviderConfig
     */
    public ResumeSessionConfig setProvider(ProviderConfig provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Gets the permission request handler.
     *
     * @return the permission handler
     */
    public PermissionHandler getOnPermissionRequest() {
        return onPermissionRequest;
    }

    /**
     * Sets a handler for permission requests from the assistant.
     *
     * @param onPermissionRequest
     *            the permission handler
     * @return this config for method chaining
     * @see PermissionHandler
     */
    public ResumeSessionConfig setOnPermissionRequest(PermissionHandler onPermissionRequest) {
        this.onPermissionRequest = onPermissionRequest;
        return this;
    }

    /**
     * Returns whether streaming is enabled.
     *
     * @return {@code true} if streaming is enabled
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Sets whether to enable streaming of response chunks.
     *
     * @param streaming
     *            {@code true} to enable streaming
     * @return this config for method chaining
     */
    public ResumeSessionConfig setStreaming(boolean streaming) {
        this.streaming = streaming;
        return this;
    }

    /**
     * Gets the MCP server configurations.
     *
     * @return the MCP servers map
     */
    public Map<String, Object> getMcpServers() {
        return mcpServers;
    }

    /**
     * Sets MCP (Model Context Protocol) server configurations.
     *
     * @param mcpServers
     *            the MCP servers configuration map
     * @return this config for method chaining
     */
    public ResumeSessionConfig setMcpServers(Map<String, Object> mcpServers) {
        this.mcpServers = mcpServers;
        return this;
    }

    /**
     * Gets the custom agent configurations.
     *
     * @return the list of custom agent configurations
     */
    public List<CustomAgentConfig> getCustomAgents() {
        return customAgents;
    }

    /**
     * Sets custom agent configurations.
     *
     * @param customAgents
     *            the list of custom agent configurations
     * @return this config for method chaining
     * @see CustomAgentConfig
     */
    public ResumeSessionConfig setCustomAgents(List<CustomAgentConfig> customAgents) {
        this.customAgents = customAgents;
        return this;
    }
}
