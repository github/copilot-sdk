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
 * Prompt-safe terminal factory outcome.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryRunTerminal(
    /** Human-readable terminal reason. */
    @JsonProperty("reason") String reason,
    /** Machine-readable terminal failure. */
    @JsonProperty("failure") Object failure,
    /** Human-readable terminal error. */
    @JsonProperty("error") String error,
    /** Prompt-safe preview of the completed result. */
    @JsonProperty("resultPreview") String resultPreview
) {
}
