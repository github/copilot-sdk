/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSqliteQueryResult(
    /** For SELECT: array of row objects. For others: empty array. */
    @JsonProperty("rows") List<Map<String, Object>> rows,
    /** Column names from the result set */
    @JsonProperty("columns") List<String> columns,
    /** Number of rows affected (for INSERT/UPDATE/DELETE) */
    @JsonProperty("rowsAffected") Long rowsAffected,
    /** SQLite last_insert_rowid() value for INSERT. */
    @JsonProperty("lastInsertRowid") Long lastInsertRowid,
    /** Describes a filesystem error. */
    @JsonProperty("error") SessionFsError error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsSqliteQueryResult {}

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
        private List<Map<String, Object>> rows;
        private List<String> columns;
        private Long rowsAffected;
        private Long lastInsertRowid;
        private SessionFsError error;

        /**
         * Sets the {@code rows} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rows(List<Map<String, Object>> value) {
            this.rows = value;
            return this;
        }

        /**
         * Sets the {@code columns} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder columns(List<String> value) {
            this.columns = value;
            return this;
        }

        /**
         * Sets the {@code rowsAffected} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rowsAffected(Long value) {
            this.rowsAffected = value;
            return this;
        }

        /**
         * Sets the {@code lastInsertRowid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastInsertRowid(Long value) {
            this.lastInsertRowid = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(SessionFsError value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFsSqliteQueryResult build() {
            if (rows == null) throw new IllegalStateException("Missing required property: rows");
            if (columns == null) throw new IllegalStateException("Missing required property: columns");
            if (rowsAffected == null) throw new IllegalStateException("Missing required property: rowsAffected");
            return new SessionFsSqliteQueryResult(rows, columns, rowsAffected, lastInsertRowid, error);
        }
    }
}
