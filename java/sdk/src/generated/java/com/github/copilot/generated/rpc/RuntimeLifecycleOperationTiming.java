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
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Fixed timestamps for one observed operation revision.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleOperationTiming(
    /** ISO 8601 timestamp when the operation began. */
    @JsonProperty("startedAt") OffsetDateTime startedAt,
    /** ISO 8601 timestamp when the current phase began. */
    @JsonProperty("phaseStartedAt") OffsetDateTime phaseStartedAt,
    /** ISO 8601 timestamp when this operation revision was recorded. */
    @JsonProperty("transitionedAt") OffsetDateTime transitionedAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    RuntimeLifecycleOperationTiming {}

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
        private OffsetDateTime startedAt;
        private OffsetDateTime phaseStartedAt;
        private OffsetDateTime transitionedAt;

        /**
         * Sets the {@code startedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder startedAt(OffsetDateTime value) {
            this.startedAt = value;
            return this;
        }

        /**
         * Sets the {@code phaseStartedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phaseStartedAt(OffsetDateTime value) {
            this.phaseStartedAt = value;
            return this;
        }

        /**
         * Sets the {@code transitionedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder transitionedAt(OffsetDateTime value) {
            this.transitionedAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RuntimeLifecycleOperationTiming build() {
            if (startedAt == null) throw new IllegalStateException("Missing required property: startedAt");
            if (phaseStartedAt == null) throw new IllegalStateException("Missing required property: phaseStartedAt");
            if (transitionedAt == null) throw new IllegalStateException("Missing required property: transitionedAt");
            return new RuntimeLifecycleOperationTiming(startedAt, phaseStartedAt, transitionedAt);
        }
    }
}
