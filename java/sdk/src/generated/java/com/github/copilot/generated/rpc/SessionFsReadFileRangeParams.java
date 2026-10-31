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
 * A byte range in a provider-owned file. Offsets and lengths count bytes, not Unicode characters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsReadFileRangeParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Path using SessionFs conventions */
    @JsonProperty("path") String path,
    /** Zero-based byte offset. At or beyond EOF the result contains no bytes. */
    @JsonProperty("offset") Long offset,
    /** Maximum bytes to return. Omit to read through EOF; zero returns only the authoritative file byte length. */
    @JsonProperty("length") Long length
) {
}
