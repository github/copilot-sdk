/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.ArrayList;
import java.util.Collections;
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
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeSessionConfig {

    private String model;
    private List<ToolDefinition> tools;
    private SystemMessageConfig systemMessage;
    private List<String> availableTools;
    private List<String> excludedTools;
    private ProviderConfig provider;
    private String reasoningEffort;
    private PermissionHandler onPermissionRequest;
    private UserInputHandler onUserInputRequest;
    private SessionHooks hooks;
    private String workingDirectory;
    private String configDir;
    private boolean disableResume;
    private boolean streaming;
    private Map<String, Object> mcpServers;
    private List<CustomAgentConfig> customAgents;
    private List<String> skillDirectories;
    private List<String> disabledSkills;
    private InfiniteSessionConfig infiniteSessions;

    /**
     * Gets the AI model to use.
     *
     * @return the model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the AI model to use for the resumed session.
     * <p>
     * Can change the model when resuming an existing session.
     *
     * @param model
     *            the model name
     * @return this config for method chaining
     */
    public ResumeSessionConfig setModel(String model) {
        this.model = model;
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
     *
     * @param systemMessage
     *            the system message configuration
     * @return this config for method chaining
     * @see SystemMessageConfig
     */
    public ResumeSessionConfig setSystemMessage(SystemMessageConfig systemMessage) {
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
     * Takes precedence over excluded tools.
     *
     * @param availableTools
     *            the list of allowed tool names
     * @return this config for method chaining
     */
    public ResumeSessionConfig setAvailableTools(List<String> availableTools) {
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
     * Tools in this list will not be available to the assistant. Ignored if
     * available tools is specified.
     *
     * @param excludedTools
     *            the list of tool names to exclude
     * @return this config for method chaining
     */
    public ResumeSessionConfig setExcludedTools(List<String> excludedTools) {
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
     * Valid values: "low", "medium", "high", "xhigh".
     *
     * @param reasoningEffort
     *            the reasoning effort level
     * @return this config for method chaining
     */
    public ResumeSessionConfig setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
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
     * Gets the user input request handler.
     *
     * @return the user input handler
     */
    public UserInputHandler getOnUserInputRequest() {
        return onUserInputRequest;
    }

    /**
     * Sets a handler for user input requests from the agent.
     *
     * @param onUserInputRequest
     *            the user input handler
     * @return this config for method chaining
     * @see UserInputHandler
     */
    public ResumeSessionConfig setOnUserInputRequest(UserInputHandler onUserInputRequest) {
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
     *
     * @param hooks
     *            the hooks configuration
     * @return this config for method chaining
     * @see SessionHooks
     */
    public ResumeSessionConfig setHooks(SessionHooks hooks) {
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
     * @return this config for method chaining
     */
    public ResumeSessionConfig setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    /**
     * Gets the configuration directory path.
     *
     * @return the configuration directory path
     */
    public String getConfigDir() {
        return configDir;
    }

    /**
     * Sets the configuration directory path.
     * <p>
     * Override the default configuration directory location.
     *
     * @param configDir
     *            the configuration directory path
     * @return this config for method chaining
     */
    public ResumeSessionConfig setConfigDir(String configDir) {
        this.configDir = configDir;
        return this;
    }

    /**
     * Returns whether the resume event is disabled.
     *
     * @return {@code true} if the session.resume event is suppressed
     */
    public boolean isDisableResume() {
        return disableResume;
    }

    /**
     * Sets whether to disable the session.resume event.
     * <p>
     * When true, the session.resume event is not emitted.
     *
     * @param disableResume
     *            {@code true} to suppress the resume event
     * @return this config for method chaining
     */
    public ResumeSessionConfig setDisableResume(boolean disableResume) {
        this.disableResume = disableResume;
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
        return mcpServers == null ? null : Collections.unmodifiableMap(mcpServers);
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
        return customAgents == null ? null : Collections.unmodifiableList(customAgents);
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

    /**
     * Gets the skill directories.
     *
     * @return the list of skill directory paths
     */
    public List<String> getSkillDirectories() {
        return skillDirectories == null ? null : Collections.unmodifiableList(skillDirectories);
    }

    /**
     * Sets directories containing skill definitions.
     *
     * @param skillDirectories
     *            the list of skill directory paths
     * @return this config for method chaining
     */
    public ResumeSessionConfig setSkillDirectories(List<String> skillDirectories) {
        this.skillDirectories = skillDirectories;
        return this;
    }

    /**
     * Gets the disabled skills.
     *
     * @return the list of disabled skill names
     */
    public List<String> getDisabledSkills() {
        return disabledSkills == null ? null : Collections.unmodifiableList(disabledSkills);
    }

    /**
     * Sets skills that should be disabled for this session.
     *
     * @param disabledSkills
     *            the list of skill names to disable
     * @return this config for method chaining
     */
    public ResumeSessionConfig setDisabledSkills(List<String> disabledSkills) {
        this.disabledSkills = disabledSkills;
        return this;
    }

    /**
     * Gets the infinite session configuration.
     *
     * @return the infinite session config
     */
    public InfiniteSessionConfig getInfiniteSessions() {
        return infiniteSessions;
    }

    /**
     * Sets the infinite session configuration for persistent workspaces and
     * automatic compaction.
     *
     * @param infiniteSessions
     *            the infinite session configuration
     * @return this config for method chaining
     * @see InfiniteSessionConfig
     */
    public ResumeSessionConfig setInfiniteSessions(InfiniteSessionConfig infiniteSessions) {
        this.infiniteSessions = infiniteSessions;
        return this;
    }

    /**
     * Creates a shallow clone of this {@code ResumeSessionConfig} instance.
     * <p>
     * Mutable collection properties are copied into new collection instances so
     * that modifications to those collections on the clone do not affect the
     * original. Other reference-type properties (like provider configuration,
     * system messages, hooks, infinite session configuration, and handlers) are
     * not deep-cloned; the original and the clone will share those objects.
     *
     * @return a clone of this config instance
     */
    @Override
    public ResumeSessionConfig clone() {
        ResumeSessionConfig copy = new ResumeSessionConfig();
        copy.model = this.model;
        copy.tools = this.tools != null ? new ArrayList<>(this.tools) : null;
        copy.systemMessage = this.systemMessage;
        copy.availableTools = this.availableTools != null ? new ArrayList<>(this.availableTools) : null;
        copy.excludedTools = this.excludedTools != null ? new ArrayList<>(this.excludedTools) : null;
        copy.provider = this.provider;
        copy.reasoningEffort = this.reasoningEffort;
        copy.onPermissionRequest = this.onPermissionRequest;
        copy.onUserInputRequest = this.onUserInputRequest;
        copy.hooks = this.hooks;
        copy.workingDirectory = this.workingDirectory;
        copy.configDir = this.configDir;
        copy.disableResume = this.disableResume;
        copy.streaming = this.streaming;
        copy.mcpServers = this.mcpServers != null ? new java.util.HashMap<>(this.mcpServers) : null;
        copy.customAgents = this.customAgents != null ? new ArrayList<>(this.customAgents) : null;
        copy.skillDirectories = this.skillDirectories != null ? new ArrayList<>(this.skillDirectories) : null;
        copy.disabledSkills = this.disabledSkills != null ? new ArrayList<>(this.disabledSkills) : null;
        copy.infiniteSessions = this.infiniteSessions;
        return copy;
    }
}
