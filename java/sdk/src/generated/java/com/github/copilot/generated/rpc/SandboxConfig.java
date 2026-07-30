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
 * Resolved sandbox configuration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfig(
    /** Whether sandboxing is enabled for the session. */
    @JsonProperty("enabled") Boolean enabled,
    /** User-managed sandbox policy fragment merged into the auto-discovered base policy. */
    @JsonProperty("userPolicy") SandboxConfigUserPolicy userPolicy,
    /** Whether to auto-add the current working directory to readwritePaths. Default: true. */
    @JsonProperty("addCurrentWorkingDirectory") Boolean addCurrentWorkingDirectory,
    /** Whether to inject the Copilot GitHub token as an `http.<host>.extraheader` so authenticated HTTPS git works inside the sandbox without the shell-based credential helper the sandbox blocks. Default: false (opt-in). */
    @JsonProperty("gitAuth") Boolean gitAuth,
    /** Whether to export `GH_TOKEN` so the `gh` CLI authenticates inside the sandbox without the OS keyring the sandbox blocks. Default: false (opt-in). */
    @JsonProperty("ghAuth") Boolean ghAuth
) {
}
