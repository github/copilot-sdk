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
 * The working directory and sandbox settings to build a file-picker policy from.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SandboxPreparePickerPolicyParams.$NullableDeserializer.class)
record SandboxPreparePickerPolicyParams(
    /** The session's working directory. Git discovery and grant canonicalization are resolved against it. */
    @JsonProperty("cwd") String cwd,
    /** The session's resolved `sandbox` slice. Omitted, or disabled, yields no policy and every candidate is kept. */
    @JsonProperty("sandbox") Object sandbox,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxPreparePickerPolicyParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxPreparePickerPolicyParams(String cwd, Object sandbox) {
        this(cwd, sandbox, java.util.Set.of());
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
        private String cwd;
        private Object sandbox;
        private boolean $presence1;

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code sandbox} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandbox(Object value) {
            this.sandbox = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxPreparePickerPolicyParams build() {
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && sandbox == null) $explicitNulls.add("sandbox");
            return new SandboxPreparePickerPolicyParams(cwd, sandbox, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("sandbox") && sandbox == null) values.put("sandbox", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SandboxPreparePickerPolicyParams> {
        @Override
        public SandboxPreparePickerPolicyParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SandboxPreparePickerPolicyParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("sandbox") && tree.get("sandbox").isNull()) $explicitNulls.add("sandbox");
            return new SandboxPreparePickerPolicyParams(readProperty(context, tree, "cwd", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "sandbox", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
