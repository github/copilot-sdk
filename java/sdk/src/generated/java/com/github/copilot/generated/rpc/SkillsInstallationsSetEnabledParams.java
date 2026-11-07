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
 * Persisted enablement update for one owned Skill installation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsInstallationsSetEnabledParams(
    /** Required authenticated bound Skill installation capability. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Exact receipt identity to update. */
    @JsonProperty("installationId") String installationId,
    /** Persisted enablement value. */
    @JsonProperty("enabled") Boolean enabled,
    /** Existing selected local session to reconcile after persistence. */
    @JsonProperty("policySessionId") String policySessionId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsInstallationsSetEnabledParams {}

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
        private String installationId;
        private Boolean enabled;
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
         * Sets the {@code installationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installationId(String value) {
            this.installationId = value;
            return this;
        }

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
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
        public SkillsInstallationsSetEnabledParams build() {
            if (contract == null) throw new IllegalStateException("Missing required property: contract");
            if (installationId == null) throw new IllegalStateException("Missing required property: installationId");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            if (policySessionId == null) throw new IllegalStateException("Missing required property: policySessionId");
            return new SkillsInstallationsSetEnabledParams(contract, installationId, enabled, policySessionId);
        }
    }
}
