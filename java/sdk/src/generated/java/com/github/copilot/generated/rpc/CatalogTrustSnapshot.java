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
 * A versioned, bounded trust observation carried unchanged with a catalog candidate and its private handle context. Current observations require a recognised T1/T2 tier; every non-current state structurally forbids a tier. Eligibility remains `unknown` while Agent Finder supplies no exposure decision, and states absent from its current wire are never inferred from age, relevance, popularity, or a tier transition.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "status", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogTrustSnapshotCurrent.class, name = "current"),
    @JsonSubTypes.Type(value = CatalogTrustSnapshotAbsent.class, name = "absent"),
    @JsonSubTypes.Type(value = CatalogTrustSnapshotStale.class, name = "stale"),
    @JsonSubTypes.Type(value = CatalogTrustSnapshotDowngraded.class, name = "downgraded"),
    @JsonSubTypes.Type(value = CatalogTrustSnapshotRevoked.class, name = "revoked"),
    @JsonSubTypes.Type(value = CatalogTrustSnapshotUnsupported.class, name = "unsupported"),
    @JsonSubTypes.Type(value = CatalogTrustSnapshotMalformed.class, name = "malformed")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class CatalogTrustSnapshot {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the status discriminator
     */
    public abstract String getStatus();
}
