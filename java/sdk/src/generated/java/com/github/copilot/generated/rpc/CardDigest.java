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
 * Semantic digest of a strictly parsed and schema-validated JSON MCP card. Both URL-backed and embedded cards are canonicalised with RFC 8785 JSON Canonicalization Scheme, encoded as UTF-8, and hashed with SHA-256.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardDigest(
    /** Digest algorithm and canonical representation */
    @JsonProperty("algorithm") CardDigestAlgorithm algorithm,
    /** SHA-256 digest of the RFC 8785 canonical UTF-8 bytes, encoded as exactly 64 lowercase hexadecimal characters. */
    @JsonProperty("value") String value
) {
}
