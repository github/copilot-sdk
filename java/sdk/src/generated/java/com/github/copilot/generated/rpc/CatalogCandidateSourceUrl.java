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
 * Candidate whose card is retrieved from a URL through the runtime's hardened fetch boundary.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogCandidateSourceUrl extends CatalogCandidateSource {

    @JsonProperty("kind")
    private final String kind = "url";

    @Override
    public String getKind() { return kind; }

    /** Card URL as advertised. Inert untrusted data: the runtime retrieves it only through its own hardened boundary, and it is never logged. */
    @JsonProperty("url")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }


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
        private String url;

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogCandidateSourceUrl build() {
            if (url == null) throw new IllegalStateException("Missing required property: url");
            CatalogCandidateSourceUrl instance = new CatalogCandidateSourceUrl();
            instance.setUrl(url);
            return instance;
        }
    }
}
