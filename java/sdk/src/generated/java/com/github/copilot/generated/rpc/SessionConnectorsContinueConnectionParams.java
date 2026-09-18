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
 * Explicitly bounded continuation of a pending Connector connection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionConnectorsContinueConnectionParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque continuation ID returned by connect, reconnect, or an earlier continuation. */
    @JsonProperty("continuationId") String continuationId,
    /** Maximum catalog requests made by this call. Must be between one and the capability limit. */
    @JsonProperty("maxAttempts") Long maxAttempts,
    /** Delay in milliseconds between attempts. Must not exceed the capability limit. */
    @JsonProperty("pollIntervalMs") Long pollIntervalMs,
    /** Maximum wall-clock duration in milliseconds for this call. Must be between one and the capability limit. */
    @JsonProperty("deadlineMs") Long deadlineMs
) {
}
