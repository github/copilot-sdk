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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Final remote configuration, not a template. The producer refuses external-value
expansion before presenting this review. Receipt-owned secrets appear only as
`${installation-secret:<id>}` references whose `<id>` matches a reviewed
`${secret:<id>}` placeholder; values are never included.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallationRemoteConfiguration(
    /** Transport in the effective persisted remote configuration. */
    @JsonProperty("transport") McpPlanRemoteTransport transport,
    /** Exact resolved endpoint, without templates or secret placeholders. */
    @JsonProperty("url") String url,
    /** Configured headers, excluding separately authorised OAuth tokens. Values may
contain owned secret references, never secret values. */
    @JsonProperty("headers") Map<String, String> headers,
    /** Configured tool selection, not permission to invoke those tools. */
    @JsonProperty("tools") List<String> tools
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationRemoteConfiguration {}

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
        private McpPlanRemoteTransport transport;
        private String url;
        private Map<String, String> headers;
        private List<String> tools;

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(McpPlanRemoteTransport value) {
            this.transport = value;
            return this;
        }

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
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(Map<String, String> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code tools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tools(List<String> value) {
            this.tools = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallationRemoteConfiguration build() {
            if (transport == null) throw new IllegalStateException("Missing required property: transport");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            if (tools == null) throw new IllegalStateException("Missing required property: tools");
            return new McpInstallationRemoteConfiguration(transport, url, headers, tools);
        }
    }
}
