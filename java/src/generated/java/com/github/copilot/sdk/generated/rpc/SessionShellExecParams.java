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
 * Request parameters for the {@code session.shell.exec} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionShellExecParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Shell command to execute */
    @JsonProperty("command") String command,
    /** Working directory (defaults to session working directory) */
    @JsonProperty("cwd") String cwd,
    /** Timeout in milliseconds (default: 30000) */
    @JsonProperty("timeout") Long timeout
) {
}
