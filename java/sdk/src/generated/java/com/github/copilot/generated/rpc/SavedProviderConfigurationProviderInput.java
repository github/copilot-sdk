/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Editable provider settings, without credential values or custom headers.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SavedProviderConfigurationProviderInput.$NullableDeserializer.class)
public record SavedProviderConfigurationProviderInput(
    /** Provider name used to qualify model selection ids. */
    @JsonProperty("name") String name,
    /** Provider protocol family. */
    @JsonProperty("type") SavedProviderConfigurationType type,
    /** OpenAI-compatible wire API. */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** HTTP or WebSocket transport. */
    @JsonProperty("transport") ProviderConfigTransport transport,
    /** Provider endpoint. */
    @JsonProperty("baseUrl") String baseUrl,
    /** Non-secret local model runtime options, such as the WinML device. */
    @JsonProperty("localModel") Object localModel,
    /** Safe Azure connection settings, including API version. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Serving product used for attribution. */
    @JsonProperty("modelProvider") ProviderConfigModelProvider modelProvider,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SavedProviderConfigurationProviderInput { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SavedProviderConfigurationProviderInput(String name, SavedProviderConfigurationType type, ProviderConfigWireApi wireApi, ProviderConfigTransport transport, String baseUrl, Object localModel, ProviderConfigAzure azure, ProviderConfigModelProvider modelProvider) {
        this(name, type, wireApi, transport, baseUrl, localModel, azure, modelProvider, java.util.Set.of());
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
        private String name;
        private SavedProviderConfigurationType type;
        private ProviderConfigWireApi wireApi;
        private ProviderConfigTransport transport;
        private String baseUrl;
        private Object localModel;
        private boolean $presence5;
        private ProviderConfigAzure azure;
        private ProviderConfigModelProvider modelProvider;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(SavedProviderConfigurationType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code wireApi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wireApi(ProviderConfigWireApi value) {
            this.wireApi = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(ProviderConfigTransport value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code baseUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder baseUrl(String value) {
            this.baseUrl = value;
            return this;
        }

        /**
         * Sets the {@code localModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder localModel(Object value) {
            this.localModel = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code azure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder azure(ProviderConfigAzure value) {
            this.azure = value;
            return this;
        }

        /**
         * Sets the {@code modelProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelProvider(ProviderConfigModelProvider value) {
            this.modelProvider = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SavedProviderConfigurationProviderInput build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (baseUrl == null) throw new IllegalStateException("Missing required property: baseUrl");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && localModel == null) $explicitNulls.add("localModel");
            return new SavedProviderConfigurationProviderInput(name, type, wireApi, transport, baseUrl, localModel, azure, modelProvider, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("localModel") && localModel == null) values.put("localModel", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SavedProviderConfigurationProviderInput> {
        @Override
        public SavedProviderConfigurationProviderInput deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SavedProviderConfigurationProviderInput.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("localModel") && tree.get("localModel").isNull()) $explicitNulls.add("localModel");
            return new SavedProviderConfigurationProviderInput(readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "type", new com.fasterxml.jackson.core.type.TypeReference<SavedProviderConfigurationType>() {}), readProperty(context, tree, "wireApi", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigWireApi>() {}), readProperty(context, tree, "transport", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigTransport>() {}), readProperty(context, tree, "baseUrl", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "localModel", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "azure", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigAzure>() {}), readProperty(context, tree, "modelProvider", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigModelProvider>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
