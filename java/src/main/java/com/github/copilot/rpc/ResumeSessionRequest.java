/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal request object for resuming an existing session.
 * <p>
 * This is a low-level class for JSON-RPC communication. For resuming sessions,
 * use
 * {@link com.github.copilot.CopilotClient#resumeSession(String, ResumeSessionConfig)}.
 *
 * @see com.github.copilot.CopilotClient#resumeSession(String,
 *      ResumeSessionConfig)
 * @see ResumeSessionConfig
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ResumeSessionRequest {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("model")
    private String model;

    @JsonProperty("reasoningEffort")
    private String reasoningEffort;

    @JsonProperty("tools")
    private List<ToolDefinition> tools;

    @JsonProperty("systemMessage")
    private SystemMessageConfig systemMessage;

    @JsonProperty("availableTools")
    private List<String> availableTools;

    @JsonProperty("excludedTools")
    private List<String> excludedTools;

    @JsonProperty("provider")
    private ProviderConfig provider;

    @JsonProperty("enableSessionTelemetry")
    private Boolean enableSessionTelemetry;

    @JsonProperty("requestPermission")
    private Boolean requestPermission;

    @JsonProperty("requestUserInput")
    private Boolean requestUserInput;

    @JsonProperty("hooks")
    private Boolean hooks;

    @JsonProperty("workingDirectory")
    private String workingDirectory;

    @JsonProperty("configDir")
    private String configDir;

    @JsonProperty("enableConfigDiscovery")
    private Boolean enableConfigDiscovery;

    @JsonProperty("skipEmbeddingRetrieval")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean skipEmbeddingRetrieval;

    @JsonProperty("organizationCustomInstructions")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String organizationCustomInstructions;

    @JsonProperty("enableOnDemandInstructionDiscovery")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableOnDemandInstructionDiscovery;

    @JsonProperty("enableFileHooks")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableFileHooks;

    @JsonProperty("enableHostGitOperations")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableHostGitOperations;

    @JsonProperty("enableSessionStore")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableSessionStore;

    @JsonProperty("enableSkills")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableSkills;

    @JsonProperty("disableResume")
    private Boolean disableResume;

    @JsonProperty("streaming")
    private Boolean streaming;

    @JsonProperty("includeSubAgentStreamingEvents")
    private Boolean includeSubAgentStreamingEvents;

    @JsonProperty("mcpServers")
    private Map<String, McpServerConfig> mcpServers;

    @JsonProperty("envValueMode")
    private String envValueMode;

    @JsonProperty("customAgents")
    private List<CustomAgentConfig> customAgents;

    @JsonProperty("defaultAgent")
    private DefaultAgentConfig defaultAgent;

    @JsonProperty("agent")
    private String agent;

    @JsonProperty("skillDirectories")
    private List<String> skillDirectories;

    @JsonProperty("instructionDirectories")
    private List<String> instructionDirectories;

    @JsonProperty("disabledSkills")
    private List<String> disabledSkills;

    @JsonProperty("infiniteSessions")
    private InfiniteSessionConfig infiniteSessions;

    @JsonProperty("commands")
    private List<CommandWireDefinition> commands;

    @JsonProperty("requestElicitation")
    private Boolean requestElicitation;

    @JsonProperty("requestExitPlanMode")
    private Boolean requestExitPlanMode;

    @JsonProperty("requestAutoModeSwitch")
    private Boolean requestAutoModeSwitch;

    @JsonProperty("modelCapabilities")
    private ModelCapabilitiesOverride modelCapabilities;

    @JsonProperty("gitHubToken")
    private String gitHubToken;

    @JsonProperty("remoteSession")
    private String remoteSession;

    /** Gets the session ID. @return the session ID */
    public String getSessionId() {
        return sessionId;
    }

    /** Sets the session ID. @param sessionId the session ID */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /** Gets the client name. @return the client name */
    public String getClientName() {
        return clientName;
    }

    /** Sets the client name. @param clientName the client name */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /** Gets the model name. @return the model */
    public String getModel() {
        return model;
    }

    /** Sets the model name. @param model the model */
    public void setModel(String model) {
        this.model = model;
    }

    /** Gets the reasoning effort. @return the reasoning effort level */
    public String getReasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Sets the reasoning effort. @param reasoningEffort the reasoning effort level
     */
    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    /** Gets the tools. @return the tool definitions */
    public List<ToolDefinition> getTools() {
        return tools == null ? null : Collections.unmodifiableList(tools);
    }

    /** Sets the tools. @param tools the tool definitions */
    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }

    /** Gets the system message config. @return the system message config */
    public SystemMessageConfig getSystemMessage() {
        return systemMessage;
    }

    /**
     * Sets the system message config. @param systemMessage the system message
     * config
     */
    public void setSystemMessage(SystemMessageConfig systemMessage) {
        this.systemMessage = systemMessage;
    }

    /** Gets available tools. @return the available tool names */
    public List<String> getAvailableTools() {
        return availableTools == null ? null : Collections.unmodifiableList(availableTools);
    }

    /** Sets available tools. @param availableTools the available tool names */
    public void setAvailableTools(List<String> availableTools) {
        this.availableTools = availableTools;
    }

    /** Gets excluded tools. @return the excluded tool names */
    public List<String> getExcludedTools() {
        return excludedTools == null ? null : Collections.unmodifiableList(excludedTools);
    }

    /** Sets excluded tools. @param excludedTools the excluded tool names */
    public void setExcludedTools(List<String> excludedTools) {
        this.excludedTools = excludedTools;
    }

    /** Gets the provider config. @return the provider */
    public ProviderConfig getProvider() {
        return provider;
    }

    /** Sets the provider config. @param provider the provider */
    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }

    /** Gets enable session telemetry flag. @return the flag */
    public Boolean getEnableSessionTelemetry() {
        return enableSessionTelemetry;
    }

    /**
     * Sets enable session telemetry flag. @param enableSessionTelemetry the flag
     */
    public void setEnableSessionTelemetry(boolean enableSessionTelemetry) {
        this.enableSessionTelemetry = enableSessionTelemetry;
    }

    /**
     * Clears the enableSessionTelemetry setting, reverting to the default behavior.
     */
    public void clearEnableSessionTelemetry() {
        this.enableSessionTelemetry = null;
    }

    /** Gets request permission flag. @return the flag */
    public Boolean getRequestPermission() {
        return requestPermission;
    }

    /** Sets request permission flag. @param requestPermission the flag */
    public void setRequestPermission(boolean requestPermission) {
        this.requestPermission = requestPermission;
    }

    /**
     * Clears the requestPermission setting, reverting to the default behavior.
     */
    public void clearRequestPermission() {
        this.requestPermission = null;
    }

    /** Gets request user input flag. @return the flag */
    public Boolean getRequestUserInput() {
        return requestUserInput;
    }

    /** Sets request user input flag. @param requestUserInput the flag */
    public void setRequestUserInput(boolean requestUserInput) {
        this.requestUserInput = requestUserInput;
    }

    /**
     * Clears the requestUserInput setting, reverting to the default behavior.
     */
    public void clearRequestUserInput() {
        this.requestUserInput = null;
    }

    /** Gets hooks flag. @return the flag */
    public Boolean getHooks() {
        return hooks;
    }

    /** Sets hooks flag. @param hooks the flag */
    public void setHooks(boolean hooks) {
        this.hooks = hooks;
    }

    /**
     * Clears the hooks setting, reverting to the default behavior.
     */
    public void clearHooks() {
        this.hooks = null;
    }

    /** Gets working directory. @return the working directory */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /** Sets working directory. @param workingDirectory the working directory */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /** Gets config directory. @return the config directory */
    public String getConfigDir() {
        return configDir;
    }

    /** Sets config directory. @param configDir the config directory */
    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

    /** Gets enable config discovery flag. @return the flag */
    public Boolean getEnableConfigDiscovery() {
        return enableConfigDiscovery;
    }

    /** Sets enable config discovery flag. @param enableConfigDiscovery the flag */
    public void setEnableConfigDiscovery(boolean enableConfigDiscovery) {
        this.enableConfigDiscovery = enableConfigDiscovery;
    }

    /**
     * Clears the enableConfigDiscovery setting, reverting to the default behavior.
     */
    public void clearEnableConfigDiscovery() {
        this.enableConfigDiscovery = null;
    }

    /** Gets skip embedding retrieval flag. @return the flag */
    public Boolean getSkipEmbeddingRetrieval() {
        return skipEmbeddingRetrieval;
    }

    /** Sets skip embedding retrieval flag. @param skipEmbeddingRetrieval the flag */
    public void setSkipEmbeddingRetrieval(boolean skipEmbeddingRetrieval) {
        this.skipEmbeddingRetrieval = skipEmbeddingRetrieval;
    }

    /** Clears the skipEmbeddingRetrieval setting, reverting to the default behavior. */
    public void clearSkipEmbeddingRetrieval() {
        this.skipEmbeddingRetrieval = null;
    }

    /** Gets organization custom instructions. @return the instructions */
    public String getOrganizationCustomInstructions() {
        return organizationCustomInstructions;
    }

    /** Sets organization custom instructions. @param organizationCustomInstructions the instructions */
    public void setOrganizationCustomInstructions(String organizationCustomInstructions) {
        this.organizationCustomInstructions = organizationCustomInstructions;
    }

    /** Gets enable on-demand instruction discovery flag. @return the flag */
    public Boolean getEnableOnDemandInstructionDiscovery() {
        return enableOnDemandInstructionDiscovery;
    }

    /**
     * Sets enable on-demand instruction discovery flag. @param
     * enableOnDemandInstructionDiscovery the flag
     */
    public void setEnableOnDemandInstructionDiscovery(boolean enableOnDemandInstructionDiscovery) {
        this.enableOnDemandInstructionDiscovery = enableOnDemandInstructionDiscovery;
    }

    /**
     * Clears the enableOnDemandInstructionDiscovery setting, reverting to the default behavior.
     */
    public void clearEnableOnDemandInstructionDiscovery() {
        this.enableOnDemandInstructionDiscovery = null;
    }

    /** Gets enable file hooks flag. @return the flag */
    public Boolean getEnableFileHooks() {
        return enableFileHooks;
    }

    /** Sets enable file hooks flag. @param enableFileHooks the flag */
    public void setEnableFileHooks(boolean enableFileHooks) {
        this.enableFileHooks = enableFileHooks;
    }

    /** Clears the enableFileHooks setting, reverting to the default behavior. */
    public void clearEnableFileHooks() {
        this.enableFileHooks = null;
    }

    /** Gets enable host git operations flag. @return the flag */
    public Boolean getEnableHostGitOperations() {
        return enableHostGitOperations;
    }

    /** Sets enable host git operations flag. @param enableHostGitOperations the flag */
    public void setEnableHostGitOperations(boolean enableHostGitOperations) {
        this.enableHostGitOperations = enableHostGitOperations;
    }

    /** Clears the enableHostGitOperations setting, reverting to the default behavior. */
    public void clearEnableHostGitOperations() {
        this.enableHostGitOperations = null;
    }

    /** Gets enable session store flag. @return the flag */
    public Boolean getEnableSessionStore() {
        return enableSessionStore;
    }

    /** Sets enable session store flag. @param enableSessionStore the flag */
    public void setEnableSessionStore(boolean enableSessionStore) {
        this.enableSessionStore = enableSessionStore;
    }

    /** Clears the enableSessionStore setting, reverting to the default behavior. */
    public void clearEnableSessionStore() {
        this.enableSessionStore = null;
    }

    /** Gets enable skills flag. @return the flag */
    public Boolean getEnableSkills() {
        return enableSkills;
    }

    /** Sets enable skills flag. @param enableSkills the flag */
    public void setEnableSkills(boolean enableSkills) {
        this.enableSkills = enableSkills;
    }

    /** Clears the enableSkills setting, reverting to the default behavior. */
    public void clearEnableSkills() {
        this.enableSkills = null;
    }

    /** Gets disable resume flag. @return the flag */
    public Boolean getDisableResume() {
        return disableResume;
    }

    /** Sets disable resume flag. @param disableResume the flag */
    public void setDisableResume(boolean disableResume) {
        this.disableResume = disableResume;
    }

    /**
     * Clears the disableResume setting, reverting to the default behavior.
     */
    public void clearDisableResume() {
        this.disableResume = null;
    }

    /** Gets streaming flag. @return the flag */
    public Boolean getStreaming() {
        return streaming;
    }

    /** Sets streaming flag. @param streaming the flag */
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    /**
     * Clears the streaming setting, reverting to the default behavior.
     */
    public void clearStreaming() {
        this.streaming = null;
    }

    /** Gets include sub-agent streaming events flag. @return the flag */
    public Boolean getIncludeSubAgentStreamingEvents() {
        return includeSubAgentStreamingEvents;
    }

    /**
     * Sets include sub-agent streaming events flag. @param
     * includeSubAgentStreamingEvents the flag
     */
    public void setIncludeSubAgentStreamingEvents(boolean includeSubAgentStreamingEvents) {
        this.includeSubAgentStreamingEvents = includeSubAgentStreamingEvents;
    }

    /**
     * Clears the includeSubAgentStreamingEvents setting, reverting to the default
     * behavior.
     */
    public void clearIncludeSubAgentStreamingEvents() {
        this.includeSubAgentStreamingEvents = null;
    }

    /** Gets MCP servers. @return the servers map */
    public Map<String, McpServerConfig> getMcpServers() {
        return mcpServers == null ? null : Collections.unmodifiableMap(mcpServers);
    }

    /** Sets MCP servers. @param mcpServers the servers map */
    public void setMcpServers(Map<String, McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    /** Gets MCP environment variable value mode. @return the mode */
    public String getEnvValueMode() {
        return envValueMode;
    }

    /** Sets MCP environment variable value mode. @param envValueMode the mode */
    public void setEnvValueMode(String envValueMode) {
        this.envValueMode = envValueMode;
    }

    /** Gets custom agents. @return the agents */
    public List<CustomAgentConfig> getCustomAgents() {
        return customAgents == null ? null : Collections.unmodifiableList(customAgents);
    }

    /** Sets custom agents. @param customAgents the agents */
    public void setCustomAgents(List<CustomAgentConfig> customAgents) {
        this.customAgents = customAgents;
    }

    /** Gets the default agent config. @return the default agent config */
    public DefaultAgentConfig getDefaultAgent() {
        return defaultAgent;
    }

    /**
     * Sets the default agent config. @param defaultAgent the default agent config
     */
    public void setDefaultAgent(DefaultAgentConfig defaultAgent) {
        this.defaultAgent = defaultAgent;
    }

    /** Gets the pre-selected agent name. @return the agent name */
    public String getAgent() {
        return agent;
    }

    /** Sets the pre-selected agent name. @param agent the agent name */
    public void setAgent(String agent) {
        this.agent = agent;
    }

    /** Gets skill directories. @return the directories */
    public List<String> getSkillDirectories() {
        return skillDirectories == null ? null : Collections.unmodifiableList(skillDirectories);
    }

    /** Sets skill directories. @param skillDirectories the directories */
    public void setSkillDirectories(List<String> skillDirectories) {
        this.skillDirectories = skillDirectories;
    }

    /** Gets instruction directories. @return the instruction directories */
    public List<String> getInstructionDirectories() {
        return instructionDirectories == null ? null : Collections.unmodifiableList(instructionDirectories);
    }

    /**
     * Sets instruction directories. @param instructionDirectories the directories
     */
    public void setInstructionDirectories(List<String> instructionDirectories) {
        this.instructionDirectories = instructionDirectories;
    }

    /** Gets disabled skills. @return the disabled skill names */
    public List<String> getDisabledSkills() {
        return disabledSkills == null ? null : Collections.unmodifiableList(disabledSkills);
    }

    /** Sets disabled skills. @param disabledSkills the skill names to disable */
    public void setDisabledSkills(List<String> disabledSkills) {
        this.disabledSkills = disabledSkills;
    }

    /** Gets infinite sessions config. @return the infinite sessions config */
    public InfiniteSessionConfig getInfiniteSessions() {
        return infiniteSessions;
    }

    /**
     * Sets infinite sessions config. @param infiniteSessions the infinite sessions
     * config
     */
    public void setInfiniteSessions(InfiniteSessionConfig infiniteSessions) {
        this.infiniteSessions = infiniteSessions;
    }

    /** Gets the commands wire definitions. @return the commands */
    public List<CommandWireDefinition> getCommands() {
        return commands == null ? null : Collections.unmodifiableList(commands);
    }

    /** Sets the commands wire definitions. @param commands the commands */
    public void setCommands(List<CommandWireDefinition> commands) {
        this.commands = commands;
    }

    /** Gets the requestElicitation flag. @return the flag */
    public Boolean getRequestElicitation() {
        return requestElicitation;
    }

    /** Sets the requestElicitation flag. @param requestElicitation the flag */
    public void setRequestElicitation(boolean requestElicitation) {
        this.requestElicitation = requestElicitation;
    }

    /**
     * Clears the requestElicitation setting, reverting to the default behavior.
     */
    public void clearRequestElicitation() {
        this.requestElicitation = null;
    }

    /** Gets the requestExitPlanMode flag. @return the flag */
    public Boolean getRequestExitPlanMode() {
        return requestExitPlanMode;
    }

    /** Sets the requestExitPlanMode flag. @param requestExitPlanMode the flag */
    public void setRequestExitPlanMode(Boolean requestExitPlanMode) {
        this.requestExitPlanMode = requestExitPlanMode;
    }

    /** Gets the requestAutoModeSwitch flag. @return the flag */
    public Boolean getRequestAutoModeSwitch() {
        return requestAutoModeSwitch;
    }

    /**
     * Sets the requestAutoModeSwitch flag. @param requestAutoModeSwitch the flag
     */
    public void setRequestAutoModeSwitch(Boolean requestAutoModeSwitch) {
        this.requestAutoModeSwitch = requestAutoModeSwitch;
    }

    /** Gets the model capabilities override. @return the override */
    public ModelCapabilitiesOverride getModelCapabilities() {
        return modelCapabilities;
    }

    /**
     * Sets the model capabilities override. @param modelCapabilities the override
     */
    public void setModelCapabilities(ModelCapabilitiesOverride modelCapabilities) {
        this.modelCapabilities = modelCapabilities;
    }

    /** Gets the GitHub token for per-session authentication. @return the token */
    public String getGitHubToken() {
        return gitHubToken;
    }

    /**
     * Sets the GitHub token for per-session authentication. @param gitHubToken the
     * token
     */
    public void setGitHubToken(String gitHubToken) {
        this.gitHubToken = gitHubToken;
    }

    /** Gets the remote session mode. @return the remote session mode */
    public String getRemoteSession() {
        return remoteSession;
    }

    /**
     * Sets the remote session mode. @param remoteSession the remote session mode
     */
    public void setRemoteSession(String remoteSession) {
        this.remoteSession = remoteSession;
    }
}
