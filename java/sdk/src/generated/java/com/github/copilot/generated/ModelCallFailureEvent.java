/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "model.call_failure". Failed LLM API call metadata for telemetry
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallFailureEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_failure"; }

    @JsonProperty("data")
    private ModelCallFailureEventData data;

    public ModelCallFailureEventData getData() { return data; }
    public void setData(ModelCallFailureEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallFailureEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallFailureEventData(
        /** Model identifier used for the failed API call */
        @JsonProperty("model") String model,
        /** Parent task tool call ID when this failed model call belongs to a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls */
        @JsonProperty("initiator") String initiator,
        /** Completion ID from the model provider (e.g., chatcmpl-abc123) */
        @JsonProperty("apiCallId") String apiCallId,
        /** GitHub request tracing ID (x-github-request-id header) for server-side log correlation */
        @JsonProperty("providerCallId") String providerCallId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Internal correlation identifier for the qualifying instruction-update delivery consumed by this failed model request. */
        @JsonProperty("instructionUpdateDeliveryId") String instructionUpdateDeliveryId,
        /** Event identifier of the latest canonical instruction update included in this failed model request. */
        @JsonProperty("instructionUpdateLatestEventId") String instructionUpdateLatestEventId,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** HTTP status code from the failed request */
        @JsonProperty("statusCode") Long statusCode,
        /** Duration of the failed API call in milliseconds */
        @JsonProperty("durationMs") Long durationMs,
        /** API endpoint used for this model call, matching CAPI supported_endpoints vocabulary */
        @JsonProperty("apiEndpoint") AssistantUsageApiEndpoint apiEndpoint,
        /** Transport used for the failed model call (http or websocket) */
        @JsonProperty("transport") ModelCallFailureTransport transport,
        /** Whether this failed dispatched request selected the managed inference gateway. Absent for older producers or when no request was dispatched; not proof of gateway receipt. */
        @JsonProperty("inferenceGateway") Boolean inferenceGateway,
        /** Whether the failure originated from an API response or the request transport */
        @JsonProperty("failureKind") ModelCallFailureKind failureKind,
        /** Serialized (uncompressed) byte length of the failed request body, before any Content-Encoding. A content-free size signal. */
        @JsonProperty("requestBodyBytes") Long requestBodyBytes,
        /** Byte length of the failed request body actually sent on the wire, after any Content-Encoding. Equals requestBodyBytes when the body was not compressed. */
        @JsonProperty("requestBodyWireBytes") Long requestBodyWireBytes,
        /** Content-Encoding applied to the failed request body; identity when uncompressed */
        @JsonProperty("requestBodyEncoding") ModelCallRequestBodyEncoding requestBodyEncoding,
        /** Uncompressed fallback that produced this outcome; absent when the body was sent with the requested encoding */
        @JsonProperty("requestBodyCompressionFallback") ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback,
        /** Content-Encoding the request asked for before the uncompressed fallback; present only with requestBodyCompressionFallback */
        @JsonProperty("requestBodyCompressionFallbackFrom") ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom,
        /** HTTP status that rejected the compressed body before the uncompressed resend; present only when requestBodyCompressionFallback is retried */
        @JsonProperty("requestBodyCompressionRejectedStatus") Long requestBodyCompressionRejectedStatus,
        /** Zero-based orchestrator retry index of the failed attempt */
        @JsonProperty("retryAttempt") Long retryAttempt,
        /** Why a chat-completions stream that the server ended could not be assembled into a response, as a fixed snake_case reason such as `missing_finish_reason`, `missing_tool_call_id` or `no_chunks`; absent for other failures. New reasons may be added */
        @JsonProperty("incompleteStreamReason") String incompleteStreamReason,
        /** Whether the incomplete stream ended with the `[DONE]` sentinel; present only with incompleteStreamReason */
        @JsonProperty("streamDoneReceived") Boolean streamDoneReceived,
        /** Why the failed call was carried by the HTTP fallback of a WebSocket-capable dispatcher; absent when no fallback occurred */
        @JsonProperty("websocketFallbackReason") ModelCallWebSocketFallbackReason websocketFallbackReason,
        /** Milliseconds spent on the WebSocket attempt before falling back to HTTP */
        @JsonProperty("websocketFallbackAfterMs") Long websocketFallbackAfterMs,
        /** Normalized cause of the WebSocket failure that triggered the HTTP fallback; absent when no fallback occurred or the cause is unknown */
        @JsonProperty("websocketFallbackErrorKind") ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind,
        /** HTTP status of the WebSocket upgrade rejection or WebSocket API error that triggered the HTTP fallback */
        @JsonProperty("websocketFallbackStatusCode") Long websocketFallbackStatusCode,
        /** WebSocket close code received before the HTTP fallback */
        @JsonProperty("websocketFallbackCloseCode") Long websocketFallbackCloseCode,
        /** Whether a WebSocket reconnect was attempted before falling back to HTTP */
        @JsonProperty("websocketFallbackReconnectAttempted") Boolean websocketFallbackReconnectAttempted,
        /** Whether the HTTP fallback started during this model call, rather than being inherited from an earlier call */
        @JsonProperty("websocketFallbackStartedThisCall") Boolean websocketFallbackStartedThisCall,
        /** Effective maximum prompt-token limit for the failed call */
        @JsonProperty("maxPromptTokens") Long maxPromptTokens,
        /** Effective maximum output-token limit for the failed call */
        @JsonProperty("maxOutputTokens") Long maxOutputTokens,
        /** Whether the failed call used a bring-your-own-key provider */
        @JsonProperty("isByok") Boolean isByok,
        /** Where the bring-your-own-key model for the failed call runs and who manages it: "local_managed" (on the device, managed by Copilot), "local_user" (on the device, managed by the user), or "remote_user" (off the device, managed by the user). Absent for Copilot-served models. */
        @JsonProperty("byokKind") String byokKind,
        /** Fixed-set provider family serving the bring-your-own-key model for the failed call (for example "openai", "anthropic", "azure_openai", "ollama", "llama_cpp", or "other"). Never the caller-supplied provider name. Absent for Copilot-served models. */
        @JsonProperty("modelProvider") String modelProvider,
        /** Fixed-set configured provider type of the bring-your-own-key model for the failed call: "openai", "azure", "anthropic", "local_model" (a user-configured local model), "winml" (a Copilot-managed Windows ML model), or "other". Never the caller-supplied provider name. Absent for Copilot-served models. */
        @JsonProperty("providerType") String providerType,
        /** Whether the session selected Auto mode for the failed call */
        @JsonProperty("isAuto") Boolean isAuto,
        /** Reasoning effort level used for the failed model call, if applicable */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Authoritative interaction classification for the failed call, matching `assistant.usage.interactionType` (for example `conversation-agent`, `conversation-subagent`, or `conversation-sampling`). Absent when the producer cannot classify the interaction. */
        @JsonProperty("interactionType") String interactionType,
        /** Where the failed model call originated */
        @JsonProperty("source") ModelCallFailureSource source,
        /** Raw provider/runtime error message for restricted telemetry */
        @JsonProperty("errorMessage") String errorMessage,
        /** For HTTP 400 failures only: whether the response carried a structured CAPI error envelope (structured_error, a deterministic validation failure) or no error body (bodyless, the transient gateway/proxy signature). Absent for non-400 failures. */
        @JsonProperty("badRequestKind") ModelCallFailureBadRequestKind badRequestKind,
        /** The `code` from the CAPI error envelope of an HTTP failure of any status (e.g. 'model_max_prompt_tokens_exceeded' on a 400, 'model_overloaded' on a 5xx) identifying which upstream failure occurred. Raw server-controlled string, emitted only through restricted telemetry. Absent when the response carried no error envelope. */
        @JsonProperty("errorCode") String errorCode,
        /** The `type` from the CAPI error envelope of an HTTP failure of any status (e.g. 'websocket_error'), a coarser companion to errorCode for envelopes that carry no code. Raw server-controlled string, emitted only through restricted telemetry. Absent when the response carried no error envelope. */
        @JsonProperty("errorType") String errorType,
        /** Bounded product-state failure classified before telemetry projection. */
        @JsonProperty("productStateError") ModelCallProductStateError productStateError,
        /** Per-quota usage snapshots parsed from the failed response's quota headers, keyed by quota identifier. Present when the error response carried quota headers (e.g. a 402 once the additional spend limit is reached) so the UI can refresh the quota display on failure. */
        @JsonProperty("quotaSnapshots") Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots,
        /** Content-free structural summary of the failing request. Contains only counts and shape flags (no prompt content), so it is safe for unrestricted telemetry. Populated only for client-error (4xx) failures. */
        @JsonProperty("requestFingerprint") ModelCallFailureRequestFingerprint requestFingerprint,
        /** Experimental HydraFusion attribution for this failed concrete model call. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param model Model identifier used for the failed API call
         * @param parentToolCallId Parent task tool call ID when this failed model call belongs to a sub-agent
         * @param initiator What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
         * @param apiCallId Completion ID from the model provider (e.g., chatcmpl-abc123)
         * @param providerCallId GitHub request tracing ID (x-github-request-id header) for server-side log correlation
         * @param serviceRequestId Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation
         * @param rte Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable.
         * @param statusCode HTTP status code from the failed request
         * @param durationMs Duration of the failed API call in milliseconds
         * @param apiEndpoint API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
         * @param transport Transport used for the failed model call (http or websocket)
         * @param failureKind Whether the failure originated from an API response or the request transport
         * @param requestBodyBytes Serialized (uncompressed) byte length of the failed request body, before any Content-Encoding. A content-free size signal.
         * @param requestBodyWireBytes Byte length of the failed request body actually sent on the wire, after any Content-Encoding. Equals requestBodyBytes when the body was not compressed.
         * @param requestBodyEncoding Content-Encoding applied to the failed request body; identity when uncompressed
         * @param requestBodyCompressionFallback Uncompressed fallback that produced this outcome; absent when the body was sent with the requested encoding
         * @param requestBodyCompressionFallbackFrom Content-Encoding the request asked for before the uncompressed fallback; present only with requestBodyCompressionFallback
         * @param requestBodyCompressionRejectedStatus HTTP status that rejected the compressed body before the uncompressed resend; present only when requestBodyCompressionFallback is retried
         * @param retryAttempt Zero-based orchestrator retry index of the failed attempt
         * @param websocketFallbackReason Why the failed call was carried by the HTTP fallback of a WebSocket-capable dispatcher; absent when no fallback occurred
         * @param websocketFallbackAfterMs Milliseconds spent on the WebSocket attempt before falling back to HTTP
         * @param websocketFallbackErrorKind Normalized cause of the WebSocket failure that triggered the HTTP fallback; absent when no fallback occurred or the cause is unknown
         * @param websocketFallbackStatusCode HTTP status of the WebSocket upgrade rejection or WebSocket API error that triggered the HTTP fallback
         * @param websocketFallbackCloseCode WebSocket close code received before the HTTP fallback
         * @param websocketFallbackReconnectAttempted Whether a WebSocket reconnect was attempted before falling back to HTTP
         * @param websocketFallbackStartedThisCall Whether the HTTP fallback started during this model call, rather than being inherited from an earlier call
         * @param maxPromptTokens Effective maximum prompt-token limit for the failed call
         * @param maxOutputTokens Effective maximum output-token limit for the failed call
         * @param isByok Whether the failed call used a bring-your-own-key provider
         * @param byokKind Where the bring-your-own-key model for the failed call runs and who manages it: "local_managed" (on the device, managed by Copilot), "local_user" (on the device, managed by the user), or "remote_user" (off the device, managed by the user). Absent for Copilot-served models.
         * @param modelProvider Fixed-set provider family serving the bring-your-own-key model for the failed call (for example "openai", "anthropic", "azure_openai", "ollama", "llama_cpp", or "other"). Never the caller-supplied provider name. Absent for Copilot-served models.
         * @param providerType Fixed-set configured provider type of the bring-your-own-key model for the failed call: "openai", "azure", "anthropic", "local_model" (a user-configured local model), "winml" (a Copilot-managed Windows ML model), or "other". Never the caller-supplied provider name. Absent for Copilot-served models.
         * @param isAuto Whether the session selected Auto mode for the failed call
         * @param reasoningEffort Reasoning effort level used for the failed model call, if applicable
         * @param interactionType Authoritative interaction classification for the failed call, matching `assistant.usage.interactionType` (for example `conversation-agent`, `conversation-subagent`, or `conversation-sampling`). Absent when the producer cannot classify the interaction.
         * @param source Where the failed model call originated
         * @param errorMessage Raw provider/runtime error message for restricted telemetry
         * @param badRequestKind For HTTP 400 failures only: whether the response carried a structured CAPI error envelope (structured_error, a deterministic validation failure) or no error body (bodyless, the transient gateway/proxy signature). Absent for non-400 failures.
         * @param errorCode The `code` from the CAPI error envelope of an HTTP failure of any status (e.g. 'model_max_prompt_tokens_exceeded' on a 400, 'model_overloaded' on a 5xx) identifying which upstream failure occurred. Raw server-controlled string, emitted only through restricted telemetry. Absent when the response carried no error envelope.
         * @param errorType The `type` from the CAPI error envelope of an HTTP failure of any status (e.g. 'websocket_error'), a coarser companion to errorCode for envelopes that carry no code. Raw server-controlled string, emitted only through restricted telemetry. Absent when the response carried no error envelope.
         * @param quotaSnapshots Per-quota usage snapshots parsed from the failed response's quota headers, keyed by quota identifier. Present when the error response carried quota headers (e.g. a 402 once the additional spend limit is reached) so the UI can refresh the quota display on failure.
         * @param requestFingerprint Content-free structural summary of the failing request. Contains only counts and shape flags (no prompt content), so it is safe for unrestricted telemetry. Populated only for client-error (4xx) failures.
         * @param fusion Experimental HydraFusion attribution for this failed concrete model call.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ModelCallFailureEventData(
            String model,
            String parentToolCallId,
            String initiator,
            String apiCallId,
            String providerCallId,
            String serviceRequestId,
            Boolean rte,
            Long statusCode,
            Long durationMs,
            AssistantUsageApiEndpoint apiEndpoint,
            ModelCallFailureTransport transport,
            ModelCallFailureKind failureKind,
            Long requestBodyBytes,
            Long requestBodyWireBytes,
            ModelCallRequestBodyEncoding requestBodyEncoding,
            ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback,
            ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom,
            Long requestBodyCompressionRejectedStatus,
            Long retryAttempt,
            ModelCallWebSocketFallbackReason websocketFallbackReason,
            Long websocketFallbackAfterMs,
            ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind,
            Long websocketFallbackStatusCode,
            Long websocketFallbackCloseCode,
            Boolean websocketFallbackReconnectAttempted,
            Boolean websocketFallbackStartedThisCall,
            Long maxPromptTokens,
            Long maxOutputTokens,
            Boolean isByok,
            String byokKind,
            String modelProvider,
            String providerType,
            Boolean isAuto,
            String reasoningEffort,
            String interactionType,
            ModelCallFailureSource source,
            String errorMessage,
            ModelCallFailureBadRequestKind badRequestKind,
            String errorCode,
            String errorType,
            Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots,
            ModelCallFailureRequestFingerprint requestFingerprint,
            FusionAttribution fusion
        ) {
            this(model, parentToolCallId, initiator, apiCallId, providerCallId, serviceRequestId, null, null, rte, statusCode, durationMs, apiEndpoint, transport, null, failureKind, requestBodyBytes, requestBodyWireBytes, requestBodyEncoding, requestBodyCompressionFallback, requestBodyCompressionFallbackFrom, requestBodyCompressionRejectedStatus, retryAttempt, null, null, websocketFallbackReason, websocketFallbackAfterMs, websocketFallbackErrorKind, websocketFallbackStatusCode, websocketFallbackCloseCode, websocketFallbackReconnectAttempted, websocketFallbackStartedThisCall, maxPromptTokens, maxOutputTokens, isByok, byokKind, modelProvider, providerType, isAuto, reasoningEffort, interactionType, source, errorMessage, badRequestKind, errorCode, errorType, null, quotaSnapshots, requestFingerprint, fusion);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ModelCallFailureEventData {}

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
            private String model;
            private String parentToolCallId;
            private String initiator;
            private String apiCallId;
            private String providerCallId;
            private String serviceRequestId;
            private String instructionUpdateDeliveryId;
            private String instructionUpdateLatestEventId;
            private Boolean rte;
            private Long statusCode;
            private Long durationMs;
            private AssistantUsageApiEndpoint apiEndpoint;
            private ModelCallFailureTransport transport;
            private Boolean inferenceGateway;
            private ModelCallFailureKind failureKind;
            private Long requestBodyBytes;
            private Long requestBodyWireBytes;
            private ModelCallRequestBodyEncoding requestBodyEncoding;
            private ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback;
            private ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom;
            private Long requestBodyCompressionRejectedStatus;
            private Long retryAttempt;
            private String incompleteStreamReason;
            private Boolean streamDoneReceived;
            private ModelCallWebSocketFallbackReason websocketFallbackReason;
            private Long websocketFallbackAfterMs;
            private ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind;
            private Long websocketFallbackStatusCode;
            private Long websocketFallbackCloseCode;
            private Boolean websocketFallbackReconnectAttempted;
            private Boolean websocketFallbackStartedThisCall;
            private Long maxPromptTokens;
            private Long maxOutputTokens;
            private Boolean isByok;
            private String byokKind;
            private String modelProvider;
            private String providerType;
            private Boolean isAuto;
            private String reasoningEffort;
            private String interactionType;
            private ModelCallFailureSource source;
            private String errorMessage;
            private ModelCallFailureBadRequestKind badRequestKind;
            private String errorCode;
            private String errorType;
            private ModelCallProductStateError productStateError;
            private Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots;
            private ModelCallFailureRequestFingerprint requestFingerprint;
            private FusionAttribution fusion;

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
                return this;
            }

            /**
             * Sets the {@code initiator} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder initiator(String value) {
                this.initiator = value;
                return this;
            }

            /**
             * Sets the {@code apiCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiCallId(String value) {
                this.apiCallId = value;
                return this;
            }

            /**
             * Sets the {@code providerCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerCallId(String value) {
                this.providerCallId = value;
                return this;
            }

            /**
             * Sets the {@code serviceRequestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serviceRequestId(String value) {
                this.serviceRequestId = value;
                return this;
            }

            /**
             * Sets the {@code instructionUpdateDeliveryId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder instructionUpdateDeliveryId(String value) {
                this.instructionUpdateDeliveryId = value;
                return this;
            }

            /**
             * Sets the {@code instructionUpdateLatestEventId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder instructionUpdateLatestEventId(String value) {
                this.instructionUpdateLatestEventId = value;
                return this;
            }

            /**
             * Sets the {@code rte} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rte(Boolean value) {
                this.rte = value;
                return this;
            }

            /**
             * Sets the {@code statusCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder statusCode(Long value) {
                this.statusCode = value;
                return this;
            }

            /**
             * Sets the {@code durationMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder durationMs(Long value) {
                this.durationMs = value;
                return this;
            }

            /**
             * Sets the {@code apiEndpoint} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiEndpoint(AssistantUsageApiEndpoint value) {
                this.apiEndpoint = value;
                return this;
            }

            /**
             * Sets the {@code transport} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder transport(ModelCallFailureTransport value) {
                this.transport = value;
                return this;
            }

            /**
             * Sets the {@code inferenceGateway} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder inferenceGateway(Boolean value) {
                this.inferenceGateway = value;
                return this;
            }

            /**
             * Sets the {@code failureKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder failureKind(ModelCallFailureKind value) {
                this.failureKind = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyBytes(Long value) {
                this.requestBodyBytes = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyWireBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyWireBytes(Long value) {
                this.requestBodyWireBytes = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyEncoding} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyEncoding(ModelCallRequestBodyEncoding value) {
                this.requestBodyEncoding = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyCompressionFallback} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyCompressionFallback(ModelCallRequestBodyCompressionFallback value) {
                this.requestBodyCompressionFallback = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyCompressionFallbackFrom} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyCompressionFallbackFrom(ModelCallRequestBodyEncoding value) {
                this.requestBodyCompressionFallbackFrom = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyCompressionRejectedStatus} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyCompressionRejectedStatus(Long value) {
                this.requestBodyCompressionRejectedStatus = value;
                return this;
            }

            /**
             * Sets the {@code retryAttempt} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder retryAttempt(Long value) {
                this.retryAttempt = value;
                return this;
            }

            /**
             * Sets the {@code incompleteStreamReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder incompleteStreamReason(String value) {
                this.incompleteStreamReason = value;
                return this;
            }

            /**
             * Sets the {@code streamDoneReceived} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder streamDoneReceived(Boolean value) {
                this.streamDoneReceived = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackReason(ModelCallWebSocketFallbackReason value) {
                this.websocketFallbackReason = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackAfterMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackAfterMs(Long value) {
                this.websocketFallbackAfterMs = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackErrorKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackErrorKind(ModelCallWebSocketFallbackErrorKind value) {
                this.websocketFallbackErrorKind = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackStatusCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackStatusCode(Long value) {
                this.websocketFallbackStatusCode = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackCloseCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackCloseCode(Long value) {
                this.websocketFallbackCloseCode = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackReconnectAttempted} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackReconnectAttempted(Boolean value) {
                this.websocketFallbackReconnectAttempted = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackStartedThisCall} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackStartedThisCall(Boolean value) {
                this.websocketFallbackStartedThisCall = value;
                return this;
            }

            /**
             * Sets the {@code maxPromptTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder maxPromptTokens(Long value) {
                this.maxPromptTokens = value;
                return this;
            }

            /**
             * Sets the {@code maxOutputTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder maxOutputTokens(Long value) {
                this.maxOutputTokens = value;
                return this;
            }

            /**
             * Sets the {@code isByok} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isByok(Boolean value) {
                this.isByok = value;
                return this;
            }

            /**
             * Sets the {@code byokKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder byokKind(String value) {
                this.byokKind = value;
                return this;
            }

            /**
             * Sets the {@code modelProvider} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelProvider(String value) {
                this.modelProvider = value;
                return this;
            }

            /**
             * Sets the {@code providerType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerType(String value) {
                this.providerType = value;
                return this;
            }

            /**
             * Sets the {@code isAuto} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isAuto(Boolean value) {
                this.isAuto = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
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
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(ModelCallFailureSource value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code errorMessage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorMessage(String value) {
                this.errorMessage = value;
                return this;
            }

            /**
             * Sets the {@code badRequestKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder badRequestKind(ModelCallFailureBadRequestKind value) {
                this.badRequestKind = value;
                return this;
            }

            /**
             * Sets the {@code errorCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorCode(String value) {
                this.errorCode = value;
                return this;
            }

            /**
             * Sets the {@code errorType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorType(String value) {
                this.errorType = value;
                return this;
            }

            /**
             * Sets the {@code productStateError} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder productStateError(ModelCallProductStateError value) {
                this.productStateError = value;
                return this;
            }

            /**
             * Sets the {@code quotaSnapshots} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder quotaSnapshots(Map<String, AssistantUsageQuotaSnapshot> value) {
                this.quotaSnapshots = value;
                return this;
            }

            /**
             * Sets the {@code requestFingerprint} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestFingerprint(ModelCallFailureRequestFingerprint value) {
                this.requestFingerprint = value;
                return this;
            }

            /**
             * Sets the {@code fusion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusion(FusionAttribution value) {
                this.fusion = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ModelCallFailureEventData build() {
                if (source == null) throw new IllegalStateException("Missing required property: source");
                return new ModelCallFailureEventData(model, parentToolCallId, initiator, apiCallId, providerCallId, serviceRequestId, instructionUpdateDeliveryId, instructionUpdateLatestEventId, rte, statusCode, durationMs, apiEndpoint, transport, inferenceGateway, failureKind, requestBodyBytes, requestBodyWireBytes, requestBodyEncoding, requestBodyCompressionFallback, requestBodyCompressionFallbackFrom, requestBodyCompressionRejectedStatus, retryAttempt, incompleteStreamReason, streamDoneReceived, websocketFallbackReason, websocketFallbackAfterMs, websocketFallbackErrorKind, websocketFallbackStatusCode, websocketFallbackCloseCode, websocketFallbackReconnectAttempted, websocketFallbackStartedThisCall, maxPromptTokens, maxOutputTokens, isByok, byokKind, modelProvider, providerType, isAuto, reasoningEffort, interactionType, source, errorMessage, badRequestKind, errorCode, errorType, productStateError, quotaSnapshots, requestFingerprint, fusion);
            }
        }
    }
}
