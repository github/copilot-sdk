/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: session-events.schema.json
 */

/**
 * Union of all session event variants emitted by the Copilot CLI runtime.
 */
export type SessionEvent =
  | StartEvent
  | ResumeEvent
  | RemoteSteerableChangedEvent
  | ErrorEvent
  | IdleEvent
  | TitleChangedEvent
  | ScheduleCreatedEvent
  | ScheduleCancelledEvent
  | InfoEvent
  | WarningEvent
  | ModelChangeEvent
  | ModeChangedEvent
  | PlanChangedEvent
  | WorkspaceFileChangedEvent
  | HandoffEvent
  | TruncationEvent
  | SnapshotRewindEvent
  | ShutdownEvent
  | ContextChangedEvent
  | UsageInfoEvent
  | CompactionStartEvent
  | CompactionCompleteEvent
  | TaskCompleteEvent
  | UserMessageEvent
  | PendingMessagesModifiedEvent
  | AssistantTurnStartEvent
  | AssistantIntentEvent
  | AssistantReasoningEvent
  | AssistantReasoningDeltaEvent
  | AssistantStreamingDeltaEvent
  | AssistantMessageEvent
  | AssistantMessageStartEvent
  | AssistantMessageDeltaEvent
  | AssistantTurnEndEvent
  | AssistantUsageEvent
  | ModelCallFailureEvent
  | AbortEvent
  | ToolUserRequestedEvent
  | ToolExecutionStartEvent
  | ToolExecutionPartialResultEvent
  | ToolExecutionProgressEvent
  | ToolExecutionCompleteEvent
  | SkillInvokedEvent
  | SubagentStartedEvent
  | SubagentCompletedEvent
  | SubagentFailedEvent
  | SubagentSelectedEvent
  | SubagentDeselectedEvent
  | HookStartEvent
  | HookEndEvent
  | SystemMessageEvent
  | SystemNotificationEvent
  | PermissionRequestedEvent
  | PermissionCompletedEvent
  | UserInputRequestedEvent
  | UserInputCompletedEvent
  | ElicitationRequestedEvent
  | ElicitationCompletedEvent
  | SamplingRequestedEvent
  | SamplingCompletedEvent
  | McpOauthRequiredEvent
  | McpOauthCompletedEvent
  | CustomNotificationEvent
  | ExternalToolRequestedEvent
  | ExternalToolCompletedEvent
  | CommandQueuedEvent
  | CommandExecuteEvent
  | CommandCompletedEvent
  | AutoModeSwitchRequestedEvent
  | AutoModeSwitchCompletedEvent
  | CommandsChangedEvent
  | CapabilitiesChangedEvent
  | ExitPlanModeRequestedEvent
  | ExitPlanModeCompletedEvent
  | ToolsUpdatedEvent
  | BackgroundTasksChangedEvent
  | SkillsLoadedEvent
  | CustomAgentsUpdatedEvent
  | McpServersLoadedEvent
  | McpServerStatusChangedEvent
  | ExtensionsLoadedEvent;
/**
 * Hosting platform type of the repository (github or ado)
 */
export type WorkingDirectoryContextHostType =
  /** Repository is hosted on GitHub. */
  | "github"
  /** Repository is hosted on Azure DevOps. */
  | "ado";
/**
 * Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
 */
export type ReasoningSummary =
  /** Do not request reasoning summaries from the model. */
  | "none"
  /** Request a concise summary of the model's reasoning. */
  | "concise"
  /** Request a detailed summary of the model's reasoning. */
  | "detailed";
/**
 * The session mode the agent is operating in
 */
export type SessionMode =
  /** The agent is responding interactively to the user. */
  | "interactive"
  /** The agent is preparing a plan before making changes. */
  | "plan"
  /** The agent is working autonomously toward task completion. */
  | "autopilot";
/**
 * The type of operation performed on the plan file
 */
export type PlanChangedOperation =
  /** The plan file was created. */
  | "create"
  /** The plan file was updated. */
  | "update"
  /** The plan file was deleted. */
  | "delete";
/**
 * Whether the file was newly created or updated
 */
export type WorkspaceFileChangedOperation =
  /** The workspace file was created. */
  | "create"
  /** The workspace file was updated. */
  | "update";
/**
 * Origin type of the session being handed off
 */
export type HandoffSourceType =
  /** The handoff originated from a remote session. */
  | "remote"
  /** The handoff originated from a local session. */
  | "local";
/**
 * Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
 */
export type ShutdownType =
  /** The session ended normally. */
  | "routine"
  /** The session ended because of a crash or fatal error. */
  | "error";
/**
 * The agent mode that was active when this message was sent
 */
export type UserMessageAgentMode =
  /** The agent is responding interactively to the user. */
  | "interactive"
  /** The agent is preparing a plan before making changes. */
  | "plan"
  /** The agent is working autonomously toward task completion. */
  | "autopilot"
  /** The agent is in shell-focused UI mode. */
  | "shell";
/**
 * A user message attachment — a file, directory, code selection, blob, or GitHub reference
 */
export type UserMessageAttachment =
  | UserMessageAttachmentFile
  | UserMessageAttachmentDirectory
  | UserMessageAttachmentSelection
  | UserMessageAttachmentGithubReference
  | UserMessageAttachmentBlob;
/**
 * Type of GitHub reference
 */
export type UserMessageAttachmentGithubReferenceType =
  /** GitHub issue reference. */
  | "issue"
  /** GitHub pull request reference. */
  | "pr"
  /** GitHub discussion reference. */
  | "discussion";
/**
 * Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
 */
export type AssistantMessageToolRequestType =
  /** Standard function-style tool call. */
  | "function"
  /** Custom grammar-based tool call. */
  | "custom";
/**
 * API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
 */
export type AssistantUsageApiEndpoint =
  /** Chat Completions API endpoint. */
  | "/chat/completions"
  /** Anthropic Messages API endpoint. */
  | "/v1/messages"
  /** Responses API endpoint. */
  | "/responses"
  /** WebSocket Responses API endpoint. */
  | "ws:/responses";
/**
 * Where the failed model call originated
 */
export type ModelCallFailureSource =
  /** Model call from the top-level agent. */
  | "top_level"
  /** Model call from a sub-agent. */
  | "subagent"
  /** Model call from MCP sampling. */
  | "mcp_sampling";
/**
 * Finite reason code describing why the current turn was aborted
 */
export type AbortReason =
  /** The local user requested the abort, for example by pressing Ctrl+C in the CLI. */
  | "user_initiated"
  /** A remote command requested the abort. */
  | "remote_command"
  /** An MCP server delivered a user.abort notification. */
  | "user_abort";
/**
 * A content block within a tool result, which may be text, terminal output, image, audio, or a resource
 */
export type ToolExecutionCompleteContent =
  | ToolExecutionCompleteContentText
  | ToolExecutionCompleteContentTerminal
  | ToolExecutionCompleteContentImage
  | ToolExecutionCompleteContentAudio
  | ToolExecutionCompleteContentResourceLink
  | ToolExecutionCompleteContentResource;
/**
 * Theme variant this icon is intended for
 */
export type ToolExecutionCompleteContentResourceLinkIconTheme =
  /** Icon intended for light themes. */
  | "light"
  /** Icon intended for dark themes. */
  | "dark";
/**
 * The embedded resource contents, either text or base64-encoded binary
 */
export type ToolExecutionCompleteContentResourceDetails = EmbeddedTextResourceContents | EmbeddedBlobResourceContents;
/**
 * Message role: "system" for system prompts, "developer" for developer-injected instructions
 */
export type SystemMessageRole =
  /** System prompt message. */
  | "system"
  /** Developer instruction message. */
  | "developer";
/**
 * Structured metadata identifying what triggered this notification
 */
export type SystemNotification =
  | SystemNotificationAgentCompleted
  | SystemNotificationAgentIdle
  | SystemNotificationNewInboxMessage
  | SystemNotificationShellCompleted
  | SystemNotificationShellDetachedCompleted
  | SystemNotificationInstructionDiscovered;
/**
 * Whether the agent completed successfully or failed
 */
export type SystemNotificationAgentCompletedStatus =
  /** The agent completed successfully. */
  | "completed"
  /** The agent failed. */
  | "failed";
/**
 * Details of the permission being requested
 */
export type PermissionRequest =
  | PermissionRequestShell
  | PermissionRequestWrite
  | PermissionRequestRead
  | PermissionRequestMcp
  | PermissionRequestUrl
  | PermissionRequestMemory
  | PermissionRequestCustomTool
  | PermissionRequestHook
  | PermissionRequestExtensionManagement
  | PermissionRequestExtensionPermissionAccess;
/**
 * Whether this is a store or vote memory operation
 */
export type PermissionRequestMemoryAction =
  /** Store a new memory. */
  | "store"
  /** Vote on an existing memory. */
  | "vote";
/**
 * Vote direction (vote only)
 */
export type PermissionRequestMemoryDirection =
  /** Vote that the memory is useful or accurate. */
  | "upvote"
  /** Vote that the memory is incorrect or outdated. */
  | "downvote";
/**
 * Derived user-facing permission prompt details for UI consumers
 */
export type PermissionPromptRequest =
  | PermissionPromptRequestCommands
  | PermissionPromptRequestWrite
  | PermissionPromptRequestRead
  | PermissionPromptRequestMcp
  | PermissionPromptRequestUrl
  | PermissionPromptRequestMemory
  | PermissionPromptRequestCustomTool
  | PermissionPromptRequestPath
  | PermissionPromptRequestHook
  | PermissionPromptRequestExtensionManagement
  | PermissionPromptRequestExtensionPermissionAccess;
/**
 * Underlying permission kind that needs path approval
 */
export type PermissionPromptRequestPathAccessKind =
  /** Read access to a filesystem path. */
  | "read"
  /** Shell command access involving a filesystem path. */
  | "shell"
  /** Write access to a filesystem path. */
  | "write";
/**
 * The result of the permission request
 */
export type PermissionResult =
  | PermissionApproved
  | PermissionApprovedForSession
  | PermissionApprovedForLocation
  | PermissionCancelled
  | PermissionDeniedByRules
  | PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser
  | PermissionDeniedInteractivelyByUser
  | PermissionDeniedByContentExclusionPolicy
  | PermissionDeniedByPermissionRequestHook;
/**
 * The approval to add as a session-scoped rule
 */
export type UserToolSessionApproval =
  | UserToolSessionApprovalCommands
  | UserToolSessionApprovalRead
  | UserToolSessionApprovalWrite
  | UserToolSessionApprovalMcp
  | UserToolSessionApprovalMemory
  | UserToolSessionApprovalCustomTool
  | UserToolSessionApprovalExtensionManagement
  | UserToolSessionApprovalExtensionPermissionAccess;
/**
 * Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
 */
export type ElicitationRequestedMode =
  /** Structured form-based elicitation. */
  | "form"
  /** Browser URL-based elicitation. */
  | "url";
/**
 * The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
 */
export type ElicitationCompletedAction =
  /** The user submitted the requested form. */
  | "accept"
  /** The user explicitly declined the request. */
  | "decline"
  /** The user dismissed the request. */
  | "cancel";
/**
 * Schema for the `ElicitationCompletedContent` type.
 */
export type ElicitationCompletedContent = (string | number | boolean | string[]) | undefined;
/**
 * Source-defined JSON payload for the custom notification
 */
export type CustomNotificationPayload =
  | string
  | number
  | boolean
  | null
  | unknown[]
  | {
      [k: string]: unknown | undefined;
    };
/**
 * The user's auto-mode-switch choice
 */
export type AutoModeSwitchResponse =
  /** Switch models for this request. */
  | "yes"
  /** Switch models now and keep using the replacement automatically. */
  | "yes_always"
  /** Do not switch models. */
  | "no";
/**
 * Exit plan mode action
 */
export type ExitPlanModeAction =
  /** Exit plan mode without starting implementation. */
  | "exit_only"
  /** Exit plan mode and continue in interactive mode. */
  | "interactive"
  /** Exit plan mode and continue autonomously. */
  | "autopilot"
  /** Exit plan mode and continue with parallel autonomous workers. */
  | "autopilot_fleet";
/**
 * Source location type (e.g., project, personal-copilot, plugin, builtin)
 */
export type SkillSource =
  /** Skill defined in the current project's skill directories. */
  | "project"
  /** Skill discovered from a parent directory in the current workspace tree. */
  | "inherited"
  /** Skill defined in the user's Copilot skill directory. */
  | "personal-copilot"
  /** Skill defined in the user's personal agents skill directory. */
  | "personal-agents"
  /** Skill provided by an installed plugin. */
  | "plugin"
  /** Skill loaded from a configured custom skill directory. */
  | "custom"
  /** Skill bundled with the runtime. */
  | "builtin";
/**
 * Configuration source: user, workspace, plugin, or builtin
 */
export type McpServerSource =
  /** Server configured in the user's global MCP configuration. */
  | "user"
  /** Server configured by the current workspace. */
  | "workspace"
  /** Server contributed by an installed plugin. */
  | "plugin"
  /** Server bundled with the runtime. */
  | "builtin";
