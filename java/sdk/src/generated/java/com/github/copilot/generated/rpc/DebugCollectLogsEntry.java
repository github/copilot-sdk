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
 * A caller-provided server-local file or directory to include in the debug bundle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsEntry(
    /** Kind of source path to include. */
    @JsonProperty("kind") DebugCollectLogsEntryKind kind,
    /** Server-local source path to read. */
    @JsonProperty("path") String path,
    /** Relative path to use inside the staged bundle/archive. */
    @JsonProperty("bundlePath") String bundlePath,
    /** How text content from this entry should be redacted. Defaults to plain-text. With none, no redaction is applied; the caller must ensure any necessary redaction is performed before this call. */
    @JsonProperty("redaction") DebugCollectLogsRedaction redaction,
    /** When true, collection fails if this entry cannot be read. Defaults to false, which records the entry in `skippedEntries`. */
    @JsonProperty("required") Boolean required
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DebugCollectLogsEntry {}

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
        private DebugCollectLogsEntryKind kind;
        private String path;
        private String bundlePath;
        private DebugCollectLogsRedaction redaction;
        private Boolean required;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(DebugCollectLogsEntryKind value) {
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
         * Sets the {@code bundlePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder bundlePath(String value) {
            this.bundlePath = value;
            return this;
        }

        /**
         * Sets the {@code redaction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder redaction(DebugCollectLogsRedaction value) {
            this.redaction = value;
            return this;
        }

        /**
         * Sets the {@code required} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder required(Boolean value) {
            this.required = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DebugCollectLogsEntry build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (bundlePath == null) throw new IllegalStateException("Missing required property: bundlePath");
            return new DebugCollectLogsEntry(kind, path, bundlePath, redaction, required);
        }
    }
}
