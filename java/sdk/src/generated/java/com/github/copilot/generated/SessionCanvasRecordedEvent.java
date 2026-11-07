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
 * Session event "session.canvas.recorded". Durable record that a canvas instance is open, used to restore open canvases on cold session resume. Intentionally omits the transient url and availability.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCanvasRecordedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.canvas.recorded"; }

    @JsonProperty("data")
    private SessionCanvasRecordedEventData data;

    public SessionCanvasRecordedEventData getData() { return data; }
    public void setData(SessionCanvasRecordedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCanvasRecordedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionCanvasRecordedEventData.$NullableDeserializer.class)
    public record SessionCanvasRecordedEventData(
        /** Stable caller-supplied canvas instance identifier */
        @JsonProperty("instanceId") String instanceId,
        /** Owning provider identifier */
        @JsonProperty("extensionId") String extensionId,
        /** Provider-local canvas identifier */
        @JsonProperty("canvasId") String canvasId,
        /** Rendered title */
        @JsonProperty("title") String title,
        /** Input supplied when the instance was opened */
        @JsonProperty("input") Object input,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCanvasRecordedEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCanvasRecordedEventData(String instanceId, String extensionId, String canvasId, String title, Object input) {
            this(instanceId, extensionId, canvasId, title, input, java.util.Set.of());
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
            private String instanceId;
            private String extensionId;
            private String canvasId;
            private String title;
            private Object input;
            private boolean $presence4;

            /**
             * Sets the {@code instanceId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder instanceId(String value) {
                this.instanceId = value;
                return this;
            }

            /**
             * Sets the {@code extensionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder extensionId(String value) {
                this.extensionId = value;
                return this;
            }

            /**
             * Sets the {@code canvasId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder canvasId(String value) {
                this.canvasId = value;
                return this;
            }

            /**
             * Sets the {@code title} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder title(String value) {
                this.title = value;
                return this;
            }

            /**
             * Sets the {@code input} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder input(Object value) {
                this.input = value;
                this.$presence4 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCanvasRecordedEventData build() {
                if (instanceId == null) throw new IllegalStateException("Missing required property: instanceId");
                if (extensionId == null) throw new IllegalStateException("Missing required property: extensionId");
                if (canvasId == null) throw new IllegalStateException("Missing required property: canvasId");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence4 && input == null) $explicitNulls.add("input");
                return new SessionCanvasRecordedEventData(instanceId, extensionId, canvasId, title, input, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("input") && input == null) values.put("input", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionCanvasRecordedEventData> {
            @Override
            public SessionCanvasRecordedEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(SessionCanvasRecordedEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("input") && tree.get("input").isNull()) $explicitNulls.add("input");
                return new SessionCanvasRecordedEventData(readProperty(context, tree, "instanceId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "extensionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "canvasId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "title", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "input", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
            }

            private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JsonNode tree, String name,
                    com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
                if (!tree.has(name)) return null;
                return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
            }
        }
    }
}
