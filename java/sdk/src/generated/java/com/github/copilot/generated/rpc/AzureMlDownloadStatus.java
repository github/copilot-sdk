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
 * Progress and retry facts for one active download.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlDownloadStatus.$NullableDeserializer.class)
public record AzureMlDownloadStatus(
    /** Current transfer phase. */
    @JsonProperty("phase") String phase,
    /** Bytes completed across all files. */
    @JsonProperty("completedBytes") Long completedBytes,
    /** Exact bytes expected across all files. */
    @JsonProperty("totalBytes") Long totalBytes,
    /** Next retry attempt, if the transfer is retrying. */
    @JsonProperty("retryAttempt") Long retryAttempt,
    /** Configured retry limit, if the transfer is retrying. */
    @JsonProperty("maxAttempts") Long maxAttempts,
    /** Redacted failure reason, if the transfer is retrying. */
    @JsonProperty("retryReason") String retryReason,
    /** Planned delay before retry, if the transfer is retrying. */
    @JsonProperty("retryDelayMs") Long retryDelayMs,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlDownloadStatus { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlDownloadStatus(String phase, Long completedBytes, Long totalBytes, Long retryAttempt, Long maxAttempts, String retryReason, Long retryDelayMs) {
        this(phase, completedBytes, totalBytes, retryAttempt, maxAttempts, retryReason, retryDelayMs, java.util.Set.of());
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
        private String phase;
        private Long completedBytes;
        private Long totalBytes;
        private Long retryAttempt;
        private boolean $presence3;
        private Long maxAttempts;
        private boolean $presence4;
        private String retryReason;
        private boolean $presence5;
        private Long retryDelayMs;
        private boolean $presence6;

        /**
         * Sets the {@code phase} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phase(String value) {
            this.phase = value;
            return this;
        }

        /**
         * Sets the {@code completedBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder completedBytes(Long value) {
            this.completedBytes = value;
            return this;
        }

        /**
         * Sets the {@code totalBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalBytes(Long value) {
            this.totalBytes = value;
            return this;
        }

        /**
         * Sets the {@code retryAttempt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder retryAttempt(Long value) {
            this.retryAttempt = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code maxAttempts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxAttempts(Long value) {
            this.maxAttempts = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Sets the {@code retryReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder retryReason(String value) {
            this.retryReason = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code retryDelayMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder retryDelayMs(Long value) {
            this.retryDelayMs = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlDownloadStatus build() {
            if (phase == null) throw new IllegalStateException("Missing required property: phase");
            if (completedBytes == null) throw new IllegalStateException("Missing required property: completedBytes");
            if (totalBytes == null) throw new IllegalStateException("Missing required property: totalBytes");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && retryAttempt == null) $explicitNulls.add("retryAttempt");
            if ($presence4 && maxAttempts == null) $explicitNulls.add("maxAttempts");
            if ($presence5 && retryReason == null) $explicitNulls.add("retryReason");
            if ($presence6 && retryDelayMs == null) $explicitNulls.add("retryDelayMs");
            return new AzureMlDownloadStatus(phase, completedBytes, totalBytes, retryAttempt, maxAttempts, retryReason, retryDelayMs, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("retryAttempt") && retryAttempt == null) values.put("retryAttempt", null);
        if ($explicitNulls.contains("maxAttempts") && maxAttempts == null) values.put("maxAttempts", null);
        if ($explicitNulls.contains("retryReason") && retryReason == null) values.put("retryReason", null);
        if ($explicitNulls.contains("retryDelayMs") && retryDelayMs == null) values.put("retryDelayMs", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlDownloadStatus> {
        @Override
        public AzureMlDownloadStatus deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlDownloadStatus.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("retryAttempt") && tree.get("retryAttempt").isNull()) $explicitNulls.add("retryAttempt");
            if (tree.has("maxAttempts") && tree.get("maxAttempts").isNull()) $explicitNulls.add("maxAttempts");
            if (tree.has("retryReason") && tree.get("retryReason").isNull()) $explicitNulls.add("retryReason");
            if (tree.has("retryDelayMs") && tree.get("retryDelayMs").isNull()) $explicitNulls.add("retryDelayMs");
            return new AzureMlDownloadStatus(readProperty(context, tree, "phase", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "completedBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "totalBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "retryAttempt", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxAttempts", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "retryReason", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "retryDelayMs", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
