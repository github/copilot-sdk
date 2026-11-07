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
 * Where and when an AI skill catalog reference was observed. Discovery provenance deliberately carries no content digest because search does not establish the exact validated content a later plan will bind.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogAiSkillCandidateProvenance(
    /** Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data. */
    @JsonProperty("authority") String authority,
    /** ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp. */
    @JsonProperty("observedAt") String observedAt,
    /** Media type advertised for the referenced AI skill card */
    @JsonProperty("mediaType") CatalogAiSkillMediaType mediaType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogAiSkillCandidateProvenance {}

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
        private String authority;
        private String observedAt;
        private CatalogAiSkillMediaType mediaType;

        /**
         * Sets the {@code authority} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authority(String value) {
            this.authority = value;
            return this;
        }

        /**
         * Sets the {@code observedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder observedAt(String value) {
            this.observedAt = value;
            return this;
        }

        /**
         * Sets the {@code mediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mediaType(CatalogAiSkillMediaType value) {
            this.mediaType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogAiSkillCandidateProvenance build() {
            if (authority == null) throw new IllegalStateException("Missing required property: authority");
            if (observedAt == null) throw new IllegalStateException("Missing required property: observedAt");
            if (mediaType == null) throw new IllegalStateException("Missing required property: mediaType");
            return new CatalogAiSkillCandidateProvenance(authority, observedAt, mediaType);
        }
    }
}
