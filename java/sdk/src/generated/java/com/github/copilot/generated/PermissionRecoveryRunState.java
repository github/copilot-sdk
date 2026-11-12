/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Replay-safe epoch counters for Autopilot Assisted Permissions recovery
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRecoveryRunState(
    /** Number of blocked recovery episodes since the latest successful permitted effectful tool call */
    @JsonProperty("consecutiveBlockedEpisodes") Long consecutiveBlockedEpisodes,
    /** Whether the claimed epoch terminal is an authoritative permission-recovery block that remains latched until the next genuine root user message */
    @JsonProperty("permissionRecoveryTerminalClaimed") Boolean permissionRecoveryTerminalClaimed,
    /** Total deferred, denied, or blocked attempt transitions in the current genuine-user-message epoch */
    @JsonProperty("totalDenialDispositions") Long totalDenialDispositions,
    /** Whether a permission-recovery terminal completion has already been claimed for the epoch */
    @JsonProperty("terminalOutcomeClaimed") Boolean terminalOutcomeClaimed
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param consecutiveBlockedEpisodes Number of blocked recovery episodes since the latest successful permitted effectful tool call
     * @param totalDenialDispositions Total deferred, denied, or blocked attempt transitions in the current genuine-user-message epoch
     * @param terminalOutcomeClaimed Whether a permission-recovery terminal completion has already been claimed for the epoch
     */
    public PermissionRecoveryRunState(
        Long consecutiveBlockedEpisodes,
        Long totalDenialDispositions,
        Boolean terminalOutcomeClaimed
    ) {
        this(consecutiveBlockedEpisodes, null, totalDenialDispositions, terminalOutcomeClaimed);
    }
}
