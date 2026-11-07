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
 * Resolved persisted workflow identity and resumed run envelope.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowResumeResult(
    /** Persisted workflow name resolved for the resumed run. */
    @JsonProperty("workflowName") String workflowName,
    /** Terminal resumed run envelope. */
    @JsonProperty("run") WorkflowRunResult run
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowResumeResult {}

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
        private String workflowName;
        private WorkflowRunResult run;

        /**
         * Sets the {@code workflowName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workflowName(String value) {
            this.workflowName = value;
            return this;
        }

        /**
         * Sets the {@code run} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder run(WorkflowRunResult value) {
            this.run = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowResumeResult build() {
            if (workflowName == null) throw new IllegalStateException("Missing required property: workflowName");
            if (run == null) throw new IllegalStateException("Missing required property: run");
            return new WorkflowResumeResult(workflowName, run);
        }
    }
}
