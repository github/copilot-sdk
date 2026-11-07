/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "hook.start". Hook invocation start details including type and input data
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HookStartEvent extends SessionEvent {

    @Override
    public String getType() { return "hook.start"; }

    @JsonProperty("data")
    private HookStartEventData data;

    public HookStartEventData getData() { return data; }
    public void setData(HookStartEventData data) { this.data = data; }

    /** Data payload for {@link HookStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = HookStartEventData.$NullableDeserializer.class)
    public record HookStartEventData(
        /** Unique identifier for this hook invocation */
        @JsonProperty("hookInvocationId") String hookInvocationId,
        /** Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart") */
        @JsonProperty("hookType") String hookType,
        /** Input data passed to the hook. For postToolUse hooks the retained copy served by session.eventLog.read (and by a resumed session) drops the tool result's inline `contents`/`uiResource`/`skillInvocation` and replaces duplicated text result fields with a `[copilot:elided ...]` marker; the live subscription stream still delivers the full value. Canonical tool output remains in the adjacent tool.execution_complete event, while an invoked skill's authoritative body remains in its skill invocation event. */
        @JsonProperty("input") Object input,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public HookStartEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public HookStartEventData(String hookInvocationId, String hookType, Object input, String parentToolCallId) {
            this(hookInvocationId, hookType, input, parentToolCallId, java.util.Set.of());
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
            private String hookInvocationId;
            private String hookType;
            private Object input;
            private boolean $presence2;
            private String parentToolCallId;

            /**
             * Sets the {@code hookInvocationId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder hookInvocationId(String value) {
                this.hookInvocationId = value;
                return this;
            }

            /**
             * Sets the {@code hookType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder hookType(String value) {
                this.hookType = value;
                return this;
            }

            /**
             * Sets the {@code input} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder input(Object value) {
                this.input = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public HookStartEventData build() {
                if (hookInvocationId == null) throw new IllegalStateException("Missing required property: hookInvocationId");
                if (hookType == null) throw new IllegalStateException("Missing required property: hookType");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence2 && input == null) $explicitNulls.add("input");
                return new HookStartEventData(hookInvocationId, hookType, input, parentToolCallId, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("input") && input == null) values.put("input", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<HookStartEventData> {
            @Override
            public HookStartEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(HookStartEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("input") && tree.get("input").isNull()) $explicitNulls.add("input");
                return new HookStartEventData(readProperty(context, tree, "hookInvocationId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "hookType", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "input", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "parentToolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
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
