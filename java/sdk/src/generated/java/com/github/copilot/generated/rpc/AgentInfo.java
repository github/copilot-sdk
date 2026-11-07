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
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentInfo {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String pluginId;
        private String name;
        private String displayName;
        private String description;
        private String path;
        private String id;
        private AgentInfoSource source;
        private Boolean userInvocable;
        private Boolean disableModelInvocation;
        private List<String> tools;
        private String model;
        private List<String> models;
        private AgentModelPolicy modelPolicy;
        private String reasoningEffort;
        private Map<String, Object> mcpServers;
        private List<String> skills;
        private String prompt;
        private Map<String, List<Object>> hooks;

        /**
         * Sets the {@code pluginId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginId(String value) {
            this.pluginId = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(AgentInfoSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code userInvocable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userInvocable(Boolean value) {
            this.userInvocable = value;
            return this;
        }

        /**
         * Sets the {@code disableModelInvocation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableModelInvocation(Boolean value) {
            this.disableModelInvocation = value;
            return this;
        }

        /**
         * Sets the {@code tools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tools(List<String> value) {
            this.tools = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code models} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder models(List<String> value) {
            this.models = value;
            return this;
        }

        /**
         * Sets the {@code modelPolicy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelPolicy(AgentModelPolicy value) {
            this.modelPolicy = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code mcpServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mcpServers(Map<String, Object> value) {
            this.mcpServers = value;
            return this;
        }

        /**
         * Sets the {@code skills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skills(List<String> value) {
            this.skills = value;
            return this;
        }

        /**
         * Sets the {@code prompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prompt(String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Sets the {@code hooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hooks(Map<String, List<Object>> value) {
            this.hooks = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentInfo build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (id == null) throw new IllegalStateException("Missing required property: id");
            return new AgentInfo(pluginId, name, displayName, description, path, id, source, userInvocable, disableModelInvocation, tools, model, models, modelPolicy, reasoningEffort, mcpServers, skills, prompt, hooks);
        }
    }

    /**
     * Creates a record without plugin ownership or content-root metadata.
     * @deprecated Use named construction via {@link #builder()}.
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
    @Deprecated(since = "2.0.0", forRemoval = false)
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
