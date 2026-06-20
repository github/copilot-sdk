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
 * A response body chunk or terminal error.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmInferenceHttpResponseChunkParams(
    /** Matches the requestId from the originating httpRequestStart frame. */
    @JsonProperty("requestId") String requestId,
    /** Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty (e.g. when the response body is empty: send a single chunk with empty data and end=true). */
    @JsonProperty("data") String data,
    /** When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text. */
    @JsonProperty("binary") Boolean binary,
    /** When true, this is the final body chunk for the response. The runtime treats the response body as complete after receiving an end-marked chunk. */
    @JsonProperty("end") Boolean end,
    /** Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored. */
    @JsonProperty("error") LlmInferenceHttpResponseChunkError error
) {
}
