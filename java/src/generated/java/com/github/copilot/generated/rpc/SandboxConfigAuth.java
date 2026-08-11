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
 * Credential-injection capability flags applied while the sandbox is enabled.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigAuth(
    /** Whether to inject git credentials as an `http.<url>.extraheader` so authenticated HTTPS git works inside the sandbox without the shell-based credential helper the sandbox blocks. github.com is served by the Copilot token; every other forge (Azure DevOps, GitHub Enterprise Server, GitLab, ...) by a credential the host resolves from the user's own helper before the sandbox is applied. Default: false (opt-in). */
    @JsonProperty("git") Boolean git,
    /** Whether to export `GH_TOKEN` so the `gh` CLI authenticates inside the sandbox without the OS keyring the sandbox blocks. Default: false (opt-in). */
    @JsonProperty("gh") Boolean gh
) {
}
