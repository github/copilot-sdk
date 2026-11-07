/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.error". Error details for timeline display including message and optional diagnostic information
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionErrorEvent extends SessionEvent {

    @Override
    public String getType() { return "session.error"; }

    @JsonProperty("data")
    private SessionErrorEventData data;

    public SessionErrorEventData getData() { return data; }
    public void setData(SessionErrorEventData data) { this.data = data; }

    /** Data payload for {@link SessionErrorEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionErrorEventData(
        /** Original root user-message ID of the failed request, including failures before user.message is emitted. Absent for session-wide errors or legacy hosts. */
        @JsonProperty("originatingMessageId") String originatingMessageId,
        /** Category of error (e.g., "authentication", "authorization", "policy", "quota", "rate_limit", "context_limit", "query") */
        @JsonProperty("errorType") String errorType,
        /** Fine-grained error code from the upstream provider, when available. For `errorType: "rate_limit"`, this is one of the `RateLimitErrorCode` values (e.g., `"user_weekly_rate_limited"`, `"user_global_rate_limited"`, `"rate_limited"`, `"user_model_rate_limited"`, `"integration_rate_limited"`). For `errorType: "quota"`, this is the CAPI quota error code (e.g., `"quota_exceeded"`, `"session_quota_exceeded"`, `"billing_not_configured"`). */
        @JsonProperty("errorCode") String errorCode,
        /** Provider-defined error type preserved from a structured upstream error envelope, when available (for example, `invalid_request_error`). */
        @JsonProperty("providerErrorType") String providerErrorType,
        /** Sanitized provider-defined error parameter preserved from a structured upstream error envelope, when available. */
        @JsonProperty("providerErrorParam") String providerErrorParam,
        /** Only set on `errorType: "rate_limit"`. When `true`, the runtime will follow this error with an `auto_mode_switch.requested` event (or silently switch if `continueOnAutoMode` is enabled). UI clients can use this flag to suppress duplicate rendering of the rate-limit error when they show their own auto-mode-switch prompt. */
        @JsonProperty("eligibleForAutoSwitch") Boolean eligibleForAutoSwitch,
        /** Human-readable error message */
        @JsonProperty("message") String message,
        /** What the user must do to recover, when the runtime knows of an action. The `message` never names a client affordance, so a client that offers one — a slash command, a settings pane, a link — renders it from this value. */
        @JsonProperty("remediation") RemediationAction remediation,
        /** Error stack trace, when available */
        @JsonProperty("stack") String stack,
        /** HTTP status code from the upstream request, if applicable */
        @JsonProperty("statusCode") Long statusCode,
        /** GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs */
        @JsonProperty("providerCallId") String providerCallId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Optional URL associated with this error that the user can open in a browser */
        @JsonProperty("url") String url
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionErrorEventData {}

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
            private String originatingMessageId;
            private String errorType;
            private String errorCode;
            private String providerErrorType;
            private String providerErrorParam;
            private Boolean eligibleForAutoSwitch;
            private String message;
            private RemediationAction remediation;
            private String stack;
            private Long statusCode;
            private String providerCallId;
            private String serviceRequestId;
            private String url;

            /**
             * Sets the {@code originatingMessageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder originatingMessageId(String value) {
                this.originatingMessageId = value;
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
             * Sets the {@code providerErrorType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerErrorType(String value) {
                this.providerErrorType = value;
                return this;
            }

            /**
             * Sets the {@code providerErrorParam} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerErrorParam(String value) {
                this.providerErrorParam = value;
                return this;
            }

            /**
             * Sets the {@code eligibleForAutoSwitch} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder eligibleForAutoSwitch(Boolean value) {
                this.eligibleForAutoSwitch = value;
                return this;
            }

            /**
             * Sets the {@code message} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder message(String value) {
                this.message = value;
                return this;
            }

            /**
             * Sets the {@code remediation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder remediation(RemediationAction value) {
                this.remediation = value;
                return this;
            }

            /**
             * Sets the {@code stack} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder stack(String value) {
                this.stack = value;
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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionErrorEventData build() {
                if (errorType == null) throw new IllegalStateException("Missing required property: errorType");
                if (message == null) throw new IllegalStateException("Missing required property: message");
                return new SessionErrorEventData(originatingMessageId, errorType, errorCode, providerErrorType, providerErrorParam, eligibleForAutoSwitch, message, remediation, stack, statusCode, providerCallId, serviceRequestId, url);
            }
        }
    }
}
