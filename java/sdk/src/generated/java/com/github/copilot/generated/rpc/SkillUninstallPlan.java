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
 * A computed Skill uninstall plan. Nothing has been removed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillUninstallPlan(
    /** One-use uninstall plan handle. */
    @JsonProperty("planHandle") String planHandle,
    /** Original removal operation identifier returned before confirmation. */
    @JsonProperty("operationId") String operationId,
    /** Original wall-clock expiry as an ISO 8601 timestamp. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Owned Skill installation being removed. */
    @JsonProperty("installation") SkillInstallationSummary installation,
    /** Safe review to present before applying removal. */
    @JsonProperty("review") SkillInstallationReview review
) {
}
