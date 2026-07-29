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
 * Request parameters for the {@code session.agent.list} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionAgentListParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** When true, request the session's configured built-in agents alongside custom agents. Listing applies feature, context, inclusion, exclusion, and user-disabled-agent policy, but does not evaluate transient invocation requirements such as model availability. Built-in metadata may be omitted when the session cannot project it, such as a relay session. */
    @JsonProperty("includeBuiltInAgents") Boolean includeBuiltInAgents,
    /** When true, request authored base prompt text on each AgentInfo. Prompt text may be omitted when unavailable, such as for agents projected through a relay session. */
    @JsonProperty("includePrompt") Boolean includePrompt
) {
}
