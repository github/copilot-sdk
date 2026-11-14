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
 * Server-driven promotion for an Auto routing preference. It can be announced before it starts.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoTierBillingPromo(
    /** Stable identifier for the promotion campaign. */
    @JsonProperty("id") String id,
    /** Percentage discount (0-100) while the promotion is active. Absent or 0 means the promotion is an announcement only: do not present a discount. */
    @JsonProperty("discountPercent") Double discountPercent,
    /** UTC ISO 8601 timestamp marking when the promotion starts. The service can announce a promotion before it starts, so do not present the discount as active before this time. Absent means it has already started. */
    @JsonProperty("startsAt") String startsAt,
    /** UTC ISO 8601 timestamp marking when the promotion ends. Absent means open-ended. The runtime omits promotions that have already ended. */
    @JsonProperty("endsAt") String endsAt,
    /** Human-readable promotion message. Does not include the start or end timestamps. */
    @JsonProperty("message") String message,
    /** Whether the service asked hosts to give this promotion a prominent surface, such as a banner. Absent means the service expressed no preference, so hosts should apply their own default rather than read it as `false`. */
    @JsonProperty("showBanner") Boolean showBanner
) {
}
