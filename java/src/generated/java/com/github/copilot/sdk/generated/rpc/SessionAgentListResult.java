/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.agent.list} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionAgentListResult(
    /** Available custom agents */
    @JsonProperty("agents") List<SessionAgentListResultAgentsItem> agents
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAgentListResultAgentsItem(
        /** Unique identifier of the custom agent */
        @JsonProperty("name") String name,
        /** Human-readable display name */
        @JsonProperty("displayName") String displayName,
        /** Description of the agent's purpose */
        @JsonProperty("description") String description
    ) {
    }
}
