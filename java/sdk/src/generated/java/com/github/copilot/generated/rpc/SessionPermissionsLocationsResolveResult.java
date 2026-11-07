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
 * Resolved location-permissions key and type.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsLocationsResolveResult(
    /** Location key used in the location-permissions store */
    @JsonProperty("locationKey") String locationKey,
    /** Whether the location is a git repo or directory */
    @JsonProperty("locationType") PermissionLocationType locationType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsLocationsResolveResult {}

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
        private String locationKey;
        private PermissionLocationType locationType;

        /**
         * Sets the {@code locationKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder locationKey(String value) {
            this.locationKey = value;
            return this;
        }

        /**
         * Sets the {@code locationType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder locationType(PermissionLocationType value) {
            this.locationType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsLocationsResolveResult build() {
            if (locationKey == null) throw new IllegalStateException("Missing required property: locationKey");
            if (locationType == null) throw new IllegalStateException("Missing required property: locationType");
            return new SessionPermissionsLocationsResolveResult(locationKey, locationType);
        }
    }
}
