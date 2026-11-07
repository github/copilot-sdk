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
 * Soft plugin defaults for this session, without persistent user preferences or hard policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsGetDefaultsResult(
    /** Opaque revision of the defaults, assignment reasons, and Team marketplaces. */
    @JsonProperty("generation") String generation,
    /** Plugin specs mapped to soft enablement defaults. Managed defaultEnabled assignments override Team defaults for the same spec. */
    @JsonProperty("defaultEnabledPlugins") Map<String, Boolean> defaultEnabledPlugins,
    /** Team-only defaults for hosts that retain activation inputs. Managed assignments are read live by the runtime resolver and must not be retained as Team defaults. */
    @JsonProperty("teamDefaultEnabledPlugins") Map<String, Boolean> teamDefaultEnabledPlugins,
    /** Team marketplace definitions available only as best-effort installation fallbacks. */
    @JsonProperty("extraKnownMarketplaces") Map<String, TeamPluginMarketplace> extraKnownMarketplaces
) {
}
