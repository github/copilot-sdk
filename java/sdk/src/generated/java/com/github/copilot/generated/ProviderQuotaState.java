/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * An account quota reading. Absence of a quantity is unknown, never zero.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderQuotaState(
    /** Provider/account whose service reported this state. */
    @JsonProperty("provider") ModelProviderRef provider,
    /** Service-owned quota identifier within this provider. */
    @JsonProperty("quotaId") String quotaId,
    /** Unit of entitledQuantity and availableQuantity. */
    @JsonProperty("unit") ProviderQuotaUnit unit,
    /** Whether the service permits access, independently of balance. */
    @JsonProperty("accessState") ProviderQuotaAccessState accessState,
    /** Service capacity state; missing quantities do not imply exhaustion. */
    @JsonProperty("capacityState") ProviderQuotaCapacityState capacityState,
    /** Whether this is a GET account reading or a pre-response admission observation. Observations are never merged across kinds. */
    @JsonProperty("observationKind") ProviderQuotaObservationKind observationKind,
    /** Whether quantities are authoritative budget measurements, advisory balances, or absent. Only authoritative budgets support percentage presentation. */
    @JsonProperty("quantityKind") ProviderQuotaQuantityKind quantityKind,
    /** Acquisition outcome. Unavailable/failed readings have no service verdict; access/capacity unavailable are compatibility placeholders only. */
    @JsonProperty("acquisitionStatus") ProviderQuotaAcquisitionStatus acquisitionStatus,
    /** When the runtime observed this reading, not a charge timestamp or guarantee that consumption has settled. */
    @JsonProperty("observedAt") String observedAt,
    /** HTTP status from acquisition, when available. */
    @JsonProperty("httpStatus") Long httpStatus,
    /** Service error code or client acquisition category, separate from the business-state reason. */
    @JsonProperty("acquisitionError") String acquisitionError,
    /** Optional metadata for an authoritative budget. Advisory balances and admission-only observations do not populate this. */
    @JsonProperty("budgetMetadata") ProviderQuotaBudgetMetadata budgetMetadata,
    /** Service-reported monthly consumption, independent of quota balances and per-call or session cost. Omitted when the service does not report monthly usage. */
    @JsonProperty("monthlyUsage") ProviderMonthlyUsage monthlyUsage,
    /** Key for the backwards-compatible snapshots projection, when the authoritative budget supports that contract. */
    @JsonProperty("compatibilityKey") String compatibilityKey,
    /** Explicit service admission flag, when reported. */
    @JsonProperty("hasQuota") Boolean hasQuota,
    /** Independently reported signed 64-bit entitlement. -1 is an unlimited sentinel, not a capacity-state rewrite; omission and null are preserved. */
    @JsonProperty("entitledQuantity") Long entitledQuantity,
    /** Independently reported signed 64-bit available quantity. Zero does not override the service access/capacity verdict; omission and null are preserved. */
    @JsonProperty("availableQuantity") Long availableQuantity,
    /** Service-reported explanation for the state. */
    @JsonProperty("reason") String reason,
    /** Service-reported quota source. */
    @JsonProperty("source") String source,
    /** Service name owning this reading. */
    @JsonProperty("service") String service
) {
}
