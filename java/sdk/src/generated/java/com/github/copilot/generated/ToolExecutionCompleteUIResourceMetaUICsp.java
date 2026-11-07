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
 * CSP domain allowlists for an MCP Apps UI resource, including connect, resource, frame, and base URI domains.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUICsp(
    /** Domains the UI resource may connect to. */
    @JsonProperty("connectDomains") List<String> connectDomains,
    /** Domains from which the UI resource may load scripts, styles, images, and other resources. */
    @JsonProperty("resourceDomains") List<String> resourceDomains,
    /** Domains the UI resource may embed as nested frames. */
    @JsonProperty("frameDomains") List<String> frameDomains,
    /** Domains the UI resource may use as document base URIs. */
    @JsonProperty("baseUriDomains") List<String> baseUriDomains
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteUIResourceMetaUICsp {}

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
        private List<String> connectDomains;
        private List<String> resourceDomains;
        private List<String> frameDomains;
        private List<String> baseUriDomains;

        /**
         * Sets the {@code connectDomains} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder connectDomains(List<String> value) {
            this.connectDomains = value;
            return this;
        }

        /**
         * Sets the {@code resourceDomains} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resourceDomains(List<String> value) {
            this.resourceDomains = value;
            return this;
        }

        /**
         * Sets the {@code frameDomains} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder frameDomains(List<String> value) {
            this.frameDomains = value;
            return this;
        }

        /**
         * Sets the {@code baseUriDomains} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder baseUriDomains(List<String> value) {
            this.baseUriDomains = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionCompleteUIResourceMetaUICsp build() {
            return new ToolExecutionCompleteUIResourceMetaUICsp(connectDomains, resourceDomains, frameDomains, baseUriDomains);
        }
    }
}
