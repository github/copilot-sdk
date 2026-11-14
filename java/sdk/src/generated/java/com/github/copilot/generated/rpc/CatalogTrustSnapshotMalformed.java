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
 * Discriminator: the trust field was empty, unbounded, or had the wrong JSON type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogTrustSnapshotMalformed extends CatalogTrustSnapshot {

    @JsonProperty("status")
    private final String status = "malformed";

    @Override
    public String getStatus() { return status; }

    /** Schema version of this runtime-owned snapshot envelope. */
    @JsonProperty("schemaVersion")
    private CatalogTrustSnapshotSchemaVersion schemaVersion;

    /** Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field. */
    @JsonProperty("eligibility")
    private CatalogTrustEligibility eligibility;

    /** Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust. */
    @JsonProperty("provenance")
    private CatalogTrustProvenance provenance;

    public CatalogTrustSnapshotSchemaVersion getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(CatalogTrustSnapshotSchemaVersion schemaVersion) { this.schemaVersion = schemaVersion; }

    public CatalogTrustEligibility getEligibility() { return eligibility; }
    public void setEligibility(CatalogTrustEligibility eligibility) { this.eligibility = eligibility; }

    public CatalogTrustProvenance getProvenance() { return provenance; }
    public void setProvenance(CatalogTrustProvenance provenance) { this.provenance = provenance; }
}