/**
 * Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
 */
export type McpServerStatus =
  /** The server is connected and available. */
  | "connected"
  /** The server failed to connect or initialize. */
  | "failed"
  /** The server requires authentication before it can connect. */
  | "needs-auth"
  /** The server connection is still being established. */
  | "pending"
  /** The server is configured but disabled. */
  | "disabled"
  /** The server is not configured for this session. */
  | "not_configured";
/**
 * Discovery source
 */
export type ExtensionsLoadedExtensionSource =
  /** Extension discovered from the current project. */
  | "project"
  /** Extension discovered from the user's extension directory. */
  | "user";
/**
 * Current status: running, disabled, failed, or starting
 */
export type ExtensionsLoadedExtensionStatus =
  /** The extension process is running. */
  | "running"
  /** The extension is installed but disabled. */
  | "disabled"
  /** The extension failed to start or crashed. */
  | "failed"
  /** The extension process is starting. */
  | "starting";

/**
 * Session event "session.start". Session initialization metadata including context and configuration
 */
export interface StartEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: StartData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.start".
   */
  type: "session.start";
}
/**
 * Session initialization metadata including context and configuration
 */
export interface StartData {
  /**
   * Whether the session was already in use by another client at start time
   */
  alreadyInUse?: boolean;
  context?: WorkingDirectoryContext;
  /**
   * Version string of the Copilot application
   */
  copilotVersion: string;
  /**
   * When set, identifies a parent session whose context this session continues — e.g., a detached headless rem-agent run launched on the parent's interactive shutdown. Telemetry from this session is reported under the parent's session_id.
   */
  detachedFromSpawningParentSessionId?: string;
  /**
   * Identifier of the software producing the events (e.g., "copilot-agent")
   */
  producer: string;
  /**
   * Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
   */
  reasoningEffort?: string;
  reasoningSummary?: ReasoningSummary;
  /**
   * Whether this session supports remote steering via GitHub
   */
  remoteSteerable?: boolean;
  /**
   * Model selected at session creation time, if any
   */
  selectedModel?: string;
  /**
   * Unique identifier for the session
   */
  sessionId: string;
  /**
   * ISO 8601 timestamp when the session was created
   */
  startTime: string;
  /**
   * Schema version number for the session event format
   */
  version: number;
}
/**
 * Working directory and git context at session start
 */
export interface WorkingDirectoryContext {
  /**
   * Base commit of current git branch at session start time
   */
  baseCommit?: string;
  /**
   * Current git branch name
   */
  branch?: string;
  /**
   * Current working directory path
   */
  cwd: string;
  /**
   * Root directory of the git repository, resolved via git rev-parse
   */
  gitRoot?: string;
  /**
   * Head commit of current git branch at session start time
   */
  headCommit?: string;
  hostType?: WorkingDirectoryContextHostType;
  /**
   * Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
   */
  repository?: string;
  /**
   * Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com")
   */
  repositoryHost?: string;
}
/**
 * Session event "session.resume". Session resume metadata including current context and event count
 */
export interface ResumeEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ResumeData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.resume".
   */
  type: "session.resume";
}
/**
 * Session resume metadata including current context and event count
 */
export interface ResumeData {
  /**
   * Whether the session was already in use by another client at resume time
   */
  alreadyInUse?: boolean;
  context?: WorkingDirectoryContext;
  /**
   * When true, tool calls and permission requests left in flight by the previous session lifetime remain pending after resume and the agentic loop awaits their results. User sends are queued behind the pending work until all such requests reach a terminal state. When false (the default), any such tool calls and permission requests are immediately marked as interrupted on resume.
   */
  continuePendingWork?: boolean;
  /**
   * Total number of persisted events in the session at the time of resume
   */
  eventCount: number;
  /**
   * Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
   */
  reasoningEffort?: string;
  reasoningSummary?: ReasoningSummary;
  /**
   * Whether this session supports remote steering via GitHub
   */
  remoteSteerable?: boolean;
  /**
   * ISO 8601 timestamp when the session was resumed
   */
  resumeTime: string;
  /**
   * Model currently selected at resume time
   */
  selectedModel?: string;
  /**
   * True when this resume attached to a session that the runtime already had running in-memory (for example, an extension joining a session another client was actively driving). False (or omitted) for cold resumes — the runtime had to reconstitute the session from its persisted event log.
   */
  sessionWasActive?: boolean;
}
/**
 * Session event "session.remote_steerable_changed". Notifies that the session's remote steering capability has changed
 */
export interface RemoteSteerableChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: RemoteSteerableChangedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.remote_steerable_changed".
   */
  type: "session.remote_steerable_changed";
}
/**
 * Notifies that the session's remote steering capability has changed
 */
export interface RemoteSteerableChangedData {
  /**
   * Whether this session now supports remote steering via GitHub
   */
  remoteSteerable: boolean;
}
/**
 * Session event "session.error". Error details for timeline display including message and optional diagnostic information
 */
export interface ErrorEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ErrorData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.error".
   */
  type: "session.error";
}
/**
 * Error details for timeline display including message and optional diagnostic information
 */
export interface ErrorData {
  /**
   * Only set on `errorType: "rate_limit"`. When `true`, the runtime will follow this error with an `auto_mode_switch.requested` event (or silently switch if `continueOnAutoMode` is enabled). UI clients can use this flag to suppress duplicate rendering of the rate-limit error when they show their own auto-mode-switch prompt.
   */
  eligibleForAutoSwitch?: boolean;
  /**
   * Fine-grained error code from the upstream provider, when available. For `errorType: "rate_limit"`, this is one of the `RateLimitErrorCode` values (e.g., `"user_weekly_rate_limited"`, `"user_global_rate_limited"`, `"rate_limited"`, `"user_model_rate_limited"`, `"integration_rate_limited"`). For `errorType: "quota"`, this is the CAPI quota error code (e.g., `"quota_exceeded"`, `"session_quota_exceeded"`, `"billing_not_configured"`).
   */
  errorCode?: string;
  /**
   * Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "context_limit", "query")
   */
  errorType: string;
  /**
   * Human-readable error message
   */
  message: string;
  /**
   * GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
   */
  providerCallId?: string;
  /**
   * Error stack trace, when available
   */
  stack?: string;
  /**
   * HTTP status code from the upstream request, if applicable
   */
  statusCode?: number;
  /**
   * Optional URL associated with this error that the user can open in a browser
   */
  url?: string;
}
/**
 * Session event "session.idle". Payload indicating the session is idle with no background agents in flight
 */
export interface IdleEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: IdleData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.idle".
   */
  type: "session.idle";
}
/**
 * Payload indicating the session is idle with no background agents in flight
 */
export interface IdleData {
  /**
   * True when the preceding agentic loop was cancelled via abort signal
   */
  aborted?: boolean;
}
/**
 * Session event "session.title_changed". Session title change payload containing the new display title
 */
export interface TitleChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: TitleChangedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.title_changed".
   */
  type: "session.title_changed";
}
/**
 * Session title change payload containing the new display title
 */
export interface TitleChangedData {
  /**
   * The new display title for the session
   */
  title: string;
}
/**
 * Session event "session.schedule_created". Scheduled prompt registered via /every or /after
 */
export interface ScheduleCreatedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ScheduleCreatedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.schedule_created".
   */
  type: "session.schedule_created";
}
/**
 * Scheduled prompt registered via /every or /after
 */
export interface ScheduleCreatedData {
  /**
   * Optional user-facing label shown in the timeline instead of the actual prompt (e.g. `/skill-name args` when the prompt is a skill invocation expansion)
   */
  displayPrompt?: string;
  /**
   * Sequential id assigned to the scheduled prompt within the session
   */
  id: number;
  /**
   * Interval between ticks in milliseconds
   */
  intervalMs: number;
  /**
   * Prompt text that gets enqueued on every tick
   */
  prompt: string;
  /**
   * Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`)
   */
  recurring?: boolean;
}
/**
 * Session event "session.schedule_cancelled". Scheduled prompt cancelled from the schedule manager dialog
 */
export interface ScheduleCancelledEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ScheduleCancelledData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.schedule_cancelled".
   */
  type: "session.schedule_cancelled";
}
/**
 * Scheduled prompt cancelled from the schedule manager dialog
 */
export interface ScheduleCancelledData {
  /**
   * Id of the scheduled prompt that was cancelled
   */
  id: number;
}
/**
 * Session event "session.info". Informational message for timeline display with categorization
 */
export interface InfoEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: InfoData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.info".
   */
  type: "session.info";
}
/**
 * Informational message for timeline display with categorization
 */
export interface InfoData {
  /**
   * Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
   */
  infoType: string;
  /**
   * Human-readable informational message for display in the timeline
   */
  message: string;
  /**
   * Optional actionable tip displayed with this message
   */
  tip?: string;
  /**
   * Optional URL associated with this message that the user can open in a browser
   */
  url?: string;
}
/**
 * Session event "session.warning". Warning message for timeline display with categorization
 */
export interface WarningEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: WarningData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.warning".
   */
  type: "session.warning";
}
/**
 * Warning message for timeline display with categorization
 */
export interface WarningData {
  /**
   * Human-readable warning message for display in the timeline
   */
  message: string;
  /**
   * Optional URL associated with this warning that the user can open in a browser
   */
  url?: string;
  /**
   * Category of warning (e.g., "subscription", "policy", "mcp")
   */
  warningType: string;
}
/**
 * Session event "session.model_change". Model change details including previous and new model identifiers
 */
export interface ModelChangeEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ModelChangeData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.model_change".
   */
  type: "session.model_change";
}
/**
 * Model change details including previous and new model identifiers
 */
export interface ModelChangeData {
  /**
   * Reason the change happened, when not user-initiated. Currently `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path. UI clients can use this to render contextual copy.
   */
  cause?: string;
  /**
   * Newly selected model identifier
   */
  newModel: string;
  /**
   * Model that was previously selected, if any
   */
  previousModel?: string;
  /**
   * Reasoning effort level before the model change, if applicable
   */
  previousReasoningEffort?: string;
  previousReasoningSummary?: ReasoningSummary;
  /**
   * Reasoning effort level after the model change, if applicable
   */
  reasoningEffort?: string | null;
  reasoningSummary?: ReasoningSummary;
}
/**
 * Session event "session.mode_changed". Agent mode change details including previous and new modes
 */
export interface ModeChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ModeChangedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.mode_changed".
   */
  type: "session.mode_changed";
}
/**
 * Agent mode change details including previous and new modes
 */
export interface ModeChangedData {
  newMode: SessionMode;
  previousMode: SessionMode;
}
/**
 * Session event "session.plan_changed". Plan file operation details indicating what changed
 */
export interface PlanChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: PlanChangedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.plan_changed".
   */
  type: "session.plan_changed";
}
/**
 * Plan file operation details indicating what changed
 */
export interface PlanChangedData {
  operation: PlanChangedOperation;
}
/**
 * Session event "session.workspace_file_changed". Workspace file change details including path and operation type
 */
export interface WorkspaceFileChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: WorkspaceFileChangedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.workspace_file_changed".
   */
  type: "session.workspace_file_changed";
}
/**
 * Workspace file change details including path and operation type
 */
export interface WorkspaceFileChangedData {
  operation: WorkspaceFileChangedOperation;
  /**
   * Relative path within the session workspace files directory
   */
  path: string;
}
/**
 * Session event "session.handoff". Session handoff metadata including source, context, and repository information
 */
export interface HandoffEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: HandoffData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.handoff".
   */
  type: "session.handoff";
}
/**
 * Session handoff metadata including source, context, and repository information
 */
export interface HandoffData {
  /**
   * Additional context information for the handoff
   */
  context?: string;
  /**
   * ISO 8601 timestamp when the handoff occurred
   */
  handoffTime: string;
  /**
   * GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com)
   */
  host?: string;
  /**
   * Session ID of the remote session being handed off
   */
  remoteSessionId?: string;
  repository?: HandoffRepository;
  sourceType: HandoffSourceType;
  /**
   * Summary of the work done in the source session
   */
  summary?: string;
}
/**
 * Repository context for the handed-off session
 */
export interface HandoffRepository {
  /**
   * Git branch name, if applicable
   */
  branch?: string;
  /**
   * Repository name
   */
  name: string;
  /**
   * Repository owner (user or organization)
   */
  owner: string;
}
/**
 * Session event "session.truncation". Conversation truncation statistics including token counts and removed content metrics
 */
export interface TruncationEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: TruncationData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.truncation".
   */
  type: "session.truncation";
}
/**
 * Conversation truncation statistics including token counts and removed content metrics
 */
