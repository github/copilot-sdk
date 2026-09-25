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
 * An explicit numbered-page request. SDK consumers treat the token as opaque. For bound search, the runtime unwraps an expiring owner-bound reference to the private authority token; only the runtime changes the authority token's targetPage. Legacy unbound navigation keeps its authority-issued token semantics. No snapshot stability is promised.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchPage(
    /** Opaque pagination token from an earlier response, owner-bound when session-bound search was requested. Never decode, modify or log it in an SDK consumer. Expired or foreign bound references require a fresh bound search, not a legacy retry. */
    @JsonProperty("token") String token,
    /** Requested one-based page. Must not exceed either the token's signed pageCount or the navigation window ceil(1000 / pageSize). Repeat the search without page to discover newly available pages beyond that signed pageCount. */
    @JsonProperty("number") Long number
) {
}
