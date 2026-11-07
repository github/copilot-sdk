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
 * One cursor-addressed page of retained experimental runtime lifecycle diagnostic events.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record LifecycleDiagnosticsReadResult(
    /** Retained lifecycle diagnostic events beginning at the requested cursor. */
    @JsonProperty("events") List<LifecycleDiagnosticEvent> events,
    /** Opaque cursor for the next independent lifecycle diagnostics read. */
    @JsonProperty("cursor") String cursor,
    /** Whether the requested cursor remained within the retained activation window. */
    @JsonProperty("cursorStatus") DiagnosticCursorStatus cursorStatus,
    /** Number of events lost before this page when known. Omitted when an activation change makes the count unknowable. */
    @JsonProperty("droppedCount") Long droppedCount,
    /** Whether additional retained lifecycle diagnostic events follow this page. */
    @JsonProperty("hasMore") Boolean hasMore
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    LifecycleDiagnosticsReadResult {}

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
        private List<LifecycleDiagnosticEvent> events;
        private String cursor;
        private DiagnosticCursorStatus cursorStatus;
        private Long droppedCount;
        private Boolean hasMore;

        /**
         * Sets the {@code events} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder events(List<LifecycleDiagnosticEvent> value) {
            this.events = value;
            return this;
        }

        /**
         * Sets the {@code cursor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        public LifecycleDiagnosticsReadResult build() {
            if (events == null) throw new IllegalStateException("Missing required property: events");
            if (cursor == null) throw new IllegalStateException("Missing required property: cursor");
            if (cursorStatus == null) throw new IllegalStateException("Missing required property: cursorStatus");
            if (hasMore == null) throw new IllegalStateException("Missing required property: hasMore");
            return new LifecycleDiagnosticsReadResult(events, cursor, cursorStatus, droppedCount, hasMore);
        }
    }
}
