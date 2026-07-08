/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The head of an outbound model-layer HTTP request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmInferenceHttpRequestStartRequest(
    /** Opaque runtime-minted id, unique per in-flight request. The SDK uses this to correlate httpRequestChunk frames and to address its httpResponseStart / httpResponseChunk replies back to the runtime. */
    @JsonProperty("requestId") String requestId,
    /** Id of the runtime session that triggered this request, when one is in scope. Absent for requests issued outside any session (e.g. startup model-catalog or capability resolution). This is a payload field — not a dispatch key — because the client-global API is registered process-wide rather than per session. */
    @JsonProperty("sessionId") String sessionId,
    /** HTTP method, e.g. GET, POST. */
    @JsonProperty("method") String method,
    /** Absolute request URL. */
    @JsonProperty("url") String url,
    @JsonProperty("headers") Map<String, List<String>> headers,
    /** Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone. */
    @JsonProperty("transport") LlmInferenceHttpRequestStartTransport transport,
    /** Stable per-agent-instance id attributing this request to a specific agent trajectory. Present when the request originates from an agent turn; absent for requests issued outside any agent context (e.g. some SDK callers). A request with an `agentId` but no `parentAgentId` is a root-agent request; one carrying both is a subagent request. Sourced from the runtime's per-request agent context and surfaced on the envelope independently of transport, so it is available for both first-party (CAPI) and BYOK/custom-provider requests; on the CAPI transport the runtime derives the upstream `X-Agent-Task-Id` header from this same context. Consumers routing each provider call to a training trajectory should key on this rather than on lifecycle events, since it is available on the request path before sampling. */
    @JsonProperty("agentId") String agentId,
    /** Id of the parent agent that spawned the agent issuing this request. Present only for subagent requests; absent for root-agent requests and non-agent requests. Combined with `agentId`, this lets consumers attribute a call to a child trajectory versus the root. Like `agentId`, it comes from the runtime's per-request agent context independently of transport; on the CAPI transport the runtime derives the upstream `X-Parent-Agent-Id` header from this same context. */
    @JsonProperty("parentAgentId") String parentAgentId,
    /** Coarse classification of the interaction that produced this request. Open string for forward-compatibility; known values include `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, and `conversation-user`. Absent when the runtime did not classify the request. Comes from the runtime's per-request agent context independently of transport; on the CAPI transport the runtime derives the upstream `X-Interaction-Type` header from this same context. */
    @JsonProperty("interactionType") String interactionType
) {
}
