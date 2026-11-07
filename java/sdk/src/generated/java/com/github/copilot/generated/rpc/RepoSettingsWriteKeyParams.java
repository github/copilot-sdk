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
 * One key to write or remove in a repository settings file.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = RepoSettingsWriteKeyParams.$NullableDeserializer.class)
record RepoSettingsWriteKeyParams(
    /** Absolute path of the repository root. */
    @JsonProperty("repoRoot") String repoRoot,
    /** Use repo for shared settings or local for settings.local.json. Other scopes are rejected. */
    @JsonProperty("scope") String scope,
    /** Canonical top-level key permitted by the repository settings allowlist. */
    @JsonProperty("key") String key,
    /** Value to validate and write. Required unless delete is true; a null value is validated, not treated as deletion. */
    @JsonProperty("value") Object value,
    /** True to remove the key instead of writing it. */
    @JsonProperty("delete") Boolean delete,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RepoSettingsWriteKeyParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RepoSettingsWriteKeyParams(String repoRoot, String scope, String key, Object value, Boolean delete) {
        this(repoRoot, scope, key, value, delete, java.util.Set.of());
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
        private String repoRoot;
        private String scope;
        private String key;
        private Object value;
        private boolean $presence3;
        private Boolean delete;

        /**
         * Sets the {@code repoRoot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public Builder scope(String value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(Object value) {
            this.value = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code delete} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder delete(Boolean value) {
            this.delete = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RepoSettingsWriteKeyParams build() {
            if (repoRoot == null) throw new IllegalStateException("Missing required property: repoRoot");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (key == null) throw new IllegalStateException("Missing required property: key");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && value == null) $explicitNulls.add("value");
            return new RepoSettingsWriteKeyParams(repoRoot, scope, key, value, delete, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("value") && value == null) values.put("value", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<RepoSettingsWriteKeyParams> {
        @Override
        public RepoSettingsWriteKeyParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(RepoSettingsWriteKeyParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("value") && tree.get("value").isNull()) $explicitNulls.add("value");
            return new RepoSettingsWriteKeyParams(readProperty(context, tree, "repoRoot", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "scope", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "key", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "value", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "delete", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
