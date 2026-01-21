/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal request object for creating a new session.
 * <p>
 * This is a low-level class for JSON-RPC communication. For creating sessions,
 * use
 * {@link com.github.copilot.sdk.CopilotClient#createSession(SessionConfig)}.
 *
 * @see com.github.copilot.sdk.CopilotClient#createSession(SessionConfig)
 * @see SessionConfig
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CreateSessionRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("tools")
    private List<ToolDef> tools;

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

    @JsonProperty("streaming")
    private Boolean streaming;

    @JsonProperty("mcpServers")
    private Map<String, Object> mcpServers;

    @JsonProperty("customAgents")
    private List<CustomAgentConfig> customAgents;

    /** Gets the model name. @return the model */
    public String getModel() {
        return model;
    }

    /** Sets the model name. @param model the model */
    public void setModel(String model) {
        this.model = model;
    }

    /** Gets the session ID. @return the session ID */
    public String getSessionId() {
        return sessionId;
    }

    /** Sets the session ID. @param sessionId the session ID */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /** Gets the tools. @return the tool definitions */
    public List<ToolDef> getTools() {
        return tools;
    }

    /** Sets the tools. @param tools the tool definitions */
    public void setTools(List<ToolDef> tools) {
        this.tools = tools;
    }

    /** Gets the system message config. @return the config */
    public SystemMessageConfig getSystemMessage() {
        return systemMessage;
    }

    /** Sets the system message config. @param systemMessage the config */
    public void setSystemMessage(SystemMessageConfig systemMessage) {
        this.systemMessage = systemMessage;
    }

    /** Gets available tools. @return the tool names */
    public List<String> getAvailableTools() {
        return availableTools;
    }

    /** Sets available tools. @param availableTools the tool names */
    public void setAvailableTools(List<String> availableTools) {
        this.availableTools = availableTools;
    }

    /** Gets excluded tools. @return the tool names */
    public List<String> getExcludedTools() {
        return excludedTools;
    }

    /** Sets excluded tools. @param excludedTools the tool names */
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
        return mcpServers;
    }

    /** Sets MCP servers. @param mcpServers the servers map */
    public void setMcpServers(Map<String, Object> mcpServers) {
        this.mcpServers = mcpServers;
    }

    /** Gets custom agents. @return the agents */
    public List<CustomAgentConfig> getCustomAgents() {
        return customAgents;
    }

    /** Sets custom agents. @param customAgents the agents */
    public void setCustomAgents(List<CustomAgentConfig> customAgents) {
        this.customAgents = customAgents;
    }
}
