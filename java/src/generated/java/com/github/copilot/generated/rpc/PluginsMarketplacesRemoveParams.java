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
 * Name of the marketplace to remove and an optional force flag.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginsMarketplacesRemoveParams(
    /** Marketplace name to remove */
    @JsonProperty("name") String name,
    /** When true, also uninstall every plugin sourced from this marketplace. When false (default), removal is a no-op if any plugin from this marketplace is installed and the dependent plugin names are returned in the result. */
    @JsonProperty("force") Boolean force
) {
}
