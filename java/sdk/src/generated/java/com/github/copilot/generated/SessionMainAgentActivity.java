/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Current main-agent state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMainAgentActivity(
    /** Whether the main agent is executing, blocked on interaction, or idle. */
    @JsonProperty("state") SessionMainAgentState state,
    /** Why the main agent is waiting. Permission takes precedence when multiple prompt types are pending. */
    @JsonProperty("waitReason") SessionMainAgentWaitReason waitReason,
    /** Whether the current main-agent turn can be interrupted without cancelling background agents or processes. */
    @JsonProperty("abortable") Boolean abortable
) {
}
