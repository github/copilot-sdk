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
import javax.annotation.processing.Generated;

/**
 * Runtime-to-owner cancellation request for a client-owned task.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TasksCancelParams(
    /** Session that owns the client task */
    @JsonProperty("sessionId") String sessionId,
    /** Canonical runtime-generated task identifier */
    @JsonProperty("id") String id,
    /** Owner-scoped task key included for correlation */
    @JsonProperty("clientTaskId") String clientTaskId,
    /** Opaque identifier shared by coalesced cancellation callers */
    @JsonProperty("cancellationId") String cancellationId,
    /** Reason the runtime requests cancellation */
    @JsonProperty("reason") ClientTaskCancelReason reason
) {
}
