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
 * Optional informational context describing how and where the permission decision was made. This does not affect permission behavior.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionDecisionContext(
    /** Disposition of the permission request as observed by the responding client. */
    @JsonProperty("outcome") PermissionDecisionOutcome outcome,
    /** Controlled reason or actor responsible for the response. */
    @JsonProperty("source") PermissionDecisionSource source,
    /** Client surface that submitted the response. */
    @JsonProperty("surface") PermissionDecisionSurface surface,
    /** Whether the responding client could ask a user interactively, was running headlessly, or had no response path. Omit when the client cannot determine this authoritatively. */
    @JsonProperty("responseCapability") PermissionResponseCapability responseCapability,
    /** Optional user-authored explanation of this decision, independent of approval scope. Collected only in restricted telemetry for accepted human responses to Assisted Permissions prompts. Never persisted in session history, sent to the agent or judge, or used to create approval rules. Existing rejection feedback is separate. */
    @JsonProperty("userFeedback") String userFeedback
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionDecisionContext {}

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
        private PermissionDecisionOutcome outcome;
        private PermissionDecisionSource source;
        private PermissionDecisionSurface surface;
        private PermissionResponseCapability responseCapability;
        private String userFeedback;

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcome(PermissionDecisionOutcome value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(PermissionDecisionSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code surface} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder surface(PermissionDecisionSurface value) {
            this.surface = value;
            return this;
        }

        /**
         * Sets the {@code responseCapability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder responseCapability(PermissionResponseCapability value) {
            this.responseCapability = value;
            return this;
        }

        /**
         * Sets the {@code userFeedback} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userFeedback(String value) {
            this.userFeedback = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionDecisionContext build() {
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (surface == null) throw new IllegalStateException("Missing required property: surface");
            return new PermissionDecisionContext(outcome, source, surface, responseCapability, userFeedback);
        }
    }
}
