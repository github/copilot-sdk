/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.plugins.list} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsListResult(
    /** Installed plugins */
    @JsonProperty("plugins") List<SessionPluginsListResultPluginsItem> plugins
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPluginsListResultPluginsItem(
        /** Plugin name */
        @JsonProperty("name") String name,
        /** Marketplace the plugin came from */
        @JsonProperty("marketplace") String marketplace,
        /** Installed version */
        @JsonProperty("version") String version,
        /** Whether the plugin is currently enabled */
        @JsonProperty("enabled") Boolean enabled
    ) {
    }
}
