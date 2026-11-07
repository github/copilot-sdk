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
 * Redacted warning for a skipped model or registry.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlCatalogWarning.$NullableDeserializer.class)
public record AzureMlCatalogWarning(
    /** Registry associated with the warning. */
    @JsonProperty("registry") AzureMlRegistry registry,
    /** Model family, or `catalog` for registry-wide errors. */
    @JsonProperty("modelName") String modelName,
    /** Version, if one was available. */
    @JsonProperty("version") String version,
    /** Safe reason without signed access details. */
    @JsonProperty("kind") String kind,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlCatalogWarning { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlCatalogWarning(AzureMlRegistry registry, String modelName, String version, String kind) {
        this(registry, modelName, version, kind, java.util.Set.of());
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
        private AzureMlRegistry registry;
        private String modelName;
        private String version;
        private boolean $presence2;
        private String kind;

        /**
         * Sets the {@code registry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder registry(AzureMlRegistry value) {
            this.registry = value;
            return this;
        }

        /**
         * Sets the {@code modelName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelName(String value) {
            this.modelName = value;
            return this;
        }

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder version(String value) {
            this.version = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlCatalogWarning build() {
            if (registry == null) throw new IllegalStateException("Missing required property: registry");
            if (modelName == null) throw new IllegalStateException("Missing required property: modelName");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && version == null) $explicitNulls.add("version");
            return new AzureMlCatalogWarning(registry, modelName, version, kind, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("version") && version == null) values.put("version", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlCatalogWarning> {
        @Override
        public AzureMlCatalogWarning deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlCatalogWarning.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("version") && tree.get("version").isNull()) $explicitNulls.add("version");
            return new AzureMlCatalogWarning(readProperty(context, tree, "registry", new com.fasterxml.jackson.core.type.TypeReference<AzureMlRegistry>() {}), readProperty(context, tree, "modelName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "version", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "kind", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
