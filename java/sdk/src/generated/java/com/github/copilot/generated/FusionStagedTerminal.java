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
 * Internal durable terminal request staged by a HydraFusion phase until an idempotent final commit selects it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionStagedTerminal(
    @JsonProperty("assistantMessage") Object assistantMessage,
    @JsonProperty("toolName") String toolName,
    @JsonProperty("toolCallId") String toolCallId,
    @JsonProperty("arguments") String arguments,
    @JsonProperty("phaseId") String phaseId
) {
}
