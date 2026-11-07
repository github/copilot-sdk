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
 * Resolved sandbox configuration.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
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
    /** Whether extension processes the session launches are confined by the sandbox. Only an explicit `false` opts out. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out). */
    @JsonProperty("sandboxExtensions") Boolean sandboxExtensions,
    /** Whether the agent may request that an individual command run outside the sandbox, which the host then approves or denies through the usual permission flow. A host capability flag rather than part of the policy: it is stripped from the effective spawn policy and only has an effect while `enabled` is true. Fail-closed, unlike the opt-out flags on this object: omitting it offers no bypass. Default: false (opt-in). */
    @JsonProperty("allowBypass") Boolean allowBypass,
    /** Set by the runtime when a managed policy forced `sandboxMcpServers` on and took the local opt-out away. Provenance rather than policy: it lets a sandbox startup failure point at the administrator instead of a setting the next managed merge would override, and it is ignored when comparing two configs for change. Only the managed merge may set it; a caller-supplied value is stripped. */
    @JsonProperty("managedMcpRoutingLocked") Boolean managedMcpRoutingLocked,
    /** The `sandboxLspServers` counterpart of `managedMcpRoutingLocked`. */
    @JsonProperty("managedLspRoutingLocked") Boolean managedLspRoutingLocked,
    /** The `sandboxExtensions` counterpart of `managedMcpRoutingLocked`. */
    @JsonProperty("managedExtensionsRoutingLocked") Boolean managedExtensionsRoutingLocked,
    /** Credential-injection capability flags. */
    @JsonProperty("auth") SandboxConfigAuth auth,
    /** Opt-in whole-value environment masking for sandboxed shell, MCP, and LSP children. Configured names get random sentinels; the local proxy substitutes them only in HTTPS request headers at their injection hosts. Approved bypasses skip masking and the sandbox proxy, so bypassed shells may receive the real environment values. Disabled or explicitly opted-out routes are not protected. No credential values are stored in this configuration. */
    @JsonProperty("credentials") SandboxCredentialsConfig credentials,
    /** Whether to auto-grant read access to tool directories discovered on PATH and in toolchain environment variables (GOROOT, JAVA_HOME, VIRTUAL_ENV, and similar), and to common developer-tool caches, config, and toolchains. Writable grants cover scratch caches, the Unix GitHub CLI cache, and Cargo's registry, git store, and lock/tracker files. A relocated CARGO_HOME gets the same narrow split: registry and git are read-write; bin is read-only; the home root, config.toml, and credentials.toml stay ungranted. Set to false to disable every grant listed above; user-installed toolchains and caches then need explicit userPolicy.filesystem readonlyPaths and readwritePaths entries. The working directory (see addCurrentWorkingDirectory), temporary storage, session log paths, and system locations follow their own rules and stay granted. Default: true (enabled by default; set to false to opt out). */
    @JsonProperty("allowDevToolAccess") Boolean allowDevToolAccess
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfig {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Boolean enabled;
        private SandboxConfigUserPolicy userPolicy;
        private Boolean addCurrentWorkingDirectory;
        private Boolean sandboxMcpServers;
        private Boolean sandboxLspServers;
        private Boolean sandboxExtensions;
        private Boolean allowBypass;
        private Boolean managedMcpRoutingLocked;
        private Boolean managedLspRoutingLocked;
        private Boolean managedExtensionsRoutingLocked;
        private SandboxConfigAuth auth;
        private SandboxCredentialsConfig credentials;
        private Boolean allowDevToolAccess;

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code userPolicy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder userPolicy(SandboxConfigUserPolicy value) {
            this.userPolicy = value;
            return this;
        }

        /**
         * Sets the {@code addCurrentWorkingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder addCurrentWorkingDirectory(Boolean value) {
            this.addCurrentWorkingDirectory = value;
            return this;
        }

        /**
         * Sets the {@code sandboxMcpServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sandboxMcpServers(Boolean value) {
            this.sandboxMcpServers = value;
            return this;
        }

        /**
         * Sets the {@code sandboxLspServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sandboxLspServers(Boolean value) {
            this.sandboxLspServers = value;
            return this;
        }

        /**
         * Sets the {@code sandboxExtensions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sandboxExtensions(Boolean value) {
            this.sandboxExtensions = value;
            return this;
        }

        /**
         * Sets the {@code allowBypass} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowBypass(Boolean value) {
            this.allowBypass = value;
            return this;
        }

        /**
         * Sets the {@code managedMcpRoutingLocked} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedMcpRoutingLocked(Boolean value) {
            this.managedMcpRoutingLocked = value;
            return this;
        }

        /**
         * Sets the {@code managedLspRoutingLocked} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedLspRoutingLocked(Boolean value) {
            this.managedLspRoutingLocked = value;
            return this;
        }

        /**
         * Sets the {@code managedExtensionsRoutingLocked} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedExtensionsRoutingLocked(Boolean value) {
            this.managedExtensionsRoutingLocked = value;
            return this;
        }

        /**
         * Sets the {@code auth} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder auth(SandboxConfigAuth value) {
            this.auth = value;
            return this;
        }

        /**
         * Sets the {@code credentials} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder credentials(SandboxCredentialsConfig value) {
            this.credentials = value;
            return this;
        }

        /**
         * Sets the {@code allowDevToolAccess} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowDevToolAccess(Boolean value) {
            this.allowDevToolAccess = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxConfig build() {
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new SandboxConfig(enabled, userPolicy, addCurrentWorkingDirectory, sandboxMcpServers, sandboxLspServers, sandboxExtensions, allowBypass, managedMcpRoutingLocked, managedLspRoutingLocked, managedExtensionsRoutingLocked, auth, credentials, allowDevToolAccess);
        }
    }

    /**
     * Creates sandbox configuration with default extension routing.
     * @deprecated Use named construction via {@link #builder()}.
     * @param enabled Whether sandboxing is enabled for the session.
     * @param userPolicy User-managed sandbox policy fragment merged into the auto-discovered base policy.
     * @param addCurrentWorkingDirectory Whether to auto-add the current working directory to readwritePaths. Default: true.
     * @param sandboxMcpServers Whether MCP servers the session launches are confined by the sandbox. Only an explicit `false` opts out; doing so also lets remote-MCP egress leave the sandbox, so the flag and `enabled` are always read together. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out).
     * @param sandboxLspServers Whether language servers the session launches are confined by the sandbox. Only an explicit `false` opts out. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out).
     * @param allowBypass Whether the agent may request that an individual command run outside the sandbox, which the host then approves or denies through the usual permission flow. A host capability flag rather than part of the policy: it is stripped from the effective spawn policy and only has an effect while `enabled` is true. Fail-closed, unlike the opt-out flags on this object: omitting it offers no bypass. Default: false (opt-in).
     * @param managedMcpRoutingLocked Set by the runtime when a managed policy forced `sandboxMcpServers` on and took the local opt-out away. Provenance rather than policy: it lets a sandbox startup failure point at the administrator instead of a setting the next managed merge would override, and it is ignored when comparing two configs for change. Only the managed merge may set it; a caller-supplied value is stripped.
     * @param managedLspRoutingLocked The `sandboxLspServers` counterpart of `managedMcpRoutingLocked`.
     * @param auth Credential-injection capability flags.
     * @param credentials Opt-in whole-value environment masking for sandboxed shell, MCP, and LSP children. Configured names get random sentinels; the local proxy substitutes them only in HTTPS request headers at their injection hosts. Approved bypasses skip masking and the sandbox proxy, so bypassed shells may receive the real environment values. Disabled or explicitly opted-out routes are not protected. No credential values are stored in this configuration.
     * @param allowDevToolAccess Whether to auto-grant read access to tool directories discovered on PATH and in toolchain environment variables (GOROOT, JAVA_HOME, VIRTUAL_ENV, and similar), and to common developer-tool caches, config, and toolchains. Writable grants cover scratch caches, the Unix GitHub CLI cache, and Cargo's registry, git store, and lock/tracker files. A relocated CARGO_HOME gets the same narrow split: registry and git are read-write; bin is read-only; the home root, config.toml, and credentials.toml stay ungranted. Set to false to disable every grant listed above; user-installed toolchains and caches then need explicit userPolicy.filesystem readonlyPaths and readwritePaths entries. The working directory (see addCurrentWorkingDirectory), temporary storage, session log paths, and system locations follow their own rules and stay granted. Default: true (enabled by default; set to false to opt out).
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfig(
        Boolean enabled,
        SandboxConfigUserPolicy userPolicy,
        Boolean addCurrentWorkingDirectory,
        Boolean sandboxMcpServers,
        Boolean sandboxLspServers,
        Boolean allowBypass,
        Boolean managedMcpRoutingLocked,
        Boolean managedLspRoutingLocked,
        SandboxConfigAuth auth,
        SandboxCredentialsConfig credentials,
        Boolean allowDevToolAccess
    ) {
        this(enabled, userPolicy, addCurrentWorkingDirectory, sandboxMcpServers, sandboxLspServers, null, allowBypass, managedMcpRoutingLocked, managedLspRoutingLocked, null, auth, credentials, allowDevToolAccess);
    }
}
