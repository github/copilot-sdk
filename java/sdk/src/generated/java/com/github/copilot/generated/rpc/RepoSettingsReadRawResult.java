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
 * Parsed repository settings without normalization or layering.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RepoSettingsReadRawResult(
    /** Stored JSON values after the existing JSONC parser and prototype-safety checks. Empty when the file is missing, blank, or invalid. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("config") Object config,
    /** True when the file exists but did not parse, so `config` is empty for that reason rather than because nothing is set. */
    @JsonProperty("invalid") Boolean invalid
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RepoSettingsReadRawResult {}

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
        private Object config;
        private boolean $presence0;
        private Boolean invalid;

        /**
         * Sets the {@code config} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder config(Object value) {
            this.config = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code invalid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder invalid(Boolean value) {
            this.invalid = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RepoSettingsReadRawResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: config");
            if (invalid == null) throw new IllegalStateException("Missing required property: invalid");
            return new RepoSettingsReadRawResult(config, invalid);
        }
    }
}
