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
 * Session event "hook.end". Hook invocation completion details including output, success status, and error information
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HookEndEvent extends SessionEvent {

    @Override
    public String getType() { return "hook.end"; }

    @JsonProperty("data")
    private HookEndEventData data;

    public HookEndEventData getData() { return data; }
    public void setData(HookEndEventData data) { this.data = data; }

    /** Data payload for {@link HookEndEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = HookEndEventData.$NullableDeserializer.class)
    public record HookEndEventData(
        /** Identifier matching the corresponding hook.start event */
        @JsonProperty("hookInvocationId") String hookInvocationId,
        /** Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart") */
        @JsonProperty("hookType") String hookType,
        /** Output data produced by the hook. Durable and resumed postToolUse receipts may omit messages owned by a successful skill invocation and replace an unchanged skill sessionLog copy with an elision marker; hook-modified or re-sourced values are preserved, and the authoritative body remains in the skill invocation event. */
        @JsonProperty("output") Object output,
        /** Whether the hook completed successfully */
        @JsonProperty("success") Boolean success,
        /** Error details when the hook failed */
        @JsonProperty("error") HookEndError error,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public HookEndEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public HookEndEventData(String hookInvocationId, String hookType, Object output, Boolean success, HookEndError error, String parentToolCallId) {
            this(hookInvocationId, hookType, output, success, error, parentToolCallId, java.util.Set.of());
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
            private Object output;
            private boolean $presence2;
            private Boolean success;
            private HookEndError error;
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
             * Sets the {@code output} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder output(Object value) {
                this.output = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Sets the {@code success} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder success(Boolean value) {
                this.success = value;
                return this;
            }

            /**
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder error(HookEndError value) {
                this.error = value;
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
            public HookEndEventData build() {
                if (hookInvocationId == null) throw new IllegalStateException("Missing required property: hookInvocationId");
                if (hookType == null) throw new IllegalStateException("Missing required property: hookType");
                if (success == null) throw new IllegalStateException("Missing required property: success");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence2 && output == null) $explicitNulls.add("output");
                return new HookEndEventData(hookInvocationId, hookType, output, success, error, parentToolCallId, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("output") && output == null) values.put("output", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<HookEndEventData> {
            @Override
            public HookEndEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(HookEndEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("output") && tree.get("output").isNull()) $explicitNulls.add("output");
                return new HookEndEventData(readProperty(context, tree, "hookInvocationId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "hookType", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "output", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "success", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<HookEndError>() {}), readProperty(context, tree, "parentToolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
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
