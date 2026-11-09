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
 * Managed Azure ML registration changes under a connection-owned family lease.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlRegistrationParams.$NullableDeserializer.class)
public record AzureMlRegistrationParams(
    /** Registry associated with the acquired family lease. */
    @JsonProperty("registry") AzureMlRegistry registry,
    /** Exact model family associated with the lease. */
    @JsonProperty("name") String name,
    /** Lease retained through transfer, registration, activation, and compensation. */
    @JsonProperty("leaseId") String leaseId,
    /** Scoped registration change to perform. */
    @JsonProperty("operation") AzureMlRegistrationOperation operation,
    /** Provider and model entries for an upsert. */
    @JsonProperty("entries") ModelProviderSaveConfigurationRequest entries,
    /** Exact prior rows returned by a successful upsert, required for restoration. */
    @JsonProperty("before") Object before,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlRegistrationParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlRegistrationParams(AzureMlRegistry registry, String name, String leaseId, AzureMlRegistrationOperation operation, ModelProviderSaveConfigurationRequest entries, Object before) {
        this(registry, name, leaseId, operation, entries, before, java.util.Set.of());
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
        private String name;
        private String leaseId;
        private AzureMlRegistrationOperation operation;
        private ModelProviderSaveConfigurationRequest entries;
        private Object before;
        private boolean $presence5;

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
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code leaseId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder leaseId(String value) {
            this.leaseId = value;
            return this;
        }

        /**
         * Sets the {@code operation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operation(AzureMlRegistrationOperation value) {
            this.operation = value;
            return this;
        }

        /**
         * Sets the {@code entries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder entries(ModelProviderSaveConfigurationRequest value) {
            this.entries = value;
            return this;
        }

        /**
         * Sets the {@code before} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder before(Object value) {
            this.before = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlRegistrationParams build() {
            if (registry == null) throw new IllegalStateException("Missing required property: registry");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (leaseId == null) throw new IllegalStateException("Missing required property: leaseId");
            if (operation == null) throw new IllegalStateException("Missing required property: operation");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && before == null) $explicitNulls.add("before");
            return new AzureMlRegistrationParams(registry, name, leaseId, operation, entries, before, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("before") && before == null) values.put("before", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlRegistrationParams> {
        @Override
        public AzureMlRegistrationParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlRegistrationParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("before") && tree.get("before").isNull()) $explicitNulls.add("before");
            return new AzureMlRegistrationParams(readProperty(context, tree, "registry", new com.fasterxml.jackson.core.type.TypeReference<AzureMlRegistry>() {}), readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "leaseId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "operation", new com.fasterxml.jackson.core.type.TypeReference<AzureMlRegistrationOperation>() {}), readProperty(context, tree, "entries", new com.fasterxml.jackson.core.type.TypeReference<ModelProviderSaveConfigurationRequest>() {}), readProperty(context, tree, "before", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
