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
 * Adapter-declared policy that tells clients whether discovery may run automatically.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderAutomaticDiscoveryPolicy(
    /** Whether automatic discovery is allowed, limited to configured providers, or explicit-only. */
    @JsonProperty("mode") ModelProviderAutomaticDiscoveryMode mode,
    /** Maximum network scope used by this adapter during discovery. */
    @JsonProperty("networkScope") ModelProviderDiscoveryNetworkScope networkScope,
    /** True when discovery requires non-null caller input. Omission or null is rejected before adapter execution. When false, omitted or null input selects adapter defaults without schema validation. */
    @JsonProperty("requiresInput") Boolean requiresInput,
    /** True when the adapter must be enabled by a trusted owner, such as a trusted extension, before automatic discovery may run. */
    @JsonProperty("requiresTrust") Boolean requiresTrust
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelProviderAutomaticDiscoveryPolicy {}

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
        private ModelProviderAutomaticDiscoveryMode mode;
        private ModelProviderDiscoveryNetworkScope networkScope;
        private Boolean requiresInput;
        private Boolean requiresTrust;

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mode(ModelProviderAutomaticDiscoveryMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code networkScope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder networkScope(ModelProviderDiscoveryNetworkScope value) {
            this.networkScope = value;
            return this;
        }

        /**
         * Sets the {@code requiresInput} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requiresInput(Boolean value) {
            this.requiresInput = value;
            return this;
        }

        /**
         * Sets the {@code requiresTrust} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requiresTrust(Boolean value) {
            this.requiresTrust = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelProviderAutomaticDiscoveryPolicy build() {
            if (mode == null) throw new IllegalStateException("Missing required property: mode");
            if (networkScope == null) throw new IllegalStateException("Missing required property: networkScope");
            if (requiresInput == null) throw new IllegalStateException("Missing required property: requiresInput");
            if (requiresTrust == null) throw new IllegalStateException("Missing required property: requiresTrust");
            return new ModelProviderAutomaticDiscoveryPolicy(mode, networkScope, requiresInput, requiresTrust);
        }
    }
}
