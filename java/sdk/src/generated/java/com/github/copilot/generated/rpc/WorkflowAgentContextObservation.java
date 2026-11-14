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
 * Requested and resolved context configuration observed when a direct workflow agent launched.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAgentContextObservation(
    /** Context-tier override explicitly supplied by the workflow call. */
    @JsonProperty("requestedTier") ContextTier requestedTier,
    /** Effective context-tier identifier reported by the initialized child session. Native sessions currently report `default` or `long_context`; remote participants may report another supported identifier. */
    @JsonProperty("resolvedTier") String resolvedTier,
    /** Effective prompt-token input budget resolved for the initialized child session. */
    @JsonProperty("promptTokenLimit") Long promptTokenLimit,
    /** Full context-window capacity resolved for the initialized child session. */
    @JsonProperty("maxContextWindowTokens") Long maxContextWindowTokens
) {
}
