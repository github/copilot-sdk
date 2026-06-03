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
 * Schema for the `PermissionDecisionApprovePermanently` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDecisionApprovePermanently extends PermissionDecision {

    @JsonProperty("kind")
    private final String kind = "approve-permanently";

    @Override
    public String getKind() { return kind; }

    /** URL domain to approve permanently */
    @JsonProperty("domain")
    private String domain;

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
}
