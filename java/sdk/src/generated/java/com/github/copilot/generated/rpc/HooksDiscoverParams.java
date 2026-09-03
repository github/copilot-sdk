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
import javax.annotation.processing.Generated;

/**
 * Optional project paths and host-exclusion behavior for server-scoped hook discovery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HooksDiscoverParams(
    /** Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion. */
    @JsonProperty("projectPaths") List<String> projectPaths,
    /** When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings. */
    @JsonProperty("excludeHostHooks") Boolean excludeHostHooks
) {
}
