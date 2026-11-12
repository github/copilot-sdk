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
 * Selects the configuration directory to run the one-time staff log-level migration against, and supplies the caller's record of whether it has already run.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMigrateStaffLogLevelOnceParams(
    /** The configuration directory, home directory and environment to resolve the settings and global-state documents from. A host that scopes a session to its own Copilot home must pass that directory here, or the migration is evaluated against the server's own home instead. */
    @JsonProperty("context") UserSettingsHostContext context,
    /** When the caller last recorded this migration, as an ISO-8601 timestamp. Supplying it lets the runtime answer `current` without re-running the evaluation. */
    @JsonProperty("cachedCompletedAt") String cachedCompletedAt
) {
}
