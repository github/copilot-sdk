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
 * Replaces or revokes one source's revocable session-scoped authority from the owning host. A non-null grant replaces only the named source; null revokes only the named source. The revision always advances the session-global fence.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticatedAuthorityUpdate(
    /** Monotonically increasing host-issued revision, from 1 to 2^53 - 1 so every SDK represents it exactly. */
    @JsonProperty("revision") Long revision,
    /** Stable source whose independent authority slot is being updated. */
    @JsonProperty("source") String source,
    /** Replacement authority, or null to revoke the named source. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("grant") AuthenticatedAuthorityGrant grant
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AuthenticatedAuthorityUpdate {}

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
        private Long revision;
        private String source;
        private AuthenticatedAuthorityGrant grant;
        private boolean $presence2;

        /**
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder revision(Long value) {
            this.revision = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code grant} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder grant(AuthenticatedAuthorityGrant value) {
            this.grant = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthenticatedAuthorityUpdate build() {
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (!$presence2) throw new IllegalStateException("Missing required property: grant");
            return new AuthenticatedAuthorityUpdate(revision, source, grant);
        }
    }
}
