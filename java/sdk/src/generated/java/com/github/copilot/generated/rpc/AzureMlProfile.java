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
 * Validated local runtime requirements for an Azure ML model.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlProfile.$NullableDeserializer.class)
public record AzureMlProfile(
    /** Sanitized model-picker display label. */
    @JsonProperty("displayName") String displayName,
    /** Published download size, when available. */
    @JsonProperty("downloadBytes") Long downloadBytes,
    /** Peak disk use during installation, when available. */
    @JsonProperty("peakDiskBytes") Long peakDiskBytes,
    /** Recommended graphics memory, when available. */
    @JsonProperty("recommendedVramBytes") Long recommendedVramBytes,
    /** Maximum input tokens accepted by the model. */
    @JsonProperty("maxPromptTokens") Long maxPromptTokens,
    /** Maximum context window accepted by the model. */
    @JsonProperty("maxContextWindowTokens") Long maxContextWindowTokens,
    /** Maximum output tokens produced by the model. */
    @JsonProperty("maxOutputTokens") Long maxOutputTokens,
    /** Whether the model supports tool calls. */
    @JsonProperty("supportsToolCalls") Boolean supportsToolCalls,
    /** Validated local runtime configuration. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("serveOptions") Object serveOptions,
    /** Published license, when available. */
    @JsonProperty("license") AzureMlLicense license,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlProfile { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlProfile(String displayName, Long downloadBytes, Long peakDiskBytes, Long recommendedVramBytes, Long maxPromptTokens, Long maxContextWindowTokens, Long maxOutputTokens, Boolean supportsToolCalls, Object serveOptions, AzureMlLicense license) {
        this(displayName, downloadBytes, peakDiskBytes, recommendedVramBytes, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, supportsToolCalls, serveOptions, license, java.util.Set.of());
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
        private String displayName;
        private Long downloadBytes;
        private boolean $presence1;
        private Long peakDiskBytes;
        private boolean $presence2;
        private Long recommendedVramBytes;
        private boolean $presence3;
        private Long maxPromptTokens;
        private Long maxContextWindowTokens;
        private Long maxOutputTokens;
        private Boolean supportsToolCalls;
        private Object serveOptions;
        private boolean $presence8;
        private AzureMlLicense license;
        private boolean $presence9;

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code downloadBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder downloadBytes(Long value) {
            this.downloadBytes = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code peakDiskBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder peakDiskBytes(Long value) {
            this.peakDiskBytes = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code recommendedVramBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder recommendedVramBytes(Long value) {
            this.recommendedVramBytes = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code maxPromptTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxPromptTokens(Long value) {
            this.maxPromptTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxContextWindowTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxContextWindowTokens(Long value) {
            this.maxContextWindowTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxOutputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxOutputTokens(Long value) {
            this.maxOutputTokens = value;
            return this;
        }

        /**
         * Sets the {@code supportsToolCalls} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder supportsToolCalls(Boolean value) {
            this.supportsToolCalls = value;
            return this;
        }

        /**
         * Sets the {@code serveOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serveOptions(Object value) {
            this.serveOptions = value;
            this.$presence8 = true;
            return this;
        }

        /**
         * Sets the {@code license} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder license(AzureMlLicense value) {
            this.license = value;
            this.$presence9 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlProfile build() {
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (maxPromptTokens == null) throw new IllegalStateException("Missing required property: maxPromptTokens");
            if (maxContextWindowTokens == null) throw new IllegalStateException("Missing required property: maxContextWindowTokens");
            if (maxOutputTokens == null) throw new IllegalStateException("Missing required property: maxOutputTokens");
            if (supportsToolCalls == null) throw new IllegalStateException("Missing required property: supportsToolCalls");
            if (!$presence8) throw new IllegalStateException("Missing required property: serveOptions");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && downloadBytes == null) $explicitNulls.add("downloadBytes");
            if ($presence2 && peakDiskBytes == null) $explicitNulls.add("peakDiskBytes");
            if ($presence3 && recommendedVramBytes == null) $explicitNulls.add("recommendedVramBytes");
            if ($presence9 && license == null) $explicitNulls.add("license");
            return new AzureMlProfile(displayName, downloadBytes, peakDiskBytes, recommendedVramBytes, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, supportsToolCalls, serveOptions, license, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("downloadBytes") && downloadBytes == null) values.put("downloadBytes", null);
        if ($explicitNulls.contains("peakDiskBytes") && peakDiskBytes == null) values.put("peakDiskBytes", null);
        if ($explicitNulls.contains("recommendedVramBytes") && recommendedVramBytes == null) values.put("recommendedVramBytes", null);
        if ($explicitNulls.contains("license") && license == null) values.put("license", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlProfile> {
        @Override
        public AzureMlProfile deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlProfile.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("downloadBytes") && tree.get("downloadBytes").isNull()) $explicitNulls.add("downloadBytes");
            if (tree.has("peakDiskBytes") && tree.get("peakDiskBytes").isNull()) $explicitNulls.add("peakDiskBytes");
            if (tree.has("recommendedVramBytes") && tree.get("recommendedVramBytes").isNull()) $explicitNulls.add("recommendedVramBytes");
            if (tree.has("license") && tree.get("license").isNull()) $explicitNulls.add("license");
            return new AzureMlProfile(readProperty(context, tree, "displayName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "downloadBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "peakDiskBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "recommendedVramBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxPromptTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxContextWindowTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxOutputTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "supportsToolCalls", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serveOptions", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "license", new com.fasterxml.jackson.core.type.TypeReference<AzureMlLicense>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
