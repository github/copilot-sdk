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
 * A user-facing personal Skill installation location without absolute host paths.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationLocation(
    /** Installation scope. Agent Finder Skills are installed in the user's personal Copilot home. */
    @JsonProperty("scope") SkillInstallationScope scope,
    /** Path relative to the Copilot home. */
    @JsonProperty("relativePath") String relativePath,
    /** Safe display label, for example ~/.copilot/skills/run-checks. */
    @JsonProperty("displayLabel") String displayLabel,
    /** Diagnostics-only absolute host path. Hosts must not display it by default. */
    @JsonProperty("diagnosticsAbsolutePath") String diagnosticsAbsolutePath
) {
}
