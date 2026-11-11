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
import javax.annotation.processing.Generated;

/**
 * Headers are available; fetch.read consumes the body and fetch.cancel discards it.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class FetchSendResultStream extends FetchSendResult {

    @JsonProperty("responseMode")
    private final FetchResponseMode responseMode = FetchResponseMode.STREAM;

    @Override
    public FetchResponseMode getResponseMode() { return responseMode; }

    /** Final URL after redirects. */
    @JsonProperty("url")
    private String url;

    /** HTTP status code, including non-success statuses. */
    @JsonProperty("status")
    private Long status;

    /** HTTP reason phrase. */
    @JsonProperty("statusText")
    private String statusText;

    /** Headers including repeated names in value order. Distinct-name order is transport-dependent. */
    @JsonProperty("headers")
    private List<FetchHeader> headers;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Long getStatus() { return status; }
    public void setStatus(Long status) { this.status = status; }

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    public List<FetchHeader> getHeaders() { return headers; }
    public void setHeaders(List<FetchHeader> headers) { this.headers = headers; }


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
        private Long status;
        private String statusText;
        private List<FetchHeader> headers;

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
        public Builder headers(List<FetchHeader> value) {
            this.headers = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FetchSendResultStream build() {
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (statusText == null) throw new IllegalStateException("Missing required property: statusText");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            FetchSendResultStream instance = new FetchSendResultStream();
            instance.setUrl(url);
            instance.setStatus(status);
            instance.setStatusText(statusText);
            instance.setHeaders(headers);
            return instance;
        }
    }
}
