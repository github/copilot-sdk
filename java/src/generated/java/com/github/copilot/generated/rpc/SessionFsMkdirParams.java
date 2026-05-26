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
 * Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsMkdirParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Path using SessionFs conventions */
    @JsonProperty("path") String path,
    /** Create parent directories as needed */
    @JsonProperty("recursive") Boolean recursive,
    /** Optional POSIX-style mode for newly created directories */
    @JsonProperty("mode") Long mode
) {
}
