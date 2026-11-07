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
 * Summary of persisted location permissions applied to the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsLocationsApplyResult(
    /** Location key used in the location-permissions store */
    @JsonProperty("locationKey") String locationKey,
    /** Whether the location is a git repo or directory */
    @JsonProperty("locationType") PermissionLocationType locationType,
    /** Whether a different location was applied since the previous apply call */
    @JsonProperty("changed") Boolean changed,
    /** Number of location-scoped rules added to the live permission service */
    @JsonProperty("appliedRuleCount") Long appliedRuleCount,
    /** Number of persisted allowed directories added to the live path manager */
    @JsonProperty("appliedDirectoryCount") Long appliedDirectoryCount,
    /** Location-scoped rules applied to the live permission service */
    @JsonProperty("appliedRules") List<PermissionRule> appliedRules
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsLocationsApplyResult {}

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
        private Boolean changed;
        private Long appliedRuleCount;
        private Long appliedDirectoryCount;
        private List<PermissionRule> appliedRules;

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
         * Sets the {@code changed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder changed(Boolean value) {
            this.changed = value;
            return this;
        }

        /**
         * Sets the {@code appliedRuleCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder appliedRuleCount(Long value) {
            this.appliedRuleCount = value;
            return this;
        }

        /**
         * Sets the {@code appliedDirectoryCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder appliedDirectoryCount(Long value) {
            this.appliedDirectoryCount = value;
            return this;
        }

        /**
         * Sets the {@code appliedRules} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder appliedRules(List<PermissionRule> value) {
            this.appliedRules = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsLocationsApplyResult build() {
            if (locationKey == null) throw new IllegalStateException("Missing required property: locationKey");
            if (locationType == null) throw new IllegalStateException("Missing required property: locationType");
            if (changed == null) throw new IllegalStateException("Missing required property: changed");
            if (appliedRuleCount == null) throw new IllegalStateException("Missing required property: appliedRuleCount");
            if (appliedDirectoryCount == null) throw new IllegalStateException("Missing required property: appliedDirectoryCount");
            if (appliedRules == null) throw new IllegalStateException("Missing required property: appliedRules");
            return new SessionPermissionsLocationsApplyResult(locationKey, locationType, changed, appliedRuleCount, appliedDirectoryCount, appliedRules);
        }
    }
}
