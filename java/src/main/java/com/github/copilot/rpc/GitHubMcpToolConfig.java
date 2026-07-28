/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for the built-in GitHub MCP server.
 *
 * <p>{@code disableFormDeferral} only applies to the built-in GitHub MCP server
 * and only has an effect when MCP Apps and form-backed GitHub tools are enabled.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GitHubMcpToolConfig {

    @JsonProperty("enableAllTools")
    private Boolean enableAllTools;

    @JsonProperty("additionalToolsets")
    private List<String> additionalToolsets;

    @JsonProperty("additionalTools")
    private List<String> additionalTools;

    @JsonProperty("enableInsidersMode")
    private Boolean enableInsidersMode;

    @JsonProperty("disableFormDeferral")
    private Boolean disableFormDeferral;

    public Boolean getEnableAllTools() {
        return enableAllTools;
    }

    public GitHubMcpToolConfig setEnableAllTools(Boolean enableAllTools) {
        this.enableAllTools = enableAllTools;
        return this;
    }

    public List<String> getAdditionalToolsets() {
        return additionalToolsets;
    }

    public GitHubMcpToolConfig setAdditionalToolsets(List<String> additionalToolsets) {
        this.additionalToolsets = additionalToolsets;
        return this;
    }

    public List<String> getAdditionalTools() {
        return additionalTools;
    }

    public GitHubMcpToolConfig setAdditionalTools(List<String> additionalTools) {
        this.additionalTools = additionalTools;
        return this;
    }

    public Boolean getEnableInsidersMode() {
        return enableInsidersMode;
    }

    public GitHubMcpToolConfig setEnableInsidersMode(Boolean enableInsidersMode) {
        this.enableInsidersMode = enableInsidersMode;
        return this;
    }

    public Boolean getDisableFormDeferral() {
        return disableFormDeferral;
    }

    public GitHubMcpToolConfig setDisableFormDeferral(Boolean disableFormDeferral) {
        this.disableFormDeferral = disableFormDeferral;
        return this;
    }
}
