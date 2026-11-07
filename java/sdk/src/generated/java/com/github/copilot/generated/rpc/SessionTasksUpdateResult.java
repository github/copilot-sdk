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
 * Result of publishing a client-owned task update.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksUpdateResult(
    /** Authoritative task after processing the update */
    @JsonProperty("task") TaskClientInfo task,
    /** Whether this invocation changed task state */
    @JsonProperty("applied") Boolean applied,
    /** Whether this invocation repeated the latest accepted update */
    @JsonProperty("duplicate") Boolean duplicate
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionTasksUpdateResult {}

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
        private TaskClientInfo task;
        private Boolean applied;
        private Boolean duplicate;

        /**
         * Sets the {@code task} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder task(TaskClientInfo value) {
            this.task = value;
            return this;
        }

        /**
         * Sets the {@code applied} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder applied(Boolean value) {
            this.applied = value;
            return this;
        }

        /**
         * Sets the {@code duplicate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder duplicate(Boolean value) {
            this.duplicate = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionTasksUpdateResult build() {
            if (task == null) throw new IllegalStateException("Missing required property: task");
            if (applied == null) throw new IllegalStateException("Missing required property: applied");
            if (duplicate == null) throw new IllegalStateException("Missing required property: duplicate");
            return new SessionTasksUpdateResult(task, applied, duplicate);
        }
    }
}
