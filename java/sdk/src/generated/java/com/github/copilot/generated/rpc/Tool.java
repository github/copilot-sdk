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
 * Built-in tool metadata with identifier, optional namespaced name, description, input-parameter schema, and usage instructions.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = Tool.$NullableDeserializer.class)
public record Tool(
    /** Tool identifier (e.g., "bash", "grep", "str_replace_editor") */
    @JsonProperty("name") String name,
    /** Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools) */
    @JsonProperty("namespacedName") String namespacedName,
    /** Description of what the tool does */
    @JsonProperty("description") String description,
    /** JSON Schema for the tool's input parameters */
    @JsonProperty("parameters") Map<String, Object> parameters,
    /** Optional instructions for how to use this tool effectively */
    @JsonProperty("instructions") String instructions,
    /** Telemetry-safety policy for the tool name and input names, not input values. Treat omitted metadata as unsafe. */
    @CopilotExperimental
    @JsonProperty("safeForTelemetry") Object safeForTelemetry,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public Tool { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public Tool(String name, String namespacedName, String description, Map<String, Object> parameters, String instructions, Object safeForTelemetry) {
        this(name, namespacedName, description, parameters, instructions, safeForTelemetry, java.util.Set.of());
    }

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
        private String name;
        private String namespacedName;
        private String description;
        private Map<String, Object> parameters;
        private String instructions;
        private Object safeForTelemetry;
        private boolean $presence5;

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
         * Sets the {@code namespacedName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder namespacedName(String value) {
            this.namespacedName = value;
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
         * Sets the {@code parameters} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder parameters(Map<String, Object> value) {
            this.parameters = value;
            return this;
        }

        /**
         * Sets the {@code instructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder instructions(String value) {
            this.instructions = value;
            return this;
        }

        /**
         * Sets the {@code safeForTelemetry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder safeForTelemetry(Object value) {
            this.safeForTelemetry = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public Tool build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && safeForTelemetry == null) $explicitNulls.add("safeForTelemetry");
            return new Tool(name, namespacedName, description, parameters, instructions, safeForTelemetry, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("safeForTelemetry") && safeForTelemetry == null) values.put("safeForTelemetry", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Tool> {
        @Override
        public Tool deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(Tool.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("safeForTelemetry") && tree.get("safeForTelemetry").isNull()) $explicitNulls.add("safeForTelemetry");
            return new Tool(readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "namespacedName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "description", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "parameters", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "instructions", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "safeForTelemetry", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
