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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record WorkflowPauseCheckpointResult(
    /** Whether this execution attempt must pause or may continue. */
    @JsonProperty("action") WorkflowPauseCheckpointAction action
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    WorkflowPauseCheckpointResult {}

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
        private WorkflowPauseCheckpointAction action;

        /**
         * Sets the {@code action} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder action(WorkflowPauseCheckpointAction value) {
            this.action = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowPauseCheckpointResult build() {
            if (action == null) throw new IllegalStateException("Missing required property: action");
            return new WorkflowPauseCheckpointResult(action);
        }
    }
}
