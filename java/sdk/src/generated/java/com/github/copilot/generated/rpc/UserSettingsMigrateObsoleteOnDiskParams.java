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
 * Selects the configuration directory whose settings document to clean.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMigrateObsoleteOnDiskParams(
    /** The configuration directory holding the settings document to clean. A host that scopes a session to its own Copilot home passes that directory here; omit it to clean the document under the home the server resolved for itself. The home directory and environment are deliberately not caller-supplied: they decide only the settings path root and the two hook-parsing flags, and those describe the process doing the reading rather than the caller. */
    @JsonProperty("configDir") String configDir
) {
}
