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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database. The provider applies its SQLite busy timeout for every call.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSqliteQueryParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** SQL query to execute */
    @JsonProperty("query") String query,
    /** How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected) */
    @JsonProperty("queryType") SessionFsSqliteQueryType queryType,
    /** Optional named bind parameters */
    @JsonProperty("params") Map<String, Object> params
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsSqliteQueryParams {}

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
        private String sessionId;
        private String query;
        private SessionFsSqliteQueryType queryType;
        private Map<String, Object> params;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code query} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder query(String value) {
            this.query = value;
            return this;
        }

        /**
         * Sets the {@code queryType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder queryType(SessionFsSqliteQueryType value) {
            this.queryType = value;
            return this;
        }

        /**
         * Sets the {@code params} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder params(Map<String, Object> value) {
            this.params = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFsSqliteQueryParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (query == null) throw new IllegalStateException("Missing required property: query");
            if (queryType == null) throw new IllegalStateException("Missing required property: queryType");
            return new SessionFsSqliteQueryParams(sessionId, query, queryType, params);
        }
    }
}
