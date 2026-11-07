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
 * An optional debug bundle entry that could not be included.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsSkippedEntry(
    /** Relative path requested for this bundle entry. */
    @JsonProperty("bundlePath") String bundlePath,
    /** Server-local source path that could not be read. */
    @JsonProperty("path") String path,
    /** Reason the entry was skipped. */
    @JsonProperty("reason") String reason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DebugCollectLogsSkippedEntry {}

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
        private String bundlePath;
        private String path;
        private String reason;

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
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DebugCollectLogsSkippedEntry build() {
            if (bundlePath == null) throw new IllegalStateException("Missing required property: bundlePath");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            return new DebugCollectLogsSkippedEntry(bundlePath, path, reason);
        }
    }
}
