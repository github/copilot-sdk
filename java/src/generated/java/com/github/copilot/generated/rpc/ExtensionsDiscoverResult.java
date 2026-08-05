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
 * Extensions discovered from persisted Copilot home state and their effective loading mode. Launch-scoped additional plugins are not included.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionsDiscoverResult(
    /** Discovered user and enabled installed-plugin extensions from persisted Copilot home state */
    @JsonProperty("extensions") List<DiscoveredExtension> extensions,
    /** Effective extension loading mode. Defaults to load_and_augment when unset. */
    @JsonProperty("mode") DiscoveredExtensionMode mode
) {
}
