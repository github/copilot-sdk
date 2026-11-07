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
 * Experimental, observational-only state of one runtime system after an operation transition. Observation and delivery never control runtime work. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleSystemStateData(
    /** Session whose lifecycle owns this system. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque identity for one session registry generation. */
    @JsonProperty("incarnationId") String incarnationId,
    /** Stable subsystem identifier. */
    @JsonProperty("system") String system,
    /** Orders every observed change to this system in the incarnation. */
    @JsonProperty("systemRevision") Long systemRevision,
    /** Authoritative condition retained after this change. Initial shared values are `current`, `disabled`, `degraded`, and `quiescent`; integrations can add bounded values as the contract matures. */
    @JsonProperty("condition") String condition,
    /** Operation transition that produced this system revision. */
    @JsonProperty("changedOperation") RuntimeLifecycleOperation changedOperation,
    /** Complete active operation set after applying the change. */
    @JsonProperty("activeOperations") List<RuntimeLifecycleOperation> activeOperations
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RuntimeLifecycleSystemStateData {}

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
        private String sessionId;
        private String incarnationId;
        private String system;
        private Long systemRevision;
        private String condition;
        private RuntimeLifecycleOperation changedOperation;
        private List<RuntimeLifecycleOperation> activeOperations;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code incarnationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder incarnationId(String value) {
            this.incarnationId = value;
            return this;
        }

        /**
         * Sets the {@code system} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder system(String value) {
            this.system = value;
            return this;
        }

        /**
         * Sets the {@code systemRevision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder systemRevision(Long value) {
            this.systemRevision = value;
            return this;
        }

        /**
         * Sets the {@code condition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder condition(String value) {
            this.condition = value;
            return this;
        }

        /**
         * Sets the {@code changedOperation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder changedOperation(RuntimeLifecycleOperation value) {
            this.changedOperation = value;
            return this;
        }

        /**
         * Sets the {@code activeOperations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activeOperations(List<RuntimeLifecycleOperation> value) {
            this.activeOperations = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RuntimeLifecycleSystemStateData build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (incarnationId == null) throw new IllegalStateException("Missing required property: incarnationId");
            if (system == null) throw new IllegalStateException("Missing required property: system");
            if (systemRevision == null) throw new IllegalStateException("Missing required property: systemRevision");
            if (changedOperation == null) throw new IllegalStateException("Missing required property: changedOperation");
            if (activeOperations == null) throw new IllegalStateException("Missing required property: activeOperations");
            return new RuntimeLifecycleSystemStateData(sessionId, incarnationId, system, systemRevision, condition, changedOperation, activeOperations);
        }
    }
}
