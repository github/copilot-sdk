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
 * A retrieved saved feed. Entries keep the feed's order and are never more than 100.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogGetSavedFeedSucceeded extends CatalogResourceSavedFeedResult {

    @JsonProperty("kind")
    private final String kind = "succeeded";

    @Override
    public String getKind() { return kind; }

    /** Runtime-issued identifier for this retrieval, with the same meaning as a catalog.search searchId, so a plan can be tied to the feed that produced its candidate. Derived from no user, account, device or feed data. */
    @JsonProperty("searchId")
    private String searchId;

    /** The feed's own identity and freshness. */
    @JsonProperty("feed")
    private CatalogSavedFeedDescription feed;

    /** Feed entries in the feed's order. */
    @JsonProperty("entries")
    private List<CatalogFeedEntry> entries;

    /** Protocol version and capabilities the runtime honoured. */
    @JsonProperty("negotiated")
    private CatalogNegotiatedContract negotiated;

    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }

    public CatalogSavedFeedDescription getFeed() { return feed; }
    public void setFeed(CatalogSavedFeedDescription feed) { this.feed = feed; }

    public List<CatalogFeedEntry> getEntries() { return entries; }
    public void setEntries(List<CatalogFeedEntry> entries) { this.entries = entries; }

    public CatalogNegotiatedContract getNegotiated() { return negotiated; }
    public void setNegotiated(CatalogNegotiatedContract negotiated) { this.negotiated = negotiated; }
}
