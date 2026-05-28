/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.github.copilot.generated.SessionEvent;
import java.util.Optional;

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
 * @see com.github.copilot.CopilotClient#resumeSession(String,
 *      ResumeSessionConfig)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeSessionConfig {

    private String clientName;
    private String model;
    private List<ToolDefinition> tools;
    private SystemMessageConfig systemMessage;
    private List<String> availableTools;
    private List<String> excludedTools;
    private ProviderConfig provider;
    private Boolean enableSessionTelemetry;
    private Boolean skipCustomInstructions;
    private Boolean customAgentsLocalOnly;
    private Boolean coauthorEnabled;
    private Boolean manageScheduleEnabled;
    private String reasoningEffort;
    private String reasoningSummary;
    private ModelCapabilitiesOverride modelCapabilities;
    private PermissionHandler onPermissionRequest;
    private UserInputHandler onUserInputRequest;
    private SessionHooks hooks;
    private String workingDirectory;
    private String configDirectory;
    private Boolean enableConfigDiscovery;
    private boolean disableResume;
    private boolean streaming;
    private Boolean includeSubAgentStreamingEvents;
    private Map<String, McpServerConfig> mcpServers;
    private String mcpOAuthTokenStorage;
    private List<CustomAgentConfig> customAgents;
    private DefaultAgentConfig defaultAgent;
    private String agent;
    private List<String> skillDirectories;
    private List<String> instructionDirectories;
    private List<String> pluginDirectories;
    private LargeToolOutputConfig largeOutput;
    private List<String> disabledSkills;
    private InfiniteSessionConfig infiniteSessions;
    private Consumer<SessionEvent> onEvent;
    private List<CommandDefinition> commands;
    private ElicitationHandler onElicitationRequest;
    private ExitPlanModeHandler onExitPlanMode;
    private AutoModeSwitchHandler onAutoModeSwitch;
    private boolean enableMcpApps;
    private String gitHubToken;
    private String remoteSession;

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
     * Gets the client name used to identify the application using the SDK.
     *
     * @return the client name, or {@code null} if not set
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Sets the client name to identify the application using the SDK.
     * <p>
     * This value is included in the User-Agent header for API requests.
     *
     * @param clientName
     *            the client name
     * @return this config for method chaining
     */
    public ResumeSessionConfig setClientName(String clientName) {
        this.clientName = clientName;
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
     * Enables or disables internal session telemetry for this session. When
     * {@code false}, disables session telemetry. When unset (the default) or
     * {@code true}, telemetry is enabled for GitHub-authenticated sessions. When a
     * custom {@link ProviderConfig} (BYOK) is configured, session telemetry is
     * always disabled regardless of this setting. This is independent of
     * {@link com.github.copilot.rpc.CopilotClientOptions#getTelemetry()
     * CopilotClientOptions.TelemetryConfig}, which configures OpenTelemetry export
     * for observability.
     *
     * @return an {@link java.util.Optional} containing whether session telemetry is
     *         enabled, or {@link java.util.Optional#empty()} for the default
     */
    @JsonIgnore
    public Optional<Boolean> getEnableSessionTelemetry() {
        return Optional.ofNullable(enableSessionTelemetry);
    }

    /**
     * Enables or disables internal session telemetry for this session. When
     * {@code false}, disables session telemetry. When unset (the default) or
     * {@code true}, telemetry is enabled for GitHub-authenticated sessions. When a
     * custom {@link ProviderConfig} (BYOK) is configured, session telemetry is
     * always disabled regardless of this setting.
     *
     * @param enableSessionTelemetry
     *            whether to enable session telemetry
     * @return this config for method chaining
     */
    public ResumeSessionConfig setEnableSessionTelemetry(boolean enableSessionTelemetry) {
        this.enableSessionTelemetry = enableSessionTelemetry;
        return this;
    }

    /**
     * Clears the enableSessionTelemetry setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearEnableSessionTelemetry() {
        this.enableSessionTelemetry = null;
        return this;
    }

    /**
     * Gets whether custom instruction file loading is suppressed.
     *
     * @return {@code true} to suppress, or empty if not explicitly set
     * @since 1.3.0
     */
    @JsonIgnore
    public Optional<Boolean> getSkipCustomInstructions() {
        return Optional.ofNullable(skipCustomInstructions);
    }

    /**
     * Sets whether to suppress loading of custom instruction files.
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session resume. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code true}
     * (skip); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the value
     * is forwarded only when explicitly set.
     *
     * @param skipCustomInstructions
     *            whether to skip custom instructions
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public ResumeSessionConfig setSkipCustomInstructions(boolean skipCustomInstructions) {
        this.skipCustomInstructions = skipCustomInstructions;
        return this;
    }

    /**
     * Clears the skipCustomInstructions setting.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearSkipCustomInstructions() {
        this.skipCustomInstructions = null;
        return this;
    }

    /**
     * Gets whether custom-agent discovery is restricted to local only.
     *
     * @return {@code true} for local only, or empty if not explicitly set
     * @since 1.3.0
     */
    @JsonIgnore
    public Optional<Boolean> getCustomAgentsLocalOnly() {
        return Optional.ofNullable(customAgentsLocalOnly);
    }

    /**
     * Sets whether custom-agent discovery is restricted to the session's local
     * working directory.
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session resume. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code true} (local
     * only); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the value is
     * forwarded only when explicitly set.
     *
     * @param customAgentsLocalOnly
     *            whether to restrict to local agents
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public ResumeSessionConfig setCustomAgentsLocalOnly(boolean customAgentsLocalOnly) {
        this.customAgentsLocalOnly = customAgentsLocalOnly;
        return this;
    }

    /**
     * Clears the customAgentsLocalOnly setting.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearCustomAgentsLocalOnly() {
        this.customAgentsLocalOnly = null;
        return this;
    }

    /**
     * Gets whether the runtime may append a Co-authored-by trailer.
     *
     * @return the coauthor enabled flag, or empty if not explicitly set
     * @since 1.3.0
     */
    @JsonIgnore
    public Optional<Boolean> getCoauthorEnabled() {
        return Optional.ofNullable(coauthorEnabled);
    }

    /**
     * Sets whether the runtime is allowed to append a {@code Co-authored-by}
     * trailer.
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session resume. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code false}
     * (disabled); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the
     * value is forwarded only when explicitly set.
     *
     * @param coauthorEnabled
     *            whether coauthor is enabled
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public ResumeSessionConfig setCoauthorEnabled(boolean coauthorEnabled) {
        this.coauthorEnabled = coauthorEnabled;
        return this;
    }

    /**
     * Clears the coauthorEnabled setting.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearCoauthorEnabled() {
        this.coauthorEnabled = null;
        return this;
    }

    /**
     * Gets whether the manage_schedule tool is enabled.
     *
     * @return the manage schedule flag, or empty if not explicitly set
     * @since 1.3.0
     */
    @JsonIgnore
    public Optional<Boolean> getManageScheduleEnabled() {
        return Optional.ofNullable(manageScheduleEnabled);
    }

    /**
     * Sets whether to enable the {@code manage_schedule} tool.
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session resume. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code false}
     * (disabled); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the
     * value is forwarded only when explicitly set.
     *
     * @param manageScheduleEnabled
     *            whether manage schedule is enabled
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public ResumeSessionConfig setManageScheduleEnabled(boolean manageScheduleEnabled) {
        this.manageScheduleEnabled = manageScheduleEnabled;
        return this;
    }

    /**
     * Clears the manageScheduleEnabled setting.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearManageScheduleEnabled() {
        this.manageScheduleEnabled = null;
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
     * Gets the reasoning summary mode.
     *
     * @return the reasoning summary mode ("none", "concise", or "detailed")
     */
    public String getReasoningSummary() {
        return reasoningSummary;
    }

    /**
     * Sets the reasoning summary mode for models that support configurable
     * reasoning summaries. Use {@code "none"} to suppress summary output regardless
     * of whether reasoning is enabled.
     *
     * @param reasoningSummary
     *            the reasoning summary mode
     * @return this config for method chaining
     */
    public ResumeSessionConfig setReasoningSummary(String reasoningSummary) {
        this.reasoningSummary = reasoningSummary;
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
    public String getConfigDirectory() {
        return configDirectory;
    }

    /**
     * Sets the configuration directory path.
     * <p>
     * Override the default configuration directory location.
     *
     * @param configDirectory
     *            the configuration directory path
     * @return this config for method chaining
     */
    public ResumeSessionConfig setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory;
        return this;
    }

    /**
     * Gets whether automatic configuration discovery is enabled.
     *
     * @return {@code true} to enable discovery, {@code false} to disable, or
     *         {@code null} to use the runtime default
     */
    @JsonIgnore
    public Optional<Boolean> getEnableConfigDiscovery() {
        return Optional.ofNullable(enableConfigDiscovery);
    }

    /**
     * Sets whether to automatically discover MCP server configurations and skill
     * directories from the working directory.
     * <p>
     * When {@code true}, the CLI scans the working directory for {@code .mcp.json},
     * {@code .vscode/mcp.json} and skill directories, and merges them with
     * explicitly provided configurations.
     *
     * @param enableConfigDiscovery
     *            {@code true} to enable discovery, {@code false} to disable, or
     *            {@code null} to use the runtime default
     * @return this config for method chaining
     */
    public ResumeSessionConfig setEnableConfigDiscovery(boolean enableConfigDiscovery) {
        this.enableConfigDiscovery = enableConfigDiscovery;
        return this;
    }

    /**
     * Clears the enableConfigDiscovery setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearEnableConfigDiscovery() {
        this.enableConfigDiscovery = null;
        return this;
    }

    /**
     * Gets whether sub-agent streaming events are included.
     *
     * @return {@code true} to include sub-agent streaming events, {@code false} to
     *         suppress them, or {@code null} to use the runtime default
     */
    @JsonIgnore
    public Optional<Boolean> getIncludeSubAgentStreamingEvents() {
        return Optional.ofNullable(includeSubAgentStreamingEvents);
    }

    /**
     * Sets whether to include sub-agent streaming events in the event stream.
     *
     * @param includeSubAgentStreamingEvents
     *            {@code true} to include streaming events, {@code false} to
     *            suppress
     * @return this config for method chaining
     */
    public ResumeSessionConfig setIncludeSubAgentStreamingEvents(boolean includeSubAgentStreamingEvents) {
        this.includeSubAgentStreamingEvents = includeSubAgentStreamingEvents;
        return this;
    }

    /**
     * Clears the includeSubAgentStreamingEvents setting, reverting to the default
     * behavior.
     *
     * @return this instance for method chaining
     */
    public ResumeSessionConfig clearIncludeSubAgentStreamingEvents() {
        this.includeSubAgentStreamingEvents = null;
        return this;
    }

    /**
     * Gets the model capabilities override.
     *
     * @return the model capabilities override, or {@code null} if not set
     */
    public ModelCapabilitiesOverride getModelCapabilities() {
        return modelCapabilities;
    }

    /**
     * Sets per-property overrides for model capabilities, deep-merged over runtime
     * defaults.
     *
     * @param modelCapabilities
     *            the model capabilities override
     * @return this config for method chaining
     * @see ModelCapabilitiesOverride
     */
    public ResumeSessionConfig setModelCapabilities(ModelCapabilitiesOverride modelCapabilities) {
        this.modelCapabilities = modelCapabilities;
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
    public Map<String, McpServerConfig> getMcpServers() {
        return mcpServers == null ? null : Collections.unmodifiableMap(mcpServers);
    }

    /**
     * Sets MCP (Model Context Protocol) server configurations.
     *
     * @param mcpServers
     *            the MCP servers configuration map
     * @return this config for method chaining
     */
    public ResumeSessionConfig setMcpServers(Map<String, McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
        return this;
    }

    /**
     * Gets the MCP OAuth token storage mode.
     *
     * @return the storage mode, or {@code null} if not set
     */
    public String getMcpOAuthTokenStorage() {
        return mcpOAuthTokenStorage;
    }

    /**
     * Sets the MCP OAuth token storage mode.
     * <p>
     * Controls how MCP OAuth tokens are stored for this session:
     * <ul>
     * <li>{@code "persistent"} — tokens are stored in the OS keychain (shared
     * across sessions)</li>
     * <li>{@code "in-memory"} — tokens are stored in memory and discarded when the
     * session ends</li>
     * </ul>
     * If not set, the SDK defaults to {@code "in-memory"} for safe multitenant
     * behavior.
     *
     * @param mcpOAuthTokenStorage
     *            the storage mode
     * @return this config for method chaining
     */
    public ResumeSessionConfig setMcpOAuthTokenStorage(String mcpOAuthTokenStorage) {
        this.mcpOAuthTokenStorage = mcpOAuthTokenStorage;
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
     * Gets the default agent configuration.
     *
     * @return the default agent configuration, or {@code null} if not set
     */
    public DefaultAgentConfig getDefaultAgent() {
        return defaultAgent;
    }

    /**
     * Sets the default agent configuration.
     * <p>
     * Use {@link DefaultAgentConfig#setExcludedTools(List)} to hide specific tools
     * from the default agent while keeping them available to custom sub-agents.
     *
     * @param defaultAgent
     *            the default agent configuration
     * @return this config for method chaining
     * @see DefaultAgentConfig
     */
    public ResumeSessionConfig setDefaultAgent(DefaultAgentConfig defaultAgent) {
        this.defaultAgent = defaultAgent;
        return this;
    }

    /**
     * Gets the name of the custom agent to activate at session start.
     *
     * @return the agent name, or {@code null} if not set
     */
    public String getAgent() {
        return agent;
    }

    /**
     * Sets the name of the custom agent to activate when the session starts.
     * <p>
     * Must match the name of one of the agents in {@link #setCustomAgents(List)}.
     *
     * @param agent
     *            the agent name to pre-select
     * @return this config for method chaining
     */
    public ResumeSessionConfig setAgent(String agent) {
        this.agent = agent;
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
     * Gets the additional directories to search for custom instruction files.
     *
     * @return the list of instruction directory paths
     */
    public List<String> getInstructionDirectories() {
        return instructionDirectories == null ? null : Collections.unmodifiableList(instructionDirectories);
    }

    /**
     * Sets additional directories to search for custom instruction files.
     *
     * @param instructionDirectories
     *            the list of instruction directory paths
     * @return this config for method chaining
     */
    public ResumeSessionConfig setInstructionDirectories(List<String> instructionDirectories) {
        this.instructionDirectories = instructionDirectories;
        return this;
    }

    /**
     * Gets the plugin directories to load Open Plugin definitions from.
     *
     * @return the list of plugin directory paths
     */
    public List<String> getPluginDirectories() {
        return pluginDirectories == null ? null : Collections.unmodifiableList(pluginDirectories);
    }

    /**
     * Sets the plugin directories to load Open Plugin definitions from.
     *
     * @param pluginDirectories
     *            the list of plugin directory paths
     * @return this config for method chaining
     */
    public ResumeSessionConfig setPluginDirectories(List<String> pluginDirectories) {
        this.pluginDirectories = pluginDirectories;
        return this;
    }

    /**
     * Gets the configuration for large tool output handling.
     *
     * @return the large output config, or {@code null} for default
     */
    public LargeToolOutputConfig getLargeOutput() {
        return largeOutput;
    }

    /**
     * Sets the configuration for large tool output handling.
     *
     * @param largeOutput
     *            the large output config
     * @return this config for method chaining
     */
    public ResumeSessionConfig setLargeOutput(LargeToolOutputConfig largeOutput) {
        this.largeOutput = largeOutput;
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
     * Gets the event handler registered before the session.resume RPC is issued.
     *
     * @return the event handler, or {@code null} if not set
     */
    public Consumer<SessionEvent> getOnEvent() {
        return onEvent;
    }

    /**
     * Sets an event handler that is registered on the session before the
     * {@code session.resume} RPC is issued.
     * <p>
     * Equivalent to calling {@link com.github.copilot.CopilotSession#on(Consumer)}
     * immediately after resumption, but executes earlier in the lifecycle so no
     * events are missed.
     *
     * @param onEvent
     *            the event handler to register before session resumption
     * @return this config for method chaining
     */
    public ResumeSessionConfig setOnEvent(Consumer<SessionEvent> onEvent) {
        this.onEvent = onEvent;
        return this;
    }

    /**
     * Gets the slash commands registered for this session.
     *
     * @return the list of command definitions, or {@code null}
     */
    public List<CommandDefinition> getCommands() {
        return commands == null ? null : Collections.unmodifiableList(commands);
    }

    /**
     * Sets slash commands registered for this session.
     * <p>
     * When the CLI has a TUI, each command appears as {@code /name} for the user to
     * invoke. The handler is called when the user executes the command.
     *
     * @param commands
     *            the list of command definitions
     * @return this config for method chaining
     * @see CommandDefinition
     */
    public ResumeSessionConfig setCommands(List<CommandDefinition> commands) {
        this.commands = commands;
        return this;
    }

    /**
     * Gets the elicitation request handler.
     *
     * @return the elicitation handler, or {@code null}
     */
    public ElicitationHandler getOnElicitationRequest() {
        return onElicitationRequest;
    }

    /**
     * Sets a handler for elicitation requests from the server or MCP tools.
     * <p>
     * When provided, the server will route elicitation requests to this handler and
     * report elicitation as a supported capability.
     *
     * @param onElicitationRequest
     *            the elicitation handler
     * @return this config for method chaining
     * @see ElicitationHandler
     */
    public ResumeSessionConfig setOnElicitationRequest(ElicitationHandler onElicitationRequest) {
        this.onElicitationRequest = onElicitationRequest;
        return this;
    }

    /**
     * Returns whether MCP Apps (SEP-1865) UI passthrough is enabled on resume.
     *
     * @return {@code true} if the consumer has opted into MCP Apps, otherwise
     *         {@code false}
     * @see #setEnableMcpApps(boolean)
     */
    public boolean isEnableMcpApps() {
        return enableMcpApps;
    }

    /**
     * Enables MCP Apps (SEP-1865) UI passthrough on the resumed session. See
     * {@link SessionConfig#setEnableMcpApps(boolean)} for full semantics (runtime
     * gate, capability inspection, renderer requirement).
     *
     * @param enableMcpApps
     *            {@code true} to opt into MCP Apps support on resume
     * @return this config for method chaining
     */
    public ResumeSessionConfig setEnableMcpApps(boolean enableMcpApps) {
        this.enableMcpApps = enableMcpApps;
        return this;
    }

    /**
     * Gets the exit-plan-mode request handler.
     *
     * @return the exit-plan-mode handler, or {@code null}
     * @since 1.0.8
     */
    public ExitPlanModeHandler getOnExitPlanMode() {
        return onExitPlanMode;
    }

    /**
     * Sets a handler for exit-plan-mode requests from the server.
     * <p>
     * When provided, the server will route {@code exitPlanMode.request} callbacks
     * to this handler.
     *
     * @param onExitPlanMode
     *            the exit-plan-mode handler
     * @return this config for method chaining
     * @see ExitPlanModeHandler
     * @since 1.0.8
     */
    public ResumeSessionConfig setOnExitPlanMode(ExitPlanModeHandler onExitPlanMode) {
        this.onExitPlanMode = onExitPlanMode;
        return this;
    }

    /**
     * Gets the auto-mode-switch request handler.
     *
     * @return the auto-mode-switch handler, or {@code null}
     * @since 1.0.8
     */
    public AutoModeSwitchHandler getOnAutoModeSwitch() {
        return onAutoModeSwitch;
    }

    /**
     * Sets a handler for auto-mode-switch requests from the server.
     * <p>
     * When provided, the server will route {@code autoModeSwitch.request} callbacks
     * to this handler.
     *
     * @param onAutoModeSwitch
     *            the auto-mode-switch handler
     * @return this config for method chaining
     * @see AutoModeSwitchHandler
     * @since 1.0.8
     */
    public ResumeSessionConfig setOnAutoModeSwitch(AutoModeSwitchHandler onAutoModeSwitch) {
        this.onAutoModeSwitch = onAutoModeSwitch;
        return this;
    }

    /**
     * Gets the GitHub token for per-session authentication.
     *
     * @return the GitHub token, or {@code null} if not set
     * @since 1.3.0
     */
    public String getGitHubToken() {
        return gitHubToken;
    }

    /**
     * Sets the GitHub token for per-session authentication.
     * <p>
     * When provided, the runtime resolves this token into a full GitHub identity
     * and stores it on the session for content exclusion, model routing, and quota
     * checks.
     *
     * @param gitHubToken
     *            the GitHub token for per-session authentication
     * @return this config for method chaining
     * @since 1.3.0
     */
    public ResumeSessionConfig setGitHubToken(String gitHubToken) {
        this.gitHubToken = gitHubToken;
        return this;
    }

    /**
     * Gets the per-session remote behavior control.
     * <p>
     * See {@link SessionConfig#getRemoteSession()} for details on possible values.
     *
     * @return the remote session mode, or {@code null} if not set
     * @since 1.4.0
     */
    public String getRemoteSession() {
        return remoteSession;
    }

    /**
     * Sets the per-session remote behavior control.
     * <p>
     * See {@link SessionConfig#setRemoteSession(String)} for details on possible
     * values.
     *
     * @param remoteSession
     *            the remote session mode
     * @return this config for method chaining
     * @since 1.4.0
     */
    public ResumeSessionConfig setRemoteSession(String remoteSession) {
        this.remoteSession = remoteSession;
        return this;
    }

    /**
     * Creates a shallow clone of this {@code ResumeSessionConfig} instance.
     * <p>
     * Mutable collection properties are copied into new collection instances so
     * that modifications to those collections on the clone do not affect the
     * original. Other reference-type properties (like provider configuration,
     * system messages, hooks, infinite session configuration, and handlers) are not
     * deep-cloned; the original and the clone will share those objects.
     *
     * @return a clone of this config instance
     */
    @Override
    public ResumeSessionConfig clone() {
        ResumeSessionConfig copy = new ResumeSessionConfig();
        copy.clientName = this.clientName;
        copy.model = this.model;
        copy.tools = this.tools != null ? new ArrayList<>(this.tools) : null;
        copy.systemMessage = this.systemMessage;
        copy.availableTools = this.availableTools != null ? new ArrayList<>(this.availableTools) : null;
        copy.excludedTools = this.excludedTools != null ? new ArrayList<>(this.excludedTools) : null;
        copy.provider = this.provider;
        copy.enableSessionTelemetry = this.enableSessionTelemetry;
        copy.reasoningEffort = this.reasoningEffort;
        copy.reasoningSummary = this.reasoningSummary;
        copy.modelCapabilities = this.modelCapabilities;
        copy.onPermissionRequest = this.onPermissionRequest;
        copy.onUserInputRequest = this.onUserInputRequest;
        copy.hooks = this.hooks;
        copy.workingDirectory = this.workingDirectory;
        copy.configDirectory = this.configDirectory;
        copy.enableConfigDiscovery = this.enableConfigDiscovery;
        copy.disableResume = this.disableResume;
        copy.streaming = this.streaming;
        copy.includeSubAgentStreamingEvents = this.includeSubAgentStreamingEvents;
        copy.mcpServers = this.mcpServers != null ? new java.util.HashMap<>(this.mcpServers) : null;
        copy.customAgents = this.customAgents != null ? new ArrayList<>(this.customAgents) : null;
        copy.defaultAgent = this.defaultAgent;
        copy.agent = this.agent;
        copy.skillDirectories = this.skillDirectories != null ? new ArrayList<>(this.skillDirectories) : null;
        copy.instructionDirectories = this.instructionDirectories != null
                ? new ArrayList<>(this.instructionDirectories)
                : null;
        copy.pluginDirectories = this.pluginDirectories != null ? new ArrayList<>(this.pluginDirectories) : null;
        copy.largeOutput = this.largeOutput;
        copy.disabledSkills = this.disabledSkills != null ? new ArrayList<>(this.disabledSkills) : null;
        copy.infiniteSessions = this.infiniteSessions;
        copy.onEvent = this.onEvent;
        copy.commands = this.commands != null ? new ArrayList<>(this.commands) : null;
        copy.onElicitationRequest = this.onElicitationRequest;
        copy.onExitPlanMode = this.onExitPlanMode;
        copy.onAutoModeSwitch = this.onAutoModeSwitch;
        copy.enableMcpApps = this.enableMcpApps;
        copy.gitHubToken = this.gitHubToken;
        copy.remoteSession = this.remoteSession;
        return copy;
    }
}
