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
 * The managed layers to retain as this process's startup policy, with the outcome of fetching each one.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsRetainPolicyParams(
    /** The resolved server policy. A known source can lack a response after a forced-refresh failure. */
    @JsonProperty("serverResponse") Object serverResponse,
    /** The device/MDM policy layer. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Sandbox floor declared by the device layer, carried separately so a document that failed validation can still raise the floor. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** Whether a device policy source demonstrably exists. This, not a read error, is the evidence a device is managed, so a fail-closed policy gates on it. */
    @JsonProperty("deviceSourceFound") Boolean deviceSourceFound,
    /** Whether any managed policy source exists, independently of fetch failures. */
    @JsonProperty("managedPolicyPresent") Boolean managedPolicyPresent,
    /** Whether the server policy source exists, even when its response is unavailable. */
    @JsonProperty("serverSourceFound") Boolean serverSourceFound,
    /** Whether a required fresh server-policy confirmation failed. */
    @JsonProperty("serverForcedRefreshFailure") Boolean serverForcedRefreshFailure,
    /** Preserve the retained server layer during a device-only resolution. */
    @JsonProperty("preserveServerState") Boolean preserveServerState,
    /** Whether the device sandbox intent is unreadable independently of its other settings. */
    @JsonProperty("deviceSandboxUndetermined") Boolean deviceSandboxUndetermined,
    /** Whether the shared sandbox policy is undetermined. Diagnostic errors alone do not imply this flag. */
    @JsonProperty("sandboxFloorUndetermined") Boolean sandboxFloorUndetermined,
    /** Whether permission policy is undetermined. Sandbox-only uncertainty does not imply this flag. */
    @JsonProperty("permissionPolicyUndetermined") Boolean permissionPolicyUndetermined,
    /** Whether uncertainty alone enabled the shared sandbox floor. */
    @JsonProperty("sandboxEnabledByUndeterminedPolicy") Boolean sandboxEnabledByUndeterminedPolicy,
    /** Shared server/device sandbox floor, excluding session-local helper policy. */
    @JsonProperty("sharedSandboxFloor") Object sharedSandboxFloor,
    /** Whether the server fetch failed. Report it honestly: true keeps the already-retained server layer rather than replacing it with an absent one. */
    @JsonProperty("serverFetchFailed") Boolean serverFetchFailed,
    /** Whether the device load failed. Report it honestly, for the same reason as `serverFetchFailed`. */
    @JsonProperty("deviceLoadFailed") Boolean deviceLoadFailed,
    /** The policy helper's response: the third managed layer that a process owning its startup policy resolves. It is retained with the other two so that every ambient read in this process, including the mutation guards and the governed marketplace allowlist that the `plugins.*` handlers derive, sees the whole policy the process fetched. A session-owning process omits it, because it resolves a helper per session against that session's working directory. */
    @JsonProperty("policyHelperResponse") Object policyHelperResponse
) {
}
