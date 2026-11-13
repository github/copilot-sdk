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
 * Exact lifecycle operation awaited by a real dependency wait.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleBlocker(
    /** System-owned capability required by the progress owner. */
    @JsonProperty("requirement") String requirement,
    /** System that owns the awaited operation. */
    @JsonProperty("system") String system,
    /** Opaque awaited operation identifier. */
    @JsonProperty("operationId") String operationId,
    /** Awaited operation revision observed by this blocker. */
    @JsonProperty("operationRevision") Long operationRevision,
    /** Current phase of the awaited operation. */
    @JsonProperty("currentPhase") String currentPhase,
    /** Current production boundary of the awaited operation. */
    @JsonProperty("currentStep") String currentStep,
    /** How the awaited operation ended when the real wait remains active. */
    @JsonProperty("operationDisposition") RuntimeLifecycleDisposition operationDisposition,
    /** Timing of this dependency wait. */
    @JsonProperty("timing") RuntimeLifecycleBlockerTiming timing
) {
}
