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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Parameters for starting fleet orchestration: an optional user prompt combined with the fleet instructions, plus the send options forwarded to the resulting turn.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFleetStartParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Optional user prompt to combine with fleet instructions */
    @JsonProperty("prompt") String prompt,
    /** Optional attachments (files, directories, selections, blobs, GitHub references) to include with the fleet request */
    @JsonProperty("attachments") List<Object> attachments,
    /** If false, this request will not trigger a Premium Request Unit charge. User requests default to billable. */
    @JsonProperty("billable") Boolean billable,
    /** If true, await completion of the agentic loop for this fleet request before returning. Defaults to false. */
    @JsonProperty("wait") Boolean wait_
) {
}
