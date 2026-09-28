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
 * An explicit numbered-page request. The SDK treats the token as opaque; only the runtime decodes it and changes its targetPage. Authority validation binds navigation to the original search. No snapshot stability or token TTL is promised.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchPage(
    /** Opaque authority-issued pagination token from an earlier response. Never decode, modify or log it in an SDK consumer. */
    @JsonProperty("token") String token,
    /** Requested one-based page. Must not exceed either the token's signed pageCount or the navigation window ceil(1000 / pageSize). Repeat the search without page to discover newly available pages beyond that signed pageCount. */
    @JsonProperty("number") Long number
) {
}
