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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.messageAuthorization". Historical decode-only claim from the retired Assisted Permissions authorization extractor. Current runtimes preserve the payload but do not establish authority from it.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionMessageAuthorizationEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.messageAuthorization"; }

    @JsonProperty("data")
    private PermissionMessageAuthorizationEventData data;

    public PermissionMessageAuthorizationEventData getData() { return data; }
    public void setData(PermissionMessageAuthorizationEventData data) { this.data = data; }

    /** Data payload for {@link PermissionMessageAuthorizationEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PermissionMessageAuthorizationEventData.$NullableDeserializer.class)
    public record PermissionMessageAuthorizationEventData(
        /** Deterministic identity of the record, derived from the turn and span offsets so re-extracting the same span mints nothing new. */
        @CopilotExperimental
        @JsonProperty("recordId") String recordId,
        /** The human turn the quoted span was read from. */
        @CopilotExperimental
        @JsonProperty("turnIndex") Long turnIndex,
        /** Whether the claim granted or denied authority. */
        @CopilotExperimental
        @JsonProperty("polarity") PermissionMessageAuthorizationPolarity polarity,
        /** The kind of effect authorized, as an action-class identifier. */
        @CopilotExperimental
        @JsonProperty("actionClass") String actionClass,
        /** Start byte offset of the authorizing span within the turn. */
        @CopilotExperimental
        @JsonProperty("spanStart") Long spanStart,
        /** End byte offset of the authorizing span within the turn. */
        @CopilotExperimental
        @JsonProperty("spanEnd") Long spanEnd,
        /** Concrete named targets that appear verbatim inside the span. */
        @CopilotExperimental
        @JsonProperty("targetMembers") List<String> targetMembers,
        /** The task the permission is scoped to, when the human named one. */
        @CopilotExperimental
        @JsonProperty("task") String task,
        /** The trusted version discriminator, when one exists. Exact shell-command grants carry the byte-identical commands grounded in the human span; world-derived classes carry a file object, remote tip, or runner only when that state was captured safely. An opaque object mirroring the runtime's adjacently-tagged resolution. */
        @CopilotExperimental
        @JsonProperty("world") Object world,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionMessageAuthorizationEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionMessageAuthorizationEventData(String recordId, Long turnIndex, PermissionMessageAuthorizationPolarity polarity, String actionClass, Long spanStart, Long spanEnd, List<String> targetMembers, String task, Object world) {
            this(recordId, turnIndex, polarity, actionClass, spanStart, spanEnd, targetMembers, task, world, java.util.Set.of());
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
            private String recordId;
            private Long turnIndex;
            private PermissionMessageAuthorizationPolarity polarity;
            private String actionClass;
            private Long spanStart;
            private Long spanEnd;
            private List<String> targetMembers;
            private String task;
            private Object world;
            private boolean $presence8;

            /**
             * Sets the {@code recordId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder recordId(String value) {
                this.recordId = value;
                return this;
            }

            /**
             * Sets the {@code turnIndex} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder turnIndex(Long value) {
                this.turnIndex = value;
                return this;
            }

            /**
             * Sets the {@code polarity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder polarity(PermissionMessageAuthorizationPolarity value) {
                this.polarity = value;
                return this;
            }

            /**
             * Sets the {@code actionClass} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder actionClass(String value) {
                this.actionClass = value;
                return this;
            }

            /**
             * Sets the {@code spanStart} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder spanStart(Long value) {
                this.spanStart = value;
                return this;
            }

            /**
             * Sets the {@code spanEnd} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder spanEnd(Long value) {
                this.spanEnd = value;
                return this;
            }

            /**
             * Sets the {@code targetMembers} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder targetMembers(List<String> value) {
                this.targetMembers = value;
                return this;
            }

            /**
             * Sets the {@code task} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder task(String value) {
                this.task = value;
                return this;
            }

            /**
             * Sets the {@code world} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder world(Object value) {
                this.world = value;
                this.$presence8 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PermissionMessageAuthorizationEventData build() {
                if (recordId == null) throw new IllegalStateException("Missing required property: recordId");
                if (turnIndex == null) throw new IllegalStateException("Missing required property: turnIndex");
                if (polarity == null) throw new IllegalStateException("Missing required property: polarity");
                if (actionClass == null) throw new IllegalStateException("Missing required property: actionClass");
                if (spanStart == null) throw new IllegalStateException("Missing required property: spanStart");
                if (spanEnd == null) throw new IllegalStateException("Missing required property: spanEnd");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence8 && world == null) $explicitNulls.add("world");
                return new PermissionMessageAuthorizationEventData(recordId, turnIndex, polarity, actionClass, spanStart, spanEnd, targetMembers, task, world, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("world") && world == null) values.put("world", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<PermissionMessageAuthorizationEventData> {
            @Override
            public PermissionMessageAuthorizationEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(PermissionMessageAuthorizationEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("world") && tree.get("world").isNull()) $explicitNulls.add("world");
                return new PermissionMessageAuthorizationEventData(readProperty(context, tree, "recordId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "turnIndex", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "polarity", new com.fasterxml.jackson.core.type.TypeReference<PermissionMessageAuthorizationPolarity>() {}), readProperty(context, tree, "actionClass", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "spanStart", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "spanEnd", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "targetMembers", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "task", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "world", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
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
