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
 * Installed plugin that contributes a discovered extension.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredExtensionPlugin(
    /** Opaque identity of the owning plugin. Matches plugins.list().plugins[].id on the same runtime host. Compare the complete value without parsing it. May be absent on older runtimes. */
    @JsonProperty("id") String id,
    /** Installed plugin name */
    @JsonProperty("name") String name
) {

    /**
     * Creates a record without plugin ownership or content-root metadata.
     *
     * @param name Installed plugin name
     */
    public DiscoveredExtensionPlugin(
        String name
    ) {
        this(null, name);
    }
}
