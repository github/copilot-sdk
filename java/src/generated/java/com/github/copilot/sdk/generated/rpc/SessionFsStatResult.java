/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code sessionFs.stat} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsStatResult(
    /** Whether the path is a file */
    @JsonProperty("isFile") Boolean isFile,
    /** Whether the path is a directory */
    @JsonProperty("isDirectory") Boolean isDirectory,
    /** File size in bytes */
    @JsonProperty("size") Double size,
    /** ISO 8601 timestamp of last modification */
    @JsonProperty("mtime") String mtime,
    /** ISO 8601 timestamp of creation */
    @JsonProperty("birthtime") String birthtime
) {
}
