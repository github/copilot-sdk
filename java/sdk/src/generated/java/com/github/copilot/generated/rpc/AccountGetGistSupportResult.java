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
 * Whether the identity can create gists, and why not when it cannot.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AccountGetGistSupportResult(
    /** True when the identity can create gists. */
    @JsonProperty("supported") Boolean supported,
    /** Machine-readable reason category when unsupported. */
    @JsonProperty("kind") String kind,
    /** Human-readable explanation to show where the gist action would have been. */
    @JsonProperty("reason") String reason
) {
}
