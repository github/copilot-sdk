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
 * Persisted local session metadata, including identifiers, timestamps, summary/name, client, context, detached state, and task ID.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalSessionMetadataValue(
    /** Stable session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Session creation time as an ISO 8601 timestamp */
    @JsonProperty("startTime") String startTime,
    /** Last-modified time of the session's persisted state, as ISO 8601 */
    @JsonProperty("modifiedTime") String modifiedTime,
    /** Short summary of the session, when one has been derived */
    @JsonProperty("summary") String summary,
    /** Optional human-friendly name set via /rename */
    @JsonProperty("name") String name,
    /** Runtime client name that created this session */
    @JsonProperty("clientName") String clientName,
    /** Always false for local sessions. */
    @JsonProperty("isRemote") Boolean isRemote,
    /** True for detached maintenance sessions that should be hidden from normal resume lists. */
    @JsonProperty("isDetached") Boolean isDetached,
    /** Pre-resolved working-directory context for session startup. */
    @JsonProperty("context") SessionContext context,
    /** GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control. */
    @JsonProperty("mcTaskId") String mcTaskId,
    /** Runtime client name that most recently ran this session: the client that created it, or a client that resumed it and then ran it. Resuming a session only to observe it does not change this. Omitted when unknown, such as for sessions persisted before this field existed. */
    @JsonProperty("lastClientName") String lastClientName
) {

    /**
     * Creates local session metadata without the client that last ran the session.
     *
     * @param sessionId Stable session identifier
     * @param startTime Session creation time as an ISO 8601 timestamp
     * @param modifiedTime Last-modified time of the session's persisted state, as ISO 8601
     * @param summary Short summary of the session, when one has been derived
     * @param name Optional human-friendly name set via /rename
     * @param clientName Runtime client name that created this session
     * @param isRemote Always false for local sessions.
     * @param isDetached True for detached maintenance sessions that should be hidden from normal resume lists.
     * @param context Pre-resolved working-directory context for session startup.
     * @param mcTaskId GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.
     */
    public LocalSessionMetadataValue(
        String sessionId,
        String startTime,
        String modifiedTime,
        String summary,
        String name,
        String clientName,
        Boolean isRemote,
        Boolean isDetached,
        SessionContext context,
        String mcTaskId
    ) {
        this(sessionId, startTime, modifiedTime, summary, name, clientName, isRemote, isDetached, context, mcTaskId, null);
    }
}
