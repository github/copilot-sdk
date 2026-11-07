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
 * The feature flags to evaluate shipped agents against.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AgentsGetAvailableBuiltinsParams.$NullableDeserializer.class)
record AgentsGetAvailableBuiltinsParams(
    /** Feature flag values keyed by name, evaluated with the runtime's truthiness rules. Omit or pass null for no flags. */
    @JsonProperty("featureFlags") Map<String, Object> featureFlags,
    /** Flag overrides keyed by name. A null entry uses the corresponding base flag; false explicitly disables it. Omit or pass null for no overrides. */
    @JsonProperty("overrides") Map<String, Object> overrides,
    /** The surface asking, which gates agents that only apply to one client. Omit or pass null to apply no client filter. */
    @JsonProperty("context") String context,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsGetAvailableBuiltinsParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsGetAvailableBuiltinsParams(Map<String, Object> featureFlags, Map<String, Object> overrides, String context) {
        this(featureFlags, overrides, context, java.util.Set.of());
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
        private Map<String, Object> featureFlags;
        private boolean $presence0;
        private Map<String, Object> overrides;
        private boolean $presence1;
        private String context;
        private boolean $presence2;

        /**
         * Sets the {@code featureFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder featureFlags(Map<String, Object> value) {
            this.featureFlags = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code overrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overrides(Map<String, Object> value) {
            this.overrides = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder context(String value) {
            this.context = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsGetAvailableBuiltinsParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && featureFlags == null) $explicitNulls.add("featureFlags");
            if ($presence1 && overrides == null) $explicitNulls.add("overrides");
            if ($presence2 && context == null) $explicitNulls.add("context");
            return new AgentsGetAvailableBuiltinsParams(featureFlags, overrides, context, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("featureFlags") && featureFlags == null) values.put("featureFlags", null);
        if ($explicitNulls.contains("overrides") && overrides == null) values.put("overrides", null);
        if ($explicitNulls.contains("context") && context == null) values.put("context", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AgentsGetAvailableBuiltinsParams> {
        @Override
        public AgentsGetAvailableBuiltinsParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AgentsGetAvailableBuiltinsParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("featureFlags") && tree.get("featureFlags").isNull()) $explicitNulls.add("featureFlags");
            if (tree.has("overrides") && tree.get("overrides").isNull()) $explicitNulls.add("overrides");
            if (tree.has("context") && tree.get("context").isNull()) $explicitNulls.add("context");
            return new AgentsGetAvailableBuiltinsParams(readProperty(context, tree, "featureFlags", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "overrides", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "context", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
