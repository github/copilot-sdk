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
import javax.annotation.processing.Generated;

/**
 * Retrieval was refused by the runtime's hardened fetch boundary before any request left the process, or before a redirect was followed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogUnsafeRetrievalError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "unsafe-retrieval";

    @Override
    public String getKind() { return kind; }

    /** Which control refused the retrieval, low cardinality so it can be aggregated without carrying a URL. */
    @JsonProperty("reason")
    private CatalogUnsafeRetrievalReason reason;

    /** Human-readable explanation, safe to surface. Never contains the refused URL, nor a query, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogUnsafeRetrievalReason getReason() { return reason; }
    public void setReason(CatalogUnsafeRetrievalReason reason) { this.reason = reason; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
