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
     * Creates provider capabilities without binary or byte-range reads.
     *
     * @param sqlite Whether the provider supports SQLite query/exists operations
     */
    public SessionFsSetProviderCapabilities(
        Boolean sqlite
    ) {
        this(sqlite, null, null);
    }

    /**
     * Creates provider capabilities without byte-range reads.
     *
     * @param sqlite Whether the provider supports SQLite query/exists operations
     * @param binary Whether the provider supports binary reads and writes through sessionFs.readFileBytes and sessionFs.writeFileBytes
     */
    public SessionFsSetProviderCapabilities(
        Boolean sqlite,
        Boolean binary
    ) {
        this(sqlite, binary, null);
    }
}
