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
 * Outcome of the remove attempt, including dependent-plugin info when applicable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginsMarketplacesRemoveResult(
    /** True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set. */
    @JsonProperty("removed") Boolean removed,
    /** Names of installed plugins that prevented removal. Populated only when `removed=false`. */
    @JsonProperty("dependentPlugins") List<String> dependentPlugins
) {
}
