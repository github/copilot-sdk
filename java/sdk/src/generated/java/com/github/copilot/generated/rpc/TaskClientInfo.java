/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Tracked client-owned task metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TaskClientInfo.$NullableDeserializer.class)
public record TaskClientInfo(
    /** Task kind */
    @JsonProperty("type") TaskClientType type,
    /** Canonical runtime-generated task identifier */
    @JsonProperty("id") String id,
    /** Owner-scoped registration and reclaim key */
    @JsonProperty("clientTaskId") String clientTaskId,
    /** Optional task display name */
    @JsonProperty("displayName") String displayName,
    /** Task description */
    @JsonProperty("description") String description,
    /** Client task lifecycle status */
    @JsonProperty("status") TaskClientStatus status,
    /** Public attribution and presence for the task owner */
    @JsonProperty("owner") TaskClientOwner owner,
    /** ISO 8601 timestamp when the task started */
    @JsonProperty("startedAt") OffsetDateTime startedAt,
    /** ISO 8601 timestamp of the latest accepted lifecycle change */
    @JsonProperty("updatedAt") OffsetDateTime updatedAt,
    /** ISO 8601 timestamp when the task reached a terminal status */
    @JsonProperty("completedAt") OffsetDateTime completedAt,
    /** Accumulated active execution time in milliseconds */
    @JsonProperty("activeTimeMs") Long activeTimeMs,
    /** ISO 8601 timestamp when the current active segment started */
    @JsonProperty("activeStartedAt") OffsetDateTime activeStartedAt,
    /** ISO 8601 timestamp when the connected owner entered idle status */
    @JsonProperty("idleSince") OffsetDateTime idleSince,
    /** ISO 8601 timestamp of the most recent orphan transition */
    @JsonProperty("orphanedAt") OffsetDateTime orphanedAt,
    /** ISO 8601 timestamp of the most recent successful reclaim */
    @JsonProperty("reclaimedAt") OffsetDateTime reclaimedAt,
    /** Execution mode, which is always background for client-owned tasks */
    @JsonProperty("executionMode") TaskClientExecutionMode executionMode,
    /** Whether the currently bound owner can receive a cancellation request */
    @JsonProperty("canCancel") Boolean canCancel,
    /** Sequence number of the latest accepted owner update */
    @JsonProperty("sequence") Long sequence,
    /** Opaque successful terminal result supplied by the task owner */
    @JsonProperty("result") Object result,
    /** Human-readable terminal failure message */
    @JsonProperty("error") String error,
    /** Optional owner-supplied terminal failure code */
    @JsonProperty("errorCode") String errorCode,
    /** Human-readable reason for terminal cancellation */
    @JsonProperty("cancellationReason") String cancellationReason,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskClientInfo { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskClientInfo(TaskClientType type, String id, String clientTaskId, String displayName, String description, TaskClientStatus status, TaskClientOwner owner, OffsetDateTime startedAt, OffsetDateTime updatedAt, OffsetDateTime completedAt, Long activeTimeMs, OffsetDateTime activeStartedAt, OffsetDateTime idleSince, OffsetDateTime orphanedAt, OffsetDateTime reclaimedAt, TaskClientExecutionMode executionMode, Boolean canCancel, Long sequence, Object result, String error, String errorCode, String cancellationReason) {
        this(type, id, clientTaskId, displayName, description, status, owner, startedAt, updatedAt, completedAt, activeTimeMs, activeStartedAt, idleSince, orphanedAt, reclaimedAt, executionMode, canCancel, sequence, result, error, errorCode, cancellationReason, java.util.Set.of());
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
        private TaskClientType type;
        private String id;
        private String clientTaskId;
        private String displayName;
        private String description;
        private TaskClientStatus status;
        private TaskClientOwner owner;
        private OffsetDateTime startedAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime completedAt;
        private Long activeTimeMs;
        private OffsetDateTime activeStartedAt;
        private OffsetDateTime idleSince;
        private OffsetDateTime orphanedAt;
        private OffsetDateTime reclaimedAt;
        private TaskClientExecutionMode executionMode;
        private Boolean canCancel;
        private Long sequence;
        private Object result;
        private boolean $presence18;
        private String error;
        private String errorCode;
        private String cancellationReason;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(TaskClientType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code clientTaskId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientTaskId(String value) {
            this.clientTaskId = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
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
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(TaskClientStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code owner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder owner(TaskClientOwner value) {
            this.owner = value;
            return this;
        }

        /**
         * Sets the {@code startedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startedAt(OffsetDateTime value) {
            this.startedAt = value;
            return this;
        }

        /**
         * Sets the {@code updatedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder updatedAt(OffsetDateTime value) {
            this.updatedAt = value;
            return this;
        }

        /**
         * Sets the {@code completedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completedAt(OffsetDateTime value) {
            this.completedAt = value;
            return this;
        }

        /**
         * Sets the {@code activeTimeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder activeTimeMs(Long value) {
            this.activeTimeMs = value;
            return this;
        }

        /**
         * Sets the {@code activeStartedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder activeStartedAt(OffsetDateTime value) {
            this.activeStartedAt = value;
            return this;
        }

        /**
         * Sets the {@code idleSince} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder idleSince(OffsetDateTime value) {
            this.idleSince = value;
            return this;
        }

        /**
         * Sets the {@code orphanedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder orphanedAt(OffsetDateTime value) {
            this.orphanedAt = value;
            return this;
        }

        /**
         * Sets the {@code reclaimedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reclaimedAt(OffsetDateTime value) {
            this.reclaimedAt = value;
            return this;
        }

        /**
         * Sets the {@code executionMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder executionMode(TaskClientExecutionMode value) {
            this.executionMode = value;
            return this;
        }

        /**
         * Sets the {@code canCancel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canCancel(Boolean value) {
            this.canCancel = value;
            return this;
        }

        /**
         * Sets the {@code sequence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sequence(Long value) {
            this.sequence = value;
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
            this.$presence18 = true;
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
         * Sets the {@code errorCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder errorCode(String value) {
            this.errorCode = value;
            return this;
        }

        /**
         * Sets the {@code cancellationReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cancellationReason(String value) {
            this.cancellationReason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskClientInfo build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (clientTaskId == null) throw new IllegalStateException("Missing required property: clientTaskId");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (owner == null) throw new IllegalStateException("Missing required property: owner");
            if (startedAt == null) throw new IllegalStateException("Missing required property: startedAt");
            if (updatedAt == null) throw new IllegalStateException("Missing required property: updatedAt");
            if (activeTimeMs == null) throw new IllegalStateException("Missing required property: activeTimeMs");
            if (executionMode == null) throw new IllegalStateException("Missing required property: executionMode");
            if (canCancel == null) throw new IllegalStateException("Missing required property: canCancel");
            if (sequence == null) throw new IllegalStateException("Missing required property: sequence");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence18 && result == null) $explicitNulls.add("result");
            return new TaskClientInfo(type, id, clientTaskId, displayName, description, status, owner, startedAt, updatedAt, completedAt, activeTimeMs, activeStartedAt, idleSince, orphanedAt, reclaimedAt, executionMode, canCancel, sequence, result, error, errorCode, cancellationReason, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("result") && result == null) values.put("result", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<TaskClientInfo> {
        @Override
        public TaskClientInfo deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(TaskClientInfo.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("result") && tree.get("result").isNull()) $explicitNulls.add("result");
            return new TaskClientInfo(readProperty(context, tree, "type", new com.fasterxml.jackson.core.type.TypeReference<TaskClientType>() {}), readProperty(context, tree, "id", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "clientTaskId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "displayName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "description", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<TaskClientStatus>() {}), readProperty(context, tree, "owner", new com.fasterxml.jackson.core.type.TypeReference<TaskClientOwner>() {}), readProperty(context, tree, "startedAt", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "updatedAt", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "completedAt", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "activeTimeMs", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "activeStartedAt", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "idleSince", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "orphanedAt", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "reclaimedAt", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "executionMode", new com.fasterxml.jackson.core.type.TypeReference<TaskClientExecutionMode>() {}), readProperty(context, tree, "canCancel", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sequence", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "result", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "errorCode", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "cancellationReason", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
