/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomAgentsUpdatedAgent(
    /** Unique identifier for the agent */
    @JsonProperty("id") String id,
    /** Internal name of the agent */
    @JsonProperty("name") String name,
    /** Human-readable display name */
    @JsonProperty("displayName") String displayName,
    /** Description of what the agent does */
    @JsonProperty("description") String description,
    /** Source location: user, project, inherited, remote, or plugin */
    @JsonProperty("source") String source,
    /** List of tool names available to this agent */
    @JsonProperty("tools") List<String> tools,
    /** Whether the agent can be selected by the user */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Model override for this agent, if set */
    @JsonProperty("model") String model
) {
}
