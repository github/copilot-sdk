/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Aggregate code change metrics for the session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShutdownCodeChanges(
    /** Total number of lines added during the session */
    @JsonProperty("linesAdded") Long linesAdded,
    /** Total number of lines removed during the session */
    @JsonProperty("linesRemoved") Long linesRemoved,
    /** List of file paths that were modified during the session */
    @JsonProperty("filesModified") List<String> filesModified
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ShutdownCodeChanges {}

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
        private Long linesAdded;
        private Long linesRemoved;
        private List<String> filesModified;

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
         * Sets the {@code filesModified} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filesModified(List<String> value) {
            this.filesModified = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ShutdownCodeChanges build() {
            if (linesAdded == null) throw new IllegalStateException("Missing required property: linesAdded");
            if (linesRemoved == null) throw new IllegalStateException("Missing required property: linesRemoved");
            if (filesModified == null) throw new IllegalStateException("Missing required property: filesModified");
            return new ShutdownCodeChanges(linesAdded, linesRemoved, filesModified);
        }
    }
}
