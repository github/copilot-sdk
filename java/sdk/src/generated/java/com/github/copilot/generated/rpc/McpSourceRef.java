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
 * Canonical identity and location of the effective MCP server declaration. The declaration is uniquely addressed by this source id together with the discovered server name.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpSourceRef(
    /** Open source-kind identifier. Known values include user, workspace, invocation, plugin, builtin, and device-registry. */
    @JsonProperty("kind") String kind,
    /** Opaque stable identity for the configuration source. Clients must not parse this value. */
    @JsonProperty("id") String id,
    /** Open semantic editability identifier. Known values are editable and read-only. */
    @JsonProperty("editability") String editability,
    /** Configuration file location, when the declaration is file-backed. */
    @JsonProperty("file") McpSourceFile file,
    /** Plugin identity, when the declaration is plugin-provided. */
    @JsonProperty("plugin") McpSourcePlugin plugin
) {
}
