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
 * Result of collecting a session debug bundle.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDebugCollectLogsResult(
    /** Destination kind that was written. */
    @JsonProperty("kind") DebugCollectLogsResultKind kind,
    /** Actual archive path or staging directory path written. This may differ from the requested path when no-overwrite suffixing or fallback-to-temp-directory was needed. */
    @JsonProperty("path") String path,
    /** Files included in the bundle. */
    @JsonProperty("entries") List<DebugCollectLogsCollectedEntry> entries,
    /** Optional files or directories that could not be included. */
    @JsonProperty("skippedEntries") List<DebugCollectLogsSkippedEntry> skippedEntries
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionDebugCollectLogsResult {}

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
        private DebugCollectLogsResultKind kind;
        private String path;
        private List<DebugCollectLogsCollectedEntry> entries;
        private List<DebugCollectLogsSkippedEntry> skippedEntries;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(DebugCollectLogsResultKind value) {
            this.kind = value;
            return this;
        }

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
         * Sets the {@code entries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entries(List<DebugCollectLogsCollectedEntry> value) {
            this.entries = value;
            return this;
        }

        /**
         * Sets the {@code skippedEntries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skippedEntries(List<DebugCollectLogsSkippedEntry> value) {
            this.skippedEntries = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionDebugCollectLogsResult build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (entries == null) throw new IllegalStateException("Missing required property: entries");
            return new SessionDebugCollectLogsResult(kind, path, entries, skippedEntries);
        }
    }
}
