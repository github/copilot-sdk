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
 * Downloads a model version to its configured local library.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlDownloadParams.$NullableDeserializer.class)
public record AzureMlDownloadParams(
    /** Unique request identifier for progress and explicit cancellation. */
    @JsonProperty("requestId") String requestId,
    /** Registry that owns the model. */
    @JsonProperty("registry") AzureMlRegistry registry,
    /** Exact model-family name. */
    @JsonProperty("name") String name,
    /** Exact published version. */
    @JsonProperty("version") String version,
    /** Version directory under this server's Azure ML library. */
    @JsonProperty("destination") String destination,
    /** Optional connection-owned lease that spans the caller's complete family mutation. */
    @JsonProperty("leaseId") String leaseId,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlDownloadParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlDownloadParams(String requestId, AzureMlRegistry registry, String name, String version, String destination, String leaseId) {
        this(requestId, registry, name, version, destination, leaseId, java.util.Set.of());
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
        private String requestId;
        private AzureMlRegistry registry;
        private String name;
        private String version;
        private String destination;
        private String leaseId;
        private boolean $presence5;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(String value) {
            this.requestId = value;
            return this;
        }

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
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code destination} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder destination(String value) {
            this.destination = value;
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
            this.$presence5 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlDownloadParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (registry == null) throw new IllegalStateException("Missing required property: registry");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (version == null) throw new IllegalStateException("Missing required property: version");
            if (destination == null) throw new IllegalStateException("Missing required property: destination");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && leaseId == null) $explicitNulls.add("leaseId");
            return new AzureMlDownloadParams(requestId, registry, name, version, destination, leaseId, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("leaseId") && leaseId == null) values.put("leaseId", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlDownloadParams> {
        @Override
        public AzureMlDownloadParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlDownloadParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("leaseId") && tree.get("leaseId").isNull()) $explicitNulls.add("leaseId");
            return new AzureMlDownloadParams(readProperty(context, tree, "requestId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "registry", new com.fasterxml.jackson.core.type.TypeReference<AzureMlRegistry>() {}), readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "version", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "destination", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "leaseId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
