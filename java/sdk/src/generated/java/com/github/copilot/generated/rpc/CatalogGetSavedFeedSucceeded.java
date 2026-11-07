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


    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String searchId;
        private CatalogSavedFeedDescription feed;
        private List<CatalogFeedEntry> entries;
        private CatalogNegotiatedContract negotiated;

        /**
         * Sets the {@code searchId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder searchId(String value) {
            this.searchId = value;
            return this;
        }

        /**
         * Sets the {@code feed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder feed(CatalogSavedFeedDescription value) {
            this.feed = value;
            return this;
        }

        /**
         * Sets the {@code entries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entries(List<CatalogFeedEntry> value) {
            this.entries = value;
            return this;
        }

        /**
         * Sets the {@code negotiated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder negotiated(CatalogNegotiatedContract value) {
            this.negotiated = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogGetSavedFeedSucceeded build() {
            if (searchId == null) throw new IllegalStateException("Missing required property: searchId");
            if (feed == null) throw new IllegalStateException("Missing required property: feed");
            if (entries == null) throw new IllegalStateException("Missing required property: entries");
            if (negotiated == null) throw new IllegalStateException("Missing required property: negotiated");
            CatalogGetSavedFeedSucceeded instance = new CatalogGetSavedFeedSucceeded();
            instance.setSearchId(searchId);
            instance.setFeed(feed);
            instance.setEntries(entries);
            instance.setNegotiated(negotiated);
            return instance;
        }
    }
}
