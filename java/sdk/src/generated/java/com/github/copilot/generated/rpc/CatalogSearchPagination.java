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
}
