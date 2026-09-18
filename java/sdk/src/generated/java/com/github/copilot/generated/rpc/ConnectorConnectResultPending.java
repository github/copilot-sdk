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
 * The service is still completing the connection without a consent URL.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ConnectorConnectResultPending extends ConnectorConnectResult {

    @JsonProperty("kind")
    private final String kind = "pending";

    @Override
    public String getKind() { return kind; }

    /** Opaque ID accepted by continueConnection. */
    @JsonProperty("continuationId")
    private String continuationId;

    public String getContinuationId() { return continuationId; }
    public void setContinuationId(String continuationId) { this.continuationId = continuationId; }
}
