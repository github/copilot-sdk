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
 * Optional capabilities declared by the provider
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSetProviderCapabilities(
    /** Whether the provider supports SQLite query/exists operations */
    @JsonProperty("sqlite") Boolean sqlite,
    /** Whether the provider supports binary reads and writes through sessionFs.readFileBytes and sessionFs.writeFileBytes */
    @JsonProperty("binary") Boolean binary,
    /** Whether every session provider implements bounded readFileRange byte reads with an authoritative total file byte length. Independent of binary support. Omitted or false retains whole-file compatibility reads. */
    @JsonProperty("readFileRange") Boolean readFileRange
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsSetProviderCapabilities {}

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
        private Boolean sqlite;
        private Boolean binary;
        private Boolean readFileRange;

        /**
         * Sets the {@code sqlite} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sqlite(Boolean value) {
            this.sqlite = value;
            return this;
        }

        /**
         * Sets the {@code binary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder binary(Boolean value) {
            this.binary = value;
            return this;
        }

        /**
         * Sets the {@code readFileRange} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder readFileRange(Boolean value) {
            this.readFileRange = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFsSetProviderCapabilities build() {
            return new SessionFsSetProviderCapabilities(sqlite, binary, readFileRange);
        }
    }

    /**
     * Creates provider capabilities without binary or byte-range reads.
     * @deprecated Use named construction via {@link #builder()}.
     * @param sqlite Whether the provider supports SQLite query/exists operations
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsSetProviderCapabilities(
        Boolean sqlite
    ) {
        this(sqlite, null, null);
    }

    /**
     * Creates provider capabilities without byte-range reads.
     * @deprecated Use named construction via {@link #builder()}.
     * @param sqlite Whether the provider supports SQLite query/exists operations
     * @param binary Whether the provider supports binary reads and writes through sessionFs.readFileBytes and sessionFs.writeFileBytes
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsSetProviderCapabilities(
        Boolean sqlite,
        Boolean binary
    ) {
        this(sqlite, binary, null);
    }
}
