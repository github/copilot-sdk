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
 * A single dependency edge read from the session SQL `todo_deps` table, indicating that one todo must complete before another.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanSqlTodoDependency(
    /** ID of the todo that has the dependency. */
    @JsonProperty("todoId") String todoId,
    /** ID of the todo it depends on. */
    @JsonProperty("dependsOn") String dependsOn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PlanSqlTodoDependency {}

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
        private String todoId;
        private String dependsOn;

        /**
         * Sets the {@code todoId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder todoId(String value) {
            this.todoId = value;
            return this;
        }

        /**
         * Sets the {@code dependsOn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dependsOn(String value) {
            this.dependsOn = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PlanSqlTodoDependency build() {
            if (todoId == null) throw new IllegalStateException("Missing required property: todoId");
            if (dependsOn == null) throw new IllegalStateException("Missing required property: dependsOn");
            return new PlanSqlTodoDependency(todoId, dependsOn);
        }
    }
}
