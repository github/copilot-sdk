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
 * Session event "permission.completed". Permission request completion notification signaling UI dismissal
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.completed"; }

    @JsonProperty("data")
    private PermissionCompletedEventData data;

    public PermissionCompletedEventData getData() { return data; }
    public void setData(PermissionCompletedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionCompletedEventData(
        /** Request ID of the resolved permission request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** Optional tool call ID associated with this permission prompt; clients may use it to correlate UI created from tool-scoped prompts */
        @JsonProperty("toolCallId") String toolCallId,
        /** Permission-recovery episode settled by this response, when the request was escalated by Autopilot */
        @JsonProperty("recoveryEpisodeId") String recoveryEpisodeId,
        /** Atomic structured blocked outcome when this permission response ended an Autopilot recovery episode unsuccessfully */
        @JsonProperty("blocker") TaskBlocker blocker,
        /** The result of the permission request */
        @JsonProperty("result") Object result,
        /** Who decided this permission request. Absent on completions recorded before this field existed, which consumers must treat as "not a human decision" rather than assuming one. Authorization records are minted only for `human_response`; an assisted-approval verdict, a host policy, an unattended fallback, and a hook resolution all produce the same `result` a person does, so this is the only field that distinguishes them. */
        @CopilotExperimental
        @JsonProperty("decisionSource") PermissionDecisionSource decisionSource
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionCompletedEventData {}

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
            private String requestId;
            private String toolCallId;
            private String recoveryEpisodeId;
            private TaskBlocker blocker;
            private Object result;
            private PermissionDecisionSource decisionSource;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
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
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Sets the {@code recoveryEpisodeId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder recoveryEpisodeId(String value) {
                this.recoveryEpisodeId = value;
                return this;
            }

            /**
             * Sets the {@code blocker} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder blocker(TaskBlocker value) {
                this.blocker = value;
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
            public PermissionCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (result == null) throw new IllegalStateException("Missing required property: result");
                return new PermissionCompletedEventData(requestId, toolCallId, recoveryEpisodeId, blocker, result, decisionSource);
            }
        }
    }
}
