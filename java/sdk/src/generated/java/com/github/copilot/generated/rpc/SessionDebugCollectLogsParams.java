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
 * Options for collecting a session debug bundle with configurable redaction.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDebugCollectLogsParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Where the bundle should be written. Use `archive` to produce a .tgz, or `directory` to stage files for caller-managed upload/post-processing. */
    @JsonProperty("destination") Object destination,
    /** Which built-in session diagnostics to include. Omitted fields default to true. */
    @JsonProperty("include") DebugCollectLogsInclude include,
    /** Caller-provided server-local files or directories to include in addition to the runtime's built-in session diagnostics. This lets host applications add their own diagnostics without changing the API shape. */
    @JsonProperty("additionalEntries") List<DebugCollectLogsEntry> additionalEntries
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionDebugCollectLogsParams {}

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
        private Object destination;
        private DebugCollectLogsInclude include;
        private List<DebugCollectLogsEntry> additionalEntries;

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
         * Sets the {@code destination} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder destination(Object value) {
            this.destination = value;
            return this;
        }

        /**
         * Sets the {@code include} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder include(DebugCollectLogsInclude value) {
            this.include = value;
            return this;
        }

        /**
         * Sets the {@code additionalEntries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder additionalEntries(List<DebugCollectLogsEntry> value) {
            this.additionalEntries = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionDebugCollectLogsParams build() {
            if (destination == null) throw new IllegalStateException("Missing required property: destination");
            return new SessionDebugCollectLogsParams(sessionId, destination, include, additionalEntries);
        }
    }
}
