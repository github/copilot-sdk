// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package rpc

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"time"
)

type RPCTypes struct {
	AccountGetQuotaResult                                            AccountGetQuotaResult                                            `json:"AccountGetQuotaResult"`
	AccountQuotaSnapshot                                             AccountQuotaSnapshot                                             `json:"AccountQuotaSnapshot"`
	AgentDeselectResult                                              AgentDeselectResult                                              `json:"AgentDeselectResult"`
	AgentGetCurrentResult                                            AgentGetCurrentResult                                            `json:"AgentGetCurrentResult"`
	AgentInfo                                                        AgentInfo                                                        `json:"AgentInfo"`
	AgentList                                                        AgentList                                                        `json:"AgentList"`
	AgentReloadResult                                                AgentReloadResult                                                `json:"AgentReloadResult"`
	AgentSelectRequest                                               AgentSelectRequest                                               `json:"AgentSelectRequest"`
	AgentSelectResult                                                AgentSelectResult                                                `json:"AgentSelectResult"`
	CommandsHandlePendingCommandRequest                              CommandsHandlePendingCommandRequest                              `json:"CommandsHandlePendingCommandRequest"`
	CommandsHandlePendingCommandResult                               CommandsHandlePendingCommandResult                               `json:"CommandsHandlePendingCommandResult"`
	CurrentModel                                                     CurrentModel                                                     `json:"CurrentModel"`
	DiscoveredMCPServer                                              DiscoveredMCPServer                                              `json:"DiscoveredMcpServer"`
	DiscoveredMCPServerSource                                        MCPServerSource                                                  `json:"DiscoveredMcpServerSource"`
	DiscoveredMCPServerType                                          DiscoveredMCPServerType                                          `json:"DiscoveredMcpServerType"`
	Extension                                                        Extension                                                        `json:"Extension"`
	ExtensionList                                                    ExtensionList                                                    `json:"ExtensionList"`
	ExtensionsDisableRequest                                         ExtensionsDisableRequest                                         `json:"ExtensionsDisableRequest"`
	ExtensionsDisableResult                                          ExtensionsDisableResult                                          `json:"ExtensionsDisableResult"`
	ExtensionsEnableRequest                                          ExtensionsEnableRequest                                          `json:"ExtensionsEnableRequest"`
	ExtensionsEnableResult                                           ExtensionsEnableResult                                           `json:"ExtensionsEnableResult"`
	ExtensionSource                                                  ExtensionSource                                                  `json:"ExtensionSource"`
	ExtensionsReloadResult                                           ExtensionsReloadResult                                           `json:"ExtensionsReloadResult"`
	ExtensionStatus                                                  ExtensionStatus                                                  `json:"ExtensionStatus"`
	FilterMapping                                                    *FilterMapping                                                   `json:"FilterMapping"`
	FilterMappingString                                              FilterMappingString                                              `json:"FilterMappingString"`
	FilterMappingValue                                               FilterMappingString                                              `json:"FilterMappingValue"`
	FleetStartRequest                                                FleetStartRequest                                                `json:"FleetStartRequest"`
	FleetStartResult                                                 FleetStartResult                                                 `json:"FleetStartResult"`
	HandleToolCallResult                                             HandleToolCallResult                                             `json:"HandleToolCallResult"`
	HistoryCompactContextWindow                                      HistoryCompactContextWindow                                      `json:"HistoryCompactContextWindow"`
	HistoryCompactResult                                             HistoryCompactResult                                             `json:"HistoryCompactResult"`
	HistoryTruncateRequest                                           HistoryTruncateRequest                                           `json:"HistoryTruncateRequest"`
	HistoryTruncateResult                                            HistoryTruncateResult                                            `json:"HistoryTruncateResult"`
	InstructionsGetSourcesResult                                     InstructionsGetSourcesResult                                     `json:"InstructionsGetSourcesResult"`
	InstructionsSources                                              InstructionsSources                                              `json:"InstructionsSources"`
	InstructionsSourcesLocation                                      InstructionsSourcesLocation                                      `json:"InstructionsSourcesLocation"`
	InstructionsSourcesType                                          InstructionsSourcesType                                          `json:"InstructionsSourcesType"`
	LogRequest                                                       LogRequest                                                       `json:"LogRequest"`
	LogResult                                                        LogResult                                                        `json:"LogResult"`
	MCPConfigAddRequest                                              MCPConfigAddRequest                                              `json:"McpConfigAddRequest"`
	MCPConfigAddResult                                               MCPConfigAddResult                                               `json:"McpConfigAddResult"`
	MCPConfigList                                                    MCPConfigList                                                    `json:"McpConfigList"`
	MCPConfigRemoveRequest                                           MCPConfigRemoveRequest                                           `json:"McpConfigRemoveRequest"`
	MCPConfigRemoveResult                                            MCPConfigRemoveResult                                            `json:"McpConfigRemoveResult"`
	MCPConfigUpdateRequest                                           MCPConfigUpdateRequest                                           `json:"McpConfigUpdateRequest"`
	MCPConfigUpdateResult                                            MCPConfigUpdateResult                                            `json:"McpConfigUpdateResult"`
	MCPDisableRequest                                                MCPDisableRequest                                                `json:"McpDisableRequest"`
	MCPDisableResult                                                 MCPDisableResult                                                 `json:"McpDisableResult"`
	MCPDiscoverRequest                                               MCPDiscoverRequest                                               `json:"McpDiscoverRequest"`
	MCPDiscoverResult                                                MCPDiscoverResult                                                `json:"McpDiscoverResult"`
	MCPEnableRequest                                                 MCPEnableRequest                                                 `json:"McpEnableRequest"`
	MCPEnableResult                                                  MCPEnableResult                                                  `json:"McpEnableResult"`
	MCPReloadResult                                                  MCPReloadResult                                                  `json:"McpReloadResult"`
	MCPServer                                                        MCPServer                                                        `json:"McpServer"`
	MCPServerConfig                                                  MCPServerConfig                                                  `json:"McpServerConfig"`
	MCPServerConfigHTTP                                              MCPServerConfigHTTP                                              `json:"McpServerConfigHttp"`
	MCPServerConfigHTTPType                                          MCPServerConfigHTTPType                                          `json:"McpServerConfigHttpType"`
	MCPServerConfigLocal                                             MCPServerConfigLocal                                             `json:"McpServerConfigLocal"`
	MCPServerConfigLocalType                                         MCPServerConfigLocalType                                         `json:"McpServerConfigLocalType"`
	MCPServerList                                                    MCPServerList                                                    `json:"McpServerList"`
	MCPServerSource                                                  MCPServerSource                                                  `json:"McpServerSource"`
	MCPServerStatus                                                  MCPServerStatus                                                  `json:"McpServerStatus"`
	Model                                                            ModelElement                                                     `json:"Model"`
	ModelBilling                                                     ModelBilling                                                     `json:"ModelBilling"`
	ModelCapabilities                                                ModelCapabilities                                                `json:"ModelCapabilities"`
	ModelCapabilitiesLimits                                          ModelCapabilitiesLimits                                          `json:"ModelCapabilitiesLimits"`
	ModelCapabilitiesLimitsVision                                    ModelCapabilitiesLimitsVision                                    `json:"ModelCapabilitiesLimitsVision"`
	ModelCapabilitiesOverride                                        ModelCapabilitiesOverride                                        `json:"ModelCapabilitiesOverride"`
	ModelCapabilitiesOverrideLimits                                  ModelCapabilitiesOverrideLimits                                  `json:"ModelCapabilitiesOverrideLimits"`
	ModelCapabilitiesOverrideLimitsVision                            ModelCapabilitiesOverrideLimitsVision                            `json:"ModelCapabilitiesOverrideLimitsVision"`
	ModelCapabilitiesOverrideSupports                                ModelCapabilitiesOverrideSupports                                `json:"ModelCapabilitiesOverrideSupports"`
	ModelCapabilitiesSupports                                        ModelCapabilitiesSupports                                        `json:"ModelCapabilitiesSupports"`
	ModelList                                                        ModelList                                                        `json:"ModelList"`
	ModelPolicy                                                      ModelPolicy                                                      `json:"ModelPolicy"`
	ModelSwitchToRequest                                             ModelSwitchToRequest                                             `json:"ModelSwitchToRequest"`
	ModelSwitchToResult                                              ModelSwitchToResult                                              `json:"ModelSwitchToResult"`
	ModeSetRequest                                                   ModeSetRequest                                                   `json:"ModeSetRequest"`
	ModeSetResult                                                    ModeSetResult                                                    `json:"ModeSetResult"`
	NameGetResult                                                    NameGetResult                                                    `json:"NameGetResult"`
	NameSetRequest                                                   NameSetRequest                                                   `json:"NameSetRequest"`
	NameSetResult                                                    NameSetResult                                                    `json:"NameSetResult"`
	PermissionDecision                                               PermissionDecision                                               `json:"PermissionDecision"`
	PermissionDecisionApproved                                       PermissionDecisionApproved                                       `json:"PermissionDecisionApproved"`
	PermissionDecisionDeniedByContentExclusionPolicy                 PermissionDecisionDeniedByContentExclusionPolicy                 `json:"PermissionDecisionDeniedByContentExclusionPolicy"`
	PermissionDecisionDeniedByPermissionRequestHook                  PermissionDecisionDeniedByPermissionRequestHook                  `json:"PermissionDecisionDeniedByPermissionRequestHook"`
	PermissionDecisionDeniedByRules                                  PermissionDecisionDeniedByRules                                  `json:"PermissionDecisionDeniedByRules"`
	PermissionDecisionDeniedInteractivelyByUser                      PermissionDecisionDeniedInteractivelyByUser                      `json:"PermissionDecisionDeniedInteractivelyByUser"`
	PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser `json:"PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser"`
	PermissionDecisionRequest                                        PermissionDecisionRequest                                        `json:"PermissionDecisionRequest"`
	PermissionRequestResult                                          PermissionRequestResult                                          `json:"PermissionRequestResult"`
	PingRequest                                                      PingRequest                                                      `json:"PingRequest"`
	PingResult                                                       PingResult                                                       `json:"PingResult"`
	PlanDeleteResult                                                 PlanDeleteResult                                                 `json:"PlanDeleteResult"`
	PlanReadResult                                                   PlanReadResult                                                   `json:"PlanReadResult"`
	PlanUpdateRequest                                                PlanUpdateRequest                                                `json:"PlanUpdateRequest"`
	PlanUpdateResult                                                 PlanUpdateResult                                                 `json:"PlanUpdateResult"`
	Plugin                                                           PluginElement                                                    `json:"Plugin"`
	PluginList                                                       PluginList                                                       `json:"PluginList"`
	ServerSkill                                                      ServerSkill                                                      `json:"ServerSkill"`
	ServerSkillList                                                  ServerSkillList                                                  `json:"ServerSkillList"`
	SessionFSAppendFileRequest                                       SessionFSAppendFileRequest                                       `json:"SessionFsAppendFileRequest"`
	SessionFSError                                                   SessionFSError                                                   `json:"SessionFsError"`
	SessionFSErrorCode                                               SessionFSErrorCode                                               `json:"SessionFsErrorCode"`
	SessionFSExistsRequest                                           SessionFSExistsRequest                                           `json:"SessionFsExistsRequest"`
	SessionFSExistsResult                                            SessionFSExistsResult                                            `json:"SessionFsExistsResult"`
	SessionFSMkdirRequest                                            SessionFSMkdirRequest                                            `json:"SessionFsMkdirRequest"`
	SessionFSReaddirRequest                                          SessionFSReaddirRequest                                          `json:"SessionFsReaddirRequest"`
	SessionFSReaddirResult                                           SessionFSReaddirResult                                           `json:"SessionFsReaddirResult"`
	SessionFSReaddirWithTypesEntry                                   SessionFSReaddirWithTypesEntry                                   `json:"SessionFsReaddirWithTypesEntry"`
	SessionFSReaddirWithTypesEntryType                               SessionFSReaddirWithTypesEntryType                               `json:"SessionFsReaddirWithTypesEntryType"`
	SessionFSReaddirWithTypesRequest                                 SessionFSReaddirWithTypesRequest                                 `json:"SessionFsReaddirWithTypesRequest"`
	SessionFSReaddirWithTypesResult                                  SessionFSReaddirWithTypesResult                                  `json:"SessionFsReaddirWithTypesResult"`
	SessionFSReadFileRequest                                         SessionFSReadFileRequest                                         `json:"SessionFsReadFileRequest"`
	SessionFSReadFileResult                                          SessionFSReadFileResult                                          `json:"SessionFsReadFileResult"`
	SessionFSRenameRequest                                           SessionFSRenameRequest                                           `json:"SessionFsRenameRequest"`
	SessionFSRmRequest                                               SessionFSRmRequest                                               `json:"SessionFsRmRequest"`
	SessionFSSetProviderConventions                                  SessionFSSetProviderConventions                                  `json:"SessionFsSetProviderConventions"`
	SessionFSSetProviderRequest                                      SessionFSSetProviderRequest                                      `json:"SessionFsSetProviderRequest"`
	SessionFSSetProviderResult                                       SessionFSSetProviderResult                                       `json:"SessionFsSetProviderResult"`
	SessionFSStatRequest                                             SessionFSStatRequest                                             `json:"SessionFsStatRequest"`
	SessionFSStatResult                                              SessionFSStatResult                                              `json:"SessionFsStatResult"`
	SessionFSWriteFileRequest                                        SessionFSWriteFileRequest                                        `json:"SessionFsWriteFileRequest"`
	SessionLogLevel                                                  SessionLogLevel                                                  `json:"SessionLogLevel"`
	SessionMode                                                      SessionMode                                                      `json:"SessionMode"`
	SessionsForkRequest                                              SessionsForkRequest                                              `json:"SessionsForkRequest"`
	SessionsForkResult                                               SessionsForkResult                                               `json:"SessionsForkResult"`
	ShellExecRequest                                                 ShellExecRequest                                                 `json:"ShellExecRequest"`
	ShellExecResult                                                  ShellExecResult                                                  `json:"ShellExecResult"`
	ShellKillRequest                                                 ShellKillRequest                                                 `json:"ShellKillRequest"`
	ShellKillResult                                                  ShellKillResult                                                  `json:"ShellKillResult"`
	ShellKillSignal                                                  ShellKillSignal                                                  `json:"ShellKillSignal"`
	Skill                                                            Skill                                                            `json:"Skill"`
	SkillList                                                        SkillList                                                        `json:"SkillList"`
	SkillsConfigSetDisabledSkillsRequest                             SkillsConfigSetDisabledSkillsRequest                             `json:"SkillsConfigSetDisabledSkillsRequest"`
	SkillsConfigSetDisabledSkillsResult                              SkillsConfigSetDisabledSkillsResult                              `json:"SkillsConfigSetDisabledSkillsResult"`
	SkillsDisableRequest                                             SkillsDisableRequest                                             `json:"SkillsDisableRequest"`
	SkillsDisableResult                                              SkillsDisableResult                                              `json:"SkillsDisableResult"`
	SkillsDiscoverRequest                                            SkillsDiscoverRequest                                            `json:"SkillsDiscoverRequest"`
	SkillsEnableRequest                                              SkillsEnableRequest                                              `json:"SkillsEnableRequest"`
	SkillsEnableResult                                               SkillsEnableResult                                               `json:"SkillsEnableResult"`
	SkillsReloadResult                                               SkillsReloadResult                                               `json:"SkillsReloadResult"`
	Tool                                                             Tool                                                             `json:"Tool"`
	ToolCallResult                                                   ToolCallResult                                                   `json:"ToolCallResult"`
	ToolList                                                         ToolList                                                         `json:"ToolList"`
	ToolsHandlePendingToolCall                                       *ToolsHandlePendingToolCall                                      `json:"ToolsHandlePendingToolCall"`
	ToolsHandlePendingToolCallRequest                                ToolsHandlePendingToolCallRequest                                `json:"ToolsHandlePendingToolCallRequest"`
	ToolsListRequest                                                 ToolsListRequest                                                 `json:"ToolsListRequest"`
	UIElicitationArrayAnyOfField                                     UIElicitationArrayAnyOfField                                     `json:"UIElicitationArrayAnyOfField"`
	UIElicitationArrayAnyOfFieldItems                                UIElicitationArrayAnyOfFieldItems                                `json:"UIElicitationArrayAnyOfFieldItems"`
	UIElicitationArrayAnyOfFieldItemsAnyOf                           UIElicitationArrayAnyOfFieldItemsAnyOf                           `json:"UIElicitationArrayAnyOfFieldItemsAnyOf"`
	UIElicitationArrayEnumField                                      UIElicitationArrayEnumField                                      `json:"UIElicitationArrayEnumField"`
	UIElicitationArrayEnumFieldItems                                 UIElicitationArrayEnumFieldItems                                 `json:"UIElicitationArrayEnumFieldItems"`
	UIElicitationFieldValue                                          *UIElicitationFieldValue                                         `json:"UIElicitationFieldValue"`
	UIElicitationRequest                                             UIElicitationRequest                                             `json:"UIElicitationRequest"`
	UIElicitationResponse                                            UIElicitationResponse                                            `json:"UIElicitationResponse"`
	UIElicitationResponseAction                                      UIElicitationResponseAction                                      `json:"UIElicitationResponseAction"`
	UIElicitationResponseContent                                     map[string]*UIElicitationFieldValue                              `json:"UIElicitationResponseContent"`
	UIElicitationResult                                              UIElicitationResult                                              `json:"UIElicitationResult"`
	UIElicitationSchema                                              UIElicitationSchema                                              `json:"UIElicitationSchema"`
	UIElicitationSchemaProperty                                      UIElicitationSchemaProperty                                      `json:"UIElicitationSchemaProperty"`
	UIElicitationSchemaPropertyBoolean                               UIElicitationSchemaPropertyBoolean                               `json:"UIElicitationSchemaPropertyBoolean"`
	UIElicitationSchemaPropertyNumber                                UIElicitationSchemaPropertyNumber                                `json:"UIElicitationSchemaPropertyNumber"`
	UIElicitationSchemaPropertyNumberType                            UIElicitationSchemaPropertyNumberTypeEnum                        `json:"UIElicitationSchemaPropertyNumberType"`
	UIElicitationSchemaPropertyString                                UIElicitationSchemaPropertyString                                `json:"UIElicitationSchemaPropertyString"`
	UIElicitationSchemaPropertyStringFormat                          UIElicitationSchemaPropertyStringFormat                          `json:"UIElicitationSchemaPropertyStringFormat"`
	UIElicitationStringEnumField                                     UIElicitationStringEnumField                                     `json:"UIElicitationStringEnumField"`
	UIElicitationStringOneOfField                                    UIElicitationStringOneOfField                                    `json:"UIElicitationStringOneOfField"`
	UIElicitationStringOneOfFieldOneOf                               UIElicitationStringOneOfFieldOneOf                               `json:"UIElicitationStringOneOfFieldOneOf"`
	UIHandlePendingElicitationRequest                                UIHandlePendingElicitationRequest                                `json:"UIHandlePendingElicitationRequest"`
	UsageGetMetricsResult                                            UsageGetMetricsResult                                            `json:"UsageGetMetricsResult"`
	UsageMetricsCodeChanges                                          UsageMetricsCodeChanges                                          `json:"UsageMetricsCodeChanges"`
	UsageMetricsModelMetric                                          UsageMetricsModelMetric                                          `json:"UsageMetricsModelMetric"`
	UsageMetricsModelMetricRequests                                  UsageMetricsModelMetricRequests                                  `json:"UsageMetricsModelMetricRequests"`
	UsageMetricsModelMetricUsage                                     UsageMetricsModelMetricUsage                                     `json:"UsageMetricsModelMetricUsage"`
	WorkspacesCreateFileRequest                                      WorkspacesCreateFileRequest                                      `json:"WorkspacesCreateFileRequest"`
	WorkspacesCreateFileResult                                       WorkspacesCreateFileResult                                       `json:"WorkspacesCreateFileResult"`
	WorkspacesGetWorkspaceResult                                     WorkspacesGetWorkspaceResult                                     `json:"WorkspacesGetWorkspaceResult"`
	WorkspacesListFilesResult                                        WorkspacesListFilesResult                                        `json:"WorkspacesListFilesResult"`
	WorkspacesReadFileRequest                                        WorkspacesReadFileRequest                                        `json:"WorkspacesReadFileRequest"`
	WorkspacesReadFileResult                                         WorkspacesReadFileResult                                         `json:"WorkspacesReadFileResult"`
}

