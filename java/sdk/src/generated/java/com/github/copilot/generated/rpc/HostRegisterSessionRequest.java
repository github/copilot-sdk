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
 * Listener-scoped registration, not a copy or durable adoption of a session.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostRegisterSessionRequest(
    /** Canonical ID of the existing resident runtime session. */
    @JsonProperty("sessionId") String sessionId,
    /** Absolute working directory of the resident session. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Additional directories already granted to the resident session. */
    @JsonProperty("additionalDirectories") List<String> additionalDirectories,
    /** Current display title of the resident session, when available. */
    @JsonProperty("title") String title,
    /** Session creation time in milliseconds since the Unix epoch, when available. */
    @JsonProperty("createdAtUnixMs") Long createdAtUnixMs,
    /** Last session modification time in milliseconds since the Unix epoch, when available. */
    @JsonProperty("modifiedAtUnixMs") Long modifiedAtUnixMs
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    HostRegisterSessionRequest {}

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
        private String workingDirectory;
        private List<String> additionalDirectories;
        private String title;
        private Long createdAtUnixMs;
        private Long modifiedAtUnixMs;

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
         * Sets the {@code workingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder workingDirectory(String value) {
            this.workingDirectory = value;
            return this;
        }

        /**
         * Sets the {@code additionalDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder additionalDirectories(List<String> value) {
            this.additionalDirectories = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code createdAtUnixMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder createdAtUnixMs(Long value) {
            this.createdAtUnixMs = value;
            return this;
        }

        /**
         * Sets the {@code modifiedAtUnixMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modifiedAtUnixMs(Long value) {
            this.modifiedAtUnixMs = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostRegisterSessionRequest build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (workingDirectory == null) throw new IllegalStateException("Missing required property: workingDirectory");
            return new HostRegisterSessionRequest(sessionId, workingDirectory, additionalDirectories, title, createdAtUnixMs, modifiedAtUnixMs);
        }
    }
}
