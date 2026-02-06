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
 * Configuration for a custom agent in a Copilot session.
 * <p>
 * Custom agents extend the capabilities of the base Copilot assistant with
 * specialized behavior, tools, and prompts. Each agent can be referenced in
 * messages using the {@code @agent-name} mention syntax.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var agent = new CustomAgentConfig().setName("code-reviewer").setDisplayName("Code Reviewer")
 * 		.setDescription("Reviews code for best practices").setPrompt("You are a code review expert...")
 * 		.setTools(List.of("read_file", "search_code"));
 *
 * var config = new SessionConfig().setCustomAgents(List.of(agent));
 * }</pre>
 *
 * @see SessionConfig#setCustomAgents(List)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomAgentConfig {

    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tools")
    private List<String> tools;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("mcpServers")
    private Map<String, Object> mcpServers;

    @JsonProperty("infer")
    private Boolean infer;

    /**
     * Gets the unique identifier name for this agent.
     *
     * @return the agent name used for {@code @mentions}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique identifier name for this agent.
     * <p>
     * This name is used to mention the agent in messages (e.g.,
     * {@code @code-reviewer}).
     *
     * @param name
     *            the agent identifier (alphanumeric and hyphens)
     * @return this config for method chaining
     */
    public CustomAgentConfig setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name shown to users
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human-readable display name.
     *
     * @param displayName
     *            the friendly name for the agent
     * @return this config for method chaining
     */
    public CustomAgentConfig setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Gets the agent description.
     *
     * @return the description of what this agent does
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets a description of the agent's capabilities.
     * <p>
     * This helps users understand when to use this agent.
     *
     * @param description
     *            the agent description
     * @return this config for method chaining
     */
    public CustomAgentConfig setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Gets the list of tool names available to this agent.
     *
     * @return the list of tool identifiers
     */
    public List<String> getTools() {
        return tools == null ? null : Collections.unmodifiableList(tools);
    }

    /**
     * Sets the tools available to this agent.
     * <p>
     * These can reference both built-in tools and custom tools registered in the
     * session.
     *
     * @param tools
     *            the list of tool names
     * @return this config for method chaining
     */
    public CustomAgentConfig setTools(List<String> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * Gets the system prompt for this agent.
     *
     * @return the agent's system prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the system prompt that defines this agent's behavior.
     * <p>
     * This prompt is used to customize the agent's responses and capabilities.
     *
     * @param prompt
     *            the system prompt
     * @return this config for method chaining
     */
    public CustomAgentConfig setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * Gets the MCP server configurations for this agent.
     *
     * @return the MCP servers map
     */
    public Map<String, Object> getMcpServers() {
        return mcpServers == null ? null : Collections.unmodifiableMap(mcpServers);
    }

    /**
     * Sets MCP (Model Context Protocol) servers available to this agent.
     *
     * @param mcpServers
     *            the MCP server configurations
     * @return this config for method chaining
     */
    public CustomAgentConfig setMcpServers(Map<String, Object> mcpServers) {
        this.mcpServers = mcpServers;
        return this;
    }

    /**
     * Gets whether inference mode is enabled.
     *
     * @return the infer flag, or {@code null} if not set
     */
    public Boolean getInfer() {
        return infer;
    }

    /**
     * Sets whether to enable inference mode for this agent.
     *
     * @param infer
     *            {@code true} to enable inference mode
     * @return this config for method chaining
     */
    public CustomAgentConfig setInfer(Boolean infer) {
        this.infer = infer;
        return this;
    }
}