export interface TruncationData {
  /**
   * Number of messages removed by truncation
   */
  messagesRemovedDuringTruncation: number;
  /**
   * Identifier of the component that performed truncation (e.g., "BasicTruncator")
   */
  performedBy: string;
  /**
   * Number of conversation messages after truncation
   */
  postTruncationMessagesLength: number;
  /**
   * Total tokens in conversation messages after truncation
   */
  postTruncationTokensInMessages: number;
  /**
   * Number of conversation messages before truncation
   */
  preTruncationMessagesLength: number;
  /**
   * Total tokens in conversation messages before truncation
   */
  preTruncationTokensInMessages: number;
  /**
   * Maximum token count for the model's context window
   */
  tokenLimit: number;
  /**
   * Number of tokens removed by truncation
   */
  tokensRemovedDuringTruncation: number;
}
/**
 * Session event "session.snapshot_rewind". Session rewind details including target event and count of removed events
 */
export interface SnapshotRewindEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SnapshotRewindData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.snapshot_rewind".
   */
  type: "session.snapshot_rewind";
}
/**
 * Session rewind details including target event and count of removed events
 */
export interface SnapshotRewindData {
  /**
   * Number of events that were removed by the rewind
   */
  eventsRemoved: number;
  /**
   * Event ID that was rewound to; this event and all after it were removed
   */
  upToEventId: string;
}
/**
 * Session event "session.shutdown". Session termination metrics including usage statistics, code changes, and shutdown reason
 */
export interface ShutdownEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ShutdownData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.shutdown".
   */
  type: "session.shutdown";
}
/**
 * Session termination metrics including usage statistics, code changes, and shutdown reason
 */
export interface ShutdownData {
  codeChanges: ShutdownCodeChanges;
  /**
   * Non-system message token count at shutdown
   */
  conversationTokens?: number;
  /**
   * Model that was selected at the time of shutdown
   */
  currentModel?: string;
  /**
   * Total tokens in context window at shutdown
   */
  currentTokens?: number;
  /**
   * Error description when shutdownType is "error"
   */
  errorReason?: string;
  /**
   * Per-model usage breakdown, keyed by model identifier
   */
  modelMetrics: {
    [k: string]: ShutdownModelMetric | undefined;
  };
  /**
   * Unix timestamp (milliseconds) when the session started
   */
  sessionStartTime: number;
  shutdownType: ShutdownType;
  /**
   * System message token count at shutdown
   */
  systemTokens?: number;
  /**
   * Session-wide per-token-type accumulated token counts
   */
  tokenDetails?: {
    [k: string]: ShutdownTokenDetail | undefined;
  };
  /**
   * Tool definitions token count at shutdown
   */
  toolDefinitionsTokens?: number;
  /**
   * Cumulative time spent in API calls during the session, in milliseconds
   */
  totalApiDurationMs: number;
  /**
   * Session-wide accumulated nano-AI units cost
   */
  totalNanoAiu?: number;
  /**
   * Total number of premium API requests used during the session
   */
  totalPremiumRequests?: number;
}
/**
 * Aggregate code change metrics for the session
 */
export interface ShutdownCodeChanges {
  /**
   * List of file paths that were modified during the session
   */
  filesModified: string[];
  /**
   * Total number of lines added during the session
   */
  linesAdded: number;
  /**
   * Total number of lines removed during the session
   */
  linesRemoved: number;
}
/**
 * Schema for the `ShutdownModelMetric` type.
 */
export interface ShutdownModelMetric {
  requests: ShutdownModelMetricRequests;
  /**
   * Token count details per type
   */
  tokenDetails?: {
    [k: string]: ShutdownModelMetricTokenDetail | undefined;
  };
  /**
   * Accumulated nano-AI units cost for this model
   */
  totalNanoAiu?: number;
  usage: ShutdownModelMetricUsage;
}
/**
 * Request count and cost metrics
 */
export interface ShutdownModelMetricRequests {
  /**
   * Cumulative cost multiplier for requests to this model
   */
  cost?: number;
  /**
   * Total number of API requests made to this model
   */
  count?: number;
}
/**
 * Schema for the `ShutdownModelMetricTokenDetail` type.
 */
export interface ShutdownModelMetricTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Token usage breakdown
 */
export interface ShutdownModelMetricUsage {
  /**
   * Total tokens read from prompt cache across all requests
   */
  cacheReadTokens: number;
  /**
   * Total tokens written to prompt cache across all requests
   */
  cacheWriteTokens: number;
  /**
   * Total input tokens consumed across all requests to this model
   */
  inputTokens: number;
  /**
   * Total output tokens produced across all requests to this model
   */
  outputTokens: number;
  /**
   * Total reasoning tokens produced across all requests to this model
   */
  reasoningTokens?: number;
}
/**
 * Schema for the `ShutdownTokenDetail` type.
 */
export interface ShutdownTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Session event "session.context_changed". Updated working directory and git context after the change
 */
export interface ContextChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: WorkingDirectoryContext;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.context_changed".
   */
  type: "session.context_changed";
}
/**
 * Session event "session.usage_info". Current context window usage statistics including token and message counts
 */
export interface UsageInfoEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: UsageInfoData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.usage_info".
   */
  type: "session.usage_info";
}
/**
 * Current context window usage statistics including token and message counts
 */
export interface UsageInfoData {
  /**
   * Token count from non-system messages (user, assistant, tool)
   */
  conversationTokens?: number;
  /**
   * Current number of tokens in the context window
   */
  currentTokens: number;
  /**
   * Whether this is the first usage_info event emitted in this session
   */
  isInitial?: boolean;
  /**
   * Current number of messages in the conversation
   */
  messagesLength: number;
  /**
   * Token count from system message(s)
   */
  systemTokens?: number;
  /**
   * Maximum token count for the model's context window
   */
  tokenLimit: number;
  /**
   * Token count from tool definitions
   */
  toolDefinitionsTokens?: number;
}
/**
 * Session event "session.compaction_start". Context window breakdown at the start of LLM-powered conversation compaction
 */
export interface CompactionStartEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CompactionStartData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.compaction_start".
   */
  type: "session.compaction_start";
}
/**
 * Context window breakdown at the start of LLM-powered conversation compaction
 */
export interface CompactionStartData {
  /**
   * Token count from non-system messages (user, assistant, tool) at compaction start
   */
  conversationTokens?: number;
  /**
   * Token count from system message(s) at compaction start
   */
  systemTokens?: number;
  /**
   * Token count from tool definitions at compaction start
   */
  toolDefinitionsTokens?: number;
}
/**
 * Session event "session.compaction_complete". Conversation compaction results including success status, metrics, and optional error details
 */
export interface CompactionCompleteEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CompactionCompleteData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.compaction_complete".
   */
  type: "session.compaction_complete";
}
/**
 * Conversation compaction results including success status, metrics, and optional error details
 */
export interface CompactionCompleteData {
  /**
   * Checkpoint snapshot number created for recovery
   */
  checkpointNumber?: number;
  /**
   * File path where the checkpoint was stored
   */
  checkpointPath?: string;
  compactionTokensUsed?: CompactionCompleteCompactionTokensUsed;
  /**
   * Token count from non-system messages (user, assistant, tool) after compaction
   */
  conversationTokens?: number;
  /**
   * User-supplied focus instructions provided to a manual `/compact` invocation. Omitted for automatic compaction and for manual compaction with no focus text.
   */
  customInstructions?: string;
  /**
   * Error message if compaction failed
   */
  error?: string;
  /**
   * Number of messages removed during compaction
   */
  messagesRemoved?: number;
  /**
   * Total tokens in conversation after compaction
   */
  postCompactionTokens?: number;
  /**
   * Number of messages before compaction
   */
  preCompactionMessagesLength?: number;
  /**
   * Total tokens in conversation before compaction
   */
  preCompactionTokens?: number;
  /**
   * GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
   */
  requestId?: string;
  /**
   * Whether compaction completed successfully
   */
  success: boolean;
  /**
   * LLM-generated summary of the compacted conversation history
   */
  summaryContent?: string;
  /**
   * Token count from system message(s) after compaction
   */
  systemTokens?: number;
  /**
   * Number of tokens removed during compaction
   */
  tokensRemoved?: number;
  /**
   * Token count from tool definitions after compaction
   */
  toolDefinitionsTokens?: number;
}
/**
 * Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)
 */
export interface CompactionCompleteCompactionTokensUsed {
  /**
   * Cached input tokens reused in the compaction LLM call
   */
  cacheReadTokens?: number;
  /**
   * Tokens written to prompt cache in the compaction LLM call
   */
  cacheWriteTokens?: number;
  copilotUsage?: CompactionCompleteCompactionTokensUsedCopilotUsage;
  /**
   * Duration of the compaction LLM call in milliseconds
   */
  duration?: number;
  /**
   * Input tokens consumed by the compaction LLM call
   */
  inputTokens?: number;
  /**
   * Model identifier used for the compaction LLM call
   */
  model?: string;
  /**
   * Output tokens produced by the compaction LLM call
   */
  outputTokens?: number;
}
/**
 * Per-request cost and usage data from the CAPI copilot_usage response field
 */
export interface CompactionCompleteCompactionTokensUsedCopilotUsage {
  /**
   * Itemized token usage breakdown
   */
  tokenDetails: CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail[];
  /**
   * Total cost in nano-AI units for this request
   */
  totalNanoAiu: number;
}
/**
 * Token usage detail for a single billing category
 */
export interface CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail {
  /**
   * Number of tokens in this billing batch
   */
  batchSize: number;
  /**
   * Cost per batch of tokens
   */
  costPerBatch: number;
  /**
   * Total token count for this entry
   */
  tokenCount: number;
  /**
   * Token category (e.g., "input", "output")
   */
  tokenType: string;
}
/**
 * Session event "session.task_complete". Task completion notification with summary from the agent
 */
export interface TaskCompleteEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: TaskCompleteData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.task_complete".
   */
  type: "session.task_complete";
}
/**
 * Task completion notification with summary from the agent
 */
export interface TaskCompleteData {
  /**
   * Whether the tool call succeeded. False when validation failed (e.g., invalid arguments)
   */
  success?: boolean;
  /**
   * Summary of the completed task, provided by the agent
   */
  summary?: string;
}
/**
 * Session event "user.message".
 */
export interface UserMessageEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: UserMessageData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "user.message".
   */
  type: "user.message";
}
/**
 * Schema for the `UserMessageData` type.
 */
export interface UserMessageData {
  agentMode?: UserMessageAgentMode;
  /**
   * Files, selections, or GitHub references attached to the message
   */
  attachments?: UserMessageAttachment[];
  /**
   * The user's message text as displayed in the timeline
   */
  content: string;
  /**
   * CAPI interaction ID for correlating this user message with its turn
   */
  interactionId?: string;
  /**
   * True when this user message was auto-injected by autopilot's continuation loop rather than typed by the user; used to distinguish autopilot-driven turns in telemetry.
   */
  isAutopilotContinuation?: boolean;
  /**
   * Path-backed native document attachments that stayed on the tagged_files path flow because native upload could not read them or would exceed the request size limit
   */
  nativeDocumentPathFallbackPaths?: string[];
  /**
   * Parent agent task ID for background telemetry correlated to this user turn
   */
  parentAgentTaskId?: string;
  /**
   * Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user)
   */
  source?: string;
  /**
   * Normalized document MIME types that were sent natively instead of through tagged_files XML
   */
  supportedNativeDocumentMimeTypes?: string[];
  /**
   * Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
   */
  transformedContent?: string;
}
/**
 * File attachment
 */
export interface UserMessageAttachmentFile {
  /**
   * User-facing display name for the attachment
   */
  displayName: string;
  lineRange?: UserMessageAttachmentFileLineRange;
  /**
   * Absolute file path
   */
  path: string;
  /**
   * Attachment type discriminator
   */
  type: "file";
}
/**
 * Optional line range to scope the attachment to a specific section of the file
 */
export interface UserMessageAttachmentFileLineRange {
  /**
   * End line number (1-based, inclusive)
   */
  end: number;
  /**
   * Start line number (1-based)
   */
  start: number;
}
/**
 * Directory attachment
 */
export interface UserMessageAttachmentDirectory {
  /**
   * User-facing display name for the attachment
   */
  displayName: string;
  /**
   * Absolute directory path
   */
  path: string;
  /**
   * Attachment type discriminator
   */
  type: "directory";
}
/**
 * Code selection attachment from an editor
 */
export interface UserMessageAttachmentSelection {
  /**
   * User-facing display name for the selection
   */
  displayName: string;
  /**
   * Absolute path to the file containing the selection
   */
  filePath: string;
  selection: UserMessageAttachmentSelectionDetails;
  /**
   * The selected text content
   */
  text: string;
  /**
   * Attachment type discriminator
   */
  type: "selection";
}
/**
 * Position range of the selection within the file
 */
export interface UserMessageAttachmentSelectionDetails {
  end: UserMessageAttachmentSelectionDetailsEnd;
  start: UserMessageAttachmentSelectionDetailsStart;
}
/**
 * End position of the selection
 */
export interface UserMessageAttachmentSelectionDetailsEnd {
  /**
   * End character offset within the line (0-based)
   */
  character: number;
  /**
   * End line number (0-based)
   */
  line: number;
}
/**
 * Start position of the selection
 */
export interface UserMessageAttachmentSelectionDetailsStart {
  /**
   * Start character offset within the line (0-based)
   */
  character: number;
  /**
   * Start line number (0-based)
   */
  line: number;
}
/**
 * GitHub issue, pull request, or discussion reference
 */
