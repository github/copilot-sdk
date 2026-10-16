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
 * A single top-level key to record in the host's machine-wide state. The write replaces only that key and leaves the rest of the document untouched, so two writers recording different one-off flags do not overwrite each other. The stored credential keys cannot be written through this method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GlobalStateWriteKeyParams(
    /** Copilot configuration directory to write the state document in, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to write the directory the server resolved for itself. Mirrors `globalState.loadForConfigDir`, so a caller can read and write the same directory. */
    @JsonProperty("configDir") String configDir,
    /** Top-level key to write, named as it appears in the result of `globalState.load`. It must be one of the writable keys that `globalState.writeKey` lists. */
    @JsonProperty("key") String key,
    /** Value to store for the key. Omit it, or pass null, to remove the key instead. */
    @JsonProperty("value") Object value
) {
}
