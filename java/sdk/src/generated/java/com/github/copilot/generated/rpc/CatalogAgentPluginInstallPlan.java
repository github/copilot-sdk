/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * What installing an Agent Plugin would do, for the user to review and confirm. Provisional: the catalogue install data it is built from is not final.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogAgentPluginInstallPlan(
    /** Every valid install source, in preference order: materialization, then marketplace, then repository. Each kind appears at most once. */
    @JsonProperty("sources") List<CatalogAgentPluginInstallSource> sources,
    /** The plugin's name, as named in its marketplace or manifest. Omitted from a materialisation-only plan, whose sources are only `materialization`: the verified manifests supply the name in the plugins.planInstall review. */
    @JsonProperty("pluginName") String pluginName,
    /** Exact plugin version to install. A plan with a marketplace source carries at least one of version and commit. */
    @JsonProperty("version") String version,
    /** Exact full commit ID to install, in lowercase hex. Always present when the plan has a repository source; a plan with a marketplace source carries at least one of version and commit. */
    @JsonProperty("commit") String commit,
    /** Components the plugin contributes, in canonical order. Empty only in a materialisation-only plan, where the verified manifests list them in the plugins.planInstall review. */
    @JsonProperty("components") List<CatalogAgentPluginComponent> components,
    /** Canvas IDs the plugin contributes. Non-empty exactly when components include `canvas`. */
    @JsonProperty("canvasIds") List<String> canvasIds,
    /** Extension IDs the plugin contributes. Non-empty exactly when components include `extension`. */
    @JsonProperty("extensionIds") List<String> extensionIds,
    /** MCP servers the plugin depends on but does not bundle. */
    @JsonProperty("mcpDependencies") List<String> mcpDependencies,
    /** Connectors the plugin depends on. */
    @JsonProperty("connectorDependencies") List<String> connectorDependencies
) {
}
