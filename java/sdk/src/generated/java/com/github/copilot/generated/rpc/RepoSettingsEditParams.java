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
 * One atomic edit of an allowed top-level repository setting.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = RepoSettingsEditParams.$NullableDeserializer.class)
record RepoSettingsEditParams(
    /** Absolute path of the repository root. */
    @JsonProperty("repoRoot") String repoRoot,
    /** Shared or local repository settings file. */
    @JsonProperty("scope") RepoSettingsEditScope scope,
    /** One canonical top-level repository setting that the allowlist permits. */
    @JsonProperty("key") String key,
    /** Set a scalar, add or remove a string list item, or remove the setting. The handler rejects a missing or incompatible value. */
    @JsonProperty("operation") SettingsEditOperation operation,
    /** Required boolean, string or number for set, required string for add-item and remove-item, and forbidden for remove. The runtime rejects all other JSON values. */
    @JsonProperty("value") Object value,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RepoSettingsEditParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RepoSettingsEditParams(String repoRoot, RepoSettingsEditScope scope, String key, SettingsEditOperation operation, Object value) {
        this(repoRoot, scope, key, operation, value, java.util.Set.of());
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
        private String repoRoot;
        private RepoSettingsEditScope scope;
        private String key;
        private SettingsEditOperation operation;
        private Object value;
        private boolean $presence4;

        /**
         * Sets the {@code repoRoot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoRoot(String value) {
            this.repoRoot = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scope(RepoSettingsEditScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code operation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operation(SettingsEditOperation value) {
            this.operation = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder value(Object value) {
            this.value = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RepoSettingsEditParams build() {
            if (repoRoot == null) throw new IllegalStateException("Missing required property: repoRoot");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (key == null) throw new IllegalStateException("Missing required property: key");
            if (operation == null) throw new IllegalStateException("Missing required property: operation");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence4 && value == null) $explicitNulls.add("value");
            return new RepoSettingsEditParams(repoRoot, scope, key, operation, value, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("value") && value == null) values.put("value", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<RepoSettingsEditParams> {
        @Override
        public RepoSettingsEditParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(RepoSettingsEditParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("value") && tree.get("value").isNull()) $explicitNulls.add("value");
            return new RepoSettingsEditParams(readProperty(context, tree, "repoRoot", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "scope", new com.fasterxml.jackson.core.type.TypeReference<RepoSettingsEditScope>() {}), readProperty(context, tree, "key", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "operation", new com.fasterxml.jackson.core.type.TypeReference<SettingsEditOperation>() {}), readProperty(context, tree, "value", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
