/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for the default agent (the built-in agent that handles turns
 * when no custom agent is selected).
 * <p>
 * Use {@link #setExcludedTools(List)} to hide specific tools from the default
 * agent while keeping them available to custom sub-agents.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var config = new SessionConfig().setTools(List.of(secretTool))
 * 		.setDefaultAgent(new DefaultAgentConfig().setExcludedTools(List.of("secret_tool")));
 * }</pre>
 *
 * @see SessionConfig#setDefaultAgent(DefaultAgentConfig)
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultAgentConfig {

    @JsonProperty("excludedTools")
    private List<String> excludedTools;

    /**
     * Gets the list of tool names excluded from the default agent.
     *
     * @return the list of excluded tool names, or {@code null} if not set
     */
    public List<String> getExcludedTools() {
        return excludedTools == null ? null : Collections.unmodifiableList(excludedTools);
    }

    /**
     * Sets the list of tool names to exclude from the default agent.
     * <p>
     * These tools remain available to custom sub-agents that reference them in
     * their {@link CustomAgentConfig#setTools(List)} list.
     *
     * @param excludedTools
     *            the list of tool names to exclude from the default agent
     * @return this config for method chaining
     */
    public DefaultAgentConfig setExcludedTools(List<String> excludedTools) {
        this.excludedTools = excludedTools;
        return this;
    }
}
