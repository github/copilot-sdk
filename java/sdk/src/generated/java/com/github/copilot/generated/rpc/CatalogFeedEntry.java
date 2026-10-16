/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * One feed entry, in its feed position. Only available entries carry a candidate.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "status", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogFeedAvailableEntry.class, name = "available"),
    @JsonSubTypes.Type(value = CatalogFeedUnavailableEntry.class, name = "unavailable"),
    @JsonSubTypes.Type(value = CatalogFeedFilteredEntry.class, name = "filtered")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class CatalogFeedEntry {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the status discriminator
     */
    public abstract String getStatus();
}
