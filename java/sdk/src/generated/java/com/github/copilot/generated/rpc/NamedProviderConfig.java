/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * External SDK input for a named custom model provider. Ingested by the native protocol boundary before host dispatch.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = NamedProviderConfig.$NullableDeserializer.class)
public record NamedProviderConfig(
    /** Unique provider name used to qualify model selection IDs. */
    @JsonProperty("name") String name,
    /** Provider protocol family. */
    @JsonProperty("type") ProviderConfigType type,
    /** Wire API used to communicate with the provider. */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** Transport used to communicate with the provider. */
    @JsonProperty("transport") ProviderConfigTransport transport,
    /** Base URL for provider API requests. */
    @JsonProperty("baseUrl") String baseUrl,
    /** Local model runtime options for a WinML provider whose base URL is its GGUF path. */
    @JsonProperty("localModel") Object localModel,
    /** Static API key used to authenticate provider requests. */
    @JsonProperty("apiKey") String apiKey,
    /** Static bearer token used to authenticate provider requests. */
    @JsonProperty("bearerToken") String bearerToken,
    /** Azure authentication configuration for the provider. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Additional HTTP headers included with provider requests. */
    @JsonProperty("headers") Map<String, String> headers,
    /** The product serving the provider's models, reported in telemetry as `model_provider`. Only affects telemetry. */
    @JsonProperty("modelProvider") ProviderConfigModelProvider modelProvider,
    /** Whether the host supplies bearer tokens dynamically. */
    @JsonProperty("hasBearerTokenProvider") Boolean hasBearerTokenProvider,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NamedProviderConfig { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NamedProviderConfig(String name, ProviderConfigType type, ProviderConfigWireApi wireApi, ProviderConfigTransport transport, String baseUrl, Object localModel, String apiKey, String bearerToken, ProviderConfigAzure azure, Map<String, String> headers, ProviderConfigModelProvider modelProvider, Boolean hasBearerTokenProvider) {
        this(name, type, wireApi, transport, baseUrl, localModel, apiKey, bearerToken, azure, headers, modelProvider, hasBearerTokenProvider, java.util.Set.of());
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
        private ProviderConfigType type;
        private ProviderConfigWireApi wireApi;
        private ProviderConfigTransport transport;
        private String baseUrl;
        private Object localModel;
        private boolean $presence5;
        private String apiKey;
        private String bearerToken;
        private ProviderConfigAzure azure;
        private Map<String, String> headers;
        private ProviderConfigModelProvider modelProvider;
        private Boolean hasBearerTokenProvider;

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
        public Builder type(ProviderConfigType value) {
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
         * Sets the {@code apiKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder apiKey(String value) {
            this.apiKey = value;
            return this;
        }

        /**
         * Sets the {@code bearerToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder bearerToken(String value) {
            this.bearerToken = value;
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
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(Map<String, String> value) {
            this.headers = value;
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
         * Sets the {@code hasBearerTokenProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasBearerTokenProvider(Boolean value) {
            this.hasBearerTokenProvider = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public NamedProviderConfig build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (baseUrl == null) throw new IllegalStateException("Missing required property: baseUrl");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && localModel == null) $explicitNulls.add("localModel");
            return new NamedProviderConfig(name, type, wireApi, transport, baseUrl, localModel, apiKey, bearerToken, azure, headers, modelProvider, hasBearerTokenProvider, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("localModel") && localModel == null) values.put("localModel", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<NamedProviderConfig> {
        @Override
        public NamedProviderConfig deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(NamedProviderConfig.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("localModel") && tree.get("localModel").isNull()) $explicitNulls.add("localModel");
            return new NamedProviderConfig(readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "type", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigType>() {}), readProperty(context, tree, "wireApi", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigWireApi>() {}), readProperty(context, tree, "transport", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigTransport>() {}), readProperty(context, tree, "baseUrl", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "localModel", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "apiKey", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "bearerToken", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "azure", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigAzure>() {}), readProperty(context, tree, "headers", new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}), readProperty(context, tree, "modelProvider", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfigModelProvider>() {}), readProperty(context, tree, "hasBearerTokenProvider", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Unique provider name used to qualify model selection IDs.
     * @param type Provider protocol family.
     * @param wireApi Wire API used to communicate with the provider.
     * @param transport Transport used to communicate with the provider.
     * @param baseUrl Base URL for provider API requests.
     * @param apiKey Static API key used to authenticate provider requests.
     * @param bearerToken Static bearer token used to authenticate provider requests.
     * @param azure Azure authentication configuration for the provider.
     * @param headers Additional HTTP headers included with provider requests.
     * @param modelProvider The product serving the provider's models, reported in telemetry as `model_provider`. Only affects telemetry.
     * @param hasBearerTokenProvider Whether the host supplies bearer tokens dynamically.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NamedProviderConfig(
        String name,
        ProviderConfigType type,
        ProviderConfigWireApi wireApi,
        ProviderConfigTransport transport,
        String baseUrl,
        String apiKey,
        String bearerToken,
        ProviderConfigAzure azure,
        Map<String, String> headers,
        ProviderConfigModelProvider modelProvider,
        Boolean hasBearerTokenProvider
    ) {
        this(name, type, wireApi, transport, baseUrl, null, apiKey, bearerToken, azure, headers, modelProvider, hasBearerTokenProvider);
    }
}
