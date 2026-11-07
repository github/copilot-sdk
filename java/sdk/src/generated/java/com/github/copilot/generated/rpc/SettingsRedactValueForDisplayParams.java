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
 * A settings value to redact before showing it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SettingsRedactValueForDisplayParams(
    /** The value to redact. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value,
    /** Where the value sits in the settings tree, which decides which parts of it are secret. */
    @JsonProperty("basePath") List<String> basePath
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SettingsRedactValueForDisplayParams {}

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
        private Object value;
        private boolean $presence0;
        private List<String> basePath;

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(Object value) {
            this.value = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code basePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder basePath(List<String> value) {
            this.basePath = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SettingsRedactValueForDisplayParams build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: value");
            if (basePath == null) throw new IllegalStateException("Missing required property: basePath");
            return new SettingsRedactValueForDisplayParams(value, basePath);
        }
    }
}
