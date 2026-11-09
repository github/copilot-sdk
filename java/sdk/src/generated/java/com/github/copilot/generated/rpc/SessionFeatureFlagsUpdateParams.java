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
 * Changes apply in field order: config, preference, assignments, secondary context.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFeatureFlagsUpdateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Patch the retained configuration. */
    @JsonProperty("configPatch") FeatureFlagConfigPatch configPatch,
    /** Apply or remove one preference. */
    @JsonProperty("enabledOverride") FeatureFlagEnabledOverride enabledOverride,
    /** Replace or clear delivered assignments. */
    @JsonProperty("assignments") FeatureFlagAssignmentsUpdate assignments,
    /** Retain a nonempty secondary context using the original whitespace rules. */
    @JsonProperty("secondaryAssignmentContext") String secondaryAssignmentContext
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsUpdateParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private FeatureFlagConfigPatch configPatch;
        private FeatureFlagEnabledOverride enabledOverride;
        private FeatureFlagAssignmentsUpdate assignments;
        private String secondaryAssignmentContext;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code configPatch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder configPatch(FeatureFlagConfigPatch value) {
            this.configPatch = value;
            return this;
        }

        /**
         * Sets the {@code enabledOverride} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enabledOverride(FeatureFlagEnabledOverride value) {
            this.enabledOverride = value;
            return this;
        }

        /**
         * Sets the {@code assignments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assignments(FeatureFlagAssignmentsUpdate value) {
            this.assignments = value;
            return this;
        }

        /**
         * Sets the {@code secondaryAssignmentContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder secondaryAssignmentContext(String value) {
            this.secondaryAssignmentContext = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFeatureFlagsUpdateParams build() {
            return new SessionFeatureFlagsUpdateParams(sessionId, configPatch, enabledOverride, assignments, secondaryAssignmentContext);
        }
    }
}
