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
 * Discovered extension metadata and persistent enablement state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredExtension(
    /** Source-qualified ID accepted by both server and session extension enablement methods */
    @JsonProperty("id") String id,
    /** Human-readable extension name */
    @JsonProperty("name") String name,
    /** Absolute path to the extension entry module, suitable for revealing it in a file manager */
    @JsonProperty("path") String path,
    /** Discovery source */
    @JsonProperty("source") DiscoveredExtensionSource source,
    /** Whether this extension's persistent per-ID preference is enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Containing plugin metadata for plugin-contributed extensions */
    @JsonProperty("plugin") DiscoveredExtensionPlugin plugin
) {
}
