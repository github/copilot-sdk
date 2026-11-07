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
 * Runtime-to-owner cancellation request for a client-owned task.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TasksCancelParams(
    /** Session that owns the client task */
    @JsonProperty("sessionId") String sessionId,
    /** Canonical runtime-generated task identifier */
    @JsonProperty("id") String id,
    /** Owner-scoped task key included for correlation */
    @JsonProperty("clientTaskId") String clientTaskId,
    /** Opaque identifier shared by coalesced cancellation callers */
    @JsonProperty("cancellationId") String cancellationId,
    /** Reason the runtime requests cancellation */
    @JsonProperty("reason") ClientTaskCancelReason reason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TasksCancelParams {}

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
        private String id;
        private String clientTaskId;
        private String cancellationId;
        private ClientTaskCancelReason reason;

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
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder clientTaskId(String value) {
            this.clientTaskId = value;
            return this;
        }

        /**
         * Sets the {@code cancellationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cancellationId(String value) {
            this.cancellationId = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(ClientTaskCancelReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TasksCancelParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (clientTaskId == null) throw new IllegalStateException("Missing required property: clientTaskId");
            if (cancellationId == null) throw new IllegalStateException("Missing required property: cancellationId");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            return new TasksCancelParams(sessionId, id, clientTaskId, cancellationId, reason);
        }
    }
}
