/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A single loaded custom agent in `session.custom_agents_updated`, with identity, source, tools, invocability, and authored model configuration.
 *
 * @since 1.0.0
 */
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
    /** List of tool names available to this agent, or null when all tools are available */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("tools") List<String> tools,
    /** Whether the agent can be selected by the user */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether model-driven invocation is disabled for this agent. */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Model override for this agent, if set */
    @JsonProperty("model") String model,
    /** Authored model ids in priority order, if configured */
    @JsonProperty("models") List<String> models,
    /** Whether authored models are preferences or required constraints */
    @JsonProperty("modelPolicy") AgentModelPolicy modelPolicy
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CustomAgentsUpdatedAgent {}

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
        private String id;
        private String name;
        private String displayName;
        private String description;
        private String source;
        private List<String> tools;
        private boolean $presence5;
        private Boolean userInvocable;
        private Boolean disableModelInvocation;
        private String model;
        private List<String> models;
        private AgentModelPolicy modelPolicy;

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
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
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
            this.$presence5 = true;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CustomAgentsUpdatedAgent build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (!$presence5) throw new IllegalStateException("Missing required property: tools");
            if (userInvocable == null) throw new IllegalStateException("Missing required property: userInvocable");
            return new CustomAgentsUpdatedAgent(id, name, displayName, description, source, tools, userInvocable, disableModelInvocation, model, models, modelPolicy);
        }
    }
}
