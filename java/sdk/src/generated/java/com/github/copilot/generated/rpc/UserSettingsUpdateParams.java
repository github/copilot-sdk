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
 * Names the user setting to merge into, and the partial object to merge.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsUpdateParams(
    /** Top-level user-setting key to merge into, such as `extensions`. */
    @JsonProperty("key") String key,
    /** Value to store for `key`. When both the stored value and this value are objects, their fields are shallow-merged and fields this object does not name are preserved. Otherwise this value replaces the stored value. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value,
    /** Copilot configuration directory to write the settings document in, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to use the directory the server resolved for itself. */
    @JsonProperty("configDir") String configDir,
    /** Single-component name for the settings document at `<configDir>/settings/<subDir>.json`; path separators, `.` and `..` are rejected. Omit it, or pass an empty string, to use the main settings document. */
    @JsonProperty("subDir") String subDir
) {
}
