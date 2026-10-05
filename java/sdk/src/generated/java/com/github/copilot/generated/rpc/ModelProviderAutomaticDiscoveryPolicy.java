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
 * Adapter-declared policy that tells clients whether discovery may run automatically.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderAutomaticDiscoveryPolicy(
    /** Whether automatic discovery is allowed, limited to configured providers, or explicit-only. */
    @JsonProperty("mode") ModelProviderAutomaticDiscoveryMode mode,
    /** Maximum network scope used by this adapter during discovery. */
    @JsonProperty("networkScope") ModelProviderDiscoveryNetworkScope networkScope,
    /** True when discovery requires non-null caller input. Omission or null is rejected before adapter execution. When false, omitted or null input selects adapter defaults without schema validation. */
    @JsonProperty("requiresInput") Boolean requiresInput,
    /** True when the adapter must be enabled by a trusted owner, such as a trusted extension, before automatic discovery may run. */
    @JsonProperty("requiresTrust") Boolean requiresTrust
) {
}
