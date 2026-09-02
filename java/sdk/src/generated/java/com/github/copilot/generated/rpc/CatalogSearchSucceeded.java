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
 * A completed catalog search: inert candidate summaries, each carrying a single-use handle.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogSearchSucceeded extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "succeeded";

    @Override
    public String getKind() { return kind; }

    /** Pseudonymous identifier for this search, issued by the runtime or by the catalog authority it queried and never by the caller, so it cannot be forged or replayed to attribute an install to a search that never happened. Always present on a success, so a result set can be tied to the installs it leads to. It identifies a search rather than a person: it is derived from no user, account, device, or query data, and must never be joined with user identity to re-identify anyone. */
    @JsonProperty("searchId")
    private String searchId;

    /** Matching candidates, never more than the requested limit. All text is inert untrusted data. */
    @JsonProperty("candidates")
    private List<CatalogCandidate> candidates;

    /** Whether further matches existed beyond the requested limit. */
    @JsonProperty("truncated")
    private Boolean truncated;

    /** Protocol version and capabilities the runtime honoured. */
    @JsonProperty("negotiated")
    private CatalogNegotiatedContract negotiated;

    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }

    public List<CatalogCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<CatalogCandidate> candidates) { this.candidates = candidates; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }

    public CatalogNegotiatedContract getNegotiated() { return negotiated; }
    public void setNegotiated(CatalogNegotiatedContract negotiated) { this.negotiated = negotiated; }
}
