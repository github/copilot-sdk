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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Todo rows + dependency edges read from the session SQL database.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPlanReadSqlTodosWithDependenciesResult(
    /** Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables. Empty when no database, no todos table, or the SELECT failed. */
    @JsonProperty("rows") List<PlanSqlTodosRow> rows,
    /** Edges from the session SQL todo_deps table. Empty when no database, no todo_deps table, or the SELECT failed. Read independently from `rows`, so a broken todo_deps table does not affect the rows result and vice versa. */
    @JsonProperty("dependencies") List<PlanSqlTodoDependency> dependencies
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPlanReadSqlTodosWithDependenciesResult {}

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
        private List<PlanSqlTodosRow> rows;
        private List<PlanSqlTodoDependency> dependencies;

        /**
         * Sets the {@code rows} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rows(List<PlanSqlTodosRow> value) {
            this.rows = value;
            return this;
        }

        /**
         * Sets the {@code dependencies} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dependencies(List<PlanSqlTodoDependency> value) {
            this.dependencies = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPlanReadSqlTodosWithDependenciesResult build() {
            if (rows == null) throw new IllegalStateException("Missing required property: rows");
            if (dependencies == null) throw new IllegalStateException("Missing required property: dependencies");
            return new SessionPlanReadSqlTodosWithDependenciesResult(rows, dependencies);
        }
    }
}
