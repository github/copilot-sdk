/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataRecomputeContextTokensResult(
    /** Sum of tokens across chat-context and system-context messages currently held by the session. */
    @JsonProperty("totalTokens") Long totalTokens,
    /** Tokens contributed by user/assistant/tool messages (excludes system/developer prompts). */
    @JsonProperty("messagesTokenCount") Long messagesTokenCount,
    /** Tokens contributed by system/developer prompt snapshots. */
    @JsonProperty("systemTokenCount") Long systemTokenCount
) {
}
