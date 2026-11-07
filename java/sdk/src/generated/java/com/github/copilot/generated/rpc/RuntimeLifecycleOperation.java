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
 * Observed state of one readiness or convergence operation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleOperation(
    /** Opaque operation identifier, unique within the lifecycle incarnation and system. */
    @JsonProperty("operationId") String operationId,
    /** Orders transitions of this operation. */
    @JsonProperty("operationRevision") Long operationRevision,
    /** System-owned reason the operation started. */
    @JsonProperty("trigger") String trigger,
    /** System-owned lifecycle stage. */
    @JsonProperty("phase") String phase,
    /** System-owned production boundary within the phase. */
    @JsonProperty("step") String step,
    /** How the operation ended. Absent while the operation remains active. */
    @JsonProperty("disposition") RuntimeLifecycleDisposition disposition,
    /** Timestamps recorded by the lifecycle service for this operation revision. */
    @JsonProperty("timing") RuntimeLifecycleOperationTiming timing
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RuntimeLifecycleOperation {}

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
        private String operationId;
        private Long operationRevision;
        private String trigger;
        private String phase;
        private String step;
        private RuntimeLifecycleDisposition disposition;
        private RuntimeLifecycleOperationTiming timing;

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
         * Sets the {@code trigger} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder trigger(String value) {
            this.trigger = value;
            return this;
        }

        /**
         * Sets the {@code phase} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phase(String value) {
            this.phase = value;
            return this;
        }

        /**
         * Sets the {@code step} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder step(String value) {
            this.step = value;
            return this;
        }

        /**
         * Sets the {@code disposition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disposition(RuntimeLifecycleDisposition value) {
            this.disposition = value;
            return this;
        }

        /**
         * Sets the {@code timing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timing(RuntimeLifecycleOperationTiming value) {
            this.timing = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RuntimeLifecycleOperation build() {
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (operationRevision == null) throw new IllegalStateException("Missing required property: operationRevision");
            if (trigger == null) throw new IllegalStateException("Missing required property: trigger");
            if (phase == null) throw new IllegalStateException("Missing required property: phase");
            if (step == null) throw new IllegalStateException("Missing required property: step");
            if (timing == null) throw new IllegalStateException("Missing required property: timing");
            return new RuntimeLifecycleOperation(operationId, operationRevision, trigger, phase, step, disposition, timing);
        }
    }
}
