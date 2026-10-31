/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Authoritative snapshot of an Autopilot permission-recovery episode
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRecoveryData(
    /** Stable identifier shared by every transition in this recovery episode */
    @JsonProperty("episodeId") String episodeId,
    /** Root or child execution-session identifier that owns this recovery episode; omitted for legacy snapshots */
    @JsonProperty("ownerSessionId") String ownerSessionId,
    /** Subagent identifier that owns this recovery episode when the request was delegated through a parent permission service */
    @JsonProperty("ownerAgentId") String ownerAgentId,
    /** Current lifecycle state of the recovery episode */
    @JsonProperty("status") PermissionRecoveryStatus status,
    /** Policy selected from the current client's response capability; mode or client changes may update it during recovery */
    @JsonProperty("onBlocked") PermissionRecoveryOnBlocked onBlocked,
    /** Controlled reason for the latest episode transition */
    @JsonProperty("reason") PermissionRecoveryReason reason,
    /** Maximum number of distinct autonomous permission attempts allowed before escalation */
    @JsonProperty("maxAttempts") Long maxAttempts,
    /** Ordered privacy-safe record of permission attempts and the successful alternative, when any */
    @JsonProperty("attempts") List<PermissionRecoveryAttempt> attempts,
    /** Optional replay checkpoint for the epoch-scoped permission-recovery breaker. New runtimes persist it on state-changing recovery snapshots; older runtimes ignore it. */
    @JsonProperty("runState") PermissionRecoveryRunState runState
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param episodeId Stable identifier shared by every transition in this recovery episode
     * @param ownerSessionId Root or child execution-session identifier that owns this recovery episode; omitted for legacy snapshots
     * @param ownerAgentId Subagent identifier that owns this recovery episode when the request was delegated through a parent permission service
     * @param status Current lifecycle state of the recovery episode
     * @param onBlocked Policy selected from the current client's response capability; mode or client changes may update it during recovery
     * @param reason Controlled reason for the latest episode transition
     * @param maxAttempts Maximum number of distinct autonomous permission attempts allowed before escalation
     * @param attempts Ordered privacy-safe record of permission attempts and the successful alternative, when any
     */
    public PermissionRecoveryData(
        String episodeId,
        String ownerSessionId,
        String ownerAgentId,
        PermissionRecoveryStatus status,
        PermissionRecoveryOnBlocked onBlocked,
        PermissionRecoveryReason reason,
        Long maxAttempts,
        List<PermissionRecoveryAttempt> attempts
    ) {
        this(episodeId, ownerSessionId, ownerAgentId, status, onBlocked, reason, maxAttempts, attempts, null);
    }
}
