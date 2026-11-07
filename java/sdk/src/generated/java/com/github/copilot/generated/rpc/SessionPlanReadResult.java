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
import javax.annotation.processing.Generated;

/**
 * Existence, contents, and resolved path of the session plan file.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPlanReadResult(
    /** Whether the plan file exists in the workspace */
    @JsonProperty("exists") Boolean exists,
    /** The content of the plan file, or null if it does not exist */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("content") String content,
    /** Absolute file path of the plan file, or null if workspace is not enabled */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("path") String path
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPlanReadResult {}

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
        private Boolean exists;
        private String content;
        private boolean $presence1;
        private String path;
        private boolean $presence2;

        /**
         * Sets the {@code exists} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder exists(Boolean value) {
            this.exists = value;
            return this;
        }

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder content(String value) {
            this.content = value;
            this.$presence1 = true;
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
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPlanReadResult build() {
            if (exists == null) throw new IllegalStateException("Missing required property: exists");
            if (!$presence1) throw new IllegalStateException("Missing required property: content");
            if (!$presence2) throw new IllegalStateException("Missing required property: path");
            return new SessionPlanReadResult(exists, content, path);
        }
    }
}
