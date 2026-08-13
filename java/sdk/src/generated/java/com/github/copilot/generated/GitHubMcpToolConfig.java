/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Per-session configuration for the built-in GitHub MCP server
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubMcpToolConfig(
    /** Whether to use the read-write endpoint and request all toolsets */
    @JsonProperty("enableAllTools") Boolean enableAllTools,
    /** Additional GitHub MCP toolsets requested by the session */
    @JsonProperty("additionalToolsets") List<String> additionalToolsets,
    /** Additional GitHub MCP tools requested by the session */
    @JsonProperty("additionalTools") List<String> additionalTools,
    /** Whether to request the GitHub MCP insiders build */
    @JsonProperty("enableInsidersMode") Boolean enableInsidersMode
) {
}
