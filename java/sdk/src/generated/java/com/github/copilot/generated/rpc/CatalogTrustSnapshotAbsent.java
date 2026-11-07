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
 * Discriminator: the authority omitted trust metadata.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogTrustSnapshotAbsent extends CatalogTrustSnapshot {

    @JsonProperty("status")
    private final String status = "absent";

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
        private CatalogTrustSnapshotSchemaVersion schemaVersion;
        private CatalogTrustEligibility eligibility;
        private CatalogTrustProvenance provenance;

        /**
         * Sets the {@code schemaVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder schemaVersion(CatalogTrustSnapshotSchemaVersion value) {
            this.schemaVersion = value;
            return this;
        }

        /**
         * Sets the {@code eligibility} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eligibility(CatalogTrustEligibility value) {
            this.eligibility = value;
            return this;
        }

        /**
         * Sets the {@code provenance} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provenance(CatalogTrustProvenance value) {
            this.provenance = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogTrustSnapshotAbsent build() {
            if (schemaVersion == null) throw new IllegalStateException("Missing required property: schemaVersion");
            if (eligibility == null) throw new IllegalStateException("Missing required property: eligibility");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            CatalogTrustSnapshotAbsent instance = new CatalogTrustSnapshotAbsent();
            instance.setSchemaVersion(schemaVersion);
            instance.setEligibility(eligibility);
            instance.setProvenance(provenance);
            return instance;
        }
    }
}
