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
 * Tracked client-owned task metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskClientInfo(
    /** Task kind */
    @JsonProperty("type") TaskClientType type,
    /** Canonical runtime-generated task identifier */
    @JsonProperty("id") String id,
    /** Owner-scoped registration and reclaim key */
    @JsonProperty("clientTaskId") String clientTaskId,
    /** Optional task display name */
    @JsonProperty("displayName") String displayName,
    /** Task description */
    @JsonProperty("description") String description,
    /** Client task lifecycle status */
    @JsonProperty("status") TaskClientStatus status,
    /** Public attribution and presence for the task owner */
    @JsonProperty("owner") TaskClientOwner owner,
    /** ISO 8601 timestamp when the task started */
    @JsonProperty("startedAt") OffsetDateTime startedAt,
    /** ISO 8601 timestamp of the latest accepted lifecycle change */
    @JsonProperty("updatedAt") OffsetDateTime updatedAt,
    /** ISO 8601 timestamp when the task reached a terminal status */
    @JsonProperty("completedAt") OffsetDateTime completedAt,
    /** Accumulated active execution time in milliseconds */
    @JsonProperty("activeTimeMs") Long activeTimeMs,
    /** ISO 8601 timestamp when the current active segment started */
    @JsonProperty("activeStartedAt") OffsetDateTime activeStartedAt,
    /** ISO 8601 timestamp when the connected owner entered idle status */
    @JsonProperty("idleSince") OffsetDateTime idleSince,
    /** ISO 8601 timestamp of the most recent orphan transition */
    @JsonProperty("orphanedAt") OffsetDateTime orphanedAt,
    /** ISO 8601 timestamp of the most recent successful reclaim */
    @JsonProperty("reclaimedAt") OffsetDateTime reclaimedAt,
    /** Execution mode, which is always background for client-owned tasks */
    @JsonProperty("executionMode") TaskClientExecutionMode executionMode,
    /** Whether the currently bound owner can receive a cancellation request */
    @JsonProperty("canCancel") Boolean canCancel,
    /** Sequence number of the latest accepted owner update */
    @JsonProperty("sequence") Long sequence,
    /** Opaque successful terminal result supplied by the task owner */
    @JsonProperty("result") Object result,
    /** Human-readable terminal failure message */
    @JsonProperty("error") String error,
    /** Optional owner-supplied terminal failure code */
    @JsonProperty("errorCode") String errorCode,
    /** Human-readable reason for terminal cancellation */
    @JsonProperty("cancellationReason") String cancellationReason
) {
}
