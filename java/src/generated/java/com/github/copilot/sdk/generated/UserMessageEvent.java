/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code user.message} session event.
 *
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
        /** The user's message text as displayed in the timeline */
        @JsonProperty("content") String content,
        /** Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching */
        @JsonProperty("transformedContent") String transformedContent,
        /** Files, selections, or GitHub references attached to the message */
        @JsonProperty("attachments") List<Object> attachments,
        /** Normalized document MIME types that were sent natively instead of through tagged_files XML */
        @JsonProperty("supportedNativeDocumentMimeTypes") List<String> supportedNativeDocumentMimeTypes,
        /** Path-backed native document attachments that stayed on the tagged_files path flow because native upload would exceed the request size limit */
        @JsonProperty("nativeDocumentPathFallbackPaths") List<String> nativeDocumentPathFallbackPaths,
        /** Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user) */
        @JsonProperty("source") String source,
        /** The agent mode that was active when this message was sent */
        @JsonProperty("agentMode") UserMessageAgentMode agentMode,
        /** CAPI interaction ID for correlating this user message with its turn */
        @JsonProperty("interactionId") String interactionId
    ) {
    }
}
