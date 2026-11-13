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
 * Inputs describing the host and account an ingestion should resolve managed policy for.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsIngestParams(
    /** The policy-helper invocation reason. Defaults to sessionStart. */
    @JsonProperty("policyHelperReason") ManagedSettingsPolicyHelperReason policyHelperReason,
    /** Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy. */
    @JsonProperty("selfFetch") Boolean selfFetch,
    /** The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched. */
    @JsonProperty("authInfo") Object authInfo,
    /** The session whose credential store and working directory the policy helper should use. Omit it when no session exists yet and the process-wide store applies. */
    @JsonProperty("sessionId") String sessionId,
    /** A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine. */
    @JsonProperty("hasDeviceResponse") Boolean hasDeviceResponse,
    /** Whether to ignore a cached server policy when reading, forcing the resolution to come from a live fetch. */
    @JsonProperty("bypassCacheRead") Boolean bypassCacheRead,
    /** How this ingestion may use the on-disk server-policy cache. Defaults to the runtime's own policy when omitted. */
    @JsonProperty("serverPolicyCacheMode") ManagedSettingsServerPolicyCacheMode serverPolicyCacheMode,
    /** Whether a configured platform policy helper may be run to produce an enforced helper layer. */
    @JsonProperty("policyHelperEnabled") Boolean policyHelperEnabled,
    /** Directory the policy helper runs in when this ingestion is not owned by a registered session. A host resolving policy before its session exists must pass this, or a directory-sensitive helper judges the wrong repository. */
    @JsonProperty("policyHelperWorkingDirectory") String policyHelperWorkingDirectory,
    /** User agent to send with the policy fetch. */
    @JsonProperty("userAgent") String userAgent,
    /** Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. Omit it and the runtime reports a generic identity; the runtime version sent alongside is the gate that actually matters. */
    @JsonProperty("clientName") String clientName,
    /** The host's environment variables, read for cache location and policy overrides. Supplied by the host rather than read from the runtime process so a command that selected its own configuration home is honored. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("environment") Object environment,
    /** The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths. */
    @JsonProperty("platform") String platform,
    /** The host's home directory, used to resolve cache and managed-file locations. */
    @JsonProperty("homeDirectory") String homeDirectory
) {
}