export interface UserMessageAttachmentGithubReference {
  /**
   * Issue, pull request, or discussion number
   */
  number: number;
  referenceType: UserMessageAttachmentGithubReferenceType;
  /**
   * Current state of the referenced item (e.g., open, closed, merged)
   */
  state: string;
  /**
   * Title of the referenced item
   */
  title: string;
  /**
   * Attachment type discriminator
   */
  type: "github_reference";
  /**
   * URL to the referenced item on GitHub
   */
  url: string;
}
/**
 * Blob attachment with inline base64-encoded data
 */
export interface UserMessageAttachmentBlob {
  /**
   * Base64-encoded content
   */
  data: string;
  /**
   * User-facing display name for the attachment
   */
  displayName?: string;
  /**
   * MIME type of the inline data
   */
  mimeType: string;
  /**
   * Attachment type discriminator
   */
  type: "blob";
}
/**
 * Session event "pending_messages.modified". Empty payload; the event signals that the pending message queue has changed
 */
export interface PendingMessagesModifiedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: PendingMessagesModifiedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "pending_messages.modified".
   */
  type: "pending_messages.modified";
}
/**
 * Empty payload; the event signals that the pending message queue has changed
 */
export interface PendingMessagesModifiedData {}
/**
 * Session event "assistant.turn_start". Turn initialization metadata including identifier and interaction tracking
 */
export interface AssistantTurnStartEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantTurnStartData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.turn_start".
   */
  type: "assistant.turn_start";
}
/**
 * Turn initialization metadata including identifier and interaction tracking
 */
export interface AssistantTurnStartData {
  /**
   * CAPI interaction ID for correlating this turn with upstream telemetry
   */
  interactionId?: string;
  /**
   * Identifier for this turn within the agentic loop, typically a stringified turn number
   */
  turnId: string;
}
/**
 * Session event "assistant.intent". Agent intent description for current activity or plan
 */
export interface AssistantIntentEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantIntentData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.intent".
   */
  type: "assistant.intent";
}
/**
 * Agent intent description for current activity or plan
 */
export interface AssistantIntentData {
  /**
   * Short description of what the agent is currently doing or planning to do
   */
  intent: string;
}
/**
 * Session event "assistant.reasoning". Assistant reasoning content for timeline display with complete thinking text
 */
export interface AssistantReasoningEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantReasoningData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.reasoning".
   */
  type: "assistant.reasoning";
}
/**
 * Assistant reasoning content for timeline display with complete thinking text
 */
export interface AssistantReasoningData {
  /**
   * The complete extended thinking text from the model
   */
  content: string;
  /**
   * Unique identifier for this reasoning block
   */
  reasoningId: string;
}
/**
 * Session event "assistant.reasoning_delta". Streaming reasoning delta for incremental extended thinking updates
 */
export interface AssistantReasoningDeltaEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantReasoningDeltaData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.reasoning_delta".
   */
  type: "assistant.reasoning_delta";
}
/**
 * Streaming reasoning delta for incremental extended thinking updates
 */
export interface AssistantReasoningDeltaData {
  /**
   * Incremental text chunk to append to the reasoning content
   */
  deltaContent: string;
  /**
   * Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event
   */
  reasoningId: string;
}
/**
 * Session event "assistant.streaming_delta". Streaming response progress with cumulative byte count
 */
export interface AssistantStreamingDeltaEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantStreamingDeltaData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.streaming_delta".
   */
  type: "assistant.streaming_delta";
}
/**
 * Streaming response progress with cumulative byte count
 */
export interface AssistantStreamingDeltaData {
  /**
   * Cumulative total bytes received from the streaming response so far
   */
  totalResponseSizeBytes: number;
}
/**
 * Session event "assistant.message". Assistant response containing text content, optional tool requests, and interaction metadata
 */
export interface AssistantMessageEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantMessageData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.message".
   */
  type: "assistant.message";
}
/**
 * Assistant response containing text content, optional tool requests, and interaction metadata
 */
export interface AssistantMessageData {
  /**
   * Raw Anthropic content array with advisor blocks (server_tool_use, advisor_tool_result) for verbatim round-tripping
   */
  anthropicAdvisorBlocks?: {
    [k: string]: unknown | undefined;
  }[];
  /**
   * Anthropic advisor model ID used for this response, for timeline display on replay
   */
  anthropicAdvisorModel?: string;
  /**
   * The assistant's text response content
   */
  content: string;
  /**
   * Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.
   */
  encryptedContent?: string;
  /**
   * CAPI interaction ID for correlating this message with upstream telemetry
   */
  interactionId?: string;
  /**
   * Unique identifier for this assistant message
   */
  messageId: string;
  /**
   * Model that produced this assistant message, if known
   */
  model?: string;
  /**
   * Actual output token count from the API response (completion_tokens), used for accurate token accounting
   */
  outputTokens?: number;
  /**
   * @deprecated
   * Tool call ID of the parent tool invocation when this event originates from a sub-agent
   */
  parentToolCallId?: string;
  /**
   * Generation phase for phased-output models (e.g., thinking vs. response phases)
   */
  phase?: string;
  /**
   * Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume.
   */
  reasoningOpaque?: string;
  /**
   * Readable reasoning text from the model's extended thinking
   */
  reasoningText?: string;
  /**
   * GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
   */
  requestId?: string;
  /**
   * Tool invocations requested by the assistant in this message
   */
  toolRequests?: AssistantMessageToolRequest[];
  /**
   * Identifier for the agent loop turn that produced this message, matching the corresponding assistant.turn_start event
   */
  turnId?: string;
}
/**
 * A tool invocation request from the assistant
 */
export interface AssistantMessageToolRequest {
  /**
   * Arguments to pass to the tool, format depends on the tool
   */
  arguments?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Resolved intention summary describing what this specific call does
   */
  intentionSummary?: string | null;
  /**
   * Name of the MCP server hosting this tool, when the tool is an MCP tool
   */
  mcpServerName?: string;
  /**
   * Original tool name on the MCP server, when the tool is an MCP tool
   */
  mcpToolName?: string;
  /**
   * Name of the tool being invoked
   */
  name: string;
  /**
   * Unique identifier for this tool call
   */
  toolCallId: string;
  /**
   * Human-readable display title for the tool
   */
  toolTitle?: string;
  type?: AssistantMessageToolRequestType;
}
/**
 * Session event "assistant.message_start". Streaming assistant message start metadata
 */
export interface AssistantMessageStartEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantMessageStartData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.message_start".
   */
  type: "assistant.message_start";
}
/**
 * Streaming assistant message start metadata
 */
export interface AssistantMessageStartData {
  /**
   * Message ID this start event belongs to, matching subsequent deltas and assistant.message
   */
  messageId: string;
  /**
   * Generation phase this message belongs to for phased-output models
   */
  phase?: string;
}
/**
 * Session event "assistant.message_delta". Streaming assistant message delta for incremental response updates
 */
export interface AssistantMessageDeltaEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantMessageDeltaData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.message_delta".
   */
  type: "assistant.message_delta";
}
/**
 * Streaming assistant message delta for incremental response updates
 */
export interface AssistantMessageDeltaData {
  /**
   * Incremental text chunk to append to the message content
   */
  deltaContent: string;
  /**
   * Message ID this delta belongs to, matching the corresponding assistant.message event
   */
  messageId: string;
  /**
   * @deprecated
   * Tool call ID of the parent tool invocation when this event originates from a sub-agent
   */
  parentToolCallId?: string;
}
/**
 * Session event "assistant.turn_end". Turn completion metadata including the turn identifier
 */
export interface AssistantTurnEndEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantTurnEndData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.turn_end".
   */
  type: "assistant.turn_end";
}
/**
 * Turn completion metadata including the turn identifier
 */
export interface AssistantTurnEndData {
  /**
   * Identifier of the turn that has ended, matching the corresponding assistant.turn_start event
   */
  turnId: string;
}
/**
 * Session event "assistant.usage". LLM API call usage metrics including tokens, costs, quotas, and billing information
 */
export interface AssistantUsageEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AssistantUsageData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "assistant.usage".
   */
  type: "assistant.usage";
}
/**
 * LLM API call usage metrics including tokens, costs, quotas, and billing information
 */
export interface AssistantUsageData {
  /**
   * Completion ID from the model provider (e.g., chatcmpl-abc123)
   */
  apiCallId?: string;
  apiEndpoint?: AssistantUsageApiEndpoint;
  /**
   * Number of tokens read from prompt cache
   */
  cacheReadTokens?: number;
  /**
   * Number of tokens written to prompt cache
   */
  cacheWriteTokens?: number;
  copilotUsage?: AssistantUsageCopilotUsage;
  /**
   * Model multiplier cost for billing purposes
   */
  cost?: number;
  /**
   * Duration of the API call in milliseconds
   */
  duration?: number;
  /**
   * What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
   */
  initiator?: string;
  /**
   * Number of input tokens consumed
   */
  inputTokens?: number;
  /**
   * Average inter-token latency in milliseconds. Only available for streaming requests
   */
  interTokenLatencyMs?: number;
  /**
   * Model identifier used for this API call
   */
  model: string;
  /**
   * Number of output tokens produced
   */
  outputTokens?: number;
  /**
   * @deprecated
   * Parent tool call ID when this usage originates from a sub-agent
   */
  parentToolCallId?: string;
  /**
   * GitHub request tracing ID (x-github-request-id header) for server-side log correlation
   */
  providerCallId?: string;
  /**
   * Per-quota resource usage snapshots, keyed by quota identifier
   */
  quotaSnapshots?: {
    [k: string]: AssistantUsageQuotaSnapshot | undefined;
  };
  /**
   * Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
   */
  reasoningEffort?: string;
  /**
   * Number of output tokens used for reasoning (e.g., chain-of-thought)
   */
  reasoningTokens?: number;
  /**
   * Time to first token in milliseconds. Only available for streaming requests
   */
  timeToFirstTokenMs?: number;
}
/**
 * Per-request cost and usage data from the CAPI copilot_usage response field
 */
export interface AssistantUsageCopilotUsage {
  /**
   * Itemized token usage breakdown
   */
  tokenDetails: AssistantUsageCopilotUsageTokenDetail[];
  /**
   * Total cost in nano-AI units for this request
   */
  totalNanoAiu: number;
}
/**
 * Token usage detail for a single billing category
 */
export interface AssistantUsageCopilotUsageTokenDetail {
  /**
   * Number of tokens in this billing batch
   */
  batchSize: number;
  /**
   * Cost per batch of tokens
   */
  costPerBatch: number;
  /**
   * Total token count for this entry
   */
  tokenCount: number;
  /**
   * Token category (e.g., "input", "output")
   */
  tokenType: string;
}
/**
 * Schema for the `AssistantUsageQuotaSnapshot` type.
 */
export interface AssistantUsageQuotaSnapshot {
  /**
   * Total requests allowed by the entitlement
   */
  entitlementRequests: number;
  /**
   * Whether the user has an unlimited usage entitlement
   */
  isUnlimitedEntitlement: boolean;
  /**
   * Number of additional usage requests made this period
   */
  overage: number;
  /**
   * Whether additional usage is allowed when quota is exhausted
   */
  overageAllowedWithExhaustedQuota: boolean;
  /**
   * Percentage of quota remaining (0 to 100)
   */
  remainingPercentage: number;
  /**
   * Date when the quota resets
   */
  resetDate?: string;
  /**
   * Whether usage is still permitted after quota exhaustion
   */
  usageAllowedWithExhaustedQuota: boolean;
  /**
   * Number of requests already consumed
   */
  usedRequests: number;
}
/**
 * Session event "model.call_failure". Failed LLM API call metadata for telemetry
 */
export interface ModelCallFailureEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ModelCallFailureData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "model.call_failure".
   */
  type: "model.call_failure";
}
/**
 * Failed LLM API call metadata for telemetry
 */
export interface ModelCallFailureData {
  /**
   * Completion ID from the model provider (e.g., chatcmpl-abc123)
   */
  apiCallId?: string;
  /**
   * Duration of the failed API call in milliseconds
   */
  durationMs?: number;
  /**
   * Raw provider/runtime error message for restricted telemetry
   */
  errorMessage?: string;
  /**
   * What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
   */
  initiator?: string;
  /**
   * Model identifier used for the failed API call
   */
  model?: string;
  /**
   * GitHub request tracing ID (x-github-request-id header) for server-side log correlation
   */
  providerCallId?: string;
  source: ModelCallFailureSource;
  /**
   * HTTP status code from the failed request
   */
  statusCode?: number;
}
/**
 * Session event "abort". Turn abort information including the reason for termination
 */
export interface AbortEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AbortData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "abort".
   */
  type: "abort";
}
/**
 * Turn abort information including the reason for termination
 */
export interface AbortData {
  reason: AbortReason;
}
/**
 * Session event "tool.user_requested". User-initiated tool invocation request with tool name and arguments
 */
