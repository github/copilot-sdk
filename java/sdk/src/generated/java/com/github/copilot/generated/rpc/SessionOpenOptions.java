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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session construction options.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionOpenOptions.$NullableDeserializer.class)
public record SessionOpenOptions(
    /** Optional stable session identifier to use for a new session. */
    @JsonProperty("sessionId") String sessionId,
    /** Optional human-friendly session name. */
    @JsonProperty("name") String name,
    /** Initial model identifier. */
    @JsonProperty("model") String model,
    /** Initial reasoning effort level. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Initial reasoning summary mode for supported model clients. */
    @JsonProperty("reasoningSummary") SessionOpenOptionsReasoningSummary reasoningSummary,
    /** Initial output verbosity level for supported models. */
    @JsonProperty("verbosity") Verbosity verbosity,
    /** Identifier of the client driving the session. */
    @JsonProperty("clientName") String clientName,
    /** OAuth Client ID Metadata Document URL used by this host for MCP authorization. */
    @JsonProperty("authClientIdMetadataUrl") String authClientIdMetadataUrl,
    /** Structured client kind used for runtime behavior gates. */
    @JsonProperty("clientKind") String clientKind,
    /** Identifier sent to LSP-style integrations. */
    @JsonProperty("lspClientName") String lspClientName,
    /** Stable integration identifier for analytics. */
    @JsonProperty("integrationId") String integrationId,
    /** ExP assignment ('flight') data injected by an SDK integrator, in the same JSON shape the Copilot CLI fetches from the experimentation service (CopilotExpAssignmentResponse). When supplied this is fed into the FeatureFlagService exactly like CLI-fetched assignments and ExP-backed flags wait for it. When absent the session does not block on ExP. */
    @JsonProperty("expAssignments") Object expAssignments,
    /** Opt-in: self-fetch and enforce enterprise managed settings, including managed hook policies, at session bootstrap. */
    @JsonProperty("enableManagedSettings") Boolean enableManagedSettings,
    /** Managed-setting paths this trusted SDK host enforces externally or does not support, excluded from fetched, device, and policy-helper policy for this session. Exact dotted paths exclude a setting and its descendants (for example sandbox or permissions.disableBypassPermissionsMode); wildcards and array indices are not supported. Host-injected managedSettings and policy availability checks are not excluded. Not persisted; re-supply on resume, where omission restores full enforcement. Requires enableManagedSettings to self-fetch policy. */
    @JsonProperty("excludedManagedSettings") List<String> excludedManagedSettings,
    /** Opt in to enforcing non-overridable managed model controls on session model, Auto-tier, reasoning-effort, and context-tier changes. Managed defaults still apply when omitted; this option only turns conflicting changes into errors. */
    @JsonProperty("enforceManagedModelDefaults") Boolean enforceManagedModelDefaults,
    /** Permissions-only enterprise policy injected by the SDK host at session create or resume. Composes restrictively with self-fetched and device policy and is not persisted. */
    @JsonProperty("managedSettings") SessionManagedSettings managedSettings,
    /** Declares that a human drives this session's conversation, enabling user-message analysis (sentiment, task completion criteria, and task analytics) for user-authored messages. Automated messages remain excluded. Defaults to false for SDK sessions. Applied when the session is created or loaded from storage; ignored when resuming a session that is already active, and for cloud sessions. */
    @JsonProperty("interactiveUser") Boolean interactiveUser,
    /** Opt in to capturing file changes for session rewind and session diff. Capture cannot reconstruct changes made before it was enabled. On create it starts capture from the first turn. It is also honored on resume: for a session that already has tracked prior turns, tracking continues automatically even if this is omitted; passing it on resume additionally enables tracking for an eligible session that has no prior root turn yet. Resuming a session whose prior root turns were never tracked has no restorable baseline, so tracking stays disabled for it and rewind reports file change tracking as unavailable; the resume itself still succeeds, so sessions that predate tracking remain loadable. The opt-in is only rejected when the session can never track (a subagent session, or one without local session storage). It is intentionally absent from the mutable options update because enabling it after edits have occurred would create an incomplete, misleading baseline. Subagents share the parent session's capture store and are not tracked as separate rewind points: a file a subagent writes is attributed to whichever root user turn was open when the capture was staged, just before the tool body ran. A turn cannot open while a staged capture is still in flight, so a subagent tool that staged under the spawning turn stays attributed to it however late the write lands, while a capture it stages after the user's next message belongs to that later turn. Attribution decides which turn's rewind point counts and file preview include that write; it does not narrow which rewinds revert it, because a rewind restores every capture from the selected turn onward, so the earlier spawning turn reverts it as well. */
    @JsonProperty("enableFileChangeTracking") Boolean enableFileChangeTracking,
    /** Feature-flag values resolved by the host. */
    @JsonProperty("featureFlags") Map<String, Boolean> featureFlags,
    /** Whether experimental behavior is enabled. */
    @JsonProperty("isExperimentalMode") Boolean isExperimentalMode,
    /** Initial authentication info for the session. */
    @JsonProperty("authInfo") Object authInfo,
    /** Custom model-provider configuration (BYOK). */
    @JsonProperty("provider") ProviderConfig provider,
    /** Options scoped to the built-in CAPI (Copilot API) provider. */
    @JsonProperty("capi") CapiSessionOptions capi,
    /** Whether a CLI host explicitly requested the initial Auto preference. False preserves a settings-derived preference without validating availability during creation; execution still validates it. Defaults to true and is ignored for non-CLI callers. */
    @JsonProperty("autoTierIsExplicit") Boolean autoTierIsExplicit,
    /** Named BYOK provider connections, additive to CAPI auth. Combining with `provider` is rejected. */
    @CopilotExperimental
    @JsonProperty("providers") List<NamedProviderConfig> providers,
    /** BYOK model definitions added to the selectable model list, each referencing a provider name. */
    @CopilotExperimental
    @JsonProperty("models") List<ProviderModelConfig> models,
    /** Working directory to anchor the session. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Additional directories the agent may access beyond the working directory. Each entry is granted to the session's file-access allow-list and surfaced to the model (system prompt context and `@`-mention completion). Conventional `.github/skills/` and `.github/agents/` definitions under each directory also join the session's project catalogs when their existing subsystem gates are enabled: added-root skills require both `enableConfigDiscovery` and effective `enableSkills`; added-root agents require `enableConfigDiscovery`. Supplying a directory therefore activates configuration from it and should be treated as a trust decision. Absolute paths are recommended; a relative path is resolved against the session's working directory. Nonexistent or unresolvable entries are skipped with a warning. This is applied during session creation and cold resume and is not persisted, so a cold resume must re-supply the directories. */
    @JsonProperty("additionalDirectories") List<String> additionalDirectories,
    /** Pre-resolved working-directory context for session startup. */
    @JsonProperty("workingDirectoryContext") SessionContext workingDirectoryContext,
    /** Whether this session supports remote steering. */
    @JsonProperty("remoteSteerable") Boolean remoteSteerable,
    /** Telemetry-only remote exporting flag. */
    @JsonProperty("remoteExporting") Boolean remoteExporting,
    /** Telemetry-only remote-defaulted flag. */
    @JsonProperty("remoteDefaultedOn") Boolean remoteDefaultedOn,
    /** Parent session ID for detached child telemetry rollup. */
    @JsonProperty("detachedFromSpawningParentSessionId") String detachedFromSpawningParentSessionId,
    /** Parent engagement ID for detached child telemetry rollup. */
    @JsonProperty("detachedFromSpawningParentEngagementId") String detachedFromSpawningParentEngagementId,
    /** Allowlist of available tool names. */
    @JsonProperty("availableTools") List<String> availableTools,
    /** Denylist of tool names. */
    @JsonProperty("excludedTools") List<String> excludedTools,
    /** Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available. */
    @JsonProperty("includedBuiltinAgents") List<String> includedBuiltinAgents,
    /** Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available. */
    @JsonProperty("excludedBuiltinAgents") List<String> excludedBuiltinAgents,
    /** Deprecated and ignored. Shell command analysis is always enabled; retained for wire compatibility. */
    @Deprecated
    @JsonProperty("enableScriptSafety") Boolean enableScriptSafety,
    /** Per-session settings for built-in shell tools. */
    @JsonProperty("shell") ShellOptions shell,
    /** Use shell.initProfile instead. Shell init profile. */
    @Deprecated
    @JsonProperty("shellInitProfile") String shellInitProfile,
    /** PowerShell process flags applied to built-in and user-requested shell commands. */
    @JsonProperty("shellProcessFlags") List<String> shellProcessFlags,
    /** Resolved sandbox configuration. */
    @JsonProperty("sandboxConfig") SandboxConfig sandboxConfig,
    /** Origin of the sandbox choice. Settings-derived origins (never_configured, user_enabled, user_disabled, repository_policy) let managed policy floor a host preference; explicit below-floor changes remain policy conflicts unless a session opt-out is authorized. Also used for telemetry provenance. */
    @CopilotExperimental
    @JsonProperty("sandboxConfigSource") SandboxConfigSource sandboxConfigSource,
    /** Whether interactive shell sessions are logged. */
    @JsonProperty("logInteractiveShells") Boolean logInteractiveShells,
    /** How MCP server environment values are interpreted. */
    @JsonProperty("envValueMode") SessionOpenOptionsEnvValueMode envValueMode,
    /** MCP server names disabled for this session. Disabled servers are not started or authenticated on create or cold resume. */
    @JsonProperty("disabledMcpServers") List<String> disabledMcpServers,
    /** Non-secret host-managed HTTP MCP servers keyed by stable managed identity. Managed provenance is runtime-established from this separate field and credentials are supplied through dynamic-header refresh. */
    @CopilotExperimental
    @JsonProperty("managedMcpServers") Map<String, ManagedMcpServerConfig> managedMcpServers,
    /** Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers. */
    @JsonProperty("allowAllMcpServerInstructions") Boolean allowAllMcpServerInstructions,
    /** Explicit discovery-only skill roots, not filesystem grants. Available without enableConfigDiscovery, subject to skill/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution. */
    @JsonProperty("skillDirectories") List<String> skillDirectories,
    /** Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without enableConfigDiscovery, subject to managed/source disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution. */
    @JsonProperty("agentDirectories") List<String> agentDirectories,
    /** Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without enableConfigDiscovery, subject to instruction/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution. */
    @JsonProperty("instructionDirectories") List<String> instructionDirectories,
    /** Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without enableConfigDiscovery or enableFileHooks, which controls repository-origin hooks only; hook/managed disables still apply. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution. */
    @JsonProperty("hookDirectories") List<String> hookDirectories,
    /** Skill scan directories and descendants excluded from discovery. Supports `~`-relative paths. */
    @JsonProperty("ignoredSkillsLocations") List<String> ignoredSkillsLocations,
    /** Whether skill loading is enabled. When omitted, explicit SDK skill roots or an SDK skill provider enable skills independently of enableConfigDiscovery. */
    @JsonProperty("enableSkills") Boolean enableSkills,
    /** Whether the requesting SDK session has a skill provider. The provider remains ephemeral and is never persisted in session options or history. When enableSkills is false, it remains bound but dormant and receives no callbacks. Cloud, relay, handoff, and raw sessions.open flows reject it because they cannot safely pre-register the callback handler. */
    @CopilotExperimental
    @JsonProperty("hasSkillProvider") Boolean hasSkillProvider,
    /** Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available. */
    @JsonProperty("includedBuiltinSkills") List<String> includedBuiltinSkills,
    /** Skill IDs disabled for this session. */
    @JsonProperty("disabledSkills") List<String> disabledSkills,
    /** Installed plugins visible to the session. */
    @JsonProperty("installedPlugins") List<InstalledPlugin> installedPlugins,
    /** Whether custom agents default to local-only execution. */
    @JsonProperty("customAgentsLocalOnly") Boolean customAgentsLocalOnly,
    /** Whether to skip custom instruction sources. */
    @JsonProperty("skipCustomInstructions") Boolean skipCustomInstructions,
    /** Whether to invalidate cached custom-instruction discovery before constructing the session. Use when instruction files may have changed earlier in the same runtime process. */
    @JsonProperty("refreshCustomInstructions") Boolean refreshCustomInstructions,
    /** Instruction source IDs disabled for this session. */
    @JsonProperty("disabledInstructionSources") List<String> disabledInstructionSources,
    /** Whether commit-message coauthor trailers are enabled. */
    @JsonProperty("coauthorEnabled") Boolean coauthorEnabled,
    /** Optional trajectory output file path. */
    @JsonProperty("trajectoryFile") String trajectoryFile,
    /** Whether model responses stream as delta events. */
    @JsonProperty("enableStreaming") Boolean enableStreaming,
    /** Experimental: enable native model citations for supported Anthropic and OpenAI models, normalized onto the `assistant.message` event. Off by default; may change or be removed while the citations surface is experimental. */
    @CopilotExperimental
    @JsonProperty("enableCitations") Boolean enableCitations,
    /** Whether the model may request multiple tool calls per response. Defaults to true. False sends a provider hint, not an execution guarantee: all returned calls are still handled and handlers may run concurrently. Re-supply on cold resume. */
    @JsonProperty("allowMultipleToolCalls") Boolean allowMultipleToolCalls,
    /** Override URL for the Copilot API endpoint. */
    @JsonProperty("copilotUrl") String copilotUrl,
    /** Whether ask_user is explicitly disabled. */
    @JsonProperty("askUserDisabled") Boolean askUserDisabled,
    /** Whether auto-mode continuation is enabled. */
    @JsonProperty("continueOnAutoMode") Boolean continueOnAutoMode,
    /** Whether the host is an interactive UI. */
    @JsonProperty("runningInInteractiveMode") Boolean runningInInteractiveMode,
    /** Whether on-demand custom instruction discovery is enabled. */
    @JsonProperty("enableOnDemandInstructionDiscovery") Boolean enableOnDemandInstructionDiscovery,
    /** Maximum decoded byte size of a single inline model-facing binary tool result persisted in session events (default 10 MB). */
    @JsonProperty("maxInlineBinaryBytes") Long maxInlineBinaryBytes,
    /** Initial model capability overrides. */
    @JsonProperty("modelCapabilitiesOverrides") ModelCapabilitiesOverride modelCapabilitiesOverrides,
    /** Initial session limits. */
    @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits,
    /** Autopilot policy. Defaults to runtime-owned continuation with at most five automatic continuations. Does not change the session mode. */
    @JsonProperty("autopilot") AutopilotOptions autopilot,
    /** Runtime context discriminator for agent filtering. */
    @JsonProperty("agentContext") String agentContext,
    /** Override directory for session event logs. */
    @JsonProperty("eventsLogDirectory") String eventsLogDirectory,
    /** Whether subagent callback events should be forwarded into the session event log sink. */
    @JsonProperty("eventsLogIncludesSubagents") Boolean eventsLogIncludesSubagents,
    /** Override Copilot configuration directory. */
    @JsonProperty("configDir") String configDir,
    /** Additional content-exclusion policies to merge into the session policy set. */
    @CopilotExperimental
    @JsonProperty("additionalContentExclusionPolicies") List<SessionOpenOptionsAdditionalContentExclusionPolicy> additionalContentExclusionPolicies,
    /** Live gate for repository-origin file hooks, including the primary repository, trusted repository-configured roots, and accepted startup additional directories (including CLI --add-dir). False suppresses these hooks; explicit hookDirectories and SDK callbacks are independent. Additional-root eligibility and the DISABLE_ADDITIONAL_DIRECTORY_HOOKS escape hatch are captured at create/cold resume, not expanded by later grants or feature-flag changes. Discovery uses containing Git roots, so supply only trusted repositories: commands and prompts need no further approval. Policy, hook-disable settings, and sandbox rules still apply. Provider-backed sessions skip repository hooks with a warning on omission, skip silently on false, and reject explicit true. */
    @JsonProperty("enableFileHooks") Boolean enableFileHooks,
    /** Memory configuration for this session. */
    @JsonProperty("memory") MemoryConfiguration memory,
    /** Capabilities enabled for this session. */
    @JsonProperty("sessionCapabilities") List<SessionCapability> sessionCapabilities,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionOpenOptions { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionOpenOptions(String sessionId, String name, String model, String reasoningEffort, SessionOpenOptionsReasoningSummary reasoningSummary, Verbosity verbosity, String clientName, String authClientIdMetadataUrl, String clientKind, String lspClientName, String integrationId, Object expAssignments, Boolean enableManagedSettings, List<String> excludedManagedSettings, Boolean enforceManagedModelDefaults, SessionManagedSettings managedSettings, Boolean interactiveUser, Boolean enableFileChangeTracking, Map<String, Boolean> featureFlags, Boolean isExperimentalMode, Object authInfo, ProviderConfig provider, CapiSessionOptions capi, Boolean autoTierIsExplicit, List<NamedProviderConfig> providers, List<ProviderModelConfig> models, String workingDirectory, List<String> additionalDirectories, SessionContext workingDirectoryContext, Boolean remoteSteerable, Boolean remoteExporting, Boolean remoteDefaultedOn, String detachedFromSpawningParentSessionId, String detachedFromSpawningParentEngagementId, List<String> availableTools, List<String> excludedTools, List<String> includedBuiltinAgents, List<String> excludedBuiltinAgents, Boolean enableScriptSafety, ShellOptions shell, String shellInitProfile, List<String> shellProcessFlags, SandboxConfig sandboxConfig, SandboxConfigSource sandboxConfigSource, Boolean logInteractiveShells, SessionOpenOptionsEnvValueMode envValueMode, List<String> disabledMcpServers, Map<String, ManagedMcpServerConfig> managedMcpServers, Boolean allowAllMcpServerInstructions, List<String> skillDirectories, List<String> agentDirectories, List<String> instructionDirectories, List<String> hookDirectories, List<String> ignoredSkillsLocations, Boolean enableSkills, Boolean hasSkillProvider, List<String> includedBuiltinSkills, List<String> disabledSkills, List<InstalledPlugin> installedPlugins, Boolean customAgentsLocalOnly, Boolean skipCustomInstructions, Boolean refreshCustomInstructions, List<String> disabledInstructionSources, Boolean coauthorEnabled, String trajectoryFile, Boolean enableStreaming, Boolean enableCitations, Boolean allowMultipleToolCalls, String copilotUrl, Boolean askUserDisabled, Boolean continueOnAutoMode, Boolean runningInInteractiveMode, Boolean enableOnDemandInstructionDiscovery, Long maxInlineBinaryBytes, ModelCapabilitiesOverride modelCapabilitiesOverrides, SessionLimitsConfig sessionLimits, AutopilotOptions autopilot, String agentContext, String eventsLogDirectory, Boolean eventsLogIncludesSubagents, String configDir, List<SessionOpenOptionsAdditionalContentExclusionPolicy> additionalContentExclusionPolicies, Boolean enableFileHooks, MemoryConfiguration memory, List<SessionCapability> sessionCapabilities) {
        this(sessionId, name, model, reasoningEffort, reasoningSummary, verbosity, clientName, authClientIdMetadataUrl, clientKind, lspClientName, integrationId, expAssignments, enableManagedSettings, excludedManagedSettings, enforceManagedModelDefaults, managedSettings, interactiveUser, enableFileChangeTracking, featureFlags, isExperimentalMode, authInfo, provider, capi, autoTierIsExplicit, providers, models, workingDirectory, additionalDirectories, workingDirectoryContext, remoteSteerable, remoteExporting, remoteDefaultedOn, detachedFromSpawningParentSessionId, detachedFromSpawningParentEngagementId, availableTools, excludedTools, includedBuiltinAgents, excludedBuiltinAgents, enableScriptSafety, shell, shellInitProfile, shellProcessFlags, sandboxConfig, sandboxConfigSource, logInteractiveShells, envValueMode, disabledMcpServers, managedMcpServers, allowAllMcpServerInstructions, skillDirectories, agentDirectories, instructionDirectories, hookDirectories, ignoredSkillsLocations, enableSkills, hasSkillProvider, includedBuiltinSkills, disabledSkills, installedPlugins, customAgentsLocalOnly, skipCustomInstructions, refreshCustomInstructions, disabledInstructionSources, coauthorEnabled, trajectoryFile, enableStreaming, enableCitations, allowMultipleToolCalls, copilotUrl, askUserDisabled, continueOnAutoMode, runningInInteractiveMode, enableOnDemandInstructionDiscovery, maxInlineBinaryBytes, modelCapabilitiesOverrides, sessionLimits, autopilot, agentContext, eventsLogDirectory, eventsLogIncludesSubagents, configDir, additionalContentExclusionPolicies, enableFileHooks, memory, sessionCapabilities, java.util.Set.of());
    }

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private String name;
        private String model;
        private String reasoningEffort;
        private SessionOpenOptionsReasoningSummary reasoningSummary;
        private Verbosity verbosity;
        private String clientName;
        private String authClientIdMetadataUrl;
        private String clientKind;
        private String lspClientName;
        private String integrationId;
        private Object expAssignments;
        private boolean $presence11;
        private Boolean enableManagedSettings;
        private List<String> excludedManagedSettings;
        private Boolean enforceManagedModelDefaults;
        private SessionManagedSettings managedSettings;
        private Boolean interactiveUser;
        private Boolean enableFileChangeTracking;
        private Map<String, Boolean> featureFlags;
        private Boolean isExperimentalMode;
        private Object authInfo;
        private ProviderConfig provider;
        private CapiSessionOptions capi;
        private Boolean autoTierIsExplicit;
        private List<NamedProviderConfig> providers;
        private List<ProviderModelConfig> models;
        private String workingDirectory;
        private List<String> additionalDirectories;
        private SessionContext workingDirectoryContext;
        private Boolean remoteSteerable;
        private Boolean remoteExporting;
        private Boolean remoteDefaultedOn;
        private String detachedFromSpawningParentSessionId;
        private String detachedFromSpawningParentEngagementId;
        private List<String> availableTools;
        private List<String> excludedTools;
        private List<String> includedBuiltinAgents;
        private List<String> excludedBuiltinAgents;
        private Boolean enableScriptSafety;
        private ShellOptions shell;
        private String shellInitProfile;
        private List<String> shellProcessFlags;
        private SandboxConfig sandboxConfig;
        private SandboxConfigSource sandboxConfigSource;
        private Boolean logInteractiveShells;
        private SessionOpenOptionsEnvValueMode envValueMode;
        private List<String> disabledMcpServers;
        private Map<String, ManagedMcpServerConfig> managedMcpServers;
        private Boolean allowAllMcpServerInstructions;
        private List<String> skillDirectories;
        private List<String> agentDirectories;
        private List<String> instructionDirectories;
        private List<String> hookDirectories;
        private List<String> ignoredSkillsLocations;
        private Boolean enableSkills;
        private Boolean hasSkillProvider;
        private List<String> includedBuiltinSkills;
        private List<String> disabledSkills;
        private List<InstalledPlugin> installedPlugins;
        private Boolean customAgentsLocalOnly;
        private Boolean skipCustomInstructions;
        private Boolean refreshCustomInstructions;
        private List<String> disabledInstructionSources;
        private Boolean coauthorEnabled;
        private String trajectoryFile;
        private Boolean enableStreaming;
        private Boolean enableCitations;
        private Boolean allowMultipleToolCalls;
        private String copilotUrl;
        private Boolean askUserDisabled;
        private Boolean continueOnAutoMode;
        private Boolean runningInInteractiveMode;
        private Boolean enableOnDemandInstructionDiscovery;
        private Long maxInlineBinaryBytes;
        private ModelCapabilitiesOverride modelCapabilitiesOverrides;
        private SessionLimitsConfig sessionLimits;
        private AutopilotOptions autopilot;
        private String agentContext;
        private String eventsLogDirectory;
        private Boolean eventsLogIncludesSubagents;
        private String configDir;
        private List<SessionOpenOptionsAdditionalContentExclusionPolicy> additionalContentExclusionPolicies;
        private Boolean enableFileHooks;
        private MemoryConfiguration memory;
        private List<SessionCapability> sessionCapabilities;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code reasoningSummary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningSummary(SessionOpenOptionsReasoningSummary value) {
            this.reasoningSummary = value;
            return this;
        }

        /**
         * Sets the {@code verbosity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder verbosity(Verbosity value) {
            this.verbosity = value;
            return this;
        }

        /**
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code authClientIdMetadataUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authClientIdMetadataUrl(String value) {
            this.authClientIdMetadataUrl = value;
            return this;
        }

        /**
         * Sets the {@code clientKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientKind(String value) {
            this.clientKind = value;
            return this;
        }

        /**
         * Sets the {@code lspClientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lspClientName(String value) {
            this.lspClientName = value;
            return this;
        }

        /**
         * Sets the {@code integrationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder integrationId(String value) {
            this.integrationId = value;
            return this;
        }

        /**
         * Sets the {@code expAssignments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expAssignments(Object value) {
            this.expAssignments = value;
            this.$presence11 = true;
            return this;
        }

        /**
         * Sets the {@code enableManagedSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableManagedSettings(Boolean value) {
            this.enableManagedSettings = value;
            return this;
        }

        /**
         * Sets the {@code excludedManagedSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder excludedManagedSettings(List<String> value) {
            this.excludedManagedSettings = value;
            return this;
        }

        /**
         * Sets the {@code enforceManagedModelDefaults} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enforceManagedModelDefaults(Boolean value) {
            this.enforceManagedModelDefaults = value;
            return this;
        }

        /**
         * Sets the {@code managedSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managedSettings(SessionManagedSettings value) {
            this.managedSettings = value;
            return this;
        }

        /**
         * Sets the {@code interactiveUser} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder interactiveUser(Boolean value) {
            this.interactiveUser = value;
            return this;
        }

        /**
         * Sets the {@code enableFileChangeTracking} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableFileChangeTracking(Boolean value) {
            this.enableFileChangeTracking = value;
            return this;
        }

        /**
         * Sets the {@code featureFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder featureFlags(Map<String, Boolean> value) {
            this.featureFlags = value;
            return this;
        }

        /**
         * Sets the {@code isExperimentalMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isExperimentalMode(Boolean value) {
            this.isExperimentalMode = value;
            return this;
        }

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authInfo(Object value) {
            this.authInfo = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provider(ProviderConfig value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code capi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capi(CapiSessionOptions value) {
            this.capi = value;
            return this;
        }

        /**
         * Sets the {@code autoTierIsExplicit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoTierIsExplicit(Boolean value) {
            this.autoTierIsExplicit = value;
            return this;
        }

        /**
         * Sets the {@code providers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providers(List<NamedProviderConfig> value) {
            this.providers = value;
            return this;
        }

        /**
         * Sets the {@code models} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder models(List<ProviderModelConfig> value) {
            this.models = value;
            return this;
        }

        /**
         * Sets the {@code workingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workingDirectory(String value) {
            this.workingDirectory = value;
            return this;
        }

        /**
         * Sets the {@code additionalDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalDirectories(List<String> value) {
            this.additionalDirectories = value;
            return this;
        }

        /**
         * Sets the {@code workingDirectoryContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workingDirectoryContext(SessionContext value) {
            this.workingDirectoryContext = value;
            return this;
        }

        /**
         * Sets the {@code remoteSteerable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteSteerable(Boolean value) {
            this.remoteSteerable = value;
            return this;
        }

        /**
         * Sets the {@code remoteExporting} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteExporting(Boolean value) {
            this.remoteExporting = value;
            return this;
        }

        /**
         * Sets the {@code remoteDefaultedOn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteDefaultedOn(Boolean value) {
            this.remoteDefaultedOn = value;
            return this;
        }

        /**
         * Sets the {@code detachedFromSpawningParentSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder detachedFromSpawningParentSessionId(String value) {
            this.detachedFromSpawningParentSessionId = value;
            return this;
        }

        /**
         * Sets the {@code detachedFromSpawningParentEngagementId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder detachedFromSpawningParentEngagementId(String value) {
            this.detachedFromSpawningParentEngagementId = value;
            return this;
        }

        /**
         * Sets the {@code availableTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availableTools(List<String> value) {
            this.availableTools = value;
            return this;
        }

        /**
         * Sets the {@code excludedTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder excludedTools(List<String> value) {
            this.excludedTools = value;
            return this;
        }

        /**
         * Sets the {@code includedBuiltinAgents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includedBuiltinAgents(List<String> value) {
            this.includedBuiltinAgents = value;
            return this;
        }

        /**
         * Sets the {@code excludedBuiltinAgents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder excludedBuiltinAgents(List<String> value) {
            this.excludedBuiltinAgents = value;
            return this;
        }

        /**
         * Sets the {@code enableScriptSafety} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableScriptSafety(Boolean value) {
            this.enableScriptSafety = value;
            return this;
        }

        /**
         * Sets the {@code shell} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shell(ShellOptions value) {
            this.shell = value;
            return this;
        }

        /**
         * Sets the {@code shellInitProfile} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shellInitProfile(String value) {
            this.shellInitProfile = value;
            return this;
        }

        /**
         * Sets the {@code shellProcessFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shellProcessFlags(List<String> value) {
            this.shellProcessFlags = value;
            return this;
        }

        /**
         * Sets the {@code sandboxConfig} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxConfig(SandboxConfig value) {
            this.sandboxConfig = value;
            return this;
        }

        /**
         * Sets the {@code sandboxConfigSource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sandboxConfigSource(SandboxConfigSource value) {
            this.sandboxConfigSource = value;
            return this;
        }

        /**
         * Sets the {@code logInteractiveShells} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder logInteractiveShells(Boolean value) {
            this.logInteractiveShells = value;
            return this;
        }

        /**
         * Sets the {@code envValueMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder envValueMode(SessionOpenOptionsEnvValueMode value) {
            this.envValueMode = value;
            return this;
        }

        /**
         * Sets the {@code disabledMcpServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disabledMcpServers(List<String> value) {
            this.disabledMcpServers = value;
            return this;
        }

        /**
         * Sets the {@code managedMcpServers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedMcpServers(Map<String, ManagedMcpServerConfig> value) {
            this.managedMcpServers = value;
            return this;
        }

        /**
         * Sets the {@code allowAllMcpServerInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowAllMcpServerInstructions(Boolean value) {
            this.allowAllMcpServerInstructions = value;
            return this;
        }

        /**
         * Sets the {@code skillDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skillDirectories(List<String> value) {
            this.skillDirectories = value;
            return this;
        }

        /**
         * Sets the {@code agentDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentDirectories(List<String> value) {
            this.agentDirectories = value;
            return this;
        }

        /**
         * Sets the {@code instructionDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder instructionDirectories(List<String> value) {
            this.instructionDirectories = value;
            return this;
        }

        /**
         * Sets the {@code hookDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hookDirectories(List<String> value) {
            this.hookDirectories = value;
            return this;
        }

        /**
         * Sets the {@code ignoredSkillsLocations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ignoredSkillsLocations(List<String> value) {
            this.ignoredSkillsLocations = value;
            return this;
        }

        /**
         * Sets the {@code enableSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableSkills(Boolean value) {
            this.enableSkills = value;
            return this;
        }

        /**
         * Sets the {@code hasSkillProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasSkillProvider(Boolean value) {
            this.hasSkillProvider = value;
            return this;
        }

        /**
         * Sets the {@code includedBuiltinSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includedBuiltinSkills(List<String> value) {
            this.includedBuiltinSkills = value;
            return this;
        }

        /**
         * Sets the {@code disabledSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disabledSkills(List<String> value) {
            this.disabledSkills = value;
            return this;
        }

        /**
         * Sets the {@code installedPlugins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedPlugins(List<InstalledPlugin> value) {
            this.installedPlugins = value;
            return this;
        }

        /**
         * Sets the {@code customAgentsLocalOnly} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder customAgentsLocalOnly(Boolean value) {
            this.customAgentsLocalOnly = value;
            return this;
        }

        /**
         * Sets the {@code skipCustomInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skipCustomInstructions(Boolean value) {
            this.skipCustomInstructions = value;
            return this;
        }

        /**
         * Sets the {@code refreshCustomInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder refreshCustomInstructions(Boolean value) {
            this.refreshCustomInstructions = value;
            return this;
        }

        /**
         * Sets the {@code disabledInstructionSources} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disabledInstructionSources(List<String> value) {
            this.disabledInstructionSources = value;
            return this;
        }

        /**
         * Sets the {@code coauthorEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder coauthorEnabled(Boolean value) {
            this.coauthorEnabled = value;
            return this;
        }

        /**
         * Sets the {@code trajectoryFile} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder trajectoryFile(String value) {
            this.trajectoryFile = value;
            return this;
        }

        /**
         * Sets the {@code enableStreaming} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableStreaming(Boolean value) {
            this.enableStreaming = value;
            return this;
        }

        /**
         * Sets the {@code enableCitations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableCitations(Boolean value) {
            this.enableCitations = value;
            return this;
        }

        /**
         * Sets the {@code allowMultipleToolCalls} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowMultipleToolCalls(Boolean value) {
            this.allowMultipleToolCalls = value;
            return this;
        }

        /**
         * Sets the {@code copilotUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotUrl(String value) {
            this.copilotUrl = value;
            return this;
        }

        /**
         * Sets the {@code askUserDisabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder askUserDisabled(Boolean value) {
            this.askUserDisabled = value;
            return this;
        }

        /**
         * Sets the {@code continueOnAutoMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder continueOnAutoMode(Boolean value) {
            this.continueOnAutoMode = value;
            return this;
        }

        /**
         * Sets the {@code runningInInteractiveMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runningInInteractiveMode(Boolean value) {
            this.runningInInteractiveMode = value;
            return this;
        }

        /**
         * Sets the {@code enableOnDemandInstructionDiscovery} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableOnDemandInstructionDiscovery(Boolean value) {
            this.enableOnDemandInstructionDiscovery = value;
            return this;
        }

        /**
         * Sets the {@code maxInlineBinaryBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxInlineBinaryBytes(Long value) {
            this.maxInlineBinaryBytes = value;
            return this;
        }

        /**
         * Sets the {@code modelCapabilitiesOverrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelCapabilitiesOverrides(ModelCapabilitiesOverride value) {
            this.modelCapabilitiesOverrides = value;
            return this;
        }

        /**
         * Sets the {@code sessionLimits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionLimits(SessionLimitsConfig value) {
            this.sessionLimits = value;
            return this;
        }

        /**
         * Sets the {@code autopilot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autopilot(AutopilotOptions value) {
            this.autopilot = value;
            return this;
        }

        /**
         * Sets the {@code agentContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentContext(String value) {
            this.agentContext = value;
            return this;
        }

        /**
         * Sets the {@code eventsLogDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eventsLogDirectory(String value) {
            this.eventsLogDirectory = value;
            return this;
        }

        /**
         * Sets the {@code eventsLogIncludesSubagents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eventsLogIncludesSubagents(Boolean value) {
            this.eventsLogIncludesSubagents = value;
            return this;
        }

        /**
         * Sets the {@code configDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configDir(String value) {
            this.configDir = value;
            return this;
        }

        /**
         * Sets the {@code additionalContentExclusionPolicies} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder additionalContentExclusionPolicies(List<SessionOpenOptionsAdditionalContentExclusionPolicy> value) {
            this.additionalContentExclusionPolicies = value;
            return this;
        }

        /**
         * Sets the {@code enableFileHooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableFileHooks(Boolean value) {
            this.enableFileHooks = value;
            return this;
        }

        /**
         * Sets the {@code memory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder memory(MemoryConfiguration value) {
            this.memory = value;
            return this;
        }

        /**
         * Sets the {@code sessionCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionCapabilities(List<SessionCapability> value) {
            this.sessionCapabilities = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionOpenOptions build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence11 && expAssignments == null) $explicitNulls.add("expAssignments");
            return new SessionOpenOptions(sessionId, name, model, reasoningEffort, reasoningSummary, verbosity, clientName, authClientIdMetadataUrl, clientKind, lspClientName, integrationId, expAssignments, enableManagedSettings, excludedManagedSettings, enforceManagedModelDefaults, managedSettings, interactiveUser, enableFileChangeTracking, featureFlags, isExperimentalMode, authInfo, provider, capi, autoTierIsExplicit, providers, models, workingDirectory, additionalDirectories, workingDirectoryContext, remoteSteerable, remoteExporting, remoteDefaultedOn, detachedFromSpawningParentSessionId, detachedFromSpawningParentEngagementId, availableTools, excludedTools, includedBuiltinAgents, excludedBuiltinAgents, enableScriptSafety, shell, shellInitProfile, shellProcessFlags, sandboxConfig, sandboxConfigSource, logInteractiveShells, envValueMode, disabledMcpServers, managedMcpServers, allowAllMcpServerInstructions, skillDirectories, agentDirectories, instructionDirectories, hookDirectories, ignoredSkillsLocations, enableSkills, hasSkillProvider, includedBuiltinSkills, disabledSkills, installedPlugins, customAgentsLocalOnly, skipCustomInstructions, refreshCustomInstructions, disabledInstructionSources, coauthorEnabled, trajectoryFile, enableStreaming, enableCitations, allowMultipleToolCalls, copilotUrl, askUserDisabled, continueOnAutoMode, runningInInteractiveMode, enableOnDemandInstructionDiscovery, maxInlineBinaryBytes, modelCapabilitiesOverrides, sessionLimits, autopilot, agentContext, eventsLogDirectory, eventsLogIncludesSubagents, configDir, additionalContentExclusionPolicies, enableFileHooks, memory, sessionCapabilities, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("expAssignments") && expAssignments == null) values.put("expAssignments", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionOpenOptions> {
        @Override
        public SessionOpenOptions deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionOpenOptions.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("expAssignments") && tree.get("expAssignments").isNull()) $explicitNulls.add("expAssignments");
            return new SessionOpenOptions(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<SessionOpenOptionsReasoningSummary>() {}), readProperty(context, tree, "verbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "clientName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "authClientIdMetadataUrl", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "clientKind", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "lspClientName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "integrationId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "expAssignments", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "enableManagedSettings", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "excludedManagedSettings", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "enforceManagedModelDefaults", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "managedSettings", new com.fasterxml.jackson.core.type.TypeReference<SessionManagedSettings>() {}), readProperty(context, tree, "interactiveUser", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableFileChangeTracking", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "featureFlags", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), readProperty(context, tree, "isExperimentalMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "authInfo", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "provider", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfig>() {}), readProperty(context, tree, "capi", new com.fasterxml.jackson.core.type.TypeReference<CapiSessionOptions>() {}), readProperty(context, tree, "autoTierIsExplicit", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "providers", new com.fasterxml.jackson.core.type.TypeReference<List<NamedProviderConfig>>() {}), readProperty(context, tree, "models", new com.fasterxml.jackson.core.type.TypeReference<List<ProviderModelConfig>>() {}), readProperty(context, tree, "workingDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "additionalDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "workingDirectoryContext", new com.fasterxml.jackson.core.type.TypeReference<SessionContext>() {}), readProperty(context, tree, "remoteSteerable", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "remoteExporting", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "remoteDefaultedOn", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "detachedFromSpawningParentSessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "detachedFromSpawningParentEngagementId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "availableTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "excludedTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "includedBuiltinAgents", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "excludedBuiltinAgents", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "enableScriptSafety", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "shell", new com.fasterxml.jackson.core.type.TypeReference<ShellOptions>() {}), readProperty(context, tree, "shellInitProfile", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "shellProcessFlags", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "sandboxConfig", new com.fasterxml.jackson.core.type.TypeReference<SandboxConfig>() {}), readProperty(context, tree, "sandboxConfigSource", new com.fasterxml.jackson.core.type.TypeReference<SandboxConfigSource>() {}), readProperty(context, tree, "logInteractiveShells", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "envValueMode", new com.fasterxml.jackson.core.type.TypeReference<SessionOpenOptionsEnvValueMode>() {}), readProperty(context, tree, "disabledMcpServers", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "managedMcpServers", new com.fasterxml.jackson.core.type.TypeReference<Map<String, ManagedMcpServerConfig>>() {}), readProperty(context, tree, "allowAllMcpServerInstructions", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "skillDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "agentDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "instructionDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "hookDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "ignoredSkillsLocations", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "enableSkills", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "hasSkillProvider", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "includedBuiltinSkills", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "disabledSkills", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "installedPlugins", new com.fasterxml.jackson.core.type.TypeReference<List<InstalledPlugin>>() {}), readProperty(context, tree, "customAgentsLocalOnly", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "skipCustomInstructions", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "refreshCustomInstructions", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "disabledInstructionSources", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "coauthorEnabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "trajectoryFile", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "enableStreaming", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableCitations", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "allowMultipleToolCalls", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "copilotUrl", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "askUserDisabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "continueOnAutoMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "runningInInteractiveMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableOnDemandInstructionDiscovery", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "maxInlineBinaryBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "modelCapabilitiesOverrides", new com.fasterxml.jackson.core.type.TypeReference<ModelCapabilitiesOverride>() {}), readProperty(context, tree, "sessionLimits", new com.fasterxml.jackson.core.type.TypeReference<SessionLimitsConfig>() {}), readProperty(context, tree, "autopilot", new com.fasterxml.jackson.core.type.TypeReference<AutopilotOptions>() {}), readProperty(context, tree, "agentContext", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "eventsLogDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "eventsLogIncludesSubagents", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "configDir", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "additionalContentExclusionPolicies", new com.fasterxml.jackson.core.type.TypeReference<List<SessionOpenOptionsAdditionalContentExclusionPolicy>>() {}), readProperty(context, tree, "enableFileHooks", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "memory", new com.fasterxml.jackson.core.type.TypeReference<MemoryConfiguration>() {}), readProperty(context, tree, "sessionCapabilities", new com.fasterxml.jackson.core.type.TypeReference<List<SessionCapability>>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param sessionId Optional stable session identifier to use for a new session.
     * @param name Optional human-friendly session name.
     * @param model Initial model identifier.
     * @param reasoningEffort Initial reasoning effort level. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied.
     * @param reasoningSummary Initial reasoning summary mode for supported model clients.
     * @param verbosity Initial output verbosity level for supported models.
     * @param clientName Identifier of the client driving the session.
     * @param authClientIdMetadataUrl OAuth Client ID Metadata Document URL used by this host for MCP authorization.
     * @param clientKind Structured client kind used for runtime behavior gates.
     * @param lspClientName Identifier sent to LSP-style integrations.
     * @param integrationId Stable integration identifier for analytics.
     * @param expAssignments ExP assignment ('flight') data injected by an SDK integrator, in the same JSON shape the Copilot CLI fetches from the experimentation service (CopilotExpAssignmentResponse). When supplied this is fed into the FeatureFlagService exactly like CLI-fetched assignments and ExP-backed flags wait for it. When absent the session does not block on ExP.
     * @param enableManagedSettings Opt-in: self-fetch and enforce enterprise managed settings, including managed hook policies, at session bootstrap.
     * @param excludedManagedSettings Managed-setting paths this trusted SDK host enforces externally or does not support, excluded from fetched, device, and policy-helper policy for this session. Exact dotted paths exclude a setting and its descendants (for example sandbox or permissions.disableBypassPermissionsMode); wildcards and array indices are not supported. Host-injected managedSettings and policy availability checks are not excluded. Not persisted; re-supply on resume, where omission restores full enforcement. Requires enableManagedSettings to self-fetch policy.
     * @param enforceManagedModelDefaults Opt in to enforcing non-overridable managed model controls on session model, Auto-tier, reasoning-effort, and context-tier changes. Managed defaults still apply when omitted; this option only turns conflicting changes into errors.
     * @param managedSettings Permissions-only enterprise policy injected by the SDK host at session create or resume. Composes restrictively with self-fetched and device policy and is not persisted.
     * @param interactiveUser Declares that a human drives this session's conversation, enabling user-message analysis (sentiment, task completion criteria, and task analytics) for user-authored messages. Automated messages remain excluded. Defaults to false for SDK sessions. Applied when the session is created or loaded from storage; ignored when resuming a session that is already active, and for cloud sessions.
     * @param enableFileChangeTracking Opt in to capturing file changes for session rewind and session diff. Capture cannot reconstruct changes made before it was enabled. On create it starts capture from the first turn. It is also honored on resume: for a session that already has tracked prior turns, tracking continues automatically even if this is omitted; passing it on resume additionally enables tracking for an eligible session that has no prior root turn yet. Resuming a session whose prior root turns were never tracked has no restorable baseline, so tracking stays disabled for it and rewind reports file change tracking as unavailable; the resume itself still succeeds, so sessions that predate tracking remain loadable. The opt-in is only rejected when the session can never track (a subagent session, or one without local session storage). It is intentionally absent from the mutable options update because enabling it after edits have occurred would create an incomplete, misleading baseline. Subagents share the parent session's capture store and are not tracked as separate rewind points: a file a subagent writes is attributed to whichever root user turn was open when the capture was staged, just before the tool body ran. A turn cannot open while a staged capture is still in flight, so a subagent tool that staged under the spawning turn stays attributed to it however late the write lands, while a capture it stages after the user's next message belongs to that later turn. Attribution decides which turn's rewind point counts and file preview include that write; it does not narrow which rewinds revert it, because a rewind restores every capture from the selected turn onward, so the earlier spawning turn reverts it as well.
     * @param featureFlags Feature-flag values resolved by the host.
     * @param isExperimentalMode Whether experimental behavior is enabled.
     * @param authInfo Initial authentication info for the session.
     * @param provider Custom model-provider configuration (BYOK).
     * @param capi Options scoped to the built-in CAPI (Copilot API) provider.
     * @param autoTierIsExplicit Whether a CLI host explicitly requested the initial Auto preference. False preserves a settings-derived preference without validating availability during creation; execution still validates it. Defaults to true and is ignored for non-CLI callers.
     * @param providers Named BYOK provider connections, additive to CAPI auth. Combining with `provider` is rejected.
     * @param models BYOK model definitions added to the selectable model list, each referencing a provider name.
     * @param workingDirectory Working directory to anchor the session.
     * @param additionalDirectories Additional directories the agent may access beyond the working directory. Each entry is granted to the session's file-access allow-list and surfaced to the model (system prompt context and `@`-mention completion). Conventional `.github/skills/` and `.github/agents/` definitions under each directory also join the session's project catalogs when their existing subsystem gates are enabled: added-root skills require both `enableConfigDiscovery` and effective `enableSkills`; added-root agents require `enableConfigDiscovery`. Supplying a directory therefore activates configuration from it and should be treated as a trust decision. Absolute paths are recommended; a relative path is resolved against the session's working directory. Nonexistent or unresolvable entries are skipped with a warning. This is applied during session creation and cold resume and is not persisted, so a cold resume must re-supply the directories.
     * @param workingDirectoryContext Pre-resolved working-directory context for session startup.
     * @param remoteSteerable Whether this session supports remote steering.
     * @param remoteExporting Telemetry-only remote exporting flag.
     * @param remoteDefaultedOn Telemetry-only remote-defaulted flag.
     * @param detachedFromSpawningParentSessionId Parent session ID for detached child telemetry rollup.
     * @param detachedFromSpawningParentEngagementId Parent engagement ID for detached child telemetry rollup.
     * @param availableTools Allowlist of available tool names.
     * @param excludedTools Denylist of tool names.
     * @param includedBuiltinAgents Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available.
     * @param excludedBuiltinAgents Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available.
     * @param enableScriptSafety Deprecated and ignored. Shell command analysis is always enabled; retained for wire compatibility.
     * @param shell Per-session settings for built-in shell tools.
     * @param shellInitProfile Use shell.initProfile instead. Shell init profile.
     * @param shellProcessFlags PowerShell process flags applied to built-in and user-requested shell commands.
     * @param sandboxConfig Resolved sandbox configuration.
     * @param sandboxConfigSource Origin of the sandbox choice. Settings-derived origins (never_configured, user_enabled, user_disabled, repository_policy) let managed policy floor a host preference; explicit below-floor changes remain policy conflicts unless a session opt-out is authorized. Also used for telemetry provenance.
     * @param logInteractiveShells Whether interactive shell sessions are logged.
     * @param envValueMode How MCP server environment values are interpreted.
     * @param disabledMcpServers MCP server names disabled for this session. Disabled servers are not started or authenticated on create or cold resume.
     * @param managedMcpServers Non-secret host-managed HTTP MCP servers keyed by stable managed identity. Managed provenance is runtime-established from this separate field and credentials are supplied through dynamic-header refresh.
     * @param allowAllMcpServerInstructions Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers.
     * @param skillDirectories Explicit discovery-only skill roots, not filesystem grants. Available without enableConfigDiscovery, subject to skill/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
     * @param agentDirectories Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without enableConfigDiscovery, subject to managed/source disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
     * @param instructionDirectories Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without enableConfigDiscovery, subject to instruction/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
     * @param hookDirectories Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without enableConfigDiscovery or enableFileHooks, which controls repository-origin hooks only; hook/managed disables still apply. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
     * @param ignoredSkillsLocations Skill scan directories and descendants excluded from discovery. Supports `~`-relative paths.
     * @param enableSkills Whether skill loading is enabled. When omitted, explicit SDK skill roots or an SDK skill provider enable skills independently of enableConfigDiscovery.
     * @param hasSkillProvider Whether the requesting SDK session has a skill provider. The provider remains ephemeral and is never persisted in session options or history. When enableSkills is false, it remains bound but dormant and receives no callbacks. Cloud, relay, handoff, and raw sessions.open flows reject it because they cannot safely pre-register the callback handler.
     * @param includedBuiltinSkills Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available.
     * @param disabledSkills Skill IDs disabled for this session.
     * @param installedPlugins Installed plugins visible to the session.
     * @param customAgentsLocalOnly Whether custom agents default to local-only execution.
     * @param skipCustomInstructions Whether to skip custom instruction sources.
     * @param refreshCustomInstructions Whether to invalidate cached custom-instruction discovery before constructing the session. Use when instruction files may have changed earlier in the same runtime process.
     * @param disabledInstructionSources Instruction source IDs disabled for this session.
     * @param coauthorEnabled Whether commit-message coauthor trailers are enabled.
     * @param trajectoryFile Optional trajectory output file path.
     * @param enableStreaming Whether model responses stream as delta events.
     * @param enableCitations Experimental: enable native model citations for supported Anthropic and OpenAI models, normalized onto the `assistant.message` event. Off by default; may change or be removed while the citations surface is experimental.
     * @param copilotUrl Override URL for the Copilot API endpoint.
     * @param askUserDisabled Whether ask_user is explicitly disabled.
     * @param continueOnAutoMode Whether auto-mode continuation is enabled.
     * @param runningInInteractiveMode Whether the host is an interactive UI.
     * @param enableOnDemandInstructionDiscovery Whether on-demand custom instruction discovery is enabled.
     * @param maxInlineBinaryBytes Maximum decoded byte size of a single inline model-facing binary tool result persisted in session events (default 10 MB).
     * @param modelCapabilitiesOverrides Initial model capability overrides.
     * @param sessionLimits Initial session limits.
     * @param autopilot Autopilot policy. Defaults to runtime-owned continuation with at most five automatic continuations. Does not change the session mode.
     * @param agentContext Runtime context discriminator for agent filtering.
     * @param eventsLogDirectory Override directory for session event logs.
     * @param eventsLogIncludesSubagents Whether subagent callback events should be forwarded into the session event log sink.
     * @param configDir Override Copilot configuration directory.
     * @param additionalContentExclusionPolicies Additional content-exclusion policies to merge into the session policy set.
     * @param enableFileHooks Live gate for repository-origin file hooks, including the primary repository, trusted repository-configured roots, and accepted startup additional directories (including CLI --add-dir). False suppresses these hooks; explicit hookDirectories and SDK callbacks are independent. Additional-root eligibility and the DISABLE_ADDITIONAL_DIRECTORY_HOOKS escape hatch are captured at create/cold resume, not expanded by later grants or feature-flag changes. Discovery uses containing Git roots, so supply only trusted repositories: commands and prompts need no further approval. Policy, hook-disable settings, and sandbox rules still apply. Provider-backed sessions skip repository hooks with a warning on omission, skip silently on false, and reject explicit true.
     * @param memory Memory configuration for this session.
     * @param sessionCapabilities Capabilities enabled for this session.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionOpenOptions(
        String sessionId,
        String name,
        String model,
        String reasoningEffort,
        SessionOpenOptionsReasoningSummary reasoningSummary,
        Verbosity verbosity,
        String clientName,
        String authClientIdMetadataUrl,
        String clientKind,
        String lspClientName,
        String integrationId,
        Object expAssignments,
        Boolean enableManagedSettings,
        List<String> excludedManagedSettings,
        Boolean enforceManagedModelDefaults,
        SessionManagedSettings managedSettings,
        Boolean interactiveUser,
        Boolean enableFileChangeTracking,
        Map<String, Boolean> featureFlags,
        Boolean isExperimentalMode,
        Object authInfo,
        ProviderConfig provider,
        CapiSessionOptions capi,
        Boolean autoTierIsExplicit,
        List<NamedProviderConfig> providers,
        List<ProviderModelConfig> models,
        String workingDirectory,
        List<String> additionalDirectories,
        SessionContext workingDirectoryContext,
        Boolean remoteSteerable,
        Boolean remoteExporting,
        Boolean remoteDefaultedOn,
        String detachedFromSpawningParentSessionId,
        String detachedFromSpawningParentEngagementId,
        List<String> availableTools,
        List<String> excludedTools,
        List<String> includedBuiltinAgents,
        List<String> excludedBuiltinAgents,
        Boolean enableScriptSafety,
        ShellOptions shell,
        String shellInitProfile,
        List<String> shellProcessFlags,
        SandboxConfig sandboxConfig,
        SandboxConfigSource sandboxConfigSource,
        Boolean logInteractiveShells,
        SessionOpenOptionsEnvValueMode envValueMode,
        List<String> disabledMcpServers,
        Map<String, ManagedMcpServerConfig> managedMcpServers,
        Boolean allowAllMcpServerInstructions,
        List<String> skillDirectories,
        List<String> agentDirectories,
        List<String> instructionDirectories,
        List<String> hookDirectories,
        List<String> ignoredSkillsLocations,
        Boolean enableSkills,
        Boolean hasSkillProvider,
        List<String> includedBuiltinSkills,
        List<String> disabledSkills,
        List<InstalledPlugin> installedPlugins,
        Boolean customAgentsLocalOnly,
        Boolean skipCustomInstructions,
        Boolean refreshCustomInstructions,
        List<String> disabledInstructionSources,
        Boolean coauthorEnabled,
        String trajectoryFile,
        Boolean enableStreaming,
        Boolean enableCitations,
        String copilotUrl,
        Boolean askUserDisabled,
        Boolean continueOnAutoMode,
        Boolean runningInInteractiveMode,
        Boolean enableOnDemandInstructionDiscovery,
        Long maxInlineBinaryBytes,
        ModelCapabilitiesOverride modelCapabilitiesOverrides,
        SessionLimitsConfig sessionLimits,
        AutopilotOptions autopilot,
        String agentContext,
        String eventsLogDirectory,
        Boolean eventsLogIncludesSubagents,
        String configDir,
        List<SessionOpenOptionsAdditionalContentExclusionPolicy> additionalContentExclusionPolicies,
        Boolean enableFileHooks,
        MemoryConfiguration memory,
        List<SessionCapability> sessionCapabilities
    ) {
        this(sessionId, name, model, reasoningEffort, reasoningSummary, verbosity, clientName, authClientIdMetadataUrl, clientKind, lspClientName, integrationId, expAssignments, enableManagedSettings, excludedManagedSettings, enforceManagedModelDefaults, managedSettings, interactiveUser, enableFileChangeTracking, featureFlags, isExperimentalMode, authInfo, provider, capi, autoTierIsExplicit, providers, models, workingDirectory, additionalDirectories, workingDirectoryContext, remoteSteerable, remoteExporting, remoteDefaultedOn, detachedFromSpawningParentSessionId, detachedFromSpawningParentEngagementId, availableTools, excludedTools, includedBuiltinAgents, excludedBuiltinAgents, enableScriptSafety, shell, shellInitProfile, shellProcessFlags, sandboxConfig, sandboxConfigSource, logInteractiveShells, envValueMode, disabledMcpServers, managedMcpServers, allowAllMcpServerInstructions, skillDirectories, agentDirectories, instructionDirectories, hookDirectories, ignoredSkillsLocations, enableSkills, hasSkillProvider, includedBuiltinSkills, disabledSkills, installedPlugins, customAgentsLocalOnly, skipCustomInstructions, refreshCustomInstructions, disabledInstructionSources, coauthorEnabled, trajectoryFile, enableStreaming, enableCitations, null, copilotUrl, askUserDisabled, continueOnAutoMode, runningInInteractiveMode, enableOnDemandInstructionDiscovery, maxInlineBinaryBytes, modelCapabilitiesOverrides, sessionLimits, autopilot, agentContext, eventsLogDirectory, eventsLogIncludesSubagents, configDir, additionalContentExclusionPolicies, enableFileHooks, memory, sessionCapabilities);
    }
}
