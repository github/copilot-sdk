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
 * The bypass-permissions state a host holds today, the account now signed in, and what the managed-policy resolution said.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveBypassDisableTransitionParams(
    /** Which layer currently disables bypass mode. Omit it when nothing does. */
    @JsonProperty("currentSource") ManagedSettingsBypassDisableSource currentSource,
    /** The account a `managed` disable is pinned to. Omit it when the current source is not `managed`; it is ignored for the other sources. */
    @JsonProperty("currentManagedIdentity") String currentManagedIdentity,
    /** The account signed in now. A `managed` disable pinned to a different account is lifted. */
    @JsonProperty("activeIdentity") String activeIdentity,
    /** What the resolution said about bypass mode. Send `unknown` rather than omitting the field when there is no verdict. */
    @JsonProperty("policy") ManagedSettingsBypassPolicyResolution policy
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveBypassDisableTransitionParams {}

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
        private ManagedSettingsBypassDisableSource currentSource;
        private String currentManagedIdentity;
        private String activeIdentity;
        private ManagedSettingsBypassPolicyResolution policy;

        /**
         * Sets the {@code currentSource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentSource(ManagedSettingsBypassDisableSource value) {
            this.currentSource = value;
            return this;
        }

        /**
         * Sets the {@code currentManagedIdentity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentManagedIdentity(String value) {
            this.currentManagedIdentity = value;
            return this;
        }

        /**
         * Sets the {@code activeIdentity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder activeIdentity(String value) {
            this.activeIdentity = value;
            return this;
        }

        /**
         * Sets the {@code policy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policy(ManagedSettingsBypassPolicyResolution value) {
            this.policy = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsResolveBypassDisableTransitionParams build() {
            if (activeIdentity == null) throw new IllegalStateException("Missing required property: activeIdentity");
            if (policy == null) throw new IllegalStateException("Missing required property: policy");
            return new ManagedSettingsResolveBypassDisableTransitionParams(currentSource, currentManagedIdentity, activeIdentity, policy);
        }
    }
}
