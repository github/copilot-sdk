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
 * A computed Skill install plan. Nothing has been applied.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallPlan(
    /** One-use plan handle, bound to the original candidate authority. */
    @JsonProperty("planHandle") String planHandle,
    /** Original operation identifier returned before confirmation. */
    @JsonProperty("operationId") String operationId,
    /** Original wall-clock expiry as an ISO 8601 timestamp. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Safe review to present before applying the plan. */
    @JsonProperty("review") SkillInstallationReview review
) {
}
