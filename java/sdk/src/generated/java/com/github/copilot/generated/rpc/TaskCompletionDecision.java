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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TaskCompletionDecision.$NullableDeserializer.class)
public record TaskCompletionDecision(
    /** Semantic result of evaluating the task completion request. */
    @JsonProperty("outcome") TaskCompletionOutcome outcome,
    /** Rationale for the completion decision, when one is available. */
    @JsonProperty("reason") String reason,
    /** Whether the rationale was derived from completion-reviewer output. */
    @JsonProperty("reviewerDerived") Boolean reviewerDerived,
    /** Information-flow metadata captured from the completion reviewer. */
    @JsonProperty("reviewerResultMeta") Object reviewerResultMeta,
    /** Active autopilot objective evaluated by the completion reviewer. */
    @JsonProperty("objectiveId") Long objectiveId,
    /** Whether completion was accepted after the reviewer-rejection budget was exhausted. */
    @JsonProperty("completionRejectionBudgetExhausted") Boolean completionRejectionBudgetExhausted,
    /** Objective eligibility token captured when the decision was evaluated. */
    @JsonProperty("completionEligibilityToken") Long completionEligibilityToken,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskCompletionDecision { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskCompletionDecision(TaskCompletionOutcome outcome, String reason, Boolean reviewerDerived, Object reviewerResultMeta, Long objectiveId, Boolean completionRejectionBudgetExhausted, Long completionEligibilityToken) {
        this(outcome, reason, reviewerDerived, reviewerResultMeta, objectiveId, completionRejectionBudgetExhausted, completionEligibilityToken, java.util.Set.of());
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
        private TaskCompletionOutcome outcome;
        private String reason;
        private Boolean reviewerDerived;
        private Object reviewerResultMeta;
        private boolean $presence3;
        private Long objectiveId;
        private Boolean completionRejectionBudgetExhausted;
        private Long completionEligibilityToken;

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcome(TaskCompletionOutcome value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code reviewerDerived} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reviewerDerived(Boolean value) {
            this.reviewerDerived = value;
            return this;
        }

        /**
         * Sets the {@code reviewerResultMeta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reviewerResultMeta(Object value) {
            this.reviewerResultMeta = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code objectiveId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder objectiveId(Long value) {
            this.objectiveId = value;
            return this;
        }

        /**
         * Sets the {@code completionRejectionBudgetExhausted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completionRejectionBudgetExhausted(Boolean value) {
            this.completionRejectionBudgetExhausted = value;
            return this;
        }

        /**
         * Sets the {@code completionEligibilityToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completionEligibilityToken(Long value) {
            this.completionEligibilityToken = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskCompletionDecision build() {
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && reviewerResultMeta == null) $explicitNulls.add("reviewerResultMeta");
            return new TaskCompletionDecision(outcome, reason, reviewerDerived, reviewerResultMeta, objectiveId, completionRejectionBudgetExhausted, completionEligibilityToken, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("reviewerResultMeta") && reviewerResultMeta == null) values.put("reviewerResultMeta", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<TaskCompletionDecision> {
        @Override
        public TaskCompletionDecision deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(TaskCompletionDecision.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("reviewerResultMeta") && tree.get("reviewerResultMeta").isNull()) $explicitNulls.add("reviewerResultMeta");
            return new TaskCompletionDecision(readProperty(context, tree, "outcome", new com.fasterxml.jackson.core.type.TypeReference<TaskCompletionOutcome>() {}), readProperty(context, tree, "reason", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reviewerDerived", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "reviewerResultMeta", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "objectiveId", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "completionRejectionBudgetExhausted", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "completionEligibilityToken", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
