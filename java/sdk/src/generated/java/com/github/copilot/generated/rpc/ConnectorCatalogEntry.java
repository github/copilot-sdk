/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Credential-free Connector catalog entry.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
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
    /** Optional catalog logo. */
    @JsonProperty("logo") String logo,
    /** Optional catalog tier. */
    @JsonProperty("tier") String tier,
    /** Optional catalog release tag. */
    @JsonProperty("releaseTag") String releaseTag,
    /** Current authoritative service connection state. */
    @JsonProperty("status") ConnectorCatalogStatus status,
    /** Opaque stable runtime IDs currently projected into the session for this Connector. */
    @JsonProperty("runtimeServerIds") List<String> runtimeServerIds
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param name Canonical Connector name used by lifecycle methods.
     * @param displayName Untrusted display label from the service.
     * @param description Untrusted service description, when present.
     * @param status Current authoritative service connection state.
     * @param runtimeServerIds Opaque stable runtime IDs currently projected into the session for this Connector.
     */
    public ConnectorCatalogEntry(
        String name,
        String displayName,
        String description,
        ConnectorCatalogStatus status,
        List<String> runtimeServerIds
    ) {
        this(name, displayName, description, null, null, null, status, runtimeServerIds);
    }
}
