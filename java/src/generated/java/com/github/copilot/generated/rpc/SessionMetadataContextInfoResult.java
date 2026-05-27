/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Token breakdown for the session's current context window, or null if uninitialized.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataContextInfoResult(
    /** Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached). */
    @JsonProperty("contextInfo") SessionMetadataContextInfoResultContextInfo contextInfo
) {

    /** Token-usage breakdown for the session's current context window */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMetadataContextInfoResultContextInfo(
        /** The model used for token counting */
        @JsonProperty("modelName") String modelName,
        /** Tokens consumed by the system prompt */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Tokens consumed by user/assistant/tool messages */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Tokens consumed by tool definitions sent to the model (excludes deferred tools) */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens,
        /** Sum of system, conversation and tool-definition tokens */
        @JsonProperty("totalTokens") Long totalTokens,
        /** Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified) */
        @JsonProperty("promptTokenLimit") Long promptTokenLimit,
        /** Token count at which background compaction starts (configurable percentage of promptTokenLimit) */
        @JsonProperty("compactionThreshold") Long compactionThreshold,
        /** Total context limit for /context display. promptTokenLimit + min(32k or 64k, outputTokenLimit) depending on model. */
        @JsonProperty("limit") Long limit,
        /** Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%) */
        @JsonProperty("bufferTokens") Long bufferTokens
    ) {
    }
}
