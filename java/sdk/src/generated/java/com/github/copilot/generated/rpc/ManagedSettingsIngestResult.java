/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The managed policy layers an ingestion resolved, together with the per-layer outcome flags a host needs to decide how much to trust the result. The flags are reported rather than folded into the settings because a host must be able to distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsIngestResult(
    /** The effective managed settings composed from every resolved layer. Omitted when no layer set anything, so an unmanaged host locks nothing. */
    @JsonProperty("settings") Object settings,
    /** The effective managed permissions layer, when the resolved policy carries one. */
    @JsonProperty("permissions") Object permissions,
    /** The server fetch failed and no cached entry stood in for it, so the server layer is unconfirmed and fail-closed gates apply. */
    @JsonProperty("serverFetchFailed") Boolean serverFetchFailed,
    /** No usable token was available, so no server fetch was attempted. */
    @JsonProperty("serverTokenUnavailable") Boolean serverTokenUnavailable,
    /** The error that made the server fetch fail, when one did. */
    @JsonProperty("serverFetchError") String serverFetchError,
    /** How the server layer was resolved. */
    @JsonProperty("serverResolution") ManagedSettingsServerPolicyResolution serverResolution,
    /** The server fetch failed in a way a later retry could succeed at, as opposed to a refusal that no retry will change. */
    @JsonProperty("serverRetryableFailure") Boolean serverRetryableFailure,
    /** A live fetch failed but a recent cached entry was served in its place, so `serverFetchFailed` stays false. Lets a consumer distinguish a clean fetch from a cache-backed fallback. */
    @JsonProperty("serverCacheFallback") Boolean serverCacheFallback,
    /** A server-managed policy is known to exist even though `serverResponse` is intentionally unset. Consumers should OR this with the presence of `serverResponse` when deciding whether the device is managed, so a fresh start during an outage still applies the fail-closed gates instead of treating the device as unmanaged. */
    @JsonProperty("serverSourceFound") Boolean serverSourceFound,
    /** The account's server-delivered policy, when one was resolved. */
    @JsonProperty("serverResponse") Object serverResponse,
    /** The sandbox object from a cached server policy that a forced refresh declined to serve in full. Folded back into the effective sandbox layer so an outage during a forced refresh still synthesizes the fail-closed sandbox floor from the administrator's known intent. */
    @JsonProperty("serverSandboxFloor") Object serverSandboxFloor,
    /** The server fetch failed while a policy demanded a fresh confirmation, which is the strict posture an enterprise opted into rather than an ordinary transient failure. */
    @JsonProperty("serverForcedRefreshFailure") Boolean serverForcedRefreshFailure,
    /** The server refused to deliver a policy because this client is too old to enforce it, carrying the user-facing sentence describing the refusal. No retry will help; the remedy is a client update. Gates that exist only because of managed policy should deny on this rather than falling back to their unmanaged default. */
    @JsonProperty("serverClientUpdateRequired") String serverClientUpdateRequired,
    /** Minimum client version requested by a successful response. Advisory only; the delivered policy remains active. */
    @JsonProperty("serverClientUpdateRequested") String serverClientUpdateRequested,
    /** The synthesized fail-closed permissions layer applied in place of a policy a refused client may not enforce. */
    @JsonProperty("serverPermissionsFloor") Object serverPermissionsFloor,
    /** The device layer discovered from the machine, when one exists. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** The device layer could not be read, so it is unconfirmed rather than known-absent. */
    @JsonProperty("deviceLoadFailed") Boolean deviceLoadFailed,
    /** The sandbox floor contributed by the device layer. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** A device-managed policy is known to exist. */
    @JsonProperty("deviceSourceFound") Boolean deviceSourceFound,
    /** Whether the device layer's sandbox intent could not be determined, so a consumer must not read its absence as permission to run unsandboxed. */
    @JsonProperty("deviceSandboxUndetermined") Boolean deviceSandboxUndetermined,
    /** The layer produced by the platform policy helper, when one ran. */
    @JsonProperty("policyHelperResponse") Object policyHelperResponse,
    /** Which channel registered the policy helper that produced the helper layer. */
    @JsonProperty("policyHelperAuthority") ManagedSettingsPolicyHelperAuthority policyHelperAuthority,
    /** The policy helper was configured but could not be run or its output could not be read. */
    @JsonProperty("policyHelperLoadFailed") Boolean policyHelperLoadFailed,
    /** A policy helper is configured for this host. */
    @JsonProperty("policyHelperConfigured") Boolean policyHelperConfigured,
    /** The policy helper's failure must be treated as fail-closed rather than ignored. */
    @JsonProperty("policyHelperFailClosed") Boolean policyHelperFailClosed,
    /** Diagnostics emitted while resolving the layers, in the order they occurred. */
    @JsonProperty("logMessages") List<ManagedSettingsLogMessage> logMessages
) {
}