type AccountGetQuotaResult struct {
	// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
	QuotaSnapshots map[string]AccountQuotaSnapshot `json:"quotaSnapshots"`
}

type AccountQuotaSnapshot struct {
	// Number of requests included in the entitlement
	EntitlementRequests int64 `json:"entitlementRequests"`
	// Whether the user has an unlimited usage entitlement
	IsUnlimitedEntitlement bool `json:"isUnlimitedEntitlement"`
	// Number of overage requests made this period
	Overage float64 `json:"overage"`
	// Whether overage is allowed when quota is exhausted
	OverageAllowedWithExhaustedQuota bool `json:"overageAllowedWithExhaustedQuota"`
	// Percentage of entitlement remaining
	RemainingPercentage float64 `json:"remainingPercentage"`
	// Date when the quota resets (ISO 8601 string)
	ResetDate *string `json:"resetDate,omitempty"`
	// Whether usage is still permitted after quota exhaustion
	UsageAllowedWithExhaustedQuota bool `json:"usageAllowedWithExhaustedQuota"`
	// Number of requests used so far this period
	UsedRequests int64 `json:"usedRequests"`
}

// Experimental: AgentDeselectResult is part of an experimental API and may change or be removed.
type AgentDeselectResult struct {
}

// Experimental: AgentGetCurrentResult is part of an experimental API and may change or be removed.
type AgentGetCurrentResult struct {
	// Currently selected custom agent, or null if using the default agent
	Agent *AgentInfo `json:"agent"`
}

// The newly selected custom agent
type AgentInfo struct {
	// Description of the agent's purpose
	Description string `json:"description"`
	// Human-readable display name
	DisplayName string `json:"displayName"`
	// Unique identifier of the custom agent
	Name string `json:"name"`
}

// Experimental: AgentList is part of an experimental API and may change or be removed.
type AgentList struct {
	// Available custom agents
	Agents []AgentInfo `json:"agents"`
}

// Experimental: AgentReloadResult is part of an experimental API and may change or be removed.
type AgentReloadResult struct {
	// Reloaded custom agents
	Agents []AgentInfo `json:"agents"`
}

// Experimental: AgentSelectRequest is part of an experimental API and may change or be removed.
type AgentSelectRequest struct {
	// Name of the custom agent to select
	Name string `json:"name"`
}

// Experimental: AgentSelectResult is part of an experimental API and may change or be removed.
type AgentSelectResult struct {
	// The newly selected custom agent
	Agent AgentInfo `json:"agent"`
}

type CommandsHandlePendingCommandRequest struct {
	// Error message if the command handler failed
	Error *string `json:"error,omitempty"`
	// Request ID from the command invocation event
	RequestID string `json:"requestId"`
}

type CommandsHandlePendingCommandResult struct {
	// Whether the command was handled successfully
	Success bool `json:"success"`
}

type CurrentModel struct {
	// Currently active model identifier
	ModelID *string `json:"modelId,omitempty"`
}

type DiscoveredMCPServer struct {
	// Whether the server is enabled (not in the disabled list)
	Enabled bool `json:"enabled"`
	// Server name (config key)
	Name string `json:"name"`
	// Configuration source
	Source MCPServerSource `json:"source"`
	// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
	Type *DiscoveredMCPServerType `json:"type,omitempty"`
}

