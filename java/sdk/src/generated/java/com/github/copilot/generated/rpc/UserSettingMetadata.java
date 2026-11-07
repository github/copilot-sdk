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
 * A single user setting's effective value alongside its default, so consumers can render settings left at their default.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSettingMetadata(
    /** The effective value: the user's value if set, otherwise the default. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value,
    /** The centrally-known default for this setting (null when no default is registered). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("default") Object default_,
    /** True when the user has not set an explicit value for this setting (i.e. it is left at its default). Reflects whether the user has overridden the key, not whether the effective value happens to equal the default — a key explicitly set to a value identical to the default still reports false. */
    @JsonProperty("isDefault") Boolean isDefault
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UserSettingMetadata {}

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
        private Object default_;
        private boolean $presence1;
        private Boolean isDefault;

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
         * Sets the {@code default} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder default_(Object value) {
            this.default_ = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code isDefault} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isDefault(Boolean value) {
            this.isDefault = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingMetadata build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: value");
            if (!$presence1) throw new IllegalStateException("Missing required property: default");
            if (isDefault == null) throw new IllegalStateException("Missing required property: isDefault");
            return new UserSettingMetadata(value, default_, isDefault);
        }
    }
}
