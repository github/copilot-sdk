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
 * Bounded, lifecycle-fenced host authority rendered to the Assisted Permissions judge. Source-specific meaning and scope remain opaque data interpreted by the judge. The source must match the enclosing update source. At most 8 statements of up to 1,024 characters each (4,096 total); scope follows the assisted-approval context JSON bounds. A session holds at most 8 active sources and 32 KiB of rendered grants.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticatedAuthorityGrant(
    /** Stable identifier for the trusted authority source; must match the update source. */
    @JsonProperty("source") String source,
    /** Stable host-issued identifier for this authority. */
    @JsonProperty("grantId") String grantId,
    /** RFC 3339 expiry timestamp. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Bounded human-readable authority statements rendered to the judge. */
    @JsonProperty("statements") List<String> statements,
    /** Structured scope facts constraining the authority statements. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("scope") Object scope
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AuthenticatedAuthorityGrant {}

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
        private String source;
        private String grantId;
        private String expiresAt;
        private List<String> statements;
        private Object scope;
        private boolean $presence4;

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
         * Sets the {@code grantId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder grantId(String value) {
            this.grantId = value;
            return this;
        }

        /**
         * Sets the {@code expiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAt(String value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Sets the {@code statements} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statements(List<String> value) {
            this.statements = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(Object value) {
            this.scope = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthenticatedAuthorityGrant build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (grantId == null) throw new IllegalStateException("Missing required property: grantId");
            if (expiresAt == null) throw new IllegalStateException("Missing required property: expiresAt");
            if (statements == null) throw new IllegalStateException("Missing required property: statements");
            if (!$presence4) throw new IllegalStateException("Missing required property: scope");
            return new AuthenticatedAuthorityGrant(source, grantId, expiresAt, statements, scope);
        }
    }
}
