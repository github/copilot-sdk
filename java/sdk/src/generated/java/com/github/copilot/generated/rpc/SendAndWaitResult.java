/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.SessionEvent;
import javax.annotation.processing.Generated;

/**
 * Internal request-correlated completion of one submitted message.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SendAndWaitResult(
    /** Identifier assigned to the submitted user message. */
    @JsonProperty("messageId") String messageId,
    /** The exact existing root assistant event owned by this request. Absent when the request completed without an assistant response. */
    @JsonProperty("response") SessionEvent response,
    /** Whether the request was aborted or removed before completing normally. */
    @JsonProperty("aborted") Boolean aborted,
    /** Existing event at the completion boundary. SDK helpers drain its synchronous subscriber delivery before returning. Absent for quiet queue removal. */
    @JsonProperty("completionEventId") String completionEventId
) {
}
