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
 * The sharing status a cloud task carries after the call. Read `credentialResolved` first: when it is false no request was attempted at all, which is a different outcome from a request the service declined.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksSharingStatusResult(
    /** Whether a credential could be resolved for the supplied `authInfo` at all. When false no request was attempted and every other field is absent, which a caller must not report as a transport failure: the account simply has no usable credential. */
    @JsonProperty("credentialResolved") Boolean credentialResolved,
    /** Whether the service returned the task successfully. Meaningful only when `credentialResolved` is true. */
    @JsonProperty("ok") Boolean ok,
    /** The task's sharing status, as the service reported it. Absent when the call did not succeed, and also when the service returned a task that carries no sharing status at all. It is a free-form string rather than a closed set because the service owns these values; a caller that only understands some of them should treat an unrecognized one as unknown rather than as an error. */
    @JsonProperty("sharingStatus") String sharingStatus
) {
}
