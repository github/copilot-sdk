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
 * A message for a host's activity view.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelTimelineMessage.$NullableDeserializer.class)
public record LocalModelTimelineMessage(
    /** Information, warning, or error. */
    @JsonProperty("type") LocalModelTimelineMessageType type,
    /** Message text. */
    @JsonProperty("text") String text,
    /** Whether the text uses Markdown. */
    @JsonProperty("markdown") Boolean markdown,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /** Information, warning, or error. */
    public enum LocalModelTimelineMessageType {
        /** The {@code info} variant. */
        INFO("info"),
        /** The {@code warning} variant. */
        WARNING("warning"),
        /** The {@code error} variant. */
        ERROR("error");

        private final String value;
        LocalModelTimelineMessageType(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static LocalModelTimelineMessageType fromValue(String value) {
            for (LocalModelTimelineMessageType v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown LocalModelTimelineMessageType value: " + value);
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelTimelineMessage { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelTimelineMessage(LocalModelTimelineMessageType type, String text, Boolean markdown) {
        this(type, text, markdown, java.util.Set.of());
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
        private LocalModelTimelineMessageType type;
        private String text;
        private Boolean markdown;
        private boolean $presence2;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(LocalModelTimelineMessageType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code text} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder text(String value) {
            this.text = value;
            return this;
        }

        /**
         * Sets the {@code markdown} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder markdown(Boolean value) {
            this.markdown = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelTimelineMessage build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (text == null) throw new IllegalStateException("Missing required property: text");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && markdown == null) $explicitNulls.add("markdown");
            return new LocalModelTimelineMessage(type, text, markdown, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("markdown") && markdown == null) values.put("markdown", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelTimelineMessage> {
        @Override
        public LocalModelTimelineMessage deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelTimelineMessage.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("markdown") && tree.get("markdown").isNull()) $explicitNulls.add("markdown");
            return new LocalModelTimelineMessage(readProperty(context, tree, "type", new com.fasterxml.jackson.core.type.TypeReference<LocalModelTimelineMessageType>() {}), readProperty(context, tree, "text", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "markdown", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
