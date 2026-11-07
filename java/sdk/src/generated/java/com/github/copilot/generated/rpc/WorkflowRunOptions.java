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
 * Options controlling workflow invocation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunOptions(
    /** Per-invocation resource ceiling overrides. */
    @JsonProperty("limits") WorkflowRunLimits limits,
    /** Whether to notify the originating session when the workflow completes. */
    @JsonProperty("notifyOnComplete") Boolean notifyOnComplete,
    /** Whether to emit workflow phase names to the session transcript. */
    @JsonProperty("logPhaseNames") Boolean logPhaseNames,
    /** Run identifier whose journal and progress should seed this resumed run. */
    @JsonProperty("resumeFromRunId") String resumeFromRunId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowRunOptions {}

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
        private WorkflowRunLimits limits;
        private Boolean notifyOnComplete;
        private Boolean logPhaseNames;
        private String resumeFromRunId;

        /**
         * Sets the {@code limits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder limits(WorkflowRunLimits value) {
            this.limits = value;
            return this;
        }

        /**
         * Sets the {@code notifyOnComplete} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder notifyOnComplete(Boolean value) {
            this.notifyOnComplete = value;
            return this;
        }

        /**
         * Sets the {@code logPhaseNames} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder logPhaseNames(Boolean value) {
            this.logPhaseNames = value;
            return this;
        }

        /**
         * Sets the {@code resumeFromRunId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resumeFromRunId(String value) {
            this.resumeFromRunId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowRunOptions build() {
            return new WorkflowRunOptions(limits, notifyOnComplete, logPhaseNames, resumeFromRunId);
        }
    }
}