type Extension struct {
	// Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')
	ID string `json:"id"`
	// Extension name (directory name)
	Name string `json:"name"`
	// Process ID if the extension is running
	PID *int64 `json:"pid,omitempty"`
	// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
	Source ExtensionSource `json:"source"`
	// Current status: running, disabled, failed, or starting
	Status ExtensionStatus `json:"status"`
}

// Experimental: ExtensionList is part of an experimental API and may change or be removed.
type ExtensionList struct {
	// Discovered extensions and their current status
	Extensions []Extension `json:"extensions"`
}

// Experimental: ExtensionsDisableRequest is part of an experimental API and may change or be removed.
type ExtensionsDisableRequest struct {
	// Source-qualified extension ID to disable
	ID string `json:"id"`
}

// Experimental: ExtensionsDisableResult is part of an experimental API and may change or be removed.
type ExtensionsDisableResult struct {
}

// Experimental: ExtensionsEnableRequest is part of an experimental API and may change or be removed.
type ExtensionsEnableRequest struct {
	// Source-qualified extension ID to enable
	ID string `json:"id"`
}

// Experimental: ExtensionsEnableResult is part of an experimental API and may change or be removed.
type ExtensionsEnableResult struct {
}

// Experimental: ExtensionsReloadResult is part of an experimental API and may change or be removed.
type ExtensionsReloadResult struct {
}

// Experimental: FleetStartRequest is part of an experimental API and may change or be removed.
type FleetStartRequest struct {
	// Optional user prompt to combine with fleet instructions
	Prompt *string `json:"prompt,omitempty"`
}

// Experimental: FleetStartResult is part of an experimental API and may change or be removed.
type FleetStartResult struct {
	// Whether fleet mode was successfully activated
	Started bool `json:"started"`
}

type HandleToolCallResult struct {
	// Whether the tool call result was handled successfully
	Success bool `json:"success"`
}

// Post-compaction context window usage breakdown
type HistoryCompactContextWindow struct {
	// Token count from non-system messages (user, assistant, tool)
	ConversationTokens *int64 `json:"conversationTokens,omitempty"`
	// Current total tokens in the context window (system + conversation + tool definitions)
	CurrentTokens int64 `json:"currentTokens"`
	// Current number of messages in the conversation
	MessagesLength int64 `json:"messagesLength"`
	// Token count from system message(s)
	SystemTokens *int64 `json:"systemTokens,omitempty"`
	// Maximum token count for the model's context window
	TokenLimit int64 `json:"tokenLimit"`
	// Token count from tool definitions
	ToolDefinitionsTokens *int64 `json:"toolDefinitionsTokens,omitempty"`
}

// Experimental: HistoryCompactResult is part of an experimental API and may change or be removed.
type HistoryCompactResult struct {
	// Post-compaction context window usage breakdown
	ContextWindow *HistoryCompactContextWindow `json:"contextWindow,omitempty"`
	// Number of messages removed during compaction
	MessagesRemoved int64 `json:"messagesRemoved"`
	// Whether compaction completed successfully
	Success bool `json:"success"`
	// Number of tokens freed by compaction
	TokensRemoved int64 `json:"tokensRemoved"`
}

// Experimental: HistoryTruncateRequest is part of an experimental API and may change or be removed.
type HistoryTruncateRequest struct {
	// Event ID to truncate to. This event and all events after it are removed from the session.
	EventID string `json:"eventId"`
}

// Experimental: HistoryTruncateResult is part of an experimental API and may change or be removed.
type HistoryTruncateResult struct {
	// Number of events that were removed
	EventsRemoved int64 `json:"eventsRemoved"`
}

type InstructionsGetSourcesResult struct {
	// Instruction sources for the session
	Sources []InstructionsSources `json:"sources"`
}

type InstructionsSources struct {
	// Glob pattern from frontmatter — when set, this instruction applies only to matching files
	ApplyTo *string `json:"applyTo,omitempty"`
	// Raw content of the instruction file
	Content string `json:"content"`
	// Short description (body after frontmatter) for use in instruction tables
	Description *string `json:"description,omitempty"`
	// Unique identifier for this source (used for toggling)
	ID string `json:"id"`
	// Human-readable label
	Label string `json:"label"`
	// Where this source lives — used for UI grouping
	Location InstructionsSourcesLocation `json:"location"`
	// File path relative to repo or absolute for home
	SourcePath string `json:"sourcePath"`
	// Category of instruction source — used for merge logic
	Type InstructionsSourcesType `json:"type"`
}

type LogRequest struct {
	// When true, the message is transient and not persisted to the session event log on disk
	Ephemeral *bool `json:"ephemeral,omitempty"`
	// Log severity level. Determines how the message is displayed in the timeline. Defaults to
	// "info".
	Level *SessionLogLevel `json:"level,omitempty"`
	// Human-readable message
	Message string `json:"message"`
	// Optional URL the user can open in their browser for more details
	URL *string `json:"url,omitempty"`
}

type LogResult struct {
	// The unique identifier of the emitted session event
	EventID string `json:"eventId"`
}

type MCPConfigAddRequest struct {
	// MCP server configuration (local/stdio or remote/http)
	Config MCPServerConfig `json:"config"`
	// Unique name for the MCP server
	Name string `json:"name"`
}

// MCP server configuration (local/stdio or remote/http)
type MCPServerConfig struct {
	Args            []string          `json:"args,omitempty"`
	Command         *string           `json:"command,omitempty"`
	Cwd             *string           `json:"cwd,omitempty"`
	Env             map[string]string `json:"env,omitempty"`
	FilterMapping   *FilterMapping    `json:"filterMapping"`
	IsDefaultServer *bool             `json:"isDefaultServer,omitempty"`
	// Timeout in milliseconds for tool calls to this server.
	Timeout *int64 `json:"timeout,omitempty"`
	// Tools to include. Defaults to all tools if not specified.
	Tools []string `json:"tools,omitempty"`
	// Remote transport type. Defaults to "http" when omitted.
	Type              *MCPServerConfigType `json:"type,omitempty"`
	Headers           map[string]string    `json:"headers,omitempty"`
	OauthClientID     *string              `json:"oauthClientId,omitempty"`
	OauthPublicClient *bool                `json:"oauthPublicClient,omitempty"`
	URL               *string              `json:"url,omitempty"`
}

type MCPConfigAddResult struct {
}

type MCPConfigList struct {
	// All MCP servers from user config, keyed by name
	Servers map[string]MCPServerConfig `json:"servers"`
}

type MCPConfigRemoveRequest struct {
	// Name of the MCP server to remove
	Name string `json:"name"`
}

type MCPConfigRemoveResult struct {
}

type MCPConfigUpdateRequest struct {
	// MCP server configuration (local/stdio or remote/http)
	Config MCPServerConfig `json:"config"`
	// Name of the MCP server to update
	Name string `json:"name"`
}

type MCPConfigUpdateResult struct {
}

type MCPDisableRequest struct {
	// Name of the MCP server to disable
	ServerName string `json:"serverName"`
}

type MCPDisableResult struct {
}

type MCPDiscoverRequest struct {
	// Working directory used as context for discovery (e.g., plugin resolution)
	WorkingDirectory *string `json:"workingDirectory,omitempty"`
}

type MCPDiscoverResult struct {
	// MCP servers discovered from all sources
	Servers []DiscoveredMCPServer `json:"servers"`
}

type MCPEnableRequest struct {
	// Name of the MCP server to enable
	ServerName string `json:"serverName"`
}

type MCPEnableResult struct {
}

type MCPReloadResult struct {
}

type MCPServer struct {
	// Error message if the server failed to connect
	Error *string `json:"error,omitempty"`
	// Server name (config key)
	Name string `json:"name"`
	// Configuration source: user, workspace, plugin, or builtin
	Source *MCPServerSource `json:"source,omitempty"`
	// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
	Status MCPServerStatus `json:"status"`
}

type MCPServerConfigHTTP struct {
	FilterMapping     *FilterMapping    `json:"filterMapping"`
	Headers           map[string]string `json:"headers,omitempty"`
	IsDefaultServer   *bool             `json:"isDefaultServer,omitempty"`
	OauthClientID     *string           `json:"oauthClientId,omitempty"`
	OauthPublicClient *bool             `json:"oauthPublicClient,omitempty"`
	// Timeout in milliseconds for tool calls to this server.
	Timeout *int64 `json:"timeout,omitempty"`
	// Tools to include. Defaults to all tools if not specified.
	Tools []string `json:"tools,omitempty"`
	// Remote transport type. Defaults to "http" when omitted.
	Type *MCPServerConfigHTTPType `json:"type,omitempty"`
	URL  string                   `json:"url"`
}

type MCPServerConfigLocal struct {
	Args            []string          `json:"args"`
	Command         string            `json:"command"`
	Cwd             *string           `json:"cwd,omitempty"`
	Env             map[string]string `json:"env,omitempty"`
	FilterMapping   *FilterMapping    `json:"filterMapping"`
	IsDefaultServer *bool             `json:"isDefaultServer,omitempty"`
	// Timeout in milliseconds for tool calls to this server.
	Timeout *int64 `json:"timeout,omitempty"`
	// Tools to include. Defaults to all tools if not specified.
	Tools []string                  `json:"tools,omitempty"`
	Type  *MCPServerConfigLocalType `json:"type,omitempty"`
}

type MCPServerList struct {
	// Configured MCP servers
	Servers []MCPServer `json:"servers"`
}

type ModeSetRequest struct {
	// The agent mode. Valid values: "interactive", "plan", "autopilot".
	Mode SessionMode `json:"mode"`
}

type ModeSetResult struct {
}

type ModelElement struct {
	// Billing information
	Billing *ModelBilling `json:"billing,omitempty"`
	// Model capabilities and limits
	Capabilities ModelCapabilities `json:"capabilities"`
	// Default reasoning effort level (only present if model supports reasoning effort)
	DefaultReasoningEffort *string `json:"defaultReasoningEffort,omitempty"`
	// Model identifier (e.g., "claude-sonnet-4.5")
	ID string `json:"id"`
	// Display name
	Name string `json:"name"`
	// Policy state (if applicable)
	Policy *ModelPolicy `json:"policy,omitempty"`
	// Supported reasoning effort levels (only present if model supports reasoning effort)
	SupportedReasoningEfforts []string `json:"supportedReasoningEfforts,omitempty"`
}

// Billing information
type ModelBilling struct {
	// Billing cost multiplier relative to the base rate
	Multiplier float64 `json:"multiplier"`
}

// Model capabilities and limits
type ModelCapabilities struct {
	// Token limits for prompts, outputs, and context window
	Limits *ModelCapabilitiesLimits `json:"limits,omitempty"`
	// Feature flags indicating what the model supports
	Supports *ModelCapabilitiesSupports `json:"supports,omitempty"`
}

// Token limits for prompts, outputs, and context window
type ModelCapabilitiesLimits struct {
	// Maximum total context window size in tokens
	MaxContextWindowTokens *int64 `json:"max_context_window_tokens,omitempty"`
	// Maximum number of output/completion tokens
	MaxOutputTokens *int64 `json:"max_output_tokens,omitempty"`
	// Maximum number of prompt/input tokens
	MaxPromptTokens *int64 `json:"max_prompt_tokens,omitempty"`
	// Vision-specific limits
	Vision *ModelCapabilitiesLimitsVision `json:"vision,omitempty"`
}

// Vision-specific limits
type ModelCapabilitiesLimitsVision struct {
	// Maximum image size in bytes
	MaxPromptImageSize int64 `json:"max_prompt_image_size"`
	// Maximum number of images per prompt
	MaxPromptImages int64 `json:"max_prompt_images"`
	// MIME types the model accepts
	SupportedMediaTypes []string `json:"supported_media_types"`
}

// Feature flags indicating what the model supports
type ModelCapabilitiesSupports struct {
	// Whether this model supports reasoning effort configuration
	ReasoningEffort *bool `json:"reasoningEffort,omitempty"`
	// Whether this model supports vision/image input
	Vision *bool `json:"vision,omitempty"`
}

