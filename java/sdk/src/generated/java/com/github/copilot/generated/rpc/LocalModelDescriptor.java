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
 * Runtime catalog entry, including installation and inference metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelDescriptor.$NullableDeserializer.class)
public record LocalModelDescriptor(
    /** Catalog identifier, distinct from the inference selection identifier. */
    @JsonProperty("id") String id,
    /** Model display name. */
    @JsonProperty("displayName") String displayName,
    /** Exact registry asset identity. */
    @JsonProperty("azureMl") LocalModelAzureMlIdentity azureMl,
    /** Source and display order. */
    @JsonProperty("source") LocalModelSource source,
    /** Default primary filename. */
    @JsonProperty("fileName") String fileName,
    /** Model publisher display name. */
    @JsonProperty("provider") String provider,
    /** Published license name. */
    @JsonProperty("license") String license,
    /** Registry display label. */
    @JsonProperty("sourceLabel") String sourceLabel,
    /** Published download size, when known. */
    @JsonProperty("approximateBytes") Double approximateBytes,
    /** Published peak installation size, when known. */
    @JsonProperty("peakDiskBytes") Double peakDiskBytes,
    /** Recommended graphics memory, when published. */
    @JsonProperty("approximateVramBytes") Double approximateVramBytes,
    /** Inference runtime name. */
    @JsonProperty("runtime") String runtime,
    /** Whether acquisition installs the inference runtime. */
    @JsonProperty("runtimeProvisioned") Boolean runtimeProvisioned,
    /** Stable BYOK provider name. */
    @JsonProperty("providerName") String providerName,
    /** Provider-qualified selection identifier. */
    @JsonProperty("selectionId") String selectionId,
    /** Catalog lifecycle category. */
    @JsonProperty("lifecycle") String lifecycle,
    /** Published inference options, preserved without mutation. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("serveOptions") Object serveOptions,
    /** Published token limits. */
    @JsonProperty("limits") LocalModelLimits limits,
    /** Whether the model supports tool calls. */
    @JsonProperty("supportsToolCalls") Boolean supportsToolCalls,
    /** Automatic registration metadata, when applicable. */
    @JsonProperty("autoLocal") LocalModelAutoRegistration autoLocal,
    /** Registry metadata, preserved without mutation. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("metadata") Object metadata,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelDescriptor { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelDescriptor(String id, String displayName, LocalModelAzureMlIdentity azureMl, LocalModelSource source, String fileName, String provider, String license, String sourceLabel, Double approximateBytes, Double peakDiskBytes, Double approximateVramBytes, String runtime, Boolean runtimeProvisioned, String providerName, String selectionId, String lifecycle, Object serveOptions, LocalModelLimits limits, Boolean supportsToolCalls, LocalModelAutoRegistration autoLocal, Object metadata) {
        this(id, displayName, azureMl, source, fileName, provider, license, sourceLabel, approximateBytes, peakDiskBytes, approximateVramBytes, runtime, runtimeProvisioned, providerName, selectionId, lifecycle, serveOptions, limits, supportsToolCalls, autoLocal, metadata, java.util.Set.of());
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
        private String id;
        private String displayName;
        private LocalModelAzureMlIdentity azureMl;
        private LocalModelSource source;
        private String fileName;
        private String provider;
        private String license;
        private String sourceLabel;
        private Double approximateBytes;
        private boolean $presence8;
        private Double peakDiskBytes;
        private boolean $presence9;
        private Double approximateVramBytes;
        private boolean $presence10;
        private String runtime;
        private Boolean runtimeProvisioned;
        private boolean $presence12;
        private String providerName;
        private String selectionId;
        private boolean $presence14;
        private String lifecycle;
        private boolean $presence15;
        private Object serveOptions;
        private boolean $presence16;
        private LocalModelLimits limits;
        private Boolean supportsToolCalls;
        private LocalModelAutoRegistration autoLocal;
        private boolean $presence19;
        private Object metadata;
        private boolean $presence20;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code azureMl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder azureMl(LocalModelAzureMlIdentity value) {
            this.azureMl = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(LocalModelSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code fileName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fileName(String value) {
            this.fileName = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provider(String value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code license} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder license(String value) {
            this.license = value;
            return this;
        }

        /**
         * Sets the {@code sourceLabel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourceLabel(String value) {
            this.sourceLabel = value;
            return this;
        }

        /**
         * Sets the {@code approximateBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder approximateBytes(Double value) {
            this.approximateBytes = value;
            this.$presence8 = true;
            return this;
        }

        /**
         * Sets the {@code peakDiskBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder peakDiskBytes(Double value) {
            this.peakDiskBytes = value;
            this.$presence9 = true;
            return this;
        }

        /**
         * Sets the {@code approximateVramBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder approximateVramBytes(Double value) {
            this.approximateVramBytes = value;
            this.$presence10 = true;
            return this;
        }

        /**
         * Sets the {@code runtime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtime(String value) {
            this.runtime = value;
            return this;
        }

        /**
         * Sets the {@code runtimeProvisioned} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtimeProvisioned(Boolean value) {
            this.runtimeProvisioned = value;
            this.$presence12 = true;
            return this;
        }

        /**
         * Sets the {@code providerName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providerName(String value) {
            this.providerName = value;
            return this;
        }

        /**
         * Sets the {@code selectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectionId(String value) {
            this.selectionId = value;
            this.$presence14 = true;
            return this;
        }

        /**
         * Sets the {@code lifecycle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lifecycle(String value) {
            this.lifecycle = value;
            this.$presence15 = true;
            return this;
        }

        /**
         * Sets the {@code serveOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serveOptions(Object value) {
            this.serveOptions = value;
            this.$presence16 = true;
            return this;
        }

        /**
         * Sets the {@code limits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder limits(LocalModelLimits value) {
            this.limits = value;
            return this;
        }

        /**
         * Sets the {@code supportsToolCalls} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportsToolCalls(Boolean value) {
            this.supportsToolCalls = value;
            return this;
        }

        /**
         * Sets the {@code autoLocal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoLocal(LocalModelAutoRegistration value) {
            this.autoLocal = value;
            this.$presence19 = true;
            return this;
        }

        /**
         * Sets the {@code metadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder metadata(Object value) {
            this.metadata = value;
            this.$presence20 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelDescriptor build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (azureMl == null) throw new IllegalStateException("Missing required property: azureMl");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (fileName == null) throw new IllegalStateException("Missing required property: fileName");
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            if (license == null) throw new IllegalStateException("Missing required property: license");
            if (sourceLabel == null) throw new IllegalStateException("Missing required property: sourceLabel");
            if (runtime == null) throw new IllegalStateException("Missing required property: runtime");
            if (providerName == null) throw new IllegalStateException("Missing required property: providerName");
            if (!$presence16) throw new IllegalStateException("Missing required property: serveOptions");
            if (limits == null) throw new IllegalStateException("Missing required property: limits");
            if (supportsToolCalls == null) throw new IllegalStateException("Missing required property: supportsToolCalls");
            if (!$presence20) throw new IllegalStateException("Missing required property: metadata");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence8 && approximateBytes == null) $explicitNulls.add("approximateBytes");
            if ($presence9 && peakDiskBytes == null) $explicitNulls.add("peakDiskBytes");
            if ($presence10 && approximateVramBytes == null) $explicitNulls.add("approximateVramBytes");
            if ($presence12 && runtimeProvisioned == null) $explicitNulls.add("runtimeProvisioned");
            if ($presence14 && selectionId == null) $explicitNulls.add("selectionId");
            if ($presence15 && lifecycle == null) $explicitNulls.add("lifecycle");
            if ($presence19 && autoLocal == null) $explicitNulls.add("autoLocal");
            return new LocalModelDescriptor(id, displayName, azureMl, source, fileName, provider, license, sourceLabel, approximateBytes, peakDiskBytes, approximateVramBytes, runtime, runtimeProvisioned, providerName, selectionId, lifecycle, serveOptions, limits, supportsToolCalls, autoLocal, metadata, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("approximateBytes") && approximateBytes == null) values.put("approximateBytes", null);
        if ($explicitNulls.contains("peakDiskBytes") && peakDiskBytes == null) values.put("peakDiskBytes", null);
        if ($explicitNulls.contains("approximateVramBytes") && approximateVramBytes == null) values.put("approximateVramBytes", null);
        if ($explicitNulls.contains("runtimeProvisioned") && runtimeProvisioned == null) values.put("runtimeProvisioned", null);
        if ($explicitNulls.contains("selectionId") && selectionId == null) values.put("selectionId", null);
        if ($explicitNulls.contains("lifecycle") && lifecycle == null) values.put("lifecycle", null);
        if ($explicitNulls.contains("autoLocal") && autoLocal == null) values.put("autoLocal", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelDescriptor> {
        @Override
        public LocalModelDescriptor deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelDescriptor.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("approximateBytes") && tree.get("approximateBytes").isNull()) $explicitNulls.add("approximateBytes");
            if (tree.has("peakDiskBytes") && tree.get("peakDiskBytes").isNull()) $explicitNulls.add("peakDiskBytes");
            if (tree.has("approximateVramBytes") && tree.get("approximateVramBytes").isNull()) $explicitNulls.add("approximateVramBytes");
            if (tree.has("runtimeProvisioned") && tree.get("runtimeProvisioned").isNull()) $explicitNulls.add("runtimeProvisioned");
            if (tree.has("selectionId") && tree.get("selectionId").isNull()) $explicitNulls.add("selectionId");
            if (tree.has("lifecycle") && tree.get("lifecycle").isNull()) $explicitNulls.add("lifecycle");
            if (tree.has("autoLocal") && tree.get("autoLocal").isNull()) $explicitNulls.add("autoLocal");
            return new LocalModelDescriptor(readProperty(context, tree, "id", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "displayName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "azureMl", new com.fasterxml.jackson.core.type.TypeReference<LocalModelAzureMlIdentity>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<LocalModelSource>() {}), readProperty(context, tree, "fileName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "provider", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "license", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "sourceLabel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "approximateBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "peakDiskBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "approximateVramBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "runtime", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "runtimeProvisioned", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "providerName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "selectionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "lifecycle", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "serveOptions", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "limits", new com.fasterxml.jackson.core.type.TypeReference<LocalModelLimits>() {}), readProperty(context, tree, "supportsToolCalls", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "autoLocal", new com.fasterxml.jackson.core.type.TypeReference<LocalModelAutoRegistration>() {}), readProperty(context, tree, "metadata", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
