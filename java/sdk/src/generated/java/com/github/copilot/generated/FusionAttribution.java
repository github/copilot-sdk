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
 * Experimental attribution linking an ordinary event to the HydraFusion turn, phase, and concrete source that produced it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionAttribution(
    /** Stable identifier for the HydraFusion turn that produced the event. */
    @JsonProperty("fusionId") String fusionId,
    /** Idempotency identifier for the authoritative commit, when the event belongs to the selected output. */
    @JsonProperty("commitId") String commitId,
    /** Synthetic HydraFusion model selected for the session. */
    @JsonProperty("syntheticModel") String syntheticModel,
    /** HydraFusion routing policy used for the turn. */
    @JsonProperty("policy") String policy,
    /** HydraFusion orchestration pattern selected for the turn. */
    @JsonProperty("pattern") String pattern,
    /** Identifier of the concrete phase that produced the event. */
    @JsonProperty("phaseId") String phaseId,
    /** Kind of concrete phase that produced the event. */
    @JsonProperty("phaseKind") String phaseKind,
    /** Semantic role assigned to the concrete phase. */
    @JsonProperty("role") String role,
    /** Concrete model that produced the attributed event. */
    @JsonProperty("sourceModel") String sourceModel,
    /** Conversation scope in which the concrete phase executed. */
    @JsonProperty("conversationScope") String conversationScope,
    /** Phase whose output supplied the authoritative content, when different from the executing phase. */
    @JsonProperty("sourcePhaseId") String sourcePhaseId
) {
}
