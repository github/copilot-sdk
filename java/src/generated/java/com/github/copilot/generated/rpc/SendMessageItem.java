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
 * A single user message to append to the session as part of a `session.sendMessages` turn
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendMessageItem(
    /** The user message text */
    @JsonProperty("prompt") String prompt,
    /** If provided, this is shown in the timeline instead of `prompt` */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** Optional attachments (files, directories, selections, blobs, GitHub references) to include with this message */
    @JsonProperty("attachments") List<Object> attachments,
    /** If false, this message will not trigger a Premium Request Unit charge. User messages default to billable. */
    @JsonProperty("billable") Boolean billable,
    /** If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange */
    @JsonProperty("requiredTool") String requiredTool,
    /** Optional provenance tag copied to the resulting user.message event. Must match one of three forms: the literal `system`, `command-<command-id>` for messages originating from a command (e.g. slash command, Mission Control command), or `schedule-<numeric-id>` for messages originating from a scheduled job. */
    @JsonProperty("source") String source
) {
}
