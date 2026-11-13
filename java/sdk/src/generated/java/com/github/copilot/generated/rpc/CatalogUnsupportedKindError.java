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
 * The request asked for a candidate kind this runtime does not serve.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogUnsupportedKindError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "unsupported-kind";

    @Override
    public String getKind() { return kind; }

    /** The kinds from the request that are not supported. */
    @JsonProperty("requestedKinds")
    private List<CatalogCandidateKind> requestedKinds;

    /** Every candidate kind this runtime can serve. */
    @JsonProperty("supportedKinds")
    private List<CatalogCandidateKind> supportedKinds;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public List<CatalogCandidateKind> getRequestedKinds() { return requestedKinds; }
    public void setRequestedKinds(List<CatalogCandidateKind> requestedKinds) { this.requestedKinds = requestedKinds; }

    public List<CatalogCandidateKind> getSupportedKinds() { return supportedKinds; }
    public void setSupportedKinds(List<CatalogCandidateKind> supportedKinds) { this.supportedKinds = supportedKinds; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
