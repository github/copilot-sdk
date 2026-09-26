/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Exact observed event identity. No private registration generation or execution handle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerEventReference(
    /** Actual runtime session scope, at most 256 UTF-8 bytes. */
    @JsonProperty("sessionId") String sessionId,
    /** Actual event occurrence UUID; copied without normalization. */
    @JsonProperty("eventId") UUID eventId,
    /** Actual event agent scope, absent for a root occurrence; at most 256 UTF-8 bytes. */
    @JsonProperty("agentId") String agentId,
    /** Type of the observed occurrence. */
    @JsonProperty("eventType") WorkerEventType eventType,
    /** Explicit provenance of this reference. */
    @JsonProperty("provenance") WorkerObservationProvenance provenance
) {
}
