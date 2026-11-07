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
 * The user's sandbox settings and the managed sandbox floor to raise them to.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SandboxResolveFlooredConfigParams.$NullableDeserializer.class)
record SandboxResolveFlooredConfigParams(
    /** The user's own `sandbox` settings slice. Omitted when the user has configured none, which still yields the managed floor rather than a disabled sandbox. */
    @JsonProperty("userSandbox") Object userSandbox,
    /** The effective managed `sandbox` slice acting as a floor. Omitted when no administrator policy applies, in which case the user's own settings stand unchanged. */
    @JsonProperty("managedSandbox") Object managedSandbox,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxResolveFlooredConfigParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxResolveFlooredConfigParams(Object userSandbox, Object managedSandbox) {
        this(userSandbox, managedSandbox, java.util.Set.of());
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
        private Object userSandbox;
        private boolean $presence0;
        private Object managedSandbox;
        private boolean $presence1;

        /**
         * Sets the {@code userSandbox} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userSandbox(Object value) {
            this.userSandbox = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code managedSandbox} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managedSandbox(Object value) {
            this.managedSandbox = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxResolveFlooredConfigParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && userSandbox == null) $explicitNulls.add("userSandbox");
            if ($presence1 && managedSandbox == null) $explicitNulls.add("managedSandbox");
            return new SandboxResolveFlooredConfigParams(userSandbox, managedSandbox, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("userSandbox") && userSandbox == null) values.put("userSandbox", null);
        if ($explicitNulls.contains("managedSandbox") && managedSandbox == null) values.put("managedSandbox", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SandboxResolveFlooredConfigParams> {
        @Override
        public SandboxResolveFlooredConfigParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SandboxResolveFlooredConfigParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("userSandbox") && tree.get("userSandbox").isNull()) $explicitNulls.add("userSandbox");
            if (tree.has("managedSandbox") && tree.get("managedSandbox").isNull()) $explicitNulls.add("managedSandbox");
            return new SandboxResolveFlooredConfigParams(readProperty(context, tree, "userSandbox", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "managedSandbox", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
