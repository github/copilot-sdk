/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Identifier of the target agent task, message content, and optional sender agent ID.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksSendMessageParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Agent task identifier */
    @JsonProperty("id") String id,
    /** Message content to send to the agent */
    @JsonProperty("message") String message,
    /** Agent ID of the sender, if sent on behalf of another agent */
    @JsonProperty("fromAgentId") String fromAgentId
) {
}
