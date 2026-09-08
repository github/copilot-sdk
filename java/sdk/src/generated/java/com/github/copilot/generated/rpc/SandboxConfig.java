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
    /** Whether MCP servers the session launches are confined by the sandbox. Only an explicit `false` opts out; doing so also lets remote-MCP egress leave the sandbox, so the flag and `enabled` are always read together. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out). */
    @JsonProperty("sandboxMcpServers") Boolean sandboxMcpServers,
    /** Whether language servers the session launches are confined by the sandbox. Only an explicit `false` opts out. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out). */
    @JsonProperty("sandboxLspServers") Boolean sandboxLspServers,
    /** Whether the agent may request that an individual command run outside the sandbox, which the host then approves or denies through the usual permission flow. A host capability flag rather than part of the policy: it is stripped from the effective spawn policy and only has an effect while `enabled` is true. Fail-closed, unlike the opt-out flags on this object: omitting it offers no bypass. Default: false (opt-in). */
    @JsonProperty("allowBypass") Boolean allowBypass,
    /** Set by the runtime when a managed policy forced `sandboxMcpServers` on and took the local opt-out away. Provenance rather than policy: it lets a sandbox startup failure point at the administrator instead of a setting the next managed merge would override, and it is ignored when comparing two configs for change. Only the managed merge may set it; a caller-supplied value is stripped. */
    @JsonProperty("managedMcpRoutingLocked") Boolean managedMcpRoutingLocked,
    /** The `sandboxLspServers` counterpart of `managedMcpRoutingLocked`. */
    @JsonProperty("managedLspRoutingLocked") Boolean managedLspRoutingLocked,
    /** Credential-injection capability flags. */
    @JsonProperty("auth") SandboxConfigAuth auth,
    /** Whether to auto-grant read access to tool directories discovered on PATH and in toolchain environment variables (GOROOT, JAVA_HOME, VIRTUAL_ENV, and similar), and to common developer-tool caches, config, and toolchains. Writable grants cover scratch caches, the Unix GitHub CLI cache, and Cargo's registry, git store, and lock/tracker files. A relocated CARGO_HOME gets the same narrow split: registry and git are read-write; bin is read-only; the home root, config.toml, and credentials.toml stay ungranted. Set to false to disable every grant listed above; user-installed toolchains and caches then need explicit userPolicy.filesystem readonlyPaths and readwritePaths entries. The working directory (see addCurrentWorkingDirectory), temporary storage, session log paths, and system locations follow their own rules and stay granted. Default: true (enabled by default; set to false to opt out). */
    @JsonProperty("allowDevToolAccess") Boolean allowDevToolAccess
) {
}
