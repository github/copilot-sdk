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
 * The startup policy a resolution composed, the per-layer outcome flags a host needs to decide how much to trust it, and the enterprise sign-in verdict for the credential it ran against. The flags are reported rather than folded into the settings because a host must distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsResolveStartupPolicyResult(
    /** How the current runtime resolved the server layer. */
    @JsonProperty("serverResolution") ManagedSettingsServerPolicyResolution serverResolution,
    /** Advisory minimum client version from a successful server response. */
    @JsonProperty("clientUpdateRequested") String clientUpdateRequested,
    /** The account's server-delivered policy, when one was resolved. */
    @JsonProperty("serverResponse") Object serverResponse,
    /** The device layer discovered from the machine, when one exists. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** The sandbox floor contributed by the device layer. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** The layer produced by the platform policy helper, when one ran. */
    @JsonProperty("policyHelperResponse") Object policyHelperResponse,
    /** The server fetch failed and no cached entry stood in for it, so the server layer is unconfirmed and fail-closed gates apply. */
    @JsonProperty("serverFetchFailed") Boolean serverFetchFailed,
    /** The device layer could not be read, so it is unconfirmed rather than known-absent. */
    @JsonProperty("deviceLoadFailed") Boolean deviceLoadFailed,
    /** The policy helper was configured but could not be run, or its output could not be read. */
    @JsonProperty("policyHelperLoadFailed") Boolean policyHelperLoadFailed,
    /** Telemetry describing which layers answered and how, for the host to report alongside its own startup events. */
    @JsonProperty("telemetry") Object telemetry,
    /** The effective sandbox floor composed across every layer, which a host must apply rather than its own default. */
    @JsonProperty("sandboxFloor") Object sandboxFloor,
    /** Some layer of the policy could not be confirmed, so its absence must not be read as permission. Folds in a failed server fetch as well as an unreadable device layer. */
    @JsonProperty("policyUndetermined") Boolean policyUndetermined,
    /** The permissions layer specifically could not be confirmed, so permission gates apply their fail-closed posture. */
    @JsonProperty("permissionPolicyUndetermined") Boolean permissionPolicyUndetermined,
    /** A managed policy is known to apply to this host, from a discovered source or a resolved document. */
    @JsonProperty("managedPolicyPresent") Boolean managedPolicyPresent,
    /** The sandbox was enabled because policy could not be confirmed rather than because an administrator asked for it. Lets a host explain a sandbox the user did not configure. */
    @JsonProperty("sandboxEnabledByUndeterminedPolicy") Boolean sandboxEnabledByUndeterminedPolicy,
    /** The enterprise sign-in verdict for the credential this resolution ran against, evaluated over the effective policy so a server-delivered pin is honored alongside a device one. Computed on every resolution, including one that was not retained, so a host that declines to apply policy still cannot proceed past a pin. */
    @JsonProperty("forceLoginOrgs") ForceLoginOrgsDecision forceLoginOrgs,
    /** The server refused to deliver a policy because this client is too old to enforce it, carrying the user-facing sentence describing the refusal. No retry will help; the remedy is a client update. */
    @JsonProperty("clientUpdateRequired") String clientUpdateRequired,
    /** Warnings raised while resolving, in the order they occurred. */
    @JsonProperty("warnings") List<String> warnings
) {
}
