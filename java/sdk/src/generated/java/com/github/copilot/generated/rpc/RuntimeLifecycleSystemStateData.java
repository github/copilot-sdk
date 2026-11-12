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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Experimental, observational-only state of one runtime system after an operation transition. Observation and delivery never control runtime work. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleSystemStateData(
    /** Session whose lifecycle owns this system. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque identity for one session registry generation. */
    @JsonProperty("incarnationId") String incarnationId,
    /** Stable subsystem identifier. */
    @JsonProperty("system") String system,
    /** Orders every observed change to this system in the incarnation. */
    @JsonProperty("systemRevision") Long systemRevision,
    /** Authoritative condition retained after this change. Initial shared values are `current`, `disabled`, `degraded`, and `quiescent`; integrations can add bounded values as the contract matures. */
    @JsonProperty("condition") String condition,
    /** Operation transition that produced this system revision. */
    @JsonProperty("changedOperation") RuntimeLifecycleOperation changedOperation,
    /** Complete active operation set after applying the change. */
    @JsonProperty("activeOperations") List<RuntimeLifecycleOperation> activeOperations
) {
}
