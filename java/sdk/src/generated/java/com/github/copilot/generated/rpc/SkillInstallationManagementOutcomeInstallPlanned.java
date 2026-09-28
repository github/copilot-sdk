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
 * A verified Skill installation plan was prepared.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationManagementOutcomeInstallPlanned(
    /** Prepared install plan. */
    @JsonProperty("plan") SkillInstallPlan plan,
    /** Skill installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements SkillInstallationManagementOutcome {
    public SkillInstallationManagementOutcomeInstallPlanned {
        kind = "install-planned";
    }

    public SkillInstallationManagementOutcomeInstallPlanned(
        SkillInstallPlan plan
    ) {
        this(plan, "install-planned");
    }
}
