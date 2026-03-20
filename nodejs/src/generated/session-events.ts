/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: session-events.schema.json
 */

export type SessionEvent =
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.start";
      /**
       * Session initialization metadata including context and configuration
       */
      data: {
        /**
         * Unique identifier for the session
         */
        sessionId: string;
        /**
         * Schema version number for the session event format
         */
        version: number;
        /**
         * Identifier of the software producing the events (e.g., "copilot-agent")
         */
        producer: string;
        /**
         * Version string of the Copilot application
         */
        copilotVersion: string;
        /**
         * ISO 8601 timestamp when the session was created
         */
        startTime: string;
        /**
         * Model selected at session creation time, if any
         */
        selectedModel?: string;
        /**
         * Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
         */
        reasoningEffort?: string;
        /**
         * Working directory and git context at session start
         */
        context?: {
          /**
           * Current working directory path
           */
          cwd: string;
          /**
           * Root directory of the git repository, resolved via git rev-parse
           */
          gitRoot?: string;
          /**
           * Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
           */
          repository?: string;
          /**
           * Hosting platform type of the repository (github or ado)
           */
          hostType?: "github" | "ado";
          /**
           * Current git branch name
           */
          branch?: string;
          /**
           * Head commit of current git branch at session start time
           */
          headCommit?: string;
          /**
           * Base commit of current git branch at session start time
           */
          baseCommit?: string;
        };
        /**
         * Whether the session was already in use by another client at start time
         */
        alreadyInUse?: boolean;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.resume";
      /**
       * Session resume metadata including current context and event count
       */
      data: {
        /**
         * ISO 8601 timestamp when the session was resumed
         */
        resumeTime: string;
        /**
         * Total number of persisted events in the session at the time of resume
         */
        eventCount: number;
        /**
         * Model currently selected at resume time
         */
        selectedModel?: string;
        /**
         * Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
         */
        reasoningEffort?: string;
        /**
         * Updated working directory and git context at resume time
         */
        context?: {
          /**
           * Current working directory path
           */
          cwd: string;
          /**
           * Root directory of the git repository, resolved via git rev-parse
           */
          gitRoot?: string;
          /**
           * Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
           */
          repository?: string;
          /**
           * Hosting platform type of the repository (github or ado)
           */
          hostType?: "github" | "ado";
          /**
           * Current git branch name
           */
          branch?: string;
          /**
           * Head commit of current git branch at session start time
           */
          headCommit?: string;
          /**
           * Base commit of current git branch at session start time
           */
          baseCommit?: string;
        };
        /**
         * Whether the session was already in use by another client at resume time
         */
        alreadyInUse?: boolean;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.error";
      /**
       * Error details for timeline display including message and optional diagnostic information
       */
      data: {
        /**
         * Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "query")
         */
        errorType: string;
        /**
         * Human-readable error message
         */
        message: string;
        /**
         * Error stack trace, when available
         */
        stack?: string;
        /**
         * HTTP status code from the upstream request, if applicable
         */
        statusCode?: number;
        /**
         * GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
         */
        providerCallId?: string;
        /**
         * Optional URL associated with this error that the user can open in a browser
         */
        url?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.idle";
      /**
       * Payload indicating the agent is idle; includes any background tasks still in flight
       */
      data: {
        /**
         * Background tasks still running when the agent became idle
         */
        backgroundTasks?: {
          /**
           * Currently running background agents
           */
          agents: {
            /**
             * Unique identifier of the background agent
             */
            agentId: string;
            /**
             * Type of the background agent
             */
            agentType: string;
            /**
             * Human-readable description of the agent task
             */
            description?: string;
          }[];
          /**
           * Currently running background shell commands
           */
          shells: {
            /**
             * Unique identifier of the background shell
             */
            shellId: string;
            /**
             * Human-readable description of the shell command
             */
            description?: string;
          }[];
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.title_changed";
      /**
       * Session title change payload containing the new display title
       */
      data: {
        /**
         * The new display title for the session
         */
        title: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.info";
      /**
       * Informational message for timeline display with categorization
       */
      data: {
        /**
         * Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
         */
        infoType: string;
        /**
         * Human-readable informational message for display in the timeline
         */
        message: string;
        /**
         * Optional URL associated with this message that the user can open in a browser
         */
        url?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.warning";
      /**
       * Warning message for timeline display with categorization
       */
      data: {
        /**
         * Category of warning (e.g., "subscription", "policy", "mcp")
         */
        warningType: string;
        /**
         * Human-readable warning message for display in the timeline
         */
        message: string;
        /**
         * Optional URL associated with this warning that the user can open in a browser
         */
        url?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.model_change";
      /**
       * Model change details including previous and new model identifiers
       */
      data: {
        /**
         * Model that was previously selected, if any
         */
        previousModel?: string;
        /**
         * Newly selected model identifier
         */
        newModel: string;
        /**
         * Reasoning effort level before the model change, if applicable
         */
        previousReasoningEffort?: string;
        /**
         * Reasoning effort level after the model change, if applicable
         */
        reasoningEffort?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.mode_changed";
      /**
       * Agent mode change details including previous and new modes
       */
      data: {
        /**
         * Agent mode before the change (e.g., "interactive", "plan", "autopilot")
         */
        previousMode: string;
        /**
         * Agent mode after the change (e.g., "interactive", "plan", "autopilot")
         */
        newMode: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.plan_changed";
      /**
       * Plan file operation details indicating what changed
       */
      data: {
        /**
         * The type of operation performed on the plan file
         */
        operation: "create" | "update" | "delete";
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.workspace_file_changed";
      /**
       * Workspace file change details including path and operation type
       */
      data: {
        /**
         * Relative path within the session workspace files directory
         */
        path: string;
        /**
         * Whether the file was newly created or updated
         */
        operation: "create" | "update";
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.handoff";
      /**
       * Session handoff metadata including source, context, and repository information
       */
      data: {
        /**
         * ISO 8601 timestamp when the handoff occurred
         */
        handoffTime: string;
        /**
         * Origin type of the session being handed off
         */
        sourceType: "remote" | "local";
        /**
         * Repository context for the handed-off session
         */
        repository?: {
          /**
           * Repository owner (user or organization)
           */
          owner: string;
          /**
           * Repository name
           */
          name: string;
          /**
           * Git branch name, if applicable
           */
          branch?: string;
        };
        /**
         * Additional context information for the handoff
         */
        context?: string;
        /**
         * Summary of the work done in the source session
         */
        summary?: string;
        /**
         * Session ID of the remote session being handed off
         */
        remoteSessionId?: string;
        /**
         * GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com)
         */
        host?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.truncation";
      /**
       * Conversation truncation statistics including token counts and removed content metrics
       */
      data: {
        /**
         * Maximum token count for the model's context window
         */
        tokenLimit: number;
        /**
         * Total tokens in conversation messages before truncation
         */
        preTruncationTokensInMessages: number;
        /**
         * Number of conversation messages before truncation
         */
        preTruncationMessagesLength: number;
        /**
         * Total tokens in conversation messages after truncation
         */
        postTruncationTokensInMessages: number;
        /**
         * Number of conversation messages after truncation
         */
        postTruncationMessagesLength: number;
        /**
         * Number of tokens removed by truncation
         */
        tokensRemovedDuringTruncation: number;
        /**
         * Number of messages removed by truncation
         */
        messagesRemovedDuringTruncation: number;
        /**
         * Identifier of the component that performed truncation (e.g., "BasicTruncator")
         */
        performedBy: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.snapshot_rewind";
      /**
       * Session rewind details including target event and count of removed events
       */
      data: {
        /**
         * Event ID that was rewound to; all events after this one were removed
         */
        upToEventId: string;
        /**
         * Number of events that were removed by the rewind
         */
        eventsRemoved: number;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.shutdown";
      /**
       * Session termination metrics including usage statistics, code changes, and shutdown reason
       */
      data: {
        /**
         * Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
         */
        shutdownType: "routine" | "error";
        /**
         * Error description when shutdownType is "error"
         */
        errorReason?: string;
        /**
         * Total number of premium API requests used during the session
         */
        totalPremiumRequests: number;
        /**
         * Cumulative time spent in API calls during the session, in milliseconds
         */
        totalApiDurationMs: number;
        /**
         * Unix timestamp (milliseconds) when the session started
         */
        sessionStartTime: number;
        /**
         * Aggregate code change metrics for the session
         */
        codeChanges: {
          /**
           * Total number of lines added during the session
           */
          linesAdded: number;
          /**
           * Total number of lines removed during the session
           */
          linesRemoved: number;
          /**
           * List of file paths that were modified during the session
           */
          filesModified: string[];
        };
        /**
         * Per-model usage breakdown, keyed by model identifier
         */
        modelMetrics: {
          [k: string]: {
            /**
             * Request count and cost metrics
             */
            requests: {
              /**
               * Total number of API requests made to this model
               */
              count: number;
              /**
               * Cumulative cost multiplier for requests to this model
               */
              cost: number;
            };
            /**
             * Token usage breakdown
             */
            usage: {
              /**
               * Total input tokens consumed across all requests to this model
               */
              inputTokens: number;
              /**
               * Total output tokens produced across all requests to this model
               */
              outputTokens: number;
              /**
               * Total tokens read from prompt cache across all requests
               */
              cacheReadTokens: number;
              /**
               * Total tokens written to prompt cache across all requests
               */
              cacheWriteTokens: number;
            };
          };
        };
        /**
         * Model that was selected at the time of shutdown
         */
        currentModel?: string;
        /**
         * Total tokens in context window at shutdown
         */
        currentTokens?: number;
        /**
         * System message token count at shutdown
         */
        systemTokens?: number;
        /**
         * Non-system message token count at shutdown
         */
        conversationTokens?: number;
        /**
         * Tool definitions token count at shutdown
         */
        toolDefinitionsTokens?: number;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.context_changed";
      /**
       * Updated working directory and git context after the change
       */
      data: {
        /**
         * Current working directory path
         */
        cwd: string;
        /**
         * Root directory of the git repository, resolved via git rev-parse
         */
        gitRoot?: string;
        /**
         * Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
         */
        repository?: string;
        /**
         * Hosting platform type of the repository (github or ado)
         */
        hostType?: "github" | "ado";
        /**
         * Current git branch name
         */
        branch?: string;
        /**
         * Head commit of current git branch at session start time
         */
        headCommit?: string;
        /**
         * Base commit of current git branch at session start time
         */
        baseCommit?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.usage_info";
      /**
       * Current context window usage statistics including token and message counts
       */
      data: {
        /**
         * Maximum token count for the model's context window
         */
        tokenLimit: number;
        /**
         * Current number of tokens in the context window
         */
        currentTokens: number;
        /**
         * Current number of messages in the conversation
         */
        messagesLength: number;
        /**
         * Token count from system message(s)
         */
        systemTokens?: number;
        /**
         * Token count from non-system messages (user, assistant, tool)
         */
        conversationTokens?: number;
        /**
         * Token count from tool definitions
         */
        toolDefinitionsTokens?: number;
        /**
         * Whether this is the first usage_info event emitted in this session
         */
        isInitial?: boolean;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.compaction_start";
      /**
       * Context window breakdown at the start of LLM-powered conversation compaction
       */
      data: {
        /**
         * Token count from system message(s) at compaction start
         */
        systemTokens?: number;
        /**
         * Token count from non-system messages (user, assistant, tool) at compaction start
         */
        conversationTokens?: number;
        /**
         * Token count from tool definitions at compaction start
         */
        toolDefinitionsTokens?: number;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.compaction_complete";
      /**
       * Conversation compaction results including success status, metrics, and optional error details
       */
      data: {
        /**
         * Whether compaction completed successfully
         */
        success: boolean;
        /**
         * Error message if compaction failed
         */
        error?: string;
        /**
         * Total tokens in conversation before compaction
         */
        preCompactionTokens?: number;
        /**
         * Total tokens in conversation after compaction
         */
        postCompactionTokens?: number;
        /**
         * Number of messages before compaction
         */
        preCompactionMessagesLength?: number;
        /**
         * Number of messages removed during compaction
         */
        messagesRemoved?: number;
        /**
         * Number of tokens removed during compaction
         */
        tokensRemoved?: number;
        /**
         * LLM-generated summary of the compacted conversation history
         */
        summaryContent?: string;
        /**
         * Checkpoint snapshot number created for recovery
         */
        checkpointNumber?: number;
        /**
         * File path where the checkpoint was stored
         */
        checkpointPath?: string;
        /**
         * Token usage breakdown for the compaction LLM call
         */
        compactionTokensUsed?: {
          /**
           * Input tokens consumed by the compaction LLM call
           */
          input: number;
          /**
           * Output tokens produced by the compaction LLM call
           */
          output: number;
          /**
           * Cached input tokens reused in the compaction LLM call
           */
          cachedInput: number;
        };
        /**
         * GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
         */
        requestId?: string;
        /**
         * Token count from system message(s) after compaction
         */
        systemTokens?: number;
        /**
         * Token count from non-system messages (user, assistant, tool) after compaction
         */
        conversationTokens?: number;
        /**
         * Token count from tool definitions after compaction
         */
        toolDefinitionsTokens?: number;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "session.task_complete";
      /**
       * Task completion notification with summary from the agent
       */
      data: {
        /**
         * Summary of the completed task, provided by the agent
         */
        summary?: string;
        /**
         * Whether the tool call succeeded. False when validation failed (e.g., invalid arguments)
         */
        success?: boolean;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "user.message";
      data: {
        /**
         * The user's message text as displayed in the timeline
         */
        content: string;
        /**
         * Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
         */
        transformedContent?: string;
        /**
         * Files, selections, or GitHub references attached to the message
         */
        attachments?: (
          | {
              /**
               * Attachment type discriminator
               */
              type: "file";
              /**
               * Absolute file path
               */
              path: string;
              /**
               * User-facing display name for the attachment
               */
              displayName: string;
              /**
               * Optional line range to scope the attachment to a specific section of the file
               */
              lineRange?: {
                /**
                 * Start line number (1-based)
                 */
                start: number;
                /**
                 * End line number (1-based, inclusive)
                 */
                end: number;
              };
            }
          | {
              /**
               * Attachment type discriminator
               */
              type: "directory";
              /**
               * Absolute directory path
               */
              path: string;
              /**
               * User-facing display name for the attachment
               */
              displayName: string;
            }
          | {
              /**
               * Attachment type discriminator
               */
              type: "selection";
              /**
               * Absolute path to the file containing the selection
               */
              filePath: string;
              /**
               * User-facing display name for the selection
               */
              displayName: string;
              /**
               * The selected text content
               */
              text: string;
              /**
               * Position range of the selection within the file
               */
              selection: {
                /**
                 * Start position of the selection
                 */
                start: {
                  /**
                   * Start line number (0-based)
                   */
                  line: number;
                  /**
                   * Start character offset within the line (0-based)
                   */
                  character: number;
                };
                /**
                 * End position of the selection
                 */
                end: {
                  /**
                   * End line number (0-based)
                   */
                  line: number;
                  /**
                   * End character offset within the line (0-based)
                   */
                  character: number;
                };
              };
            }
          | {
              /**
               * Attachment type discriminator
               */
              type: "github_reference";
              /**
               * Issue, pull request, or discussion number
               */
              number: number;
              /**
               * Title of the referenced item
               */
              title: string;
              /**
               * Type of GitHub reference
               */
              referenceType: "issue" | "pr" | "discussion";
              /**
               * Current state of the referenced item (e.g., open, closed, merged)
               */
              state: string;
              /**
               * URL to the referenced item on GitHub
               */
              url: string;
            }
          | {
              /**
               * Attachment type discriminator
               */
              type: "blob";
              /**
               * Base64-encoded content
               */
              data: string;
              /**
               * MIME type of the inline data
               */
              mimeType: string;
              /**
               * User-facing display name for the attachment
               */
              displayName?: string;
            }
        )[];
        /**
         * Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user)
         */
        source?: string;
        /**
         * The agent mode that was active when this message was sent
         */
        agentMode?: "interactive" | "plan" | "autopilot" | "shell";
        /**
         * CAPI interaction ID for correlating this user message with its turn
         */
        interactionId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "pending_messages.modified";
      /**
       * Empty payload; the event signals that the pending message queue has changed
       */
      data: {};
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "assistant.turn_start";
      /**
       * Turn initialization metadata including identifier and interaction tracking
       */
      data: {
        /**
         * Identifier for this turn within the agentic loop, typically a stringified turn number
         */
        turnId: string;
        /**
         * CAPI interaction ID for correlating this turn with upstream telemetry
         */
        interactionId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "assistant.intent";
      /**
       * Agent intent description for current activity or plan
       */
      data: {
        /**
         * Short description of what the agent is currently doing or planning to do
         */
        intent: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "assistant.reasoning";
      /**
       * Assistant reasoning content for timeline display with complete thinking text
       */
      data: {
        /**
         * Unique identifier for this reasoning block
         */
        reasoningId: string;
        /**
         * The complete extended thinking text from the model
         */
        content: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "assistant.reasoning_delta";
      /**
       * Streaming reasoning delta for incremental extended thinking updates
       */
      data: {
        /**
         * Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event
         */
        reasoningId: string;
        /**
         * Incremental text chunk to append to the reasoning content
         */
        deltaContent: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "assistant.streaming_delta";
      /**
       * Streaming response progress with cumulative byte count
       */
      data: {
        /**
         * Cumulative total bytes received from the streaming response so far
         */
        totalResponseSizeBytes: number;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "assistant.message";
      /**
       * Assistant response containing text content, optional tool requests, and interaction metadata
       */
      data: {
        /**
         * Unique identifier for this assistant message
         */
        messageId: string;
        /**
         * The assistant's text response content
         */
        content: string;
        /**
         * Tool invocations requested by the assistant in this message
         */
        toolRequests?: {
          /**
           * Unique identifier for this tool call
           */
          toolCallId: string;
          /**
           * Name of the tool being invoked
           */
          name: string;
          /**
           * Arguments to pass to the tool, format depends on the tool
           */
          arguments?: {
            [k: string]: unknown;
          };
          /**
           * Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
           */
          type?: "function" | "custom";
          /**
           * Human-readable display title for the tool
           */
          toolTitle?: string;
          /**
           * Resolved intention summary describing what this specific call does
           */
          intentionSummary?: string | null;
        }[];
        /**
         * Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume.
         */
        reasoningOpaque?: string;
        /**
         * Readable reasoning text from the model's extended thinking
         */
        reasoningText?: string;
        /**
         * Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.
         */
        encryptedContent?: string;
        /**
         * Generation phase for phased-output models (e.g., thinking vs. response phases)
         */
        phase?: string;
        /**
         * Actual output token count from the API response (completion_tokens), used for accurate token accounting
         */
        outputTokens?: number;
        /**
         * CAPI interaction ID for correlating this message with upstream telemetry
         */
        interactionId?: string;
        /**
         * Tool call ID of the parent tool invocation when this event originates from a sub-agent
         */
        parentToolCallId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "assistant.message_delta";
      /**
       * Streaming assistant message delta for incremental response updates
       */
      data: {
        /**
         * Message ID this delta belongs to, matching the corresponding assistant.message event
         */
        messageId: string;
        /**
         * Incremental text chunk to append to the message content
         */
        deltaContent: string;
        /**
         * Tool call ID of the parent tool invocation when this event originates from a sub-agent
         */
        parentToolCallId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "assistant.turn_end";
      /**
       * Turn completion metadata including the turn identifier
       */
      data: {
        /**
         * Identifier of the turn that has ended, matching the corresponding assistant.turn_start event
         */
        turnId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "assistant.usage";
      /**
       * LLM API call usage metrics including tokens, costs, quotas, and billing information
       */
      data: {
        /**
         * Model identifier used for this API call
         */
        model: string;
        /**
         * Number of input tokens consumed
         */
        inputTokens?: number;
        /**
         * Number of output tokens produced
         */
        outputTokens?: number;
        /**
         * Number of tokens read from prompt cache
         */
        cacheReadTokens?: number;
        /**
         * Number of tokens written to prompt cache
         */
        cacheWriteTokens?: number;
        /**
         * Model multiplier cost for billing purposes
         */
        cost?: number;
        /**
         * Duration of the API call in milliseconds
         */
        duration?: number;
        /**
         * What initiated this API call (e.g., "sub-agent"); absent for user-initiated calls
         */
        initiator?: string;
        /**
         * Completion ID from the model provider (e.g., chatcmpl-abc123)
         */
        apiCallId?: string;
        /**
         * GitHub request tracing ID (x-github-request-id header) for server-side log correlation
         */
        providerCallId?: string;
        /**
         * Parent tool call ID when this usage originates from a sub-agent
         */
        parentToolCallId?: string;
        /**
         * Per-quota resource usage snapshots, keyed by quota identifier
         */
        quotaSnapshots?: {
          [k: string]: {
            /**
             * Whether the user has an unlimited usage entitlement
             */
            isUnlimitedEntitlement: boolean;
            /**
             * Total requests allowed by the entitlement
             */
            entitlementRequests: number;
            /**
             * Number of requests already consumed
             */
            usedRequests: number;
            /**
             * Whether usage is still permitted after quota exhaustion
             */
            usageAllowedWithExhaustedQuota: boolean;
            /**
             * Number of requests over the entitlement limit
             */
            overage: number;
            /**
             * Whether overage is allowed when quota is exhausted
             */
            overageAllowedWithExhaustedQuota: boolean;
            /**
             * Percentage of quota remaining (0.0 to 1.0)
             */
            remainingPercentage: number;
            /**
             * Date when the quota resets
             */
            resetDate?: string;
          };
        };
        /**
         * Per-request cost and usage data from the CAPI copilot_usage response field
         */
        copilotUsage?: {
          /**
           * Itemized token usage breakdown
           */
          tokenDetails: {
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
          }[];
          /**
           * Total cost in nano-AIU (AI Units) for this request
           */
          totalNanoAiu: number;
        };
        /**
         * Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
         */
        reasoningEffort?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "abort";
      /**
       * Turn abort information including the reason for termination
       */
      data: {
        /**
         * Reason the current turn was aborted (e.g., "user initiated")
         */
        reason: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "tool.user_requested";
      /**
       * User-initiated tool invocation request with tool name and arguments
       */
      data: {
        /**
         * Unique identifier for this tool call
         */
        toolCallId: string;
        /**
         * Name of the tool the user wants to invoke
         */
        toolName: string;
        /**
         * Arguments for the tool invocation
         */
        arguments?: {
          [k: string]: unknown;
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "tool.execution_start";
      /**
       * Tool execution startup details including MCP server information when applicable
       */
      data: {
        /**
         * Unique identifier for this tool call
         */
        toolCallId: string;
        /**
         * Name of the tool being executed
         */
        toolName: string;
        /**
         * Arguments passed to the tool
         */
        arguments?: {
          [k: string]: unknown;
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
         * Tool call ID of the parent tool invocation when this event originates from a sub-agent
         */
        parentToolCallId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "tool.execution_partial_result";
      /**
       * Streaming tool execution output for incremental result display
       */
      data: {
        /**
         * Tool call ID this partial result belongs to
         */
        toolCallId: string;
        /**
         * Incremental output chunk from the running tool
         */
        partialOutput: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "tool.execution_progress";
      /**
       * Tool execution progress notification with status message
       */
      data: {
        /**
         * Tool call ID this progress notification belongs to
         */
        toolCallId: string;
        /**
         * Human-readable progress status message (e.g., from an MCP server)
         */
        progressMessage: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "tool.execution_complete";
      /**
       * Tool execution completion results including success status, detailed output, and error information
       */
      data: {
        /**
         * Unique identifier for the completed tool call
         */
        toolCallId: string;
        /**
         * Whether the tool execution completed successfully
         */
        success: boolean;
        /**
         * Model identifier that generated this tool call
         */
        model?: string;
        /**
         * CAPI interaction ID for correlating this tool execution with upstream telemetry
         */
        interactionId?: string;
        /**
         * Whether this tool call was explicitly requested by the user rather than the assistant
         */
        isUserRequested?: boolean;
        /**
         * Tool execution result on success
         */
        result?: {
          /**
           * Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency
           */
          content: string;
          /**
           * Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent.
           */
          detailedContent?: string;
          /**
           * Structured content blocks (text, images, audio, resources) returned by the tool in their native format
           */
          contents?: (
            | {
                /**
                 * Content block type discriminator
                 */
                type: "text";
                /**
                 * The text content
                 */
                text: string;
              }
            | {
                /**
                 * Content block type discriminator
                 */
                type: "terminal";
                /**
                 * Terminal/shell output text
                 */
                text: string;
                /**
                 * Process exit code, if the command has completed
                 */
                exitCode?: number;
                /**
                 * Working directory where the command was executed
                 */
                cwd?: string;
              }
            | {
                /**
                 * Content block type discriminator
                 */
                type: "image";
                /**
                 * Base64-encoded image data
                 */
                data: string;
                /**
                 * MIME type of the image (e.g., image/png, image/jpeg)
                 */
                mimeType: string;
              }
            | {
                /**
                 * Content block type discriminator
                 */
                type: "audio";
                /**
                 * Base64-encoded audio data
                 */
                data: string;
                /**
                 * MIME type of the audio (e.g., audio/wav, audio/mpeg)
                 */
                mimeType: string;
              }
            | {
                /**
                 * Icons associated with this resource
                 */
                icons?: {
                  /**
                   * URL or path to the icon image
                   */
                  src: string;
                  /**
                   * MIME type of the icon image
                   */
                  mimeType?: string;
                  /**
                   * Available icon sizes (e.g., ['16x16', '32x32'])
                   */
                  sizes?: string[];
                  /**
                   * Theme variant this icon is intended for
                   */
                  theme?: "light" | "dark";
                }[];
                /**
                 * Resource name identifier
                 */
                name: string;
                /**
                 * Human-readable display title for the resource
                 */
                title?: string;
                /**
                 * URI identifying the resource
                 */
                uri: string;
                /**
                 * Human-readable description of the resource
                 */
                description?: string;
                /**
                 * MIME type of the resource content
                 */
                mimeType?: string;
                /**
                 * Size of the resource in bytes
                 */
                size?: number;
                /**
                 * Content block type discriminator
                 */
                type: "resource_link";
              }
            | {
                /**
                 * Content block type discriminator
                 */
                type: "resource";
                /**
                 * The embedded resource contents, either text or base64-encoded binary
                 */
                resource:
                  | {
                      /**
                       * URI identifying the resource
                       */
                      uri: string;
                      /**
                       * MIME type of the text content
                       */
                      mimeType?: string;
                      /**
                       * Text content of the resource
                       */
                      text: string;
                    }
                  | {
                      /**
                       * URI identifying the resource
                       */
                      uri: string;
                      /**
                       * MIME type of the blob content
                       */
                      mimeType?: string;
                      /**
                       * Base64-encoded binary content of the resource
                       */
                      blob: string;
                    };
              }
          )[];
        };
        /**
         * Error details when the tool execution failed
         */
        error?: {
          /**
           * Human-readable error message
           */
          message: string;
          /**
           * Machine-readable error code
           */
          code?: string;
        };
        /**
         * Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
         */
        toolTelemetry?: {
          [k: string]: unknown;
        };
        /**
         * Tool call ID of the parent tool invocation when this event originates from a sub-agent
         */
        parentToolCallId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "skill.invoked";
      /**
       * Skill invocation details including content, allowed tools, and plugin metadata
       */
      data: {
        /**
         * Name of the invoked skill
         */
        name: string;
        /**
         * File path to the SKILL.md definition
         */
        path: string;
        /**
         * Full content of the skill file, injected into the conversation for the model
         */
        content: string;
        /**
         * Tool names that should be auto-approved when this skill is active
         */
        allowedTools?: string[];
        /**
         * Name of the plugin this skill originated from, when applicable
         */
        pluginName?: string;
        /**
         * Version of the plugin this skill originated from, when applicable
         */
        pluginVersion?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "subagent.started";
      /**
       * Sub-agent startup details including parent tool call and agent information
       */
      data: {
        /**
         * Tool call ID of the parent tool invocation that spawned this sub-agent
         */
        toolCallId: string;
        /**
         * Internal name of the sub-agent
         */
        agentName: string;
        /**
         * Human-readable display name of the sub-agent
         */
        agentDisplayName: string;
        /**
         * Description of what the sub-agent does
         */
        agentDescription: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "subagent.completed";
      /**
       * Sub-agent completion details for successful execution
       */
      data: {
        /**
         * Tool call ID of the parent tool invocation that spawned this sub-agent
         */
        toolCallId: string;
        /**
         * Internal name of the sub-agent
         */
        agentName: string;
        /**
         * Human-readable display name of the sub-agent
         */
        agentDisplayName: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "subagent.failed";
      /**
       * Sub-agent failure details including error message and agent information
       */
      data: {
        /**
         * Tool call ID of the parent tool invocation that spawned this sub-agent
         */
        toolCallId: string;
        /**
         * Internal name of the sub-agent
         */
        agentName: string;
        /**
         * Human-readable display name of the sub-agent
         */
        agentDisplayName: string;
        /**
         * Error message describing why the sub-agent failed
         */
        error: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "subagent.selected";
      /**
       * Custom agent selection details including name and available tools
       */
      data: {
        /**
         * Internal name of the selected custom agent
         */
        agentName: string;
        /**
         * Human-readable display name of the selected custom agent
         */
        agentDisplayName: string;
        /**
         * List of tool names available to this agent, or null for all tools
         */
        tools: string[] | null;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "subagent.deselected";
      /**
       * Empty payload; the event signals that the custom agent was deselected, returning to the default agent
       */
      data: {};
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "hook.start";
      /**
       * Hook invocation start details including type and input data
       */
      data: {
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
          [k: string]: unknown;
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "hook.end";
      /**
       * Hook invocation completion details including output, success status, and error information
       */
      data: {
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
          [k: string]: unknown;
        };
        /**
         * Whether the hook completed successfully
         */
        success: boolean;
        /**
         * Error details when the hook failed
         */
        error?: {
          /**
           * Human-readable error message
           */
          message: string;
          /**
           * Error stack trace, when available
           */
          stack?: string;
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "system.message";
      /**
       * System or developer message content with role and optional template metadata
       */
      data: {
        /**
         * The system or developer prompt text
         */
        content: string;
        /**
         * Message role: "system" for system prompts, "developer" for developer-injected instructions
         */
        role: "system" | "developer";
        /**
         * Optional name identifier for the message source
         */
        name?: string;
        /**
         * Metadata about the prompt template and its construction
         */
        metadata?: {
          /**
           * Version identifier of the prompt template used
           */
          promptVersion?: string;
          /**
           * Template variables used when constructing the prompt
           */
          variables?: {
            [k: string]: unknown;
          };
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      /**
       * When true, the event is transient and not persisted to the session event log on disk
       */
      ephemeral?: boolean;
      type: "system.notification";
      /**
       * System-generated notification for runtime events like background task completion
       */
      data: {
        /**
         * The notification text, typically wrapped in <system_notification> XML tags
         */
        content: string;
        /**
         * Structured metadata identifying what triggered this notification
         */
        kind:
          | {
              type: "agent_completed";
              /**
               * Unique identifier of the background agent
               */
              agentId: string;
              /**
               * Type of the agent (e.g., explore, task, general-purpose)
               */
              agentType: string;
              /**
               * Whether the agent completed successfully or failed
               */
              status: "completed" | "failed";
              /**
               * Human-readable description of the agent task
               */
              description?: string;
              /**
               * The full prompt given to the background agent
               */
              prompt?: string;
            }
          | {
              type: "agent_idle";
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
            }
          | {
              type: "shell_completed";
              /**
               * Unique identifier of the shell session
               */
              shellId: string;
              /**
               * Exit code of the shell command, if available
               */
              exitCode?: number;
              /**
               * Human-readable description of the command
               */
              description?: string;
            }
          | {
              type: "shell_detached_completed";
              /**
               * Unique identifier of the detached shell session
               */
              shellId: string;
              /**
               * Human-readable description of the command
               */
              description?: string;
            };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "permission.requested";
      /**
       * Permission request notification requiring client approval with request details
       */
      data: {
        /**
         * Unique identifier for this permission request; used to respond via session.respondToPermission()
         */
        requestId: string;
        /**
         * Details of the permission being requested
         */
        permissionRequest:
          | {
              /**
               * Permission kind discriminator
               */
              kind: "shell";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * The complete shell command text to be executed
               */
              fullCommandText: string;
              /**
               * Human-readable description of what the command intends to do
               */
              intention: string;
              /**
               * Parsed command identifiers found in the command text
               */
              commands: {
                /**
                 * Command identifier (e.g., executable name)
                 */
                identifier: string;
                /**
                 * Whether this command is read-only (no side effects)
                 */
                readOnly: boolean;
              }[];
              /**
               * File paths that may be read or written by the command
               */
              possiblePaths: string[];
              /**
               * URLs that may be accessed by the command
               */
              possibleUrls: {
                /**
                 * URL that may be accessed by the command
                 */
                url: string;
              }[];
              /**
               * Whether the command includes a file write redirection (e.g., > or >>)
               */
              hasWriteFileRedirection: boolean;
              /**
               * Whether the UI can offer session-wide approval for this command pattern
               */
              canOfferSessionApproval: boolean;
              /**
               * Optional warning message about risks of running this command
               */
              warning?: string;
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "write";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Human-readable description of the intended file change
               */
              intention: string;
              /**
               * Path of the file being written to
               */
              fileName: string;
              /**
               * Unified diff showing the proposed changes
               */
              diff: string;
              /**
               * Complete new file contents for newly created files
               */
              newFileContents?: string;
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "read";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Human-readable description of why the file is being read
               */
              intention: string;
              /**
               * Path of the file or directory being read
               */
              path: string;
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "mcp";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Name of the MCP server providing the tool
               */
              serverName: string;
              /**
               * Internal name of the MCP tool
               */
              toolName: string;
              /**
               * Human-readable title of the MCP tool
               */
              toolTitle: string;
              /**
               * Arguments to pass to the MCP tool
               */
              args?: {
                [k: string]: unknown;
              };
              /**
               * Whether this MCP tool is read-only (no side effects)
               */
              readOnly: boolean;
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "url";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Human-readable description of why the URL is being accessed
               */
              intention: string;
              /**
               * URL to be fetched
               */
              url: string;
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "memory";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Topic or subject of the memory being stored
               */
              subject: string;
              /**
               * The fact or convention being stored
               */
              fact: string;
              /**
               * Source references for the stored fact
               */
              citations: string;
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "custom-tool";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Name of the custom tool
               */
              toolName: string;
              /**
               * Description of what the custom tool does
               */
              toolDescription: string;
              /**
               * Arguments to pass to the custom tool
               */
              args?: {
                [k: string]: unknown;
              };
            }
          | {
              /**
               * Permission kind discriminator
               */
              kind: "hook";
              /**
               * Tool call ID that triggered this permission request
               */
              toolCallId?: string;
              /**
               * Name of the tool the hook is gating
               */
              toolName: string;
              /**
               * Arguments of the tool call being gated
               */
              toolArgs?: {
                [k: string]: unknown;
              };
              /**
               * Optional message from the hook explaining why confirmation is needed
               */
              hookMessage?: string;
            };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "permission.completed";
      /**
       * Permission request completion notification signaling UI dismissal
       */
      data: {
        /**
         * Request ID of the resolved permission request; clients should dismiss any UI for this request
         */
        requestId: string;
        /**
         * The result of the permission request
         */
        result: {
          /**
           * The outcome of the permission request
           */
          kind:
            | "approved"
            | "denied-by-rules"
            | "denied-no-approval-rule-and-could-not-request-from-user"
            | "denied-interactively-by-user"
            | "denied-by-content-exclusion-policy";
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "user_input.requested";
      /**
       * User input request notification with question and optional predefined choices
       */
      data: {
        /**
         * Unique identifier for this input request; used to respond via session.respondToUserInput()
         */
        requestId: string;
        /**
         * The question or prompt to present to the user
         */
        question: string;
        /**
         * Predefined choices for the user to select from, if applicable
         */
        choices?: string[];
        /**
         * Whether the user can provide a free-form text response in addition to predefined choices
         */
        allowFreeform?: boolean;
        /**
         * The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses
         */
        toolCallId?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "user_input.completed";
      /**
       * User input request completion notification signaling UI dismissal
       */
      data: {
        /**
         * Request ID of the resolved user input request; clients should dismiss any UI for this request
         */
        requestId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "elicitation.requested";
      /**
       * Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
       */
      data: {
        /**
         * Unique identifier for this elicitation request; used to respond via session.respondToElicitation()
         */
        requestId: string;
        /**
         * Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs
         */
        toolCallId?: string;
        /**
         * The source that initiated the request (MCP server name, or absent for agent-initiated)
         */
        elicitationSource?: string;
        /**
         * Message describing what information is needed from the user
         */
        message: string;
        /**
         * Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
         */
        mode?: "form" | "url";
        /**
         * JSON Schema describing the form fields to present to the user (form mode only)
         */
        requestedSchema?: {
          /**
           * Schema type indicator (always 'object')
           */
          type: "object";
          /**
           * Form field definitions, keyed by field name
           */
          properties: {
            [k: string]: unknown;
          };
          /**
           * List of required field names
           */
          required?: string[];
        };
        /**
         * URL to open in the user's browser (url mode only)
         */
        url?: string;
        [k: string]: unknown;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "elicitation.completed";
      /**
       * Elicitation request completion notification signaling UI dismissal
       */
      data: {
        /**
         * Request ID of the resolved elicitation request; clients should dismiss any UI for this request
         */
        requestId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "mcp.oauth_required";
      /**
       * OAuth authentication request for an MCP server
       */
      data: {
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
        /**
         * Static OAuth client configuration, if the server specifies one
         */
        staticClientConfig?: {
          /**
           * OAuth client ID for the server
           */
          clientId: string;
          /**
           * Whether this is a public OAuth client
           */
          publicClient?: boolean;
        };
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "mcp.oauth_completed";
      /**
       * MCP OAuth request completion notification
       */
      data: {
        /**
         * Request ID of the resolved OAuth request
         */
        requestId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "external_tool.requested";
      /**
       * External tool invocation request for client-side tool execution
       */
      data: {
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
         * Arguments to pass to the external tool
         */
        arguments?: {
          [k: string]: unknown;
        };
        /**
         * W3C Trace Context traceparent header for the execute_tool span
         */
        traceparent?: string;
        /**
         * W3C Trace Context tracestate header for the execute_tool span
         */
        tracestate?: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "external_tool.completed";
      /**
       * External tool completion notification signaling UI dismissal
       */
      data: {
        /**
         * Request ID of the resolved external tool request; clients should dismiss any UI for this request
         */
        requestId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "command.queued";
      /**
       * Queued slash command dispatch request for client execution
       */
      data: {
        /**
         * Unique identifier for this request; used to respond via session.respondToQueuedCommand()
         */
        requestId: string;
        /**
         * The slash command text to be executed (e.g., /help, /clear)
         */
        command: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "command.execute";
      /**
       * Registered command dispatch request routed to the owning client
       */
      data: {
        /**
         * Unique identifier; used to respond via session.commands.handlePendingCommand()
         */
        requestId: string;
        /**
         * The full command text (e.g., /deploy production)
         */
        command: string;
        /**
         * Command name without leading /
         */
        commandName: string;
        /**
         * Raw argument string after the command name
         */
        args: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "command.completed";
      /**
       * Queued command completion notification signaling UI dismissal
       */
      data: {
        /**
         * Request ID of the resolved command request; clients should dismiss any UI for this request
         */
        requestId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "commands.changed";
      /**
       * SDK command registration change notification
       */
      data: {
        /**
         * Current list of registered SDK commands
         */
        commands: {
          name: string;
          description?: string;
        }[];
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "exit_plan_mode.requested";
      /**
       * Plan approval request with plan content and available user actions
       */
      data: {
        /**
         * Unique identifier for this request; used to respond via session.respondToExitPlanMode()
         */
        requestId: string;
        /**
         * Summary of the plan that was created
         */
        summary: string;
        /**
         * Full content of the plan file
         */
        planContent: string;
        /**
         * Available actions the user can take (e.g., approve, edit, reject)
         */
        actions: string[];
        /**
         * The recommended action for the user to take
         */
        recommendedAction: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "exit_plan_mode.completed";
      /**
       * Plan mode exit completion notification signaling UI dismissal
       */
      data: {
        /**
         * Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request
         */
        requestId: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.tools_updated";
      data: {
        model: string;
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.background_tasks_changed";
      data: {};
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.skills_loaded";
      data: {
        /**
         * Array of resolved skill metadata
         */
        skills: {
          /**
           * Unique identifier for the skill
           */
          name: string;
          /**
           * Description of what the skill does
           */
          description: string;
          /**
           * Source location type of the skill (e.g., project, personal, plugin)
           */
          source: string;
          /**
           * Whether the skill can be invoked by the user as a slash command
           */
          userInvocable: boolean;
          /**
           * Whether the skill is currently enabled
           */
          enabled: boolean;
          /**
           * Absolute path to the skill file, if available
           */
          path?: string;
        }[];
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.mcp_servers_loaded";
      data: {
        /**
         * Array of MCP server status summaries
         */
        servers: {
          /**
           * Server name (config key)
           */
          name: string;
          /**
           * Connection status: connected, failed, pending, disabled, or not_configured
           */
          status: "connected" | "failed" | "pending" | "disabled" | "not_configured";
          /**
           * Configuration source: user, workspace, plugin, or builtin
           */
          source?: string;
          /**
           * Error message if the server failed to connect
           */
          error?: string;
        }[];
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.mcp_server_status_changed";
      data: {
        /**
         * Name of the MCP server whose status changed
         */
        serverName: string;
        /**
         * New connection status: connected, failed, pending, disabled, or not_configured
         */
        status: "connected" | "failed" | "pending" | "disabled" | "not_configured";
      };
    }
  | {
      /**
       * Unique event identifier (UUID v4), generated when the event is emitted
       */
      id: string;
      /**
       * ISO 8601 timestamp when the event was created
       */
      timestamp: string;
      /**
       * ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.
       */
      parentId: string | null;
      ephemeral: true;
      type: "session.extensions_loaded";
      data: {
        /**
         * Array of discovered extensions and their status
         */
        extensions: {
          /**
           * Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper')
           */
          id: string;
          /**
           * Extension name (directory name)
           */
          name: string;
          /**
           * Discovery source
           */
          source: "project" | "user";
          /**
           * Current status: running, disabled, failed, or starting
           */
          status: "running" | "disabled" | "failed" | "starting";
        }[];
      };
    };
