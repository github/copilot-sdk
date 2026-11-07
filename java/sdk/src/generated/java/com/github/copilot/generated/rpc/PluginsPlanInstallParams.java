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
 * Side-effect-free planning of one Agent Plugin search candidate through its Agent Finder materialisation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginsPlanInstallParams(
    /** Required authenticated bound catalogue and agent-plugin-confirmed-installation capabilities. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Fresh single-use Agent Plugin candidate handle returned by a bound catalogue search. */
    @JsonProperty("candidateHandle") String candidateHandle,
    /** Existing local session attached to this connection. */
    @JsonProperty("policySessionId") String policySessionId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PluginsPlanInstallParams {}

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
        private CatalogClientContract contract;
        private String candidateHandle;
        private String policySessionId;

        /**
         * Sets the {@code contract} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contract(CatalogClientContract value) {
            this.contract = value;
            return this;
        }

        /**
         * Sets the {@code candidateHandle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder candidateHandle(String value) {
            this.candidateHandle = value;
            return this;
        }

        /**
         * Sets the {@code policySessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policySessionId(String value) {
            this.policySessionId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PluginsPlanInstallParams build() {
            if (contract == null) throw new IllegalStateException("Missing required property: contract");
            if (candidateHandle == null) throw new IllegalStateException("Missing required property: candidateHandle");
            if (policySessionId == null) throw new IllegalStateException("Missing required property: policySessionId");
            return new PluginsPlanInstallParams(contract, candidateHandle, policySessionId);
        }
    }
}
