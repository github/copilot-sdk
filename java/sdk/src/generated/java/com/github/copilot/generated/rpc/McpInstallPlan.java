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
 * A normalised, inert description of what installing an MCP server would involve. Carries no raw card, no install specification, and no secret value.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallPlan(
    /** Opaque, runtime-instance scoped, TTL-bound, single-use handle for this plan. Rejected when stale, replayed, or presented to a different runtime instance. Never logged. */
    @JsonProperty("planHandle") String planHandle,
    /** ISO 8601 timestamp after which the plan handle is stale and will be rejected. Abandoning a plan needs no call: an unused handle simply expires, so cancellation before commit is side-effect free. */
    @JsonProperty("planHandleExpiresAt") String planHandleExpiresAt,
    /** Normalised identity of the server the plan would install. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Origin and semantic digest of the exact validated JSON MCP card content bound to this plan. */
    @JsonProperty("provenance") McpPlanProvenance provenance,
    /** Every eligible transport, so a host can present an explicit choice. A completed plan always has at least one; when none is eligible, planning returns `CatalogUnavailableTransportError` instead. */
    @JsonProperty("transportChoices") List<Object> transportChoices,
    /** Identifier of the choice the runtime would pick by default. Omitted when there is no eligible transport, or when the runtime expresses no preference. */
    @JsonProperty("recommendedTransportChoiceId") String recommendedTransportChoiceId,
    /** Configuration scope and key the plan would write to. */
    @JsonProperty("target") McpPlanTarget target,
    /** Outcome of evaluating the server against registry and enterprise policy. */
    @JsonProperty("policy") McpPlanPolicyResult policy,
    /** The configuration changes installing would make, described rather than serialised, so the mutable configuration payload stays behind the runtime boundary. */
    @JsonProperty("configurationChanges") List<McpPlanConfigurationChange> configurationChanges,
    /** Whether applying this plan would require an MCP reload to take effect. Planning itself never reloads. */
    @JsonProperty("reloadRequired") Boolean reloadRequired,
    /** Whether the plan cannot be applied without further input, because a required value has no default or a secret must be supplied. */
    @JsonProperty("requiresInteractiveConfiguration") Boolean requiresInteractiveConfiguration
) {
}
