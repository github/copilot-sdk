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
        private McpInstallPlan plan;
        private CatalogNegotiatedContract negotiated;

        /**
         * Sets the {@code plan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder plan(McpInstallPlan value) {
            this.plan = value;
            return this;
        }

        /**
         * Sets the {@code negotiated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder negotiated(CatalogNegotiatedContract value) {
            this.negotiated = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanInstallPlanned build() {
            if (plan == null) throw new IllegalStateException("Missing required property: plan");
            if (negotiated == null) throw new IllegalStateException("Missing required property: negotiated");
            McpPlanInstallPlanned instance = new McpPlanInstallPlanned();
            instance.setPlan(plan);
            instance.setNegotiated(negotiated);
            return instance;
        }
    }
}
