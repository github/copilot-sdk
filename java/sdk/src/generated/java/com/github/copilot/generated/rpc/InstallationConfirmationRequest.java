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
 * One connection-owned, expiring request for a trusted host's explicit user decision.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallationConfirmationRequest(
    /** Original engine-resolved selector for a bound operation, never a dispatch default.
Bound MCP confirmation always includes it; correlate it with the original pending action. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Opaque one-use challenge. Return unchanged; never log or persist. */
    @JsonProperty("confirmationId") String confirmationId,
    /** Random identifier of this installation operation, not a plan handle. */
    @JsonProperty("operationId") String operationId,
    /** Original plan expiry as an ISO 8601 timestamp. Confirmation never extends it. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Opaque commitment to the exact review and inputs. Return unchanged; never log. */
    @JsonProperty("reviewFingerprint") String reviewFingerprint,
    /** Resource-specific review to present before collecting the user's decision. */
    @JsonProperty("review") InstallationReview review
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstallationConfirmationRequest {}

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
        private String policySessionId;
        private String confirmationId;
        private String operationId;
        private String expiresAt;
        private String reviewFingerprint;
        private InstallationReview review;

        /**
         * Sets the {@code policySessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policySessionId(String value) {
            this.policySessionId = value;
            return this;
        }

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
         * Sets the {@code operationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operationId(String value) {
            this.operationId = value;
            return this;
        }

        /**
         * Sets the {@code expiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAt(String value) {
            this.expiresAt = value;
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
         * Sets the {@code review} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder review(InstallationReview value) {
            this.review = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstallationConfirmationRequest build() {
            if (confirmationId == null) throw new IllegalStateException("Missing required property: confirmationId");
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (expiresAt == null) throw new IllegalStateException("Missing required property: expiresAt");
            if (reviewFingerprint == null) throw new IllegalStateException("Missing required property: reviewFingerprint");
            if (review == null) throw new IllegalStateException("Missing required property: review");
            return new InstallationConfirmationRequest(policySessionId, confirmationId, operationId, expiresAt, reviewFingerprint, review);
        }
    }
}
