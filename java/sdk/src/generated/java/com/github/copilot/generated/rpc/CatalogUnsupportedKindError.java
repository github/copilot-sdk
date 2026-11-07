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
        private List<CatalogCandidateKind> requestedKinds;
        private List<CatalogCandidateKind> supportedKinds;
        private String message;

        /**
         * Sets the {@code requestedKinds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestedKinds(List<CatalogCandidateKind> value) {
            this.requestedKinds = value;
            return this;
        }

        /**
         * Sets the {@code supportedKinds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedKinds(List<CatalogCandidateKind> value) {
            this.supportedKinds = value;
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
        public CatalogUnsupportedKindError build() {
            if (requestedKinds == null) throw new IllegalStateException("Missing required property: requestedKinds");
            if (supportedKinds == null) throw new IllegalStateException("Missing required property: supportedKinds");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            CatalogUnsupportedKindError instance = new CatalogUnsupportedKindError();
            instance.setRequestedKinds(requestedKinds);
            instance.setSupportedKinds(supportedKinds);
            instance.setMessage(message);
            return instance;
        }
    }
}
