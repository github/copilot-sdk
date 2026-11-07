/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * MCP Apps UI resource metadata for a completed tool result, including CSP, permissions, domain, and border preference.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUI(
    /** CSP domain allowlists for an MCP Apps UI resource, including connect, resource, frame, and base URI domains. */
    @JsonProperty("csp") ToolExecutionCompleteUIResourceMetaUICsp csp,
    /** Browser permission metadata for an MCP Apps UI resource, including camera, microphone, geolocation, and clipboard-write. */
    @JsonProperty("permissions") ToolExecutionCompleteUIResourceMetaUIPermissions permissions,
    /** Optional dedicated origin for the rendered MCP Apps UI resource. */
    @JsonProperty("domain") String domain,
    /** Whether the host should render a border around the MCP Apps UI resource. */
    @JsonProperty("prefersBorder") Boolean prefersBorder
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteUIResourceMetaUI {}

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
        private ToolExecutionCompleteUIResourceMetaUICsp csp;
        private ToolExecutionCompleteUIResourceMetaUIPermissions permissions;
        private String domain;
        private Boolean prefersBorder;

        /**
         * Sets the {@code csp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder csp(ToolExecutionCompleteUIResourceMetaUICsp value) {
            this.csp = value;
            return this;
        }

        /**
         * Sets the {@code permissions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissions(ToolExecutionCompleteUIResourceMetaUIPermissions value) {
            this.permissions = value;
            return this;
        }

        /**
         * Sets the {@code domain} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder domain(String value) {
            this.domain = value;
            return this;
        }

        /**
         * Sets the {@code prefersBorder} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prefersBorder(Boolean value) {
            this.prefersBorder = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionCompleteUIResourceMetaUI build() {
            return new ToolExecutionCompleteUIResourceMetaUI(csp, permissions, domain, prefersBorder);
        }
    }
}
