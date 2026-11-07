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
 * Registers or reclaims a client-owned task.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksRegisterParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Task kind */
    @JsonProperty("type") TaskClientType type,
    /** Owner-scoped idempotency key used for registration and reclaim */
    @JsonProperty("clientTaskId") String clientTaskId,
    /** Human-readable description of the external work */
    @JsonProperty("description") String description,
    /** Optional short display name for the external work */
    @JsonProperty("displayName") String displayName,
    /** Whether the owner supports runtime cancellation requests */
    @JsonProperty("cancellable") Boolean cancellable,
    /** Expected current sequence for idempotent registration or orphan reclaim */
    @JsonProperty("expectedSequence") Long expectedSequence
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionTasksRegisterParams {}

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
        private TaskClientType type;
        private String clientTaskId;
        private String description;
        private String displayName;
        private Boolean cancellable;
        private Long expectedSequence;

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
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder type(TaskClientType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code clientTaskId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientTaskId(String value) {
            this.clientTaskId = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code cancellable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cancellable(Boolean value) {
            this.cancellable = value;
            return this;
        }

        /**
         * Sets the {@code expectedSequence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expectedSequence(Long value) {
            this.expectedSequence = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionTasksRegisterParams build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (clientTaskId == null) throw new IllegalStateException("Missing required property: clientTaskId");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (cancellable == null) throw new IllegalStateException("Missing required property: cancellable");
            return new SessionTasksRegisterParams(sessionId, type, clientTaskId, description, displayName, cancellable, expectedSequence);
        }
    }
}
