/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Exact delivery and optional occurrence of a consumed worker notification.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerNotificationReference(
    /** Actual notificationDeliveryId UUID. */
    @JsonProperty("deliveryId") UUID deliveryId,
    /** May be omitted only on the matching current system.notification. */
    @JsonProperty("event") WorkerEventReference event,
    /** Actual consumption mode. */
    @JsonProperty("mode") WorkerNotificationMode mode
) {
}
