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
 * One cursor-addressed page of retained session diagnostics.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDiagnosticsReadResult(
    /** Retained records beginning at the requested cursor. */
    @JsonProperty("entries") List<SessionDiagnosticsReadResultEntriesItem> entries,
    /** Opaque cursor for the next independent read. */
    @JsonProperty("cursor") String cursor,
    /** Whether the requested cursor remained within the retained buffer window. */
    @JsonProperty("cursorStatus") DiagnosticCursorStatus cursorStatus,
    /** Number of records lost before this page when known. Omitted when a buffer generation change makes the count unknowable. */
    @JsonProperty("droppedCount") Long droppedCount,
    /** Whether additional retained records follow this page. */
    @JsonProperty("hasMore") Boolean hasMore
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionDiagnosticsReadResultEntriesItem(
        /** UTC RFC 3339 timestamp captured at the diagnostic source. */
        @JsonProperty("timestamp") String timestamp,
        /** Severity of this emitted diagnostic record. */
        @JsonProperty("level") DiagnosticSeverity level,
        /** Agent identifier for a subagent host. Omitted for the root agent. */
        @JsonProperty("agentId") String agentId,
        /** Human-readable diagnostic summary. */
        @JsonProperty("message") String message,
        /** Whether message or data was truncated to the record-size bound. */
        @JsonProperty("truncated") Boolean truncated,
        /** Original byte count when a known-size message or data value was truncated. */
        @JsonProperty("originalBytes") Long originalBytes,
        /** Typed MCP diagnostic detail. */
        @JsonProperty("details") McpDiagnosticDetails details,
        /** Diagnostic source identifying the typed details payload. */
        @JsonProperty("source") String source
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionDiagnosticsReadResultEntriesItem {}

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
            private String timestamp;
            private DiagnosticSeverity level;
            private String agentId;
            private String message;
            private Boolean truncated;
            private Long originalBytes;
            private McpDiagnosticDetails details;
            private String source;

            /**
             * Sets the {@code timestamp} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder timestamp(String value) {
                this.timestamp = value;
                return this;
            }

            /**
             * Sets the {@code level} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder level(DiagnosticSeverity value) {
                this.level = value;
                return this;
            }

            /**
             * Sets the {@code agentId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentId(String value) {
                this.agentId = value;
                return this;
            }

            /**
             * Sets the {@code message} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder message(String value) {
                this.message = value;
                return this;
            }

            /**
             * Sets the {@code truncated} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder truncated(Boolean value) {
                this.truncated = value;
                return this;
            }

            /**
             * Sets the {@code originalBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder originalBytes(Long value) {
                this.originalBytes = value;
                return this;
            }

            /**
             * Sets the {@code details} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder details(McpDiagnosticDetails value) {
                this.details = value;
                return this;
            }

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(String value) {
                this.source = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionDiagnosticsReadResultEntriesItem build() {
                if (timestamp == null) throw new IllegalStateException("Missing required property: timestamp");
                if (level == null) throw new IllegalStateException("Missing required property: level");
                if (message == null) throw new IllegalStateException("Missing required property: message");
                if (details == null) throw new IllegalStateException("Missing required property: details");
                if (source == null) throw new IllegalStateException("Missing required property: source");
                return new SessionDiagnosticsReadResultEntriesItem(timestamp, level, agentId, message, truncated, originalBytes, details, source);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionDiagnosticsReadResult {}

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
        private List<SessionDiagnosticsReadResultEntriesItem> entries;
        private String cursor;
        private DiagnosticCursorStatus cursorStatus;
        private Long droppedCount;
        private Boolean hasMore;

        /**
         * Sets the {@code entries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entries(List<SessionDiagnosticsReadResultEntriesItem> value) {
            this.entries = value;
            return this;
        }

        /**
         * Sets the {@code cursor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cursor(String value) {
            this.cursor = value;
            return this;
        }

        /**
         * Sets the {@code cursorStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cursorStatus(DiagnosticCursorStatus value) {
            this.cursorStatus = value;
            return this;
        }

        /**
         * Sets the {@code droppedCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder droppedCount(Long value) {
            this.droppedCount = value;
            return this;
        }

        /**
         * Sets the {@code hasMore} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasMore(Boolean value) {
            this.hasMore = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionDiagnosticsReadResult build() {
            if (entries == null) throw new IllegalStateException("Missing required property: entries");
            if (cursor == null) throw new IllegalStateException("Missing required property: cursor");
            if (cursorStatus == null) throw new IllegalStateException("Missing required property: cursorStatus");
            if (hasMore == null) throw new IllegalStateException("Missing required property: hasMore");
            return new SessionDiagnosticsReadResult(entries, cursor, cursorStatus, droppedCount, hasMore);
        }
    }
}
