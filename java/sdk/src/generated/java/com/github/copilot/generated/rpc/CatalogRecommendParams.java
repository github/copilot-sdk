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
 * Initial-page official-product recommendations for a complete task. The runtime fixes recommendationPolicy to official-product and pageSize to 10; callers cannot override the policy, request pagination or fall back to search.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogRecommendParams(
    /** Required contract, including catalog-recommend, catalog-search-session-bound and catalog-search-credential-required. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Existing attached local session whose account, host and connection own the candidate references. Never creates or resumes a session. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Full original task, including action, account, output and host requirements. Forwarded without trimming or rewriting and excluded from telemetry. */
    @JsonProperty("query") String query,
    /** Requested product. Agent Finder resolves aliases and applies its versioned official-product approvals before limiting results. */
    @JsonProperty("product") String product
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogRecommendParams {}

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
        private String query;
        private String product;

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
         * Sets the {@code query} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder query(String value) {
            this.query = value;
            return this;
        }

        /**
         * Sets the {@code product} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder product(String value) {
            this.product = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogRecommendParams build() {
            if (contract == null) throw new IllegalStateException("Missing required property: contract");
            if (policySessionId == null) throw new IllegalStateException("Missing required property: policySessionId");
            if (query == null) throw new IllegalStateException("Missing required property: query");
            if (product == null) throw new IllegalStateException("Missing required property: product");
            return new CatalogRecommendParams(contract, policySessionId, query, product);
        }
    }
}
