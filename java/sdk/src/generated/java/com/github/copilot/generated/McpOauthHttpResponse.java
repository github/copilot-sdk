/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Raw HTTP response details from the OAuth auth challenge, as observed by the runtime.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpOauthHttpResponse(
    /** HTTP status code returned with the auth challenge. */
    @JsonProperty("statusCode") Long statusCode,
    /** HTTP response headers as observed by the runtime. Order and casing are transport-dependent, and duplicate header names may appear multiple times. */
    @JsonProperty("headers") List<HeaderEntry> headers,
    /** Complete UTF-8 response body for host-specific challenge handling, including an empty string for an empty body. Omitted when the complete body is not valid UTF-8; body read failures fail the HTTP operation rather than exposing a partial response. */
    @JsonProperty("body") String body
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpOauthHttpResponse {}

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
        private Long statusCode;
        private List<HeaderEntry> headers;
        private String body;

        /**
         * Sets the {@code statusCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statusCode(Long value) {
            this.statusCode = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(List<HeaderEntry> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code body} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder body(String value) {
            this.body = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthHttpResponse build() {
            if (statusCode == null) throw new IllegalStateException("Missing required property: statusCode");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            return new McpOauthHttpResponse(statusCode, headers, body);
        }
    }
}
