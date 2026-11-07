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
 * Exact lifecycle operation awaited by a real dependency wait.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleBlocker(
    /** System-owned capability required by the progress owner. */
    @JsonProperty("requirement") String requirement,
    /** System that owns the awaited operation. */
    @JsonProperty("system") String system,
    /** Opaque awaited operation identifier. */
    @JsonProperty("operationId") String operationId,
    /** Awaited operation revision observed by this blocker. */
    @JsonProperty("operationRevision") Long operationRevision,
    /** Current phase of the awaited operation. */
    @JsonProperty("currentPhase") String currentPhase,
    /** Current production boundary of the awaited operation. */
    @JsonProperty("currentStep") String currentStep,
    /** How the awaited operation ended when the real wait remains active. */
    @JsonProperty("operationDisposition") RuntimeLifecycleDisposition operationDisposition,
    /** Timing of this dependency wait. */
    @JsonProperty("timing") RuntimeLifecycleBlockerTiming timing
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RuntimeLifecycleBlocker {}

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
        private String requirement;
        private String system;
        private String operationId;
        private Long operationRevision;
        private String currentPhase;
        private String currentStep;
        private RuntimeLifecycleDisposition operationDisposition;
        private RuntimeLifecycleBlockerTiming timing;

        /**
         * Sets the {@code requirement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requirement(String value) {
            this.requirement = value;
            return this;
        }

        /**
         * Sets the {@code system} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder system(String value) {
            this.system = value;
            return this;
        }

        /**
         * Sets the {@code operationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operationId(String value) {
            this.operationId = value;
            return this;
        }

        /**
         * Sets the {@code operationRevision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operationRevision(Long value) {
            this.operationRevision = value;
            return this;
        }

        /**
         * Sets the {@code currentPhase} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder currentPhase(String value) {
            this.currentPhase = value;
            return this;
        }

        /**
         * Sets the {@code currentStep} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder currentStep(String value) {
            this.currentStep = value;
            return this;
        }

        /**
         * Sets the {@code operationDisposition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operationDisposition(RuntimeLifecycleDisposition value) {
            this.operationDisposition = value;
            return this;
        }

        /**
         * Sets the {@code timing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timing(RuntimeLifecycleBlockerTiming value) {
            this.timing = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RuntimeLifecycleBlocker build() {
            if (requirement == null) throw new IllegalStateException("Missing required property: requirement");
            if (system == null) throw new IllegalStateException("Missing required property: system");
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (operationRevision == null) throw new IllegalStateException("Missing required property: operationRevision");
            if (currentPhase == null) throw new IllegalStateException("Missing required property: currentPhase");
            if (currentStep == null) throw new IllegalStateException("Missing required property: currentStep");
            if (timing == null) throw new IllegalStateException("Missing required property: timing");
            return new RuntimeLifecycleBlocker(requirement, system, operationId, operationRevision, currentPhase, currentStep, operationDisposition, timing);
        }
    }
}
