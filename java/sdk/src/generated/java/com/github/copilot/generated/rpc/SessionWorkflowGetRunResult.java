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
 * Complete current or terminal workflow run envelope.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionWorkflowGetRunResult.$NullableDeserializer.class)
public record SessionWorkflowGetRunResult(
    /** Workflow run identifier. */
    @JsonProperty("runId") String runId,
    /** One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime. */
    @JsonProperty("attempt") Long attempt,
    /** Current or terminal workflow run status. */
    @JsonProperty("status") WorkflowRunStatus status,
    /** Completed workflow result. */
    @JsonProperty("result") Object result,
    /** Error message for an errored run. */
    @JsonProperty("error") String error,
    /** Machine-readable failure details for a halted or errored run. */
    @JsonProperty("failure") Object failure,
    /** Reason for a halted or cancelled run. */
    @JsonProperty("reason") String reason,
    /** Partial journal and progress snapshot for a halted, cancelled, or errored run. */
    @JsonProperty("snapshot") Object snapshot,
    /** Structured pause initiator metadata for a paused attempt. */
    @JsonProperty("pauseInfo") Object pauseInfo,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkflowGetRunResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkflowGetRunResult(String runId, Long attempt, WorkflowRunStatus status, Object result, String error, Object failure, String reason, Object snapshot, Object pauseInfo) {
        this(runId, attempt, status, result, error, failure, reason, snapshot, pauseInfo, java.util.Set.of());
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
        private String runId;
        private Long attempt;
        private WorkflowRunStatus status;
        private Object result;
        private boolean $presence3;
        private String error;
        private Object failure;
        private String reason;
        private Object snapshot;
        private boolean $presence7;
        private Object pauseInfo;

        /**
         * Sets the {@code runId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runId(String value) {
            this.runId = value;
            return this;
        }

        /**
         * Sets the {@code attempt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attempt(Long value) {
            this.attempt = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(WorkflowRunStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code result} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder result(Object value) {
            this.result = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Sets the {@code failure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder failure(Object value) {
            this.failure = value;
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
         * Sets the {@code snapshot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder snapshot(Object value) {
            this.snapshot = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code pauseInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pauseInfo(Object value) {
            this.pauseInfo = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkflowGetRunResult build() {
            if (runId == null) throw new IllegalStateException("Missing required property: runId");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && result == null) $explicitNulls.add("result");
            if ($presence7 && snapshot == null) $explicitNulls.add("snapshot");
            return new SessionWorkflowGetRunResult(runId, attempt, status, result, error, failure, reason, snapshot, pauseInfo, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("result") && result == null) values.put("result", null);
        if ($explicitNulls.contains("snapshot") && snapshot == null) values.put("snapshot", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionWorkflowGetRunResult> {
        @Override
        public SessionWorkflowGetRunResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionWorkflowGetRunResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("result") && tree.get("result").isNull()) $explicitNulls.add("result");
            if (tree.has("snapshot") && tree.get("snapshot").isNull()) $explicitNulls.add("snapshot");
            return new SessionWorkflowGetRunResult(readProperty(context, tree, "runId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "attempt", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<WorkflowRunStatus>() {}), readProperty(context, tree, "result", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "failure", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "reason", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "snapshot", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "pauseInfo", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
