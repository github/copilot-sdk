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
    /** HTTP request headers, preserving multiple values per name. */
    @JsonProperty("headers") Map<String, List<String>> headers,
    /** Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone. */
    @JsonProperty("transport") LlmInferenceHttpRequestStartTransport transport,
    /** Stable identity of the agent trajectory that issued this request. Present when the request originates from an agent turn; absent for requests outside any agent context. This is the same identity used by lifecycle and bridged session events and remains constant across turns and retries. */
    @JsonProperty("agentId") String agentId,
    /** Stable identity of the immediate parent trajectory. Present for child trajectories such as subagents and conversation-sampling requests; absent for root-agent and non-agent requests. */
    @JsonProperty("parentAgentId") String parentAgentId,
    /** Identity of the agent invocation (one agentic loop) that issued this request. It remains fixed across physical retries within the invocation and is distinct from the stable trajectory `agentId`. A caller-supplied invocation id always takes precedence (this covers auxiliary calls that have no model call id). Otherwise, first-party CAPI requests fall back to the runtime's agent task id — the same value the runtime emits as the `X-Agent-Task-Id` header — while custom-provider requests fall back to the model call id. */
    @JsonProperty("agentInvocationId") String agentInvocationId,
    /** Coarse classification of the interaction that produced this request. Open string for forward-compatibility; known values include `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, and `conversation-user`. Absent when the runtime did not classify the request. Comes from the runtime's per-request agent context independently of transport; on the CAPI transport the runtime derives the upstream `X-Interaction-Type` header from this same context. */
    @JsonProperty("interactionType") String interactionType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LlmInferenceHttpRequestStartRequest {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String requestId;
        private String sessionId;
        private String method;
        private String url;
        private Map<String, List<String>> headers;
        private LlmInferenceHttpRequestStartTransport transport;
        private String agentId;
        private String parentAgentId;
        private String agentInvocationId;
        private String interactionType;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestId(String value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code method} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder method(String value) {
            this.method = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(Map<String, List<String>> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(LlmInferenceHttpRequestStartTransport value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code agentId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentId(String value) {
            this.agentId = value;
            return this;
        }

        /**
         * Sets the {@code parentAgentId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder parentAgentId(String value) {
            this.parentAgentId = value;
            return this;
        }

        /**
         * Sets the {@code agentInvocationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentInvocationId(String value) {
            this.agentInvocationId = value;
            return this;
        }

        /**
         * Sets the {@code interactionType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder interactionType(String value) {
            this.interactionType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LlmInferenceHttpRequestStartRequest build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (method == null) throw new IllegalStateException("Missing required property: method");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            return new LlmInferenceHttpRequestStartRequest(requestId, sessionId, method, url, headers, transport, agentId, parentAgentId, agentInvocationId, interactionType);
        }
    }
}
