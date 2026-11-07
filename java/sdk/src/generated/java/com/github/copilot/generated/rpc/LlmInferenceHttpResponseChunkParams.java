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
 * A response body chunk or terminal error.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmInferenceHttpResponseChunkParams(
    /** Matches the requestId from the originating httpRequestStart frame. */
    @JsonProperty("requestId") String requestId,
    /** Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty (e.g. when the response body is empty: send a single chunk with empty data and end=true). */
    @JsonProperty("data") String data,
    /** When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text. */
    @JsonProperty("binary") Boolean binary,
    /** When true, this is the final body chunk for the response. The runtime treats the response body as complete after receiving an end-marked chunk. */
    @JsonProperty("end") Boolean end,
    /** Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored. */
    @JsonProperty("error") LlmInferenceHttpResponseChunkError error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LlmInferenceHttpResponseChunkParams {}

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
        private String data;
        private Boolean binary;
        private Boolean end;
        private LlmInferenceHttpResponseChunkError error;

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
         * Sets the {@code data} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder data(String value) {
            this.data = value;
            return this;
        }

        /**
         * Sets the {@code binary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder binary(Boolean value) {
            this.binary = value;
            return this;
        }

        /**
         * Sets the {@code end} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder end(Boolean value) {
            this.end = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(LlmInferenceHttpResponseChunkError value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LlmInferenceHttpResponseChunkParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (data == null) throw new IllegalStateException("Missing required property: data");
            return new LlmInferenceHttpResponseChunkParams(requestId, data, binary, end, error);
        }
    }
}
