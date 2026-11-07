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
 * Result of registering or reclaiming a client-owned task.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksRegisterResult(
    /** Authoritative registered or reclaimed task */
    @JsonProperty("task") TaskClientInfo task,
    /** True only when this invocation created a new task */
    @JsonProperty("created") Boolean created,
    /** True only when this invocation reclaimed an orphaned task */
    @JsonProperty("reclaimed") Boolean reclaimed
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionTasksRegisterResult {}

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
        private Boolean created;
        private Boolean reclaimed;

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
         * Sets the {@code created} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder created(Boolean value) {
            this.created = value;
            return this;
        }

        /**
         * Sets the {@code reclaimed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reclaimed(Boolean value) {
            this.reclaimed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionTasksRegisterResult build() {
            if (task == null) throw new IllegalStateException("Missing required property: task");
            if (created == null) throw new IllegalStateException("Missing required property: created");
            if (reclaimed == null) throw new IllegalStateException("Missing required property: reclaimed");
            return new SessionTasksRegisterResult(task, created, reclaimed);
        }
    }
}
