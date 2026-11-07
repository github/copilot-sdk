/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.workspace_file_changed". Workspace file change details including path and operation type
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWorkspaceFileChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.workspace_file_changed"; }

    @JsonProperty("data")
    private SessionWorkspaceFileChangedEventData data;

    public SessionWorkspaceFileChangedEventData getData() { return data; }
    public void setData(SessionWorkspaceFileChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionWorkspaceFileChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspaceFileChangedEventData(
        /** Relative path within the session workspace files directory */
        @JsonProperty("path") String path,
        /** Whether the file was newly created or updated */
        @JsonProperty("operation") WorkspaceFileChangedOperation operation
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionWorkspaceFileChangedEventData {}

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
            private String path;
            private WorkspaceFileChangedOperation operation;

            /**
             * Sets the {@code path} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder path(String value) {
                this.path = value;
                return this;
            }

            /**
             * Sets the {@code operation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder operation(WorkspaceFileChangedOperation value) {
                this.operation = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionWorkspaceFileChangedEventData build() {
                if (path == null) throw new IllegalStateException("Missing required property: path");
                if (operation == null) throw new IllegalStateException("Missing required property: operation");
                return new SessionWorkspaceFileChangedEventData(path, operation);
            }
        }
    }
}
