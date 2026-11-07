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
 * The caller's protocol version or required capabilities cannot be honoured. Returned instead of a partial or ambiguous success.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpInstallationResultCatalogNegotiationRefusedError extends McpInstallationResult {

    @JsonProperty("kind")
    private final String kind = "negotiation-refused";

    @Override
    public String getKind() { return kind; }

    /** Whether the version or the capability set was the problem. */
    @JsonProperty("reason")
    private CatalogNegotiationRefusedReason reason;

    /** Protocol version of the runtime that refused the request. */
    @JsonProperty("runtimeProtocolVersion")
    private Long runtimeProtocolVersion;

    /** Lowest caller protocol version this runtime will serve. */
    @JsonProperty("minimumSupportedProtocolVersion")
    private Long minimumSupportedProtocolVersion;

    /** Capabilities this runtime can safely advertise to this caller. The complete five-capability protocol-3 legacy set is always present; every capability added after that baseline appears only when the caller required it, so an older closed-enum decoder can still consume a refusal. This list does not imply that every deployment has enabled every operation. */
    @JsonProperty("supportedCapabilities")
    private List<String> supportedCapabilities;

    /** The subset of the caller's bounded extensible capability identifiers this runtime cannot honour. */
    @JsonProperty("unsupportedCapabilities")
    private List<String> unsupportedCapabilities;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogNegotiationRefusedReason getReason() { return reason; }
    public void setReason(CatalogNegotiationRefusedReason reason) { this.reason = reason; }

    public Long getRuntimeProtocolVersion() { return runtimeProtocolVersion; }
    public void setRuntimeProtocolVersion(Long runtimeProtocolVersion) { this.runtimeProtocolVersion = runtimeProtocolVersion; }

    public Long getMinimumSupportedProtocolVersion() { return minimumSupportedProtocolVersion; }
    public void setMinimumSupportedProtocolVersion(Long minimumSupportedProtocolVersion) { this.minimumSupportedProtocolVersion = minimumSupportedProtocolVersion; }

    public List<String> getSupportedCapabilities() { return supportedCapabilities; }
    public void setSupportedCapabilities(List<String> supportedCapabilities) { this.supportedCapabilities = supportedCapabilities; }

    public List<String> getUnsupportedCapabilities() { return unsupportedCapabilities; }
    public void setUnsupportedCapabilities(List<String> unsupportedCapabilities) { this.unsupportedCapabilities = unsupportedCapabilities; }

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
        private CatalogNegotiationRefusedReason reason;
        private Long runtimeProtocolVersion;
        private Long minimumSupportedProtocolVersion;
        private List<String> supportedCapabilities;
        private List<String> unsupportedCapabilities;
        private String message;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(CatalogNegotiationRefusedReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code runtimeProtocolVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtimeProtocolVersion(Long value) {
            this.runtimeProtocolVersion = value;
            return this;
        }

        /**
         * Sets the {@code minimumSupportedProtocolVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder minimumSupportedProtocolVersion(Long value) {
            this.minimumSupportedProtocolVersion = value;
            return this;
        }

        /**
         * Sets the {@code supportedCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedCapabilities(List<String> value) {
            this.supportedCapabilities = value;
            return this;
        }

        /**
         * Sets the {@code unsupportedCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder unsupportedCapabilities(List<String> value) {
            this.unsupportedCapabilities = value;
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
        public McpInstallationResultCatalogNegotiationRefusedError build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (runtimeProtocolVersion == null) throw new IllegalStateException("Missing required property: runtimeProtocolVersion");
            if (minimumSupportedProtocolVersion == null) throw new IllegalStateException("Missing required property: minimumSupportedProtocolVersion");
            if (supportedCapabilities == null) throw new IllegalStateException("Missing required property: supportedCapabilities");
            if (unsupportedCapabilities == null) throw new IllegalStateException("Missing required property: unsupportedCapabilities");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            McpInstallationResultCatalogNegotiationRefusedError instance = new McpInstallationResultCatalogNegotiationRefusedError();
            instance.setReason(reason);
            instance.setRuntimeProtocolVersion(runtimeProtocolVersion);
            instance.setMinimumSupportedProtocolVersion(minimumSupportedProtocolVersion);
            instance.setSupportedCapabilities(supportedCapabilities);
            instance.setUnsupportedCapabilities(unsupportedCapabilities);
            instance.setMessage(message);
            return instance;
        }
    }
}
