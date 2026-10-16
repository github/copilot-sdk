/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The filesystem and environment facts a user-settings file is resolved from. Passed explicitly rather than read from the runtime's own process, so a host whose configuration directory differs from its process environment resolves the file it means.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsHostContext(
    /** The Copilot configuration directory, when the host set one. Takes precedence over `COPILOT_HOME` and the home directory. */
    @JsonProperty("configDir") String configDir,
    /** The user's home directory, used when neither a configuration directory nor `COPILOT_HOME` is set. */
    @JsonProperty("homeDirectory") String homeDirectory,
    /** The host's environment variables. `COPILOT_HOME` is read from here, as are the variables a settings hook may expand. */
    @JsonProperty("environment") Map<String, Object> environment
) {
}
