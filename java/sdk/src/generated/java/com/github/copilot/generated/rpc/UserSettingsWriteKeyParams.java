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
 * A raw top-level setting edit with explicit read-failure behavior.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsWriteKeyParams(
    /** The top-level setting to write without canonicalizing legacy siblings. */
    @JsonProperty("key") String key,
    /** Optional host paths and environment. Omit to use the server's settings home. */
    @JsonProperty("context") UserSettingsHostContext context,
    /** The value to store. Explicit null is retained. Pass null when deleting. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value,
    /** Remove the key instead of writing it. Defaults to false. */
    @JsonProperty("delete") Boolean delete,
    /** Refuse to replace an unreadable or invalid file. Defaults to false. */
    @JsonProperty("refuseOnReadFailure") Boolean refuseOnReadFailure
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsWriteKeyParams {}

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
        private String key;
        private UserSettingsHostContext context;
        private Object value;
        private boolean $presence2;
        private Boolean delete;
        private Boolean refuseOnReadFailure;

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder context(UserSettingsHostContext value) {
            this.context = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(Object value) {
            this.value = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code delete} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder delete(Boolean value) {
            this.delete = value;
            return this;
        }

        /**
         * Sets the {@code refuseOnReadFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder refuseOnReadFailure(Boolean value) {
            this.refuseOnReadFailure = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsWriteKeyParams build() {
            if (key == null) throw new IllegalStateException("Missing required property: key");
            if (!$presence2) throw new IllegalStateException("Missing required property: value");
            return new UserSettingsWriteKeyParams(key, context, value, delete, refuseOnReadFailure);
        }
    }
}
