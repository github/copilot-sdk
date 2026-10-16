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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Caller population and overrides for static feature-flag resolution, independent of experiment assignments and active runtime state.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record FeatureFlagsResolveParams(
    /** Whether the caller is GitHub or Microsoft staff. */
    @JsonProperty("isStaff") Boolean isStaff,
    /** Whether the caller is in an experimental population. */
    @JsonProperty("isExperimental") Boolean isExperimental,
    /** Whether the caller belongs to the team population used by feature availability. */
    @JsonProperty("isTeam") Boolean isTeam,
    /** GitHub-staff membership. Omit when it was never recorded: a legacy staff account then counts as both staff populations and keeps every staff feature. */
    @JsonProperty("isStaffGithub") Boolean isStaffGitHub,
    /** Microsoft-staff membership. Omitted the same way as `isStaffGitHub`. */
    @JsonProperty("isStaffMicrosoft") Boolean isStaffMicrosoft,
    /** Resolve against this already-computed flag set rather than each flag's declared availability. Keyed by flag name. */
    @JsonProperty("baseFlags") Map<String, Boolean> baseFlags,
    /** Environment for direct feature-flag overrides and `COPILOT_CLI_ENABLED_FEATURE_FLAGS`. Does not apply `COPILOT_EXP_*` experiment assignments. Defaults to this runtime process's own environment. */
    @JsonProperty("environment") Map<String, String> environment,
    /** User settings to apply, including `enabledFeatureFlags`, legacy `feature_flags.enabled`, and supported convenience settings. */
    @JsonProperty("config") Object config,
    /** Flags forced to a value after every other layer, keyed by flag name. */
    @JsonProperty("flagOverrides") Map<String, Boolean> flagOverrides
) {
}
