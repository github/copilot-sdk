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
 * One connection-owned, expiring request for a trusted host's explicit user decision.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallationConfirmationRequest(
    /** Original engine-resolved selector for a bound operation, never a dispatch default.
Bound MCP confirmation always includes it; correlate it with the original pending action. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Opaque one-use challenge. Return unchanged; never log or persist. */
    @JsonProperty("confirmationId") String confirmationId,
    /** Random identifier of this installation operation, not a plan handle. */
    @JsonProperty("operationId") String operationId,
    /** Original plan expiry as an ISO 8601 timestamp. Confirmation never extends it. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Opaque commitment to the exact review and inputs. Return unchanged; never log. */
    @JsonProperty("reviewFingerprint") String reviewFingerprint,
    /** Resource-specific review to present before collecting the user's decision. */
    @JsonProperty("review") InstallationConfirmationRequestReview review
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InstallationConfirmationRequestReview(
        /** The exact MCP action and its reviewed changes. */
        @JsonProperty("review") Object review,
        /** Reviewed resource discriminator. */
        @JsonProperty("resource") String resource
    ) {
    }
}