export interface ToolUserRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ToolUserRequestedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "tool.user_requested".
   */
  type: "tool.user_requested";
}
/**
 * User-initiated tool invocation request with tool name and arguments
 */
export interface ToolUserRequestedData {
  /**
   * Arguments for the tool invocation
   */
  arguments?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Unique identifier for this tool call
   */
  toolCallId: string;
  /**
   * Name of the tool the user wants to invoke
   */
  toolName: string;
}
/**
 * Session event "tool.execution_start". Tool execution startup details including MCP server information when applicable
 */
export interface ToolExecutionStartEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ToolExecutionStartData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "tool.execution_start".
   */
  type: "tool.execution_start";
}
/**
 * Tool execution startup details including MCP server information when applicable
 */
export interface ToolExecutionStartData {
  /**
   * Arguments passed to the tool
   */
  arguments?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Name of the MCP server hosting this tool, when the tool is an MCP tool
   */
  mcpServerName?: string;
  /**
   * Original tool name on the MCP server, when the tool is an MCP tool
   */
  mcpToolName?: string;
  /**
   * @deprecated
   * Tool call ID of the parent tool invocation when this event originates from a sub-agent
   */
  parentToolCallId?: string;
  /**
   * Unique identifier for this tool call
   */
  toolCallId: string;
  /**
   * Name of the tool being executed
   */
  toolName: string;
  /**
   * Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event
   */
  turnId?: string;
}
/**
 * Session event "tool.execution_partial_result". Streaming tool execution output for incremental result display
 */
export interface ToolExecutionPartialResultEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ToolExecutionPartialData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "tool.execution_partial_result".
   */
  type: "tool.execution_partial_result";
}
/**
 * Streaming tool execution output for incremental result display
 */
export interface ToolExecutionPartialData {
  /**
   * Incremental output chunk from the running tool
   */
  partialOutput: string;
  /**
   * Tool call ID this partial result belongs to
   */
  toolCallId: string;
}
/**
 * Session event "tool.execution_progress". Tool execution progress notification with status message
 */
export interface ToolExecutionProgressEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ToolExecutionProgressData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "tool.execution_progress".
   */
  type: "tool.execution_progress";
}
/**
 * Tool execution progress notification with status message
 */
export interface ToolExecutionProgressData {
  /**
   * Human-readable progress status message (e.g., from an MCP server)
   */
  progressMessage: string;
  /**
   * Tool call ID this progress notification belongs to
   */
  toolCallId: string;
}
/**
 * Session event "tool.execution_complete". Tool execution completion results including success status, detailed output, and error information
 */
export interface ToolExecutionCompleteEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ToolExecutionCompleteData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "tool.execution_complete".
   */
  type: "tool.execution_complete";
}
/**
 * Tool execution completion results including success status, detailed output, and error information
 */
export interface ToolExecutionCompleteData {
  error?: ToolExecutionCompleteError;
  /**
   * CAPI interaction ID for correlating this tool execution with upstream telemetry
   */
  interactionId?: string;
  /**
   * Whether this tool call was explicitly requested by the user rather than the assistant
   */
  isUserRequested?: boolean;
  /**
   * Model identifier that generated this tool call
   */
  model?: string;
  /**
   * @deprecated
   * Tool call ID of the parent tool invocation when this event originates from a sub-agent
   */
  parentToolCallId?: string;
  result?: ToolExecutionCompleteResult;
  /**
   * Whether this tool execution ran inside a sandbox container
   */
  sandboxed?: boolean;
  /**
   * Whether the tool execution completed successfully
   */
  success: boolean;
  /**
   * Unique identifier for the completed tool call
   */
  toolCallId: string;
  /**
   * Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
   */
  toolTelemetry?: {
    [k: string]:
      | {
          [k: string]: unknown | undefined;
        }
      | undefined;
  };
  /**
   * Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event
   */
  turnId?: string;
}
/**
 * Error details when the tool execution failed
 */
export interface ToolExecutionCompleteError {
  /**
   * Machine-readable error code
   */
  code?: string;
  /**
   * Human-readable error message
   */
  message: string;
}
/**
 * Tool execution result on success
 */
export interface ToolExecutionCompleteResult {
  /**
   * Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency
   */
  content: string;
  /**
   * Structured content blocks (text, images, audio, resources) returned by the tool in their native format
   */
  contents?: ToolExecutionCompleteContent[];
  /**
   * Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent.
   */
  detailedContent?: string;
}
/**
 * Plain text content block
 */
export interface ToolExecutionCompleteContentText {
  /**
   * The text content
   */
  text: string;
  /**
   * Content block type discriminator
   */
  type: "text";
}
/**
 * Terminal/shell output content block with optional exit code and working directory
 */
export interface ToolExecutionCompleteContentTerminal {
  /**
   * Working directory where the command was executed
   */
  cwd?: string;
  /**
   * Process exit code, if the command has completed
   */
  exitCode?: number;
  /**
   * Terminal/shell output text
   */
  text: string;
  /**
   * Content block type discriminator
   */
  type: "terminal";
}
/**
 * Image content block with base64-encoded data
 */
export interface ToolExecutionCompleteContentImage {
  /**
   * Base64-encoded image data
   */
  data: string;
  /**
   * MIME type of the image (e.g., image/png, image/jpeg)
   */
  mimeType: string;
  /**
   * Content block type discriminator
   */
  type: "image";
}
/**
 * Audio content block with base64-encoded data
 */
export interface ToolExecutionCompleteContentAudio {
  /**
   * Base64-encoded audio data
   */
  data: string;
  /**
   * MIME type of the audio (e.g., audio/wav, audio/mpeg)
   */
  mimeType: string;
  /**
   * Content block type discriminator
   */
  type: "audio";
}
/**
 * Resource link content block referencing an external resource
 */
export interface ToolExecutionCompleteContentResourceLink {
  /**
   * Human-readable description of the resource
   */
  description?: string;
  /**
   * Icons associated with this resource
   */
  icons?: ToolExecutionCompleteContentResourceLinkIcon[];
  /**
   * MIME type of the resource content
   */
  mimeType?: string;
  /**
   * Resource name identifier
   */
  name: string;
  /**
   * Size of the resource in bytes
   */
  size?: number;
  /**
   * Human-readable display title for the resource
   */
  title?: string;
  /**
   * Content block type discriminator
   */
  type: "resource_link";
  /**
   * URI identifying the resource
   */
  uri: string;
}
/**
 * Icon image for a resource
 */
export interface ToolExecutionCompleteContentResourceLinkIcon {
  /**
   * MIME type of the icon image
   */
  mimeType?: string;
  /**
   * Available icon sizes (e.g., ['16x16', '32x32'])
   */
  sizes?: string[];
  /**
   * URL or path to the icon image
   */
  src: string;
  theme?: ToolExecutionCompleteContentResourceLinkIconTheme;
}
/**
 * Embedded resource content block with inline text or binary data
 */
export interface ToolExecutionCompleteContentResource {
  resource: ToolExecutionCompleteContentResourceDetails;
  /**
   * Content block type discriminator
   */
  type: "resource";
}
/**
 * Schema for the `EmbeddedTextResourceContents` type.
 */
export interface EmbeddedTextResourceContents {
  /**
   * MIME type of the text content
   */
  mimeType?: string;
  /**
   * Text content of the resource
   */
  text: string;
  /**
   * URI identifying the resource
   */
  uri: string;
}
/**
 * Schema for the `EmbeddedBlobResourceContents` type.
 */
export interface EmbeddedBlobResourceContents {
  /**
   * Base64-encoded binary content of the resource
   */
  blob: string;
  /**
   * MIME type of the blob content
   */
  mimeType?: string;
  /**
   * URI identifying the resource
   */
  uri: string;
}
/**
 * Session event "skill.invoked". Skill invocation details including content, allowed tools, and plugin metadata
 */
export interface SkillInvokedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SkillInvokedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "skill.invoked".
   */
  type: "skill.invoked";
}
/**
 * Skill invocation details including content, allowed tools, and plugin metadata
 */
export interface SkillInvokedData {
  /**
   * Tool names that should be auto-approved when this skill is active
   */
  allowedTools?: string[];
  /**
   * Full content of the skill file, injected into the conversation for the model
   */
  content: string;
  /**
   * Description of the skill from its SKILL.md frontmatter
   */
  description?: string;
  /**
   * Name of the invoked skill
   */
  name: string;
  /**
   * File path to the SKILL.md definition
   */
  path: string;
  /**
   * Name of the plugin this skill originated from, when applicable
   */
  pluginName?: string;
  /**
   * Version of the plugin this skill originated from, when applicable
   */
  pluginVersion?: string;
}
/**
 * Session event "subagent.started". Sub-agent startup details including parent tool call and agent information
 */
export interface SubagentStartedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SubagentStartedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "subagent.started".
   */
  type: "subagent.started";
}
/**
 * Sub-agent startup details including parent tool call and agent information
 */
export interface SubagentStartedData {
  /**
   * Description of what the sub-agent does
   */
  agentDescription: string;
  /**
   * Human-readable display name of the sub-agent
   */
  agentDisplayName: string;
  /**
   * Internal name of the sub-agent
   */
  agentName: string;
  /**
   * Model the sub-agent will run with, when known at start. Surfaced in the timeline for auto-selected sub-agents (e.g. rubber-duck).
   */
  model?: string;
  /**
   * Tool call ID of the parent tool invocation that spawned this sub-agent
   */
  toolCallId: string;
}
/**
 * Session event "subagent.completed". Sub-agent completion details for successful execution
 */
export interface SubagentCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SubagentCompletedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "subagent.completed".
   */
  type: "subagent.completed";
}
/**
 * Sub-agent completion details for successful execution
 */
export interface SubagentCompletedData {
  /**
   * Human-readable display name of the sub-agent
   */
  agentDisplayName: string;
  /**
   * Internal name of the sub-agent
   */
  agentName: string;
  /**
   * Wall-clock duration of the sub-agent execution in milliseconds
   */
  durationMs?: number;
  /**
   * Model used by the sub-agent
   */
  model?: string;
  /**
   * Tool call ID of the parent tool invocation that spawned this sub-agent
   */
  toolCallId: string;
  /**
   * Total tokens (input + output) consumed by the sub-agent
   */
  totalTokens?: number;
  /**
   * Total number of tool calls made by the sub-agent
   */
  totalToolCalls?: number;
}
/**
 * Session event "subagent.failed". Sub-agent failure details including error message and agent information
 */
export interface SubagentFailedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SubagentFailedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "subagent.failed".
   */
  type: "subagent.failed";
}
/**
 * Sub-agent failure details including error message and agent information
 */
export interface SubagentFailedData {
  /**
   * Human-readable display name of the sub-agent
   */
  agentDisplayName: string;
  /**
   * Internal name of the sub-agent
   */
  agentName: string;
  /**
   * Wall-clock duration of the sub-agent execution in milliseconds
   */
  durationMs?: number;
  /**
   * Error message describing why the sub-agent failed
   */
  error: string;
  /**
   * Model used by the sub-agent (if any model calls succeeded before failure)
   */
  model?: string;
  /**
   * Tool call ID of the parent tool invocation that spawned this sub-agent
   */
  toolCallId: string;
  /**
   * Total tokens (input + output) consumed before the sub-agent failed
   */
  totalTokens?: number;
  /**
   * Total number of tool calls made before the sub-agent failed
   */
  totalToolCalls?: number;
}
/**
 * Session event "subagent.selected". Custom agent selection details including name and available tools
 */
export interface SubagentSelectedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SubagentSelectedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "subagent.selected".
   */
  type: "subagent.selected";
}
/**
 * Custom agent selection details including name and available tools
 */
export interface SubagentSelectedData {
  /**
   * Human-readable display name of the selected custom agent
   */
  agentDisplayName: string;
  /**
   * Internal name of the selected custom agent
   */
  agentName: string;
  /**
   * List of tool names available to this agent, or null for all tools
   */
  tools: string[] | null;
}
/**
 * Session event "subagent.deselected". Empty payload; the event signals that the custom agent was deselected, returning to the default agent
 */
export interface SubagentDeselectedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SubagentDeselectedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "subagent.deselected".
   */
  type: "subagent.deselected";
}
/**
 * Empty payload; the event signals that the custom agent was deselected, returning to the default agent
 */
export interface SubagentDeselectedData {}
/**
 * Session event "hook.start". Hook invocation start details including type and input data
 */
export interface HookStartEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: HookStartData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "hook.start".
   */
  type: "hook.start";
}
/**
 * Hook invocation start details including type and input data
 */
export interface HookStartData {
  /**
   * Unique identifier for this hook invocation
   */
  hookInvocationId: string;
  /**
   * Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
   */
  hookType: string;
  /**
   * Input data passed to the hook
   */
  input?: {
    [k: string]: unknown | undefined;
  };
}
/**
 * Session event "hook.end". Hook invocation completion details including output, success status, and error information
 */
export interface HookEndEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: HookEndData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "hook.end".
   */
  type: "hook.end";
}
/**
 * Hook invocation completion details including output, success status, and error information
 */
