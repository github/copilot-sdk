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
public record SessionOptionsUpdateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The model ID to use for assistant turns. */
    @JsonProperty("model") String model,
    /** Reasoning effort for the selected model (model-defined enum). */
    @JsonProperty("reasoningEffort") String reasoningEffort,
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
    /** Custom model-provider configuration (BYOK). Opaque shape; see `ProviderConfig` in the runtime. */
    @JsonProperty("provider") Object provider,
    /** Absolute working-directory path for shell tools. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Allowlist of tool names available to this session. */
    @JsonProperty("availableTools") List<String> availableTools,
    /** Denylist of tool names for this session. */
    @JsonProperty("excludedTools") List<String> excludedTools,
    /** Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set. */
    @JsonProperty("toolFilterPrecedence") OptionsUpdateToolFilterPrecedence toolFilterPrecedence,
    /** Whether shell-script safety heuristics are enabled. */
    @JsonProperty("enableScriptSafety") Boolean enableScriptSafety,
    /** Shell init profile (`None` or `NonInteractive`). */
    @JsonProperty("shellInitProfile") String shellInitProfile,
    /** Per-shell process flags (e.g., `pwsh` arguments). */
    @JsonProperty("shellProcessFlags") List<String> shellProcessFlags,
    /** Sandbox configuration shape; opaque to SDK consumers. See `SandboxConfig` in the runtime. */
    @JsonProperty("sandboxConfig") Object sandboxConfig,
    /** Whether interactive shell sessions are logged. */
    @JsonProperty("logInteractiveShells") Boolean logInteractiveShells,
    /** How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch). */
    @JsonProperty("envValueMode") OptionsUpdateEnvValueMode envValueMode,
    /** Additional directories to search for skills. */
    @JsonProperty("skillDirectories") List<String> skillDirectories,
    /** Skill IDs that should be excluded from this session. */
    @JsonProperty("disabledSkills") List<String> disabledSkills,
    /** Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions` and the runtime-side `ON_DEMAND_INSTRUCTIONS` feature flag. */
    @JsonProperty("enableOnDemandInstructionDiscovery") Boolean enableOnDemandInstructionDiscovery,
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
    /** Additional content-exclusion policies to merge into the session's policy set. Opaque shape; see `ContentExclusionApiResponse` in the runtime. */
    @JsonProperty("additionalContentExclusionPolicies") List<Object> additionalContentExclusionPolicies,
    /** Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users). */
    @JsonProperty("manageScheduleEnabled") Boolean manageScheduleEnabled,
    /** Whether to skip embedding retrieval pipeline initialization and execution. */
    @JsonProperty("skipEmbeddingRetrieval") Boolean skipEmbeddingRetrieval,
    /** Organization-level custom instructions to inject into the system prompt. */
    @JsonProperty("organizationCustomInstructions") String organizationCustomInstructions,
    /** Whether to enable loading of `.github/hooks/` filesystem hooks. Separate from the SDK callback hook mechanism. */
    @JsonProperty("enableFileHooks") Boolean enableFileHooks,
    /** Whether to enable host git operations (context resolution, child repo scanning, git info in system prompt). */
    @JsonProperty("enableHostGitOperations") Boolean enableHostGitOperations,
    /** Whether to enable cross-session store writes and reads. */
    @JsonProperty("enableSessionStore") Boolean enableSessionStore,
    /** Whether to enable skill directory scanning and loading. Falls back to enableConfigDiscovery when unset. */
    @JsonProperty("enableSkills") Boolean enableSkills
) {
}
