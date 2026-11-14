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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Caller population and startup inputs to resolve named online-experiment gates for. Carries no account: the runtime resolves the assignment for the authentication it already holds.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record FeatureFlagsResolveStartupGatesParams(
    /** The feature flags to resolve. A name this runtime does not declare is reported as an error rather than as a disabled flag. */
    @JsonProperty("flags") List<String> flags,
    /** Whether the caller is GitHub or Microsoft staff. */
    @JsonProperty("isStaff") Boolean isStaff,
    /** Whether the caller is in an experimental population. */
    @JsonProperty("isExperimental") Boolean isExperimental,
    /** Whether the caller belongs to a team plan. */
    @JsonProperty("isTeam") Boolean isTeam,
    /** GitHub-staff membership. Omit when it was never recorded: a legacy staff account then counts as both staff populations and keeps every staff feature. */
    @JsonProperty("isStaffGithub") Boolean isStaffGitHub,
    /** Microsoft-staff membership. Omitted the same way as `isStaffGitHub`. */
    @JsonProperty("isStaffMicrosoft") Boolean isStaffMicrosoft,
    /** Environment to read flag overrides from. Defaults to this runtime process's own environment. */
    @JsonProperty("environment") Map<String, String> environment,
    /** User settings document whose `featureFlags` block is layered on top of the resolved set. */
    @JsonProperty("config") Object config,
    /** Whether streamer mode is on, which withholds the populations an experiment would otherwise segment on. */
    @JsonProperty("streamerMode") Boolean streamerMode,
    /** When this installation first launched, in milliseconds since the Unix epoch. Experiments that segment on installation age read it. Omit when the first launch was never recorded. */
    @JsonProperty("firstLaunchAtMillis") Double firstLaunchAtMillis,
    /** Whether the runtime may refresh the cached assignment over the network without an active session. The refresh never blocks this call: the answer comes from the cache either way, so a refresh only affects a later launch. */
    @JsonProperty("refreshWithoutSession") Boolean refreshWithoutSession
) {
}
