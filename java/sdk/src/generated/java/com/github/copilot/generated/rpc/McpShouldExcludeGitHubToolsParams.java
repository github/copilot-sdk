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
import javax.annotation.processing.Generated;

/**
 * The session tool filters that decide whether the session still has a shell to run `gh` with.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = McpShouldExcludeGitHubToolsParams.$NullableDeserializer.class)
record McpShouldExcludeGitHubToolsParams(
    /** The session's tool allowlist, when it set one. Omitted means the session constrains nothing this way. */
    @JsonProperty("availableTools") List<String> availableTools,
    /** The session's tool denylist, when it set one. Omitted means the session constrains nothing this way. */
    @JsonProperty("excludedTools") List<String> excludedTools,
    /** How the allowlist and denylist combine when both are set. Omitted means the default every session gets, so a caller that never chose a precedence is answered as its sessions behave. */
    @JsonProperty("toolFilterPrecedence") OptionsUpdateToolFilterPrecedence toolFilterPrecedence,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpShouldExcludeGitHubToolsParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpShouldExcludeGitHubToolsParams(List<String> availableTools, List<String> excludedTools, OptionsUpdateToolFilterPrecedence toolFilterPrecedence) {
        this(availableTools, excludedTools, toolFilterPrecedence, java.util.Set.of());
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
        private List<String> availableTools;
        private List<String> excludedTools;
        private OptionsUpdateToolFilterPrecedence toolFilterPrecedence;
        private boolean $presence2;

        /**
         * Sets the {@code availableTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder availableTools(List<String> value) {
            this.availableTools = value;
            return this;
        }

        /**
         * Sets the {@code excludedTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder excludedTools(List<String> value) {
            this.excludedTools = value;
            return this;
        }

        /**
         * Sets the {@code toolFilterPrecedence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolFilterPrecedence(OptionsUpdateToolFilterPrecedence value) {
            this.toolFilterPrecedence = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpShouldExcludeGitHubToolsParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && toolFilterPrecedence == null) $explicitNulls.add("toolFilterPrecedence");
            return new McpShouldExcludeGitHubToolsParams(availableTools, excludedTools, toolFilterPrecedence, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("toolFilterPrecedence") && toolFilterPrecedence == null) values.put("toolFilterPrecedence", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<McpShouldExcludeGitHubToolsParams> {
        @Override
        public McpShouldExcludeGitHubToolsParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(McpShouldExcludeGitHubToolsParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("toolFilterPrecedence") && tree.get("toolFilterPrecedence").isNull()) $explicitNulls.add("toolFilterPrecedence");
            return new McpShouldExcludeGitHubToolsParams(readProperty(context, tree, "availableTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "excludedTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolFilterPrecedence", new com.fasterxml.jackson.core.type.TypeReference<OptionsUpdateToolFilterPrecedence>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
