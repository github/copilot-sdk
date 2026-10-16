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
    @JsonProperty("binary") Boolean binary
) {

    /**
     * Creates provider capabilities without binary reads.
     *
     * @param sqlite Whether the provider supports SQLite query/exists operations
     */
    public SessionFsSetProviderCapabilities(
        Boolean sqlite
    ) {
        this(sqlite, null);
    }
}
