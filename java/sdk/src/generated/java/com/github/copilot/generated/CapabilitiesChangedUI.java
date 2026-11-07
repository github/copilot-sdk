/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * UI capability changes
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CapabilitiesChangedUI(
    /** Whether elicitation is now supported */
    @JsonProperty("elicitation") Boolean elicitation,
    /** Whether MCP Apps (SEP-1865) UI passthrough is now supported */
    @JsonProperty("mcpApps") Boolean mcpApps,
    /** Whether canvas rendering is now supported */
    @JsonProperty("canvases") Boolean canvases,
    /** Whether a recovery-aware Auto-switch responder is registered. This does not imply current eligibility or override model policy. */
    @JsonProperty("autoModeRecovery") Boolean autoModeRecovery
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CapabilitiesChangedUI {}

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
        private Boolean elicitation;
        private Boolean mcpApps;
        private Boolean canvases;
        private Boolean autoModeRecovery;

        /**
         * Sets the {@code elicitation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder elicitation(Boolean value) {
            this.elicitation = value;
            return this;
        }

        /**
         * Sets the {@code mcpApps} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpApps(Boolean value) {
            this.mcpApps = value;
            return this;
        }

        /**
         * Sets the {@code canvases} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canvases(Boolean value) {
            this.canvases = value;
            return this;
        }

        /**
         * Sets the {@code autoModeRecovery} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoModeRecovery(Boolean value) {
            this.autoModeRecovery = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CapabilitiesChangedUI build() {
            return new CapabilitiesChangedUI(elicitation, mcpApps, canvases, autoModeRecovery);
        }
    }

    /**
     * Creates UI capability changes without an Auto recovery update.
     * @deprecated Use named construction via {@link #builder()}.
     * @param elicitation Whether elicitation is now supported
     * @param mcpApps Whether MCP Apps (SEP-1865) UI passthrough is now supported
     * @param canvases Whether canvas rendering is now supported
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CapabilitiesChangedUI(
        Boolean elicitation,
        Boolean mcpApps,
        Boolean canvases
    ) {
        this(elicitation, mcpApps, canvases, null);
    }
}
