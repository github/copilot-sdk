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
 * The HTTP outcome of reading a cloud task, rather than the task alone. A 404 arrives here as a non-ok result with `statusCode` 404, not as an error, so a caller can tell an absent task apart from a failed call.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksGetResult(
    /** Whether the API returned the task successfully. */
    @JsonProperty("ok") Boolean ok,
    /** The task record, as returned by the API. Present only when `ok` is true; a caller parses it itself because the runtime does not narrow the API's own shape. */
    @JsonProperty("payloadJson") String payloadJson,
    /** HTTP status code of the response, when one was received. */
    @JsonProperty("statusCode") Long statusCode,
    /** Human-readable failure explanation, when the call failed. */
    @JsonProperty("errorMessage") String errorMessage,
    /** Categorised failure kind, when the call failed. */
    @JsonProperty("errorKind") String errorKind,
    /** The error body the API returned, preserved verbatim so a host can surface the service's own message. */
    @JsonProperty("errorPayloadJson") String errorPayloadJson
) {
}
