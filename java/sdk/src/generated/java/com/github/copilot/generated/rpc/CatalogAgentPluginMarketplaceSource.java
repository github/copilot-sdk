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
 * A plugin marketplace named by verified catalogue install data, installed as `pluginName@name`.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogAgentPluginMarketplaceSource extends CatalogAgentPluginInstallSource {

    @JsonProperty("kind")
    private final String kind = "marketplace";

    @Override
    public String getKind() { return kind; }

    /** Marketplace name. */
    @JsonProperty("name")
    private String name;

    /** Canonical lowercase GitHub owner/repository hosting the marketplace. */
    @JsonProperty("repository")
    private String repository;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }
}
