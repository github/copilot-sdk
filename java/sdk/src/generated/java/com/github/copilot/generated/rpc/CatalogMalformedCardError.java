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


    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private CatalogMalformedCardReason reason;
        private CatalogMediaType mediaType;
        private String message;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(CatalogMalformedCardReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code mediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mediaType(CatalogMediaType value) {
            this.mediaType = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogMalformedCardError build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            CatalogMalformedCardError instance = new CatalogMalformedCardError();
            instance.setReason(reason);
            instance.setMediaType(mediaType);
            instance.setMessage(message);
            return instance;
        }
    }
}
