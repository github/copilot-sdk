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
 * Variant {@code unavailable} of {@link SessionLimitPredictionResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionLimitPredictionResultUnavailable extends SessionLimitPredictionResult {

    @JsonProperty("kind")
    private final String kind = "unavailable";

    @Override
    public String getKind() { return kind; }

    /** Reason no prediction is available. */
    @JsonProperty("reason")
    private SessionLimitPredictionUnavailableReason reason;

    public SessionLimitPredictionUnavailableReason getReason() { return reason; }
    public void setReason(SessionLimitPredictionUnavailableReason reason) { this.reason = reason; }


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
        private SessionLimitPredictionUnavailableReason reason;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(SessionLimitPredictionUnavailableReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionLimitPredictionResultUnavailable build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            SessionLimitPredictionResultUnavailable instance = new SessionLimitPredictionResultUnavailable();
            instance.setReason(reason);
            return instance;
        }
    }
}
