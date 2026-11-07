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
 * Variant {@code proxy-response} of {@link GitHubFetchResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubFetchResultProxyResponse extends GitHubFetchResult {

    @JsonProperty("kind")
    private final String kind = "proxy-response";

    @Override
    public String getKind() { return kind; }

    /** Complete proxy failure message. */
    @JsonProperty("message")
    private String message;

    /** HTTP proxy status. */
    @JsonProperty("status")
    private Long status;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getStatus() { return status; }
    public void setStatus(Long status) { this.status = status; }


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
        private String message;
        private Long status;

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
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(Long value) {
            this.status = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchResultProxyResponse build() {
            if (message == null) throw new IllegalStateException("Missing required property: message");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            GitHubFetchResultProxyResponse instance = new GitHubFetchResultProxyResponse();
            instance.setMessage(message);
            instance.setStatus(status);
            return instance;
        }
    }
}
