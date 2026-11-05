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
 * Plugin names (or specs) to disable in the session's authoritative working directory.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsDisableParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Plugin names, "plugin@marketplace" specs, or opaque directSourceId values from inventory to disable. Use directSourceId to distinguish same-named direct installations. This persists user-level enablement, not a session-only override. Unknown targets and session-only plugin-directory mounts are ignored. Plugin-owned MCP servers are stopped in active sessions immediately; other plugin contributions remain available until each session reloads plugins. */
    @JsonProperty("names") List<String> names
) {
}
