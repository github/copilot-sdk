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
 * The caller's protocol version or required capabilities cannot be honoured. Returned instead of a partial or ambiguous success.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogNegotiationRefusedError extends CatalogSearchResult {

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

    /** Every wire feature this runtime understands, so the caller can retry within that contract. This list does not imply that every deployment has enabled every operation. */
    @JsonProperty("supportedCapabilities")
    private List<CatalogCapability> supportedCapabilities;

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

    public List<CatalogCapability> getSupportedCapabilities() { return supportedCapabilities; }
    public void setSupportedCapabilities(List<CatalogCapability> supportedCapabilities) { this.supportedCapabilities = supportedCapabilities; }

    public List<String> getUnsupportedCapabilities() { return unsupportedCapabilities; }
    public void setUnsupportedCapabilities(List<String> unsupportedCapabilities) { this.unsupportedCapabilities = unsupportedCapabilities; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
