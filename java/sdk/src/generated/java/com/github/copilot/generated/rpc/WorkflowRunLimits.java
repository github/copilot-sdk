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
 * Wire-only per-invocation workflow resource ceiling overrides.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = WorkflowRunLimits.$NullableDeserializer.class)
public record WorkflowRunLimits(
    /** Maximum number of workflow subagents that may run concurrently. */
    @JsonProperty("maxConcurrentSubagents") Long maxConcurrentSubagents,
    /** Maximum total number of workflow subagents that may be admitted. */
    @JsonProperty("maxTotalSubagents") Long maxTotalSubagents,
    /** Maximum accumulated active-execution time in seconds. Active execution includes the entire extension body, subprocess waits, queued-agent waits, and sleeps; time between resumed attempts is not counted. */
    @JsonProperty("timeoutSeconds") Double timeoutSeconds,
    /** Maximum AI credits consumed by workflow subagents and their descendants. The post-paid ceiling is soft: parallel turns can settle beyond it before the run stops. */
    @JsonProperty("maxAiCredits") Double maxAiCredits,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowRunLimits { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowRunLimits(Long maxConcurrentSubagents, Long maxTotalSubagents, Double timeoutSeconds, Double maxAiCredits) {
        this(maxConcurrentSubagents, maxTotalSubagents, timeoutSeconds, maxAiCredits, java.util.Set.of());
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
        private Long maxConcurrentSubagents;
        private boolean $presence0;
        private Long maxTotalSubagents;
        private boolean $presence1;
        private Double timeoutSeconds;
        private boolean $presence2;
        private Double maxAiCredits;
        private boolean $presence3;

        /**
         * Sets the {@code maxConcurrentSubagents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxConcurrentSubagents(Long value) {
            this.maxConcurrentSubagents = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code maxTotalSubagents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxTotalSubagents(Long value) {
            this.maxTotalSubagents = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code timeoutSeconds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timeoutSeconds(Double value) {
            this.timeoutSeconds = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code maxAiCredits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxAiCredits(Double value) {
            this.maxAiCredits = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowRunLimits build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && maxConcurrentSubagents == null) $explicitNulls.add("maxConcurrentSubagents");
            if ($presence1 && maxTotalSubagents == null) $explicitNulls.add("maxTotalSubagents");
            if ($presence2 && timeoutSeconds == null) $explicitNulls.add("timeoutSeconds");
            if ($presence3 && maxAiCredits == null) $explicitNulls.add("maxAiCredits");
            return new WorkflowRunLimits(maxConcurrentSubagents, maxTotalSubagents, timeoutSeconds, maxAiCredits, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("maxConcurrentSubagents") && maxConcurrentSubagents == null) values.put("maxConcurrentSubagents", null);
        if ($explicitNulls.contains("maxTotalSubagents") && maxTotalSubagents == null) values.put("maxTotalSubagents", null);
        if ($explicitNulls.contains("timeoutSeconds") && timeoutSeconds == null) values.put("timeoutSeconds", null);
        if ($explicitNulls.contains("maxAiCredits") && maxAiCredits == null) values.put("maxAiCredits", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<WorkflowRunLimits> {
        @Override
        public WorkflowRunLimits deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(WorkflowRunLimits.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("maxConcurrentSubagents") && tree.get("maxConcurrentSubagents").isNull()) $explicitNulls.add("maxConcurrentSubagents");
            if (tree.has("maxTotalSubagents") && tree.get("maxTotalSubagents").isNull()) $explicitNulls.add("maxTotalSubagents");
            if (tree.has("timeoutSeconds") && tree.get("timeoutSeconds").isNull()) $explicitNulls.add("timeoutSeconds");
            if (tree.has("maxAiCredits") && tree.get("maxAiCredits").isNull()) $explicitNulls.add("maxAiCredits");
            return new WorkflowRunLimits(readProperty(context, tree, "maxConcurrentSubagents", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxTotalSubagents", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "timeoutSeconds", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "maxAiCredits", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