export interface HookEndData {
  error?: HookEndError;
  /**
   * Identifier matching the corresponding hook.start event
   */
  hookInvocationId: string;
  /**
   * Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
   */
  hookType: string;
  /**
   * Output data produced by the hook
   */
  output?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Whether the hook completed successfully
   */
  success: boolean;
}
/**
 * Error details when the hook failed
 */
export interface HookEndError {
  /**
   * Human-readable error message
   */
  message: string;
  /**
   * Error stack trace, when available
   */
  stack?: string;
}
/**
 * Session event "system.message". System/developer instruction content with role and optional template metadata
 */
export interface SystemMessageEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SystemMessageData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "system.message".
   */
  type: "system.message";
}
/**
 * System/developer instruction content with role and optional template metadata
 */
export interface SystemMessageData {
  /**
   * The system or developer prompt text sent as model input
   */
  content: string;
  metadata?: SystemMessageMetadata;
  /**
   * Optional name identifier for the message source
   */
  name?: string;
  role: SystemMessageRole;
}
/**
 * Metadata about the prompt template and its construction
 */
export interface SystemMessageMetadata {
  /**
   * Version identifier of the prompt template used
   */
  promptVersion?: string;
  /**
   * Template variables used when constructing the prompt
   */
  variables?: {
    [k: string]:
      | {
          [k: string]: unknown | undefined;
        }
      | undefined;
  };
}
/**
 * Session event "system.notification". System-generated notification for runtime events like background task completion
 */
export interface SystemNotificationEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SystemNotificationData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "system.notification".
   */
  type: "system.notification";
}
/**
 * System-generated notification for runtime events like background task completion
 */
export interface SystemNotificationData {
  /**
   * The notification text, typically wrapped in <system_notification> XML tags
   */
  content: string;
  kind: SystemNotification;
}
/**
 * Schema for the `SystemNotificationAgentCompleted` type.
 */
export interface SystemNotificationAgentCompleted {
  /**
   * Unique identifier of the background agent
   */
  agentId: string;
  /**
   * Type of the agent (e.g., explore, task, general-purpose)
   */
  agentType: string;
  /**
   * Human-readable description of the agent task
   */
  description?: string;
  /**
   * The full prompt given to the background agent
   */
  prompt?: string;
  status: SystemNotificationAgentCompletedStatus;
  /**
   * Type discriminator. Always "agent_completed".
   */
  type: "agent_completed";
}
/**
 * Schema for the `SystemNotificationAgentIdle` type.
 */
export interface SystemNotificationAgentIdle {
  /**
   * Unique identifier of the background agent
   */
  agentId: string;
  /**
   * Type of the agent (e.g., explore, task, general-purpose)
   */
  agentType: string;
  /**
   * Human-readable description of the agent task
   */
  description?: string;
  /**
   * Type discriminator. Always "agent_idle".
   */
  type: "agent_idle";
}
/**
 * Schema for the `SystemNotificationNewInboxMessage` type.
 */
export interface SystemNotificationNewInboxMessage {
  /**
   * Unique identifier of the inbox entry
   */
  entryId: string;
  /**
   * Human-readable name of the sender
   */
  senderName: string;
  /**
   * Category of the sender (e.g., sidekick-agent, plugin, hook)
   */
  senderType: string;
  /**
   * Short summary shown before the agent decides whether to read the inbox
   */
  summary: string;
  /**
   * Type discriminator. Always "new_inbox_message".
   */
  type: "new_inbox_message";
}
/**
 * Schema for the `SystemNotificationShellCompleted` type.
 */
export interface SystemNotificationShellCompleted {
  /**
   * Human-readable description of the command
   */
  description?: string;
  /**
   * Exit code of the shell command, if available
   */
  exitCode?: number;
  /**
   * Unique identifier of the shell session
   */
  shellId: string;
  /**
   * Type discriminator. Always "shell_completed".
   */
  type: "shell_completed";
}
/**
 * Schema for the `SystemNotificationShellDetachedCompleted` type.
 */
export interface SystemNotificationShellDetachedCompleted {
  /**
   * Human-readable description of the command
   */
  description?: string;
  /**
   * Unique identifier of the detached shell session
   */
  shellId: string;
  /**
   * Type discriminator. Always "shell_detached_completed".
   */
  type: "shell_detached_completed";
}
/**
 * Schema for the `SystemNotificationInstructionDiscovered` type.
 */
export interface SystemNotificationInstructionDiscovered {
  /**
   * Human-readable label for the timeline (e.g., 'AGENTS.md from packages/billing/')
   */
  description?: string;
  /**
   * Relative path to the discovered instruction file
   */
  sourcePath: string;
  /**
   * Path of the file access that triggered discovery
   */
  triggerFile: string;
  /**
   * Tool command that triggered discovery (currently always 'view')
   */
  triggerTool: string;
  /**
   * Type discriminator. Always "instruction_discovered".
   */
  type: "instruction_discovered";
}
/**
 * Session event "permission.requested". Permission request notification requiring client approval with request details
 */
export interface PermissionRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: PermissionRequestedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "permission.requested".
   */
  type: "permission.requested";
}
/**
 * Permission request notification requiring client approval with request details
 */
export interface PermissionRequestedData {
  permissionRequest: PermissionRequest;
  promptRequest?: PermissionPromptRequest;
  /**
   * Unique identifier for this permission request; used to respond via session.respondToPermission()
   */
  requestId: string;
  /**
   * When true, this permission was already resolved by a permissionRequest hook and requires no client action
   */
  resolvedByHook?: boolean;
}
/**
 * Shell command permission request
 */
export interface PermissionRequestShell {
  /**
   * Whether the UI can offer session-wide approval for this command pattern
   */
  canOfferSessionApproval: boolean;
  /**
   * Parsed command identifiers found in the command text
   */
  commands: PermissionRequestShellCommand[];
  /**
   * The complete shell command text to be executed
   */
  fullCommandText: string;
  /**
   * Whether the command includes a file write redirection (e.g., > or >>)
   */
  hasWriteFileRedirection: boolean;
  /**
   * Human-readable description of what the command intends to do
   */
  intention: string;
  /**
   * Permission kind discriminator
   */
  kind: "shell";
  /**
   * File paths that may be read or written by the command
   */
  possiblePaths: string[];
  /**
   * URLs that may be accessed by the command
   */
  possibleUrls: PermissionRequestShellPossibleUrl[];
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Optional warning message about risks of running this command
   */
  warning?: string;
}
/**
 * Schema for the `PermissionRequestShellCommand` type.
 */
export interface PermissionRequestShellCommand {
  /**
   * Command identifier (e.g., executable name)
   */
  identifier: string;
  /**
   * Whether this command is read-only (no side effects)
   */
  readOnly: boolean;
}
/**
 * Schema for the `PermissionRequestShellPossibleUrl` type.
 */
export interface PermissionRequestShellPossibleUrl {
  /**
   * URL that may be accessed by the command
   */
  url: string;
}
/**
 * File write permission request
 */
