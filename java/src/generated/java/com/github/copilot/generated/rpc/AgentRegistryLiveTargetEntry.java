/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRegistryLiveTargetEntry(
    /** Registry entry schema version (1 = ui-server, 2 = managed-server) */
    @JsonProperty("schemaVersion") Long schemaVersion,
    /** Process kind tag for the registry entry */
    @JsonProperty("kind") AgentRegistryLiveTargetEntryKind kind,
    /** Operating-system pid of the process owning this entry */
    @JsonProperty("pid") Long pid,
    /** Bind host for the entry's JSON-RPC server */
    @JsonProperty("host") String host,
    /** TCP port the entry's JSON-RPC server is listening on */
    @JsonProperty("port") Long port,
    /** Connection token (null when the target is unauthenticated) */
    @JsonProperty("token") String token,
    /** Session ID of the foreground session for this entry */
    @JsonProperty("sessionId") String sessionId,
    /** Friendly session name (when set) */
    @JsonProperty("sessionName") String sessionName,
    /** Working directory of the session (when known) */
    @JsonProperty("cwd") String cwd,
    /** Git branch of the session (when known) */
    @JsonProperty("branch") String branch,
    /** Model identifier currently selected for the session */
    @JsonProperty("model") String model,
    /** Coarse lifecycle status of the foreground session */
    @JsonProperty("status") AgentRegistryLiveTargetEntryStatus status,
    /** Kind of attention required when status === "attention". Meaningful only when status === "attention". */
    @JsonProperty("attentionKind") AgentRegistryLiveTargetEntryAttentionKind attentionKind,
    /** Monotonic per-publisher revision counter incremented on every status update. Lets watchers detect transient flips. */
    @JsonProperty("statusRevision") Long statusRevision,
    /** How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled. */
    @JsonProperty("lastTerminalEvent") AgentRegistryLiveTargetEntryLastTerminalEvent lastTerminalEvent,
    /** ISO 8601 timestamp captured at registration */
    @JsonProperty("startedAt") String startedAt,
    /** Copilot CLI version that wrote the entry */
    @JsonProperty("copilotVersion") String copilotVersion,
    /** Wall-clock milliseconds since the watcher last observed this entry (heartbeat freshness) */
    @JsonProperty("lastSeenMs") Long lastSeenMs
) {
}
