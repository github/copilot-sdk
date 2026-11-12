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
 * A computed MCP install plan. Nothing has been applied: the plan describes what installing would change, and the plan handle is what a later apply operation would consume.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpPlanInstallPlanned extends McpPlanInstallResult {

    @JsonProperty("kind")
    private final String kind = "planned";

    @Override
    public String getKind() { return kind; }

    /** The normalised plan. */
    @JsonProperty("plan")
    private McpInstallPlan plan;

    /** Protocol version and capabilities the runtime honoured. */
    @JsonProperty("negotiated")
    private CatalogNegotiatedContract negotiated;

    public McpInstallPlan getPlan() { return plan; }
    public void setPlan(McpInstallPlan plan) { this.plan = plan; }

    public CatalogNegotiatedContract getNegotiated() { return negotiated; }
    public void setNegotiated(CatalogNegotiatedContract negotiated) { this.negotiated = negotiated; }
}
