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
 * The managed (enterprise policy) layer and the user layer to fold together. The field names carry the layer identity: nothing about a settings object identifies which layer produced it, so a generic base/override pair would let a caller transpose them and silently resolve the governance keys in the user's favour.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMergeManagedAndUserParams(
    /** The managed layer. Its governance keys win, and the per-entry `enabledPlugins` and `extraKnownMarketplaces` control maps are stamped back from it after the union. Omitted when the host is unmanaged. */
    @JsonProperty("managed") Object managed,
    /** The user's own settings layer. Wins on every key the managed layer does not govern. Omitted when the user has no settings yet. */
    @JsonProperty("user") Object user
) {
}
