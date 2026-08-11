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
 * Request parameters for the {@code session.plugins.reload} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsReloadParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Reload MCP server connections after refreshing plugins. Defaults to true. */
    @JsonProperty("reloadMcp") Boolean reloadMcp,
    /** Re-run custom-agent discovery after refreshing plugins. Defaults to true. */
    @JsonProperty("reloadCustomAgents") Boolean reloadCustomAgents,
    /** Re-load user, plugin, and (subject to `deferRepoHooks`) repo hooks. Defaults to true. Has no effect when the host has not registered a hook reloader (e.g. remote sessions). */
    @JsonProperty("reloadHooks") Boolean reloadHooks,
    /** Re-discover and relaunch subprocess extensions (including plugin-shipped extensions) after refreshing plugins. Defaults to true. Has no effect when the session has no active extension controller (e.g. extensions were not requested for the session). */
    @JsonProperty("reloadExtensions") Boolean reloadExtensions,
    /** When true, skip repo-level hooks during the hook reload. Use before folder trust is confirmed; load them post-trust via `sessions.loadDeferredRepoHooks`. */
    @JsonProperty("deferRepoHooks") Boolean deferRepoHooks
) {
}
