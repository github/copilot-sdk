/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Inclusive durable event range summarized by a completion receipt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionReceiptEventRange(
    /** Identifier of the user message that starts the covered exchange. */
    @JsonProperty("startEventId") String startEventId,
    /** Identifier of the assistant turn-end event that ends the covered exchange. Always equals the receipt's sourceEventId, so either field is a valid join key. */
    @JsonProperty("endEventId") String endEventId
) {
}
