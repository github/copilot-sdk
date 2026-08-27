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
    /** Credential-injection capability flags. */
    @JsonProperty("auth") SandboxConfigAuth auth,
    /** Whether to auto-grant read access to the tool directories discovered on PATH and in toolchain environment variables (GOROOT, CARGO_HOME, JAVA_HOME, VIRTUAL_ENV, and similar), and to common developer-tool caches, registries, and toolchains in their default home locations (cargo, go, npm, Maven, and more), plus read-write access to (and up-front creation of) the scratch caches builds write on every run (go-build, ccache, sccache, Gradle caches, Cargo lock/tracker files), so builds work without extra configuration; a relocated CARGO_HOME additionally gets its Cargo lock files granted read-write. Set to false to disable every grant listed above: user-installed toolchains (rustup, nvm, pyenv, conda, pipx) then need explicit userPolicy.filesystem entries — readonlyPaths to read them, plus readwriteFiles for a relocated CARGO_HOME's .package-cache and .global-cache, which Cargo locks on every build. Only these developer-tool grants are affected: the working directory (see addCurrentWorkingDirectory), temporary storage, session log paths, and system locations follow their own rules and stay granted, so commands still run. Default: true (enabled by default; set to false to opt out). */
    @JsonProperty("allowDevToolAccess") Boolean allowDevToolAccess
) {
}
