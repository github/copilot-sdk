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
 * Evidence about the summarizer's latest model call when a compaction failed. Every value is a string so that new values stay readable by older runtimes.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteFailureDiagnostics(
    /** Failure reason of the summarizer's latest failed model call, kept when a later attempt or the compaction deadline ended the compaction. Uses the failureReason values. */
    @JsonProperty("lastAttemptFailureReason") String lastAttemptFailureReason,
    /** Error code the provider returned for that call, or its error type when it gave no code */
    @JsonProperty("providerErrorCode") String providerErrorCode,
    /** Bounded kind of that call's transport failure, such as dns, tls, proxy, connection_refused, connection_reset, connect_timeout, or timeout */
    @JsonProperty("transportErrorKind") String transportErrorKind,
    /** Finish reason of the summarizer's latest successful response, such as stop, length, or content_filter */
    @JsonProperty("finishReason") String finishReason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompactionCompleteFailureDiagnostics {}

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
        private String lastAttemptFailureReason;
        private String providerErrorCode;
        private String transportErrorKind;
        private String finishReason;

        /**
         * Sets the {@code lastAttemptFailureReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastAttemptFailureReason(String value) {
            this.lastAttemptFailureReason = value;
            return this;
        }

        /**
         * Sets the {@code providerErrorCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providerErrorCode(String value) {
            this.providerErrorCode = value;
            return this;
        }

        /**
         * Sets the {@code transportErrorKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transportErrorKind(String value) {
            this.transportErrorKind = value;
            return this;
        }

        /**
         * Sets the {@code finishReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder finishReason(String value) {
            this.finishReason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CompactionCompleteFailureDiagnostics build() {
            return new CompactionCompleteFailureDiagnostics(lastAttemptFailureReason, providerErrorCode, transportErrorKind, finishReason);
        }
    }
}
