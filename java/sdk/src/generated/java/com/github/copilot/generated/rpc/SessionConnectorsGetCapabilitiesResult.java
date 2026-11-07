/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Feature detection and hard polling limits for the EXPERIMENTAL session connector API.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionConnectorsGetCapabilitiesResult(
    /** Connector API contract version. */
    @JsonProperty("apiVersion") Long apiVersion,
    /** Current session availability. Disabled availability is reported without making a Connector request. */
    @JsonProperty("availability") ConnectorAvailability availability,
    /** Whether connect and reconnect can return an opaque continuation for bounded consent polling. */
    @JsonProperty("consentContinuation") Boolean consentContinuation,
    /** Whether callers select a host-owned GitHub account through an opaque selection ID rather than supplying a provider token. */
    @JsonProperty("opaqueAccountSelection") Boolean opaqueAccountSelection,
    /** Whether getAccount is supported. Absence means false. */
    @JsonProperty("sessionAccountSelection") Boolean sessionAccountSelection,
    /** Whether reconcile accepts forceConnectorName. Absence means false. */
    @JsonProperty("targetedReconcile") Boolean targetedReconcile,
    /** Maximum accepted polling attempts for one continuation call. */
    @JsonProperty("maxPollAttempts") Long maxPollAttempts,
    /** Maximum accepted delay in milliseconds between polling attempts. */
    @JsonProperty("maxPollIntervalMs") Long maxPollIntervalMs,
    /** Maximum accepted wall-clock deadline in milliseconds for one continuation call. */
    @JsonProperty("maxDeadlineMs") Long maxDeadlineMs
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionConnectorsGetCapabilitiesResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Long apiVersion;
        private ConnectorAvailability availability;
        private Boolean consentContinuation;
        private Boolean opaqueAccountSelection;
        private Boolean sessionAccountSelection;
        private Boolean targetedReconcile;
        private Long maxPollAttempts;
        private Long maxPollIntervalMs;
        private Long maxDeadlineMs;

        /**
         * Sets the {@code apiVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder apiVersion(Long value) {
            this.apiVersion = value;
            return this;
        }

        /**
         * Sets the {@code availability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder availability(ConnectorAvailability value) {
            this.availability = value;
            return this;
        }

        /**
         * Sets the {@code consentContinuation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder consentContinuation(Boolean value) {
            this.consentContinuation = value;
            return this;
        }

        /**
         * Sets the {@code opaqueAccountSelection} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder opaqueAccountSelection(Boolean value) {
            this.opaqueAccountSelection = value;
            return this;
        }

        /**
         * Sets the {@code sessionAccountSelection} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionAccountSelection(Boolean value) {
            this.sessionAccountSelection = value;
            return this;
        }

        /**
         * Sets the {@code targetedReconcile} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder targetedReconcile(Boolean value) {
            this.targetedReconcile = value;
            return this;
        }

        /**
         * Sets the {@code maxPollAttempts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxPollAttempts(Long value) {
            this.maxPollAttempts = value;
            return this;
        }

        /**
         * Sets the {@code maxPollIntervalMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxPollIntervalMs(Long value) {
            this.maxPollIntervalMs = value;
            return this;
        }

        /**
         * Sets the {@code maxDeadlineMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxDeadlineMs(Long value) {
            this.maxDeadlineMs = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionConnectorsGetCapabilitiesResult build() {
            if (apiVersion == null) throw new IllegalStateException("Missing required property: apiVersion");
            if (availability == null) throw new IllegalStateException("Missing required property: availability");
            if (consentContinuation == null) throw new IllegalStateException("Missing required property: consentContinuation");
            if (opaqueAccountSelection == null) throw new IllegalStateException("Missing required property: opaqueAccountSelection");
            if (maxPollAttempts == null) throw new IllegalStateException("Missing required property: maxPollAttempts");
            if (maxPollIntervalMs == null) throw new IllegalStateException("Missing required property: maxPollIntervalMs");
            if (maxDeadlineMs == null) throw new IllegalStateException("Missing required property: maxDeadlineMs");
            return new SessionConnectorsGetCapabilitiesResult(apiVersion, availability, consentContinuation, opaqueAccountSelection, sessionAccountSelection, targetedReconcile, maxPollAttempts, maxPollIntervalMs, maxDeadlineMs);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param apiVersion Connector API contract version.
     * @param availability Current session availability. Disabled availability is reported without making a Connector request.
     * @param consentContinuation Whether connect and reconnect can return an opaque continuation for bounded consent polling.
     * @param opaqueAccountSelection Whether callers select a host-owned GitHub account through an opaque selection ID rather than supplying a provider token.
     * @param maxPollAttempts Maximum accepted polling attempts for one continuation call.
     * @param maxPollIntervalMs Maximum accepted delay in milliseconds between polling attempts.
     * @param maxDeadlineMs Maximum accepted wall-clock deadline in milliseconds for one continuation call.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionConnectorsGetCapabilitiesResult(
        Long apiVersion,
        ConnectorAvailability availability,
        Boolean consentContinuation,
        Boolean opaqueAccountSelection,
        Long maxPollAttempts,
        Long maxPollIntervalMs,
        Long maxDeadlineMs
    ) {
        this(apiVersion, availability, consentContinuation, opaqueAccountSelection, null, null, maxPollAttempts, maxPollIntervalMs, maxDeadlineMs);
    }
}
