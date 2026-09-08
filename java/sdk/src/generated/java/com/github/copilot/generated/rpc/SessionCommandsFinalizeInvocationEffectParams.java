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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The pending slash-command invocation effect to finalize, plus whether the host applied or cancelled it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCommandsFinalizeInvocationEffectParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The slash-command result object that produced the pending effect, echoed back unchanged. */
    @JsonProperty("effect") Map<String, Object> effect,
    /** Whether the host applied or cancelled the pending invocation effect. */
    @JsonProperty("outcome") CommandsInvocationEffectOutcome outcome
) {
}
