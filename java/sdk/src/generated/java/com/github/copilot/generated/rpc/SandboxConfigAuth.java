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
 * Credential-injection capability flags applied while the sandbox is enabled. For the same capability independent of sandboxing, and matched to the credential's GitHub host, see `shell.credentials`; the two are additive.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigAuth(
    /** Whether to authenticate sandboxed HTTPS git through the local masking proxy. The child receives a fake `http.<url>.extraheader`; the real Authorization header is substituted only at its original HTTPS host, port, and repository path scope. github.com uses the Copilot token; other forges use credentials resolved from the user's own helper on the host. Default: false (opt-in). */
    @JsonProperty("git") Boolean git,
    /** Whether to authenticate sandboxed gh through the local masking proxy. The child receives a fake GH_TOKEN; its real value is substituted only at github.com, api.github.com and uploads.github.com (github.com because gh repo clone authenticates git through gh auth git-credential). The repository's GitHub account takes precedence over the Copilot login. Default: false (opt-in). */
    @JsonProperty("gh") Boolean gh
) {
}
