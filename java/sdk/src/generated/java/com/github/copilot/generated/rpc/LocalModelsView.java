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
 * Connection-owned local model state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelsView.$NullableDeserializer.class)
public record LocalModelsView(
    /** Whether the host offers local model management. */
    @JsonProperty("enabled") Boolean enabled,
    /** Local files, runtime detection, and resource requirements. */
    @JsonProperty("machine") LocalModelMachineView machine,
    /** Active download progress by catalog identifier. */
    @JsonProperty("downloads") Map<String, LocalModelDownloadProgress> downloads,
    /** Active setup or removal stage by catalog identifier. */
    @JsonProperty("operations") Map<String, LocalModelOperation> operations,
    /** Persistent presentation notices by catalog identifier. */
    @JsonProperty("notices") Map<String, LocalModelNotice> notices,
    /** Whether a catalog refresh is active. */
    @JsonProperty("catalogLoading") Boolean catalogLoading,
    /** Catalog error, when the refresh failed. */
    @JsonProperty("catalogError") String catalogError,
    /** Whether internal registry models must remain hidden. */
    @JsonProperty("hideDevModels") Boolean hideDevModels,
    /** Whether the downloadable catalog must remain hidden. */
    @JsonProperty("hideWinMlModels") Boolean hideWinMlModels,
    /** Inference runtime prerequisite guidance. */
    @JsonProperty("prerequisiteNotice") String prerequisiteNotice,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelsView { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelsView(Boolean enabled, LocalModelMachineView machine, Map<String, LocalModelDownloadProgress> downloads, Map<String, LocalModelOperation> operations, Map<String, LocalModelNotice> notices, Boolean catalogLoading, String catalogError, Boolean hideDevModels, Boolean hideWinMlModels, String prerequisiteNotice) {
        this(enabled, machine, downloads, operations, notices, catalogLoading, catalogError, hideDevModels, hideWinMlModels, prerequisiteNotice, java.util.Set.of());
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
        private Boolean enabled;
        private LocalModelMachineView machine;
        private Map<String, LocalModelDownloadProgress> downloads;
        private Map<String, LocalModelOperation> operations;
        private Map<String, LocalModelNotice> notices;
        private Boolean catalogLoading;
        private String catalogError;
        private boolean $presence6;
        private Boolean hideDevModels;
        private Boolean hideWinMlModels;
        private String prerequisiteNotice;

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code machine} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder machine(LocalModelMachineView value) {
            this.machine = value;
            return this;
        }

        /**
         * Sets the {@code downloads} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder downloads(Map<String, LocalModelDownloadProgress> value) {
            this.downloads = value;
            return this;
        }

        /**
         * Sets the {@code operations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operations(Map<String, LocalModelOperation> value) {
            this.operations = value;
            return this;
        }

        /**
         * Sets the {@code notices} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder notices(Map<String, LocalModelNotice> value) {
            this.notices = value;
            return this;
        }

        /**
         * Sets the {@code catalogLoading} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogLoading(Boolean value) {
            this.catalogLoading = value;
            return this;
        }

        /**
         * Sets the {@code catalogError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogError(String value) {
            this.catalogError = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code hideDevModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hideDevModels(Boolean value) {
            this.hideDevModels = value;
            return this;
        }

        /**
         * Sets the {@code hideWinMlModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hideWinMlModels(Boolean value) {
            this.hideWinMlModels = value;
            return this;
        }

        /**
         * Sets the {@code prerequisiteNotice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prerequisiteNotice(String value) {
            this.prerequisiteNotice = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelsView build() {
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            if (machine == null) throw new IllegalStateException("Missing required property: machine");
            if (downloads == null) throw new IllegalStateException("Missing required property: downloads");
            if (operations == null) throw new IllegalStateException("Missing required property: operations");
            if (notices == null) throw new IllegalStateException("Missing required property: notices");
            if (catalogLoading == null) throw new IllegalStateException("Missing required property: catalogLoading");
            if (hideDevModels == null) throw new IllegalStateException("Missing required property: hideDevModels");
            if (hideWinMlModels == null) throw new IllegalStateException("Missing required property: hideWinMlModels");
            if (prerequisiteNotice == null) throw new IllegalStateException("Missing required property: prerequisiteNotice");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence6 && catalogError == null) $explicitNulls.add("catalogError");
            return new LocalModelsView(enabled, machine, downloads, operations, notices, catalogLoading, catalogError, hideDevModels, hideWinMlModels, prerequisiteNotice, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("catalogError") && catalogError == null) values.put("catalogError", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelsView> {
        @Override
        public LocalModelsView deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelsView.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("catalogError") && tree.get("catalogError").isNull()) $explicitNulls.add("catalogError");
            return new LocalModelsView(readProperty(context, tree, "enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "machine", new com.fasterxml.jackson.core.type.TypeReference<LocalModelMachineView>() {}), readProperty(context, tree, "downloads", new com.fasterxml.jackson.core.type.TypeReference<Map<String, LocalModelDownloadProgress>>() {}), readProperty(context, tree, "operations", new com.fasterxml.jackson.core.type.TypeReference<Map<String, LocalModelOperation>>() {}), readProperty(context, tree, "notices", new com.fasterxml.jackson.core.type.TypeReference<Map<String, LocalModelNotice>>() {}), readProperty(context, tree, "catalogLoading", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "catalogError", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "hideDevModels", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "hideWinMlModels", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "prerequisiteNotice", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
