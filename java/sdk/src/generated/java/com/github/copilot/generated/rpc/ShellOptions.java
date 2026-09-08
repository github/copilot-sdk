/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Per-session settings for built-in shell tools.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShellOptions(
    /** Controls automatic non-interactive profile loading where supported. Explicit initScripts are unaffected. */
    @JsonProperty("initProfile") ShellInitProfile initProfile,
    /** Ordered host-provided script paths sourced before each built-in shell command when the
entry's shell target matches the active shell. Use these for rc files, environment setup scripts,
or other custom scripts. A script that returns a nonzero status is reported, and later scripts
and the user command continue while the shell remains running. Because scripts are sourced into
the command shell, `exit`, `exec`, failures under `set -e`, or other shell-terminating behavior
can prevent continuation. Script standard output is preserved; Bash script stderr is discarded,
PowerShell exception messages are replaced, and runtime-generated failure notices omit
configured script paths. When sandboxing is enabled, each script must already be readable under
the active sandbox filesystem policy. Pass an empty array to clear the list. */
    @JsonProperty("initScripts") List<ShellInitScript> initScripts,
    /** Flags passed to the active built-in shell process on startup, replacing its default flags.
When omitted, the built-in Bash shell uses `--norc --noprofile`,
and the built-in PowerShell shell uses `-NoProfile -NoLogo`. */
    @JsonProperty("processFlags") List<String> processFlags,
    /** Command-scoped GitHub credential injection for shell commands. */
    @JsonProperty("credentials") ShellCredentials credentials
) {
}
