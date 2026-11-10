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
 * A Copilot Fusion tier request for the session, named as the CLI's `--fusion-tier` names it. The runtime resolves the tier to the Auto routing preference it stands for and requests that preference exactly as `switchAutoTier` does. This updates Auto configuration only; it does not change the selected model to `auto`.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSwitchFusionTierParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Copilot Fusion tier to activate when a future user turn using the `auto` model safely mints a replacement model and token pair. Use `switchAutoTier` to return to provider-default Auto routing. */
    @JsonProperty("fusionTier") FusionTier fusionTier,
    /** Origin to record on the effective `session.model_change` event. Defaults to `sdk` when omitted. */
    @JsonProperty("source") ModelChangeSource source
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSwitchFusionTierParams {}

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
        private FusionTier fusionTier;
        private ModelChangeSource source;

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
         * Sets the {@code fusionTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder fusionTier(FusionTier value) {
            this.fusionTier = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder source(ModelChangeSource value) {
            this.source = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelSwitchFusionTierParams build() {
            if (fusionTier == null) throw new IllegalStateException("Missing required property: fusionTier");
            return new SessionModelSwitchFusionTierParams(sessionId, fusionTier, source);
        }
    }
}
