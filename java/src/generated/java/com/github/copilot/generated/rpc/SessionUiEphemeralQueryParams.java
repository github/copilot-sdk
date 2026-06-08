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
 * Transient question to answer without adding it to conversation history.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiEphemeralQueryParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Question to answer from the current conversation context. */
    @JsonProperty("question") String question,
    /** In-process streaming callback `(text) => void` invoked with each token as the model emits it. Marked internal: excluded from the public SDK surface. In a process-separated SDK this is replaced by a streaming RPC that yields chunks and a final answer. */
    @JsonProperty("onChunk") Object onChunk,
    /** In-process `AbortSignal` forwarded to the model client to cancel an in-flight request. Marked internal: excluded from the public SDK surface. Replaced by an explicit cancellation token + cancel RPC in the SDK migration. */
    @JsonProperty("abortSignal") Object abortSignal
) {
}
