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
 * Durable configuration ownership is distinct from session-specific usability.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallationSummary(
    /** Exact durable installation receipt identity. */
    @JsonProperty("installationId") String installationId,
    /** Original installing operation, not a fresh management operation. */
    @JsonProperty("operationId") String operationId,
    /** Identity retained from the validated original plan. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Exact alternative retained in the installing receipt. */
    @JsonProperty("choiceId") String choiceId,
    /** Ownership or setup state, never inferred proof of tool usability. */
    @JsonProperty("state") McpInstallationState state,
    /** Catalogue identity retained from the installed plan when available. */
    @JsonProperty("catalogue") InstallationCatalogueIdentity catalogue,
    /** ISO 8601 wall-clock installation time when available. */
    @JsonProperty("installedAt") String installedAt
) {
}
