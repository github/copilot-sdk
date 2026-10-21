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
 * Inputs describing the host and account whose managed remote-control policy should be resolved. Mirrors `managedSettings.ingest`, and deliberately carries no retention decision: reading this gate never becomes this runtime process's startup policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsLoadRemoteControlParams(
    /** The policy-helper invocation reason. Defaults to sessionStart. */
    @JsonProperty("policyHelperReason") ManagedSettingsPolicyHelperReason policyHelperReason,
    /** Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy. */
    @JsonProperty("selfFetch") Boolean selfFetch,
    /** The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched. */
    @JsonProperty("authInfo") Object authInfo,
    /** A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine. */
    @JsonProperty("hasDeviceResponse") Boolean hasDeviceResponse,
    /** Whether a configured platform policy helper may be run to produce an enforced helper layer. */
    @JsonProperty("policyHelperEnabled") Boolean policyHelperEnabled,
    /** User agent to send with the policy fetch. */
    @JsonProperty("userAgent") String userAgent,
    /** Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. That refusal is exactly what turns into an explicit `disabled` below, so a host that omits this field forfeits the protection. */
    @JsonProperty("clientName") String clientName,
    /** The host's environment variables, read for cache location and policy overrides. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("environment") Object environment,
    /** The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths. */
    @JsonProperty("platform") String platform,
    /** The host's home directory, used to resolve cache and managed-file locations. */
    @JsonProperty("homeDirectory") String homeDirectory
) {
}
