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
 * Observed state of one readiness or convergence operation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleOperation(
    /** Opaque operation identifier, unique within the lifecycle incarnation and system. */
    @JsonProperty("operationId") String operationId,
    /** Orders transitions of this operation. */
    @JsonProperty("operationRevision") Long operationRevision,
    /** System-owned reason the operation started. */
    @JsonProperty("trigger") String trigger,
    /** System-owned lifecycle stage. */
    @JsonProperty("phase") String phase,
    /** System-owned production boundary within the phase. */
    @JsonProperty("step") String step,
    /** How the operation ended. Absent while the operation remains active. */
    @JsonProperty("disposition") RuntimeLifecycleDisposition disposition,
    /** Timestamps recorded by the lifecycle service for this operation revision. */
    @JsonProperty("timing") RuntimeLifecycleOperationTiming timing
) {
}
