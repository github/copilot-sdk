/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.tasks.startAgent} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksStartAgentParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Type of agent to start (e.g., 'explore', 'task', 'general-purpose') */
    @JsonProperty("agentType") String agentType,
    /** Task prompt for the agent */
    @JsonProperty("prompt") String prompt,
    /** Short name for the agent, used to generate a human-readable ID */
    @JsonProperty("name") String name,
    /** Short description of the task */
    @JsonProperty("description") String description,
    /** Optional model override */
    @JsonProperty("model") String model
) {
}
