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
 * Patch of mutable session options to apply to the running session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionOptionsUpdateParams.$NullableDeserializer.class)
public record SessionOptionsUpdateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Enable or disable the native image tool for this session without changing the conversation model.
Omitted values preserve the current setting. Re-supply consent on cold resume. */
    @JsonProperty("imageGeneration") ProtocolImageGenerationOptions imageGeneration,
    /** The model ID to use for assistant turns. */
    @JsonProperty("model") String model,
    /** Per-property model capability overrides for the selected model. */
    @JsonProperty("modelCapabilitiesOverrides") ModelCapabilitiesOverride modelCapabilitiesOverrides,
    /** Reasoning effort for the selected model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Reasoning summary mode for supported model clients. */
    @JsonProperty("reasoningSummary") OptionsUpdateReasoningSummary reasoningSummary,
    /** Output verbosity level for supported models. */
    @JsonProperty("verbosity") Verbosity verbosity,
    /** Identifier of the client driving the session. */
    @JsonProperty("clientName") String clientName,
    /** Identifier sent to LSP-style integrations. */
    @JsonProperty("lspClientName") String lspClientName,
    /** Stable integration identifier used for analytics and rate-limit attribution. */
    @JsonProperty("integrationId") String integrationId,
    /** Map of feature-flag IDs to their boolean enabled state. */
    @JsonProperty("featureFlags") Map<String, Boolean> featureFlags,
    /** Whether experimental capabilities are enabled. */
    @JsonProperty("isExperimentalMode") Boolean isExperimentalMode,
    /** Custom model-provider configuration (BYOK). */
    @JsonProperty("provider") ProviderConfig provider,
    /** Options scoped to the built-in CAPI (Copilot API) provider. */
    @JsonProperty("capi") CapiSessionOptions capi,
    /** Absolute working-directory path for shell tools. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Allowlist of tool names available to this session. */
    @JsonProperty("availableTools") List<String> availableTools,
    /** Denylist of tool names for this session. */
    @JsonProperty("excludedTools") List<String> excludedTools,
    /** Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available. Set to null to remove the allowlist restriction. */
    @JsonProperty("includedBuiltinAgents") List<String> includedBuiltinAgents,
    /** Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available. */
    @JsonProperty("excludedBuiltinAgents") List<String> excludedBuiltinAgents,
    /** Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set. */
    @JsonProperty("toolFilterPrecedence") OptionsUpdateToolFilterPrecedence toolFilterPrecedence,
    /** Deprecated and ignored. Shell command analysis is always enabled; retained for wire compatibility. */
    @Deprecated
    @JsonProperty("enableScriptSafety") Boolean enableScriptSafety,
    /** Per-session settings for built-in shell tools. */
    @JsonProperty("shell") ShellOptions shell,
    /** Use shell.initProfile instead. Shell init profile (`None` or `NonInteractive`). */
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
    /** How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch). */
    @JsonProperty("envValueMode") OptionsUpdateEnvValueMode envValueMode,
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
    /** Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available. Set to null to remove the allowlist restriction. */
    @JsonProperty("includedBuiltinSkills") List<String> includedBuiltinSkills,
    /** Skill IDs that should be excluded from this session. */
    @JsonProperty("disabledSkills") List<String> disabledSkills,
    /** Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions`. */
    @JsonProperty("enableOnDemandInstructionDiscovery") Boolean enableOnDemandInstructionDiscovery,
    /** Maximum decoded byte size of a single model-facing binary tool result (e.g. an image) persisted inline in session events and re-presented to the model on later turns / resume. Larger results are persisted as a metadata-only marker and shown to the model as a short text note. Defaults to 10 MB. */
    @JsonProperty("maxInlineBinaryBytes") Long maxInlineBinaryBytes,
    /** Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes. */
    @JsonProperty("installedPlugins") List<SessionInstalledPlugin> installedPlugins,
    /** Whether to default custom agents to local-only execution. */
    @JsonProperty("customAgentsLocalOnly") Boolean customAgentsLocalOnly,
    /** When true, the selected custom agent's prompt is not injected into the user message (skill context is still injected). Used by automation triggers where the agent prompt is already in the problem statement. */
    @JsonProperty("suppressCustomAgentPrompt") Boolean suppressCustomAgentPrompt,
    /** Whether to skip loading custom instruction sources. */
    @JsonProperty("skipCustomInstructions") Boolean skipCustomInstructions,
    /** Instruction source IDs to exclude from the system prompt. */
    @JsonProperty("disabledInstructionSources") List<String> disabledInstructionSources,
    /** Whether to include the `Co-authored-by` trailer in commit messages. */
    @JsonProperty("coauthorEnabled") Boolean coauthorEnabled,
    /** Optional path for trajectory output. */
    @JsonProperty("trajectoryFile") String trajectoryFile,
    /** Whether to stream model responses. */
    @JsonProperty("enableStreaming") Boolean enableStreaming,
    /** Override URL for the Copilot API endpoint. */
    @JsonProperty("copilotUrl") String copilotUrl,
    /** Whether to disable the `ask_user` tool (encourages autonomous behavior). */
    @JsonProperty("askUserDisabled") Boolean askUserDisabled,
    /** Whether to allow auto-mode continuation across turns. */
    @JsonProperty("continueOnAutoMode") Boolean continueOnAutoMode,
    /** Whether the session is running in an interactive UI. */
    @JsonProperty("runningInInteractiveMode") Boolean runningInInteractiveMode,
    /** Whether to surface reasoning-summary events from the model. */
    @JsonProperty("enableReasoningSummaries") Boolean enableReasoningSummaries,
    /** Runtime context discriminator (e.g., `cli`, `actions`). */
    @JsonProperty("agentContext") String agentContext,
    /** Override directory for the session-events log. When unset, the runtime's default events log directory is used. */
    @JsonProperty("eventsLogDirectory") String eventsLogDirectory,
    /** Whether subagent callback events should be forwarded into the session event log sink. */
    @JsonProperty("eventsLogIncludesSubagents") Boolean eventsLogIncludesSubagents,
    /** Additional content-exclusion policies to merge into the session's policy set. */
    @CopilotExperimental
    @JsonProperty("additionalContentExclusionPolicies") List<OptionsUpdateAdditionalContentExclusionPolicy> additionalContentExclusionPolicies,
    /** Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users). */
    @JsonProperty("manageScheduleEnabled") Boolean manageScheduleEnabled,
    /** Replaces the session's capability set with the given list. Use to enable or disable capabilities mid-session (e.g., remove `memory` for reproducible scripted runs). Omit the field to leave the existing capability set unchanged. */
    @JsonProperty("sessionCapabilities") List<SessionCapability> sessionCapabilities,
    /** Whether to skip embedding retrieval pipeline initialization and execution. */
    @JsonProperty("skipEmbeddingRetrieval") Boolean skipEmbeddingRetrieval,
    /** Organization-level custom instructions to inject into the system prompt. */
    @JsonProperty("organizationCustomInstructions") String organizationCustomInstructions,
    /** Whether to enable repository-origin file hooks from `.github/hooks/` and trusted repository-configured roots. Does not control explicit hookDirectories or SDK callback hooks. */
    @JsonProperty("enableFileHooks") Boolean enableFileHooks,
    /** Whether to enable host git operations (context resolution, child repo scanning, git info in system prompt). */
    @JsonProperty("enableHostGitOperations") Boolean enableHostGitOperations,
    /** Whether to enable cross-session store writes and reads. */
    @JsonProperty("enableSessionStore") Boolean enableSessionStore,
    /** Whether skill loading is enabled. Explicit false disables every source, including explicit SDK roots and a bound SDK provider; changing the value invalidates the loaded skill snapshot. When omitted, creation falls back to enableConfigDiscovery unless explicit SDK skill roots or an SDK skill provider are supplied. */
    @JsonProperty("enableSkills") Boolean enableSkills,
    /** Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier. */
    @JsonProperty("contextTier") OptionsUpdateContextTier contextTier,
    /** Optional session limits. Pass null to clear the session limits. */
    @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits,
    /** Patch the Autopilot policy. Omitted fields retain their current values; an empty object makes no changes. Does not change the session mode. */
    @JsonProperty("autopilot") AutopilotOptions autopilot,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionOptionsUpdateParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionOptionsUpdateParams(String sessionId, ProtocolImageGenerationOptions imageGeneration, String model, ModelCapabilitiesOverride modelCapabilitiesOverrides, String reasoningEffort, OptionsUpdateReasoningSummary reasoningSummary, Verbosity verbosity, String clientName, String lspClientName, String integrationId, Map<String, Boolean> featureFlags, Boolean isExperimentalMode, ProviderConfig provider, CapiSessionOptions capi, String workingDirectory, List<String> availableTools, List<String> excludedTools, List<String> includedBuiltinAgents, List<String> excludedBuiltinAgents, OptionsUpdateToolFilterPrecedence toolFilterPrecedence, Boolean enableScriptSafety, ShellOptions shell, String shellInitProfile, List<String> shellProcessFlags, SandboxConfig sandboxConfig, SandboxConfigSource sandboxConfigSource, Boolean logInteractiveShells, OptionsUpdateEnvValueMode envValueMode, Boolean allowAllMcpServerInstructions, List<String> skillDirectories, List<String> agentDirectories, List<String> instructionDirectories, List<String> hookDirectories, List<String> ignoredSkillsLocations, List<String> includedBuiltinSkills, List<String> disabledSkills, Boolean enableOnDemandInstructionDiscovery, Long maxInlineBinaryBytes, List<SessionInstalledPlugin> installedPlugins, Boolean customAgentsLocalOnly, Boolean suppressCustomAgentPrompt, Boolean skipCustomInstructions, List<String> disabledInstructionSources, Boolean coauthorEnabled, String trajectoryFile, Boolean enableStreaming, String copilotUrl, Boolean askUserDisabled, Boolean continueOnAutoMode, Boolean runningInInteractiveMode, Boolean enableReasoningSummaries, String agentContext, String eventsLogDirectory, Boolean eventsLogIncludesSubagents, List<OptionsUpdateAdditionalContentExclusionPolicy> additionalContentExclusionPolicies, Boolean manageScheduleEnabled, List<SessionCapability> sessionCapabilities, Boolean skipEmbeddingRetrieval, String organizationCustomInstructions, Boolean enableFileHooks, Boolean enableHostGitOperations, Boolean enableSessionStore, Boolean enableSkills, OptionsUpdateContextTier contextTier, SessionLimitsConfig sessionLimits, AutopilotOptions autopilot) {
        this(sessionId, imageGeneration, model, modelCapabilitiesOverrides, reasoningEffort, reasoningSummary, verbosity, clientName, lspClientName, integrationId, featureFlags, isExperimentalMode, provider, capi, workingDirectory, availableTools, excludedTools, includedBuiltinAgents, excludedBuiltinAgents, toolFilterPrecedence, enableScriptSafety, shell, shellInitProfile, shellProcessFlags, sandboxConfig, sandboxConfigSource, logInteractiveShells, envValueMode, allowAllMcpServerInstructions, skillDirectories, agentDirectories, instructionDirectories, hookDirectories, ignoredSkillsLocations, includedBuiltinSkills, disabledSkills, enableOnDemandInstructionDiscovery, maxInlineBinaryBytes, installedPlugins, customAgentsLocalOnly, suppressCustomAgentPrompt, skipCustomInstructions, disabledInstructionSources, coauthorEnabled, trajectoryFile, enableStreaming, copilotUrl, askUserDisabled, continueOnAutoMode, runningInInteractiveMode, enableReasoningSummaries, agentContext, eventsLogDirectory, eventsLogIncludesSubagents, additionalContentExclusionPolicies, manageScheduleEnabled, sessionCapabilities, skipEmbeddingRetrieval, organizationCustomInstructions, enableFileHooks, enableHostGitOperations, enableSessionStore, enableSkills, contextTier, sessionLimits, autopilot, java.util.Set.of());
    }

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
        private String sessionId;
        private ProtocolImageGenerationOptions imageGeneration;
        private String model;
        private ModelCapabilitiesOverride modelCapabilitiesOverrides;
        private String reasoningEffort;
        private OptionsUpdateReasoningSummary reasoningSummary;
        private Verbosity verbosity;
        private String clientName;
        private String lspClientName;
        private String integrationId;
        private Map<String, Boolean> featureFlags;
        private Boolean isExperimentalMode;
        private ProviderConfig provider;
        private CapiSessionOptions capi;
        private String workingDirectory;
        private List<String> availableTools;
        private List<String> excludedTools;
        private List<String> includedBuiltinAgents;
        private boolean $presence17;
        private List<String> excludedBuiltinAgents;
        private OptionsUpdateToolFilterPrecedence toolFilterPrecedence;
        private Boolean enableScriptSafety;
        private ShellOptions shell;
        private String shellInitProfile;
        private List<String> shellProcessFlags;
        private SandboxConfig sandboxConfig;
        private SandboxConfigSource sandboxConfigSource;
        private Boolean logInteractiveShells;
        private OptionsUpdateEnvValueMode envValueMode;
        private Boolean allowAllMcpServerInstructions;
        private List<String> skillDirectories;
        private List<String> agentDirectories;
        private List<String> instructionDirectories;
        private List<String> hookDirectories;
        private List<String> ignoredSkillsLocations;
        private List<String> includedBuiltinSkills;
        private boolean $presence34;
        private List<String> disabledSkills;
        private Boolean enableOnDemandInstructionDiscovery;
        private Long maxInlineBinaryBytes;
        private List<SessionInstalledPlugin> installedPlugins;
        private Boolean customAgentsLocalOnly;
        private Boolean suppressCustomAgentPrompt;
        private Boolean skipCustomInstructions;
        private List<String> disabledInstructionSources;
        private Boolean coauthorEnabled;
        private String trajectoryFile;
        private Boolean enableStreaming;
        private String copilotUrl;
        private Boolean askUserDisabled;
        private Boolean continueOnAutoMode;
        private Boolean runningInInteractiveMode;
        private Boolean enableReasoningSummaries;
        private String agentContext;
        private String eventsLogDirectory;
        private Boolean eventsLogIncludesSubagents;
        private List<OptionsUpdateAdditionalContentExclusionPolicy> additionalContentExclusionPolicies;
        private Boolean manageScheduleEnabled;
        private List<SessionCapability> sessionCapabilities;
        private Boolean skipEmbeddingRetrieval;
        private String organizationCustomInstructions;
        private Boolean enableFileHooks;
        private Boolean enableHostGitOperations;
        private Boolean enableSessionStore;
        private Boolean enableSkills;
        private OptionsUpdateContextTier contextTier;
        private SessionLimitsConfig sessionLimits;
        private boolean $presence64;
        private AutopilotOptions autopilot;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code imageGeneration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder imageGeneration(ProtocolImageGenerationOptions value) {
            this.imageGeneration = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code modelCapabilitiesOverrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelCapabilitiesOverrides(ModelCapabilitiesOverride value) {
            this.modelCapabilitiesOverrides = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder reasoningSummary(OptionsUpdateReasoningSummary value) {
            this.reasoningSummary = value;
            return this;
        }

        /**
         * Sets the {@code verbosity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code lspClientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder integrationId(String value) {
            this.integrationId = value;
            return this;
        }

        /**
         * Sets the {@code featureFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder isExperimentalMode(Boolean value) {
            this.isExperimentalMode = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder capi(CapiSessionOptions value) {
            this.capi = value;
            return this;
        }

        /**
         * Sets the {@code workingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder workingDirectory(String value) {
            this.workingDirectory = value;
            return this;
        }

        /**
         * Sets the {@code availableTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder includedBuiltinAgents(List<String> value) {
            this.includedBuiltinAgents = value;
            this.$presence17 = true;
            return this;
        }

        /**
         * Sets the {@code excludedBuiltinAgents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder excludedBuiltinAgents(List<String> value) {
            this.excludedBuiltinAgents = value;
            return this;
        }

        /**
         * Sets the {@code toolFilterPrecedence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolFilterPrecedence(OptionsUpdateToolFilterPrecedence value) {
            this.toolFilterPrecedence = value;
            return this;
        }

        /**
         * Sets the {@code enableScriptSafety} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder envValueMode(OptionsUpdateEnvValueMode value) {
            this.envValueMode = value;
            return this;
        }

        /**
         * Sets the {@code allowAllMcpServerInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder ignoredSkillsLocations(List<String> value) {
            this.ignoredSkillsLocations = value;
            return this;
        }

        /**
         * Sets the {@code includedBuiltinSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder includedBuiltinSkills(List<String> value) {
            this.includedBuiltinSkills = value;
            this.$presence34 = true;
            return this;
        }

        /**
         * Sets the {@code disabledSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disabledSkills(List<String> value) {
            this.disabledSkills = value;
            return this;
        }

        /**
         * Sets the {@code enableOnDemandInstructionDiscovery} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder maxInlineBinaryBytes(Long value) {
            this.maxInlineBinaryBytes = value;
            return this;
        }

        /**
         * Sets the {@code installedPlugins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder installedPlugins(List<SessionInstalledPlugin> value) {
            this.installedPlugins = value;
            return this;
        }

        /**
         * Sets the {@code customAgentsLocalOnly} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder customAgentsLocalOnly(Boolean value) {
            this.customAgentsLocalOnly = value;
            return this;
        }

        /**
         * Sets the {@code suppressCustomAgentPrompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder suppressCustomAgentPrompt(Boolean value) {
            this.suppressCustomAgentPrompt = value;
            return this;
        }

        /**
         * Sets the {@code skipCustomInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipCustomInstructions(Boolean value) {
            this.skipCustomInstructions = value;
            return this;
        }

        /**
         * Sets the {@code disabledInstructionSources} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder enableStreaming(Boolean value) {
            this.enableStreaming = value;
            return this;
        }

        /**
         * Sets the {@code copilotUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder runningInInteractiveMode(Boolean value) {
            this.runningInInteractiveMode = value;
            return this;
        }

        /**
         * Sets the {@code enableReasoningSummaries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableReasoningSummaries(Boolean value) {
            this.enableReasoningSummaries = value;
            return this;
        }

        /**
         * Sets the {@code agentContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder eventsLogIncludesSubagents(Boolean value) {
            this.eventsLogIncludesSubagents = value;
            return this;
        }

        /**
         * Sets the {@code additionalContentExclusionPolicies} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder additionalContentExclusionPolicies(List<OptionsUpdateAdditionalContentExclusionPolicy> value) {
            this.additionalContentExclusionPolicies = value;
            return this;
        }

        /**
         * Sets the {@code manageScheduleEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder manageScheduleEnabled(Boolean value) {
            this.manageScheduleEnabled = value;
            return this;
        }

        /**
         * Sets the {@code sessionCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionCapabilities(List<SessionCapability> value) {
            this.sessionCapabilities = value;
            return this;
        }

        /**
         * Sets the {@code skipEmbeddingRetrieval} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipEmbeddingRetrieval(Boolean value) {
            this.skipEmbeddingRetrieval = value;
            return this;
        }

        /**
         * Sets the {@code organizationCustomInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder organizationCustomInstructions(String value) {
            this.organizationCustomInstructions = value;
            return this;
        }

        /**
         * Sets the {@code enableFileHooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableFileHooks(Boolean value) {
            this.enableFileHooks = value;
            return this;
        }

        /**
         * Sets the {@code enableHostGitOperations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableHostGitOperations(Boolean value) {
            this.enableHostGitOperations = value;
            return this;
        }

        /**
         * Sets the {@code enableSessionStore} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableSessionStore(Boolean value) {
            this.enableSessionStore = value;
            return this;
        }

        /**
         * Sets the {@code enableSkills} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableSkills(Boolean value) {
            this.enableSkills = value;
            return this;
        }

        /**
         * Sets the {@code contextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contextTier(OptionsUpdateContextTier value) {
            this.contextTier = value;
            return this;
        }

        /**
         * Sets the {@code sessionLimits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionLimits(SessionLimitsConfig value) {
            this.sessionLimits = value;
            this.$presence64 = true;
            return this;
        }

        /**
         * Sets the {@code autopilot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder autopilot(AutopilotOptions value) {
            this.autopilot = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionOptionsUpdateParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence17 && includedBuiltinAgents == null) $explicitNulls.add("includedBuiltinAgents");
            if ($presence34 && includedBuiltinSkills == null) $explicitNulls.add("includedBuiltinSkills");
            if ($presence64 && sessionLimits == null) $explicitNulls.add("sessionLimits");
            return new SessionOptionsUpdateParams(sessionId, imageGeneration, model, modelCapabilitiesOverrides, reasoningEffort, reasoningSummary, verbosity, clientName, lspClientName, integrationId, featureFlags, isExperimentalMode, provider, capi, workingDirectory, availableTools, excludedTools, includedBuiltinAgents, excludedBuiltinAgents, toolFilterPrecedence, enableScriptSafety, shell, shellInitProfile, shellProcessFlags, sandboxConfig, sandboxConfigSource, logInteractiveShells, envValueMode, allowAllMcpServerInstructions, skillDirectories, agentDirectories, instructionDirectories, hookDirectories, ignoredSkillsLocations, includedBuiltinSkills, disabledSkills, enableOnDemandInstructionDiscovery, maxInlineBinaryBytes, installedPlugins, customAgentsLocalOnly, suppressCustomAgentPrompt, skipCustomInstructions, disabledInstructionSources, coauthorEnabled, trajectoryFile, enableStreaming, copilotUrl, askUserDisabled, continueOnAutoMode, runningInInteractiveMode, enableReasoningSummaries, agentContext, eventsLogDirectory, eventsLogIncludesSubagents, additionalContentExclusionPolicies, manageScheduleEnabled, sessionCapabilities, skipEmbeddingRetrieval, organizationCustomInstructions, enableFileHooks, enableHostGitOperations, enableSessionStore, enableSkills, contextTier, sessionLimits, autopilot, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("includedBuiltinAgents") && includedBuiltinAgents == null) values.put("includedBuiltinAgents", null);
        if ($explicitNulls.contains("includedBuiltinSkills") && includedBuiltinSkills == null) values.put("includedBuiltinSkills", null);
        if ($explicitNulls.contains("sessionLimits") && sessionLimits == null) values.put("sessionLimits", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionOptionsUpdateParams> {
        @Override
        public SessionOptionsUpdateParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionOptionsUpdateParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("includedBuiltinAgents") && tree.get("includedBuiltinAgents").isNull()) $explicitNulls.add("includedBuiltinAgents");
            if (tree.has("includedBuiltinSkills") && tree.get("includedBuiltinSkills").isNull()) $explicitNulls.add("includedBuiltinSkills");
            if (tree.has("sessionLimits") && tree.get("sessionLimits").isNull()) $explicitNulls.add("sessionLimits");
            return new SessionOptionsUpdateParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "imageGeneration", new com.fasterxml.jackson.core.type.TypeReference<ProtocolImageGenerationOptions>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "modelCapabilitiesOverrides", new com.fasterxml.jackson.core.type.TypeReference<ModelCapabilitiesOverride>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<OptionsUpdateReasoningSummary>() {}), readProperty(context, tree, "verbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "clientName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "lspClientName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "integrationId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "featureFlags", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), readProperty(context, tree, "isExperimentalMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "provider", new com.fasterxml.jackson.core.type.TypeReference<ProviderConfig>() {}), readProperty(context, tree, "capi", new com.fasterxml.jackson.core.type.TypeReference<CapiSessionOptions>() {}), readProperty(context, tree, "workingDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "availableTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "excludedTools", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "includedBuiltinAgents", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "excludedBuiltinAgents", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolFilterPrecedence", new com.fasterxml.jackson.core.type.TypeReference<OptionsUpdateToolFilterPrecedence>() {}), readProperty(context, tree, "enableScriptSafety", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "shell", new com.fasterxml.jackson.core.type.TypeReference<ShellOptions>() {}), readProperty(context, tree, "shellInitProfile", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "shellProcessFlags", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "sandboxConfig", new com.fasterxml.jackson.core.type.TypeReference<SandboxConfig>() {}), readProperty(context, tree, "sandboxConfigSource", new com.fasterxml.jackson.core.type.TypeReference<SandboxConfigSource>() {}), readProperty(context, tree, "logInteractiveShells", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "envValueMode", new com.fasterxml.jackson.core.type.TypeReference<OptionsUpdateEnvValueMode>() {}), readProperty(context, tree, "allowAllMcpServerInstructions", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "skillDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "agentDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "instructionDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "hookDirectories", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "ignoredSkillsLocations", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "includedBuiltinSkills", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "disabledSkills", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "enableOnDemandInstructionDiscovery", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "maxInlineBinaryBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "installedPlugins", new com.fasterxml.jackson.core.type.TypeReference<List<SessionInstalledPlugin>>() {}), readProperty(context, tree, "customAgentsLocalOnly", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "suppressCustomAgentPrompt", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "skipCustomInstructions", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "disabledInstructionSources", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "coauthorEnabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "trajectoryFile", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "enableStreaming", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "copilotUrl", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "askUserDisabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "continueOnAutoMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "runningInInteractiveMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableReasoningSummaries", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "agentContext", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "eventsLogDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "eventsLogIncludesSubagents", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "additionalContentExclusionPolicies", new com.fasterxml.jackson.core.type.TypeReference<List<OptionsUpdateAdditionalContentExclusionPolicy>>() {}), readProperty(context, tree, "manageScheduleEnabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sessionCapabilities", new com.fasterxml.jackson.core.type.TypeReference<List<SessionCapability>>() {}), readProperty(context, tree, "skipEmbeddingRetrieval", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "organizationCustomInstructions", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "enableFileHooks", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableHostGitOperations", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableSessionStore", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "enableSkills", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<OptionsUpdateContextTier>() {}), readProperty(context, tree, "sessionLimits", new com.fasterxml.jackson.core.type.TypeReference<SessionLimitsConfig>() {}), readProperty(context, tree, "autopilot", new com.fasterxml.jackson.core.type.TypeReference<AutopilotOptions>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
