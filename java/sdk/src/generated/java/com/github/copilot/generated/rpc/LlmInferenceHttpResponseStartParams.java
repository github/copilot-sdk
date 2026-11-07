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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Response head.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmInferenceHttpResponseStartParams(
    /** Matches the requestId from the originating httpRequestStart frame. */
    @JsonProperty("requestId") String requestId,
    /** HTTP status code. */
    @JsonProperty("status") Long status,
    /** Optional HTTP status reason phrase. */
    @JsonProperty("statusText") String statusText,
    /** HTTP response headers, preserving multiple values per name. */
    @JsonProperty("headers") Map<String, List<String>> headers
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LlmInferenceHttpResponseStartParams {}

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
        private String requestId;
        private Long status;
        private String statusText;
        private Map<String, List<String>> headers;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestId(String value) {
            this.requestId = value;
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
         * Sets the {@code statusText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statusText(String value) {
            this.statusText = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(Map<String, List<String>> value) {
            this.headers = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LlmInferenceHttpResponseStartParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            return new LlmInferenceHttpResponseStartParams(requestId, status, statusText, headers);
        }
    }
}
