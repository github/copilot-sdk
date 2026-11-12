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
import javax.annotation.processing.Generated;

/**
 * Whether the gh-replaceable GitHub MCP tools may be clipped for the session described by the request.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpShouldExcludeGitHubToolsResult(
    /** True only when both halves hold: the session's filters still reach the platform shell tool, and the host actually has the `gh` those tools would be replaced by. Feed it straight back as the `excludeGhReplaceableTools` build option. */
    @JsonProperty("excludeGhReplaceableTools") Boolean excludeGhReplaceableTools
) {
}
