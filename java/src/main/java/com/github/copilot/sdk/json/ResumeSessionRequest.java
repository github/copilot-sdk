/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

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
 * {@link com.github.copilot.sdk.CopilotClient#resumeSession(String, ResumeSessionConfig)}.
 *
 * @see com.github.copilot.sdk.CopilotClient#resumeSession(String,
 *      ResumeSessionConfig)
 * @see ResumeSessionConfig
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ResumeSessionRequest {

    @JsonProperty("sessionId")
    private String sessionId;

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

    @JsonProperty("disableResume")
    private Boolean disableResume;

    @JsonProperty("streaming")
    private Boolean streaming;

    @JsonProperty("mcpServers")
    private Map<String, Object> mcpServers;

    @JsonProperty("customAgents")
    private List<CustomAgentConfig> customAgents;

    @JsonProperty("skillDirectories")
    private List<String> skillDirectories;

    @JsonProperty("disabledSkills")
    private List<String> disabledSkills;

    @JsonProperty("infiniteSessions")
    private InfiniteSessionConfig infiniteSessions;

    /** Gets the session ID. @return the session ID */
    public String getSessionId() {
        return sessionId;
    }

    /** Sets the session ID. @param sessionId the session ID */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    /** Gets request permission flag. @return the flag */
    public Boolean getRequestPermission() {
        return requestPermission;
    }

    /** Sets request permission flag. @param requestPermission the flag */
    public void setRequestPermission(Boolean requestPermission) {
        this.requestPermission = requestPermission;
    }

    /** Gets request user input flag. @return the flag */
    public Boolean getRequestUserInput() {
        return requestUserInput;
    }

    /** Sets request user input flag. @param requestUserInput the flag */
    public void setRequestUserInput(Boolean requestUserInput) {
        this.requestUserInput = requestUserInput;
    }

    /** Gets hooks flag. @return the flag */
    public Boolean getHooks() {
        return hooks;
    }

    /** Sets hooks flag. @param hooks the flag */
    public void setHooks(Boolean hooks) {
        this.hooks = hooks;
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

    /** Gets disable resume flag. @return the flag */
    public Boolean getDisableResume() {
        return disableResume;
    }

    /** Sets disable resume flag. @param disableResume the flag */
    public void setDisableResume(Boolean disableResume) {
        this.disableResume = disableResume;
    }

    /** Gets streaming flag. @return the flag */
    public Boolean getStreaming() {
        return streaming;
    }

    /** Sets streaming flag. @param streaming the flag */
    public void setStreaming(Boolean streaming) {
        this.streaming = streaming;
    }

    /** Gets MCP servers. @return the servers map */
    public Map<String, Object> getMcpServers() {
        return mcpServers == null ? null : Collections.unmodifiableMap(mcpServers);
    }

    /** Sets MCP servers. @param mcpServers the servers map */
    public void setMcpServers(Map<String, Object> mcpServers) {
        this.mcpServers = mcpServers;
    }

    /** Gets custom agents. @return the agents */
    public List<CustomAgentConfig> getCustomAgents() {
        return customAgents == null ? null : Collections.unmodifiableList(customAgents);
    }

    /** Sets custom agents. @param customAgents the agents */
    public void setCustomAgents(List<CustomAgentConfig> customAgents) {
        this.customAgents = customAgents;
    }

    /** Gets skill directories. @return the directories */
    public List<String> getSkillDirectories() {
        return skillDirectories == null ? null : Collections.unmodifiableList(skillDirectories);
    }

    /** Sets skill directories. @param skillDirectories the directories */
    public void setSkillDirectories(List<String> skillDirectories) {
        this.skillDirectories = skillDirectories;
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
}
