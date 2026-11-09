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
 * Host preferences for its connection-owned manager, not authorization claims.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelsConfigureParams.$NullableDeserializer.class)
public record LocalModelsConfigureParams(
    /** Attached source session for live registration and selection. Its lifetime does not change. */
    @JsonProperty("sessionId") String sessionId,
    /** Whether the host currently offers local model management. */
    @JsonProperty("enabled") Boolean enabled,
    /** Whether the host currently displays the downloadable catalog. */
    @JsonProperty("catalogEnabled") Boolean catalogEnabled,
    /** Request the internal registry only when runtime-owned staff admission also permits it. */
    @JsonProperty("includeDev") Boolean includeDev,
    /** Suppress internal model facts while the host displays shared content. */
    @JsonProperty("streamerMode") Boolean streamerMode,
    /** Host diagnostic log path, used only in failure guidance. */
    @JsonProperty("diagnosticLogPath") String diagnosticLogPath,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelsConfigureParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelsConfigureParams(String sessionId, Boolean enabled, Boolean catalogEnabled, Boolean includeDev, Boolean streamerMode, String diagnosticLogPath) {
        this(sessionId, enabled, catalogEnabled, includeDev, streamerMode, diagnosticLogPath, java.util.Set.of());
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
        private String sessionId;
        private boolean $presence0;
        private Boolean enabled;
        private Boolean catalogEnabled;
        private Boolean includeDev;
        private Boolean streamerMode;
        private String diagnosticLogPath;
        private boolean $presence5;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code catalogEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder catalogEnabled(Boolean value) {
            this.catalogEnabled = value;
            return this;
        }

        /**
         * Sets the {@code includeDev} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder includeDev(Boolean value) {
            this.includeDev = value;
            return this;
        }

        /**
         * Sets the {@code streamerMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder streamerMode(Boolean value) {
            this.streamerMode = value;
            return this;
        }

        /**
         * Sets the {@code diagnosticLogPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder diagnosticLogPath(String value) {
            this.diagnosticLogPath = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelsConfigureParams build() {
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            if (catalogEnabled == null) throw new IllegalStateException("Missing required property: catalogEnabled");
            if (includeDev == null) throw new IllegalStateException("Missing required property: includeDev");
            if (streamerMode == null) throw new IllegalStateException("Missing required property: streamerMode");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && sessionId == null) $explicitNulls.add("sessionId");
            if ($presence5 && diagnosticLogPath == null) $explicitNulls.add("diagnosticLogPath");
            return new LocalModelsConfigureParams(sessionId, enabled, catalogEnabled, includeDev, streamerMode, diagnosticLogPath, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("sessionId") && sessionId == null) values.put("sessionId", null);
        if ($explicitNulls.contains("diagnosticLogPath") && diagnosticLogPath == null) values.put("diagnosticLogPath", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelsConfigureParams> {
        @Override
        public LocalModelsConfigureParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelsConfigureParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("sessionId") && tree.get("sessionId").isNull()) $explicitNulls.add("sessionId");
            if (tree.has("diagnosticLogPath") && tree.get("diagnosticLogPath").isNull()) $explicitNulls.add("diagnosticLogPath");
            return new LocalModelsConfigureParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "catalogEnabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "includeDev", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "streamerMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "diagnosticLogPath", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
