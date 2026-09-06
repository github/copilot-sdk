/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Public owner attribution for a client-owned task. Identifiers are opaque and never authorize requests.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskClientOwner(
    /** Opaque session-scoped participant identity */
    @JsonProperty("participantId") String participantId,
    /** Opaque identity of the currently or most recently bound session join */
    @JsonProperty("joinId") String joinId,
    /** Class of the task owner */
    @JsonProperty("kind") TaskClientOwnerKind kind,
    /** Display-only owner name */
    @JsonProperty("displayName") String displayName,
    /** Display-only owner source */
    @JsonProperty("source") String source,
    /** Whether this task's bound join is currently connected */
    @JsonProperty("presence") TaskClientOwnerPresence presence,
    /** ISO 8601 timestamp when the bound join disconnected */
    @JsonProperty("disconnectedAt") OffsetDateTime disconnectedAt
) {
}
