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
 * Raw file bytes encoded as base64 and the authoritative total file byte length at the read, or a filesystem error.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsReadFileRangeResult(
    /** Standard padded base64 encoding of the requested bytes. Range boundaries may split UTF-8 characters. */
    @JsonProperty("content") String content,
    /** Total raw byte length of the file, independent of the requested range or base64 encoding. */
    @JsonProperty("byteLength") Long byteLength,
    /** Describes a filesystem error. */
    @JsonProperty("error") SessionFsError error
) {
}
