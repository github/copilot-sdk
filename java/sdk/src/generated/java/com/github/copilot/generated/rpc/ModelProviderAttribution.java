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
 * Contributor attribution, independent of routing identity and authorization.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderAttribution(
    /** Kind of component that supplied the adapter. Attribution does not confer authority. */
    @JsonProperty("source") ModelProviderProvenanceSource source,
    /** Stable contributor identifier. Required and nonblank for extension and custom sources; optional for built-in and configured sources. Does not grant authority. */
    @JsonProperty("ownerId") String ownerId,
    /** Human-readable contributor name, not the adapter display name. */
    @JsonProperty("ownerDisplayName") String ownerDisplayName
) {
}
