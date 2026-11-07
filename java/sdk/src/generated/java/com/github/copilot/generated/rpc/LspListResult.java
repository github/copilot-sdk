/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Runtime-owned configuration inventory and configuration diagnostics.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LspListResult(
    /** Effective servers in merge order. */
    @JsonProperty("configs") List<LspServerSummary> configs,
    /** User entries, including entries disabled by project configuration. */
    @JsonProperty("userServers") List<LspServerSummary> userServers,
    /** Effective plugin entries. */
    @JsonProperty("pluginServers") List<LspServerSummary> pluginServers,
    /** Project entries, including disabled entries absent from the effective set. */
    @JsonProperty("projectServers") List<LspServerSummary> projectServers,
    /** User configuration file path. */
    @JsonProperty("userConfigPath") String userConfigPath,
    /** Nearest project configuration file path. */
    @JsonProperty("projectConfigPath") String projectConfigPath,
    /** User configuration read or validation failure. */
    @JsonProperty("userConfigError") String userConfigError,
    /** Project files rejected by the loader. */
    @JsonProperty("projectProblems") List<LspConfigurationProblem> projectProblems,
    /** Failure to inspect project configuration. */
    @JsonProperty("projectInspectionError") String projectInspectionError,
    /** Identifiers disabled through the user toggle list. */
    @JsonProperty("userDisabledServers") List<String> userDisabledServers,
    /** Configuration loader diagnostics. */
    @JsonProperty("warnings") List<String> warnings,
    /** Plugin configuration diagnostics. */
    @JsonProperty("pluginWarnings") List<String> pluginWarnings
) {
}
