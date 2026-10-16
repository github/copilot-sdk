/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result of validating a managed-settings document.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsValidateResult(
    /** Whether the runtime would accept the document within the preview resource limits. Always equals whether `settings` is present. An invalid document is rejected as a whole. */
    @JsonProperty("valid") Boolean valid,
    /** Canonical form of the document the runtime would apply, with unrecognized keys removed. Absent when the document is invalid. */
    @JsonProperty("settings") Object settings,
    /** Errors that reject the document and warnings about content the runtime ignores. */
    @JsonProperty("diagnostics") List<ManagedSettingsDiagnostic> diagnostics
) {
}
