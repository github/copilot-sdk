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
 * The overrides and host context to resolve effective runtime settings from.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsBuildRuntimeParams(
    /** Settings that take precedence over every document and environment layer, such as the values a host derived from its own command line. Keyed by setting name, with each value carried verbatim, so a host may pass keys this runtime does not recognize. Absent when the host contributes no overrides of its own. */
    @JsonProperty("overrides") Map<String, Object> overrides,
    /** The directory the settings are being resolved for. This selects the repository-scoped document, so a host resolving settings for a directory other than its own process working directory must pass that directory here. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** The environment to read settings-bearing variables from. Passed explicitly rather than read from the server process so a host can resolve settings for an environment it is about to create rather than the one it runs in. */
    @JsonProperty("environment") Map<String, String> environment,
    /** The platform to apply platform-specific defaults for, in the same spelling the host's own runtime reports. */
    @JsonProperty("platform") String platform
) {
}
