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
 * Workspace checkpoint metadata with assigned number, human-readable title, and checkpoint filename.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkspacesCheckpoints(
    /** Checkpoint number assigned by the workspace manager */
    @JsonProperty("number") Long number,
    /** Human-readable checkpoint title */
    @JsonProperty("title") String title,
    /** Filename of the checkpoint within the workspace checkpoints directory */
    @JsonProperty("filename") String filename
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkspacesCheckpoints {}

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
        private Long number;
        private String title;
        private String filename;

        /**
         * Sets the {@code number} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder number(Long value) {
            this.number = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code filename} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filename(String value) {
            this.filename = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkspacesCheckpoints build() {
            if (number == null) throw new IllegalStateException("Missing required property: number");
            if (title == null) throw new IllegalStateException("Missing required property: title");
            if (filename == null) throw new IllegalStateException("Missing required property: filename");
            return new WorkspacesCheckpoints(number, title, filename);
        }
    }
}
