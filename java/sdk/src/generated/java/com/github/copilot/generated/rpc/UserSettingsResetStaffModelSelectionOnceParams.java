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
import javax.annotation.processing.Generated;

/**
 * Selects the configuration directory to run the one-time staff model-selection reset against, and supplies the caller's record of whether it has already run.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsResetStaffModelSelectionOnceParams(
    /** The configuration directory, home directory and environment to resolve the settings and global-state documents from. A host that scopes a session to its own Copilot home must pass that directory here, or the reset is evaluated against the server's own home instead. */
    @JsonProperty("context") UserSettingsHostContext context,
    /** Whether the signed-in account is a staff account. The reset applies only to staff, so anything other than `true` returns `didReset: false` without touching the settings document. */
    @JsonProperty("staff") Boolean staff,
    /** When the caller last recorded this reset, as an ISO-8601 timestamp. A timestamp at or after the cutoff means the reset already ran and is returned unchanged. */
    @JsonProperty("staffModelResetAt") String staffModelResetAt
) {
}
