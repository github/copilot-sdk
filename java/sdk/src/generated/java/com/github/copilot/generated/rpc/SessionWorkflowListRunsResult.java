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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A page of workflow runs in durable creation order.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionWorkflowListRunsResult.$NullableDeserializer.class)
public record SessionWorkflowListRunsResult(
    /** Workflow run summaries in durable creation order. */
    @JsonProperty("runs") List<WorkflowRunSummary> runs,
    /** Oldest terminal-run cursor in this page, or null when the terminal window is empty. */
    @JsonProperty("oldestSeq") Long oldestSeq,
    /** Newest terminal-run cursor in this page, or null when the terminal window is empty. */
    @JsonProperty("newestSeq") Long newestSeq,
    /** Whether terminal runs newer than this page exist. */
    @JsonProperty("hasMoreNewer") Boolean hasMoreNewer,
    /** Number of terminal runs older than this page. */
    @JsonProperty("omittedOlder") Long omittedOlder,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkflowListRunsResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkflowListRunsResult(List<WorkflowRunSummary> runs, Long oldestSeq, Long newestSeq, Boolean hasMoreNewer, Long omittedOlder) {
        this(runs, oldestSeq, newestSeq, hasMoreNewer, omittedOlder, java.util.Set.of());
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
        private List<WorkflowRunSummary> runs;
        private Long oldestSeq;
        private boolean $presence1;
        private Long newestSeq;
        private boolean $presence2;
        private Boolean hasMoreNewer;
        private Long omittedOlder;

        /**
         * Sets the {@code runs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runs(List<WorkflowRunSummary> value) {
            this.runs = value;
            return this;
        }

        /**
         * Sets the {@code oldestSeq} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder oldestSeq(Long value) {
            this.oldestSeq = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code newestSeq} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder newestSeq(Long value) {
            this.newestSeq = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code hasMoreNewer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasMoreNewer(Boolean value) {
            this.hasMoreNewer = value;
            return this;
        }

        /**
         * Sets the {@code omittedOlder} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder omittedOlder(Long value) {
            this.omittedOlder = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkflowListRunsResult build() {
            if (runs == null) throw new IllegalStateException("Missing required property: runs");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && oldestSeq == null) $explicitNulls.add("oldestSeq");
            if ($presence2 && newestSeq == null) $explicitNulls.add("newestSeq");
            return new SessionWorkflowListRunsResult(runs, oldestSeq, newestSeq, hasMoreNewer, omittedOlder, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("oldestSeq") && oldestSeq == null) values.put("oldestSeq", null);
        if ($explicitNulls.contains("newestSeq") && newestSeq == null) values.put("newestSeq", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionWorkflowListRunsResult> {
        @Override
        public SessionWorkflowListRunsResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionWorkflowListRunsResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("oldestSeq") && tree.get("oldestSeq").isNull()) $explicitNulls.add("oldestSeq");
            if (tree.has("newestSeq") && tree.get("newestSeq").isNull()) $explicitNulls.add("newestSeq");
            return new SessionWorkflowListRunsResult(readProperty(context, tree, "runs", new com.fasterxml.jackson.core.type.TypeReference<List<WorkflowRunSummary>>() {}), readProperty(context, tree, "oldestSeq", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "newestSeq", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "hasMoreNewer", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "omittedOlder", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
