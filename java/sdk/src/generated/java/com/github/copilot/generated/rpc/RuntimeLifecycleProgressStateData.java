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
 * Experimental, observational-only state of runtime progress and the lifecycle operations that block it. Observation and delivery never control runtime or model behavior. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleProgressStateData(
    /** Session whose lifecycle owns this progress. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque identity for one session registry generation. */
    @JsonProperty("incarnationId") String incarnationId,
    /** Progress-owner-defined kind. */
    @JsonProperty("kind") String kind,
    /** Opaque progress identifier, unique within the incarnation and kind. */
    @JsonProperty("progressId") String progressId,
    /** Agent that owns this progress. Absent for the primary agent and progress without an agent owner. */
    @JsonProperty("agentId") String agentId,
    /** Orders changes to this progress record. */
    @JsonProperty("revision") Long revision,
    /** Progress-owner-defined phase. */
    @JsonProperty("phase") String phase,
    /** How the progress record ended. Absent while it remains active. */
    @JsonProperty("disposition") RuntimeLifecycleDisposition disposition,
    /** Immutable causal origin, when another lifecycle progress record caused this one. */
    @JsonProperty("origin") RuntimeLifecycleProgressOrigin origin,
    /** Timestamps recorded by the lifecycle service for this progress revision. */
    @JsonProperty("timing") RuntimeLifecycleProgressTiming timing,
    /** Complete dependency blocker set after this progress change. */
    @JsonProperty("blockers") List<RuntimeLifecycleBlocker> blockers
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RuntimeLifecycleProgressStateData {}

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
        private String kind;
        private String progressId;
        private String agentId;
        private Long revision;
        private String phase;
        private RuntimeLifecycleDisposition disposition;
        private RuntimeLifecycleProgressOrigin origin;
        private RuntimeLifecycleProgressTiming timing;
        private List<RuntimeLifecycleBlocker> blockers;

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
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code progressId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder progressId(String value) {
            this.progressId = value;
            return this;
        }

        /**
         * Sets the {@code agentId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder agentId(String value) {
            this.agentId = value;
            return this;
        }

        /**
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder revision(Long value) {
            this.revision = value;
            return this;
        }

        /**
         * Sets the {@code phase} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phase(String value) {
            this.phase = value;
            return this;
        }

        /**
         * Sets the {@code disposition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disposition(RuntimeLifecycleDisposition value) {
            this.disposition = value;
            return this;
        }

        /**
         * Sets the {@code origin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder origin(RuntimeLifecycleProgressOrigin value) {
            this.origin = value;
            return this;
        }

        /**
         * Sets the {@code timing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timing(RuntimeLifecycleProgressTiming value) {
            this.timing = value;
            return this;
        }

        /**
         * Sets the {@code blockers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder blockers(List<RuntimeLifecycleBlocker> value) {
            this.blockers = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RuntimeLifecycleProgressStateData build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (incarnationId == null) throw new IllegalStateException("Missing required property: incarnationId");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (progressId == null) throw new IllegalStateException("Missing required property: progressId");
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            if (phase == null) throw new IllegalStateException("Missing required property: phase");
            if (timing == null) throw new IllegalStateException("Missing required property: timing");
            if (blockers == null) throw new IllegalStateException("Missing required property: blockers");
            return new RuntimeLifecycleProgressStateData(sessionId, incarnationId, kind, progressId, agentId, revision, phase, disposition, origin, timing, blockers);
        }
    }
}
