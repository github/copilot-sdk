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
 * An opted-in local session, reported by its live owner.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSessionInfo(
    /** Live peer's session identifier. */
    @JsonProperty("sessionId") String sessionId,
    /** Human-readable peer name. */
    @JsonProperty("name") String name,
    /** Peer's current working directory. */
    @JsonProperty("cwd") String cwd,
    /** Peer's local session-state directory containing events.jsonl, when known. */
    @JsonProperty("sessionStatePath") String sessionStatePath,
    /** Either idle or busy. A busy session may still accept messages. */
    @JsonProperty("state") String state,
    /** Peer's selected model, when known. */
    @JsonProperty("model") String model,
    /** The session that created this root through session management, if any. */
    @JsonProperty("parentSessionId") String parentSessionId
) {
}
