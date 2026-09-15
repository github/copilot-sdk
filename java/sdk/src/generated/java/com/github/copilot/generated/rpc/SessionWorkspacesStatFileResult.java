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
 * Filesystem metadata for a path in the session workspace files directory.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesStatFileResult(
    /** Whether the path identifies a regular file */
    @JsonProperty("isFile") Boolean isFile,
    /** Whether the path identifies a directory */
    @JsonProperty("isDirectory") Boolean isDirectory,
    /** Size in bytes */
    @JsonProperty("size") Double size,
    /** Last modification time in Unix epoch milliseconds */
    @JsonProperty("mtimeMs") Double mtimeMs,
    /** Creation time in Unix epoch milliseconds */
    @JsonProperty("birthtimeMs") Double birthtimeMs
) {
}
