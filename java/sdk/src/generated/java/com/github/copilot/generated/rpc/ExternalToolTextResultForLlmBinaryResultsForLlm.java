/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Binary result returned by a tool for the model
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalToolTextResultForLlmBinaryResultsForLlm(
    /** Binary result type discriminator. Use "image" for images and "resource" for other binary data. */
    @JsonProperty("type") ExternalToolTextResultForLlmBinaryResultsForLlmType type,
    /** Base64-encoded binary data */
    @JsonProperty("data") String data,
    /** MIME type of the binary data */
    @JsonProperty("mimeType") String mimeType,
    /** Human-readable description of the binary data */
    @JsonProperty("description") String description,
    /** Optional metadata from the producing tool. */
    @JsonProperty("metadata") Map<String, Object> metadata
) {
}
