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
 * Host report that its execution of a queued command started or stopped waiting for runtime state. The report is observational and never changes command execution.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionCommandsReportQueuedCommandWaitParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Request ID from the `command.queued` event the host is executing. */
    @JsonProperty("requestId") String requestId,
    /** Stable identity of the account whose managed policy the host awaits. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("accountIdentity") String accountIdentity,
    /** Runtime state the host waits for. */
    @JsonProperty("requirement") QueuedCommandWaitRequirement requirement,
    /** True when the wait starts and false when it ends. */
    @JsonProperty("waiting") Boolean waiting
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionCommandsReportQueuedCommandWaitParams {}

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
        private String requestId;
        private String accountIdentity;
        private boolean $presence2;
        private QueuedCommandWaitRequirement requirement;
        private Boolean waiting;

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
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(String value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code accountIdentity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accountIdentity(String value) {
            this.accountIdentity = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code requirement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requirement(QueuedCommandWaitRequirement value) {
            this.requirement = value;
            return this;
        }

        /**
         * Sets the {@code waiting} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder waiting(Boolean value) {
            this.waiting = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionCommandsReportQueuedCommandWaitParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (!$presence2) throw new IllegalStateException("Missing required property: accountIdentity");
            if (requirement == null) throw new IllegalStateException("Missing required property: requirement");
            if (waiting == null) throw new IllegalStateException("Missing required property: waiting");
            return new SessionCommandsReportQueuedCommandWaitParams(sessionId, requestId, accountIdentity, requirement, waiting);
        }
    }
}
