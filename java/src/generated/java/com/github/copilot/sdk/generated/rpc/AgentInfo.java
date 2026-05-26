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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Schema for the `AgentInfo` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentInfo(
    /** Unique identifier of the custom agent */
    @JsonProperty("name") String name,
    /** Human-readable display name */
    @JsonProperty("displayName") String displayName,
    /** Description of the agent's purpose */
    @JsonProperty("description") String description,
    /** Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path. */
    @JsonProperty("path") String path,
    /** Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned. */
    @JsonProperty("id") String id,
    /** Where the agent definition was loaded from */
    @JsonProperty("source") AgentInfoSource source,
    /** Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only. */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Allowed tool names for this agent. Empty array means none; omitted means inherit defaults. */
    @JsonProperty("tools") List<String> tools,
    /** Preferred model id for this agent. When omitted, inherits the outer agent's model. */
    @JsonProperty("model") String model,
    /** MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema. */
    @JsonProperty("mcpServers") Map<String, Object> mcpServers,
    /** Skill names preloaded into this agent's context. Omitted means none. */
    @JsonProperty("skills") List<String> skills
) {
}
