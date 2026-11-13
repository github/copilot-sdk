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
 * One atomic edit of a canonical setting in settings.json.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsEditParams(
    /** Configuration directory to edit. Omitted or empty resolves the server's own home, as in `user.settings.loadForConfigDir`. */
    @JsonProperty("configDir") String configDir,
    /** Nonempty canonical path segments of the setting to edit, starting with its top-level key. */
    @JsonProperty("canonicalPath") List<String> canonicalPath,
    /** Set a scalar, add or remove a string list item, or remove the setting. The handler rejects a missing or incompatible value. */
    @JsonProperty("operation") SettingsEditOperation operation,
    /** Required boolean, string or number for set, required string for add-item and remove-item, and forbidden for remove. The runtime rejects all other JSON values. */
    @JsonProperty("value") Object value
) {
}
