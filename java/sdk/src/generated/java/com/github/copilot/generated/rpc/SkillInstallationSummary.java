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
 * Durable verified Skill ownership summary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationSummary(
    /** Exact durable installation receipt identity. */
    @JsonProperty("installationId") String installationId,
    /** Operation that installed this Skill. */
    @JsonProperty("operationId") String operationId,
    /** Skill invocation name. */
    @JsonProperty("name") String name,
    /** User-facing installation location without an absolute host path. */
    @JsonProperty("target") SkillInstallationLocation target,
    /** Persisted enablement requested for this installation. */
    @JsonProperty("configuredEnabled") Boolean configuredEnabled,
    /** Bound-session load observation. */
    @JsonProperty("sessionState") SkillInstallationSessionState sessionState,
    /** Exact retained verified source identity. */
    @JsonProperty("source") SkillInstallationSource source,
    /** Catalogue identity retained at install time. */
    @JsonProperty("catalogue") InstallationCatalogueIdentity catalogue,
    /** ISO 8601 wall-clock installation time. */
    @JsonProperty("installedAt") String installedAt,
    /** Ownership state observed from files and receipts. */
    @JsonProperty("ownershipState") SkillInstallationOwnershipState ownershipState
) {
}
