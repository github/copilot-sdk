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
 * What came of a Copilot Free enrollment attempt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AccountSubscribeCopilotFreeResult(
    /** `subscribed` when GitHub answered, and `status` then carries its answer. `no-token` when no credential could be resolved for the identity, which is the caller's cue to ask the user to sign in again rather than to report a failure. `failed` when the attempt itself errored, and `message` then explains it. */
    @JsonProperty("kind") String kind,
    /** GitHub's enrollment status when `kind` is `subscribed`, for example `subscribed` or `already_subscribed`. Absent otherwise. */
    @JsonProperty("status") String status,
    /** Explanation to show the user. Present on `failed`, and optionally alongside a `subscribed` status. */
    @JsonProperty("message") String message
) {
}
