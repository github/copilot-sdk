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
 * Compaction settings for this session. Each call replaces the previous settings: an omitted field returns to its default. Settings live in memory until the session ends; re-apply them after resuming a session in a new process.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistorySetCompactionConfigParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model that summarizes this session's conversation when it compacts,
instead of the session's own model. Applies only when both are Copilot
models the session may use; for a BYOK or provider session, or a model
the session's catalog or allowed-model policy rejects, compaction
summarizes on the session's model. Takes precedence over any runtime
experiment that picks a compaction model. Blank values are ignored. */
    @JsonProperty("model") String model,
    /** Summarization instruction sent as the final user message of a
compaction request, replacing the built-in prompt entirely; the caller
owns the summary's format. `/compact` focus instructions and a note that
tools are unavailable are appended to it. Blank values are ignored. */
    @JsonProperty("prompt") String prompt,
    /** Reasoning effort for compaction requests: one of `none`, `minimal`,
`low`, `medium`, `high`, `xhigh` or `max`. Applies when the model that
summarizes supports it; otherwise the request keeps its usual effort.
Takes precedence over any runtime experiment that adjusts compaction
reasoning effort. Any other value is rejected. */
    @JsonProperty("reasoningEffort") String reasoningEffort
) {
}
