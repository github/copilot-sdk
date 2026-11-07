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
 * Completed and verified Azure ML model files.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlDownloadResult.$NullableDeserializer.class)
public record AzureMlDownloadResult(
    /** Installed GGUF path. */
    @JsonProperty("primaryPath") String primaryPath,
    /** Published manifest path. */
    @JsonProperty("manifestPath") String manifestPath,
    /** Exact bytes across the model version. */
    @JsonProperty("totalBytes") Long totalBytes,
    /** Whether all prior verified files were reused. */
    @JsonProperty("reused") Boolean reused,
    /** Validated local runtime settings. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("serveOptions") Object serveOptions,
    /** Maximum supported local concurrency. */
    @JsonProperty("maxConcurrent") Long maxConcurrent,
    /** Installed companion file paths. */
    @JsonProperty("extraFiles") List<String> extraFiles,
    /** Transient transfer failures retried during this run. */
    @JsonProperty("totalFailures") Long totalFailures,
    /** Bytes received over the network during this run, including discarded bytes. */
    @JsonProperty("receivedBytes") Long receivedBytes,
    /** Whether a partial from a prior run continued after the server accepted its range. */
    @JsonProperty("resumed") Boolean resumed,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlDownloadResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlDownloadResult(String primaryPath, String manifestPath, Long totalBytes, Boolean reused, Object serveOptions, Long maxConcurrent, List<String> extraFiles, Long totalFailures, Long receivedBytes, Boolean resumed) {
        this(primaryPath, manifestPath, totalBytes, reused, serveOptions, maxConcurrent, extraFiles, totalFailures, receivedBytes, resumed, java.util.Set.of());
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
        private String primaryPath;
        private String manifestPath;
        private Long totalBytes;
        private Boolean reused;
        private Object serveOptions;
        private boolean $presence4;
        private Long maxConcurrent;
        private List<String> extraFiles;
        private Long totalFailures;
        private boolean $presence7;
        private Long receivedBytes;
        private boolean $presence8;
        private Boolean resumed;
        private boolean $presence9;

        /**
         * Sets the {@code primaryPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder primaryPath(String value) {
            this.primaryPath = value;
            return this;
        }

        /**
         * Sets the {@code manifestPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder manifestPath(String value) {
            this.manifestPath = value;
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
         * Sets the {@code reused} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reused(Boolean value) {
            this.reused = value;
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
            this.$presence4 = true;
            return this;
        }

        /**
         * Sets the {@code maxConcurrent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxConcurrent(Long value) {
            this.maxConcurrent = value;
            return this;
        }

        /**
         * Sets the {@code extraFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder extraFiles(List<String> value) {
            this.extraFiles = value;
            return this;
        }

        /**
         * Sets the {@code totalFailures} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalFailures(Long value) {
            this.totalFailures = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code receivedBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder receivedBytes(Long value) {
            this.receivedBytes = value;
            this.$presence8 = true;
            return this;
        }

        /**
         * Sets the {@code resumed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resumed(Boolean value) {
            this.resumed = value;
            this.$presence9 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlDownloadResult build() {
            if (primaryPath == null) throw new IllegalStateException("Missing required property: primaryPath");
            if (manifestPath == null) throw new IllegalStateException("Missing required property: manifestPath");
            if (totalBytes == null) throw new IllegalStateException("Missing required property: totalBytes");
            if (reused == null) throw new IllegalStateException("Missing required property: reused");
            if (!$presence4) throw new IllegalStateException("Missing required property: serveOptions");
            if (maxConcurrent == null) throw new IllegalStateException("Missing required property: maxConcurrent");
            if (extraFiles == null) throw new IllegalStateException("Missing required property: extraFiles");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence7 && totalFailures == null) $explicitNulls.add("totalFailures");
            if ($presence8 && receivedBytes == null) $explicitNulls.add("receivedBytes");
            if ($presence9 && resumed == null) $explicitNulls.add("resumed");
            return new AzureMlDownloadResult(primaryPath, manifestPath, totalBytes, reused, serveOptions, maxConcurrent, extraFiles, totalFailures, receivedBytes, resumed, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("totalFailures") && totalFailures == null) values.put("totalFailures", null);
        if ($explicitNulls.contains("receivedBytes") && receivedBytes == null) values.put("receivedBytes", null);
        if ($explicitNulls.contains("resumed") && resumed == null) values.put("resumed", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlDownloadResult> {
        @Override
        public AzureMlDownloadResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlDownloadResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("totalFailures") && tree.get("totalFailures").isNull()) $explicitNulls.add("totalFailures");
            if (tree.has("receivedBytes") && tree.get("receivedBytes").isNull()) $explicitNulls.add("receivedBytes");
            if (tree.has("resumed") && tree.get("resumed").isNull()) $explicitNulls.add("resumed");
            return new AzureMlDownloadResult(readProperty(context, tree, "primaryPath", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "manifestPath", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "totalBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "reused", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serveOptions", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "maxConcurrent", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "extraFiles", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "totalFailures", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "receivedBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "resumed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
