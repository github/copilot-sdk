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
         * Creates event data with the components it had before later optional fields were added.
         */
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
    }
}
