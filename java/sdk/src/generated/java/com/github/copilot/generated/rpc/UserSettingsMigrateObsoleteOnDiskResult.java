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
 * The outcome of the one-time obsolete-settings cleanup.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMigrateObsoleteOnDiskResult(
    /** What the cleanup did. Only `changed` rewrote the document. The cleaned settings are not returned here: the document is written before this replies, so `user.settings.load` reads it authoritatively. */
    @JsonProperty("status") UserSettingsMigrateObsoleteOnDiskStatus status
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsMigrateObsoleteOnDiskResult {}

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
        private UserSettingsMigrateObsoleteOnDiskStatus status;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(UserSettingsMigrateObsoleteOnDiskStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsMigrateObsoleteOnDiskResult build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new UserSettingsMigrateObsoleteOnDiskResult(status);
        }
    }
}
