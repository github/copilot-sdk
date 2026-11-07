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
 * Options for one workflow-scoped subagent call.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = WorkflowAgentOptions.$NullableDeserializer.class)
public record WorkflowAgentOptions(
    /** Optional label distinguishing otherwise identical memoized agent calls. */
    @JsonProperty("label") String label,
    /** Optional JSON Schema for structured agent output. */
    @JsonProperty("schema") Object schema,
    /** Optional model identifier for the subagent. */
    @JsonProperty("model") String model,
    /** Optional reasoning effort override for the subagent. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Optional context tier override for the subagent. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** Optional built-in or custom agent name whose definition configures the subagent. */
    @JsonProperty("agent") String agent,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowAgentOptions { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowAgentOptions(String label, Object schema, String model, String reasoningEffort, ContextTier contextTier, String agent) {
        this(label, schema, model, reasoningEffort, contextTier, agent, java.util.Set.of());
    }

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
        private String label;
        private Object schema;
        private boolean $presence1;
        private String model;
        private String reasoningEffort;
        private ContextTier contextTier;
        private String agent;

        /**
         * Sets the {@code label} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder label(String value) {
            this.label = value;
            return this;
        }

        /**
         * Sets the {@code schema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder schema(Object value) {
            this.schema = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code contextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contextTier(ContextTier value) {
            this.contextTier = value;
            return this;
        }

        /**
         * Sets the {@code agent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder agent(String value) {
            this.agent = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowAgentOptions build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && schema == null) $explicitNulls.add("schema");
            return new WorkflowAgentOptions(label, schema, model, reasoningEffort, contextTier, agent, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("schema") && schema == null) values.put("schema", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<WorkflowAgentOptions> {
        @Override
        public WorkflowAgentOptions deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(WorkflowAgentOptions.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("schema") && tree.get("schema").isNull()) $explicitNulls.add("schema");
            return new WorkflowAgentOptions(readProperty(context, tree, "label", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "schema", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<ContextTier>() {}), readProperty(context, tree, "agent", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
