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
 * Inputs describing the host and account a startup policy resolution should run for. Mirrors `managedSettings.ingest`, and adds the decision of whether the resolved policy is retained for this runtime process.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveStartupPolicyParams(
    /** The policy-helper invocation reason. Defaults to sessionStart. */
    @JsonProperty("policyHelperReason") ManagedSettingsPolicyHelperReason policyHelperReason,
    /** Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy. */
    @JsonProperty("selfFetch") Boolean selfFetch,
    /** The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched and the sign-in pin is evaluated against no credential. */
    @JsonProperty("authInfo") Object authInfo,
    /** The session whose credential store, working directory and client kind the resolution should use. Omit it when no session exists yet and the process-wide store applies. A resolution whose session names a credential other than `authInfo` is returned but not retained, so a stale sign-in cannot apply policy for an account that has already been switched away from. */
    @JsonProperty("sessionId") String sessionId,
    /** A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine. */
    @JsonProperty("hasDeviceResponse") Boolean hasDeviceResponse,
    /** Whether to ignore a cached server policy when reading, forcing the resolution to come from a live fetch. */
    @JsonProperty("bypassCacheRead") Boolean bypassCacheRead,
    /** How this resolution may use the on-disk server-policy cache. Defaults to the runtime's own policy when omitted. Rejected when combined with `bypassCacheRead`, because the two express the same decision and accepting both would leave the caller unsure which one the runtime honored. */
    @JsonProperty("serverPolicyCacheMode") ManagedSettingsServerPolicyCacheMode serverPolicyCacheMode,
    /** Whether a configured platform policy helper may be run to produce an enforced helper layer. */
    @JsonProperty("policyHelperEnabled") Boolean policyHelperEnabled,
    /** Directory the policy helper runs in when this resolution is not owned by a registered session. A host resolving policy before its session exists must pass this, or a directory-sensitive helper judges the wrong repository. */
    @JsonProperty("policyHelperWorkingDirectory") String policyHelperWorkingDirectory,
    /** User agent to send with the policy fetch. */
    @JsonProperty("userAgent") String userAgent,
    /** Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. */
    @JsonProperty("clientName") String clientName,
    /** The host's environment variables, read for cache location, policy overrides, and the variables that would make a membership snapshot forgeable. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("environment") Object environment,
    /** The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths. */
    @JsonProperty("platform") String platform,
    /** The host's home directory, used to resolve cache and managed-file locations. */
    @JsonProperty("homeDirectory") String homeDirectory,
    /** Whether to retain the resolved policy as this runtime process's startup policy, so later work in the process enforces it. Set false to resolve without side effects, which is what a protocol host wants before session creation: its sessions apply the complete policy through the normal lifecycle, and retaining here would let a readiness credential affect unrelated sessions. The returned verdict is identical either way — this decides only whether the process keeps it. */
    @JsonProperty("retainAndApply") Boolean retainAndApply
) {
}
