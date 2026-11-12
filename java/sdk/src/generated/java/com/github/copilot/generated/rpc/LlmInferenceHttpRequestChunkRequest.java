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
 * A request body chunk or cancellation signal.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmInferenceHttpRequestChunkRequest(
    /** Matches the requestId from the originating httpRequestStart frame. */
    @JsonProperty("requestId") String requestId,
    /** Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty. */
    @JsonProperty("data") String data,
    /** When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text. */
    @JsonProperty("binary") Boolean binary,
    /** When true, this is the final body chunk for the request. The SDK may rely on having received an end-marked chunk before treating the request body as complete. */
    @JsonProperty("end") Boolean end,
    /** When true, the runtime is cancelling the in-flight request (e.g. upstream consumer aborted). `data` is ignored. Implies end-of-request. */
    @JsonProperty("cancel") Boolean cancel,
    /** Optional human-readable reason for the cancellation, propagated for logging. */
    @JsonProperty("cancelReason") String cancelReason,
    /** Identity of the agent invocation (one agentic loop) this body chunk belongs to, matching the `agentInvocationId` semantics on httpRequestStart. Carried per chunk so a persistent transport can attribute successive turns correctly: when a WebSocket connection is reused across turns, the httpRequestStart identity reflects only the turn that opened the connection, so each later turn stamps its own invocation id here. Absent when the runtime has no invocation context for the request, or on the plain-HTTP transport where every request has its own httpRequestStart. */
    @JsonProperty("agentInvocationId") String agentInvocationId
) {
}
