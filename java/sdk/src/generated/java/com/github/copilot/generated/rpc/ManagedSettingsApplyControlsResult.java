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
 * The user settings with the managed layer's controlled entries stamped on.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsApplyControlsResult(
    /** The resulting settings document. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("settingsJson") Object settingsJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsApplyControlsResult {}

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
        private Object settingsJson;
        private boolean $presence0;

        /**
         * Sets the {@code settingsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settingsJson(Object value) {
            this.settingsJson = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsApplyControlsResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: settingsJson");
            return new ManagedSettingsApplyControlsResult(settingsJson);
        }
    }
}
