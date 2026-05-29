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
 * Inputs to spawn a managed-server child via the controller's spawn delegate.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRegistrySpawnParams(
    /** Working directory for the spawned child (must be an existing directory) */
    @JsonProperty("cwd") String cwd,
    /** Custom or built-in agent name (e.g. 'explore'). When omitted, the child uses its own default. */
    @JsonProperty("agentName") String agentName,
    /** Model identifier to apply to the new session */
    @JsonProperty("model") String model,
    /** Friendly session name. Must satisfy validateSessionName: non-empty, no leading/trailing whitespace, <=100 chars, no control chars, no double quotes. */
    @JsonProperty("name") String name,
    /** Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode. */
    @JsonProperty("permissionMode") AgentRegistrySpawnPermissionMode permissionMode,
    /** Optional first user message. Forwarded to the caller (the CLI's spawn wrapper sends it post-attach via the standard LocalRpcSession.send path). */
    @JsonProperty("initialPrompt") String initialPrompt
) {
}
