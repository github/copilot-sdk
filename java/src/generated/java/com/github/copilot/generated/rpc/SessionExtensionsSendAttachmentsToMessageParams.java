/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Parameters for session.extensions.sendAttachmentsToMessage.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionExtensionsSendAttachmentsToMessageParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Optional canvas instance binding the push for provenance. When supplied, the runtime resolves the canvas, verifies it is owned by the calling extension, and stamps canvasId/instanceId onto each extension_context entry. When omitted, no resolution runs and those fields stay unset on the attachment. */
    @JsonProperty("instanceId") String instanceId,
    /** Attachments to push into the next user-message turn. extension_context entries take the slim shape; standard variants take their full SendAttachment shape. */
    @JsonProperty("attachments") List<Object> attachments
) {
}
