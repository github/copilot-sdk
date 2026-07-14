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
 * Active server-driven promotion for a model, including its discount and expiry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBillingPromo(
    /** Stable identifier for the promotion campaign. */
    @JsonProperty("id") String id,
    /** Percentage discount (0-100) applied while the promotion is active. May be fractional. */
    @JsonProperty("discountPercent") Double discountPercent,
    /** UTC ISO 8601 timestamp marking when the promotion ends. Always present: the API only surfaces a promo whose expiry parses and is in the future. Consumers should treat a past value as expired. */
    @JsonProperty("endsAt") String endsAt,
    /** Human-readable promotion message. Does not include the expiry timestamp; consumers may format endsAt and append it. */
    @JsonProperty("message") String message
) {
}
