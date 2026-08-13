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
 * New auth credentials to install on the session. Omit to leave credentials unchanged.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionGitHubAuthSetCredentialsParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime installs the supplied value immediately for outbound model/API requests. When the credential carries a raw token (`token`, `env`, or `gh-cli`) but no `copilotUser`, the runtime additionally re-resolves `copilotUser` server-side (best-effort, asynchronously, after the synchronous install) so plan/quota/billing metadata regains fidelity; on resolution failure the verbatim credential remains installed. It does NOT otherwise validate the credential. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit. */
    @JsonProperty("credentials") Object credentials
) {
}
