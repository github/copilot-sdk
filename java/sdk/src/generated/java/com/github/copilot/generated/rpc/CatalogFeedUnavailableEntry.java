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
 * An entry that cannot be presented, kept in its position so clients decide how to render it.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogFeedUnavailableEntry extends CatalogFeedEntry {

    @JsonProperty("status")
    private final String status = "unavailable";

    @Override
    public String getStatus() { return status; }

    /** The entry's resource identifier as saved in the feed. */
    @JsonProperty("identifier")
    private String identifier;

    /** Why the entry cannot be presented. */
    @JsonProperty("reason")
    private CatalogFeedUnavailableReason reason;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public CatalogFeedUnavailableReason getReason() { return reason; }
    public void setReason(CatalogFeedUnavailableReason reason) { this.reason = reason; }
}
