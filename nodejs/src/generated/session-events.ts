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
           * Repository identifier in "owner/name" format, derived from the git remote URL
           */
          repository?: string;
          /**
           * Current git branch name
           */
          branch?: string;
        };
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
           * Repository identifier in "owner/name" format, derived from the git remote URL
           */
          repository?: string;
          /**
           * Current git branch name
           */
          branch?: string;
        };
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
      data: {
        /**
         * Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
         */
        infoType: string;
        /**
         * Human-readable informational message for display in the timeline
         */
        message: string;
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
      data: {
        /**
         * Category of warning (e.g., "subscription", "policy", "mcp")
         */
        warningType: string;
        /**
         * Human-readable warning message for display in the timeline
         */
        message: string;
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
      data: {
        /**
         * Model that was previously selected, if any
         */
        previousModel?: string;
        /**
         * Newly selected model identifier
         */
        newModel: string;
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
         * Repository identifier in "owner/name" format, derived from the git remote URL
         */
        repository?: string;
        /**
         * Current git branch name
         */
        branch?: string;
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
       * Empty payload; the event signals that LLM-powered conversation compaction has begun
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
      type: "session.compaction_complete";
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
      data: {
        /**
         * Optional summary of the completed task, provided by the agent
         */
        summary?: string;
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
              type: "file";
              /**
               * Absolute file or directory path
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
              type: "directory";
              /**
               * Absolute file or directory path
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
      data: {
        /**
         * Unique identifier for this elicitation request; used to respond via session.respondToElicitation()
         */
        requestId: string;
        /**
         * Message describing what information is needed from the user
         */
        message: string;
        /**
         * Elicitation mode; currently only "form" is supported. Defaults to "form" when absent.
         */
        mode?: "form";
        /**
         * JSON Schema describing the form fields to present to the user
         */
        requestedSchema: {
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
      type: "external_tool.requested";
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
      type: "command.completed";
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
      type: "exit_plan_mode.requested";
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
      data: {
        /**
         * Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request
         */
        requestId: string;
      };
    };
