/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A completed catalog search containing inert candidate summaries. MCP server and AI skill variants carry a single-use handle; the Agent Plugin variant is handleless.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogSearchSucceeded extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "succeeded";

    @Override
    public String getKind() { return kind; }

    /** Pseudonymous identifier for this search, issued by the runtime or by the catalog authority it queried and never by the caller, so it cannot be forged or replayed to attribute an install to a search that never happened. Always present on a success, so a result set can be tied to the installs it leads to. It identifies a search rather than a person: it is derived from no user, account, device, or query data, and must never be joined with user identity to re-identify anyone. */
    @JsonProperty("searchId")
    private String searchId;

    /** Matching candidates, never more than the requested limit. All text is inert untrusted data. */
    @JsonProperty("candidates")
    private List<CatalogCandidate> candidates;

    /** Legacy indication that the authority returned a page token. Preserved for compatibility; this is not a has-next-page indicator. Use pagination.hasNextPage when pagination metadata is present. */
    @JsonProperty("truncated")
    private Boolean truncated;

    /** Protocol version and capabilities the runtime honoured. */
    @JsonProperty("negotiated")
    private CatalogNegotiatedContract negotiated;

    /** Navigation metadata for callers explicitly requiring catalog-search-pagination. Omitted when the authority returns no token, including tokenless first-page and continuation responses. Counts are never substituted from candidates.length. */
    @JsonProperty("pagination")
    private CatalogSearchPagination pagination;

    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }

    public List<CatalogCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<CatalogCandidate> candidates) { this.candidates = candidates; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }

    public CatalogNegotiatedContract getNegotiated() { return negotiated; }
    public void setNegotiated(CatalogNegotiatedContract negotiated) { this.negotiated = negotiated; }

    public CatalogSearchPagination getPagination() { return pagination; }
    public void setPagination(CatalogSearchPagination pagination) { this.pagination = pagination; }


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
        private String searchId;
        private List<CatalogCandidate> candidates;
        private Boolean truncated;
        private CatalogNegotiatedContract negotiated;
        private CatalogSearchPagination pagination;

        /**
         * Sets the {@code searchId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder searchId(String value) {
            this.searchId = value;
            return this;
        }

        /**
         * Sets the {@code candidates} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder candidates(List<CatalogCandidate> value) {
            this.candidates = value;
            return this;
        }

        /**
         * Sets the {@code truncated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder truncated(Boolean value) {
            this.truncated = value;
            return this;
        }

        /**
         * Sets the {@code negotiated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder negotiated(CatalogNegotiatedContract value) {
            this.negotiated = value;
            return this;
        }

        /**
         * Sets the {@code pagination} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pagination(CatalogSearchPagination value) {
            this.pagination = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogSearchSucceeded build() {
            if (searchId == null) throw new IllegalStateException("Missing required property: searchId");
            if (candidates == null) throw new IllegalStateException("Missing required property: candidates");
            if (truncated == null) throw new IllegalStateException("Missing required property: truncated");
            if (negotiated == null) throw new IllegalStateException("Missing required property: negotiated");
            CatalogSearchSucceeded instance = new CatalogSearchSucceeded();
            instance.setSearchId(searchId);
            instance.setCandidates(candidates);
            instance.setTruncated(truncated);
            instance.setNegotiated(negotiated);
            instance.setPagination(pagination);
            return instance;
        }
    }
}
