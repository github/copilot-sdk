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
 * Retrieves one generated feed, computed on every request, by strategy and key, such as `telemetry` and `popular`. Requires catalog-feeds and catalog-search-credential-required; add catalog-search-session-bound with policySessionId to retain the selected session's authority for installation, exactly as for catalog.search.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogGetGeneratedFeedParams(
    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Select an existing attached local session. Requires authenticated, session-bound catalogue support.
The runtime never creates, resumes or reconfigures a session to honour this selector. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Generation strategy, such as `telemetry`. */
    @JsonProperty("strategy") String strategy,
    /** Feed key within the strategy, such as `popular`. */
    @JsonProperty("key") String key,
    /** Accept only resources currently assessed at one of these trust tiers. Entries whose resource is in another tier, or has none, are returned as filtered in their original position. Omit for an unfiltered feed. */
    @JsonProperty("trustTiers") List<CatalogTrustTier> trustTiers
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogGetGeneratedFeedParams {}

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
        private String policySessionId;
        private String strategy;
        private String key;
        private List<CatalogTrustTier> trustTiers;

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
         * Sets the {@code strategy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder strategy(String value) {
            this.strategy = value;
            return this;
        }

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code trustTiers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder trustTiers(List<CatalogTrustTier> value) {
            this.trustTiers = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogGetGeneratedFeedParams build() {
            if (contract == null) throw new IllegalStateException("Missing required property: contract");
            if (strategy == null) throw new IllegalStateException("Missing required property: strategy");
            if (key == null) throw new IllegalStateException("Missing required property: key");
            return new CatalogGetGeneratedFeedParams(contract, policySessionId, strategy, key, trustTiers);
        }
    }
}
