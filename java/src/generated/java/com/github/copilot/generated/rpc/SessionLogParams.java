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
 * Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLogParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Human-readable message */
    @JsonProperty("message") String message,
    /** Log severity level. Determines how the message is displayed in the timeline. Defaults to "info". */
    @JsonProperty("level") SessionLogLevel level,
    /** Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification". */
    @JsonProperty("type") String type,
    /** When true, the message is transient and not persisted to the session event log on disk */
    @JsonProperty("ephemeral") Boolean ephemeral,
    /** Optional URL the user can open in their browser for more details */
    @JsonProperty("url") String url,
    /** Optional actionable tip displayed alongside the message. Only honored on `level: "info"`. */
    @JsonProperty("tip") String tip
) {
}
