/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Plugin(
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
