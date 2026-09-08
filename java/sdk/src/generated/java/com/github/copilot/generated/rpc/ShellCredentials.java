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
 * Command-scoped GitHub credential injection for the shell commands an agent runs.

Each channel is opt-in and independent, and injection is scoped to the individual command
spawn: the credential is resolved from the session's *current* authentication at every spawn
and reaches only spawns whose script actually invokes `git` or `gh`. Because nothing is
retained between spawns, replacing the session credential (`session.gitHubAuth.setCredentials`)
changes what the next spawned command presents — which seeding a credential into the runtime
process's own environment cannot do, since a child's environment is fixed at `exec`.

The credential is matched to the host it authenticates to, so a github.com credential is never
presented to a GitHub Enterprise host and vice versa. Where a channel cannot express that
boundary it injects nothing rather than crossing it -- see `gh` below.

This is independent of `sandboxConfig`: it is a decision about which identity the agent
presents, not about what the agent may touch, and it works on every platform whether or not
an OS sandboxing backend is available. `sandboxConfig.auth` remains the sandbox-scoped
spelling and is additive with this one.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShellCredentials(
    /** Whether to authenticate the agent's `git` commands as the session's GitHub credential, by
injecting an `http.<host>.extraheader` (plus `insteadOf` rewrites so SSH-spelled remotes for
that host use the authenticated HTTPS transport). Applied only to a spawn that runs a
remote-contacting `git` subcommand. Default: false (opt-in). */
    @JsonProperty("git") Boolean git,
    /** Whether to authenticate the agent's `gh` commands as the session's GitHub credential, by
exporting `GH_TOKEN` to a spawn that runs `gh`. Any inherited `gh` credential is removed from
spawns that do not, so the credential stays command-scoped.

Applies to a github.com credential only. `gh` picks its credential variable from the host a
command targets rather than the one the credential belongs to, and the command can choose that
target, so `GH_ENTERPRISE_TOKEN` would offer a single-tenant enterprise credential to every
other enterprise host. A session whose credential is enterprise-scoped therefore runs `gh`
unauthenticated; its `git` commands are unaffected, because `http.<host>.extraheader` is scoped
to one host by construction. Default: false (opt-in). */
    @JsonProperty("gh") Boolean gh
) {
}
