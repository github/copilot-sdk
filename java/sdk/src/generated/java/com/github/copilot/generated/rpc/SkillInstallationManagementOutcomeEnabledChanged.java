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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Enablement changed and the selected session was reconciled.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationManagementOutcomeEnabledChanged(
    /** Updated installation summary. */
    @JsonProperty("installation") SkillInstallationSummary installation,
    /** Safe reload or reconciliation diagnostics. */
    @JsonProperty("diagnostics") List<String> diagnostics,
    /** Skill installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements SkillInstallationManagementOutcome {
    public SkillInstallationManagementOutcomeEnabledChanged {
        kind = "enabled-changed";
    }

    public SkillInstallationManagementOutcomeEnabledChanged(
        SkillInstallationSummary installation,
        List<String> diagnostics
    ) {
        this(installation, diagnostics, "enabled-changed");
    }
}