export interface PermissionRequestWrite {
  /**
   * Whether the UI can offer session-wide approval for file write operations
   */
  canOfferSessionApproval: boolean;
  /**
   * Unified diff showing the proposed changes
   */
  diff: string;
  /**
   * Path of the file being written to
   */
  fileName: string;
  /**
   * Human-readable description of the intended file change
   */
  intention: string;
  /**
   * Permission kind discriminator
   */
  kind: "write";
  /**
   * Complete new file contents for newly created files
   */
  newFileContents?: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * File or directory read permission request
 */
export interface PermissionRequestRead {
  /**
   * Human-readable description of why the file is being read
   */
  intention: string;
  /**
   * Permission kind discriminator
   */
  kind: "read";
  /**
   * Path of the file or directory being read
   */
  path: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * MCP tool invocation permission request
 */
export interface PermissionRequestMcp {
  /**
   * Arguments to pass to the MCP tool
   */
  args?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Permission kind discriminator
   */
  kind: "mcp";
  /**
   * Whether this MCP tool is read-only (no side effects)
   */
  readOnly: boolean;
  /**
   * Name of the MCP server providing the tool
   */
  serverName: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Internal name of the MCP tool
   */
  toolName: string;
  /**
   * Human-readable title of the MCP tool
   */
  toolTitle: string;
}
/**
 * URL access permission request
 */
export interface PermissionRequestUrl {
  /**
   * Human-readable description of why the URL is being accessed
   */
  intention: string;
  /**
   * Permission kind discriminator
   */
  kind: "url";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * URL to be fetched
   */
  url: string;
}
/**
 * Memory operation permission request
 */
export interface PermissionRequestMemory {
  action?: PermissionRequestMemoryAction;
  /**
   * Source references for the stored fact (store only)
   */
  citations?: string;
  direction?: PermissionRequestMemoryDirection;
  /**
   * The fact being stored or voted on
   */
  fact: string;
  /**
   * Permission kind discriminator
   */
  kind: "memory";
  /**
   * Reason for the vote (vote only)
   */
  reason?: string;
  /**
   * Topic or subject of the memory (store only)
   */
  subject?: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Custom tool invocation permission request
 */
export interface PermissionRequestCustomTool {
  /**
   * Arguments to pass to the custom tool
   */
  args?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Permission kind discriminator
   */
  kind: "custom-tool";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Description of what the custom tool does
   */
  toolDescription: string;
  /**
   * Name of the custom tool
   */
  toolName: string;
}
/**
 * Hook confirmation permission request
 */
export interface PermissionRequestHook {
  /**
   * Optional message from the hook explaining why confirmation is needed
   */
  hookMessage?: string;
  /**
   * Permission kind discriminator
   */
  kind: "hook";
  /**
   * Arguments of the tool call being gated
   */
  toolArgs?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Name of the tool the hook is gating
   */
  toolName: string;
}
/**
 * Extension management permission request
 */
export interface PermissionRequestExtensionManagement {
  /**
   * Name of the extension being managed
   */
  extensionName?: string;
  /**
   * Permission kind discriminator
   */
  kind: "extension-management";
  /**
   * The extension management operation (scaffold, reload)
   */
  operation: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Extension permission access request
 */
export interface PermissionRequestExtensionPermissionAccess {
  /**
   * Capabilities the extension is requesting
   */
  capabilities: string[];
  /**
   * Name of the extension requesting permission access
   */
  extensionName: string;
  /**
   * Permission kind discriminator
   */
  kind: "extension-permission-access";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Shell command permission prompt
 */
export interface PermissionPromptRequestCommands {
  /**
   * Whether the UI can offer session-wide approval for this command pattern
   */
  canOfferSessionApproval: boolean;
  /**
   * Command identifiers covered by this approval prompt
   */
  commandIdentifiers: string[];
  /**
   * The complete shell command text to be executed
   */
  fullCommandText: string;
  /**
   * Human-readable description of what the command intends to do
   */
  intention: string;
  /**
   * Prompt kind discriminator
   */
  kind: "commands";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Optional warning message about risks of running this command
   */
  warning?: string;
}
/**
 * File write permission prompt
 */
export interface PermissionPromptRequestWrite {
  /**
   * Whether the UI can offer session-wide approval for file write operations
   */
  canOfferSessionApproval: boolean;
  /**
   * Unified diff showing the proposed changes
   */
  diff: string;
  /**
   * Path of the file being written to
   */
  fileName: string;
  /**
   * Human-readable description of the intended file change
   */
  intention: string;
  /**
   * Prompt kind discriminator
   */
  kind: "write";
  /**
   * Complete new file contents for newly created files
   */
  newFileContents?: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * File read permission prompt
 */
export interface PermissionPromptRequestRead {
  /**
   * Human-readable description of why the file is being read
   */
  intention: string;
  /**
   * Prompt kind discriminator
   */
  kind: "read";
  /**
   * Path of the file or directory being read
   */
  path: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * MCP tool invocation permission prompt
 */
export interface PermissionPromptRequestMcp {
  args?: unknown;
  /**
   * Prompt kind discriminator
   */
  kind: "mcp";
  /**
   * Name of the MCP server providing the tool
   */
  serverName: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Internal name of the MCP tool
   */
  toolName: string;
  /**
   * Human-readable title of the MCP tool
   */
  toolTitle: string;
}
/**
 * URL access permission prompt
 */
export interface PermissionPromptRequestUrl {
  /**
   * Human-readable description of why the URL is being accessed
   */
  intention: string;
  /**
   * Prompt kind discriminator
   */
  kind: "url";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * URL to be fetched
   */
  url: string;
}
/**
 * Memory operation permission prompt
 */
export interface PermissionPromptRequestMemory {
  action?: PermissionRequestMemoryAction;
  /**
   * Source references for the stored fact (store only)
   */
  citations?: string;
  direction?: PermissionRequestMemoryDirection;
  /**
   * The fact being stored or voted on
   */
  fact: string;
  /**
   * Prompt kind discriminator
   */
  kind: "memory";
  /**
   * Reason for the vote (vote only)
   */
  reason?: string;
  /**
   * Topic or subject of the memory (store only)
   */
  subject?: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Custom tool invocation permission prompt
 */
export interface PermissionPromptRequestCustomTool {
  /**
   * Arguments to pass to the custom tool
   */
  args?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Prompt kind discriminator
   */
  kind: "custom-tool";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Description of what the custom tool does
   */
  toolDescription: string;
  /**
   * Name of the custom tool
   */
  toolName: string;
}
/**
 * Path access permission prompt
 */
export interface PermissionPromptRequestPath {
  accessKind: PermissionPromptRequestPathAccessKind;
  /**
   * Prompt kind discriminator
   */
  kind: "path";
  /**
   * File paths that require explicit approval
   */
  paths: string[];
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Hook confirmation permission prompt
 */
export interface PermissionPromptRequestHook {
  /**
   * Optional message from the hook explaining why confirmation is needed
   */
  hookMessage?: string;
  /**
   * Prompt kind discriminator
   */
  kind: "hook";
  /**
   * Arguments of the tool call being gated
   */
  toolArgs?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
  /**
   * Name of the tool the hook is gating
   */
  toolName: string;
}
/**
 * Extension management permission prompt
 */
export interface PermissionPromptRequestExtensionManagement {
  /**
   * Name of the extension being managed
   */
  extensionName?: string;
  /**
   * Prompt kind discriminator
   */
  kind: "extension-management";
  /**
   * The extension management operation (scaffold, reload)
   */
  operation: string;
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Extension permission access prompt
 */
export interface PermissionPromptRequestExtensionPermissionAccess {
  /**
   * Capabilities the extension is requesting
   */
  capabilities: string[];
  /**
   * Name of the extension requesting permission access
   */
  extensionName: string;
  /**
   * Prompt kind discriminator
   */
  kind: "extension-permission-access";
  /**
   * Tool call ID that triggered this permission request
   */
  toolCallId?: string;
}
/**
 * Session event "permission.completed". Permission request completion notification signaling UI dismissal
 */
export interface PermissionCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: PermissionCompletedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "permission.completed".
   */
  type: "permission.completed";
}
/**
 * Permission request completion notification signaling UI dismissal
 */
export interface PermissionCompletedData {
  /**
   * Request ID of the resolved permission request; clients should dismiss any UI for this request
   */
  requestId: string;
  result: PermissionResult;
  /**
   * Optional tool call ID associated with this permission prompt; clients may use it to correlate UI created from tool-scoped prompts
   */
  toolCallId?: string;
}
/**
 * Schema for the `PermissionApproved` type.
 */
export interface PermissionApproved {
  /**
   * The permission request was approved
   */
  kind: "approved";
}
/**
 * Schema for the `PermissionApprovedForSession` type.
 */
export interface PermissionApprovedForSession {
  approval: UserToolSessionApproval;
  /**
   * Approved and remembered for the rest of the session
   */
  kind: "approved-for-session";
}
/**
 * Schema for the `UserToolSessionApprovalCommands` type.
 */
export interface UserToolSessionApprovalCommands {
  /**
   * Command identifiers approved by the user
   */
  commandIdentifiers: string[];
  /**
   * Command approval kind
   */
  kind: "commands";
}
/**
 * Schema for the `UserToolSessionApprovalRead` type.
 */
export interface UserToolSessionApprovalRead {
  /**
   * Read approval kind
   */
  kind: "read";
}
/**
 * Schema for the `UserToolSessionApprovalWrite` type.
 */
export interface UserToolSessionApprovalWrite {
  /**
   * Write approval kind
   */
  kind: "write";
}
/**
 * Schema for the `UserToolSessionApprovalMcp` type.
 */
export interface UserToolSessionApprovalMcp {
  /**
   * MCP tool approval kind
   */
  kind: "mcp";
  /**
   * MCP server name
   */
  serverName: string;
  /**
   * Optional MCP tool name, or null for all tools on the server
   */
  toolName: string | null;
}
/**
 * Schema for the `UserToolSessionApprovalMemory` type.
 */
export interface UserToolSessionApprovalMemory {
  /**
   * Memory approval kind
   */
  kind: "memory";
}
/**
 * Schema for the `UserToolSessionApprovalCustomTool` type.
 */
export interface UserToolSessionApprovalCustomTool {
  /**
   * Custom tool approval kind
   */
  kind: "custom-tool";
  /**
   * Custom tool name
   */
  toolName: string;
}
/**
 * Schema for the `UserToolSessionApprovalExtensionManagement` type.
 */
export interface UserToolSessionApprovalExtensionManagement {
  /**
   * Extension management approval kind
   */
  kind: "extension-management";
  /**
   * Optional operation identifier
   */
  operation?: string;
}
/**
 * Schema for the `UserToolSessionApprovalExtensionPermissionAccess` type.
 */
export interface UserToolSessionApprovalExtensionPermissionAccess {
  /**
   * Extension name
   */
  extensionName: string;
  /**
   * Extension permission access approval kind
   */
  kind: "extension-permission-access";
}
/**
 * Schema for the `PermissionApprovedForLocation` type.
 */
export interface PermissionApprovedForLocation {
  approval: UserToolSessionApproval;
  /**
   * Approved and persisted for this project location
   */
  kind: "approved-for-location";
  /**
   * The location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
}
/**
 * Schema for the `PermissionCancelled` type.
 */
export interface PermissionCancelled {
  /**
   * The permission request was cancelled before a response was used
   */
  kind: "cancelled";
  /**
   * Optional explanation of why the request was cancelled
   */
  reason?: string;
}
/**
 * Schema for the `PermissionDeniedByRules` type.
 */
export interface PermissionDeniedByRules {
  /**
   * Denied because approval rules explicitly blocked it
   */
  kind: "denied-by-rules";
  /**
   * Rules that denied the request
   */
  rules: PermissionRule[];
}
/**
 * Schema for the `PermissionRule` type.
 */
export interface PermissionRule {
  /**
   * Argument value matched against the request, or null when the rule kind has no argument (e.g. 'read', 'write', 'memory').
   */
  argument: string | null;
  /**
   * The rule kind, such as Shell or GitHubMCP
   */
  kind: string;
}
/**
 * Schema for the `PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type.
 */
export interface PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser {
  /**
   * Denied because no approval rule matched and user confirmation was unavailable
   */
  kind: "denied-no-approval-rule-and-could-not-request-from-user";
}
/**
 * Schema for the `PermissionDeniedInteractivelyByUser` type.
 */
export interface PermissionDeniedInteractivelyByUser {
  /**
   * Optional feedback from the user explaining the denial
   */
  feedback?: string;
  /**
   * Whether to force-reject the current agent turn
   */
  forceReject?: boolean;
  /**
   * Denied by the user during an interactive prompt
   */
  kind: "denied-interactively-by-user";
}
/**
 * Schema for the `PermissionDeniedByContentExclusionPolicy` type.
 */
export interface PermissionDeniedByContentExclusionPolicy {
  /**
   * Denied by the organization's content exclusion policy
   */
  kind: "denied-by-content-exclusion-policy";
  /**
   * Human-readable explanation of why the path was excluded
   */
  message: string;
  /**
   * File path that triggered the exclusion
   */
  path: string;
}
/**
 * Schema for the `PermissionDeniedByPermissionRequestHook` type.
 */
export interface PermissionDeniedByPermissionRequestHook {
  /**
   * Whether to interrupt the current agent turn
   */
  interrupt?: boolean;
  /**
   * Denied by a permission request hook registered by an extension or plugin
   */
  kind: "denied-by-permission-request-hook";
  /**
   * Optional message from the hook explaining the denial
   */
  message?: string;
}
/**
 * Session event "user_input.requested". User input request notification with question and optional predefined choices
 */
export interface UserInputRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: UserInputRequestedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "user_input.requested".
   */
  type: "user_input.requested";
}
/**
 * User input request notification with question and optional predefined choices
 */
export interface UserInputRequestedData {
  /**
   * Whether the user can provide a free-form text response in addition to predefined choices
   */
  allowFreeform?: boolean;
  /**
   * Predefined choices for the user to select from, if applicable
   */
  choices?: string[];
  /**
   * The question or prompt to present to the user
   */
  question: string;
  /**
   * Unique identifier for this input request; used to respond via session.respondToUserInput()
   */
  requestId: string;
  /**
   * The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses
   */
  toolCallId?: string;
}
/**
 * Session event "user_input.completed". User input request completion with the user's response
 */
export interface UserInputCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: UserInputCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "user_input.completed".
   */
  type: "user_input.completed";
}
/**
 * User input request completion with the user's response
 */
export interface UserInputCompletedData {
  /**
   * The user's answer to the input request
   */
  answer?: string;
  /**
   * Request ID of the resolved user input request; clients should dismiss any UI for this request
   */
  requestId: string;
  /**
   * Whether the answer was typed as free-form text rather than selected from choices
   */
  wasFreeform?: boolean;
}
/**
 * Session event "elicitation.requested". Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
 */
export interface ElicitationRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ElicitationRequestedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "elicitation.requested".
   */
  type: "elicitation.requested";
}
/**
 * Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
 */
export interface ElicitationRequestedData {
  /**
   * The source that initiated the request (MCP server name, or absent for agent-initiated)
   */
  elicitationSource?: string;
  /**
   * Message describing what information is needed from the user
   */
  message: string;
  mode?: ElicitationRequestedMode;
  requestedSchema?: ElicitationRequestedSchema;
  /**
   * Unique identifier for this elicitation request; used to respond via session.respondToElicitation()
   */
  requestId: string;
  /**
   * Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs
   */
  toolCallId?: string;
  /**
   * URL to open in the user's browser (url mode only)
   */
  url?: string;
  [k: string]: unknown | undefined;
}
/**
 * JSON Schema describing the form fields to present to the user (form mode only)
 */
export interface ElicitationRequestedSchema {
  /**
   * Form field definitions, keyed by field name
   */
  properties: {
    [k: string]:
      | {
          [k: string]: unknown | undefined;
        }
      | undefined;
  };
  /**
   * List of required field names
   */
  required?: string[];
  /**
   * Schema type indicator (always 'object')
   */
  type: "object";
}
/**
 * Session event "elicitation.completed". Elicitation request completion with the user's response
 */
export interface ElicitationCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ElicitationCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "elicitation.completed".
   */
  type: "elicitation.completed";
}
/**
 * Elicitation request completion with the user's response
 */
export interface ElicitationCompletedData {
  action?: ElicitationCompletedAction;
  /**
   * The submitted form data when action is 'accept'; keys match the requested schema fields
   */
  content?: {
    [k: string]: ElicitationCompletedContent | undefined;
  };
  /**
   * Request ID of the resolved elicitation request; clients should dismiss any UI for this request
   */
  requestId: string;
}
/**
 * Session event "sampling.requested". Sampling request from an MCP server; contains the server name and a requestId for correlation
 */
export interface SamplingRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SamplingRequestedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "sampling.requested".
   */
  type: "sampling.requested";
}
/**
 * Sampling request from an MCP server; contains the server name and a requestId for correlation
 */
export interface SamplingRequestedData {
  /**
   * The JSON-RPC request ID from the MCP protocol
   */
  mcpRequestId: string | number;
  /**
   * Unique identifier for this sampling request; used to respond via session.respondToSampling()
   */
  requestId: string;
  /**
   * Name of the MCP server that initiated the sampling request
   */
  serverName: string;
  [k: string]: unknown | undefined;
}
/**
 * Session event "sampling.completed". Sampling request completion notification signaling UI dismissal
 */
export interface SamplingCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SamplingCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "sampling.completed".
   */
  type: "sampling.completed";
}
/**
 * Sampling request completion notification signaling UI dismissal
 */
export interface SamplingCompletedData {
  /**
   * Request ID of the resolved sampling request; clients should dismiss any UI for this request
   */
  requestId: string;
}
/**
 * Session event "mcp.oauth_required". OAuth authentication request for an MCP server
 */
export interface McpOauthRequiredEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: McpOauthRequiredData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "mcp.oauth_required".
   */
  type: "mcp.oauth_required";
}
/**
 * OAuth authentication request for an MCP server
 */
export interface McpOauthRequiredData {
  /**
   * Unique identifier for this OAuth request; used to respond via session.respondToMcpOAuth()
   */
  requestId: string;
  /**
   * Display name of the MCP server that requires OAuth
   */
  serverName: string;
  /**
   * URL of the MCP server that requires OAuth
   */
  serverUrl: string;
  staticClientConfig?: McpOauthRequiredStaticClientConfig;
}
/**
 * Static OAuth client configuration, if the server specifies one
 */
export interface McpOauthRequiredStaticClientConfig {
  /**
   * OAuth client ID for the server
   */
  clientId: string;
  /**
   * Optional non-default OAuth grant type. When set to 'client_credentials', the OAuth flow runs headlessly using the client_id + keychain-stored secret (no browser, no callback server).
   */
  grantType?: "client_credentials";
  /**
   * Whether this is a public OAuth client
   */
  publicClient?: boolean;
}
/**
 * Session event "mcp.oauth_completed". MCP OAuth request completion notification
 */
export interface McpOauthCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: McpOauthCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "mcp.oauth_completed".
   */
  type: "mcp.oauth_completed";
}
/**
 * MCP OAuth request completion notification
 */
export interface McpOauthCompletedData {
  /**
   * Request ID of the resolved OAuth request
   */
  requestId: string;
}
/**
 * Session event "session.custom_notification". Opaque custom notification data. Consumers may branch on source and name, but payload semantics are source-defined.
 */
export interface CustomNotificationEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CustomNotificationData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.custom_notification".
   */
  type: "session.custom_notification";
}
/**
 * Opaque custom notification data. Consumers may branch on source and name, but payload semantics are source-defined.
 */
export interface CustomNotificationData {
  /**
   * Source-defined custom notification name
   */
  name: string;
  payload: CustomNotificationPayload;
  /**
   * Namespace for the custom notification producer
   */
  source: string;
  subject?: CustomNotificationSubject;
  /**
   * Optional source-defined payload schema version
   */
  version?: number;
}
/**
 * Optional source-defined string identifiers describing the payload subject
 */
export interface CustomNotificationSubject {
  [k: string]: string | undefined;
}
/**
 * Session event "external_tool.requested". External tool invocation request for client-side tool execution
 */
export interface ExternalToolRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ExternalToolRequestedData;
  /**
   * When true, the event is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "external_tool.requested".
   */
  type: "external_tool.requested";
}
/**
 * External tool invocation request for client-side tool execution
 */
