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
 * The request was rejected before any work was done, because a bounded field fell outside its permitted range or a required field was unusable.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogInvalidRequestError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "invalid-request";

    @Override
    public String getKind() { return kind; }

    /** Which request field was rejected. */
    @JsonProperty("field")
    private CatalogInvalidRequestField field;

    /** Human-readable explanation, safe to surface. Never echoes the offending value, nor a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogInvalidRequestField getField() { return field; }
    public void setField(CatalogInvalidRequestField field) { this.field = field; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
