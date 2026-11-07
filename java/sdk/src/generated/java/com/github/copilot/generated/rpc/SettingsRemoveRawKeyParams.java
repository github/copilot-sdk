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
 * One key to remove from a settings file, named exactly as it appears in the file.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SettingsRemoveRawKeyParams(
    /** Absolute path of the settings file to edit. */
    @JsonProperty("filePath") String filePath,
    /** The key to remove, spelled as it appears in the file. It need not be a recognized setting: this exists to remove keys the loader rejected. */
    @JsonProperty("rawKey") String rawKey
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SettingsRemoveRawKeyParams {}

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
        private String filePath;
        private String rawKey;

        /**
         * Sets the {@code filePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filePath(String value) {
            this.filePath = value;
            return this;
        }

        /**
         * Sets the {@code rawKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rawKey(String value) {
            this.rawKey = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SettingsRemoveRawKeyParams build() {
            if (filePath == null) throw new IllegalStateException("Missing required property: filePath");
            if (rawKey == null) throw new IllegalStateException("Missing required property: rawKey");
            return new SettingsRemoveRawKeyParams(filePath, rawKey);
        }
    }
}
