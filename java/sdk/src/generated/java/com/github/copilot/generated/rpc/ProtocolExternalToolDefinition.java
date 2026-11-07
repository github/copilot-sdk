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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Serializable definition of a caller-implemented tool whose execution is handled over the SDK connection.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProtocolExternalToolDefinition(
    /** Unique model-visible tool name. */
    @JsonProperty("name") String name,
    /** Model-visible explanation of what the tool does. */
    @JsonProperty("description") String description,
    /** Optional human-readable display title. */
    @JsonProperty("title") String title,
    /** JSON Schema describing the tool's input arguments. */
    @JsonProperty("parameters") Map<String, Object> parameters,
    /** Whether this definition replaces a built-in tool with the same name. */
    @JsonProperty("overridesBuiltInTool") Boolean overridesBuiltInTool,
    /** Whether execution bypasses the normal tool permission prompt. */
    @JsonProperty("skipPermission") Boolean skipPermission,
    /** Tool-loading deferral policy. */
    @JsonProperty("defer") ProtocolExternalToolDefer defer,
    /** Whether the tool executes commands in a terminal. */
    @JsonProperty("isTerminal") Boolean isTerminal,
    /** Optional caller-defined metadata associated with the tool. */
    @JsonProperty("metadata") Map<String, Object> metadata,
    /** Optional namespace grouping this tool with related tools. Where the model
provider supports native tool namespaces, deferred tools sharing a namespace
are presented to the model as one group; elsewhere the tool stays flat. The
tool's `name` remains its unique invocation name either way. */
    @JsonProperty("namespace") ProtocolExternalToolNamespace namespace
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProtocolExternalToolDefinition {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String name;
        private String description;
        private String title;
        private Map<String, Object> parameters;
        private Boolean overridesBuiltInTool;
        private Boolean skipPermission;
        private ProtocolExternalToolDefer defer;
        private Boolean isTerminal;
        private Map<String, Object> metadata;
        private ProtocolExternalToolNamespace namespace;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code parameters} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder parameters(Map<String, Object> value) {
            this.parameters = value;
            return this;
        }

        /**
         * Sets the {@code overridesBuiltInTool} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder overridesBuiltInTool(Boolean value) {
            this.overridesBuiltInTool = value;
            return this;
        }

        /**
         * Sets the {@code skipPermission} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipPermission(Boolean value) {
            this.skipPermission = value;
            return this;
        }

        /**
         * Sets the {@code defer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder defer(ProtocolExternalToolDefer value) {
            this.defer = value;
            return this;
        }

        /**
         * Sets the {@code isTerminal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder isTerminal(Boolean value) {
            this.isTerminal = value;
            return this;
        }

        /**
         * Sets the {@code metadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder metadata(Map<String, Object> value) {
            this.metadata = value;
            return this;
        }

        /**
         * Sets the {@code namespace} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder namespace(ProtocolExternalToolNamespace value) {
            this.namespace = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProtocolExternalToolDefinition build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            return new ProtocolExternalToolDefinition(name, description, title, parameters, overridesBuiltInTool, skipPermission, defer, isTerminal, metadata, namespace);
        }
    }
}
