/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * One server-discovered hook action from user, repository, plugin, or managed-policy configuration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredHook(
    /** Deterministic identifier for this server-discovered action row. It remains stable while the project, origin, source, event, action content, and duplicate ordinal are unchanged. This is row identity, not the key persisted in disabledHooks. */
    @JsonProperty("id") String id,
    /** Hook event that invokes this action. */
    @JsonProperty("hookType") HookType hookType,
    /** Configuration tier that contributed this hook action. */
    @JsonProperty("origin") HookOrigin origin,
    /** Human-readable source label, such as a hook file path, settings source, or plugin name. */
    @JsonProperty("source") String source,
    /** Input project path for which this server-side action was resolved. Set on every row returned for project-scoped discovery, including repeated user and policy actions. */
    @JsonProperty("projectPath") String projectPath,
    /** Whether this action is enabled under the server-side discovery settings. Concrete sessions may differ because they can add session-specific directories, plugins, or trust. False when its disable key is present in the user's disabled-hooks setting or disable-all settings suppress the action. */
    @JsonProperty("enabled") Boolean enabled,
    /** Durable content hash used by hook enablement. Identical actions may intentionally share this key. Omitted when changing the user's disabled-hooks setting cannot change the action's current server-discovered state, including managed-policy hooks, session-start prompt actions, actions suppressed by disable-all settings, and projectless plugin actions that require project-directory expansion. */
    @JsonProperty("disableKey") String disableKey
) {
}
