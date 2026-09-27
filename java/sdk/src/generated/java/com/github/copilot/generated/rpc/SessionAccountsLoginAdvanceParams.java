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
 * Advance an in-flight login flow, optionally fulfilling an input-required step.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionAccountsLoginAdvanceParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque flow id from begin. */
    @JsonProperty("flowId") String flowId,
    /** Neutral input fulfilling a preceding input-required step (e.g. a GHEC host); ignored otherwise. */
    @JsonProperty("input") String input
) {
}
