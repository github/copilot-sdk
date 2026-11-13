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
 * The managed remote-control policy in force for an account, if any. An absent policy means the account is unmanaged for this capability, which a host reads as allowed — so absence here is a positive statement that no policy applies, never a stand-in for a resolution that failed. A resolution that cannot be completed is rejected instead.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsLoadRemoteControlResult(
    /** The account's `remoteControl` policy document. Absent when no managed policy governs remote control for this account. Reported as an explicit disabled policy, rather than as absence, when the service refused to deliver a policy this client is too old to enforce. */
    @JsonProperty("remoteControl") Object remoteControl
) {
}
