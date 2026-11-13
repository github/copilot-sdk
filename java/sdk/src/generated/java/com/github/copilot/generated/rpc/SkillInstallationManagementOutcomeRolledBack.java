/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Interrupted work was safely compensated and the pending marker was cleared.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationManagementOutcomeRolledBack(
    /** Cause of the compensation. */
    @JsonProperty("reason") SkillInstallationFailureReason reason,
    /** Original operation identity. */
    @JsonProperty("operation_id") String operationId,
    /** Skill installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements SkillInstallationManagementOutcome {
    public SkillInstallationManagementOutcomeRolledBack {
        kind = "rolled-back";
    }

    public SkillInstallationManagementOutcomeRolledBack(
        SkillInstallationFailureReason reason,
        String operationId
    ) {
        this(reason, operationId, "rolled-back");
    }
}
