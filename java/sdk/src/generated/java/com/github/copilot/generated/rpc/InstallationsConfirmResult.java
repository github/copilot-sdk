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
 * A response is meaningful only on the connection and request that issued its challenge.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallationsConfirmResult(
    /** Exact challenge from the request. */
    @JsonProperty("confirmationId") String confirmationId,
    /** Exact review commitment from the request. */
    @JsonProperty("reviewFingerprint") String reviewFingerprint,
    /** Fresh explicit user decision. There is no default. */
    @JsonProperty("decision") InstallationDecision decision
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstallationsConfirmResult {}

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
        private String confirmationId;
        private String reviewFingerprint;
        private InstallationDecision decision;

        /**
         * Sets the {@code confirmationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder confirmationId(String value) {
            this.confirmationId = value;
            return this;
        }

        /**
         * Sets the {@code reviewFingerprint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reviewFingerprint(String value) {
            this.reviewFingerprint = value;
            return this;
        }

        /**
         * Sets the {@code decision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder decision(InstallationDecision value) {
            this.decision = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstallationsConfirmResult build() {
            if (confirmationId == null) throw new IllegalStateException("Missing required property: confirmationId");
            if (reviewFingerprint == null) throw new IllegalStateException("Missing required property: reviewFingerprint");
            if (decision == null) throw new IllegalStateException("Missing required property: decision");
            return new InstallationsConfirmResult(confirmationId, reviewFingerprint, decision);
        }
    }
}
