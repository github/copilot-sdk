/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** Structured client kind used for runtime behavior gates. */
    @JsonProperty("clientKind") String clientKind,
    /** Identifier sent to LSP-style integrations. */
    @JsonProperty("lspClientName") String lspClientName,
    /** Stable integration identifier for analytics. */
    @JsonProperty("integrationId") String integrationId,
    /** ExP assignment ('flight') data injected by an SDK integrator, in the same JSON shape the Copilot CLI fetches from the experimentation service (CopilotExpAssignmentResponse). When supplied this is fed into the FeatureFlagService exactly like CLI-fetched assignments and ExP-backed flags wait for it. When absent the session does not block on ExP. */
    @JsonProperty("expAssignments") Object expAssignments,
    /** Opt-in: self-fetch and enforce enterprise managed settings at session bootstrap. */
    @JsonProperty("enableManagedSettings") Boolean enableManagedSettings,
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
    /** Named BYOK provider connections, additive to CAPI auth. Combining with `provider` is rejected. */
    @JsonProperty("providers") List<NamedProviderConfig> providers,
    /** BYOK model definitions added to the selectable model list, each referencing a provider name. */
    @JsonProperty("models") List<ProviderModelConfig> models,
    /** Working directory to anchor the session. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Additional directories the agent may access beyond the working directory. Each entry is granted to the session's file-access allow-list and surfaced to the model (system prompt context and `@`-mention completion). Absolute paths are recommended; a relative path is resolved against the session's working directory. Nonexistent or unresolvable entries are skipped with a warning. This is applied on both session creation and resume, and is not persisted: a resumed session that omits this option does not retain previously supplied directories (re-supply them, exactly as the CLI re-passes `--add-dir`). */
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
    /** Whether shell-script safety heuristics are enabled. */
    @JsonProperty("enableScriptSafety") Boolean enableScriptSafety,
    /** Per-session settings for built-in shell tools. */
    @JsonProperty("shell") ShellOptions shell,
    /** Use shell.initProfile instead. Shell init profile. */
    @JsonProperty("shellInitProfile") String shellInitProfile,
    /** PowerShell process flags applied to built-in and user-requested shell commands. */
    @JsonProperty("shellProcessFlags") List<String> shellProcessFlags,
    /** Resolved sandbox configuration. */
    @JsonProperty("sandboxConfig") SandboxConfig sandboxConfig,
    /** Whether interactive shell sessions are logged. */
    @JsonProperty("logInteractiveShells") Boolean logInteractiveShells,
    /** How MCP server environment values are interpreted. */
    @JsonProperty("envValueMode") SessionOpenOptionsEnvValueMode envValueMode,
    /** Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers. */
    @JsonProperty("allowAllMcpServerInstructions") Boolean allowAllMcpServerInstructions,
    /** Additional directories to search for skills. */
    @JsonProperty("skillDirectories") List<String> skillDirectories,
    /** Skill IDs disabled for this session. */
    @JsonProperty("disabledSkills") List<String> disabledSkills,
    /** Installed plugins visible to the session. */
    @JsonProperty("installedPlugins") List<InstalledPlugin> installedPlugins,
    /** Whether custom agents default to local-only execution. */
    @JsonProperty("customAgentsLocalOnly") Boolean customAgentsLocalOnly,
    /** Whether to skip custom instruction sources. */
    @JsonProperty("skipCustomInstructions") Boolean skipCustomInstructions,
    /** Instruction source IDs disabled for this session. */
    @JsonProperty("disabledInstructionSources") List<String> disabledInstructionSources,
    /** Whether commit-message coauthor trailers are enabled. */
    @JsonProperty("coauthorEnabled") Boolean coauthorEnabled,
    /** Optional trajectory output file path. */
    @JsonProperty("trajectoryFile") String trajectoryFile,
    /** Whether model responses stream as delta events. */
    @JsonProperty("enableStreaming") Boolean enableStreaming,
    /** Experimental: enable native model citations (Anthropic models today), normalized onto the `assistant.message` event. Off by default; may change or be removed while the citations surface is experimental. */
    @JsonProperty("enableCitations") Boolean enableCitations,
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
    /** Runtime context discriminator for agent filtering. */
    @JsonProperty("agentContext") String agentContext,
    /** Override directory for session event logs. */
    @JsonProperty("eventsLogDirectory") String eventsLogDirectory,
    /** Whether subagent callback events should be forwarded into the session event log sink. */
    @JsonProperty("eventsLogIncludesSubagents") Boolean eventsLogIncludesSubagents,
    /** Override Copilot configuration directory. */
    @JsonProperty("configDir") String configDir,
    /** Additional content-exclusion policies to merge into the session policy set. */
    @JsonProperty("additionalContentExclusionPolicies") List<SessionOpenOptionsAdditionalContentExclusionPolicy> additionalContentExclusionPolicies,
    /** Memory configuration for this session. */
    @JsonProperty("memory") MemoryConfiguration memory,
    /** Capabilities enabled for this session. */
    @JsonProperty("sessionCapabilities") List<SessionCapability> sessionCapabilities
) {
}