// Policy state (if applicable)
type ModelPolicy struct {
	// Current policy state for this model
	State string `json:"state"`
	// Usage terms or conditions for this model
	Terms string `json:"terms"`
}

// Override individual model capabilities resolved by the runtime
type ModelCapabilitiesOverride struct {
	// Token limits for prompts, outputs, and context window
	Limits *ModelCapabilitiesOverrideLimits `json:"limits,omitempty"`
	// Feature flags indicating what the model supports
	Supports *ModelCapabilitiesOverrideSupports `json:"supports,omitempty"`
}

// Token limits for prompts, outputs, and context window
type ModelCapabilitiesOverrideLimits struct {
	// Maximum total context window size in tokens
	MaxContextWindowTokens *int64                                 `json:"max_context_window_tokens,omitempty"`
	MaxOutputTokens        *int64                                 `json:"max_output_tokens,omitempty"`
	MaxPromptTokens        *int64                                 `json:"max_prompt_tokens,omitempty"`
	Vision                 *ModelCapabilitiesOverrideLimitsVision `json:"vision,omitempty"`
}

type ModelCapabilitiesOverrideLimitsVision struct {
	// Maximum image size in bytes
	MaxPromptImageSize *int64 `json:"max_prompt_image_size,omitempty"`
	// Maximum number of images per prompt
	MaxPromptImages *int64 `json:"max_prompt_images,omitempty"`
	// MIME types the model accepts
	SupportedMediaTypes []string `json:"supported_media_types,omitempty"`
}

// Feature flags indicating what the model supports
type ModelCapabilitiesOverrideSupports struct {
	ReasoningEffort *bool `json:"reasoningEffort,omitempty"`
	Vision          *bool `json:"vision,omitempty"`
}

type ModelList struct {
	// List of available models with full metadata
	Models []ModelElement `json:"models"`
}

type ModelSwitchToRequest struct {
	// Override individual model capabilities resolved by the runtime
	ModelCapabilities *ModelCapabilitiesOverride `json:"modelCapabilities,omitempty"`
	// Model identifier to switch to
	ModelID string `json:"modelId"`
	// Reasoning effort level to use for the model
	ReasoningEffort *string `json:"reasoningEffort,omitempty"`
}

type ModelSwitchToResult struct {
	// Currently active model identifier after the switch
	ModelID *string `json:"modelId,omitempty"`
}

type NameGetResult struct {
	// The session name, falling back to the auto-generated summary, or null if neither exists
	Name *string `json:"name"`
}

type NameSetRequest struct {
	// New session name (1–100 characters, trimmed of leading/trailing whitespace)
	Name string `json:"name"`
}

type NameSetResult struct {
}

type PermissionDecision struct {
	// The permission request was approved
	//
	// Denied because approval rules explicitly blocked it
	//
	// Denied because no approval rule matched and user confirmation was unavailable
	//
	// Denied by the user during an interactive prompt
	//
	// Denied by the organization's content exclusion policy
	//
	// Denied by a permission request hook registered by an extension or plugin
	Kind PermissionDecisionKind `json:"kind"`
	// Rules that denied the request
	Rules []any `json:"rules,omitempty"`
	// Optional feedback from the user explaining the denial
	Feedback *string `json:"feedback,omitempty"`
	// Human-readable explanation of why the path was excluded
	//
	// Optional message from the hook explaining the denial
	Message *string `json:"message,omitempty"`
	// File path that triggered the exclusion
	Path *string `json:"path,omitempty"`
	// Whether to interrupt the current agent turn
	Interrupt *bool `json:"interrupt,omitempty"`
}

type PermissionDecisionApproved struct {
	// The permission request was approved
	Kind PermissionDecisionApprovedKind `json:"kind"`
}

type PermissionDecisionDeniedByContentExclusionPolicy struct {
	// Denied by the organization's content exclusion policy
	Kind PermissionDecisionDeniedByContentExclusionPolicyKind `json:"kind"`
	// Human-readable explanation of why the path was excluded
	Message string `json:"message"`
	// File path that triggered the exclusion
	Path string `json:"path"`
}

type PermissionDecisionDeniedByPermissionRequestHook struct {
	// Whether to interrupt the current agent turn
	Interrupt *bool `json:"interrupt,omitempty"`
	// Denied by a permission request hook registered by an extension or plugin
	Kind PermissionDecisionDeniedByPermissionRequestHookKind `json:"kind"`
	// Optional message from the hook explaining the denial
	Message *string `json:"message,omitempty"`
}

type PermissionDecisionDeniedByRules struct {
	// Denied because approval rules explicitly blocked it
	Kind PermissionDecisionDeniedByRulesKind `json:"kind"`
	// Rules that denied the request
	Rules []any `json:"rules"`
}

type PermissionDecisionDeniedInteractivelyByUser struct {
	// Optional feedback from the user explaining the denial
	Feedback *string `json:"feedback,omitempty"`
	// Denied by the user during an interactive prompt
	Kind PermissionDecisionDeniedInteractivelyByUserKind `json:"kind"`
}

type PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser struct {
	// Denied because no approval rule matched and user confirmation was unavailable
	Kind PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind `json:"kind"`
}

type PermissionDecisionRequest struct {
	// Request ID of the pending permission request
	RequestID string             `json:"requestId"`
	Result    PermissionDecision `json:"result"`
}

type PermissionRequestResult struct {
	// Whether the permission request was handled successfully
	Success bool `json:"success"`
}

type PingRequest struct {
	// Optional message to echo back
	Message *string `json:"message,omitempty"`
}

type PingResult struct {
	// Echoed message (or default greeting)
	Message string `json:"message"`
	// Server protocol version number
	ProtocolVersion int64 `json:"protocolVersion"`
	// Server timestamp in milliseconds
	Timestamp int64 `json:"timestamp"`
}

type PlanDeleteResult struct {
}

type PlanReadResult struct {
	// The content of the plan file, or null if it does not exist
	Content *string `json:"content"`
	// Whether the plan file exists in the workspace
	Exists bool `json:"exists"`
	// Absolute file path of the plan file, or null if workspace is not enabled
	Path *string `json:"path"`
}

type PlanUpdateRequest struct {
	// The new content for the plan file
	Content string `json:"content"`
}

type PlanUpdateResult struct {
}

type PluginElement struct {
	// Whether the plugin is currently enabled
	Enabled bool `json:"enabled"`
	// Marketplace the plugin came from
	Marketplace string `json:"marketplace"`
	// Plugin name
	Name string `json:"name"`
	// Installed version
	Version *string `json:"version,omitempty"`
}

// Experimental: PluginList is part of an experimental API and may change or be removed.
type PluginList struct {
	// Installed plugins
	Plugins []PluginElement `json:"plugins"`
}

type ServerSkill struct {
	// Description of what the skill does
	Description string `json:"description"`
	// Whether the skill is currently enabled (based on global config)
	Enabled bool `json:"enabled"`
	// Unique identifier for the skill
	Name string `json:"name"`
	// Absolute path to the skill file
	Path *string `json:"path,omitempty"`
	// The project path this skill belongs to (only for project/inherited skills)
	ProjectPath *string `json:"projectPath,omitempty"`
	// Source location type (e.g., project, personal-copilot, plugin, builtin)
	Source string `json:"source"`
	// Whether the skill can be invoked by the user as a slash command
	UserInvocable bool `json:"userInvocable"`
}

type ServerSkillList struct {
	// All discovered skills across all sources
	Skills []ServerSkill `json:"skills"`
}

