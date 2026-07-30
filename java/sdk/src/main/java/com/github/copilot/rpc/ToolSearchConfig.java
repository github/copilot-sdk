/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Overrides the runtime's built-in tool-search behavior.
 * <p>
 * Tool search defers tools to keep the model's active tool set small. To
 * override the tool-search tool's implementation, register a tool named
 * {@code "tool_search_tool"} with {@code overridesBuiltInTool} set to
 * {@code true}.
 *
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolSearchConfig {

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("deferThreshold")
    private Integer deferThreshold;

    /**
     * Gets whether tool search is enabled.
     *
     * @return {@code true} if enabled, {@code false} if disabled, or {@code null}
     *         for the runtime default
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Toggle that enables or disables tool search.
     *
     * @param enabled
     *            {@code true} to enable, {@code false} to disable
     * @return this config for method chaining
     */
    public ToolSearchConfig setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Gets the tool count above which MCP and external tools are deferred behind
     * tool search.
     *
     * @return the defer threshold, or {@code null} for the runtime default (30)
     */
    public Integer getDeferThreshold() {
        return deferThreshold;
    }

    /**
     * Sets the tool count above which MCP and external tools are deferred behind
     * tool search. Defaults to the runtime default (30) when unset.
     *
     * @param deferThreshold
     *            the threshold value
     * @return this config for method chaining
     */
    public ToolSearchConfig setDeferThreshold(Integer deferThreshold) {
        this.deferThreshold = deferThreshold;
        return this;
    }
}
