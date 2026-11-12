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
 * A card could not be parsed or did not satisfy its declared media type's schema.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogMalformedCardError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "malformed-card";

    @Override
    public String getKind() { return kind; }

    /** How the card failed validation. */
    @JsonProperty("reason")
    private CatalogMalformedCardReason reason;

    /** Media type the card was interpreted as, when it declared one this runtime recognises. */
    @JsonProperty("mediaType")
    private CatalogMediaType mediaType;

    /** Human-readable explanation, safe to surface. Never echoes card content, nor a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogMalformedCardReason getReason() { return reason; }
    public void setReason(CatalogMalformedCardReason reason) { this.reason = reason; }

    public CatalogMediaType getMediaType() { return mediaType; }
    public void setMediaType(CatalogMediaType mediaType) { this.mediaType = mediaType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
