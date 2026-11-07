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
 * Session plugin metadata, with name, marketplace, optional version, and enabled state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Plugin(
    /** Soft assignment from effective managed settings. This does not mark the plugin as hard-managed. */
    @JsonProperty("assignment") ManagedPluginAssignment assignment,
    /** Plugin name */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from */
    @JsonProperty("marketplace") String marketplace,
    /** Installed version */
    @JsonProperty("version") String version,
    /** Whether the plugin is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Opaque stable identity for a direct plugin source. */
    @JsonProperty("directSourceId") String directSourceId,
    /** Absolute local directory containing this plugin's resolved content, including disabled installations. Absent when no content directory is resolved. This is the plugin root, not its marketplace directory; clients must not forward it back as an additional plugin. */
    @JsonProperty("rootPath") String rootPath,
    /** Absolute marketplace directory for a live plugin. */
    @JsonProperty("installedFrom") String installedFrom,
    /** Runtime plugin provenance, such as "builtin". */
    @JsonProperty("source") String source,
    /** Whether enterprise managed settings control this plugin. */
    @JsonProperty("managed") Boolean managed,
    /** Enabled state required by enterprise managed settings. */
    @JsonProperty("managedDesiredEnabled") Boolean managedDesiredEnabled,
    /** Whether this managed desired plugin has an installed or live record. */
    @JsonProperty("installed") Boolean installed,
    /** Opaque plugin identity in this runtime's inventory. Matches global inventory id and MCP effectiveSource.plugin.id for the same plugin. Stable across enablement changes, updates and restarts while the source is unchanged. Session-only plugin identities are scoped to their session; not a cross-host identifier or a mutation argument. Compare the complete value without parsing it. May be absent on older runtimes. */
    @JsonProperty("id") String id
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param name Plugin name
     * @param marketplace Marketplace the plugin came from
     * @param version Installed version
     * @param enabled Whether the plugin is currently enabled
     * @param directSourceId Opaque stable identity for a direct plugin source.
     * @param installedFrom Absolute marketplace directory for a live plugin.
     * @param source Runtime plugin provenance, such as "builtin".
     * @param managed Whether enterprise managed settings control this plugin.
     * @param managedDesiredEnabled Enabled state required by enterprise managed settings.
     * @param installed Whether this managed desired plugin has an installed or live record.
     */
    public Plugin(
        String name,
        String marketplace,
        String version,
        Boolean enabled,
        String directSourceId,
        String installedFrom,
        String source,
        Boolean managed,
        Boolean managedDesiredEnabled,
        Boolean installed
    ) {
        this(null, name, marketplace, version, enabled, directSourceId, null, installedFrom, source, managed, managedDesiredEnabled, installed, null);
    }
}
