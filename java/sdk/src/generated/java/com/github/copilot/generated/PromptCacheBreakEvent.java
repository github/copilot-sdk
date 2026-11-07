/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "prompt_cache_break". A detected loss of a previously cached prompt prefix
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PromptCacheBreakEvent extends SessionEvent {

    @Override
    public String getType() { return "prompt_cache_break"; }

    @JsonProperty("data")
    private PromptCacheBreakEventData data;

    public PromptCacheBreakEventData getData() { return data; }
    public void setData(PromptCacheBreakEventData data) { this.data = data; }

    /** Data payload for {@link PromptCacheBreakEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PromptCacheBreakEventData.$NullableDeserializer.class)
    public record PromptCacheBreakEventData(
        /** The highest-precedence reason for the cache break */
        @JsonProperty("primaryReason") String primaryReason,
        /** All reasons that contributed to the cache break, ordered by precedence */
        @JsonProperty("contributingReasons") List<String> contributingReasons,
        /** Request state that established the prior cache frontier */
        @JsonProperty("beforeRequest") Object beforeRequest,
        /** Request state whose cached prefix fell short */
        @JsonProperty("afterRequest") Object afterRequest,
        /** Number of cached prefix tokens that survived */
        @JsonProperty("survivedTokens") Long survivedTokens,
        /** Prior cached prompt frontier in tokens */
        @JsonProperty("frontierTokens") Long frontierTokens,
        /** Cached prefix tokens lost since the prior call */
        @JsonProperty("shortfallTokens") Long shortfallTokens,
        /** Fraction of the prior cache frontier that survived */
        @JsonProperty("retentionRatio") Double retentionRatio,
        /** Model that held the prior cache frontier, when the call changed models */
        @JsonProperty("modelFrom") String modelFrom,
        /** Model this call targeted, when the call changed models */
        @JsonProperty("modelTo") String modelTo,
        /** Telemetry-safe names of tools added since the prior call */
        @JsonProperty("toolsAdded") List<String> toolsAdded,
        /** Telemetry-safe names of tools removed since the prior call */
        @JsonProperty("toolsRemoved") List<String> toolsRemoved,
        /** Telemetry-safe names of tools whose definition changed since the prior call */
        @JsonProperty("toolsRedefined") List<String> toolsRedefined,
        /** Raw names of tools added since the prior call, restricted because a tool name can be user-authored */
        @JsonProperty("toolsAddedRaw") List<String> toolsAddedRaw,
        /** Raw names of tools removed since the prior call, restricted because a tool name can be user-authored */
        @JsonProperty("toolsRemovedRaw") List<String> toolsRemovedRaw,
        /** Raw names of tools redefined since the prior call, restricted because a tool name can be user-authored */
        @JsonProperty("toolsRedefinedRaw") List<String> toolsRedefinedRaw,
        /** Changed definition parts of redefined tools, as `tool:part` entries; property-level parts only for telemetry-safe tools, whose other names are hashed */
        @JsonProperty("toolsRedefinedParts") List<String> toolsRedefinedParts,
        /** Whether the tool list kept its members but changed their order */
        @JsonProperty("toolsReordered") Boolean toolsReordered,
        /** Names of the system-prompt segments whose content changed */
        @JsonProperty("systemSegmentsChanged") List<String> systemSegmentsChanged,
        /** Names of the cache-configuration fields that changed */
        @JsonProperty("cacheConfigChangedFields") List<String> cacheConfigChangedFields,
        /** Index of the first conversation message whose content changed */
        @JsonProperty("rewriteMessageIndex") Long rewriteMessageIndex,
        /** Shape of the history rewrite, for example whether the history grew or shrank */
        @JsonProperty("rewriteShape") String rewriteShape,
        /** Subsystems that announced a history rewrite before this call, for example compaction or truncation */
        @JsonProperty("rewriteSource") List<String> rewriteSource,
        /** Name of the sub-agent whose conversation broke, stamped by the parent bridge */
        @JsonProperty("agentName") String agentName,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PromptCacheBreakEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PromptCacheBreakEventData(String primaryReason, List<String> contributingReasons, Object beforeRequest, Object afterRequest, Long survivedTokens, Long frontierTokens, Long shortfallTokens, Double retentionRatio, String modelFrom, String modelTo, List<String> toolsAdded, List<String> toolsRemoved, List<String> toolsRedefined, List<String> toolsAddedRaw, List<String> toolsRemovedRaw, List<String> toolsRedefinedRaw, List<String> toolsRedefinedParts, Boolean toolsReordered, List<String> systemSegmentsChanged, List<String> cacheConfigChangedFields, Long rewriteMessageIndex, String rewriteShape, List<String> rewriteSource, String agentName) {
            this(primaryReason, contributingReasons, beforeRequest, afterRequest, survivedTokens, frontierTokens, shortfallTokens, retentionRatio, modelFrom, modelTo, toolsAdded, toolsRemoved, toolsRedefined, toolsAddedRaw, toolsRemovedRaw, toolsRedefinedRaw, toolsRedefinedParts, toolsReordered, systemSegmentsChanged, cacheConfigChangedFields, rewriteMessageIndex, rewriteShape, rewriteSource, agentName, java.util.Set.of());
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
            private String primaryReason;
            private List<String> contributingReasons;
            private Object beforeRequest;
            private boolean $presence2;
            private Object afterRequest;
            private boolean $presence3;
            private Long survivedTokens;
            private Long frontierTokens;
            private Long shortfallTokens;
            private Double retentionRatio;
            private String modelFrom;
            private String modelTo;
            private List<String> toolsAdded;
            private List<String> toolsRemoved;
            private List<String> toolsRedefined;
            private List<String> toolsAddedRaw;
            private List<String> toolsRemovedRaw;
            private List<String> toolsRedefinedRaw;
            private List<String> toolsRedefinedParts;
            private Boolean toolsReordered;
            private List<String> systemSegmentsChanged;
            private List<String> cacheConfigChangedFields;
            private Long rewriteMessageIndex;
            private String rewriteShape;
            private List<String> rewriteSource;
            private String agentName;

            /**
             * Sets the {@code primaryReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder primaryReason(String value) {
                this.primaryReason = value;
                return this;
            }

            /**
             * Sets the {@code contributingReasons} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contributingReasons(List<String> value) {
                this.contributingReasons = value;
                return this;
            }

            /**
             * Sets the {@code beforeRequest} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder beforeRequest(Object value) {
                this.beforeRequest = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Sets the {@code afterRequest} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder afterRequest(Object value) {
                this.afterRequest = value;
                this.$presence3 = true;
                return this;
            }

            /**
             * Sets the {@code survivedTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder survivedTokens(Long value) {
                this.survivedTokens = value;
                return this;
            }

            /**
             * Sets the {@code frontierTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder frontierTokens(Long value) {
                this.frontierTokens = value;
                return this;
            }

            /**
             * Sets the {@code shortfallTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder shortfallTokens(Long value) {
                this.shortfallTokens = value;
                return this;
            }

            /**
             * Sets the {@code retentionRatio} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder retentionRatio(Double value) {
                this.retentionRatio = value;
                return this;
            }

            /**
             * Sets the {@code modelFrom} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelFrom(String value) {
                this.modelFrom = value;
                return this;
            }

            /**
             * Sets the {@code modelTo} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelTo(String value) {
                this.modelTo = value;
                return this;
            }

            /**
             * Sets the {@code toolsAdded} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsAdded(List<String> value) {
                this.toolsAdded = value;
                return this;
            }

            /**
             * Sets the {@code toolsRemoved} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsRemoved(List<String> value) {
                this.toolsRemoved = value;
                return this;
            }

            /**
             * Sets the {@code toolsRedefined} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsRedefined(List<String> value) {
                this.toolsRedefined = value;
                return this;
            }

            /**
             * Sets the {@code toolsAddedRaw} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsAddedRaw(List<String> value) {
                this.toolsAddedRaw = value;
                return this;
            }

            /**
             * Sets the {@code toolsRemovedRaw} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsRemovedRaw(List<String> value) {
                this.toolsRemovedRaw = value;
                return this;
            }

            /**
             * Sets the {@code toolsRedefinedRaw} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsRedefinedRaw(List<String> value) {
                this.toolsRedefinedRaw = value;
                return this;
            }

            /**
             * Sets the {@code toolsRedefinedParts} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsRedefinedParts(List<String> value) {
                this.toolsRedefinedParts = value;
                return this;
            }

            /**
             * Sets the {@code toolsReordered} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolsReordered(Boolean value) {
                this.toolsReordered = value;
                return this;
            }

            /**
             * Sets the {@code systemSegmentsChanged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder systemSegmentsChanged(List<String> value) {
                this.systemSegmentsChanged = value;
                return this;
            }

            /**
             * Sets the {@code cacheConfigChangedFields} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cacheConfigChangedFields(List<String> value) {
                this.cacheConfigChangedFields = value;
                return this;
            }

            /**
             * Sets the {@code rewriteMessageIndex} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rewriteMessageIndex(Long value) {
                this.rewriteMessageIndex = value;
                return this;
            }

            /**
             * Sets the {@code rewriteShape} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rewriteShape(String value) {
                this.rewriteShape = value;
                return this;
            }

            /**
             * Sets the {@code rewriteSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rewriteSource(List<String> value) {
                this.rewriteSource = value;
                return this;
            }

            /**
             * Sets the {@code agentName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentName(String value) {
                this.agentName = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PromptCacheBreakEventData build() {
                if (primaryReason == null) throw new IllegalStateException("Missing required property: primaryReason");
                if (contributingReasons == null) throw new IllegalStateException("Missing required property: contributingReasons");
                if (survivedTokens == null) throw new IllegalStateException("Missing required property: survivedTokens");
                if (frontierTokens == null) throw new IllegalStateException("Missing required property: frontierTokens");
                if (shortfallTokens == null) throw new IllegalStateException("Missing required property: shortfallTokens");
                if (retentionRatio == null) throw new IllegalStateException("Missing required property: retentionRatio");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence2 && beforeRequest == null) $explicitNulls.add("beforeRequest");
                if ($presence3 && afterRequest == null) $explicitNulls.add("afterRequest");
                return new PromptCacheBreakEventData(primaryReason, contributingReasons, beforeRequest, afterRequest, survivedTokens, frontierTokens, shortfallTokens, retentionRatio, modelFrom, modelTo, toolsAdded, toolsRemoved, toolsRedefined, toolsAddedRaw, toolsRemovedRaw, toolsRedefinedRaw, toolsRedefinedParts, toolsReordered, systemSegmentsChanged, cacheConfigChangedFields, rewriteMessageIndex, rewriteShape, rewriteSource, agentName, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("beforeRequest") && beforeRequest == null) values.put("beforeRequest", null);
            if ($explicitNulls.contains("afterRequest") && afterRequest == null) values.put("afterRequest", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<PromptCacheBreakEventData> {
            @Override
            public PromptCacheBreakEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(PromptCacheBreakEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("beforeRequest") && tree.get("beforeRequest").isNull()) $explicitNulls.add("beforeRequest");
                if (tree.has("afterRequest") && tree.get("afterRequest").isNull()) $explicitNulls.add("afterRequest");
                return new PromptCacheBreakEventData(readProperty(context, tree, "primaryReason", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "contributingReasons", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "beforeRequest", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "afterRequest", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "survivedTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "frontierTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "shortfallTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "retentionRatio", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "modelFrom", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "modelTo", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolsAdded", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsRemoved", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsRedefined", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsAddedRaw", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsRemovedRaw", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsRedefinedRaw", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsRedefinedParts", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolsReordered", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "systemSegmentsChanged", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "cacheConfigChangedFields", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "rewriteMessageIndex", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "rewriteShape", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "rewriteSource", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "agentName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
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
