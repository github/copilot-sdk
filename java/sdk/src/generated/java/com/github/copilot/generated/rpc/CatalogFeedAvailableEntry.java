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
 * An entry whose resource resolves and is presented as the same candidate catalog.search would return.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogFeedAvailableEntry extends CatalogFeedEntry {

    @JsonProperty("status")
    private final String status = "available";

    @Override
    public String getStatus() { return status; }

    /** The entry's resource identifier as saved in the feed. */
    @JsonProperty("identifier")
    private String identifier;

    /** The entry's resource, with the same projection, installability, handle and display metadata as a catalog.search candidate. */
    @JsonProperty("candidate")
    private CatalogCandidate candidate;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public CatalogCandidate getCandidate() { return candidate; }
    public void setCandidate(CatalogCandidate candidate) { this.candidate = candidate; }
}