export interface ExternalToolRequestedData {
  /**
   * Arguments to pass to the external tool
   */
  arguments?: {
    [k: string]: unknown | undefined;
  };
  /**
   * Unique identifier for this request; used to respond via session.respondToExternalTool()
   */
  requestId: string;
  /**
   * Session ID that this external tool request belongs to
   */
  sessionId: string;
  /**
   * Tool call ID assigned to this external tool invocation
   */
  toolCallId: string;
  /**
   * Name of the external tool to invoke
   */
  toolName: string;
  /**
   * W3C Trace Context traceparent header for the execute_tool span
   */
  traceparent?: string;
  /**
   * W3C Trace Context tracestate header for the execute_tool span
   */
  tracestate?: string;
}
/**
 * Session event "external_tool.completed". External tool completion notification signaling UI dismissal
 */
export interface ExternalToolCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ExternalToolCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral?: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "external_tool.completed".
   */
  type: "external_tool.completed";
}
/**
 * External tool completion notification signaling UI dismissal
 */
export interface ExternalToolCompletedData {
  /**
   * Request ID of the resolved external tool request; clients should dismiss any UI for this request
   */
  requestId: string;
}
/**
 * Session event "command.queued". Queued slash command dispatch request for client execution
 */
export interface CommandQueuedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CommandQueuedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "command.queued".
   */
  type: "command.queued";
}
/**
 * Queued slash command dispatch request for client execution
 */
export interface CommandQueuedData {
  /**
   * The slash command text to be executed (e.g., /help, /clear)
   */
  command: string;
  /**
   * Unique identifier for this request; used to respond via session.respondToQueuedCommand()
   */
  requestId: string;
}
/**
 * Session event "command.execute". Registered command dispatch request routed to the owning client
 */
export interface CommandExecuteEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CommandExecuteData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "command.execute".
   */
  type: "command.execute";
}
/**
 * Registered command dispatch request routed to the owning client
 */
export interface CommandExecuteData {
  /**
   * Raw argument string after the command name
   */
  args: string;
  /**
   * The full command text (e.g., /deploy production)
   */
  command: string;
  /**
   * Command name without leading /
   */
  commandName: string;
  /**
   * Unique identifier; used to respond via session.commands.handlePendingCommand()
   */
  requestId: string;
}
/**
 * Session event "command.completed". Queued command completion notification signaling UI dismissal
 */
export interface CommandCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CommandCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "command.completed".
   */
  type: "command.completed";
}
/**
 * Queued command completion notification signaling UI dismissal
 */
export interface CommandCompletedData {
  /**
   * Request ID of the resolved command request; clients should dismiss any UI for this request
   */
  requestId: string;
}
/**
 * Session event "auto_mode_switch.requested". Auto mode switch request notification requiring user approval
 */
export interface AutoModeSwitchRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AutoModeSwitchRequestedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "auto_mode_switch.requested".
   */
  type: "auto_mode_switch.requested";
}
/**
 * Auto mode switch request notification requiring user approval
 */
export interface AutoModeSwitchRequestedData {
  /**
   * The rate limit error code that triggered this request
   */
  errorCode?: string;
  /**
   * Unique identifier for this request; used to respond via session.respondToAutoModeSwitch()
   */
  requestId: string;
  /**
   * Seconds until the rate limit resets, when known. Lets clients render a humanized reset time alongside the prompt.
   */
  retryAfterSeconds?: number;
}
/**
 * Session event "auto_mode_switch.completed". Auto mode switch completion notification
 */
export interface AutoModeSwitchCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: AutoModeSwitchCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "auto_mode_switch.completed".
   */
  type: "auto_mode_switch.completed";
}
/**
 * Auto mode switch completion notification
 */
export interface AutoModeSwitchCompletedData {
  /**
   * Request ID of the resolved request; clients should dismiss any UI for this request
   */
  requestId: string;
  response: AutoModeSwitchResponse;
}
/**
 * Session event "commands.changed". SDK command registration change notification
 */
export interface CommandsChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CommandsChangedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "commands.changed".
   */
  type: "commands.changed";
}
/**
 * SDK command registration change notification
 */
export interface CommandsChangedData {
  /**
   * Current list of registered SDK commands
   */
  commands: CommandsChangedCommand[];
}
/**
 * Schema for the `CommandsChangedCommand` type.
 */
export interface CommandsChangedCommand {
  /**
   * Optional human-readable command description.
   */
  description?: string;
  /**
   * Slash command name without the leading slash.
   */
  name: string;
}
/**
 * Session event "capabilities.changed". Session capability change notification
 */
export interface CapabilitiesChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CapabilitiesChangedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "capabilities.changed".
   */
  type: "capabilities.changed";
}
/**
 * Session capability change notification
 */
export interface CapabilitiesChangedData {
  ui?: CapabilitiesChangedUI;
}
/**
 * UI capability changes
 */
export interface CapabilitiesChangedUI {
  /**
   * Whether elicitation is now supported
   */
  elicitation?: boolean;
}
/**
 * Session event "exit_plan_mode.requested". Plan approval request with plan content and available user actions
 */
export interface ExitPlanModeRequestedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ExitPlanModeRequestedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "exit_plan_mode.requested".
   */
  type: "exit_plan_mode.requested";
}
/**
 * Plan approval request with plan content and available user actions
 */
export interface ExitPlanModeRequestedData {
  /**
   * Available actions the user can take
   */
  actions: ExitPlanModeAction[];
  /**
   * Full content of the plan file
   */
  planContent: string;
  recommendedAction: ExitPlanModeAction;
  /**
   * Unique identifier for this request; used to respond via session.respondToExitPlanMode()
   */
  requestId: string;
  /**
   * Summary of the plan that was created
   */
  summary: string;
}
/**
 * Session event "exit_plan_mode.completed". Plan mode exit completion with the user's approval decision and optional feedback
 */
export interface ExitPlanModeCompletedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ExitPlanModeCompletedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "exit_plan_mode.completed".
   */
  type: "exit_plan_mode.completed";
}
/**
 * Plan mode exit completion with the user's approval decision and optional feedback
 */
export interface ExitPlanModeCompletedData {
  /**
   * Whether the plan was approved by the user
   */
  approved?: boolean;
  /**
   * Whether edits should be auto-approved without confirmation
   */
  autoApproveEdits?: boolean;
  /**
   * Free-form feedback from the user if they requested changes to the plan
   */
  feedback?: string;
  /**
   * Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request
   */
  requestId: string;
  selectedAction?: ExitPlanModeAction;
}
/**
 * Session event "session.tools_updated".
 */
export interface ToolsUpdatedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ToolsUpdatedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.tools_updated".
   */
  type: "session.tools_updated";
}
/**
 * Schema for the `ToolsUpdatedData` type.
 */
export interface ToolsUpdatedData {
  /**
   * Identifier of the model the resolved tools apply to.
   */
  model: string;
}
/**
 * Session event "session.background_tasks_changed".
 */
export interface BackgroundTasksChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: BackgroundTasksChangedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.background_tasks_changed".
   */
  type: "session.background_tasks_changed";
}
/**
 * Schema for the `BackgroundTasksChangedData` type.
 */
export interface BackgroundTasksChangedData {}
/**
 * Session event "session.skills_loaded".
 */
export interface SkillsLoadedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: SkillsLoadedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.skills_loaded".
   */
  type: "session.skills_loaded";
}
/**
 * Schema for the `SkillsLoadedData` type.
 */
export interface SkillsLoadedData {
  /**
   * Array of resolved skill metadata
   */
  skills: SkillsLoadedSkill[];
}
/**
 * Schema for the `SkillsLoadedSkill` type.
 */
export interface SkillsLoadedSkill {
  /**
   * Description of what the skill does
   */
  description: string;
  /**
   * Whether the skill is currently enabled
   */
  enabled: boolean;
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Absolute path to the skill file, if available
   */
  path?: string;
  source: SkillSource;
  /**
   * Whether the skill can be invoked by the user as a slash command
   */
  userInvocable: boolean;
}
/**
 * Session event "session.custom_agents_updated".
 */
export interface CustomAgentsUpdatedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: CustomAgentsUpdatedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.custom_agents_updated".
   */
  type: "session.custom_agents_updated";
}
/**
 * Schema for the `CustomAgentsUpdatedData` type.
 */
export interface CustomAgentsUpdatedData {
  /**
   * Array of loaded custom agent metadata
   */
  agents: CustomAgentsUpdatedAgent[];
  /**
   * Fatal errors from agent loading
   */
  errors: string[];
  /**
   * Non-fatal warnings from agent loading
   */
  warnings: string[];
}
/**
 * Schema for the `CustomAgentsUpdatedAgent` type.
 */
export interface CustomAgentsUpdatedAgent {
  /**
   * Description of what the agent does
   */
  description: string;
  /**
   * Human-readable display name
   */
  displayName: string;
  /**
   * Unique identifier for the agent
   */
  id: string;
  /**
   * Model override for this agent, if set
   */
  model?: string;
  /**
   * Internal name of the agent
   */
  name: string;
  /**
   * Source location: user, project, inherited, remote, or plugin
   */
  source: string;
  /**
   * List of tool names available to this agent, or null when all tools are available
   */
  tools: string[] | null;
  /**
   * Whether the agent can be selected by the user
   */
  userInvocable: boolean;
}
/**
 * Session event "session.mcp_servers_loaded".
 */
export interface McpServersLoadedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: McpServersLoadedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.mcp_servers_loaded".
   */
  type: "session.mcp_servers_loaded";
}
/**
 * Schema for the `McpServersLoadedData` type.
 */
export interface McpServersLoadedData {
  /**
   * Array of MCP server status summaries
   */
  servers: McpServersLoadedServer[];
}
/**
 * Schema for the `McpServersLoadedServer` type.
 */
export interface McpServersLoadedServer {
  /**
   * Error message if the server failed to connect
   */
  error?: string;
  /**
   * Server name (config key)
   */
  name: string;
  source?: McpServerSource;
  status: McpServerStatus;
}
/**
 * Session event "session.mcp_server_status_changed".
 */
export interface McpServerStatusChangedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: McpServerStatusChangedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.mcp_server_status_changed".
   */
  type: "session.mcp_server_status_changed";
}
/**
 * Schema for the `McpServerStatusChangedData` type.
 */
export interface McpServerStatusChangedData {
  /**
   * Name of the MCP server whose status changed
   */
  serverName: string;
  status: McpServerStatus;
}
/**
 * Session event "session.extensions_loaded".
 */
export interface ExtensionsLoadedEvent {
  /**
   * Sub-agent instance identifier. Absent for events from the root/main agent and session-level events.
   */
  agentId?: string;
  data: ExtensionsLoadedData;
  /**
   * Always true for events that are transient and not persisted to the session event log on disk.
   */
  ephemeral: true;
  /**
   * Unique event identifier (UUID v4), generated when the event is emitted
   */
  id: string;
  /**
   * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
   */
  parentId: string | null;
  /**
   * ISO 8601 timestamp when the event was created
   */
  timestamp: string;
  /**
   * Type discriminator. Always "session.extensions_loaded".
   */
  type: "session.extensions_loaded";
}
/**
 * Schema for the `ExtensionsLoadedData` type.
 */
export interface ExtensionsLoadedData {
  /**
   * Array of discovered extensions and their status
   */
  extensions: ExtensionsLoadedExtension[];
}
/**
 * Schema for the `ExtensionsLoadedExtension` type.
 */
export interface ExtensionsLoadedExtension {
  /**
   * Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper')
   */
  id: string;
  /**
   * Extension name (directory name)
   */
  name: string;
  source: ExtensionsLoadedExtensionSource;
  status: ExtensionsLoadedExtensionStatus;
}
