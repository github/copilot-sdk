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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Agent metadata, including identifiers, display details, source, tools, model, models, reasoning effort, MCP servers, skills, and file path.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentInfo(
    /** Opaque identity of the plugin owning this agent. Matches plugins.list().plugins[].id on the same runtime host. Compare the complete value without parsing it. Omitted for non-plugin agents. */
    @JsonProperty("pluginId") String pluginId,
    /** Name of the agent. Use `id` as the stable selection identifier. */
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
    /** Whether model-driven invocation is disabled for this agent. */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Allowed tool names for this agent. Empty array means none; omitted means inherit defaults. */
    @JsonProperty("tools") List<String> tools,
    /** Authored preferred model id for this agent. Runtime model selection may choose a different model; omitted means no authored preference. */
    @JsonProperty("model") String model,
    /** Authored preferred model ids for this agent, in priority order. Runtime model selection chooses the first available model; omitted means no authored preference. */
    @JsonProperty("models") List<String> models,
    /** Whether authored models are preferences or required constraints. */
    @JsonProperty("modelPolicy") AgentModelPolicy modelPolicy,
    /** Authored reasoning effort for this agent. Applied on selection to models that support it; omitted means no authored preference. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema. */
    @CopilotExperimental
    @JsonProperty("mcpServers") Map<String, Object> mcpServers,
    /** Skill names preloaded into this agent's context. Omitted means none. */
    @JsonProperty("skills") List<String> skills,
    /** Authored base prompt for the agent. Runtime prompt assembly may add dynamic context at invocation time. Omitted from `session.agent.list` unless `includePrompt` is true. */
    @JsonProperty("prompt") String prompt,
    /** Declarative hook actions keyed by event name, executed only while this agent is active. */
    @JsonProperty("hooks") Map<String, List<Object>> hooks
) {

    /**
     * Creates a record without plugin ownership or content-root metadata.
     *
     * @param name Name of the agent. Use `id` as the stable selection identifier.
     * @param displayName Human-readable display name
     * @param description Description of the agent's purpose
     * @param path Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.
     * @param id Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned.
     * @param source Where the agent definition was loaded from
     * @param userInvocable Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only.
     * @param disableModelInvocation Whether model-driven invocation is disabled for this agent.
     * @param tools Allowed tool names for this agent. Empty array means none; omitted means inherit defaults.
     * @param model Authored preferred model id for this agent. Runtime model selection may choose a different model; omitted means no authored preference.
     * @param models Authored preferred model ids for this agent, in priority order. Runtime model selection chooses the first available model; omitted means no authored preference.
     * @param modelPolicy Whether authored models are preferences or required constraints.
     * @param reasoningEffort Authored reasoning effort for this agent. Applied on selection to models that support it; omitted means no authored preference.
     * @param mcpServers MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema.
     * @param skills Skill names preloaded into this agent's context. Omitted means none.
     * @param prompt Authored base prompt for the agent. Runtime prompt assembly may add dynamic context at invocation time. Omitted from `session.agent.list` unless `includePrompt` is true.
     * @param hooks Declarative hook actions keyed by event name, executed only while this agent is active.
     */
    public AgentInfo(
        String name,
        String displayName,
        String description,
        String path,
        String id,
        AgentInfoSource source,
        Boolean userInvocable,
        Boolean disableModelInvocation,
        List<String> tools,
        String model,
        List<String> models,
        AgentModelPolicy modelPolicy,
        String reasoningEffort,
        Map<String, Object> mcpServers,
        List<String> skills,
        String prompt,
        Map<String, List<Object>> hooks
    ) {
        this(null, name, displayName, description, path, id, source, userInvocable, disableModelInvocation, tools, model, models, modelPolicy, reasoningEffort, mcpServers, skills, prompt, hooks);
    }
}
