/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Management outcome for verified Skill inventory, planning and removal.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeRecoveryRequired.class, name = "recovery-required"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeInstallPlanned.class, name = "install-planned"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeListed.class, name = "listed"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeRecovered.class, name = "recovered"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeRolledBack.class, name = "rolled-back"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeUninstallPlanned.class, name = "uninstall-planned"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeOperation.class, name = "operation"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeEnabledChanged.class, name = "enabled-changed"),
    @JsonSubTypes.Type(value = SkillInstallationManagementOutcomeRefused.class, name = "refused")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public sealed interface SkillInstallationManagementOutcome permits SkillInstallationManagementOutcomeRecoveryRequired, SkillInstallationManagementOutcomeInstallPlanned, SkillInstallationManagementOutcomeListed, SkillInstallationManagementOutcomeRecovered, SkillInstallationManagementOutcomeRolledBack, SkillInstallationManagementOutcomeUninstallPlanned, SkillInstallationManagementOutcomeOperation, SkillInstallationManagementOutcomeEnabledChanged, SkillInstallationManagementOutcomeRefused {
    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    @JsonProperty("kind")
    String kind();
}
