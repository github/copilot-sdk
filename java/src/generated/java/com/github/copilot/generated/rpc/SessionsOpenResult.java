/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result of opening a session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsOpenResult(
    /** Outcome of the open request. */
    @JsonProperty("status") SessionsOpenStatus status,
    /** Opened session ID. Omitted when status is `not_found`. */
    @JsonProperty("sessionId") String sessionId,
    /** In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead. */
    @JsonProperty("sessionApi") Object sessionApi,
    /** Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array. */
    @JsonProperty("startupPrompts") List<String> startupPrompts,
    /** Remote session ID, present when status is `connected`. */
    @JsonProperty("remoteSessionId") String remoteSessionId,
    /** Remote session metadata, present when status is `connected`. */
    @JsonProperty("metadata") RemoteSessionMetadataValue metadata,
    /** Handoff progress steps, present when status is `handed_off`. */
    @JsonProperty("progress") List<SessionsOpenProgress> progress
) {
}
