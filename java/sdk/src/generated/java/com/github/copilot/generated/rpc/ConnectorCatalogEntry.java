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
 * Credential-free Connector catalog entry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorCatalogEntry(
    /** Canonical Connector name used by lifecycle methods. */
    @JsonProperty("name") String name,
    /** Untrusted display label from the service. */
    @JsonProperty("displayName") String displayName,
    /** Untrusted service description, when present. */
    @JsonProperty("description") String description,
    /** Current authoritative service connection state. */
    @JsonProperty("status") ConnectorCatalogStatus status,
    /** Opaque stable runtime IDs currently projected into the session for this Connector. */
    @JsonProperty("runtimeServerIds") List<String> runtimeServerIds
) {
}
