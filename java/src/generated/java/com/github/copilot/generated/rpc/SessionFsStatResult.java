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
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Filesystem metadata for the requested path, or a filesystem error if the stat failed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsStatResult(
    /** Whether the path is a file */
    @JsonProperty("isFile") Boolean isFile,
    /** Whether the path is a directory */
    @JsonProperty("isDirectory") Boolean isDirectory,
    /** File size in bytes */
    @JsonProperty("size") Long size,
    /** ISO 8601 timestamp of last modification */
    @JsonProperty("mtime") OffsetDateTime mtime,
    /** ISO 8601 timestamp of creation */
    @JsonProperty("birthtime") OffsetDateTime birthtime,
    /** Describes a filesystem error. */
    @JsonProperty("error") SessionFsError error
) {
}
