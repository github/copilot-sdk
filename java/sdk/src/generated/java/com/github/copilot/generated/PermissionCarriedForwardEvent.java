/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.carriedForward". Historical decode-only receipt from the retired Assisted Permissions authorization extractor. Current runtimes ignore it for permission decisions.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionCarriedForwardEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.carriedForward"; }

    @JsonProperty("data")
    private PermissionCarriedForwardEventData data;

    public PermissionCarriedForwardEventData getData() { return data; }
    public void setData(PermissionCarriedForwardEventData data) { this.data = data; }

    /** Data payload for {@link PermissionCarriedForwardEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionCarriedForwardEventData(
        /** Authorization edge minted for this admission. Not a prompt id: no prompt was raised, so no client should expect a request with this id. */
        @CopilotExperimental
        @JsonProperty("requestId") String requestId,
        /** Tool call this admission authorizes. Its execution receipts the prior grant, which is how a single-effect approval is spent rather than carried forward again. */
        @CopilotExperimental
        @JsonProperty("toolCallId") String toolCallId,
        /** Identity of the prior authorization record that contained the proposal. */
        @CopilotExperimental
        @JsonProperty("recordId") String recordId,
        /** Always `authorization_carry_forward`. Stated explicitly so a consumer reading this event cannot mistake it for a human, host-policy, or assisted-approval decision. */
        @CopilotExperimental
        @JsonProperty("decisionSource") PermissionDecisionSource decisionSource
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionCarriedForwardEventData {}

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
            private String requestId;
            private String toolCallId;
            private String recordId;
            private PermissionDecisionSource decisionSource;

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
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Sets the {@code recordId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder recordId(String value) {
                this.recordId = value;
                return this;
            }

            /**
             * Sets the {@code decisionSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder decisionSource(PermissionDecisionSource value) {
                this.decisionSource = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PermissionCarriedForwardEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (recordId == null) throw new IllegalStateException("Missing required property: recordId");
                if (decisionSource == null) throw new IllegalStateException("Missing required property: decisionSource");
                return new PermissionCarriedForwardEventData(requestId, toolCallId, recordId, decisionSource);
            }
        }
    }
}
