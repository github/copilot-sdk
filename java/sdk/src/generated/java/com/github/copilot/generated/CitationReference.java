/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A single citation occurrence linking a span of generated text to a supporting source.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CitationReference.$NullableDeserializer.class)
public record CitationReference(
    /** Identifier of the CitationSource this reference points to (CitationSource.id). */
    @JsonProperty("sourceId") String sourceId,
    /** The exact text from the source that supports the cited span, when provided by the model. */
    @JsonProperty("citedText") String citedText,
    /** Location within the source that supports the cited span, when the provider reports one. */
    @JsonProperty("location") Object location,
    /** Provider-native citation correlation data (e.g. Anthropic search_result_index / document_index), passed through opaquely for debugging and forward compatibility. */
    @JsonProperty("providerMetadata") Object providerMetadata,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CitationReference { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CitationReference(String sourceId, String citedText, Object location, Object providerMetadata) {
        this(sourceId, citedText, location, providerMetadata, java.util.Set.of());
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
        private String sourceId;
        private String citedText;
        private Object location;
        private Object providerMetadata;
        private boolean $presence3;

        /**
         * Sets the {@code sourceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sourceId(String value) {
            this.sourceId = value;
            return this;
        }

        /**
         * Sets the {@code citedText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder citedText(String value) {
            this.citedText = value;
            return this;
        }

        /**
         * Sets the {@code location} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder location(Object value) {
            this.location = value;
            return this;
        }

        /**
         * Sets the {@code providerMetadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providerMetadata(Object value) {
            this.providerMetadata = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CitationReference build() {
            if (sourceId == null) throw new IllegalStateException("Missing required property: sourceId");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && providerMetadata == null) $explicitNulls.add("providerMetadata");
            return new CitationReference(sourceId, citedText, location, providerMetadata, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("providerMetadata") && providerMetadata == null) values.put("providerMetadata", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<CitationReference> {
        @Override
        public CitationReference deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(CitationReference.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("providerMetadata") && tree.get("providerMetadata").isNull()) $explicitNulls.add("providerMetadata");
            return new CitationReference(readProperty(context, tree, "sourceId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "citedText", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "location", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "providerMetadata", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
