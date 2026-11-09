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
 * The runtime's interpretation of the reasoning-summary arm.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureFlagReasoningSummariesArm(
    /** Whether summaries are hidden by default. */
    @JsonProperty("offByDefault") Boolean offByDefault,
    /** Whether the hidden-summary hint is enabled. */
    @JsonProperty("hintEnabled") Boolean hintEnabled
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FeatureFlagReasoningSummariesArm {}

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
        private Boolean offByDefault;
        private Boolean hintEnabled;

        /**
         * Sets the {@code offByDefault} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder offByDefault(Boolean value) {
            this.offByDefault = value;
            return this;
        }

        /**
         * Sets the {@code hintEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hintEnabled(Boolean value) {
            this.hintEnabled = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FeatureFlagReasoningSummariesArm build() {
            if (offByDefault == null) throw new IllegalStateException("Missing required property: offByDefault");
            if (hintEnabled == null) throw new IllegalStateException("Missing required property: hintEnabled");
            return new FeatureFlagReasoningSummariesArm(offByDefault, hintEnabled);
        }
    }
}
