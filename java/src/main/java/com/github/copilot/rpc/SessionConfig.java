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
 * @see com.github.copilot.CopilotClient#createSession(SessionConfig)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionConfig {

    private String sessionId;
    private String clientName;
    private String model;
    private String reasoningEffort;
    private String reasoningSummary;
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
    private PermissionHandler onPermissionRequest;
    private UserInputHandler onUserInputRequest;
    private SessionHooks hooks;
    private String workingDirectory;
    private boolean streaming;
    private Boolean includeSubAgentStreamingEvents;
    private Map<String, McpServerConfig> mcpServers;
    private List<CustomAgentConfig> customAgents;
    private DefaultAgentConfig defaultAgent;
    private String agent;
    private InfiniteSessionConfig infiniteSessions;
    private List<String> skillDirectories;
    private List<String> instructionDirectories;
    private List<String> pluginDirectories;
    private LargeToolOutputConfig largeOutput;
    private List<String> disabledSkills;
    private String configDirectory;
    private Boolean enableConfigDiscovery;
    private ModelCapabilitiesOverride modelCapabilities;
    private Consumer<SessionEvent> onEvent;
    private List<CommandDefinition> commands;
    private ElicitationHandler onElicitationRequest;
    private ExitPlanModeHandler onExitPlanMode;
    private AutoModeSwitchHandler onAutoModeSwitch;
    private boolean enableMcpApps;
    private String gitHubToken;
    private String remoteSession;
    private CloudSessionOptions cloud;

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
     * @return this config instance for method chaining
     */
    public SessionConfig setClientName(String clientName) {
        this.clientName = clientName;
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
     * @return this config instance for method chaining
     */
    public SessionConfig setReasoningSummary(String reasoningSummary) {
        this.reasoningSummary = reasoningSummary;
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
     * Use {@link com.github.copilot.SystemMessageMode#APPEND} to add instructions
     * while preserving default behavior, or
     * {@link com.github.copilot.SystemMessageMode#REPLACE} to fully customize.
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
     * @return this config instance for method chaining
     */
    public SessionConfig setEnableSessionTelemetry(boolean enableSessionTelemetry) {
        this.enableSessionTelemetry = enableSessionTelemetry;
        return this;
    }

    /**
     * Clears the enableSessionTelemetry setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public SessionConfig clearEnableSessionTelemetry() {
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
     * Sets whether to suppress loading of custom instruction files (e.g.
     * {@code .github/copilot-instructions.md}, {@code AGENTS.md}) from the working
     * directory.
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session creation. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code true}
     * (skip); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the value
     * is forwarded only when explicitly set.
     *
     * @param skipCustomInstructions
     *            whether to skip custom instructions
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig setSkipCustomInstructions(boolean skipCustomInstructions) {
        this.skipCustomInstructions = skipCustomInstructions;
        return this;
    }

    /**
     * Clears the skipCustomInstructions setting.
     *
     * @return this instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig clearSkipCustomInstructions() {
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
     * working directory (no organisation-level discovery).
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session creation. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code true} (local
     * only); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the value is
     * forwarded only when explicitly set.
     *
     * @param customAgentsLocalOnly
     *            whether to restrict to local agents
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig setCustomAgentsLocalOnly(boolean customAgentsLocalOnly) {
        this.customAgentsLocalOnly = customAgentsLocalOnly;
        return this;
    }

    /**
     * Clears the customAgentsLocalOnly setting.
     *
     * @return this instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig clearCustomAgentsLocalOnly() {
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
     * trailer when it commits on behalf of the user.
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session creation. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code false}
     * (disabled); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the
     * value is forwarded only when explicitly set.
     *
     * @param coauthorEnabled
     *            whether coauthor is enabled
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig setCoauthorEnabled(boolean coauthorEnabled) {
        this.coauthorEnabled = coauthorEnabled;
        return this;
    }

    /**
     * Clears the coauthorEnabled setting.
     *
     * @return this instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig clearCoauthorEnabled() {
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
     * Sets whether to enable the {@code manage_schedule} tool (host scheduler
     * integration).
     * <p>
     * This option is sent to the server via a {@code session.options.update}
     * JSON-RPC call immediately after session creation. In
     * {@link CopilotClientMode#EMPTY EMPTY} mode the default is {@code false}
     * (disabled); in {@link CopilotClientMode#COPILOT_CLI COPILOT_CLI} mode the
     * value is forwarded only when explicitly set.
     *
     * @param manageScheduleEnabled
     *            whether manage schedule is enabled
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig setManageScheduleEnabled(boolean manageScheduleEnabled) {
        this.manageScheduleEnabled = manageScheduleEnabled;
        return this;
    }

    /**
     * Clears the manageScheduleEnabled setting.
     *
     * @return this instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig clearManageScheduleEnabled() {
        this.manageScheduleEnabled = null;
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
    public Map<String, McpServerConfig> getMcpServers() {
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
    public SessionConfig setMcpServers(Map<String, McpServerConfig> mcpServers) {
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
     * @return this config instance for method chaining
     * @see DefaultAgentConfig
     */
    public SessionConfig setDefaultAgent(DefaultAgentConfig defaultAgent) {
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
     * @return this config instance for method chaining
     */
    public SessionConfig setAgent(String agent) {
        this.agent = agent;
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
     * @return this config instance for method chaining
     */
    public SessionConfig setInstructionDirectories(List<String> instructionDirectories) {
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
     * @return this config instance for method chaining
     */
    public SessionConfig setPluginDirectories(List<String> pluginDirectories) {
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
     * @return this config instance for method chaining
     */
    public SessionConfig setLargeOutput(LargeToolOutputConfig largeOutput) {
        this.largeOutput = largeOutput;
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
    public String getConfigDirectory() {
        return configDirectory;
    }

    /**
     * Sets a custom configuration directory for the session.
     * <p>
     * This allows using a specific directory for session configuration instead of
     * the default location.
     *
     * @param configDirectory
     *            the configuration directory path
     * @return this config instance for method chaining
     */
    public SessionConfig setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory;
        return this;
    }

    /**
     * Gets whether automatic configuration discovery is enabled.
     *
     * @return an {@link java.util.Optional} containing {@code true} to enable
     *         discovery or {@code false} to disable, or
     *         {@link java.util.Optional#empty()} to use the runtime default
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
     * explicitly provided {@link #setMcpServers(Map)} and
     * {@link #setSkillDirectories(List)}, with explicit values taking precedence on
     * name collision.
     *
     * @param enableConfigDiscovery
     *            {@code true} to enable discovery, {@code false} to disable
     * @return this config instance for method chaining
     */
    public SessionConfig setEnableConfigDiscovery(boolean enableConfigDiscovery) {
        this.enableConfigDiscovery = enableConfigDiscovery;
        return this;
    }

    /**
     * Clears the enableConfigDiscovery setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public SessionConfig clearEnableConfigDiscovery() {
        this.enableConfigDiscovery = null;
        return this;
    }

    /**
     * Gets whether sub-agent streaming events are included.
     *
     * @return an {@link java.util.Optional} containing {@code true} to include
     *         sub-agent streaming events or {@code false} to suppress them, or
     *         {@link java.util.Optional#empty()} to use the runtime default
     */
    @JsonIgnore
    public Optional<Boolean> getIncludeSubAgentStreamingEvents() {
        return Optional.ofNullable(includeSubAgentStreamingEvents);
    }

    /**
     * Sets whether to include sub-agent streaming events in the event stream.
     * <p>
     * When {@code true}, streaming delta events from sub-agents (e.g.,
     * {@code assistant.message_delta} with {@code agentId} set) are forwarded to
     * this connection. When {@code false}, only non-streaming sub-agent events and
     * {@code subagent.*} lifecycle events are forwarded; streaming deltas from
     * sub-agents are suppressed. Default: {@code true}.
     *
     * @param includeSubAgentStreamingEvents
     *            {@code true} to include streaming events, {@code false} to
     *            suppress
     * @return this config instance for method chaining
     */
    public SessionConfig setIncludeSubAgentStreamingEvents(boolean includeSubAgentStreamingEvents) {
        this.includeSubAgentStreamingEvents = includeSubAgentStreamingEvents;
        return this;
    }

    /**
     * Clears the includeSubAgentStreamingEvents setting, reverting to the default
     * behavior.
     *
     * @return this instance for method chaining
     */
    public SessionConfig clearIncludeSubAgentStreamingEvents() {
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
     * <p>
     * Use this to override specific model capabilities (such as vision support) for
     * this session. Only non-null fields in the override are applied; unset fields
     * retain their runtime defaults.
     *
     * @param modelCapabilities
     *            the model capabilities override
     * @return this config instance for method chaining
     * @see ModelCapabilitiesOverride
     */
    public SessionConfig setModelCapabilities(ModelCapabilitiesOverride modelCapabilities) {
        this.modelCapabilities = modelCapabilities;
        return this;
    }

    /**
     * Gets the event handler registered before the session.create RPC is issued.
     *
     * @return the event handler, or {@code null} if not set
     */
    public Consumer<SessionEvent> getOnEvent() {
        return onEvent;
    }

    /**
     * Sets an event handler that is registered on the session before the
     * {@code session.create} RPC is issued.
     * <p>
     * Equivalent to calling {@link com.github.copilot.CopilotSession#on(Consumer)}
     * immediately after creation, but executes earlier in the lifecycle so no
     * events are missed. Using this property rather than
     * {@code CopilotSession.on()} guarantees that early events emitted by the CLI
     * during session creation (e.g. {@code session.start}) are delivered to the
     * handler.
     *
     * @param onEvent
     *            the event handler to register before session creation
     * @return this config instance for method chaining
     */
    public SessionConfig setOnEvent(Consumer<SessionEvent> onEvent) {
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
     * @return this config instance for method chaining
     * @see CommandDefinition
     */
    public SessionConfig setCommands(List<CommandDefinition> commands) {
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
     * @return this config instance for method chaining
     * @see ElicitationHandler
     */
    public SessionConfig setOnElicitationRequest(ElicitationHandler onElicitationRequest) {
        this.onElicitationRequest = onElicitationRequest;
        return this;
    }

    /**
     * Returns whether MCP Apps (SEP-1865) UI passthrough is enabled on this
     * session.
     *
     * @return {@code true} if the consumer has opted into MCP Apps, otherwise
     *         {@code false}
     * @see #setEnableMcpApps(boolean)
     */
    public boolean isEnableMcpApps() {
        return enableMcpApps;
    }

    /**
     * Enables MCP Apps (SEP-1865) UI passthrough on this session.
     * <p>
     * When {@code true} <b>and</b> the runtime has MCP Apps enabled (via the
     * {@code MCP_APPS} feature flag or {@code COPILOT_MCP_APPS=true} environment
     * override), the runtime adds the {@code mcp-apps} capability to the session,
     * which causes it to advertise the
     * {@code extensions.io.modelcontextprotocol/ui} extension to MCP servers (so
     * they expose {@code _meta.ui.resourceUri} on tools) and to expose the
     * {@code session.rpc.mcp.apps.{listTools,callTool,readResource,
     * setHostContext,getHostContext,diagnose}} JSON-RPC methods.
     * <p>
     * If the runtime gate is off, the opt-in is silently dropped server-side (the
     * runtime logs a warning); the session is created normally but the MCP Apps
     * surface is unavailable. Inspect {@link SessionUiCapabilities#getMcpApps()} on
     * {@link com.github.copilot.CopilotSession#getCapabilities()} to detect this.
     * <p>
     * SDK consumers MUST set this to {@code true} only when they have an iframe
     * renderer that can display {@code ui://} MCP App bundles. Setting it without a
     * renderer will cause MCP servers to register UI-enabled tool variants the
     * consumer cannot display.
     *
     * @param enableMcpApps
     *            {@code true} to opt into MCP Apps support
     * @return this config instance for method chaining
     */
    public SessionConfig setEnableMcpApps(boolean enableMcpApps) {
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
     * @return this config instance for method chaining
     * @see ExitPlanModeHandler
     * @since 1.0.8
     */
    public SessionConfig setOnExitPlanMode(ExitPlanModeHandler onExitPlanMode) {
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
     * @return this config instance for method chaining
     * @see AutoModeSwitchHandler
     * @since 1.0.8
     */
    public SessionConfig setOnAutoModeSwitch(AutoModeSwitchHandler onAutoModeSwitch) {
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
     * @return this config instance for method chaining
     * @since 1.3.0
     */
    public SessionConfig setGitHubToken(String gitHubToken) {
        this.gitHubToken = gitHubToken;
        return this;
    }

    /**
     * Gets the per-session remote behavior control.
     * <p>
     * Possible values:
     * <ul>
     * <li>{@code "off"} — local only, no remote export (default)</li>
     * <li>{@code "export"} — export session events to GitHub without enabling
     * remote steering</li>
     * <li>{@code "on"} — export to GitHub AND enable remote steering</li>
     * </ul>
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
     * Possible values:
     * <ul>
     * <li>{@code "off"} — local only, no remote export (default)</li>
     * <li>{@code "export"} — export session events to GitHub without enabling
     * remote steering</li>
     * <li>{@code "on"} — export to GitHub AND enable remote steering</li>
     * </ul>
     *
     * @param remoteSession
     *            the remote session mode
     * @return this config instance for method chaining
     * @since 1.4.0
     */
    public SessionConfig setRemoteSession(String remoteSession) {
        this.remoteSession = remoteSession;
        return this;
    }

    /**
     * Gets the cloud session options.
     * <p>
     * When set, creates a remote session in the cloud instead of a local session.
     * The optional repository is associated with the cloud session.
     *
     * @return the cloud session options, or {@code null} if not set
     * @since 1.5.0
     */
    public CloudSessionOptions getCloud() {
        return cloud;
    }

    /**
     * Sets the cloud session options.
     * <p>
     * When set, creates a remote session in the cloud instead of a local session.
     * The optional repository is associated with the cloud session.
     *
     * @param cloud
     *            the cloud session options
     * @return this config instance for method chaining
     * @since 1.5.0
     */
    public SessionConfig setCloud(CloudSessionOptions cloud) {
        this.cloud = cloud;
        return this;
    }

    /**
     * Creates a shallow clone of this {@code SessionConfig} instance.
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
    public SessionConfig clone() {
        SessionConfig copy = new SessionConfig();
        copy.sessionId = this.sessionId;
        copy.clientName = this.clientName;
        copy.model = this.model;
        copy.reasoningEffort = this.reasoningEffort;
        copy.reasoningSummary = this.reasoningSummary;
        copy.tools = this.tools != null ? new ArrayList<>(this.tools) : null;
        copy.systemMessage = this.systemMessage;
        copy.availableTools = this.availableTools != null ? new ArrayList<>(this.availableTools) : null;
        copy.excludedTools = this.excludedTools != null ? new ArrayList<>(this.excludedTools) : null;
        copy.provider = this.provider;
        copy.enableSessionTelemetry = this.enableSessionTelemetry;
        copy.skipCustomInstructions = this.skipCustomInstructions;
        copy.customAgentsLocalOnly = this.customAgentsLocalOnly;
        copy.coauthorEnabled = this.coauthorEnabled;
        copy.manageScheduleEnabled = this.manageScheduleEnabled;
        copy.onPermissionRequest = this.onPermissionRequest;
        copy.onUserInputRequest = this.onUserInputRequest;
        copy.hooks = this.hooks;
        copy.workingDirectory = this.workingDirectory;
        copy.streaming = this.streaming;
        copy.includeSubAgentStreamingEvents = this.includeSubAgentStreamingEvents;
        copy.mcpServers = this.mcpServers != null ? new java.util.HashMap<>(this.mcpServers) : null;
        copy.customAgents = this.customAgents != null ? new ArrayList<>(this.customAgents) : null;
        copy.defaultAgent = this.defaultAgent;
        copy.agent = this.agent;
        copy.infiniteSessions = this.infiniteSessions;
        copy.skillDirectories = this.skillDirectories != null ? new ArrayList<>(this.skillDirectories) : null;
        copy.instructionDirectories = this.instructionDirectories != null
                ? new ArrayList<>(this.instructionDirectories)
                : null;
        copy.pluginDirectories = this.pluginDirectories != null ? new ArrayList<>(this.pluginDirectories) : null;
        copy.largeOutput = this.largeOutput;
        copy.disabledSkills = this.disabledSkills != null ? new ArrayList<>(this.disabledSkills) : null;
        copy.configDirectory = this.configDirectory;
        copy.enableConfigDiscovery = this.enableConfigDiscovery;
        copy.modelCapabilities = this.modelCapabilities;
        copy.onEvent = this.onEvent;
        copy.commands = this.commands != null ? new ArrayList<>(this.commands) : null;
        copy.onElicitationRequest = this.onElicitationRequest;
        copy.onExitPlanMode = this.onExitPlanMode;
        copy.onAutoModeSwitch = this.onAutoModeSwitch;
        copy.enableMcpApps = this.enableMcpApps;
        copy.gitHubToken = this.gitHubToken;
        copy.remoteSession = this.remoteSession;
        copy.cloud = this.cloud;
        return copy;
    }
}
