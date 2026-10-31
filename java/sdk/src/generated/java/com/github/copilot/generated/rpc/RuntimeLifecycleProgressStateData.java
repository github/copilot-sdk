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
 * Experimental, observational-only state of runtime progress and the lifecycle operations that block it. Observation and delivery never control runtime or model behavior. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleProgressStateData(
    /** Session whose lifecycle owns this progress. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque identity for one session registry generation. */
    @JsonProperty("incarnationId") String incarnationId,
    /** Progress-owner-defined kind. */
    @JsonProperty("kind") String kind,
    /** Opaque progress identifier, unique within the incarnation and kind. */
    @JsonProperty("progressId") String progressId,
    /** Orders changes to this progress record. */
    @JsonProperty("revision") Long revision,
    /** Progress-owner-defined phase. */
    @JsonProperty("phase") String phase,
    /** How the progress record ended. Absent while it remains active. */
    @JsonProperty("disposition") RuntimeLifecycleDisposition disposition,
    /** Immutable causal origin, when another lifecycle progress record caused this one. */
    @JsonProperty("origin") RuntimeLifecycleProgressOrigin origin,
    /** Timestamps recorded by the lifecycle service for this progress revision. */
    @JsonProperty("timing") RuntimeLifecycleProgressTiming timing,
    /** Complete dependency blocker set after this progress change. */
    @JsonProperty("blockers") List<RuntimeLifecycleBlocker> blockers
) {
}
