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
 * The session tool filters that decide whether the session still has a shell to run `gh` with.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpShouldExcludeGitHubToolsParams(
    /** The session's tool allowlist, when it set one. Omitted means the session constrains nothing this way. */
    @JsonProperty("availableTools") List<String> availableTools,
    /** The session's tool denylist, when it set one. Omitted means the session constrains nothing this way. */
    @JsonProperty("excludedTools") List<String> excludedTools,
    /** How the allowlist and denylist combine when both are set. Omitted means the default every session gets, so a caller that never chose a precedence is answered as its sessions behave. */
    @JsonProperty("toolFilterPrecedence") OptionsUpdateToolFilterPrecedence toolFilterPrecedence
) {
}
