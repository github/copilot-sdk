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
 * Host-owned consent is required before bounded continuation can complete.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ConnectorConnectResultConsentRequired extends ConnectorConnectResult {

    @JsonProperty("kind")
    private final String kind = "consent_required";

    @Override
    public String getKind() { return kind; }

    /** Validated HTTPS consent URL. The runtime does not open it. */
    @JsonProperty("consentUrl")
    private String consentUrl;

    /** Opaque ID accepted by continueConnection. */
    @JsonProperty("continuationId")
    private String continuationId;

    public String getConsentUrl() { return consentUrl; }
    public void setConsentUrl(String consentUrl) { this.consentUrl = consentUrl; }

    public String getContinuationId() { return continuationId; }
    public void setContinuationId(String continuationId) { this.continuationId = continuationId; }
}
