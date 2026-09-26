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
 * An observed worker admission, not a claim that execution succeeded.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerAdmission(
    /** The producer's admission kind. */
    @JsonProperty("kind") WorkerAdmissionKind kind,
    /** Canonical logical message identity, independent of queueItemId; at most 256 UTF-8 bytes. */
    @JsonProperty("messageId") String messageId,
    /** May be omitted only for the matching current worker user.message. */
    @JsonProperty("event") WorkerEventReference event,
    /** Actual AHP participant Turn UUID, not a native turn counter or provenance signal. */
    @JsonProperty("ahpTurnId") UUID ahpTurnId
) {
}
