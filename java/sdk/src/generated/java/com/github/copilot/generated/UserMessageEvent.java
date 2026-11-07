/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "user.message". Payload of `user.message` with displayed and model-transformed content, attachments, source/delivery metadata, mode, and telemetry IDs.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserMessageEvent extends SessionEvent {

    @Override
    public String getType() { return "user.message"; }

    @JsonProperty("data")
    private UserMessageEventData data;

    public UserMessageEventData getData() { return data; }
    public void setData(UserMessageEventData data) { this.data = data; }

    /** Data payload for {@link UserMessageEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserMessageEventData(
        /** IDs of accepted assistant messages that caused this delivered inter-agent prompt, referring to assistant.message.data.messageId across the agent tree. Captured when sending and retained through queued or immediate delivery. An empty list means no model-produced dependency; absence means provenance is unavailable. */
        @JsonProperty("causalDependencies") List<String> causalDependencies,
        /** The user's message text as displayed in the timeline */
        @JsonProperty("content") String content,
        /** Provider reasoning settings anchored before this model-facing message for cache-stable replay; the historical responsesReasoning name is retained for compatibility */
        @JsonProperty("responsesReasoning") ResponsesReasoning responsesReasoning,
        /** Stable identity of the logical user message, matching the ID returned by send and retained by pending queue snapshots */
        @JsonProperty("messageId") String messageId,
        /** Opaque clientTurnId the caller supplied on the send that delivered this message, echoed when the message is consumed, whether it started a turn, drained from the queue or was consumed as steering. Absent when that send supplied none. */
        @JsonProperty("clientTurnId") String clientTurnId,
        /** Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching */
        @JsonProperty("transformedContent") String transformedContent,
        /** Files, selections, or GitHub references attached to the message */
        @JsonProperty("attachments") List<Object> attachments,
        /** Normalized document MIME types that were sent natively instead of through tagged_files XML */
        @JsonProperty("supportedNativeDocumentMimeTypes") List<String> supportedNativeDocumentMimeTypes,
        /** Path-backed native document attachments that stayed on the tagged_files path flow because native upload could not read them or would exceed the request size limit */
        @JsonProperty("nativeDocumentPathFallbackPaths") List<String> nativeDocumentPathFallbackPaths,
        /** Origin of this message, used for timeline filtering and attribution (e.g., `skill-pdf` for hidden skill injection or `agent-<agent-id>` for an inter-agent prompt) */
        @JsonProperty("source") String source,
        /** How this message was delivered to the agentic loop relative to loop state (idle-start vs. steering/queued while busy). The timing axis; combine with `source` (origin) for the full picture. Used for telemetry attribution. */
        @JsonProperty("delivery") UserMessageDelivery delivery,
        /** The agent mode that was active when this message was sent */
        @JsonProperty("agentMode") UserMessageAgentMode agentMode,
        /** True when this user message was auto-injected by autopilot's continuation loop rather than typed by the user; used to distinguish autopilot-driven turns in telemetry. */
        @JsonProperty("isAutopilotContinuation") Boolean isAutopilotContinuation,
        /** CAPI interaction ID for correlating this user message with its turn */
        @JsonProperty("interactionId") String interactionId,
        /** The agent-loop turn ID that consumed this message; absent when no agent-loop turn consumed it */
        @JsonProperty("turnId") String turnId,
        /** Parent agent task ID for background telemetry correlated to this user turn */
        @JsonProperty("parentAgentTaskId") String parentAgentTaskId
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param causalDependencies IDs of accepted assistant messages that caused this delivered inter-agent prompt, referring to assistant.message.data.messageId across the agent tree. Captured when sending and retained through queued or immediate delivery. An empty list means no model-produced dependency; absence means provenance is unavailable.
         * @param content The user's message text as displayed in the timeline
         * @param responsesReasoning Provider reasoning settings anchored before this model-facing message for cache-stable replay; the historical responsesReasoning name is retained for compatibility
         * @param messageId Stable identity of the logical user message, matching the ID returned by send and retained by pending queue snapshots
         * @param transformedContent Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
         * @param attachments Files, selections, or GitHub references attached to the message
         * @param supportedNativeDocumentMimeTypes Normalized document MIME types that were sent natively instead of through tagged_files XML
         * @param nativeDocumentPathFallbackPaths Path-backed native document attachments that stayed on the tagged_files path flow because native upload could not read them or would exceed the request size limit
         * @param source Origin of this message, used for timeline filtering and attribution (e.g., `skill-pdf` for hidden skill injection or `agent-<agent-id>` for an inter-agent prompt)
         * @param delivery How this message was delivered to the agentic loop relative to loop state (idle-start vs. steering/queued while busy). The timing axis; combine with `source` (origin) for the full picture. Used for telemetry attribution.
         * @param agentMode The agent mode that was active when this message was sent
         * @param isAutopilotContinuation True when this user message was auto-injected by autopilot's continuation loop rather than typed by the user; used to distinguish autopilot-driven turns in telemetry.
         * @param interactionId CAPI interaction ID for correlating this user message with its turn
         * @param turnId The agent-loop turn ID that consumed this message; absent when no agent-loop turn consumed it
         * @param parentAgentTaskId Parent agent task ID for background telemetry correlated to this user turn
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public UserMessageEventData(
            List<String> causalDependencies,
            String content,
            ResponsesReasoning responsesReasoning,
            String messageId,
            String transformedContent,
            List<Object> attachments,
            List<String> supportedNativeDocumentMimeTypes,
            List<String> nativeDocumentPathFallbackPaths,
            String source,
            UserMessageDelivery delivery,
            UserMessageAgentMode agentMode,
            Boolean isAutopilotContinuation,
            String interactionId,
            String turnId,
            String parentAgentTaskId
        ) {
            this(causalDependencies, content, responsesReasoning, messageId, null, transformedContent, attachments, supportedNativeDocumentMimeTypes, nativeDocumentPathFallbackPaths, source, delivery, agentMode, isAutopilotContinuation, interactionId, turnId, parentAgentTaskId);
        }

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param content The user's message text as displayed in the timeline
         * @param responsesReasoning Provider reasoning settings anchored before this model-facing message for cache-stable replay; the historical responsesReasoning name is retained for compatibility
         * @param messageId Stable identity of the logical user message, matching the ID returned by send and retained by pending queue snapshots
         * @param transformedContent Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
         * @param attachments Files, selections, or GitHub references attached to the message
         * @param supportedNativeDocumentMimeTypes Normalized document MIME types that were sent natively instead of through tagged_files XML
         * @param nativeDocumentPathFallbackPaths Path-backed native document attachments that stayed on the tagged_files path flow because native upload could not read them or would exceed the request size limit
         * @param source Origin of this message, used for timeline filtering and attribution (e.g., `skill-pdf` for hidden skill injection or `agent-<agent-id>` for an inter-agent prompt)
         * @param delivery How this message was delivered to the agentic loop relative to loop state (idle-start vs. steering/queued while busy). The timing axis; combine with `source` (origin) for the full picture. Used for telemetry attribution.
         * @param agentMode The agent mode that was active when this message was sent
         * @param isAutopilotContinuation True when this user message was auto-injected by autopilot's continuation loop rather than typed by the user; used to distinguish autopilot-driven turns in telemetry.
         * @param interactionId CAPI interaction ID for correlating this user message with its turn
         * @param turnId The agent-loop turn ID that consumed this message; absent when no agent-loop turn consumed it
         * @param parentAgentTaskId Parent agent task ID for background telemetry correlated to this user turn
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public UserMessageEventData(
            String content,
            ResponsesReasoning responsesReasoning,
            String messageId,
            String transformedContent,
            List<Object> attachments,
            List<String> supportedNativeDocumentMimeTypes,
            List<String> nativeDocumentPathFallbackPaths,
            String source,
            UserMessageDelivery delivery,
            UserMessageAgentMode agentMode,
            Boolean isAutopilotContinuation,
            String interactionId,
            String turnId,
            String parentAgentTaskId
        ) {
            this(null, content, responsesReasoning, messageId, null, transformedContent, attachments, supportedNativeDocumentMimeTypes, nativeDocumentPathFallbackPaths, source, delivery, agentMode, isAutopilotContinuation, interactionId, turnId, parentAgentTaskId);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public UserMessageEventData {}

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
            private List<String> causalDependencies;
            private String content;
            private ResponsesReasoning responsesReasoning;
            private String messageId;
            private String clientTurnId;
            private String transformedContent;
            private List<Object> attachments;
            private List<String> supportedNativeDocumentMimeTypes;
            private List<String> nativeDocumentPathFallbackPaths;
            private String source;
            private UserMessageDelivery delivery;
            private UserMessageAgentMode agentMode;
            private Boolean isAutopilotContinuation;
            private String interactionId;
            private String turnId;
            private String parentAgentTaskId;

            /**
             * Sets the {@code causalDependencies} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder causalDependencies(List<String> value) {
                this.causalDependencies = value;
                return this;
            }

            /**
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder content(String value) {
                this.content = value;
                return this;
            }

            /**
             * Sets the {@code responsesReasoning} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder responsesReasoning(ResponsesReasoning value) {
                this.responsesReasoning = value;
                return this;
            }

            /**
             * Sets the {@code messageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder messageId(String value) {
                this.messageId = value;
                return this;
            }

            /**
             * Sets the {@code clientTurnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder clientTurnId(String value) {
                this.clientTurnId = value;
                return this;
            }

            /**
             * Sets the {@code transformedContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder transformedContent(String value) {
                this.transformedContent = value;
                return this;
            }

            /**
             * Sets the {@code attachments} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder attachments(List<Object> value) {
                this.attachments = value;
                return this;
            }

            /**
             * Sets the {@code supportedNativeDocumentMimeTypes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder supportedNativeDocumentMimeTypes(List<String> value) {
                this.supportedNativeDocumentMimeTypes = value;
                return this;
            }

            /**
             * Sets the {@code nativeDocumentPathFallbackPaths} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder nativeDocumentPathFallbackPaths(List<String> value) {
                this.nativeDocumentPathFallbackPaths = value;
                return this;
            }

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(String value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code delivery} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder delivery(UserMessageDelivery value) {
                this.delivery = value;
                return this;
            }

            /**
             * Sets the {@code agentMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentMode(UserMessageAgentMode value) {
                this.agentMode = value;
                return this;
            }

            /**
             * Sets the {@code isAutopilotContinuation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isAutopilotContinuation(Boolean value) {
                this.isAutopilotContinuation = value;
                return this;
            }

            /**
             * Sets the {@code interactionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionId(String value) {
                this.interactionId = value;
                return this;
            }

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

            /**
             * Sets the {@code parentAgentTaskId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentAgentTaskId(String value) {
                this.parentAgentTaskId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public UserMessageEventData build() {
                if (content == null) throw new IllegalStateException("Missing required property: content");
                return new UserMessageEventData(causalDependencies, content, responsesReasoning, messageId, clientTurnId, transformedContent, attachments, supportedNativeDocumentMimeTypes, nativeDocumentPathFallbackPaths, source, delivery, agentMode, isAutopilotContinuation, interactionId, turnId, parentAgentTaskId);
            }
        }
    }
}