type SessionFSAppendFileRequest struct {
	// Content to append
	Content string `json:"content"`
	// Optional POSIX-style mode for newly created files
	Mode *int64 `json:"mode,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

// Describes a filesystem error.
type SessionFSError struct {
	// Error classification
	Code SessionFSErrorCode `json:"code"`
	// Free-form detail about the error, for logging/diagnostics
	Message *string `json:"message,omitempty"`
}

type SessionFSExistsRequest struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSExistsResult struct {
	// Whether the path exists
	Exists bool `json:"exists"`
}

type SessionFSMkdirRequest struct {
	// Optional POSIX-style mode for newly created directories
	Mode *int64 `json:"mode,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Create parent directories as needed
	Recursive *bool `json:"recursive,omitempty"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSReadFileRequest struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSReadFileResult struct {
	// File content as UTF-8 string
	Content string `json:"content"`
	// Describes a filesystem error.
	Error *SessionFSError `json:"error,omitempty"`
}

type SessionFSReaddirRequest struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSReaddirResult struct {
	// Entry names in the directory
	Entries []string `json:"entries"`
	// Describes a filesystem error.
	Error *SessionFSError `json:"error,omitempty"`
}

type SessionFSReaddirWithTypesEntry struct {
	// Entry name
	Name string `json:"name"`
	// Entry type
	Type SessionFSReaddirWithTypesEntryType `json:"type"`
}

type SessionFSReaddirWithTypesRequest struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSReaddirWithTypesResult struct {
	// Directory entries with type information
	Entries []SessionFSReaddirWithTypesEntry `json:"entries"`
	// Describes a filesystem error.
	Error *SessionFSError `json:"error,omitempty"`
}

type SessionFSRenameRequest struct {
	// Destination path using SessionFs conventions
	Dest string `json:"dest"`
	// Target session identifier
	SessionID string `json:"sessionId"`
	// Source path using SessionFs conventions
	Src string `json:"src"`
}

type SessionFSRmRequest struct {
	// Ignore errors if the path does not exist
	Force *bool `json:"force,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Remove directories and their contents recursively
	Recursive *bool `json:"recursive,omitempty"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSSetProviderRequest struct {
	// Path conventions used by this filesystem
	Conventions SessionFSSetProviderConventions `json:"conventions"`
	// Initial working directory for sessions
	InitialCwd string `json:"initialCwd"`
	// Path within each session's SessionFs where the runtime stores files for that session
	SessionStatePath string `json:"sessionStatePath"`
}

type SessionFSSetProviderResult struct {
	// Whether the provider was set successfully
	Success bool `json:"success"`
}

type SessionFSStatRequest struct {
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

type SessionFSStatResult struct {
	// ISO 8601 timestamp of creation
	Birthtime time.Time `json:"birthtime"`
	// Describes a filesystem error.
	Error *SessionFSError `json:"error,omitempty"`
	// Whether the path is a directory
	IsDirectory bool `json:"isDirectory"`
	// Whether the path is a file
	IsFile bool `json:"isFile"`
	// ISO 8601 timestamp of last modification
	Mtime time.Time `json:"mtime"`
	// File size in bytes
	Size int64 `json:"size"`
}

type SessionFSWriteFileRequest struct {
	// Content to write
	Content string `json:"content"`
	// Optional POSIX-style mode for newly created files
	Mode *int64 `json:"mode,omitempty"`
	// Path using SessionFs conventions
	Path string `json:"path"`
	// Target session identifier
	SessionID string `json:"sessionId"`
}

// Experimental: SessionsForkRequest is part of an experimental API and may change or be removed.
type SessionsForkRequest struct {
	// Source session ID to fork from
	SessionID string `json:"sessionId"`
	// Optional event ID boundary. When provided, the fork includes only events before this ID
	// (exclusive). When omitted, all events are included.
	ToEventID *string `json:"toEventId,omitempty"`
}

// Experimental: SessionsForkResult is part of an experimental API and may change or be removed.
type SessionsForkResult struct {
	// The new forked session's ID
	SessionID string `json:"sessionId"`
}

type ShellExecRequest struct {
	// Shell command to execute
	Command string `json:"command"`
	// Working directory (defaults to session working directory)
	Cwd *string `json:"cwd,omitempty"`
	// Timeout in milliseconds (default: 30000)
	Timeout *int64 `json:"timeout,omitempty"`
}

type ShellExecResult struct {
	// Unique identifier for tracking streamed output
	ProcessID string `json:"processId"`
}

type ShellKillRequest struct {
	// Process identifier returned by shell.exec
	ProcessID string `json:"processId"`
	// Signal to send (default: SIGTERM)
	Signal *ShellKillSignal `json:"signal,omitempty"`
}

type ShellKillResult struct {
	// Whether the signal was sent successfully
	Killed bool `json:"killed"`
}

type Skill struct {
	// Description of what the skill does
	Description string `json:"description"`
	// Whether the skill is currently enabled
	Enabled bool `json:"enabled"`
	// Unique identifier for the skill
	Name string `json:"name"`
	// Absolute path to the skill file
	Path *string `json:"path,omitempty"`
	// Source location type (e.g., project, personal, plugin)
	Source string `json:"source"`
	// Whether the skill can be invoked by the user as a slash command
	UserInvocable bool `json:"userInvocable"`
}

// Experimental: SkillList is part of an experimental API and may change or be removed.
type SkillList struct {
	// Available skills
	Skills []Skill `json:"skills"`
}

type SkillsConfigSetDisabledSkillsRequest struct {
	// List of skill names to disable
	DisabledSkills []string `json:"disabledSkills"`
}

type SkillsConfigSetDisabledSkillsResult struct {
}

// Experimental: SkillsDisableRequest is part of an experimental API and may change or be removed.
type SkillsDisableRequest struct {
	// Name of the skill to disable
	Name string `json:"name"`
}

// Experimental: SkillsDisableResult is part of an experimental API and may change or be removed.
type SkillsDisableResult struct {
}

type SkillsDiscoverRequest struct {
	// Optional list of project directory paths to scan for project-scoped skills
	ProjectPaths []string `json:"projectPaths,omitempty"`
	// Optional list of additional skill directory paths to include
	SkillDirectories []string `json:"skillDirectories,omitempty"`
}

// Experimental: SkillsEnableRequest is part of an experimental API and may change or be removed.
type SkillsEnableRequest struct {
	// Name of the skill to enable
	Name string `json:"name"`
}

// Experimental: SkillsEnableResult is part of an experimental API and may change or be removed.
type SkillsEnableResult struct {
}

// Experimental: SkillsReloadResult is part of an experimental API and may change or be removed.
type SkillsReloadResult struct {
}

type Tool struct {
	// Description of what the tool does
	Description string `json:"description"`
	// Optional instructions for how to use this tool effectively
	Instructions *string `json:"instructions,omitempty"`
	// Tool identifier (e.g., "bash", "grep", "str_replace_editor")
	Name string `json:"name"`
	// Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP
	// tools)
	NamespacedName *string `json:"namespacedName,omitempty"`
	// JSON Schema for the tool's input parameters
	Parameters map[string]any `json:"parameters,omitempty"`
}

type ToolCallResult struct {
	// Error message if the tool call failed
	Error *string `json:"error,omitempty"`
	// Type of the tool result
	ResultType *string `json:"resultType,omitempty"`
	// Text result to send back to the LLM
	TextResultForLlm string `json:"textResultForLlm"`
	// Telemetry data from tool execution
	ToolTelemetry map[string]any `json:"toolTelemetry,omitempty"`
}

type ToolList struct {
	// List of available built-in tools with metadata
	Tools []Tool `json:"tools"`
}

type ToolsHandlePendingToolCallRequest struct {
	// Error message if the tool call failed
	Error *string `json:"error,omitempty"`
	// Request ID of the pending tool call
	RequestID string `json:"requestId"`
	// Tool call result (string or expanded result object)
	Result *ToolsHandlePendingToolCall `json:"result"`
}

type ToolsListRequest struct {
	// Optional model ID — when provided, the returned tool list reflects model-specific
	// overrides
	Model *string `json:"model,omitempty"`
}

type UIElicitationArrayAnyOfField struct {
	Default     []string                          `json:"default,omitempty"`
	Description *string                           `json:"description,omitempty"`
	Items       UIElicitationArrayAnyOfFieldItems `json:"items"`
	MaxItems    *float64                          `json:"maxItems,omitempty"`
	MinItems    *float64                          `json:"minItems,omitempty"`
	Title       *string                           `json:"title,omitempty"`
	Type        UIElicitationArrayAnyOfFieldType  `json:"type"`
}

type UIElicitationArrayAnyOfFieldItems struct {
	AnyOf []UIElicitationArrayAnyOfFieldItemsAnyOf `json:"anyOf"`
}

type UIElicitationArrayAnyOfFieldItemsAnyOf struct {
	Const string `json:"const"`
	Title string `json:"title"`
}

type UIElicitationArrayEnumField struct {
	Default     []string                         `json:"default,omitempty"`
	Description *string                          `json:"description,omitempty"`
	Items       UIElicitationArrayEnumFieldItems `json:"items"`
	MaxItems    *float64                         `json:"maxItems,omitempty"`
	MinItems    *float64                         `json:"minItems,omitempty"`
	Title       *string                          `json:"title,omitempty"`
	Type        UIElicitationArrayAnyOfFieldType `json:"type"`
}

type UIElicitationArrayEnumFieldItems struct {
	Enum []string                             `json:"enum"`
	Type UIElicitationArrayEnumFieldItemsType `json:"type"`
}

type UIElicitationRequest struct {
	// Message describing what information is needed from the user
	Message string `json:"message"`
	// JSON Schema describing the form fields to present to the user
	RequestedSchema UIElicitationSchema `json:"requestedSchema"`
}

// JSON Schema describing the form fields to present to the user
type UIElicitationSchema struct {
	// Form field definitions, keyed by field name
	Properties map[string]UIElicitationSchemaProperty `json:"properties"`
	// List of required field names
	Required []string `json:"required,omitempty"`
	// Schema type indicator (always 'object')
	Type UIElicitationSchemaType `json:"type"`
}

type UIElicitationSchemaProperty struct {
	Default     *UIElicitationFieldValue                 `json:"default"`
	Description *string                                  `json:"description,omitempty"`
	Enum        []string                                 `json:"enum,omitempty"`
	EnumNames   []string                                 `json:"enumNames,omitempty"`
	Title       *string                                  `json:"title,omitempty"`
	Type        UIElicitationSchemaPropertyType          `json:"type"`
	OneOf       []UIElicitationStringOneOfFieldOneOf     `json:"oneOf,omitempty"`
	Items       *UIElicitationArrayFieldItems            `json:"items,omitempty"`
	MaxItems    *float64                                 `json:"maxItems,omitempty"`
	MinItems    *float64                                 `json:"minItems,omitempty"`
	Format      *UIElicitationSchemaPropertyStringFormat `json:"format,omitempty"`
	MaxLength   *float64                                 `json:"maxLength,omitempty"`
	MinLength   *float64                                 `json:"minLength,omitempty"`
	Maximum     *float64                                 `json:"maximum,omitempty"`
	Minimum     *float64                                 `json:"minimum,omitempty"`
}

type UIElicitationArrayFieldItems struct {
	Enum  []string                                 `json:"enum,omitempty"`
	Type  *UIElicitationArrayEnumFieldItemsType    `json:"type,omitempty"`
	AnyOf []UIElicitationArrayAnyOfFieldItemsAnyOf `json:"anyOf,omitempty"`
}

type UIElicitationStringOneOfFieldOneOf struct {
	Const string `json:"const"`
	Title string `json:"title"`
}

// The elicitation response (accept with form values, decline, or cancel)
type UIElicitationResponse struct {
	// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
	Action UIElicitationResponseAction `json:"action"`
	// The form values submitted by the user (present when action is 'accept')
	Content map[string]*UIElicitationFieldValue `json:"content,omitempty"`
}

type UIElicitationResult struct {
	// Whether the response was accepted. False if the request was already resolved by another
	// client.
	Success bool `json:"success"`
}

type UIElicitationSchemaPropertyBoolean struct {
	Default     *bool                                  `json:"default,omitempty"`
	Description *string                                `json:"description,omitempty"`
	Title       *string                                `json:"title,omitempty"`
	Type        UIElicitationSchemaPropertyBooleanType `json:"type"`
}

type UIElicitationSchemaPropertyNumber struct {
	Default     *float64                                  `json:"default,omitempty"`
	Description *string                                   `json:"description,omitempty"`
	Maximum     *float64                                  `json:"maximum,omitempty"`
	Minimum     *float64                                  `json:"minimum,omitempty"`
	Title       *string                                   `json:"title,omitempty"`
	Type        UIElicitationSchemaPropertyNumberTypeEnum `json:"type"`
}

type UIElicitationSchemaPropertyString struct {
	Default     *string                                  `json:"default,omitempty"`
	Description *string                                  `json:"description,omitempty"`
	Format      *UIElicitationSchemaPropertyStringFormat `json:"format,omitempty"`
	MaxLength   *float64                                 `json:"maxLength,omitempty"`
	MinLength   *float64                                 `json:"minLength,omitempty"`
	Title       *string                                  `json:"title,omitempty"`
	Type        UIElicitationArrayEnumFieldItemsType     `json:"type"`
}

type UIElicitationStringEnumField struct {
	Default     *string                              `json:"default,omitempty"`
	Description *string                              `json:"description,omitempty"`
	Enum        []string                             `json:"enum"`
	EnumNames   []string                             `json:"enumNames,omitempty"`
	Title       *string                              `json:"title,omitempty"`
	Type        UIElicitationArrayEnumFieldItemsType `json:"type"`
}

type UIElicitationStringOneOfField struct {
	Default     *string                              `json:"default,omitempty"`
	Description *string                              `json:"description,omitempty"`
	OneOf       []UIElicitationStringOneOfFieldOneOf `json:"oneOf"`
	Title       *string                              `json:"title,omitempty"`
	Type        UIElicitationArrayEnumFieldItemsType `json:"type"`
}

type UIHandlePendingElicitationRequest struct {
	// The unique request ID from the elicitation.requested event
	RequestID string `json:"requestId"`
	// The elicitation response (accept with form values, decline, or cancel)
	Result UIElicitationResponse `json:"result"`
}

// Experimental: UsageGetMetricsResult is part of an experimental API and may change or be removed.
type UsageGetMetricsResult struct {
	// Aggregated code change metrics
	CodeChanges UsageMetricsCodeChanges `json:"codeChanges"`
	// Currently active model identifier
	CurrentModel *string `json:"currentModel,omitempty"`
	// Input tokens from the most recent main-agent API call
	LastCallInputTokens int64 `json:"lastCallInputTokens"`
	// Output tokens from the most recent main-agent API call
	LastCallOutputTokens int64 `json:"lastCallOutputTokens"`
	// Per-model token and request metrics, keyed by model identifier
	ModelMetrics map[string]UsageMetricsModelMetric `json:"modelMetrics"`
	// Session start timestamp (epoch milliseconds)
	SessionStartTime int64 `json:"sessionStartTime"`
	// Total time spent in model API calls (milliseconds)
	TotalAPIDurationMS float64 `json:"totalApiDurationMs"`
	// Total user-initiated premium request cost across all models (may be fractional due to
	// multipliers)
	TotalPremiumRequestCost float64 `json:"totalPremiumRequestCost"`
	// Raw count of user-initiated API requests
	TotalUserRequests int64 `json:"totalUserRequests"`
}

// Aggregated code change metrics
type UsageMetricsCodeChanges struct {
	// Number of distinct files modified
	FilesModifiedCount int64 `json:"filesModifiedCount"`
	// Total lines of code added
	LinesAdded int64 `json:"linesAdded"`
	// Total lines of code removed
	LinesRemoved int64 `json:"linesRemoved"`
}

type UsageMetricsModelMetric struct {
	// Request count and cost metrics for this model
	Requests UsageMetricsModelMetricRequests `json:"requests"`
	// Token usage metrics for this model
	Usage UsageMetricsModelMetricUsage `json:"usage"`
}

// Request count and cost metrics for this model
type UsageMetricsModelMetricRequests struct {
	// User-initiated premium request cost (with multiplier applied)
	Cost float64 `json:"cost"`
	// Number of API requests made with this model
	Count int64 `json:"count"`
}

// Token usage metrics for this model
type UsageMetricsModelMetricUsage struct {
	// Total tokens read from prompt cache
	CacheReadTokens int64 `json:"cacheReadTokens"`
	// Total tokens written to prompt cache
	CacheWriteTokens int64 `json:"cacheWriteTokens"`
	// Total input tokens consumed
	InputTokens int64 `json:"inputTokens"`
	// Total output tokens produced
	OutputTokens int64 `json:"outputTokens"`
	// Total output tokens used for reasoning
	ReasoningTokens *int64 `json:"reasoningTokens,omitempty"`
}

type WorkspacesCreateFileRequest struct {
	// File content to write as a UTF-8 string
	Content string `json:"content"`
	// Relative path within the workspace files directory
	Path string `json:"path"`
}

type WorkspacesCreateFileResult struct {
}

type WorkspacesGetWorkspaceResult struct {
	// Current workspace metadata, or null if not available
	Workspace *WorkspaceClass `json:"workspace"`
}

type WorkspaceClass struct {
	Branch                 *string           `json:"branch,omitempty"`
	ChronicleSyncDismissed *bool             `json:"chronicle_sync_dismissed,omitempty"`
	CreatedAt              *time.Time        `json:"created_at,omitempty"`
	Cwd                    *string           `json:"cwd,omitempty"`
	GitRoot                *string           `json:"git_root,omitempty"`
	HostType               *HostType         `json:"host_type,omitempty"`
	ID                     string            `json:"id"`
	McLastEventID          *string           `json:"mc_last_event_id,omitempty"`
	McSessionID            *string           `json:"mc_session_id,omitempty"`
	McTaskID               *string           `json:"mc_task_id,omitempty"`
	Name                   *string           `json:"name,omitempty"`
	RemoteSteerable        *bool             `json:"remote_steerable,omitempty"`
	Repository             *string           `json:"repository,omitempty"`
	SessionSyncLevel       *SessionSyncLevel `json:"session_sync_level,omitempty"`
	Summary                *string           `json:"summary,omitempty"`
	SummaryCount           *int64            `json:"summary_count,omitempty"`
	UpdatedAt              *time.Time        `json:"updated_at,omitempty"`
}

type WorkspacesListFilesResult struct {
	// Relative file paths in the workspace files directory
	Files []string `json:"files"`
}

type WorkspacesReadFileRequest struct {
	// Relative path within the workspace files directory
	Path string `json:"path"`
}

type WorkspacesReadFileResult struct {
	// File content as a UTF-8 string
	Content string `json:"content"`
}

// Configuration source
//
// Configuration source: user, workspace, plugin, or builtin
type MCPServerSource string

const (
	MCPServerSourceBuiltin   MCPServerSource = "builtin"
	MCPServerSourceUser      MCPServerSource = "user"
	MCPServerSourcePlugin    MCPServerSource = "plugin"
	MCPServerSourceWorkspace MCPServerSource = "workspace"
)

// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
type DiscoveredMCPServerType string

const (
	DiscoveredMCPServerTypeHTTP   DiscoveredMCPServerType = "http"
	DiscoveredMCPServerTypeSSE    DiscoveredMCPServerType = "sse"
	DiscoveredMCPServerTypeStdio  DiscoveredMCPServerType = "stdio"
	DiscoveredMCPServerTypeMemory DiscoveredMCPServerType = "memory"
)

// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
type ExtensionSource string

const (
	ExtensionSourceUser    ExtensionSource = "user"
	ExtensionSourceProject ExtensionSource = "project"
)

// Current status: running, disabled, failed, or starting
type ExtensionStatus string

const (
	ExtensionStatusDisabled ExtensionStatus = "disabled"
	ExtensionStatusFailed   ExtensionStatus = "failed"
	ExtensionStatusRunning  ExtensionStatus = "running"
	ExtensionStatusStarting ExtensionStatus = "starting"
)

type FilterMappingString string

const (
	FilterMappingStringHiddenCharacters FilterMappingString = "hidden_characters"
	FilterMappingStringMarkdown         FilterMappingString = "markdown"
	FilterMappingStringNone             FilterMappingString = "none"
)

// Where this source lives — used for UI grouping
type InstructionsSourcesLocation string

const (
	InstructionsSourcesLocationUser             InstructionsSourcesLocation = "user"
	InstructionsSourcesLocationRepository       InstructionsSourcesLocation = "repository"
	InstructionsSourcesLocationWorkingDirectory InstructionsSourcesLocation = "working-directory"
)

// Category of instruction source — used for merge logic
type InstructionsSourcesType string

const (
	InstructionsSourcesTypeChildInstructions InstructionsSourcesType = "child-instructions"
	InstructionsSourcesTypeHome              InstructionsSourcesType = "home"
	InstructionsSourcesTypeModel             InstructionsSourcesType = "model"
	InstructionsSourcesTypeNestedAgents      InstructionsSourcesType = "nested-agents"
	InstructionsSourcesTypeRepo              InstructionsSourcesType = "repo"
	InstructionsSourcesTypeVscode            InstructionsSourcesType = "vscode"
)

// Log severity level. Determines how the message is displayed in the timeline. Defaults to
// "info".
type SessionLogLevel string

const (
	SessionLogLevelError   SessionLogLevel = "error"
	SessionLogLevelInfo    SessionLogLevel = "info"
	SessionLogLevelWarning SessionLogLevel = "warning"
)

// Remote transport type. Defaults to "http" when omitted.
type MCPServerConfigType string

const (
	MCPServerConfigTypeHTTP  MCPServerConfigType = "http"
	MCPServerConfigTypeLocal MCPServerConfigType = "local"
	MCPServerConfigTypeSSE   MCPServerConfigType = "sse"
	MCPServerConfigTypeStdio MCPServerConfigType = "stdio"
)

// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
type MCPServerStatus string

const (
	MCPServerStatusConnected     MCPServerStatus = "connected"
	MCPServerStatusDisabled      MCPServerStatus = "disabled"
	MCPServerStatusFailed        MCPServerStatus = "failed"
	MCPServerStatusNeedsAuth     MCPServerStatus = "needs-auth"
	MCPServerStatusNotConfigured MCPServerStatus = "not_configured"
	MCPServerStatusPending       MCPServerStatus = "pending"
)

// Remote transport type. Defaults to "http" when omitted.
type MCPServerConfigHTTPType string

const (
	MCPServerConfigHTTPTypeHTTP MCPServerConfigHTTPType = "http"
	MCPServerConfigHTTPTypeSSE  MCPServerConfigHTTPType = "sse"
)

type MCPServerConfigLocalType string

const (
	MCPServerConfigLocalTypeLocal MCPServerConfigLocalType = "local"
	MCPServerConfigLocalTypeStdio MCPServerConfigLocalType = "stdio"
)

// The agent mode. Valid values: "interactive", "plan", "autopilot".
type SessionMode string

const (
	SessionModeAutopilot   SessionMode = "autopilot"
	SessionModeInteractive SessionMode = "interactive"
	SessionModePlan        SessionMode = "plan"
)

type PermissionDecisionKind string

const (
	PermissionDecisionKindApproved                                       PermissionDecisionKind = "approved"
	PermissionDecisionKindDeniedByContentExclusionPolicy                 PermissionDecisionKind = "denied-by-content-exclusion-policy"
	PermissionDecisionKindDeniedByPermissionRequestHook                  PermissionDecisionKind = "denied-by-permission-request-hook"
	PermissionDecisionKindDeniedByRules                                  PermissionDecisionKind = "denied-by-rules"
	PermissionDecisionKindDeniedInteractivelyByUser                      PermissionDecisionKind = "denied-interactively-by-user"
	PermissionDecisionKindDeniedNoApprovalRuleAndCouldNotRequestFromUser PermissionDecisionKind = "denied-no-approval-rule-and-could-not-request-from-user"
)

type PermissionDecisionApprovedKind string

const (
	PermissionDecisionApprovedKindApproved PermissionDecisionApprovedKind = "approved"
)

type PermissionDecisionDeniedByContentExclusionPolicyKind string

const (
	PermissionDecisionDeniedByContentExclusionPolicyKindDeniedByContentExclusionPolicy PermissionDecisionDeniedByContentExclusionPolicyKind = "denied-by-content-exclusion-policy"
)

type PermissionDecisionDeniedByPermissionRequestHookKind string

const (
	PermissionDecisionDeniedByPermissionRequestHookKindDeniedByPermissionRequestHook PermissionDecisionDeniedByPermissionRequestHookKind = "denied-by-permission-request-hook"
)

type PermissionDecisionDeniedByRulesKind string

const (
	PermissionDecisionDeniedByRulesKindDeniedByRules PermissionDecisionDeniedByRulesKind = "denied-by-rules"
)

type PermissionDecisionDeniedInteractivelyByUserKind string

const (
	PermissionDecisionDeniedInteractivelyByUserKindDeniedInteractivelyByUser PermissionDecisionDeniedInteractivelyByUserKind = "denied-interactively-by-user"
)

type PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind string

const (
	PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKindDeniedNoApprovalRuleAndCouldNotRequestFromUser PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind = "denied-no-approval-rule-and-could-not-request-from-user"
)

// Error classification
type SessionFSErrorCode string

const (
	SessionFSErrorCodeENOENT  SessionFSErrorCode = "ENOENT"
	SessionFSErrorCodeUNKNOWN SessionFSErrorCode = "UNKNOWN"
)

// Entry type
type SessionFSReaddirWithTypesEntryType string

const (
	SessionFSReaddirWithTypesEntryTypeDirectory SessionFSReaddirWithTypesEntryType = "directory"
	SessionFSReaddirWithTypesEntryTypeFile      SessionFSReaddirWithTypesEntryType = "file"
)

// Path conventions used by this filesystem
type SessionFSSetProviderConventions string

const (
	SessionFSSetProviderConventionsPosix   SessionFSSetProviderConventions = "posix"
	SessionFSSetProviderConventionsWindows SessionFSSetProviderConventions = "windows"
)

// Signal to send (default: SIGTERM)
type ShellKillSignal string

const (
	ShellKillSignalSIGINT  ShellKillSignal = "SIGINT"
	ShellKillSignalSIGKILL ShellKillSignal = "SIGKILL"
	ShellKillSignalSIGTERM ShellKillSignal = "SIGTERM"
)

type UIElicitationArrayAnyOfFieldType string

const (
	UIElicitationArrayAnyOfFieldTypeArray UIElicitationArrayAnyOfFieldType = "array"
)

type UIElicitationArrayEnumFieldItemsType string

const (
	UIElicitationArrayEnumFieldItemsTypeString UIElicitationArrayEnumFieldItemsType = "string"
)

type UIElicitationSchemaPropertyStringFormat string

const (
	UIElicitationSchemaPropertyStringFormatDate     UIElicitationSchemaPropertyStringFormat = "date"
	UIElicitationSchemaPropertyStringFormatDateTime UIElicitationSchemaPropertyStringFormat = "date-time"
	UIElicitationSchemaPropertyStringFormatEmail    UIElicitationSchemaPropertyStringFormat = "email"
	UIElicitationSchemaPropertyStringFormatURI      UIElicitationSchemaPropertyStringFormat = "uri"
)

type UIElicitationSchemaPropertyType string

const (
	UIElicitationSchemaPropertyTypeInteger UIElicitationSchemaPropertyType = "integer"
	UIElicitationSchemaPropertyTypeNumber  UIElicitationSchemaPropertyType = "number"
	UIElicitationSchemaPropertyTypeArray   UIElicitationSchemaPropertyType = "array"
	UIElicitationSchemaPropertyTypeBoolean UIElicitationSchemaPropertyType = "boolean"
	UIElicitationSchemaPropertyTypeString  UIElicitationSchemaPropertyType = "string"
)

type UIElicitationSchemaType string

const (
	UIElicitationSchemaTypeObject UIElicitationSchemaType = "object"
)

// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
type UIElicitationResponseAction string

const (
	UIElicitationResponseActionAccept  UIElicitationResponseAction = "accept"
	UIElicitationResponseActionCancel  UIElicitationResponseAction = "cancel"
	UIElicitationResponseActionDecline UIElicitationResponseAction = "decline"
)

type UIElicitationSchemaPropertyBooleanType string

const (
	UIElicitationSchemaPropertyBooleanTypeBoolean UIElicitationSchemaPropertyBooleanType = "boolean"
)

type UIElicitationSchemaPropertyNumberTypeEnum string

const (
	UIElicitationSchemaPropertyNumberTypeEnumInteger UIElicitationSchemaPropertyNumberTypeEnum = "integer"
	UIElicitationSchemaPropertyNumberTypeEnumNumber  UIElicitationSchemaPropertyNumberTypeEnum = "number"
)

type HostType string

const (
	HostTypeAdo    HostType = "ado"
	HostTypeGithub HostType = "github"
)

type SessionSyncLevel string

const (
	SessionSyncLevelRepoAndUser SessionSyncLevel = "repo_and_user"
	SessionSyncLevelLocal       SessionSyncLevel = "local"
	SessionSyncLevelUser        SessionSyncLevel = "user"
)

type FilterMapping struct {
	Enum    *FilterMappingString
	EnumMap map[string]FilterMappingString
}

// Tool call result (string or expanded result object)
type ToolsHandlePendingToolCall struct {
	String         *string
	ToolCallResult *ToolCallResult
}

type UIElicitationFieldValue struct {
	Bool        *bool
	Double      *float64
	String      *string
	StringArray []string
}

type serverApi struct {
	client *jsonrpc2.Client
}

type ServerModelsApi serverApi

func (a *ServerModelsApi) List(ctx context.Context) (*ModelList, error) {
	raw, err := a.client.Request("models.list", nil)
	if err != nil {
		return nil, err
	}
	var result ModelList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerToolsApi serverApi

func (a *ServerToolsApi) List(ctx context.Context, params *ToolsListRequest) (*ToolList, error) {
	raw, err := a.client.Request("tools.list", params)
	if err != nil {
		return nil, err
	}
	var result ToolList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerAccountApi serverApi

func (a *ServerAccountApi) GetQuota(ctx context.Context) (*AccountGetQuotaResult, error) {
	raw, err := a.client.Request("account.getQuota", nil)
	if err != nil {
		return nil, err
	}
	var result AccountGetQuotaResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerMcpApi serverApi

func (a *ServerMcpApi) Discover(ctx context.Context, params *MCPDiscoverRequest) (*MCPDiscoverResult, error) {
	raw, err := a.client.Request("mcp.discover", params)
	if err != nil {
		return nil, err
	}
	var result MCPDiscoverResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerMcpConfigApi serverApi

func (a *ServerMcpConfigApi) List(ctx context.Context) (*MCPConfigList, error) {
	raw, err := a.client.Request("mcp.config.list", nil)
	if err != nil {
		return nil, err
	}
	var result MCPConfigList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ServerMcpConfigApi) Add(ctx context.Context, params *MCPConfigAddRequest) (*MCPConfigAddResult, error) {
	raw, err := a.client.Request("mcp.config.add", params)
	if err != nil {
		return nil, err
	}
	var result MCPConfigAddResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ServerMcpConfigApi) Update(ctx context.Context, params *MCPConfigUpdateRequest) (*MCPConfigUpdateResult, error) {
	raw, err := a.client.Request("mcp.config.update", params)
	if err != nil {
		return nil, err
	}
	var result MCPConfigUpdateResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ServerMcpConfigApi) Remove(ctx context.Context, params *MCPConfigRemoveRequest) (*MCPConfigRemoveResult, error) {
	raw, err := a.client.Request("mcp.config.remove", params)
	if err != nil {
		return nil, err
	}
	var result MCPConfigRemoveResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (s *ServerMcpApi) Config() *ServerMcpConfigApi {
	return (*ServerMcpConfigApi)(s)
}

type ServerSkillsApi serverApi

func (a *ServerSkillsApi) Discover(ctx context.Context, params *SkillsDiscoverRequest) (*ServerSkillList, error) {
	raw, err := a.client.Request("skills.discover", params)
	if err != nil {
		return nil, err
	}
	var result ServerSkillList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ServerSkillsConfigApi serverApi

func (a *ServerSkillsConfigApi) SetDisabledSkills(ctx context.Context, params *SkillsConfigSetDisabledSkillsRequest) (*SkillsConfigSetDisabledSkillsResult, error) {
	raw, err := a.client.Request("skills.config.setDisabledSkills", params)
	if err != nil {
		return nil, err
	}
	var result SkillsConfigSetDisabledSkillsResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (s *ServerSkillsApi) Config() *ServerSkillsConfigApi {
	return (*ServerSkillsConfigApi)(s)
}

type ServerSessionFsApi serverApi

func (a *ServerSessionFsApi) SetProvider(ctx context.Context, params *SessionFSSetProviderRequest) (*SessionFSSetProviderResult, error) {
	raw, err := a.client.Request("sessionFs.setProvider", params)
	if err != nil {
		return nil, err
	}
	var result SessionFSSetProviderResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: ServerSessionsApi contains experimental APIs that may change or be removed.
type ServerSessionsApi serverApi

func (a *ServerSessionsApi) Fork(ctx context.Context, params *SessionsForkRequest) (*SessionsForkResult, error) {
	raw, err := a.client.Request("sessions.fork", params)
	if err != nil {
		return nil, err
	}
	var result SessionsForkResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// ServerRpc provides typed server-scoped RPC methods.
type ServerRpc struct {
	common serverApi // Reuse a single struct instead of allocating one for each service on the heap.

	Models    *ServerModelsApi
	Tools     *ServerToolsApi
	Account   *ServerAccountApi
	Mcp       *ServerMcpApi
	Skills    *ServerSkillsApi
	SessionFs *ServerSessionFsApi
	Sessions  *ServerSessionsApi
}

func (a *ServerRpc) Ping(ctx context.Context, params *PingRequest) (*PingResult, error) {
	raw, err := a.common.client.Request("ping", params)
	if err != nil {
		return nil, err
	}
	var result PingResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func NewServerRpc(client *jsonrpc2.Client) *ServerRpc {
	r := &ServerRpc{}
	r.common = serverApi{client: client}
	r.Models = (*ServerModelsApi)(&r.common)
	r.Tools = (*ServerToolsApi)(&r.common)
	r.Account = (*ServerAccountApi)(&r.common)
	r.Mcp = (*ServerMcpApi)(&r.common)
	r.Skills = (*ServerSkillsApi)(&r.common)
	r.SessionFs = (*ServerSessionFsApi)(&r.common)
	r.Sessions = (*ServerSessionsApi)(&r.common)
	return r
}

type sessionApi struct {
	client    *jsonrpc2.Client
	sessionID string
}

type ModelApi sessionApi

func (a *ModelApi) GetCurrent(ctx context.Context) (*CurrentModel, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.model.getCurrent", req)
	if err != nil {
		return nil, err
	}
	var result CurrentModel
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ModelApi) SwitchTo(ctx context.Context, params *ModelSwitchToRequest) (*ModelSwitchToResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["modelId"] = params.ModelID
		if params.ReasoningEffort != nil {
			req["reasoningEffort"] = *params.ReasoningEffort
		}
		if params.ModelCapabilities != nil {
			req["modelCapabilities"] = *params.ModelCapabilities
		}
	}
	raw, err := a.client.Request("session.model.switchTo", req)
	if err != nil {
		return nil, err
	}
	var result ModelSwitchToResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ModeApi sessionApi

func (a *ModeApi) Get(ctx context.Context) (*SessionMode, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.mode.get", req)
	if err != nil {
		return nil, err
	}
	var result SessionMode
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ModeApi) Set(ctx context.Context, params *ModeSetRequest) (*ModeSetResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["mode"] = params.Mode
	}
	raw, err := a.client.Request("session.mode.set", req)
	if err != nil {
		return nil, err
	}
	var result ModeSetResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type NameApi sessionApi

func (a *NameApi) Get(ctx context.Context) (*NameGetResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.name.get", req)
	if err != nil {
		return nil, err
	}
	var result NameGetResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *NameApi) Set(ctx context.Context, params *NameSetRequest) (*NameSetResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.name.set", req)
	if err != nil {
		return nil, err
	}
	var result NameSetResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type PlanApi sessionApi

func (a *PlanApi) Read(ctx context.Context) (*PlanReadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.plan.read", req)
	if err != nil {
		return nil, err
	}
	var result PlanReadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *PlanApi) Update(ctx context.Context, params *PlanUpdateRequest) (*PlanUpdateResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["content"] = params.Content
	}
	raw, err := a.client.Request("session.plan.update", req)
	if err != nil {
		return nil, err
	}
	var result PlanUpdateResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *PlanApi) Delete(ctx context.Context) (*PlanDeleteResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.plan.delete", req)
	if err != nil {
		return nil, err
	}
	var result PlanDeleteResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type WorkspacesApi sessionApi

func (a *WorkspacesApi) GetWorkspace(ctx context.Context) (*WorkspacesGetWorkspaceResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.workspaces.getWorkspace", req)
	if err != nil {
		return nil, err
	}
	var result WorkspacesGetWorkspaceResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *WorkspacesApi) ListFiles(ctx context.Context) (*WorkspacesListFilesResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.workspaces.listFiles", req)
	if err != nil {
		return nil, err
	}
	var result WorkspacesListFilesResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *WorkspacesApi) ReadFile(ctx context.Context, params *WorkspacesReadFileRequest) (*WorkspacesReadFileResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["path"] = params.Path
	}
	raw, err := a.client.Request("session.workspaces.readFile", req)
	if err != nil {
		return nil, err
	}
	var result WorkspacesReadFileResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *WorkspacesApi) CreateFile(ctx context.Context, params *WorkspacesCreateFileRequest) (*WorkspacesCreateFileResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["path"] = params.Path
		req["content"] = params.Content
	}
	raw, err := a.client.Request("session.workspaces.createFile", req)
	if err != nil {
		return nil, err
	}
	var result WorkspacesCreateFileResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type InstructionsApi sessionApi

func (a *InstructionsApi) GetSources(ctx context.Context) (*InstructionsGetSourcesResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.instructions.getSources", req)
	if err != nil {
		return nil, err
	}
	var result InstructionsGetSourcesResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: FleetApi contains experimental APIs that may change or be removed.
type FleetApi sessionApi

func (a *FleetApi) Start(ctx context.Context, params *FleetStartRequest) (*FleetStartResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		if params.Prompt != nil {
			req["prompt"] = *params.Prompt
		}
	}
	raw, err := a.client.Request("session.fleet.start", req)
	if err != nil {
		return nil, err
	}
	var result FleetStartResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: AgentApi contains experimental APIs that may change or be removed.
type AgentApi sessionApi

func (a *AgentApi) List(ctx context.Context) (*AgentList, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.list", req)
	if err != nil {
		return nil, err
	}
	var result AgentList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentApi) GetCurrent(ctx context.Context) (*AgentGetCurrentResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.getCurrent", req)
	if err != nil {
		return nil, err
	}
	var result AgentGetCurrentResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentApi) Select(ctx context.Context, params *AgentSelectRequest) (*AgentSelectResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.agent.select", req)
	if err != nil {
		return nil, err
	}
	var result AgentSelectResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentApi) Deselect(ctx context.Context) (*AgentDeselectResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.deselect", req)
	if err != nil {
		return nil, err
	}
	var result AgentDeselectResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *AgentApi) Reload(ctx context.Context) (*AgentReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.agent.reload", req)
	if err != nil {
		return nil, err
	}
	var result AgentReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: SkillsApi contains experimental APIs that may change or be removed.
type SkillsApi sessionApi

func (a *SkillsApi) List(ctx context.Context) (*SkillList, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.skills.list", req)
	if err != nil {
		return nil, err
	}
	var result SkillList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *SkillsApi) Enable(ctx context.Context, params *SkillsEnableRequest) (*SkillsEnableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.skills.enable", req)
	if err != nil {
		return nil, err
	}
	var result SkillsEnableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *SkillsApi) Disable(ctx context.Context, params *SkillsDisableRequest) (*SkillsDisableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["name"] = params.Name
	}
	raw, err := a.client.Request("session.skills.disable", req)
	if err != nil {
		return nil, err
	}
	var result SkillsDisableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *SkillsApi) Reload(ctx context.Context) (*SkillsReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.skills.reload", req)
	if err != nil {
		return nil, err
	}
	var result SkillsReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: McpApi contains experimental APIs that may change or be removed.
type McpApi sessionApi

func (a *McpApi) List(ctx context.Context) (*MCPServerList, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.mcp.list", req)
	if err != nil {
		return nil, err
	}
	var result MCPServerList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *McpApi) Enable(ctx context.Context, params *MCPEnableRequest) (*MCPEnableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["serverName"] = params.ServerName
	}
	raw, err := a.client.Request("session.mcp.enable", req)
	if err != nil {
		return nil, err
	}
	var result MCPEnableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *McpApi) Disable(ctx context.Context, params *MCPDisableRequest) (*MCPDisableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["serverName"] = params.ServerName
	}
	raw, err := a.client.Request("session.mcp.disable", req)
	if err != nil {
		return nil, err
	}
	var result MCPDisableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *McpApi) Reload(ctx context.Context) (*MCPReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.mcp.reload", req)
	if err != nil {
		return nil, err
	}
	var result MCPReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: PluginsApi contains experimental APIs that may change or be removed.
type PluginsApi sessionApi

func (a *PluginsApi) List(ctx context.Context) (*PluginList, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.plugins.list", req)
	if err != nil {
		return nil, err
	}
	var result PluginList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: ExtensionsApi contains experimental APIs that may change or be removed.
type ExtensionsApi sessionApi

func (a *ExtensionsApi) List(ctx context.Context) (*ExtensionList, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.extensions.list", req)
	if err != nil {
		return nil, err
	}
	var result ExtensionList
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ExtensionsApi) Enable(ctx context.Context, params *ExtensionsEnableRequest) (*ExtensionsEnableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["id"] = params.ID
	}
	raw, err := a.client.Request("session.extensions.enable", req)
	if err != nil {
		return nil, err
	}
	var result ExtensionsEnableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ExtensionsApi) Disable(ctx context.Context, params *ExtensionsDisableRequest) (*ExtensionsDisableResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["id"] = params.ID
	}
	raw, err := a.client.Request("session.extensions.disable", req)
	if err != nil {
		return nil, err
	}
	var result ExtensionsDisableResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ExtensionsApi) Reload(ctx context.Context) (*ExtensionsReloadResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.extensions.reload", req)
	if err != nil {
		return nil, err
	}
	var result ExtensionsReloadResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ToolsApi sessionApi

func (a *ToolsApi) HandlePendingToolCall(ctx context.Context, params *ToolsHandlePendingToolCallRequest) (*HandleToolCallResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		if params.Result != nil {
			req["result"] = *params.Result
		}
		if params.Error != nil {
			req["error"] = *params.Error
		}
	}
	raw, err := a.client.Request("session.tools.handlePendingToolCall", req)
	if err != nil {
		return nil, err
	}
	var result HandleToolCallResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type CommandsApi sessionApi

func (a *CommandsApi) HandlePendingCommand(ctx context.Context, params *CommandsHandlePendingCommandRequest) (*CommandsHandlePendingCommandResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		if params.Error != nil {
			req["error"] = *params.Error
		}
	}
	raw, err := a.client.Request("session.commands.handlePendingCommand", req)
	if err != nil {
		return nil, err
	}
	var result CommandsHandlePendingCommandResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type UIApi sessionApi

func (a *UIApi) Elicitation(ctx context.Context, params *UIElicitationRequest) (*UIElicitationResponse, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["message"] = params.Message
		req["requestedSchema"] = params.RequestedSchema
	}
	raw, err := a.client.Request("session.ui.elicitation", req)
	if err != nil {
		return nil, err
	}
	var result UIElicitationResponse
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *UIApi) HandlePendingElicitation(ctx context.Context, params *UIHandlePendingElicitationRequest) (*UIElicitationResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		req["result"] = params.Result
	}
	raw, err := a.client.Request("session.ui.handlePendingElicitation", req)
	if err != nil {
		return nil, err
	}
	var result UIElicitationResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type PermissionsApi sessionApi

func (a *PermissionsApi) HandlePendingPermissionRequest(ctx context.Context, params *PermissionDecisionRequest) (*PermissionRequestResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["requestId"] = params.RequestID
		req["result"] = params.Result
	}
	raw, err := a.client.Request("session.permissions.handlePendingPermissionRequest", req)
	if err != nil {
		return nil, err
	}
	var result PermissionRequestResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

type ShellApi sessionApi

func (a *ShellApi) Exec(ctx context.Context, params *ShellExecRequest) (*ShellExecResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["command"] = params.Command
		if params.Cwd != nil {
			req["cwd"] = *params.Cwd
		}
		if params.Timeout != nil {
			req["timeout"] = *params.Timeout
		}
	}
	raw, err := a.client.Request("session.shell.exec", req)
	if err != nil {
		return nil, err
	}
	var result ShellExecResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *ShellApi) Kill(ctx context.Context, params *ShellKillRequest) (*ShellKillResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["processId"] = params.ProcessID
		if params.Signal != nil {
			req["signal"] = *params.Signal
		}
	}
	raw, err := a.client.Request("session.shell.kill", req)
	if err != nil {
		return nil, err
	}
	var result ShellKillResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: HistoryApi contains experimental APIs that may change or be removed.
type HistoryApi sessionApi

func (a *HistoryApi) Compact(ctx context.Context) (*HistoryCompactResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.history.compact", req)
	if err != nil {
		return nil, err
	}
	var result HistoryCompactResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (a *HistoryApi) Truncate(ctx context.Context, params *HistoryTruncateRequest) (*HistoryTruncateResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	if params != nil {
		req["eventId"] = params.EventID
	}
	raw, err := a.client.Request("session.history.truncate", req)
	if err != nil {
		return nil, err
	}
	var result HistoryTruncateResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// Experimental: UsageApi contains experimental APIs that may change or be removed.
type UsageApi sessionApi

func (a *UsageApi) GetMetrics(ctx context.Context) (*UsageGetMetricsResult, error) {
	req := map[string]any{"sessionId": a.sessionID}
	raw, err := a.client.Request("session.usage.getMetrics", req)
	if err != nil {
		return nil, err
	}
	var result UsageGetMetricsResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

// SessionRpc provides typed session-scoped RPC methods.
type SessionRpc struct {
	common sessionApi // Reuse a single struct instead of allocating one for each service on the heap.

	Model        *ModelApi
	Mode         *ModeApi
	Name         *NameApi
	Plan         *PlanApi
	Workspaces   *WorkspacesApi
	Instructions *InstructionsApi
	Fleet        *FleetApi
	Agent        *AgentApi
	Skills       *SkillsApi
	Mcp          *McpApi
	Plugins      *PluginsApi
	Extensions   *ExtensionsApi
	Tools        *ToolsApi
	Commands     *CommandsApi
	UI           *UIApi
	Permissions  *PermissionsApi
	Shell        *ShellApi
	History      *HistoryApi
	Usage        *UsageApi
}

func (a *SessionRpc) Log(ctx context.Context, params *LogRequest) (*LogResult, error) {
	req := map[string]any{"sessionId": a.common.sessionID}
	if params != nil {
		req["message"] = params.Message
		if params.Level != nil {
			req["level"] = *params.Level
		}
		if params.Ephemeral != nil {
			req["ephemeral"] = *params.Ephemeral
		}
		if params.URL != nil {
			req["url"] = *params.URL
		}
	}
	raw, err := a.common.client.Request("session.log", req)
	if err != nil {
		return nil, err
	}
	var result LogResult
	if err := json.Unmarshal(raw, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func NewSessionRpc(client *jsonrpc2.Client, sessionID string) *SessionRpc {
	r := &SessionRpc{}
	r.common = sessionApi{client: client, sessionID: sessionID}
	r.Model = (*ModelApi)(&r.common)
	r.Mode = (*ModeApi)(&r.common)
	r.Name = (*NameApi)(&r.common)
	r.Plan = (*PlanApi)(&r.common)
	r.Workspaces = (*WorkspacesApi)(&r.common)
	r.Instructions = (*InstructionsApi)(&r.common)
	r.Fleet = (*FleetApi)(&r.common)
	r.Agent = (*AgentApi)(&r.common)
	r.Skills = (*SkillsApi)(&r.common)
	r.Mcp = (*McpApi)(&r.common)
	r.Plugins = (*PluginsApi)(&r.common)
	r.Extensions = (*ExtensionsApi)(&r.common)
	r.Tools = (*ToolsApi)(&r.common)
	r.Commands = (*CommandsApi)(&r.common)
	r.UI = (*UIApi)(&r.common)
	r.Permissions = (*PermissionsApi)(&r.common)
	r.Shell = (*ShellApi)(&r.common)
	r.History = (*HistoryApi)(&r.common)
	r.Usage = (*UsageApi)(&r.common)
	return r
}

type SessionFsHandler interface {
	ReadFile(request *SessionFSReadFileRequest) (*SessionFSReadFileResult, error)
	WriteFile(request *SessionFSWriteFileRequest) (*SessionFSError, error)
	AppendFile(request *SessionFSAppendFileRequest) (*SessionFSError, error)
	Exists(request *SessionFSExistsRequest) (*SessionFSExistsResult, error)
	Stat(request *SessionFSStatRequest) (*SessionFSStatResult, error)
	Mkdir(request *SessionFSMkdirRequest) (*SessionFSError, error)
	Readdir(request *SessionFSReaddirRequest) (*SessionFSReaddirResult, error)
	ReaddirWithTypes(request *SessionFSReaddirWithTypesRequest) (*SessionFSReaddirWithTypesResult, error)
	Rm(request *SessionFSRmRequest) (*SessionFSError, error)
	Rename(request *SessionFSRenameRequest) (*SessionFSError, error)
}

// ClientSessionApiHandlers provides all client session API handler groups for a session.
type ClientSessionApiHandlers struct {
	SessionFs SessionFsHandler
}

func clientSessionHandlerError(err error) *jsonrpc2.Error {
	if err == nil {
		return nil
	}
	var rpcErr *jsonrpc2.Error
	if errors.As(err, &rpcErr) {
		return rpcErr
	}
	return &jsonrpc2.Error{Code: -32603, Message: err.Error()}
}

// RegisterClientSessionApiHandlers registers handlers for server-to-client session API calls.
func RegisterClientSessionApiHandlers(client *jsonrpc2.Client, getHandlers func(sessionID string) *ClientSessionApiHandlers) {
	client.SetRequestHandler("sessionFs.readFile", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSReadFileRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.ReadFile(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.writeFile", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSWriteFileRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.WriteFile(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.appendFile", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSAppendFileRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.AppendFile(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.exists", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSExistsRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Exists(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.stat", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSStatRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Stat(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.mkdir", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSMkdirRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Mkdir(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.readdir", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSReaddirRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Readdir(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.readdirWithTypes", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSReaddirWithTypesRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.ReaddirWithTypes(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.rm", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSRmRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Rm(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
	client.SetRequestHandler("sessionFs.rename", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request SessionFSRenameRequest
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
		}
		handlers := getHandlers(request.SessionID)
		if handlers == nil || handlers.SessionFs == nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No sessionFs handler registered for session: %s", request.SessionID)}
		}
		result, err := handlers.SessionFs.Rename(&request)
		if err != nil {
			return nil, clientSessionHandlerError(err)
		}
		raw, err := json.Marshal(result)
		if err != nil {
			return nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
		}
		return raw, nil
	})
}
