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
 * A batch of file-picker candidates to check against a prepared policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SandboxFilterPickerCandidatesParams.$NullableDeserializer.class)
record SandboxFilterPickerCandidatesParams(
    /** The candidates to check, in the order the answers are returned. */
    @JsonProperty("candidates") List<SandboxPickerCandidate> candidates,
    /** The session's working directory, matching the one the policy was prepared from. */
    @JsonProperty("cwd") String cwd,
    /** The policy returned by `sandbox.preparePickerPolicy`. Omitted keeps every candidate. */
    @JsonProperty("sandbox") Object sandbox,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxFilterPickerCandidatesParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxFilterPickerCandidatesParams(List<SandboxPickerCandidate> candidates, String cwd, Object sandbox) {
        this(candidates, cwd, sandbox, java.util.Set.of());
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
        private List<SandboxPickerCandidate> candidates;
        private String cwd;
        private Object sandbox;
        private boolean $presence2;

        /**
         * Sets the {@code candidates} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder candidates(List<SandboxPickerCandidate> value) {
            this.candidates = value;
            return this;
        }

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
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxFilterPickerCandidatesParams build() {
            if (candidates == null) throw new IllegalStateException("Missing required property: candidates");
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && sandbox == null) $explicitNulls.add("sandbox");
            return new SandboxFilterPickerCandidatesParams(candidates, cwd, sandbox, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("sandbox") && sandbox == null) values.put("sandbox", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SandboxFilterPickerCandidatesParams> {
        @Override
        public SandboxFilterPickerCandidatesParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SandboxFilterPickerCandidatesParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("sandbox") && tree.get("sandbox").isNull()) $explicitNulls.add("sandbox");
            return new SandboxFilterPickerCandidatesParams(readProperty(context, tree, "candidates", new com.fasterxml.jackson.core.type.TypeReference<List<SandboxPickerCandidate>>() {}), readProperty(context, tree, "cwd", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "sandbox", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
