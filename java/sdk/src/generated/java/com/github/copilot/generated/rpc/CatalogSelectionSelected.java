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
 * The chosen candidate was transferred into a fresh bounded single-use handle for a later explicit planning request.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogSelectionSelected extends CatalogSelectionResult {

    @JsonProperty("kind")
    private final String kind = "selected";

    @Override
    public String getKind() { return kind; }

    /** Fresh single-use candidate handle accepted by mcp.planInstall. Returned only to the native host and never included in model-tool output. */
    @JsonProperty("candidateHandle")
    private String candidateHandle;

    /** The exact search identifier privately bound to the selected candidate. */
    @JsonProperty("searchId")
    private String searchId;

    public String getCandidateHandle() { return candidateHandle; }
    public void setCandidateHandle(String candidateHandle) { this.candidateHandle = candidateHandle; }

    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }
}
