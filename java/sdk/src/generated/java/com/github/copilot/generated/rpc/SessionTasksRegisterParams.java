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
 * Registers or reclaims a client-owned task.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksRegisterParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Task kind */
    @JsonProperty("type") TaskClientType type,
    /** Owner-scoped idempotency key used for registration and reclaim */
    @JsonProperty("clientTaskId") String clientTaskId,
    /** Human-readable description of the external work */
    @JsonProperty("description") String description,
    /** Optional short display name for the external work */
    @JsonProperty("displayName") String displayName,
    /** Whether the owner supports runtime cancellation requests */
    @JsonProperty("cancellable") Boolean cancellable,
    /** Expected current sequence for idempotent registration or orphan reclaim */
    @JsonProperty("expectedSequence") Long expectedSequence
) {
}
