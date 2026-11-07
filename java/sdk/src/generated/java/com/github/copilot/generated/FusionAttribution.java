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
import javax.annotation.processing.Generated;

/**
 * Experimental attribution linking an ordinary event to the HydraFusion turn, phase, and concrete source that produced it.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionAttribution(
    /** Whether this model request consumed a user steering message rather than only internal Fusion work. */
    @JsonProperty("hasUserSteering") Boolean hasUserSteering,
    /** Stable identifier for the HydraFusion turn that produced the event. */
    @JsonProperty("fusionId") String fusionId,
    /** Idempotency identifier for the authoritative commit, when the event belongs to the selected output. */
    @JsonProperty("commitId") String commitId,
    /** Synthetic HydraFusion model selected for the session. */
    @JsonProperty("syntheticModel") String syntheticModel,
    /** HydraFusion routing policy used for the turn. */
    @JsonProperty("policy") String policy,
    /** HydraFusion orchestration pattern selected for the turn. */
    @JsonProperty("pattern") String pattern,
    /** Identifier of the concrete phase that produced the event. */
    @JsonProperty("phaseId") String phaseId,
    /** Kind of concrete phase that produced the event. */
    @JsonProperty("phaseKind") String phaseKind,
    /** Semantic role assigned to the concrete phase. */
    @JsonProperty("role") String role,
    /** Concrete model that produced the attributed event. */
    @JsonProperty("sourceModel") String sourceModel,
    /** Conversation scope in which the concrete phase executed. */
    @JsonProperty("conversationScope") String conversationScope,
    /** Phase whose output supplied the authoritative content, when different from the executing phase. */
    @JsonProperty("sourcePhaseId") String sourcePhaseId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FusionAttribution {}

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
        private Boolean hasUserSteering;
        private String fusionId;
        private String commitId;
        private String syntheticModel;
        private String policy;
        private String pattern;
        private String phaseId;
        private String phaseKind;
        private String role;
        private String sourceModel;
        private String conversationScope;
        private String sourcePhaseId;

        /**
         * Sets the {@code hasUserSteering} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasUserSteering(Boolean value) {
            this.hasUserSteering = value;
            return this;
        }

        /**
         * Sets the {@code fusionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder fusionId(String value) {
            this.fusionId = value;
            return this;
        }

        /**
         * Sets the {@code commitId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder commitId(String value) {
            this.commitId = value;
            return this;
        }

        /**
         * Sets the {@code syntheticModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder syntheticModel(String value) {
            this.syntheticModel = value;
            return this;
        }

        /**
         * Sets the {@code policy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder policy(String value) {
            this.policy = value;
            return this;
        }

        /**
         * Sets the {@code pattern} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pattern(String value) {
            this.pattern = value;
            return this;
        }

        /**
         * Sets the {@code phaseId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phaseId(String value) {
            this.phaseId = value;
            return this;
        }

        /**
         * Sets the {@code phaseKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phaseKind(String value) {
            this.phaseKind = value;
            return this;
        }

        /**
         * Sets the {@code role} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder role(String value) {
            this.role = value;
            return this;
        }

        /**
         * Sets the {@code sourceModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sourceModel(String value) {
            this.sourceModel = value;
            return this;
        }

        /**
         * Sets the {@code conversationScope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder conversationScope(String value) {
            this.conversationScope = value;
            return this;
        }

        /**
         * Sets the {@code sourcePhaseId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sourcePhaseId(String value) {
            this.sourcePhaseId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FusionAttribution build() {
            if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
            if (syntheticModel == null) throw new IllegalStateException("Missing required property: syntheticModel");
            if (policy == null) throw new IllegalStateException("Missing required property: policy");
            if (pattern == null) throw new IllegalStateException("Missing required property: pattern");
            return new FusionAttribution(hasUserSteering, fusionId, commitId, syntheticModel, policy, pattern, phaseId, phaseKind, role, sourceModel, conversationScope, sourcePhaseId);
        }
    }
}
