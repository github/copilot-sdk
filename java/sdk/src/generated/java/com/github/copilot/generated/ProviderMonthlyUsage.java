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
 * An authoritative monthly usage reading for the observation's provider, account, and service.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderMonthlyUsage(
    /** Availability of monthly consumption. Missing or unavailable usage must never be interpreted as zero. */
    @JsonProperty("state") ProviderMonthlyUsageState state,
    /** Unit of consumedQuantity; independent of the parent observation's balance unit. */
    @JsonProperty("unit") ProviderQuotaUnit unit,
    /** Identity scope of the reading within its provider and service. */
    @JsonProperty("scope") ProviderMonthlyUsageScope scope,
    /** Nonnegative finite consumption reported by the service, preserving zero and fractions. Present only when state is available; never computed from tokens or balance differences. */
    @JsonProperty("consumedQuantity") Double consumedQuantity,
    /** Service read time in UTC RFC 3339, not a ledger reconciliation watermark or confirmation of the latest inference charge. */
    @JsonProperty("queriedAt") String queriedAt,
    /** Start of the service-reported monthly billing cycle, in UTC RFC 3339. */
    @JsonProperty("cycleStart") String cycleStart,
    /** End of the service-reported monthly billing cycle, in UTC RFC 3339. */
    @JsonProperty("resetOn") String resetOn
) {
}
