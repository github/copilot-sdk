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
 * Indicates whether the credential update succeeded.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionGitHubAuthSetCredentialsResult(
    /** Whether the operation succeeded */
    @JsonProperty("success") Boolean success,
    /** Whether the session ended up with a populated `copilotUser` for the installed credentials. `true` when the supplied credential already carried `copilotUser` or it was successfully re-resolved server-side. `false` when the credential is installed without `copilotUser` — either re-resolution failed, or the variant cannot be re-resolved from the credential alone (only the raw-token variants `token`, `env`, and `gh-cli` can). In both `false` cases the token swap still applied, but plan/quota/billing metadata is degraded. Present whenever a credential was supplied; omitted only when no credential was supplied (no-op call). */
    @JsonProperty("copilotUserResolved") Boolean copilotUserResolved
) {
}
