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
 * A file that a conversation-and-files rewind would restore.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryRewindFilePreview(
    /** Absolute path of the captured file. */
    @JsonProperty("path") String path,
    /** Aggregate change made across the discarded turns. */
    @JsonProperty("changeType") HistoryRewindChangeType changeType,
    /** Lines added across the discarded turns. */
    @JsonProperty("linesAdded") Long linesAdded,
    /** Lines removed across the discarded turns. */
    @JsonProperty("linesRemoved") Long linesRemoved
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HistoryRewindFilePreview {}

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
        private HistoryRewindChangeType changeType;
        private Long linesAdded;
        private Long linesRemoved;

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
         * Sets the {@code changeType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder changeType(HistoryRewindChangeType value) {
            this.changeType = value;
            return this;
        }

        /**
         * Sets the {@code linesAdded} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder linesAdded(Long value) {
            this.linesAdded = value;
            return this;
        }

        /**
         * Sets the {@code linesRemoved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder linesRemoved(Long value) {
            this.linesRemoved = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HistoryRewindFilePreview build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (changeType == null) throw new IllegalStateException("Missing required property: changeType");
            if (linesAdded == null) throw new IllegalStateException("Missing required property: linesAdded");
            if (linesRemoved == null) throw new IllegalStateException("Missing required property: linesRemoved");
            return new HistoryRewindFilePreview(path, changeType, linesAdded, linesRemoved);
        }
    }
}
