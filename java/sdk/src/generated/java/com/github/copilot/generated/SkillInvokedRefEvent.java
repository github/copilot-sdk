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
 * Session event "skill.invoked_ref". Internal durable skill invocation receipt whose content resolves from an earlier inline skill event in the same session.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillInvokedRefEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.invoked_ref"; }

    @JsonProperty("data")
    private SkillInvokedRefEventData data;

    public SkillInvokedRefEventData getData() { return data; }
    public void setData(SkillInvokedRefEventData data) { this.data = data; }

    /** Data payload for {@link SkillInvokedRefEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SkillInvokedRefEventData.$NullableDeserializer.class)
    public record SkillInvokedRefEventData(
        /** Name of the invoked skill */
        @JsonProperty("name") String name,
        /** Projected chat-message count when the skill was invoked. Preserved from the inline event data when the authored body is deduplicated. */
        @JsonProperty("invokedAtTurn") Long invokedAtTurn,
        /** Model identifier active when the skill was invoked, when known */
        @JsonProperty("model") String model,
        /** File path to the SKILL.md definition, or an empty string for an SDK-provided skill without a filesystem identity */
        @JsonProperty("path") String path,
        /** Content identifier of an earlier inline skill event in this session, in the prefixed form `sha256:<lowercase hex digest>` over the UTF-8 bytes of that event's `content` */
        @JsonProperty("contentId") String contentId,
        /** UTF-16 code unit length of the referenced skill content. Derived from the referenced body and validated against it when the reference is expanded; a reference whose length disagrees with the body it names is rejected instead of expanded */
        @JsonProperty("contentLength") Long contentLength,
        /** Exact invocation arguments preserved from this invocation's inline event data, not from the referenced reusable body */
        @JsonProperty("arguments") String arguments,
        /** Tool names that should be auto-approved when this skill is active */
        @JsonProperty("allowedTools") List<String> allowedTools,
        /** Whether model invocation is disabled for this skill */
        @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
        /** Source identifier for where the skill was discovered */
        @JsonProperty("source") String source,
        /** Held MCP skill origin and manifest, preserved from the inline invocation. */
        @JsonProperty("mcpOrigin") Object mcpOrigin,
        /** MCP resource metadata preserved from the inline invocation. */
        @JsonProperty("mcpMeta") Object mcpMeta,
        /** Name of the plugin this skill originated from, when applicable */
        @JsonProperty("pluginName") String pluginName,
        /** Version of the plugin this skill originated from, when applicable */
        @JsonProperty("pluginVersion") String pluginVersion,
        /** Description of the skill from its SKILL.md frontmatter */
        @JsonProperty("description") String description,
        /** What triggered the skill invocation */
        @JsonProperty("trigger") SkillInvokedTrigger trigger,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param name Name of the invoked skill
         * @param invokedAtTurn Projected chat-message count when the skill was invoked. Preserved from the inline event data when the authored body is deduplicated.
         * @param model Model identifier active when the skill was invoked, when known
         * @param path File path to the SKILL.md definition, or an empty string for an SDK-provided skill without a filesystem identity
         * @param contentId Content identifier of an earlier inline skill event in this session, in the prefixed form `sha256:<lowercase hex digest>` over the UTF-8 bytes of that event's `content`
         * @param contentLength UTF-16 code unit length of the referenced skill content. Derived from the referenced body and validated against it when the reference is expanded; a reference whose length disagrees with the body it names is rejected instead of expanded
         * @param allowedTools Tool names that should be auto-approved when this skill is active
         * @param disableModelInvocation Whether model invocation is disabled for this skill
         * @param source Source identifier for where the skill was discovered
         * @param pluginName Name of the plugin this skill originated from, when applicable
         * @param pluginVersion Version of the plugin this skill originated from, when applicable
         * @param description Description of the skill from its SKILL.md frontmatter
         * @param trigger What triggered the skill invocation
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SkillInvokedRefEventData(
            String name,
            Long invokedAtTurn,
            String model,
            String path,
            String contentId,
            Long contentLength,
            List<String> allowedTools,
            Boolean disableModelInvocation,
            String source,
            String pluginName,
            String pluginVersion,
            String description,
            SkillInvokedTrigger trigger
        ) {
            this(name, invokedAtTurn, model, path, contentId, contentLength, null, allowedTools, disableModelInvocation, source, null, null, pluginName, pluginVersion, description, trigger);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SkillInvokedRefEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SkillInvokedRefEventData(String name, Long invokedAtTurn, String model, String path, String contentId, Long contentLength, String arguments, List<String> allowedTools, Boolean disableModelInvocation, String source, Object mcpOrigin, Object mcpMeta, String pluginName, String pluginVersion, String description, SkillInvokedTrigger trigger) {
            this(name, invokedAtTurn, model, path, contentId, contentLength, arguments, allowedTools, disableModelInvocation, source, mcpOrigin, mcpMeta, pluginName, pluginVersion, description, trigger, java.util.Set.of());
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
            private String name;
            private Long invokedAtTurn;
            private String model;
            private String path;
            private String contentId;
            private Long contentLength;
            private String arguments;
            private List<String> allowedTools;
            private Boolean disableModelInvocation;
            private String source;
            private Object mcpOrigin;
            private boolean $presence10;
            private Object mcpMeta;
            private boolean $presence11;
            private String pluginName;
            private String pluginVersion;
            private String description;
            private SkillInvokedTrigger trigger;

            /**
             * Sets the {@code name} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder name(String value) {
                this.name = value;
                return this;
            }

            /**
             * Sets the {@code invokedAtTurn} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder invokedAtTurn(Long value) {
                this.invokedAtTurn = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code path} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder path(String value) {
                this.path = value;
                return this;
            }

            /**
             * Sets the {@code contentId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contentId(String value) {
                this.contentId = value;
                return this;
            }

            /**
             * Sets the {@code contentLength} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contentLength(Long value) {
                this.contentLength = value;
                return this;
            }

            /**
             * Sets the {@code arguments} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder arguments(String value) {
                this.arguments = value;
                return this;
            }

            /**
             * Sets the {@code allowedTools} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder allowedTools(List<String> value) {
                this.allowedTools = value;
                return this;
            }

            /**
             * Sets the {@code disableModelInvocation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder disableModelInvocation(Boolean value) {
                this.disableModelInvocation = value;
                return this;
            }

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(String value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code mcpOrigin} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpOrigin(Object value) {
                this.mcpOrigin = value;
                this.$presence10 = true;
                return this;
            }

            /**
             * Sets the {@code mcpMeta} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpMeta(Object value) {
                this.mcpMeta = value;
                this.$presence11 = true;
                return this;
            }

            /**
             * Sets the {@code pluginName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder pluginName(String value) {
                this.pluginName = value;
                return this;
            }

            /**
             * Sets the {@code pluginVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder pluginVersion(String value) {
                this.pluginVersion = value;
                return this;
            }

            /**
             * Sets the {@code description} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder description(String value) {
                this.description = value;
                return this;
            }

            /**
             * Sets the {@code trigger} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder trigger(SkillInvokedTrigger value) {
                this.trigger = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SkillInvokedRefEventData build() {
                if (name == null) throw new IllegalStateException("Missing required property: name");
                if (path == null) throw new IllegalStateException("Missing required property: path");
                if (contentId == null) throw new IllegalStateException("Missing required property: contentId");
                if (contentLength == null) throw new IllegalStateException("Missing required property: contentLength");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence10 && mcpOrigin == null) $explicitNulls.add("mcpOrigin");
                if ($presence11 && mcpMeta == null) $explicitNulls.add("mcpMeta");
                return new SkillInvokedRefEventData(name, invokedAtTurn, model, path, contentId, contentLength, arguments, allowedTools, disableModelInvocation, source, mcpOrigin, mcpMeta, pluginName, pluginVersion, description, trigger, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("mcpOrigin") && mcpOrigin == null) values.put("mcpOrigin", null);
            if ($explicitNulls.contains("mcpMeta") && mcpMeta == null) values.put("mcpMeta", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SkillInvokedRefEventData> {
            @Override
            public SkillInvokedRefEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(SkillInvokedRefEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("mcpOrigin") && tree.get("mcpOrigin").isNull()) $explicitNulls.add("mcpOrigin");
                if (tree.has("mcpMeta") && tree.get("mcpMeta").isNull()) $explicitNulls.add("mcpMeta");
                return new SkillInvokedRefEventData(readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "invokedAtTurn", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "path", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "contentId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "contentLength", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "arguments", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "allowedTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "disableModelInvocation", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "mcpOrigin", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "mcpMeta", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "pluginName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "pluginVersion", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "description", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "trigger", new com.fasterxml.jackson.core.type.TypeReference<SkillInvokedTrigger>() {}), $explicitNulls);
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
