/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
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
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionConfig {

    private String sessionId;
    private String model;
    private String reasoningEffort;
    private List<ToolDefinition> tools;
    private SystemMessageConfig systemMessage;
    private List<String> availableTools;
    private List<String> excludedTools;
    private ProviderConfig provider;
    private PermissionHandler onPermissionRequest;
    private UserInputHandler onUserInputRequest;
    private SessionHooks hooks;
    private String workingDirectory;
    private boolean streaming;
    private Map<String, Object> mcpServers;
    private List<CustomAgentConfig> customAgents;
    private InfiniteSessionConfig infiniteSessions;
    private List<String> skillDirectories;
    private List<String> disabledSkills;
    private String configDir;

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
     * Gets the reasoning effort level.
     *
     * @return the reasoning effort level ("low", "medium", "high", or "xhigh")
     */
    public String getReasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Sets the reasoning effort level for models that support it.
     * <p>
     * Valid values: "low", "medium", "high", "xhigh". Only applies to models where
     * {@code capabilities.supports.reasoningEffort} is true.
     *
     * @param reasoningEffort
     *            the reasoning effort level
     * @return this config instance for method chaining
     */
    public SessionConfig setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }

    /**
     * Gets the custom tools for this session.
     *
     * @return the list of tool definitions
     */
    public List<ToolDefinition> getTools() {
        return tools == null ? null : Collections.unmodifiableList(tools);
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
        return availableTools == null ? null : Collections.unmodifiableList(availableTools);
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
        return excludedTools == null ? null : Collections.unmodifiableList(excludedTools);
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
     * Gets the user input request handler.
     *
     * @return the user input handler
     */
    public UserInputHandler getOnUserInputRequest() {
        return onUserInputRequest;
    }

    /**
     * Sets a handler for user input requests from the agent.
     * <p>
     * When provided, enables the ask_user tool for the agent to request user input.
     *
     * @param onUserInputRequest
     *            the user input handler
     * @return this config instance for method chaining
     * @see UserInputHandler
     */
    public SessionConfig setOnUserInputRequest(UserInputHandler onUserInputRequest) {
        this.onUserInputRequest = onUserInputRequest;
        return this;
    }

    /**
     * Gets the hook handlers configuration.
     *
     * @return the session hooks
     */
    public SessionHooks getHooks() {
        return hooks;
    }

    /**
     * Sets hook handlers for session lifecycle events.
     * <p>
     * Hooks allow you to intercept and modify tool execution behavior.
     *
     * @param hooks
     *            the hooks configuration
     * @return this config instance for method chaining
     * @see SessionHooks
     */
    public SessionConfig setHooks(SessionHooks hooks) {
        this.hooks = hooks;
        return this;
    }

    /**
     * Gets the working directory for the session.
     *
     * @return the working directory path
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory for the session.
     *
     * @param workingDirectory
     *            the working directory path
     * @return this config instance for method chaining
     */
    public SessionConfig setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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
        return mcpServers == null ? null : Collections.unmodifiableMap(mcpServers);
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
        return customAgents == null ? null : Collections.unmodifiableList(customAgents);
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

    /**
     * Gets the infinite sessions configuration.
     *
     * @return the infinite sessions config
     */
    public InfiniteSessionConfig getInfiniteSessions() {
        return infiniteSessions;
    }

    /**
     * Sets the infinite session configuration for persistent workspaces and
     * automatic compaction.
     * <p>
     * When enabled (default), sessions automatically manage context limits and
     * persist state to a workspace directory. The workspace contains checkpoints/,
     * plan.md, and files/ subdirectories.
     *
     * @param infiniteSessions
     *            the infinite sessions configuration
     * @return this config instance for method chaining
     * @see InfiniteSessionConfig
     */
    public SessionConfig setInfiniteSessions(InfiniteSessionConfig infiniteSessions) {
        this.infiniteSessions = infiniteSessions;
        return this;
    }

    /**
     * Gets the skill directories.
     *
     * @return the list of skill directory paths
     */
    public List<String> getSkillDirectories() {
        return skillDirectories == null ? null : Collections.unmodifiableList(skillDirectories);
    }

    /**
     * Sets the skill directories for loading custom skills.
     * <p>
     * Skills are loaded from SKILL.md files in subdirectories of the specified
     * directories. Each skill subdirectory should contain a SKILL.md file with YAML
     * frontmatter defining the skill metadata.
     *
     * @param skillDirectories
     *            the list of skill directory paths
     * @return this config instance for method chaining
     */
    public SessionConfig setSkillDirectories(List<String> skillDirectories) {
        this.skillDirectories = skillDirectories;
        return this;
    }

    /**
     * Gets the disabled skill names.
     *
     * @return the list of disabled skill names
     */
    public List<String> getDisabledSkills() {
        return disabledSkills == null ? null : Collections.unmodifiableList(disabledSkills);
    }

    /**
     * Sets the list of skill names to disable.
     * <p>
     * Skills in this list will not be applied to the session, even if they are
     * found in the skill directories.
     *
     * @param disabledSkills
     *            the list of skill names to disable
     * @return this config instance for method chaining
     */
    public SessionConfig setDisabledSkills(List<String> disabledSkills) {
        this.disabledSkills = disabledSkills;
        return this;
    }

    /**
     * Gets the custom configuration directory.
     *
     * @return the config directory path
     */
    public String getConfigDir() {
        return configDir;
    }

    /**
     * Sets a custom configuration directory for the session.
     * <p>
     * This allows using a specific directory for session configuration instead of
     * the default location.
     *
     * @param configDir
     *            the configuration directory path
     * @return this config instance for method chaining
     */
    public SessionConfig setConfigDir(String configDir) {
        this.configDir = configDir;
        return this;
    }
}
