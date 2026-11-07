/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Authority-reported navigation metadata, returned only to callers requiring catalog-search-pagination and only when a supported token is present. Tokenless first-page and continuation responses omit this object; no counts are inferred from candidates. The opaque token may be retained for previous or numbered navigation even when hasNextPage is false.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchPagination(
    /** Opaque pagination token. Session-bound search returns an expiring runtime-owned reference retaining the exact private authority token, original search and authority. Legacy unbound search returns the authority token unchanged, without a runtime-created expiry. Only the runtime unwraps tokens or changes targetPage; SDK consumers must not decode, modify or log them. */
    @JsonProperty("token") String token,
    /** One-based page returned by the authority. */
    @JsonProperty("currentPage") Long currentPage,
    /** Page size bound to the search, equal to the effective request limit. */
    @JsonProperty("pageSize") Long pageSize,
    /** Backend-reported count for this response, not the number of returned candidates. Its relationship to the full query result set is unknown. */
    @JsonProperty("totalCount") Long totalCount,
    /** The relationship of totalCount to the complete query result set is unknown; neither exactness nor a lower-bound guarantee is implied. */
    @JsonProperty("totalCountRelation") CatalogSearchTotalCountRelation totalCountRelation,
    /** Backend-reported page count, which may exceed maxPage. Present pagination metadata always describes a multi-page result; zero- and single-page responses omit pagination. Navigation targets must also be within the signed pageCount carried by the supplied token. */
    @JsonProperty("pageCount") Long pageCount,
    /** Navigation window ceiling ceil(1000 / pageSize), not the number of existing pages. Legal targets must not exceed this ceiling or the token's signed pageCount. */
    @JsonProperty("maxPage") Long maxPage,
    /** Whether the authority token advertises a valid next target within the navigation window. Not inferred from token presence, truncated, or currentPage being less than pageCount. */
    @JsonProperty("hasNextPage") Boolean hasNextPage
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogSearchPagination {}

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
        private String token;
        private Long currentPage;
        private Long pageSize;
        private Long totalCount;
        private CatalogSearchTotalCountRelation totalCountRelation;
        private Long pageCount;
        private Long maxPage;
        private Boolean hasNextPage;

        /**
         * Sets the {@code token} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder token(String value) {
            this.token = value;
            return this;
        }

        /**
         * Sets the {@code currentPage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentPage(Long value) {
            this.currentPage = value;
            return this;
        }

        /**
         * Sets the {@code pageSize} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pageSize(Long value) {
            this.pageSize = value;
            return this;
        }

        /**
         * Sets the {@code totalCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalCount(Long value) {
            this.totalCount = value;
            return this;
        }

        /**
         * Sets the {@code totalCountRelation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalCountRelation(CatalogSearchTotalCountRelation value) {
            this.totalCountRelation = value;
            return this;
        }

        /**
         * Sets the {@code pageCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pageCount(Long value) {
            this.pageCount = value;
            return this;
        }

        /**
         * Sets the {@code maxPage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPage(Long value) {
            this.maxPage = value;
            return this;
        }

        /**
         * Sets the {@code hasNextPage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasNextPage(Boolean value) {
            this.hasNextPage = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogSearchPagination build() {
            if (token == null) throw new IllegalStateException("Missing required property: token");
            if (currentPage == null) throw new IllegalStateException("Missing required property: currentPage");
            if (pageSize == null) throw new IllegalStateException("Missing required property: pageSize");
            if (totalCount == null) throw new IllegalStateException("Missing required property: totalCount");
            if (totalCountRelation == null) throw new IllegalStateException("Missing required property: totalCountRelation");
            if (pageCount == null) throw new IllegalStateException("Missing required property: pageCount");
            if (maxPage == null) throw new IllegalStateException("Missing required property: maxPage");
            if (hasNextPage == null) throw new IllegalStateException("Missing required property: hasNextPage");
            return new CatalogSearchPagination(token, currentPage, pageSize, totalCount, totalCountRelation, pageCount, maxPage, hasNextPage);
        }
    }
}
