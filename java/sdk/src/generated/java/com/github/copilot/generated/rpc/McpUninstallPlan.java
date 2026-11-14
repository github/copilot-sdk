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
 * Exact inert removal plan. No configuration or credentials have changed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpUninstallPlan(
    /** One-use original connection and authority-bound plan handle. */
    @JsonProperty("planHandle") String planHandle,
    /** The original operation, inspectable and cancellable on this same connection. */
    @JsonProperty("operationId") String operationId,
    /** Original wall-clock expiry in milliseconds. Applying never renews it. */
    @JsonProperty("expiresAtEpochMs") Long expiresAtEpochMs,
    /** Original owned receipt being removed. */
    @JsonProperty("installation") McpInstallationSummary installation,
    /** Whether removal restores a protected earlier configuration. */
    @JsonProperty("restoresPreviousConfiguration") Boolean restoresPreviousConfiguration,
    /** Exact configured input slots owned by this installation, never shared OAuth tokens. */
    @JsonProperty("ownedSecretCount") Long ownedSecretCount,
    /** Shared authentication is deliberately retained; revocation is a separate action. */
    @JsonProperty("preservesSharedAuthentication") Boolean preservesSharedAuthentication
) {
}
