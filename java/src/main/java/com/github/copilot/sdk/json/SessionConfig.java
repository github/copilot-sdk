/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration for creating a new Copilot session.
 * <p>
 * This class provides options for customizing session behavior, including model
 * selection, tool registration, system message customization, and more. All
 * setter methods return {@code this} for method chaining.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var config = new SessionConfig().setModel("gpt-5").setStreaming(true).setSystemMessage(
 * 		new SystemMessageConfig().setMode(SystemMessageMode.APPEND).setContent("Be concise in your responses."));
 *
 * var session = client.createSession(config).get();
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotClient#createSession(SessionConfig)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionConfig {

    private String sessionId;
    private String model;
    private List<ToolDefinition> tools;
    private SystemMessageConfig systemMessage;
    private List<String> availableTools;
    private List<String> excludedTools;
    private ProviderConfig provider;
    private PermissionHandler onPermissionRequest;
    private boolean streaming;
    private Map<String, Object> mcpServers;
    private List<CustomAgentConfig> customAgents;

    /**
     * Gets the custom session ID.
     *
     * @return the session ID, or {@code null} to generate automatically
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets a custom session ID.
     * <p>
     * If not provided, a unique session ID will be generated automatically.
     *
     * @param sessionId
     *            the custom session ID
     * @return this config instance for method chaining
     */
    public SessionConfig setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets the AI model to use.
     *
     * @return the model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the AI model to use for this session.
     * <p>
     * Examples: "gpt-5", "claude-sonnet-4.5", "o3-mini".
     *
     * @param model
     *            the model name
     * @return this config instance for method chaining
     */
    public SessionConfig setModel(String model) {
        this.model = model;
        return this;
    }

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
     * <p>
     * Tools allow the assistant to call back into your application to perform
     * actions or retrieve information.
     *
     * @param tools
     *            the list of tool definitions
     * @return this config instance for method chaining
     * @see ToolDefinition
     */
    public SessionConfig setTools(List<ToolDefinition> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * Gets the system message configuration.
     *
     * @return the system message config
     */
    public SystemMessageConfig getSystemMessage() {
        return systemMessage;
    }

    /**
     * Sets the system message configuration.
     * <p>
     * The system message controls the behavior and personality of the assistant.
     * Use {@link com.github.copilot.sdk.SystemMessageMode#APPEND} to add
     * instructions while preserving default behavior, or
     * {@link com.github.copilot.sdk.SystemMessageMode#REPLACE} to fully customize.
     *
     * @param systemMessage
     *            the system message configuration
     * @return this config instance for method chaining
     * @see SystemMessageConfig
     */
    public SessionConfig setSystemMessage(SystemMessageConfig systemMessage) {
        this.systemMessage = systemMessage;
        return this;
    }

    /**
     * Gets the list of allowed tool names.
     *
     * @return the list of available tool names
     */
    public List<String> getAvailableTools() {
        return availableTools;
    }

    /**
     * Sets the list of tool names that are allowed in this session.
     * <p>
     * When specified, only tools in this list will be available to the assistant.
     *
     * @param availableTools
     *            the list of allowed tool names
     * @return this config instance for method chaining
     */
    public SessionConfig setAvailableTools(List<String> availableTools) {
        this.availableTools = availableTools;
        return this;
    }

    /**
     * Gets the list of excluded tool names.
     *
     * @return the list of excluded tool names
     */
    public List<String> getExcludedTools() {
        return excludedTools;
    }

    /**
     * Sets the list of tool names to exclude from this session.
     * <p>
     * Tools in this list will not be available to the assistant.
     *
     * @param excludedTools
     *            the list of tool names to exclude
     * @return this config instance for method chaining
     */
    public SessionConfig setExcludedTools(List<String> excludedTools) {
        this.excludedTools = excludedTools;
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
     * Sets a custom API provider for BYOK (Bring Your Own Key) scenarios.
     * <p>
     * This allows using your own OpenAI, Azure OpenAI, or other compatible API
     * endpoints instead of the default Copilot backend.
     *
     * @param provider
     *            the provider configuration
     * @return this config instance for method chaining
     * @see ProviderConfig
     */
    public SessionConfig setProvider(ProviderConfig provider) {
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
     * <p>
     * When the assistant needs permission to perform certain actions, this handler
     * will be invoked to approve or deny the request.
     *
     * @param onPermissionRequest
     *            the permission handler
     * @return this config instance for method chaining
     * @see PermissionHandler
     */
    public SessionConfig setOnPermissionRequest(PermissionHandler onPermissionRequest) {
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
     * <p>
     * When enabled, the session will emit {@code AssistantMessageDeltaEvent} events
     * as the response is generated, allowing for real-time display of partial
     * responses.
     *
     * @param streaming
     *            {@code true} to enable streaming
     * @return this config instance for method chaining
     */
    public SessionConfig setStreaming(boolean streaming) {
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
     * <p>
     * MCP servers extend the assistant's capabilities by providing additional
     * context sources and tools.
     *
     * @param mcpServers
     *            the MCP servers configuration map
     * @return this config instance for method chaining
     */
    public SessionConfig setMcpServers(Map<String, Object> mcpServers) {
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
     * <p>
     * Custom agents allow extending the assistant with specialized behaviors and
     * capabilities.
     *
     * @param customAgents
     *            the list of custom agent configurations
     * @return this config instance for method chaining
     * @see CustomAgentConfig
     */
    public SessionConfig setCustomAgents(List<CustomAgentConfig> customAgents) {
        this.customAgents = customAgents;
        return this;
    }
}
