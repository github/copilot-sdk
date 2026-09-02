/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";

import type { AbortReason, AgentModelPolicy, Attachment, AutoTier, ContextTier, EmbeddedBlobResourceContents, EmbeddedTextResourceContents, McpOauthHttpResponse, McpOauthWWWAuthenticateParams, McpServerSource, McpServerStatus, ModelChangeSource, PermissionMode, PermissionPromptRequest, PermissionRule, ReasoningSummary, SessionEvent, SessionLimitsConfig, SessionMode, ShutdownType, SkillSource, TaskCompleteData, TaskCompletionOutcome, UserToolSessionApproval, Verbosity } from "./session-events.js";

/** A value that can be represented losslessly on the SDK JSON wire. */
export type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

/**
 * A value that lives only in this process and never crosses the JSON-RPC
 * boundary, such as a callback or a host object handle.
 * @internal
 */
export type OpaqueInProcessValue = unknown;

/**
 * Authentication credentials accepted only at native protocol ingress. Runtime outputs use credential-free `AuthIdentity` metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthInfo".
 */
/** @experimental */
export type AuthInfo =
  | HMACAuthInfo
  | EnvAuthInfo
  | TokenAuthInfo
  | TokenProviderAuthInfo
  | CopilotApiTokenAuthInfo
  | UserAuthInfo
  | GhCliAuthInfo
  | ApiKeyAuthInfo;
/**
 * User to log out
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountLogoutRequest".
 */
/** @experimental */
export type AccountLogoutRequest = {
  [k: string]: unknown | undefined;
};
/**
 * Resolved Anthropic adaptive-thinking capability for a model.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AdaptiveThinkingSupport".
 */
/** @experimental */
export type AdaptiveThinkingSupport =
  /** The model does not accept thinking.type='adaptive' */
  | "unsupported"
  /** The model accepts adaptive thinking but also accepts thinking.type='enabled' */
  | "optional"
  /** The model only accepts adaptive thinking and rejects thinking.type='enabled' with HTTP 400 (e.g. opus-4.7/4.8) */
  | "required";
/**
 * Which tier this directory belongs to
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentDiscoveryPathScope".
 */
/** @experimental */
export type AgentDiscoveryPathScope =
  /** The user's personal agent configuration directory. */
  | "user"
  /** A project's repository agent directory. */
  | "project";
/**
 * Where the agent definition was loaded from
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentInfoSource".
 */
/** @experimental */
export type AgentInfoSource =
  /** Agent loaded from the user's personal agent configuration. */
  | "user"
  /** Agent loaded from the current project's repository configuration. */
  | "project"
  /** Agent inherited from a parent project or workspace. */
  | "inherited"
  /** Agent provided by a remote runtime or service. */
  | "remote"
  /** Agent contributed by an installed plugin. */
  | "plugin"
  /** Agent built into the Copilot runtime. */
  | "builtin";
/**
 * Controls whether built-in agents and authored prompt text are included.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentListRequest".
 */
/** @experimental */
export type AgentListRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * When true, request the session's configured built-in agents alongside custom agents. Listing applies feature, context, inclusion, exclusion, and user-disabled-agent policy, but does not evaluate transient invocation requirements such as model availability. Built-in metadata may be omitted when the session cannot project it, such as a relay session.
       */
      includeBuiltInAgents?: boolean;
      /**
       * When true, request authored base prompt text on each AgentInfo. Prompt text may be omitted when unavailable, such as for agents projected through a relay session.
       */
      includePrompt?: boolean;
    };
/**
 * Process kind tag for the registry entry
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLiveTargetEntryKind".
 */
/** @experimental */
export type AgentRegistryLiveTargetEntryKind =
  /** Interactive Copilot CLI exposing a UI server (legacy/normal CLI process) */
  | "ui-server"
  /** Headless `--server --managed-server` child spawned by a controller */
  | "managed-server";
/**
 * Coarse lifecycle status of the foreground session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLiveTargetEntryStatus".
 */
/** @experimental */
export type AgentRegistryLiveTargetEntryStatus =
  /** Session is actively processing a turn */
  | "working"
  /** Session is idle, waiting for input */
  | "waiting"
  /** Last turn completed successfully */
  | "done"
  /** Session needs user attention (see attentionKind for the specific reason) */
  | "attention";
/**
 * Kind of attention required when status === "attention". Meaningful only when status === "attention".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLiveTargetEntryAttentionKind".
 */
/** @experimental */
export type AgentRegistryLiveTargetEntryAttentionKind =
  /** Session is blocked on an unrecoverable error */
  | "error"
  /** Session is waiting for a tool-permission decision */
  | "permission"
  /** Session is waiting for the user to approve or reject a plan */
  | "exit_plan"
  /** Session is waiting on an elicitation prompt */
  | "elicitation"
  /** Session is waiting for free-form user input */
  | "user_input";
/**
 * How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLiveTargetEntryLastTerminalEvent".
 */
/** @experimental */
export type AgentRegistryLiveTargetEntryLastTerminalEvent =
  /** Last turn ended cleanly (model returned a final assistant message) */
  | "turn_end"
  /** Last turn was aborted (e.g. user interrupted) */
  | "abort";
/**
 * Categorized reason for log-open failure
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLogCaptureOpenErrorReason".
 */
/** @experimental */
export type AgentRegistryLogCaptureOpenErrorReason =
  /** Filesystem permission denied opening the log file */
  | "permission"
  /** No space left on device */
  | "disk_full"
  /** Other / uncategorized open failure */
  | "other";
/**
 * Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnPermissionMode".
 */
/** @experimental */
export type AgentRegistrySpawnPermissionMode =
  /** Standard permission posture (prompts for each request) */
  | "default"
  /** Full allow-all (requires the controller-local session to currently be in allow-all mode) */
  | "yolo";
/**
 * Outcome of an agentRegistry.spawn call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnResult".
 */
/** @experimental */
export type AgentRegistrySpawnResult =
  | AgentRegistrySpawnSpawned
  | AgentRegistrySpawnError
  | AgentRegistrySpawnRegistryTimeout
  | AgentRegistrySpawnValidationError;
/**
 * Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnValidationErrorReason".
 */
/** @experimental */
export type AgentRegistrySpawnValidationErrorReason =
  /** Provided cwd does not exist on disk */
  | "cwd-not-found"
  /** Provided cwd exists but is not a directory */
  | "cwd-not-directory"
  /** Session name failed validateSessionName */
  | "invalid-name"
  /** Requested agent name was not found in builtin or custom agents */
  | "unknown-agent"
  /** Requested model is not available to this session */
  | "unknown-model"
  /** Caller asked for permissionMode='yolo' but the controller is not currently in allow-all mode */
  | "yolo-not-allowed";
/**
 * Which parameter field was invalid. Omitted when the rejection is not field-specific.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnValidationErrorField".
 */
/** @experimental */
export type AgentRegistrySpawnValidationErrorField =
  /** The cwd parameter */
  | "cwd"
  /** The session name parameter */
  | "name"
  /** The agentName parameter */
  | "agentName"
  /** The model parameter */
  | "model"
  /** The permissionMode parameter */
  | "permissionMode";
/**
 * Authentication type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthInfoType".
 */
/** @experimental */
export type AuthInfoType =
  /** Authentication provided by a GitHub App HMAC credential. */
  | "hmac"
  /** Authentication resolved from environment-provided credentials. */
  | "env"
  /** Authentication from an interactive user sign-in. */
  | "user"
  /** Authentication delegated to the GitHub CLI. */
  | "gh-cli"
  /** Authentication from an API key credential. */
  | "api-key"
  /** Authentication from a GitHub token. */
  | "token"
  /** Authentication from an SDK GitHub token callback. */
  | "token-provider"
  /** Authentication from a Copilot API token. */
  | "copilot-api-token";
/**
 * Validation errors from the most recent authentication attempt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthValidationErrors".
 */
/** @experimental */
export type AuthValidationErrors = AuthValidationError[];
/**
 * Root JSON Schema type for a built-in tool input.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolInputSchemaType".
 */
/** @experimental */
export type BuiltinToolInputSchemaType = /** The tool accepts a JSON object. */ "object";
/**
 * Custom input-format kind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolFormatType".
 */
/** @experimental */
export type BuiltinToolFormatType = /** The tool input is parsed with the supplied grammar. */ "grammar";
/**
 * Telemetry-safety policy for a built-in tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolSafeForTelemetry".
 */
/** @experimental */
export type BuiltinToolSafeForTelemetry = boolean | BuiltinToolSafeTelemetryFields;
/**
 * JSON Schema for canvas open input
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasJsonSchema".
 */
/** @experimental */
export type CanvasJsonSchema = JsonValue;
/**
 * Provider-supplied action result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasActionInvokeResult".
 */
/** @experimental */
export type CanvasActionInvokeResult = JsonValue;
/**
 * Canonical digest algorithm for a validated MCP card
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CardDigestAlgorithm".
 */
/** @experimental */
export type CardDigestAlgorithm = /** SHA-256 over RFC 8785 canonical JSON encoded as UTF-8. */ "sha256-rfc8785";
/**
 * SHA-256 digest encoded as exactly 64 lowercase hexadecimal characters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CardDigestValue".
 */
/** @experimental */
export type CardDigestValue = string;
/**
 * Where a candidate's card came from. Exactly one of a URL or embedded data: the union has no variant carrying both, and no variant carrying neither, so the rule holds structurally rather than by validation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCandidateSource".
 */
/** @experimental */
export type CatalogCandidateSource = CatalogCandidateSourceUrl | CatalogCandidateSourceEmbedded;
/**
 * Why the catalog authority did not accept the caller's identity
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogAuthenticationRequiredReason".
 */
/** @experimental */
export type CatalogAuthenticationRequiredReason =
  /** No credential was presented, so there is nothing to refresh and the caller must sign in. */
  | "no-credential"
  /** A credential was presented and its lifetime has elapsed. A silent refresh is worth attempting before prompting anyone. */
  | "credential-expired"
  /** A credential was presented and the authority refused it, for example because it was revoked, malformed, or issued for another audience. Refreshing the same rejected credential is not useful; the caller must sign in again. */
  | "credential-rejected";
/**
 * One inert catalog result, represented as an MCP server or discovery-only AI skill variant so kind, media type, provenance, and installability cannot contradict each other.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCandidate".
 */
/** @experimental */
export type CatalogCandidate = CatalogMcpServerCandidate | CatalogAiSkillCandidate;
/**
 * JSON MCP card media type accepted for install planning
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerCardMediaType".
 */
/** @experimental */
export type McpServerCardMediaType =
  /** The current MCP server card media type. */
  | "application/mcp-server-card+json"
  /** The legacy MCP server card media type, accepted for compatibility. */
  | "application/mcp-server+json";
/**
 * Whether an MCP server candidate can be planned for installation
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogMcpServerInstallability".
 */
/** @experimental */
export type CatalogMcpServerInstallability =
  /** An install plan can be computed for this MCP server candidate. */
  | "installable"
  /** Policy forbids installing this MCP server candidate. */
  | "not-installable-policy";
/**
 * What kind of resource a catalog candidate describes
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCandidateKind".
 */
/** @experimental */
export type CatalogCandidateKind =
  /** An MCP server, which can be planned for installation. */
  | "mcp-server"
  /** An AI skill, which is discoverable but not installable through this surface. */
  | "ai-skill";
/**
 * A wire feature a caller can require of the catalog surface, negotiated per request. A grant means the runtime understands the feature's contract, not that the deployment has enabled the operation; typed unavailable results report availability separately.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCapability".
 */
/** @experimental */
export type CatalogCapability =
  /** Understands the current `application/mcp-server-card+json` media type. */
  | "mcp-server-card"
  /** Understands the legacy `application/mcp-server+json` media type. */
  | "legacy-mcp-server-card"
  /** Understands `application/ai-skill` candidates as discovery-only and typed non-installable. */
  | "ai-skill-discovery"
  /** Understands side-effect-free MCP install-plan requests, results, and plan handles; `planning-unavailable` separately reports that planning is not enabled. */
  | "mcp-install-planning"
  /** Understands plans that enumerate every eligible transport rather than a single preferred one. */
  | "multiple-transport-choice";
/**
 * Bounded extensible wire-feature identifier. Known values are described by `CatalogCapability`; newer callers may send future identifiers so an older runtime can return a typed negotiation refusal instead of failing schema validation. Capability negotiation establishes contract understanding, while each operation's result separately reports runtime availability.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCapabilityId".
 */
/** @experimental */
export type CatalogCapabilityId = string;
/**
 * Which wire-contract rule an upstream response broke
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogContractViolationReason".
 */
/** @experimental */
export type CatalogContractViolationReason =
  /** A result carried both a URL and embedded data, when exactly one is permitted. */
  | "both-url-and-data"
  /** A result carried neither a URL nor embedded data, when exactly one is required. */
  | "neither-url-nor-data"
  /** Two results claimed the same normalised identity. */
  | "duplicate-identity"
  /** A result declared no media type, or one this contract does not model. */
  | "unknown-media-type";
/**
 * Which kind of opaque handle was presented
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogHandleType".
 */
/** @experimental */
export type CatalogHandleType =
  /** A search candidate handle. */
  | "candidate"
  /** An install plan handle. */
  | "plan";
/**
 * Why a presented handle was rejected
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogHandleRejectionReason".
 */
/** @experimental */
export type CatalogHandleRejectionReason =
  /** The handle is unparseable, unknown, or was issued for a different operation. */
  | "invalid"
  /** The handle's time to live has elapsed. */
  | "stale"
  /** The handle has already been used, and handles are single-use. */
  | "replayed"
  /** The handle was issued by a different runtime instance. */
  | "foreign";
/**
 * Which request field was rejected before any work was done
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogInvalidRequestField".
 */
/** @experimental */
export type CatalogInvalidRequestField =
  /** The search query was empty or longer than permitted. */
  | "query"
  /** The requested result count fell outside its permitted range. */
  | "limit"
  /** The requested candidate kinds were empty or contained a duplicate. */
  | "kinds"
  /** The negotiation block was missing or malformed. */
  | "contract"
  /** The plan source was missing or malformed. */
  | "source"
  /** The supplied card was missing its media type, URL, or data. */
  | "card"
  /** The requested configuration scope is not one this runtime writes. */
  | "scope";
/**
 * How a card failed validation
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogMalformedCardReason".
 */
/** @experimental */
export type CatalogMalformedCardReason =
  /** The document is not well-formed JSON. */
  | "invalid-json"
  /** The document does not satisfy its media type's schema. */
  | "schema-violation"
  /** The declared media type is not one this runtime understands. */
  | "unsupported-media-type"
  /** A field the media type requires is absent. */
  | "missing-required-field"
  /** The document exceeded the permitted size. */
  | "size-limit-exceeded";
/**
 * Media type a catalog card is interpreted as
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogMediaType".
 */
/** @experimental */
export type CatalogMediaType =
  /** The current MCP server card media type. */
  | "application/mcp-server-card+json"
  /** The legacy MCP server card media type, accepted for compatibility. */
  | "application/mcp-server+json"
  /** An AI skill card. Representable and searchable, but typed non-installable. */
  | "application/ai-skill";
/**
 * Why capability and protocol-version negotiation refused a caller
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNegotiationRefusedReason".
 */
/** @experimental */
export type CatalogNegotiationRefusedReason =
  /** The caller's protocol version is below the lowest this runtime serves. */
  | "unsupported-protocol-version"
  /** The caller requires at least one capability this runtime cannot honour. */
  | "unsupported-capability";
/**
 * Categorised network failure, low cardinality so it can be aggregated without carrying a URL
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNetworkFailureReason".
 */
/** @experimental */
export type CatalogNetworkFailureReason =
  /** No network is available, so nothing was attempted. */
  | "offline"
  /** The authority's name could not be resolved. */
  | "dns"
  /** The request exceeded its time budget. */
  | "timeout"
  /** The TLS handshake or certificate validation failed. */
  | "tls"
  /** The connection was refused or reset. */
  | "connection-refused"
  /** The configured proxy returned 407 and requires authentication. */
  | "proxy-authentication-required"
  /** The authority rate-limited requests and supplied or implied a bounded cooldown. */
  | "rate-limited"
  /** The authority returned a transient 5xx response. */
  | "service-unavailable"
  /** The authority returned another status the runtime treats as a failure. */
  | "http-status"
  /** The response exceeded the permitted size. */
  | "response-too-large"
  /** A redirect was refused by the runtime's redirect policy. */
  | "redirect-rejected";
/**
 * Why a discoverable candidate cannot be installed
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNotInstallableReason".
 */
/** @experimental */
export type CatalogNotInstallableReason =
  /** This kind of resource is not installable through this surface. */
  | "kind-not-installable"
  /** AI skills are discoverable but have no typed importer in this phase. */
  | "ai-skill-not-installable"
  /** Policy forbids installing this candidate. */
  | "policy-forbids";
/**
 * Which authority produced a policy decision
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanPolicySource".
 */
/** @experimental */
export type McpPlanPolicySource =
  /** No policy applied, so the server is permitted by default. */
  | "none"
  /** An enterprise allowlist evaluated the server. */
  | "enterprise-allowlist"
  /** The registry the card came from evaluated the server. */
  | "registry-policy"
  /** Local trust settings evaluated the server. */
  | "local-trust";
/**
 * Outcome of a catalog.search call: either bounded inert candidates, or one typed refusal. Never a partial success.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogSearchResult".
 */
/** @experimental */
export type CatalogSearchResult =
  | CatalogSearchSucceeded
  | CatalogNegotiationRefusedError
  | CatalogUnsupportedKindError
  | CatalogInvalidRequestError
  | CatalogAuthenticationRequiredError
  | CatalogPolicyRejectedError
  | CatalogNetworkFailureError
  | CatalogUnsafeRetrievalError
  | CatalogMalformedCardError
  | CatalogContractViolationError
  | CatalogUnavailableError;
/**
 * Which hardened-fetch control refused a retrieval
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnsafeRetrievalReason".
 */
/** @experimental */
export type CatalogUnsafeRetrievalReason =
  /** The URL used a scheme the runtime refuses to fetch. */
  | "blocked-scheme"
  /** The URL embedded credentials. */
  | "credentials-in-url"
  /** The URL resolved to a loopback, private, link-local, or cloud metadata address. */
  | "blocked-address"
  /** A redirect target resolved to a blocked address. */
  | "redirect-to-blocked-address"
  /** The configured proxy policy refused the request. */
  | "proxy-rejected"
  /** The authority is not permitted for card retrieval. */
  | "host-not-permitted";
/**
 * Why a catalog operation is not available on this runtime
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnavailableReason".
 */
/** @experimental */
export type CatalogUnavailableReason =
  /** Bounded search is not wired up on this runtime build. */
  | "search-unavailable"
  /** Install planning is not wired up on this runtime build. */
  | "planning-unavailable"
  /** No catalog authority is configured for this runtime. */
  | "authority-not-configured"
  /** The surface is disabled by policy on this runtime. */
  | "disabled-by-policy";
/**
 * Why no usable transport could be offered
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnavailableTransportReason".
 */
/** @experimental */
export type CatalogUnavailableTransportReason =
  /** The card advertises no transport this runtime can use. */
  | "no-eligible-transport"
  /** Every advertised transport is of a kind this runtime does not implement. */
  | "transport-not-supported"
  /** Eligible remotes could not be enumerated, so no explicit choice can be offered. */
  | "remote-enumeration-unavailable";
/**
 * Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandKind".
 */
/** @experimental */
export type SlashCommandKind =
  /** Command implemented by the runtime. */
  | "builtin"
  /** Command backed by a skill. */
  | "skill"
  /** Command registered by an SDK client or extension. */
  | "client";
/**
 * Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInputCompletion".
 */
/** @experimental */
export type SlashCommandInputCompletion = /** Input should complete filesystem directories. */ "directory";
/**
 * Whether a pending slash-command invocation effect was applied or cancelled by the host.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsInvocationEffectOutcome".
 */
/** @experimental */
export type CommandsInvocationEffectOutcome =
  /** The host applied the pending invocation effect. */
  | "applied"
  /** The host cancelled the pending invocation effect, so any provisional state must be reverted. */
  | "cancelled";

/** @experimental */
export type CommandsInvocationOrigin = "settings";
/**
 * Optional filters controlling which command sources to include in the listing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsListRequest".
 */
/** @experimental */
export type CommandsListRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * Include runtime built-in commands
       */
      includeBuiltins?: boolean;
      /**
       * Include enabled user-invocable skills and commands
       */
      includeSkills?: boolean;
      /**
       * Include commands registered by protocol clients, including SDK clients and extensions
       */
      includeClientCommands?: boolean;
    };
/**
 * Result of the queued command execution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandResult".
 */
/** @experimental */
export type QueuedCommandResult = QueuedCommandHandled | QueuedCommandNotHandled;
/**
 * Neutral SDK discriminator for the connected remote session kind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectedRemoteSessionMetadataKind".
 */
/** @experimental */
export type ConnectedRemoteSessionMetadataKind =
  /** Remote CLI session. */
  | "remote-session"
  /** GitHub Copilot coding agent session. */
  | "coding-agent";
/**
 * Controls how MCP tool result content is filtered: none leaves content unchanged, markdown sanitizes HTML while preserving Markdown-friendly output, and hidden_characters removes characters that can hide directives.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ContentFilterMode".
 */
/** @experimental */
export type ContentFilterMode =
  /** Leave MCP tool result content unchanged. */
  | "none"
  /** Sanitize HTML while preserving Markdown-friendly output. */
  | "markdown"
  /** Remove characters that can hide directives. */
  | "hidden_characters";
/**
 * Source category for a collected debug bundle entry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsSource".
 */
/** @experimental */
export type DebugCollectLogsSource =
  /** Session event log. */
  | "events"
  /** Process log for the session. */
  | "process-log"
  /** Interactive shell log for the session. */
  | "shell-log"
  /** Caller-provided diagnostic entry. */
  | "additional";
/**
 * Destination for the redacted debug bundle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsDestination".
 */
/** @experimental */
export type DebugCollectLogsDestination =
  | {
      /**
       * Absolute or server-relative path for the .tgz archive to create.
       */
      outputPath: string;
      /**
       * When true, create the archive atomically without overwriting an existing file by appending ` (N)` before the extension as needed. Defaults to false.
       */
      noOverwrite?: boolean;
      /**
       * Destination variant discriminator.
       */
      kind: "archive";
    }
  | {
      /**
       * Directory where redacted files should be staged. The directory is created if needed.
       */
      outputDirectory: string;
      /**
       * Destination variant discriminator.
       */
      kind: "directory";
    };
/**
 * Kind of caller-provided debug log entry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsEntryKind".
 */
/** @experimental */
export type DebugCollectLogsEntryKind =
  /** Include a single server-local file. */
  | "file"
  /** Include files from a server-local directory recursively. */
  | "directory";
/**
 * How a collected debug entry should be redacted before being staged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsRedaction".
 */
/** @experimental */
export type DebugCollectLogsRedaction =
  /** Redact the file as plain UTF-8 log text. */
  | "plain-text"
  /** Redact each non-empty line as a session event JSON object, falling back to plain-text redaction for malformed lines. */
  | "events-jsonl";
/**
 * Destination kind that was written.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsResultKind".
 */
/** @experimental */
export type DebugCollectLogsResultKind =
  /** A .tgz archive was written. */
  | "archive"
  /** A directory containing redacted files was written. */
  | "directory";
/**
 * Persisted extension discovery source
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtensionSource".
 */
/** @experimental */
export type DiscoveredExtensionSource =
  /** Extension discovered from the user's extensions directory. */
  | "user"
  /** Extension contributed by an installed plugin. */
  | "plugin";
/**
 * Effective extension loading and agent-management mode
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtensionMode".
 */
/** @experimental */
export type DiscoveredExtensionMode =
  /** Extensions are not loaded. */
  | "disabled"
  /** Extensions are loaded, but the agent cannot create, reload, or manage them. */
  | "load_only"
  /** Extensions are loaded and the agent can create, reload, and manage them. */
  | "load_and_augment";
/**
 * Hook event name. Discovery emits the file-configurable subset; SDK callbacks additionally support callback-only events.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HookType".
 */
/** @experimental */
export type HookType =
  /** Runs before a tool is invoked. */
  | "preToolUse"
  /** Runs before an MCP tool is invoked. */
  | "preMcpToolCall"
  /** Runs after a tool completes successfully. */
  | "postToolUse"
  /** Runs after a tool fails. */
  | "postToolUseFailure"
  /** Runs after the user submits a prompt. */
  | "userPromptSubmitted"
  /** Runs after the runtime transforms the submitted prompt for the model, before it is added to session history. */
  | "userPromptTransformed"
  /** Runs when a session starts. */
  | "sessionStart"
  /** Runs when a session ends. */
  | "sessionEnd"
  /** Runs after an agent result is produced. */
  | "postResult"
  /** Runs before a pull request description is generated. */
  | "prePRDescription"
  /** Runs when the agent encounters an error. */
  | "errorOccurred"
  /** Runs when the agent stops. */
  | "agentStop"
  /** Runs when a subagent starts. */
  | "subagentStart"
  /** Runs when a subagent stops. */
  | "subagentStop"
  /** Runs before conversation context is compacted. */
  | "preCompact"
  /** Runs when the agent requests permission. */
  | "permissionRequest"
  /** Runs when the agent emits a notification. */
  | "notification";
/**
 * Configuration tier that contributed a discovered hook action.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HookOrigin".
 */
/** @experimental */
export type HookOrigin =
  /** Hook loaded from user settings or the user's hook directory. */
  | "user"
  /** Hook loaded from repository settings or the repository hook directory. */
  | "repository"
  /** Hook provided by an enabled installed or explicit plugin. Projectless rows omit projectPath and do not expand a project directory. */
  | "plugin"
  /** Hook enforced by centrally managed policy. */
  | "policy";
/**
 * Server transport type: stdio, http, sse (deprecated), or memory
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredMcpServerType".
 */
/** @experimental */
export type DiscoveredMcpServerType =
  /** Server communicates over stdio with a local child process. */
  | "stdio"
  /** Server communicates over streamable HTTP. */
  | "http"
  /** Server communicates over Server-Sent Events (deprecated). */
  | "sse"
  /** Server is backed by an in-memory runtime implementation. */
  | "memory";
/**
 * Either '*' to receive all event types, or a non-empty list of event types to receive
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogTypes".
 */
/** @experimental */
export type EventLogTypes = "*" | [string, ...string[]];
/**
 * Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsAgentScope".
 */
/** @experimental */
export type EventsAgentScope =
  /** Return main-agent events and typed subagent lifecycle events. */
  | "primary"
  /** Return events from all agents. */
  | "all";
/**
 * Direction to page through the session's persisted event history. 'forward' pages from the cursor toward newer events; 'backward' returns the newest window first (tail-first) and pages toward older events. Events within a returned batch are always chronological (oldest-to-newest), even for a backward read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsReadDirection".
 */
/** @experimental */
export type EventsReadDirection =
  /** Page from the cursor toward newer events (default). */
  | "forward"
  /** Tail-first: return the newest events and page toward older events. */
  | "backward";
/**
 * Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read fell back to a boundary of the remaining history (the beginning for a forward read, the tail for a backward read). The fallback page is a fresh boundary snapshot, not a continuation of the requested cursor, so it may overlap already-rendered events; on 'expired' a consumer should reset/rebase its pagination state (or deduplicate by event id) before continuing from the returned cursor.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsCursorStatus".
 */
/** @experimental */
export type EventsCursorStatus =
  /** The cursor was applied successfully. */
  | "ok"
  /** The cursor referred to history that is no longer available. */
  | "expired";
/**
 * Discovery source: project (.github/extensions/), user (~/.copilot/extensions/), plugin (installed plugin), or session (session-state/<id>/extensions/)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionSource".
 */
/** @experimental */
export type ExtensionSource =
  /** Extension discovered from the current project's .github/extensions directory. */
  | "project"
  /** Extension discovered from the user's ~/.copilot/extensions directory. */
  | "user"
  /** Extension contributed by an installed plugin. */
  | "plugin"
  /** Extension discovered from the current session's state directory (loaded only for this session). */
  | "session";
/**
 * Current status: running, disabled, failed, or starting
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionStatus".
 */
/** @experimental */
export type ExtensionStatus =
  /** The extension process is running. */
  | "running"
  /** The extension is installed but disabled. */
  | "disabled"
  /** The extension failed to start or crashed. */
  | "failed"
  /** The extension process is starting. */
  | "starting";
/**
 * Tool call result (string or expanded result object)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolResult".
 */
/** @experimental */
export type ExternalToolResult = string | ExternalToolTextResultForLlm;
/**
 * Binary result type discriminator. Use "image" for images and "resource" for other binary data.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmBinaryResultsForLlmType".
 */
/** @experimental */
export type ExternalToolTextResultForLlmBinaryResultsForLlmType =
  /** Binary image data. */
  | "image"
  /** Other binary resource data. */
  | "resource";
/**
 * A content block within a tool result, which may be text, terminal output, image, audio, or a resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContent".
 */
/** @experimental */
export type ExternalToolTextResultForLlmContent =
  | ExternalToolTextResultForLlmContentText
  | ExternalToolTextResultForLlmContentTerminal
  | ExternalToolTextResultForLlmContentShellExit
  | ExternalToolTextResultForLlmContentImage
  | ExternalToolTextResultForLlmContentAudio
  | ExternalToolTextResultForLlmContentResourceLink
  | ExternalToolTextResultForLlmContentResource;
/**
 * Theme variant this icon is intended for
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceLinkIconTheme".
 */
/** @experimental */
export type ExternalToolTextResultForLlmContentResourceLinkIconTheme =
  /** Icon intended for light themes. */
  | "light"
  /** Icon intended for dark themes. */
  | "dark";
/**
 * The embedded resource contents, either text or base64-encoded binary
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceDetails".
 */
/** @experimental */
export type ExternalToolTextResultForLlmContentResourceDetails =
  | EmbeddedTextResourceContents
  | EmbeddedBlobResourceContents;
/**
 * Execution-critical factory storage operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryDurableOperation".
 */
/** @experimental */
export type FactoryDurableOperation =
  /** Creating the durable run and declared phases. */
  | "createRun"
  /** Persisting the transition to running. */
  | "markRunStarted"
  /** Persisting the terminal run envelope. */
  | "finishRun"
  /** Persisting subagent admission accounting. */
  | "reserveAgent"
  /** Rolling back an uncommitted subagent admission. */
  | "releaseAgent"
  /** Persisting an idempotent model-usage charge. */
  | "chargeCredit"
  /** Persisting active execution time. */
  | "addElapsed"
  /** Reading the authoritative AI-credit total. */
  | "reconcileCreditTotal"
  /** Reading a journal entry without treating storage failure as a cache miss. */
  | "journalGet"
  /** Persisting a journal entry before reporting success. */
  | "journalPut"
  /** Renewing the durable owner lease that proves this process still owns the run. */
  | "refreshLease";
/**
 * Current or terminal state of a factory run.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunStatus".
 */
/** @experimental */
export type FactoryRunStatus =
  /** The run was minted and is awaiting approval. */
  | "pending"
  /** The run is executing. */
  | "running"
  /** The run completed successfully. */
  | "completed"
  /** The run was interrupted while resource budget remained. */
  | "halted"
  /** The run was cancelled before completion. */
  | "cancelled"
  /** The factory body failed or reached a cumulative resource ceiling. */
  | "error";
/**
 * Machine-readable factory run failure.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunFailure".
 */
/** @experimental */
export type FactoryRunFailure =
  | {
      kind: FactoryRunFailureKind;
      /**
       * Approved effective ceiling that was reached.
       */
      value: number;
      /**
       * Factory run identifier.
       */
      runId: string;
      /**
       * Factory failure variant discriminator.
       */
      type: "factory_limit_reached";
    }
  | {
      /**
       * Factory run identifier whose changed limits were declined.
       */
      runId: string;
      /**
       * Human-readable reason the resume did not proceed.
       */
      reason: string;
      /**
       * Factory failure variant discriminator.
       */
      type: "factory_resume_declined";
    }
  | {
      /**
       * Stable failure code.
       */
      code: string;
      operation: FactoryDurableOperation;
      /**
       * Factory run identifier.
       */
      runId: string;
      /**
       * Factory failure variant discriminator.
       */
      type: "factory_durable_failure";
    }
  | {
      /**
       * Factory run identifier.
       */
      runId: string;
      /**
       * Confirmed usage in nano-AIU, representing the floor of what the run spent.
       */
      drainedNanoAiu: number;
      /**
       * Factory failure variant discriminator.
       */
      type: "factory_accounting_incomplete";
    }
  | {
      /**
       * Factory run identifier.
       */
      runId: string;
      /**
       * Factory failure variant discriminator.
       */
      type: "factory_provider_disconnected";
    };
/**
 * Cumulative resource ceiling that stopped a factory run.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunFailureKind".
 */
/** @experimental */
export type FactoryRunFailureKind =
  /** The run admitted the approved maximum total number of subagents. */
  | "maxTotalSubagents"
  /** The run reached the approved accumulated active-execution time in seconds. */
  | "timeoutSeconds"
  /** The run's settled subagent model usage exceeded the approved AI-credit ceiling, or no headroom remained for another subagent. */
  | "maxAiCredits";
/**
 * Kind of factory progress line.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryLogLineKind".
 */
/** @experimental */
export type FactoryLogLineKind =
  /** A narrator log line. */
  | "log"
  /** A named factory phase marker. */
  | "phase";
/**
 * Derived lifecycle state of a factory phase.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryPhaseStatus".
 */
/** @experimental */
export type FactoryPhaseStatus =
  /** The phase has not been entered yet. */
  | "pending"
  /** The phase is currently entered and accumulating active time. */
  | "active"
  /** The phase was entered and has since been closed. */
  | "completed"
  /** The phase was never entered because a later phase was entered or the run reached a terminal state. */
  | "skipped";
/**
 * Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FilterMapping".
 */
/** @experimental */
export type FilterMapping =
  | {
      [k: string]: ContentFilterMode;
    }
  | ContentFilterMode;
/**
 * Why the runtime is requesting a GitHub credential.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GitHubTokenAcquireReason".
 */
/** @experimental */
export type GitHubTokenAcquireReason =
  /** The runtime is acquiring the registration's first credential. */
  | "initial"
  /** The runtime is replacing a credential that is approaching expiry. */
  | "refresh";
/**
 * SDK host response to a GitHub credential request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GitHubTokenAcquireResult".
 */
/** @experimental */
export type GitHubTokenAcquireResult =
  | {
      /**
       * GitHub access token acquired by the SDK host.
       */
      accessToken: string;
      /**
       * OAuth token type. Defaults to bearer when omitted.
       */
      tokenType?: string;
      /**
       * Remaining token lifetime in seconds when callback execution completes. It must exceed the one-hour preflight refresh threshold.
       */
      expiresIn: number;
      /**
       * GitHub credential response variant discriminator.
       */
      kind: "token";
    }
  | {
      /**
       * GitHub credential response variant discriminator.
       */
      kind: "cancelled";
    };
/**
 * Optional compaction parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactRequest".
 */
/** @experimental */
export type HistoryCompactRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * Optional user-provided instructions to focus the compaction summary
       */
      customInstructions?: string;
      /**
       * What initiated this compaction request, recorded as the `trigger` on the persisted `session.compaction_start` / `session.compaction_complete` events. When absent, the compaction is persisted without trigger attribution (initiator unknown).
       */
      trigger?: /** User-requested compaction, e.g. the /compact command or a direct history.compact call. */
        | "manual"
        /** Compaction requested while switching to a model with a smaller context window. */
        | "model_switch";
      /**
       * Context window token limit this compaction is targeting, recorded as the `tokenLimit` on the persisted `session.compaction_start` / `session.compaction_complete` events. Set it when the compaction targets a window other than the compacting model's own, e.g. switching to a model with a smaller context window: the compaction still runs on the current model, so the limit that motivated it would otherwise be lost. When absent, the events record the compacting model's own resolved limit. Attribution metadata only - it does not change how much the compaction removes.
       */
      tokenLimit?: number;
    };
/**
 * Reason a captured file was not restored.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryFileRestoreSkipReason".
 */
/** @experimental */
export type HistoryFileRestoreSkipReason =
  /** The file changed after Copilot's last captured write. */
  | "user-modified"
  /** A faithful preimage was not captured. */
  | "skipped-capture";
/**
 * Reason a rewind read (rewind points, file-restore preview, or session diff) could not be answered from the session's file-change captures.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindUnavailableReason".
 */
/** @experimental */
export type HistoryRewindUnavailableReason =
  /** The session did not opt into file-change tracking before its first turn. */
  | "file-change-tracking-disabled"
  /** The session still has work that may mutate files or history. Transient: the same request succeeds once the session settles, so callers should retry rather than treat it as a failure. */
  | "session-busy"
  /** Remote-backed rewind routing is not supported. */
  | "unsupported-remote-session";
/**
 * Aggregate file change represented by a rewind preview.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindChangeType".
 */
/** @experimental */
export type HistoryRewindChangeType =
  /** The discarded turns created the file. */
  | "created"
  /** The discarded turns deleted the file. */
  | "deleted"
  /** The discarded turns modified the file. */
  | "modified";
/**
 * Scope of a rewind operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindMode".
 */
/** @experimental */
export type HistoryRewindMode =
  /** Discard conversation events while leaving files unchanged. */
  | "conversation"
  /** Discard conversation events and restore captured files changed by those turns. */
  | "conversation-and-files";
/**
 * Outcome of a rewind request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindOutcome".
 */
/** @experimental */
export type HistoryRewindOutcome =
  /** The requested rewind completed; reachable in either mode. */
  | "success"
  /** The session still has work that may mutate files or history; reachable in either mode. */
  | "session-busy"
  /** A conversation-and-files rewind was requested for a session that did not enable capture; conversation-only rewinds never produce this. */
  | "file-change-tracking-disabled"
  /** Remote-backed rewind routing is not supported; reachable in either mode. */
  | "unsupported-remote-session"
  /** File restore failed and all applied file changes were rolled back; only conversation-and-files rewinds produce this. */
  | "files-rolled-back"
  /** File restore failed and its rollback could not fully restore the pre-rewind state; only conversation-and-files rewinds produce this. */
  | "rollback-incomplete"
  /** Conversation truncation failed. In conversation-and-files mode any files that were restored are left in place because conversation history cannot be un-truncated; in conversation-only mode no files are restored. Consult restoredFiles for what, if anything, was applied. */
  | "truncation-failed"
  /** The conversation was rewound (and, in conversation-and-files mode, captured files were restored), but persisted checkpoints could not be cleaned up; reachable in either mode. */
  | "checkpoint-cleanup-failed"
  /** Files and conversation were rewound, but obsolete file snapshots could not be removed; only conversation-and-files rewinds produce this. */
  | "snapshot-prune-failed";
/**
 * Source for direct repo installs (when marketplace is empty)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSource".
 */
/** @experimental */
export type InstalledPluginSource =
  | string
  | InstalledPluginSourceGitHub
  | InstalledPluginSourceUrl
  | InstalledPluginSourceLocal;
/**
 * Which tier this target belongs to
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionDiscoveryPathLocation".
 */
/** @experimental */
export type InstructionDiscoveryPathLocation =
  /** Instructions live in user-level configuration. */
  | "user"
  /** Instructions live in repository-level configuration. */
  | "repository"
  /** Instructions live under the current working directory. */
  | "working-directory"
  /** Instructions live in plugin-provided configuration. */
  | "plugin";
/**
 * Whether the target is a single file or a directory of instruction files
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionDiscoveryPathKind".
 */
/** @experimental */
export type InstructionDiscoveryPathKind =
  /** The target is a single instruction file. */
  | "file"
  /** The target is a directory that holds instruction files. */
  | "directory";
/**
 * Category of instruction source — used for merge logic
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionSourceType".
 */
/** @experimental */
export type InstructionSourceType =
  /** Instructions loaded from the user's home configuration. */
  | "home"
  /** Instructions loaded from repository-scoped files. */
  | "repo"
  /** Instructions loaded from model-specific files. */
  | "model"
  /** Instructions loaded from VS Code instruction files. */
  | "vscode"
  /** Instructions discovered from nested agent files. */
  | "nested-agents"
  /** Instructions inherited from child instruction files. */
  | "child-instructions"
  /** Instructions supplied by an installed plugin. */
  | "plugin";
/**
 * Where this source lives — used for UI grouping
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionSourceLocation".
 */
/** @experimental */
export type InstructionSourceLocation =
  /** Instructions live in user-level configuration. */
  | "user"
  /** Instructions live in repository-level configuration. */
  | "repository"
  /** Instructions live under the current working directory. */
  | "working-directory"
  /** Instructions live in plugin-provided configuration. */
  | "plugin";
/**
 * Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpRequestStartTransport".
 */
/** @experimental */
export type LlmInferenceHttpRequestStartTransport =
  /** Plain HTTP or SSE response. Each body chunk is an opaque byte range; the response is a status line, headers, and a (possibly streamed) body. */
  | "http"
  /** Full-duplex WebSocket channel. Each body chunk maps to exactly one WebSocket message and the `binary` flag distinguishes text from binary frames; request and response chunks flow concurrently. */
  | "websocket";
/**
 * Repository host type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContextHostType".
 */
/** @experimental */
export type SessionContextHostType =
  /** Session repository is hosted on GitHub. */
  | "github"
  /** Session repository is hosted on Azure DevOps. */
  | "ado";
/**
 * Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLogLevel".
 */
/** @experimental */
export type SessionLogLevel =
  /** Informational message. */
  | "info"
  /** Warning message that may require attention. */
  | "warning"
  /** Error message describing a failure. */
  | "error";
/**
 * UI theme preference per SEP-1865
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsTheme".
 */
/** @experimental */
export type McpAppsHostContextDetailsTheme =
  /** Light UI theme */
  | "light"
  /** Dark UI theme */
  | "dark";
/**
 * Current display mode (SEP-1865)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsDisplayMode".
 */
/** @experimental */
export type McpAppsHostContextDetailsDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Allowed values for the `McpAppsHostContextDetailsAvailableDisplayMode` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsAvailableDisplayMode".
 */
/** @experimental */
export type McpAppsHostContextDetailsAvailableDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Platform type for responsive design
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetailsPlatform".
 */
/** @experimental */
export type McpAppsHostContextDetailsPlatform =
  /** Host runs in a web browser */
  | "web"
  /** Host runs as a desktop application */
  | "desktop"
  /** Host runs on a mobile device */
  | "mobile";
/**
 * UI theme preference per SEP-1865
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsTheme".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsTheme =
  /** Light UI theme */
  | "light"
  /** Dark UI theme */
  | "dark";
/**
 * Current display mode (SEP-1865)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsDisplayMode".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Allowed values for the `McpAppsSetHostContextDetailsAvailableDisplayMode` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsAvailableDisplayMode".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsAvailableDisplayMode =
  /** Rendered inline within the host conversation surface */
  | "inline"
  /** Rendered as a fullscreen overlay */
  | "fullscreen"
  /** Rendered as a picture-in-picture floating panel */
  | "pip";
/**
 * Platform type for responsive design
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetailsPlatform".
 */
/** @experimental */
export type McpAppsSetHostContextDetailsPlatform =
  /** Host runs in a web browser */
  | "web"
  /** Host runs as a desktop application */
  | "desktop"
  /** Host runs on a mobile device */
  | "mobile";
/**
 * Serializable MCP server configuration (stdio process or remote HTTP/SSE)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSerializableServerConfig".
 */
/** @experimental */
export type McpSerializableServerConfig = McpServerConfigStdio | McpServerConfigHttp;
/**
 * Telemetry-obfuscation policy for an MCP server's tools.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSafeForTelemetry".
 */
/** @experimental */
export type McpSafeForTelemetry = boolean | McpSafeForTelemetryFields;
/**
 * Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerAuthConfig".
 */
/** @experimental */
export type McpServerAuthConfig = boolean | McpServerAuthConfigRedirectPort;
/**
 * Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigDeferTools".
 */
/** @experimental */
export type McpServerConfigDeferTools =
  /** Tools may be deferred under certain conditions */
  | "auto"
  /** Tools are always included in the initial tool list, even when tool search is enabled. */
  | "never";
/**
 * Local MCP transport type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigStdioType".
 */
/** @experimental */
export type McpServerConfigStdioType =
  /** Legacy alias for the local stdio transport. */
  | "local"
  /** Server communicates over stdio with a local child process. */
  | "stdio";
/**
 * Remote transport type. Defaults to "http" when omitted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpType".
 */
/** @experimental */
export type McpServerConfigHttpType =
  /** Streamable HTTP transport. */
  | "http"
  /** Server-Sent Events transport. */
  | "sse";
/**
 * OAuth grant type to use when authenticating to the remote MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttpOauthGrantType".
 */
/** @experimental */
export type McpServerConfigHttpOauthGrantType =
  /** Interactive browser-based authorization code flow with PKCE. */
  | "authorization_code"
  /** Headless client credentials flow using the configured OAuth client. */
  | "client_credentials";
/**
 * Structured MCP elicitation mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpElicitationFormMode".
 */
/** @experimental */
export type McpElicitationFormMode = "form";
/**
 * Host response: supply dynamic headers or decline this refresh.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpHeadersHandlePendingHeadersRefreshRequest".
 */
/** @experimental */
export type McpHeadersHandlePendingHeadersRefreshRequest =
  | {
      /**
       * Headers to overlay onto the MCP request. Dynamic headers override static config headers but do not replace SDK-managed request headers.
       */
      headers: {
        [k: string]: string | undefined;
      };
      /**
       * Headers-refresh response variant discriminator.
       */
      kind: "headers";
    }
  | {
      /**
       * Headers-refresh response variant discriminator.
       */
      kind: "none";
    };
/**
 * One eligible way to run the server, represented as a tagged package or remote variant so package identity and endpoint states cannot contradict the install method.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanTransportChoice".
 */
/** @experimental */
export type McpPlanTransportChoice = McpPlanTransportChoicePackage | McpPlanTransportChoiceRemote;
/**
 * Transport exposed by a locally launched package
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanPackageTransport".
 */
/** @experimental */
export type McpPlanPackageTransport =
  /** A locally launched process spoken to over standard input and output. */
  "stdio";
/**
 * Discriminator for a package-backed transport choice
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanPackageInstallMethod".
 */
/** @experimental */
export type McpPlanPackageInstallMethod = /** Install and run a local package. */ "package";
/**
 * One non-secret value a transport choice needs, represented as a scalar or enumerated variant so enum values cannot be missing or attached to another type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRequiredValue".
 */
/** @experimental */
export type McpPlanRequiredValue = McpPlanRequiredValueScalar | McpPlanRequiredValueEnum;
/**
 * Discriminator for a scalar required value
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRequiredValueScalarKind".
 */
/** @experimental */
export type McpPlanRequiredValueScalarKind = /** The value uses one scalar type. */ "scalar";
/**
 * Where a required value is applied when the planned server is launched
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanValueCategory".
 */
/** @experimental */
export type McpPlanValueCategory =
  /** Set as an environment variable on the launched process. */
  | "environment-variable"
  /** Passed to the runtime that launches the package. */
  | "runtime-argument"
  /** Passed to the packaged server itself. */
  | "package-argument"
  /** Sent as a request header to a remote endpoint. */
  | "header"
  /** Substituted into the remote endpoint URL. */
  | "url-variable";
/**
 * Scalar type a required value must conform to
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanScalarValueType".
 */
/** @experimental */
export type McpPlanScalarValueType =
  /** Free text. */
  | "string"
  /** A number. */
  | "number"
  /** A boolean. */
  | "boolean"
  /** A filesystem path. */
  | "path";
/**
 * Discriminator for an enumerated required value
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRequiredValueEnumKind".
 */
/** @experimental */
export type McpPlanRequiredValueEnumKind = /** The value uses a fixed non-empty enumeration. */ "enum";
/**
 * Discriminator for an enumerated required value
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanEnumValueType".
 */
/** @experimental */
export type McpPlanEnumValueType = /** One of a fixed, non-empty set of permitted values. */ "enum";
/**
 * A runtime-assigned secret placeholder. The identifier is carried once, inside the placeholder, so it cannot contradict a separate secret-id field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanSecretReference".
 */
/** @experimental */
export type McpPlanSecretReference = string;
/**
 * Transport exposed by a remote endpoint
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRemoteTransport".
 */
/** @experimental */
export type McpPlanRemoteTransport =
  /** An HTTP endpoint. */
  | "http"
  /** A streamable HTTP endpoint. */
  | "streamable-http"
  /** A server-sent events endpoint. */
  | "sse";
/**
 * Discriminator for a remote-endpoint transport choice
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRemoteInstallMethod".
 */
/** @experimental */
export type McpPlanRemoteInstallMethod = /** Connect to a remote endpoint. */ "remote";
/**
 * Configuration scope an MCP install plan targets
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanScope".
 */
/** @experimental */
export type McpPlanScope = /** The user's own MCP configuration. */ "user";
/**
 * What policy decided for a planned server
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanPolicyDecision".
 */
/** @experimental */
export type McpPlanPolicyDecision =
  /** Policy permits the server. */
  | "allowed"
  /** Policy forbids the server, so the plan cannot be applied. */
  | "blocked"
  /** Policy permits the server only after an explicit approval. */
  | "requires-approval";
/**
 * Whether a planned configuration change would create or modify an entry
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanConfigurationOperation".
 */
/** @experimental */
export type McpPlanConfigurationOperation =
  /** Creates a configuration entry that does not exist yet. */
  | "add"
  /** Modifies a configuration entry that already exists. */
  | "update";
/**
 * Consumer allowed to call an MCP tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpToolUiVisibility".
 */
/** @experimental */
export type McpToolUiVisibility =
  /** The model may call the tool. */
  | "model"
  /** An MCP App view may call the tool. */
  | "app";
/**
 * Host response to the pending OAuth request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthPendingRequestResponse".
 */
/** @experimental */
export type McpOauthPendingRequestResponse =
  | {
      /**
       * Access token acquired by the SDK host
       */
      accessToken: string;
      /**
       * OAuth token type. Defaults to bearer when omitted.
       */
      tokenType?: string;
      /**
       * Token lifetime in seconds, if known.
       */
      expiresIn?: number;
      /**
       * OAuth response variant discriminator.
       */
      kind: "token";
    }
  | {
      /**
       * OAuth response variant discriminator.
       */
      kind: "cancelled";
    };
/**
 * OAuth grant type override for this login.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthLoginGrantType".
 */
/** @experimental */
export type McpOauthLoginGrantType =
  /** Interactive browser-based OAuth flow using an authorization code, typically with PKCE. */
  | "authorization_code"
  /** Headless OAuth flow where a confidential client authenticates directly with a client secret. */
  | "client_credentials";
/**
 * Why a passive MCP OAuth probe determined authentication is needed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthProbeNeedsAuthReason".
 */
/** @experimental */
export type McpOauthProbeNeedsAuthReason =
  /** No token was sent and the server requires authentication. */
  | "initial"
  /** A cached token was sent and rejected. */
  | "refresh"
  /** The server returned a 403 insufficient_scope challenge, indicating additional scopes or audience are needed. */
  | "upscope";
/**
 * Passive MCP OAuth probe result. `authenticated` means the server accepted the probe request while an OAuth-origin access token was attached; it does not prove the server required or independently validated that token. The probe does not make a second unauthenticated request. Failed is an expected probe-domain outcome; JSON-RPC errors are reserved for API-call failures.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthProbeResult".
 */
/** @experimental */
export type McpOauthProbeResult =
  | {
      httpResponse: McpOauthHttpResponse;
      /**
       * Probe outcome variant discriminator.
       */
      status: "no-auth-required";
    }
  | {
      httpResponse: McpOauthHttpResponse;
      /**
       * Probe outcome variant discriminator.
       */
      status: "authenticated";
    }
  | {
      httpResponse: McpOauthHttpResponse;
      reason: McpOauthProbeNeedsAuthReason;
      wwwAuthenticateParams?: McpOauthWWWAuthenticateParams;
      /**
       * Probe outcome variant discriminator.
       */
      status: "needs-auth";
    }
  | {
      /**
       * Human-readable probe failure detail.
       */
      error: string;
      httpResponse?: McpOauthHttpResponse;
      /**
       * Probe outcome variant discriminator.
       */
      status: "failed";
    };
/**
 * What an install plan is computed from: a candidate handle from a previous search, or a card supplied directly.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallSource".
 */
/** @experimental */
export type McpPlanInstallSource = McpPlanInstallSourceCandidate | McpPlanInstallSourceCard;
/**
 * Discriminator for a candidate-backed install-plan source
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallSourceCandidateKind".
 */
/** @experimental */
export type McpPlanInstallSourceCandidateKind = /** Plan from a candidate returned by catalog search. */ "candidate";
/**
 * Discriminator for a caller-supplied-card install-plan source
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallSourceCardKind".
 */
/** @experimental */
export type McpPlanInstallSourceCardKind = /** Plan directly from a caller-supplied card. */ "card";
/**
 * A card supplied directly by the caller. Exactly one of a URL or embedded data, encoded structurally so neither both nor neither can be expressed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerCardReference".
 */
/** @experimental */
export type McpServerCardReference = McpServerCardUrl | McpServerCardEmbedded;
/**
 * Discriminator for a URL-backed MCP server card
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerCardUrlKind".
 */
/** @experimental */
export type McpServerCardUrlKind = /** Retrieve the card from its URL. */ "url";
/**
 * Discriminator for an embedded MCP server card
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerCardEmbeddedKind".
 */
/** @experimental */
export type McpServerCardEmbeddedKind = /** Use the embedded card document. */ "embedded";
/**
 * Outcome of an mcp.planInstall call: either a normalised plan, or one typed refusal. Nothing is written in either case.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallResult".
 */
/** @experimental */
export type McpPlanInstallResult =
  | McpPlanInstallPlanned
  | CatalogNegotiationRefusedError
  | CatalogHandleRejectedError
  | CatalogInvalidRequestError
  | CatalogAuthenticationRequiredError
  | CatalogPolicyRejectedError
  | CatalogNetworkFailureError
  | CatalogUnsafeRetrievalError
  | CatalogMalformedCardError
  | CatalogContractViolationError
  | CatalogUnavailableTransportError
  | CatalogNotInstallableError
  | CatalogUnavailableError;
/**
 * MCP server configuration (stdio, remote HTTP/SSE, or in-process)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfig".
 */
/** @experimental */
export type McpServerConfig = (McpServerConfigStdio | McpServerConfigHttp) | undefined;
/**
 * Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSamplingExecutionAction".
 */
/** @experimental */
export type McpSamplingExecutionAction =
  /** The sampling inference completed and produced a result. */
  | "success"
  /** The sampling inference failed or was rejected. */
  | "failure"
  /** The sampling inference was cancelled before completion. */
  | "cancelled";
/**
 * In-process MCP transport type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigMemoryType".
 */
/** @experimental */
/** @internal */
export type McpServerConfigMemoryType = "memory";
/**
 * How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSetEnvValueModeDetails".
 */
/** @experimental */
export type McpSetEnvValueModeDetails =
  /** Treat MCP server environment values as literal strings. */
  | "direct"
  /** Treat MCP server environment values as host-side references to resolve before launch. */
  | "indirect";
/**
 * Per-source context-window attribution, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContextAttribution".
 */
/** @experimental */
export type SessionContextAttribution = {
  /**
   * Total token count of the current context window the entries are measured against (system message + conversation messages + tool definitions — the same total reported by /context). Divide an entry's `tokens` by this to derive its share.
   */
  totalTokens: number;
  /**
   * The concrete model id the entire breakdown was tokenized against (feeds the per-model token multiplier). Under `Auto` (Free/Student) this is the resolved model, not the literal `auto` sentinel, so totals are not undercounted. A single-model approximation of a potentially multi-model Auto session.
   */
  modelId: string;
  /**
   * How `modelId` was chosen. Not a closed set — tolerate unknown values. Known values today: `autoResolved` (the model Auto resolved to), `selected` (the user's explicitly selected model), `default` (a fallback before any model is known).
   */
  modelSource: string;
  /**
   * Maximum prompt tokens the resolved model accepts — the denominator for a `##k/###k` context-usage display. Mirrors `SessionContextInfo.promptTokenLimit`.
   */
  promptTokenLimit: number;
  /**
   * Prompt limit plus the model's output reserve: the full context window `categories.freeSpace` and `categories.buffer` are measured against. Mirrors `SessionContextInfo.limit`.
   */
  limit: number;
  /**
   * Output reserve plus the tokens past the buffer-exhaustion blocking threshold. Mirrors `SessionContextInfo.bufferTokens`.
   */
  bufferTokens: number;
  /**
   * Token count at which background compaction starts. Mirrors `SessionContextInfo.compactionThreshold`.
   */
  compactionThreshold: number;
  /**
   * The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
   */
  categories: {
    /**
     * System prompt tokens, excluding custom instructions.
     */
    systemPrompt: number;
    /**
     * Custom-instructions tokens (0 when none are configured).
     */
    customInstructions: number;
    /**
     * Non-MCP tool-definition tokens.
     */
    systemTools: number;
    /**
     * MCP tool-definition tokens.
     */
    mcpTools: number;
    /**
     * Conversation (user/assistant/tool) message tokens.
     */
    messages: number;
    /**
     * Remaining unused window capacity (clamped at 0).
     */
    freeSpace: number;
    /**
     * Output reserve plus post-blocking-threshold buffer.
     */
    buffer: number;
  };
  /**
   * Flat list of per-source attribution entries. Group by `kind` and render unrecognized kinds generically. Nesting and rollups are expressed via `parentId`.
   */
  entries: {
    /**
     * Source category for this entry. Not a closed set — tolerate unknown values. Known values today: `skill`, `subagent`, `mcpServer`, `tool`, `system`, `toolDefinition`, `plugin`.
     */
    kind: string;
    /**
     * Identifier for this entry, formed by joining its `kind` and source name (e.g. `tool:bash`, `skill:tmux`, `toolDefinition:bash`); unique within the snapshot. Use it to match the same entry across snapshots, to correlate with other APIs (skill/agent/MCP registries), and as the `parentId` target for nesting. Distinct from the human-facing `label`.
     */
    id: string;
    /**
     * Human-readable display label, e.g. `bash` or `skill: tmux`. Presentation-only; may be localized/reformatted without notice — do not key off it.
     */
    label: string;
    /**
     * Token count currently in context attributable to this entry.
     */
    tokens: number;
    /**
     * Optional `id` of the parent entry: e.g. a `plugin` entry parenting its `skill`/`mcpServer` entries, or the `system` entry parenting `toolDefinition` entries. Omitted for top-level entries.
     */
    parentId?: string;
    /**
     * Supplementary per-entry metadata (e.g. `messageCount`, `role`, `evictable`, `pluginSource`). Values are stringified; parse as needed and ignore unrecognized keys.
     */
    attributes?: {
      [k: string]: string | undefined;
    };
  }[];
  /**
   * Successful compaction history for the session.
   */
  compactions: {
    /**
     * Number of successful compactions in this session.
     */
    count: number;
  };
} | null;
/**
 * Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContextInfo".
 */
/** @experimental */
export type SessionContextInfo = {
  /**
   * The model used for token counting
   */
  modelName: string;
  /**
   * Tokens consumed by the system prompt
   */
  systemTokens: number;
  /**
   * Tokens consumed by user/assistant/tool messages
   */
  conversationTokens: number;
  /**
   * Tokens consumed by tool definitions sent to the model (excludes deferred tools)
   */
  toolDefinitionsTokens: number;
  /**
   * Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
   */
  mcpToolsTokens: number;
  /**
   * Sum of system, conversation and tool-definition tokens
   */
  totalTokens: number;
  /**
   * Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified)
   */
  promptTokenLimit: number;
  /**
   * Token count at which background compaction starts (configurable percentage of promptTokenLimit)
   */
  compactionThreshold: number;
  /**
   * Prompt token limit plus the model's full output token limit.
   */
  limit: number;
  /**
   * Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%)
   */
  bufferTokens: number;
} | null;
/**
 * Hosting platform type of the repository
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionWorkingDirectoryContextHostType".
 */
/** @experimental */
export type SessionWorkingDirectoryContextHostType =
  /** The working directory repository is hosted on GitHub. */
  | "github"
  /** The working directory repository is hosted on Azure DevOps. */
  | "ado";
/**
 * The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotCurrentMode".
 */
/** @experimental */
export type MetadataSnapshotCurrentMode =
  /** The agent is responding interactively to the user. */
  | "interactive"
  /** The agent is preparing a plan before making changes. */
  | "plan"
  /** The agent is working autonomously toward task completion. */
  | "autopilot";
/**
 * Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotRemoteMetadataTaskType".
 */
/** @experimental */
export type MetadataSnapshotRemoteMetadataTaskType =
  /** Remote task originated from Copilot Coding Agent. */
  | "cca"
  /** Remote task originated from a CLI remote-session invocation. */
  | "cli";
/**
 * Current policy state for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPolicyState".
 */
/** @experimental */
export type ModelPolicyState =
  /** The model is enabled by policy. */
  | "enabled"
  /** The model is disabled by policy. */
  | "disabled"
  /** No explicit policy is configured for the model. */
  | "unconfigured";
/**
 * Model capability category for grouping in the model picker
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerCategory".
 */
/** @experimental */
export type ModelPickerCategory =
  /** Lightweight model category optimized for faster, lower-cost interactions. */
  | "lightweight"
  /** Versatile model category suitable for a broad range of tasks. */
  | "versatile"
  /** Powerful model category optimized for complex tasks. */
  | "powerful";
/**
 * Relative cost tier for token-based billing users
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerPriceCategory".
 */
/** @experimental */
export type ModelPickerPriceCategory =
  /** Lowest relative token cost tier. */
  | "low"
  /** Medium relative token cost tier. */
  | "medium"
  /** High relative token cost tier. */
  | "high"
  /** Highest relative token cost tier. */
  | "very_high";
/**
 * Optional listing options.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelListRequest".
 */
/** @experimental */
export type ModelListRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * If true, bypasses the per-session model list cache and re-fetches from CAPI.
       */
      skipCache?: boolean;
    };
/**
 * Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderConfigType".
 */
/** @experimental */
export type ProviderConfigType =
  /** Generic OpenAI-compatible API. */
  | "openai"
  /** Azure OpenAI Service endpoint. */
  | "azure"
  /** Anthropic API endpoint. */
  | "anthropic";
/**
 * Wire API format (openai/azure only). Defaults to "completions".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderConfigWireApi".
 */
/** @experimental */
export type ProviderConfigWireApi =
  /** OpenAI Chat Completions wire format. */
  | "completions"
  /** OpenAI Responses API wire format. */
  | "responses";
/**
 * Provider transport. Defaults to "http".
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderConfigTransport".
 */
/** @experimental */
export type ProviderConfigTransport =
  /** HTTP request/streaming transport. */
  | "http"
  /** WebSocket transport. */
  | "websockets";
/**
 * Allowed values for the `OptionsUpdateAdditionalContentExclusionPolicyScope` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateAdditionalContentExclusionPolicyScope".
 */
/** @experimental */
export type OptionsUpdateAdditionalContentExclusionPolicyScope =
  /** The content exclusion policy applies to the current repository. */
  | "repo"
  /** The content exclusion policy applies across all repositories. */
  | "all";
/**
 * Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateContextTier".
 */
/** @experimental */
export type OptionsUpdateContextTier =
  /** Use the model's default context tier and its standard token limits / pricing. */
  | "default"
  /** Use the model's long-context tier (when available) so larger inputs are accepted and tier-specific pricing applies. */
  | "long_context";
/**
 * How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateEnvValueMode".
 */
/** @experimental */
export type OptionsUpdateEnvValueMode =
  /** Pass MCP server environment values as literal strings. */
  | "direct"
  /** Resolve MCP server environment values from host-side references. */
  | "indirect";
/**
 * Reasoning summary mode for supported model clients.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateReasoningSummary".
 */
/** @experimental */
export type OptionsUpdateReasoningSummary =
  /** Do not request reasoning summaries from the model. */
  | "none"
  /** Request a concise summary of model reasoning. */
  | "concise"
  /** Request a detailed summary of model reasoning. */
  | "detailed";
/**
 * Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateToolFilterPrecedence".
 */
/** @experimental */
export type OptionsUpdateToolFilterPrecedence =
  /** If availableTools is set, it is the only constraint that applies (excludedTools is ignored). Preserves CLI / pre-existing client behavior. Default. */
  | "available"
  /** A tool is enabled if and only if it matches the allowlist (or the allowlist is unset) AND it does not match the denylist. Makes 'all except X' expressible by combining the two lists. */
  | "excluded";
/**
 * The client's response to the pending permission prompt
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecision".
 */
/** @experimental */
export type PermissionDecision =
  | PermissionDecisionApproveOnce
  | PermissionDecisionApproveForSession
  | PermissionDecisionApproveForLocation
  | PermissionDecisionApprovePermanently
  | PermissionDecisionReject
  | PermissionDecisionUserNotAvailable
  | PermissionDecisionApproved
  | PermissionDecisionApprovedForSession
  | PermissionDecisionApprovedForLocation
  | PermissionDecisionCancelled
  | PermissionDecisionDeniedByRules
  | PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser
  | PermissionDecisionDeniedInteractivelyByUser
  | PermissionDecisionDeniedByContentExclusionPolicy
  | PermissionDecisionDeniedByPermissionRequestHook;
/**
 * Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApproval".
 */
/** @experimental */
export type PermissionDecisionApproveForSessionApproval =
  | PermissionDecisionApproveForSessionApprovalCommands
  | PermissionDecisionApproveForSessionApprovalRead
  | PermissionDecisionApproveForSessionApprovalWrite
  | PermissionDecisionApproveForSessionApprovalMcp
  | PermissionDecisionApproveForSessionApprovalMcpSampling
  | PermissionDecisionApproveForSessionApprovalMemory
  | PermissionDecisionApproveForSessionApprovalCustomTool
  | PermissionDecisionApproveForSessionApprovalExtensionManagement
  | PermissionDecisionApproveForSessionApprovalFactory
  | PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess
  | PermissionDecisionApproveForSessionApprovalExtensionEnvAccess;
/**
 * Approval to persist for this location
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApproval".
 */
/** @experimental */
export type PermissionDecisionApproveForLocationApproval =
  | PermissionDecisionApproveForLocationApprovalCommands
  | PermissionDecisionApproveForLocationApprovalRead
  | PermissionDecisionApproveForLocationApprovalWrite
  | PermissionDecisionApproveForLocationApprovalMcp
  | PermissionDecisionApproveForLocationApprovalMcpSampling
  | PermissionDecisionApproveForLocationApprovalMemory
  | PermissionDecisionApproveForLocationApprovalCustomTool
  | PermissionDecisionApproveForLocationApprovalExtensionManagement
  | PermissionDecisionApproveForLocationApprovalFactory
  | PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess
  | PermissionDecisionApproveForLocationApprovalExtensionEnvAccess;
/**
 * Disposition of a permission request as observed by the responding client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionOutcome".
 */
/** @experimental */
export type PermissionDecisionOutcome =
  /** The request was approved automatically without a new human decision. */
  | "auto_approved"
  /** The request was denied without an interactive user decision; source records why. */
  | "autopilot_denied"
  /** The response came from an interactive user prompt. */
  | "prompted_user";
/**
 * Controlled reason or actor responsible for a permission response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionSource".
 */
/** @experimental */
export type PermissionDecisionSource =
  /** The response followed the assisted-approval judge recommendation. */
  | "assisted_approval"
  /** A human supplied the response through an interactive prompt. */
  | "human_response"
  /** The host applied a standing policy or override rather than a judge recommendation or human decision. */
  | "host_policy"
  /** The host denied the request because no interactive user response was available. */
  | "unattended_fallback";
/**
 * Client surface that submitted a permission response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionSurface".
 */
/** @experimental */
export type PermissionDecisionSurface =
  /** The interactive Copilot CLI terminal UI. */
  | "tui"
  /** The non-interactive Copilot CLI prompt mode. */
  | "prompt_mode"
  /** The Copilot App client. */
  | "copilot_app"
  /** An Agent Client Protocol host. */
  | "acp"
  /** A generic Copilot SDK client. */
  | "sdk";
/**
 * Response capability available to the client when it settled a permission request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionResponseCapability".
 */
/** @experimental */
export type PermissionResponseCapability =
  /** The client could ask a user for this decision. */
  | "interactive"
  /** The client could return an automated response but could not ask a user. */
  | "headless"
  /** The client had no response path available. */
  | "none";
/**
 * Tool approval to persist and apply
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetails".
 */
/** @experimental */
export type PermissionsLocationsAddToolApprovalDetails =
  | PermissionsLocationsAddToolApprovalDetailsCommands
  | PermissionsLocationsAddToolApprovalDetailsRead
  | PermissionsLocationsAddToolApprovalDetailsWrite
  | PermissionsLocationsAddToolApprovalDetailsMcp
  | PermissionsLocationsAddToolApprovalDetailsMcpSampling
  | PermissionsLocationsAddToolApprovalDetailsMemory
  | PermissionsLocationsAddToolApprovalDetailsCustomTool
  | PermissionsLocationsAddToolApprovalDetailsExtensionManagement
  | PermissionsLocationsAddToolApprovalDetailsFactory
  | PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess
  | PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccess;
/**
 * Whether the location is a git repo or directory
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationType".
 */
/** @experimental */
export type PermissionLocationType =
  /** The permission location is persisted at the git repository root. */
  | "repo"
  /** The permission location is persisted at the working directory. */
  | "dir";
/**
 * Optional source for permission-mode telemetry. Defaults to `rpc` when omitted for SDK callers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionModeSource".
 */
/** @experimental */
export type PermissionModeSource =
  /** The mode was set from a CLI command-line flag. */
  | "cli_flag"
  /** The mode was set by a slash command. */
  | "slash_command"
  /** The mode was set by confirming autopilot behavior. */
  | "autopilot_confirmation"
  /** The mode was set at startup by the `defaultPermissionMode` user setting. */
  | "user_setting"
  /** The mode was set through an RPC caller. */
  | "rpc";
/**
 * Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicyScope".
 */
/** @experimental */
export type PermissionsConfigureAdditionalContentExclusionPolicyScope =
  /** The content exclusion policy applies to the current repository. */
  | "repo"
  /** The content exclusion policy applies across all repositories. */
  | "all";
/**
 * Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsModifyRulesScope".
 */
/** @experimental */
export type PermissionsModifyRulesScope =
  /** Apply the rule change only to this session. */
  | "session"
  /** Persist the rule change for this project location. */
  | "location";
/**
 * Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllSource".
 */
/** @experimental */
export type PermissionsSetApproveAllSource =
  /** Allow-all was enabled from a CLI command-line flag. */
  | "cli_flag"
  /** Allow-all was enabled by a slash command. */
  | "slash_command"
  /** Allow-all was enabled by confirming autopilot behavior. */
  | "autopilot_confirmation"
  /** Allow-all was enabled at startup by the `defaultPermissionMode` user setting. */
  | "user_setting"
  /** Allow-all was enabled through an RPC caller. */
  | "rpc";
/**
 * Optional flags controlling which side effects the reload performs.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsReloadRequest".
 */
/** @experimental */
export type PluginsReloadRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * Reload MCP server connections after refreshing plugins. Defaults to true.
       */
      reloadMcp?: boolean;
      /**
       * Re-run custom-agent discovery after refreshing plugins. Defaults to true.
       */
      reloadCustomAgents?: boolean;
      /**
       * Re-load user, plugin, and (subject to `deferRepoHooks`) repo hooks. Defaults to true. Has no effect when the host has not registered a hook reloader (e.g. remote sessions).
       */
      reloadHooks?: boolean;
      /**
       * Re-discover and relaunch subprocess extensions (including plugin-shipped extensions) after refreshing plugins. Defaults to true. Has no effect when the session has no active extension controller (e.g. extensions were not requested for the session).
       */
      reloadExtensions?: boolean;
      /**
       * When true, skip repo-level hooks during the hook reload. Use before folder trust is confirmed; load them post-trust via `sessions.loadDeferredRepoHooks`.
       */
      deferRepoHooks?: boolean;
    };
/**
 * Controls whether the runtime may defer loading an external tool definition.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProtocolExternalToolDefer".
 */
/** @experimental */
export type ProtocolExternalToolDefer =
  /** The runtime may defer the tool according to its tool-loading policy. */
  | "auto"
  /** The runtime must include the tool without deferring it. */
  | "never";
/**
 * Provider family. Matches the `type` field of a BYOK provider config.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderEndpointType".
 */
/** @experimental */
export type ProviderEndpointType =
  /** OpenAI-compatible endpoint (use the OpenAI client library). */
  | "openai"
  /** Azure OpenAI endpoint (use the OpenAI client library with the Azure base URL). */
  | "azure"
  /** Anthropic endpoint (use the Anthropic client library). */
  | "anthropic";
/**
 * Wire API to be used, when required for the provider type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderEndpointWireApi".
 */
/** @experimental */
export type ProviderEndpointWireApi =
  /** Classic chat-completions request shape. */
  | "completions"
  /** Newer responses request shape. */
  | "responses";
/**
 * Transport to be used for provider requests.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderEndpointTransport".
 */
/** @experimental */
export type ProviderEndpointTransport =
  /** HTTP request/streaming transport. */
  | "http"
  /** WebSocket transport. */
  | "websockets";
/**
 * Optional model identifier to scope the endpoint snapshot to.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderGetEndpointRequest".
 */
/** @experimental */
export type ProviderGetEndpointRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * Model identifier the caller intends to use against the returned endpoint. Used to pick the correct wire shape. Omit to use whichever model the session is currently using.
       */
      modelId?: string;
    };
/**
 * Attachment union accepted by push input, covering files, directories, GitHub objects, blobs, snippets, and extension context.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachment".
 */
/** @experimental */
export type PushAttachment =
  | PushAttachmentFile
  | PushAttachmentDirectory
  | PushAttachmentSelection
  | PushAttachmentGitHubReference
  | PushAttachmentGitHubCommit
  | PushAttachmentGitHubRelease
  | PushAttachmentGitHubActionsJob
  | PushAttachmentGitHubRepository
  | PushAttachmentGitHubFileDiff
  | PushAttachmentGitHubTreeComparison
  | PushAttachmentGitHubUrl
  | PushAttachmentGitHubFile
  | PushAttachmentGitHubSnippet
  | PushAttachmentBlob
  | ExtensionContextPushInput;
/**
 * Type of GitHub reference
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubReferenceType".
 */
/** @experimental */
export type PushAttachmentGitHubReferenceType =
  /** GitHub issue reference. */
  | "issue"
  /** GitHub pull request reference. */
  | "pr"
  /** GitHub discussion reference. */
  | "discussion";
/**
 * The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAgentMode".
 */
/** @experimental */
export type SendAgentMode =
  /** The agent is responding interactively to the user. */
  | "interactive"
  /** The agent is preparing a plan before making changes. */
  | "plan"
  /** The agent is working autonomously toward task completion. */
  | "autopilot"
  /** The agent is in shell-focused UI mode. */
  | "shell";
/**
 * How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendMode".
 */
/** @experimental */
export type SendMode =
  /** Append the message to the normal session queue. */
  | "enqueue"
  /** Interject the message during the in-progress turn. */
  | "immediate";
/**
 * Whether this item is a queued user message or a queued slash command / model change
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuePendingItemsKind".
 */
/** @experimental */
export type QueuePendingItemsKind =
  /** A queued user message. */
  | "message"
  /** A queued slash command or model-change command. */
  | "command";
/**
 * State of the runtime-managed remote-control singleton.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStatus".
 */
/** @experimental */
export type RemoteControlStatus =
  | RemoteControlStatusOff
  | RemoteControlStatusConnecting
  | RemoteControlStatusActive
  | RemoteControlStatusError;
/**
 * Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionMode".
 */
/** @experimental */
export type RemoteSessionMode =
  /** Disable remote session export and steering. */
  | "off"
  /** Export session events to GitHub without enabling remote steering. */
  | "export"
  /** Enable both remote session export and remote steering. */
  | "on";
/**
 * What a remote host says one of its sessions is doing right now. Deliberately coarse: this is what a host can report for EVERY session in a catalogue listing, without a client subscribing to each one. AHP's `SessionSummary.status` is the source today; `input-needed` covers both a permission prompt and an `ask_user` question, since the summary does not say which.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionHostStatus".
 */
/** @experimental */
export type RemoteSessionHostStatus =
  /** No turn is running. */
  | "idle"
  /** A turn is running. */
  | "working"
  /** The session is blocked on the user: a permission prompt or an `ask_user` question. */
  | "input-needed"
  /** The session ended its last turn in an error. */
  | "error";
/**
 * Whether the remote task originated from CCA or CLI `--remote`.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionMetadataTaskType".
 */
/** @experimental */
export type RemoteSessionMetadataTaskType =
  /** GitHub Copilot coding agent task. */
  | "cca"
  /** CLI remote task. */
  | "cli";
/**
 * Origin of the sandbox choice supplied by an internal client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigSource".
 */
/** @experimental */
/** @internal */
export type SandboxConfigSource =
  /** The client applied the default because no sandbox preference was configured. */
  | "never_configured"
  /** The user's persisted settings enabled the sandbox. */
  | "user_enabled"
  /** The user's persisted settings disabled the sandbox. */
  | "user_disabled"
  /** A command-line flag selected the sandbox state for this session. */
  | "session_flag"
  /** The user disabled the sandbox for the current session. */
  | "session_disabled"
  /** The client disabled the sandbox because the host cannot enforce it. */
  | "unsupported_host"
  /** A repository policy selected the sandbox state. */
  | "repository_policy";
/**
 * Current authentication information, or null when no authentication is active.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionAuthInfoResult".
 */
/** @experimental */
export type SessionAuthInfoResult = AuthIdentity | null;
/**
 * Session capability enabled for this session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionCapability".
 */
/** @experimental */
export type SessionCapability =
  /** TUI-specific prompt hints such as keyboard shortcuts. */
  | "tui-hints"
  /** Plan-mode handling and instructions. */
  | "plan-mode"
  /** Memory tool and memories prompt section. */
  | "memory"
  /** Copilot CLI documentation tool and prompt section. */
  | "cli-documentation"
  /** Interactive ask_user tool support. */
  | "ask-user"
  /** Interactive CLI identity and behavior. */
  | "interactive-mode"
  /** Automatic hidden system notifications. */
  | "system-notifications"
  /** SDK elicitation support. */
  | "elicitation"
  /** Cross-session history tools and session-store SQL prompt/tool metadata. */
  | "session-store"
  /** MCP Apps UI passthrough. */
  | "mcp-apps"
  /** Host-provided canvas rendering support. */
  | "canvas-renderer";
/**
 * Error classification
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsErrorCode".
 */
/** @experimental */
export type SessionFsErrorCode =
  /** The requested path does not exist. */
  | "ENOENT"
  /** The filesystem operation failed for an unspecified reason. */
  | "UNKNOWN";
/**
 * Entry type
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesEntryType".
 */
/** @experimental */
export type SessionFsReaddirWithTypesEntryType =
  /** The entry is a file. */
  | "file"
  /** The entry is a directory. */
  | "directory";
/**
 * Path conventions used by this filesystem
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderConventions".
 */
/** @experimental */
export type SessionFsSetProviderConventions =
  /** Paths use Windows path conventions. */
  | "windows"
  /** Paths use POSIX path conventions. */
  | "posix";
/**
 * How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryType".
 */
/** @experimental */
export type SessionFsSqliteQueryType =
  /** Execute DDL or multi-statement SQL without returning rows. */
  | "exec"
  /** Execute a SELECT-style query and return rows. */
  | "query"
  /** Execute INSERT, UPDATE, or DELETE SQL and return affected-row metadata. */
  | "run";
/**
 * SQLite transaction failure classification.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteTransactionErrorClass".
 */
/** @experimental */
export type SessionFsSqliteTransactionErrorClass =
  /** SQLite reported BUSY or LOCKED before commit; the transaction was rolled back and may be retried. */
  | "busyOrLocked"
  /** The statement, database, or provider failed definitively and must not be retried automatically. */
  | "fatal"
  /** The transport failed after the provider may have committed; retrying could duplicate effects. */
  | "postCommitAmbiguous";
/**
 * Source descriptor for direct repo installs (when marketplace is empty)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSource".
 */
/** @experimental */
export type SessionInstalledPluginSource =
  | string
  | SessionInstalledPluginSourceGitHub
  | SessionInstalledPluginSourceUrl
  | SessionInstalledPluginSourceLocal;
/**
 * Client population used for the prediction baseline.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionClientType".
 */
/** @experimental */
export type SessionLimitPredictionClientType =
  /** Interactive CLI sessions where a user can accept, edit, or top up the limit. */
  | "cli-interactive"
  /** Prompt/non-interactive CLI sessions where the initial limit must cover more of the run. */
  | "cli-prompt";
/**
 * Baseline fallback level used to create the prediction.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionSource".
 */
/** @experimental */
export type SessionLimitPredictionSource =
  /** The prediction used the exact resolved model's baseline cell. */
  | "model"
  /** The exact model was unavailable, so the prediction used the model family's baseline cell. */
  | "family"
  /** No model or family cell was available, so the prediction used the global client-type baseline cell. */
  | "global";
/**
 * Semantic usage tier used for a recommended cap or additional headroom.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionTier".
 */
/** @experimental */
export type SessionLimitPredictionTier =
  /** Recommended starting tier. */
  | "recommended"
  /** Additional headroom for longer-running sessions. */
  | "additional_headroom"
  /** Generous headroom for unusually high usage. */
  | "generous_headroom"
  /** Maximum available headroom tier. */
  | "maximum_headroom";
/**
 * Parameters for predicting an AI-credit session limit. Omitting `modelId` uses the session's currently selected model.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionRequest".
 */
/** @experimental */
export type SessionLimitPredictionRequest =
  | {
      [k: string]: unknown | undefined;
    }
  | {
      /**
       * Optional model identifier override. If omitted, the session's current model is used.
       */
      modelId?: string;
      clientType?: SessionLimitPredictionClientType;
    };
/**
 * Prediction result. Available results include prediction details; unavailable results include an explicit reason.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionResult".
 */
/** @experimental */
export type SessionLimitPredictionResult =
  | {
      prediction: SessionLimitPredictionDetails;
      /**
       * Prediction result variant discriminator.
       */
      kind: "available";
    }
  | {
      reason: SessionLimitPredictionUnavailableReason;
      /**
       * Prediction result variant discriminator.
       */
      kind: "unavailable";
    };
/**
 * Reason a prediction could not be computed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionUnavailableReason".
 */
/** @experimental */
export type SessionLimitPredictionUnavailableReason =
  /** The current model is auto and has not resolved to a concrete model yet. */
  | "auto_unresolved"
  /** No model was provided and the session does not currently have a selected model. */
  | "no_model";
/**
 * Local or remote session metadata entry. Narrow on `isRemote` to access source-specific fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionListEntry".
 */
/** @experimental */
export type SessionListEntry = LocalSessionMetadataValue | RemoteSessionMetadataValue;
/**
 * Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceSummary".
 */
/** @experimental */
export type WorkspaceSummary = {
  /**
   * Workspace identifier (1:1 with sessionId)
   */
  id: string;
  /**
   * Current working directory at session start
   */
  cwd?: string;
  /**
   * Resolved git root for cwd, if any
   */
  git_root?: string;
  /**
   * Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
   */
  repository?: string;
  host_type?: WorkspaceSummaryHostType;
  /**
   * Branch checked out at session start, if any
   */
  branch?: string;
  /**
   * Display name for the session, if set
   */
  name?: string;
  /**
   * Whether the display name was explicitly set by the user
   */
  user_named?: boolean;
  /**
   * ISO 8601 timestamp when the workspace was created
   */
  created_at?: string;
  /**
   * ISO 8601 timestamp when the workspace was last updated
   */
  updated_at?: string;
} | null;
/**
 * Repository host type, if known
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceSummaryHostType".
 */
/** @experimental */
export type WorkspaceSummaryHostType =
  /** Workspace summary repository is hosted on GitHub. */
  | "github"
  /** Workspace summary repository is hosted on Azure DevOps. */
  | "ado";
/**
 * Initial reasoning summary mode for supported model clients.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptionsReasoningSummary".
 */
/** @experimental */
export type SessionOpenOptionsReasoningSummary =
  /** Do not request reasoning summaries from the model. */
  | "none"
  /** Request a concise summary of model reasoning. */
  | "concise"
  /** Request a detailed summary of model reasoning. */
  | "detailed";
/**
 * Controls automatic non-interactive profile loading where supported. Explicit initScripts are unaffected.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellInitProfile".
 */
/** @experimental */
export type ShellInitProfile =
  /** Disable automatic non-interactive profile loading. Explicit initScripts still run. */
  | "none"
  /** Allow automatic non-interactive profile loading when supported. Explicit initScripts still run. */
  | "non-interactive";
/**
 * Supported built-in shells for initialization scripts.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellInitScriptShell".
 */
/** @experimental */
export type ShellInitScriptShell =
  /** Source the script in the built-in Bash shell on macOS and Linux. */
  | "bash"
  /** Source the script in the built-in PowerShell shell on Windows. */
  | "powershell";
/**
 * How MCP server environment values are interpreted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptionsEnvValueMode".
 */
/** @experimental */
export type SessionOpenOptionsEnvValueMode =
  /** Pass MCP server environment values as literal strings. */
  | "direct"
  /** Resolve MCP server environment values from host-side references. */
  | "indirect";
/**
 * Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptionsAdditionalContentExclusionPolicyScope".
 */
/** @experimental */
export type SessionOpenOptionsAdditionalContentExclusionPolicyScope =
  /** The content exclusion policy applies to the current repository. */
  | "repo"
  /** The content exclusion policy applies across all repositories. */
  | "all";
/**
 * Open a session by creating, resuming, attaching, connecting to a remote, or handing off.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenParams".
 */
/** @experimental */
export type SessionOpenParams =
  | SessionsOpenCreate
  | SessionsOpenResume
  | SessionsOpenResumeLast
  | SessionsOpenAttach
  | SessionsOpenRemote
  | SessionsOpenCloud
  | SessionsOpenHandoff;
/**
 * Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenHandoffTaskType".
 */
/** @experimental */
export type SessionsOpenHandoffTaskType =
  /** GitHub Copilot coding agent task. */
  | "cca"
  /** CLI remote task. */
  | "cli";
/**
 * Outcome of the open request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenStatus".
 */
/** @experimental */
export type SessionsOpenStatus =
  /** A new session was created. */
  | "created"
  /** An existing session was loaded or reattached. */
  | "resumed"
  /** No matching persisted session was found. */
  | "not_found"
  /** Connected to an existing remote session. */
  | "connected"
  /** Remote session was handed off to a new local session. */
  | "handed_off";
/**
 * Handoff step.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenProgressStep".
 */
/** @experimental */
export type SessionsOpenProgressStep =
  /** Loading the source session's events from the remote service. */
  | "load-session"
  /** Validating that the local repository matches the remote session's repository. */
  | "validate-repo"
  /** Checking the local working tree for uncommitted changes that would block the handoff. */
  | "check-changes"
  /** Checking out the branch associated with the remote session in the local working tree. */
  | "checkout-branch"
  /** Creating the new local session and seeding it with the source session's events. */
  | "create-session"
  /** Persisting the newly-created local session to disk. */
  | "save-session";
/**
 * Step status.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenProgressStatus".
 */
/** @experimental */
export type SessionsOpenProgressStatus =
  /** The step has started and has not yet finished. */
  | "in-progress"
  /** The step has completed successfully. */
  | "complete";
/**
 * Authentication credentials accepted by session.gitHubAuth.setCredentials. Session-owned token-provider identities cannot be installed through this method.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SettableAuthInfo".
 */
/** @experimental */
export type SettableAuthInfo =
  | HMACAuthInfo
  | EnvAuthInfo
  | SettableTokenAuthInfo
  | CopilotApiTokenAuthInfo
  | UserAuthInfo
  | GhCliAuthInfo
  | ApiKeyAuthInfo;
/**
 * Rust-owned settings predicates exposed across the SDK boundary. Raw feature-flag names are intentionally not part of the contract.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsPredicateName".
 */
/** @experimental */
export type SessionSettingsPredicateName =
  /** Whether the security-tools feature flag enables security tool wiring. */
  | "securityToolsEnabled"
  /** Whether third-party security tools should receive the security prompt. */
  | "thirdPartySecurityPromptEnabled"
  /** Whether validation may run in parallel. */
  | "parallelValidationEnabled"
  /** Whether runtime timing telemetry is enabled. */
  | "runtimeTimingTelemetryEnabled"
  /** Whether the co-author hook is enabled. */
  | "coAuthorHookEnabled"
  /** Whether Chronicle integration is enabled. */
  | "chronicleEnabled"
  /** Whether content-exclusion policy may self-fetch data. */
  | "contentExclusionSelfFetchEnabled"
  /** Whether Claude Opus token-limit caps should be applied. */
  | "capClaudeOpusTokenLimitsEnabled"
  /** Whether code-review behavior is enabled. */
  | "codeReviewFeatureEnabled"
  /** Whether CCA should use the TypeScript autofind behavior. */
  | "ccaUseTsAutofindEnabled"
  /** Whether the dependency checker is enabled. */
  | "dependencyCheckerEnabled"
  /** Whether the Dependabot checker is enabled. */
  | "dependabotCheckerEnabled"
  /** Whether the CodeQL checker is enabled. */
  | "codeqlCheckerEnabled"
  /** Whether trivial-change handling is enabled. */
  | "trivialChangeEnabled"
  /** Whether trivial-change skip behavior is enabled. */
  | "trivialChangeSkipEnabled"
  /** Whether trivial-change handling is enabled for code review. */
  | "trivialChangeEnabledForCodeReview"
  /** Whether trivial-change skip behavior is enabled for code review. */
  | "trivialChangeSkipEnabledForCodeReview"
  /** Whether trivial-change handling is enabled for a specific tool. */
  | "trivialChangeEnabledForTool"
  /** Whether trivial-change skip behavior is enabled for a specific tool. */
  | "trivialChangeSkipEnabledForTool";
/**
 * Which session sources to include. Defaults to `local` for backward compatibility.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSource".
 */
/** @experimental */
export type SessionSource =
  /** Return only local sessions. */
  | "local"
  /** Return only remote sessions. */
  | "remote"
  /** Return both local and remote sessions. */
  | "all";
/**
 * Sharing status for a synced session. "repo" makes the session visible to anyone with read access to the repository; "unshared" restricts it to the creator and collaborators.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionVisibilityStatus".
 */
/** @experimental */
export type SessionVisibilityStatus =
  /** The session is visible to repository readers. */
  | "repo"
  /** The session is restricted to its creator and collaborators. */
  | "unshared";
/**
 * Signal to send (default: SIGTERM)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillSignal".
 */
/** @experimental */
export type ShellKillSignal =
  /** Request graceful process termination. */
  | "SIGTERM"
  /** Forcefully terminate the process. */
  | "SIGKILL"
  /** Send an interrupt signal to the process. */
  | "SIGINT";
/**
 * Which tier this directory belongs to
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillDiscoveryScope".
 */
/** @experimental */
export type SkillDiscoveryScope =
  /** A project's repository skill directory. */
  | "project"
  /** The user's personal Copilot skill directory. */
  | "personal-copilot"
  /** The user's personal agents skill directory. */
  | "personal-agents"
  /** A configured custom skill directory. */
  | "custom";
/**
 * Result of invoking the slash command (text output, prompt to send to the agent, completion, or subcommand selection).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInvocationResult".
 */
/** @experimental */
export type SlashCommandInvocationResult =
  | SlashCommandTextResult
  | SlashCommandAgentPromptResult
  | SlashCommandCompletedResult
  | SlashCommandSelectSubcommandResult
  | SlashCommandAddTimelineEntryResult
  | SlashCommandShowDialogResult
  | SlashCommandSetModelResult
  | SlashCommandSetPlanModelResult;
/**
 * Subagent settings to apply, or null to clear the live session override
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SubagentSettings".
 */
/** @experimental */
export type SubagentSettings = {
  /**
   * Per-agent settings keyed by subagent agent_type
   */
  agents?: {
    [k: string]: SubagentSettingsEntry | undefined;
  };
  /**
   * Names of subagents the user has turned off; they cannot be dispatched
   */
  disabledSubagents?: string[];
  /**
   * Maximum number of subagents that can run concurrently; applies to usage-based billing users only
   */
  maxConcurrency?: number;
  /**
   * Maximum subagent nesting depth; applies to usage-based billing users only
   */
  maxDepth?: number;
} | null;
/**
 * Context tier override for matching subagents
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SubagentSettingsEntryContextTier".
 */
/** @experimental */
export type SubagentSettingsEntryContextTier =
  /** Inherit the parent session's effective context tier at dispatch time. */
  | "inherit"
  /** Use the model's default context window. */
  | "default"
  /** Pin the subagent to the long-context tier when supported. */
  | "long_context";
/**
 * Current lifecycle status of the task
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskStatus".
 */
/** @experimental */
export type TaskStatus =
  /** The task is actively executing. */
  | "running"
  /** The task is waiting for additional input. */
  | "idle"
  /** The task finished successfully. */
  | "completed"
  /** The task finished with an error. */
  | "failed"
  /** The task was cancelled before completion. */
  | "cancelled";
/**
 * Whether task execution is synchronously awaited or managed in the background
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskExecutionMode".
 */
/** @experimental */
export type TaskExecutionMode =
  /** The task was started with synchronous waiting. */
  | "sync"
  /** The task is managed in the background. */
  | "background";
/**
 * Tracked task union returned by task APIs, containing either an agent task or a shell task.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskInfo".
 */
/** @experimental */
export type TaskInfo = TaskAgentInfo | TaskShellInfo;
/**
 * Whether the shell runs inside a managed PTY session or as an independent background process
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfoAttachmentMode".
 */
/** @experimental */
export type TaskShellInfoAttachmentMode =
  /** The shell runs in a managed PTY session. */
  | "attached"
  /** The shell runs as an independent background process. */
  | "detached";
/**
 * Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskProgress".
 */
/** @experimental */
export type TaskProgress = (TaskAgentProgress | TaskShellProgress) | null;
/**
 * Canonical result returned by a session tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolResult".
 */
/** @experimental */
export type ToolResult = string | ToolResultExpanded;
/**
 * Execution outcome classification.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolResultType".
 */
/** @experimental */
export type ToolResultType =
  /** The tool completed successfully. */
  | "success"
  /** The tool failed. */
  | "failure"
  /** The tool exceeded its execution timeout. */
  | "timeout"
  /** The tool request was rejected before execution. */
  | "rejected"
  /** Permission policy denied the tool request. */
  | "denied";
/**
 * User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIAutoModeSwitchResponse".
 */
/** @experimental */
export type UIAutoModeSwitchResponse =
  /** Allow the automatic mode switch for this turn. */
  | "yes"
  /** Allow this mode switch and persist the preference. */
  | "yes_always"
  /** Decline the automatic mode switch. */
  | "no";
/**
 * Submitted UI elicitation field value: string, number, boolean, or an array of strings.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationFieldValue".
 */
/** @experimental */
export type UIElicitationFieldValue = string | number | boolean | string[];
/**
 * Definition for a single elicitation form field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaProperty".
 */
/** @experimental */
export type UIElicitationSchemaProperty =
  | (
      | UIElicitationStringEnumField
      | UIElicitationStringOneOfField
      | UIElicitationArrayEnumField
      | UIElicitationArrayAnyOfField
      | UIElicitationSchemaPropertyBoolean
      | UIElicitationSchemaPropertyString
      | UIElicitationSchemaPropertyNumber
    )
  | undefined;
/**
 * Optional format hint that constrains the accepted input.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyStringFormat".
 */
/** @experimental */
export type UIElicitationSchemaPropertyStringFormat =
  /** Email address string format. */
  | "email"
  /** URI string format. */
  | "uri"
  /** Calendar date string format. */
  | "date"
  /** Date-time string format. */
  | "date-time";
/**
 * Numeric type accepted by the field.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyNumberType".
 */
/** @experimental */
export type UIElicitationSchemaPropertyNumberType =
  /** Any JSON number. */
  | "number"
  /** Integer JSON number. */
  | "integer";
/**
 * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponseAction".
 */
/** @experimental */
export type UIElicitationResponseAction =
  /** The user submitted the requested form values. */
  | "accept"
  /** The user explicitly declined to provide the requested input. */
  | "decline"
  /** The user dismissed the elicitation request. */
  | "cancel";
/**
 * The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIExitPlanModeAction".
 */
/** @experimental */
export type UIExitPlanModeAction =
  /** Exit plan mode without starting implementation. */
  | "exit_only"
  /** Exit plan mode and continue interactively. */
  | "interactive"
  /** Exit plan mode and continue in autopilot mode. */
  | "autopilot"
  /** Exit plan mode and continue in autopilot mode with parallel subagent execution. */
  | "autopilot_fleet";
/**
 * User action selected for an exhausted session limit.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UISessionLimitsExhaustedResponseAction".
 */
/** @experimental */
export type UISessionLimitsExhaustedResponseAction =
  /** Increase the current max by an exact AI Credits amount. */
  | "add"
  /** Set a new absolute max AI Credits value. */
  | "set"
  /** Remove the current session limit. */
  | "unset"
  /** Leave the limit unchanged and cancel the blocked model request. */
  | "cancel";
/**
 * Type of change represented by this file diff.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceDiffFileChangeType".
 */
/** @experimental */
export type WorkspaceDiffFileChangeType =
  /** The file was added. */
  | "added"
  /** The file was modified. */
  | "modified"
  /** The file was deleted. */
  | "deleted"
  /** The file was renamed. */
  | "renamed";
/**
 * Diff mode requested by the client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceDiffMode".
 */
/** @experimental */
export type WorkspaceDiffMode =
  /** Return staged, unstaged, and untracked working tree changes. */
  | "unstaged"
  /** Return changes compared with the default branch. */
  | "branch"
  /** Return the cumulative diff of files Copilot changed this session (used in non-git workspaces). */
  | "session";
/**
 * Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesWorkspaceDetailsHostType".
 */
/** @experimental */
export type WorkspacesWorkspaceDetailsHostType =
  /** Workspace repository is hosted on GitHub. */
  | "github"
  /** Workspace repository is hosted on Azure DevOps. */
  | "ado";
/**
 * List of all authenticated users
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountGetAllUsersResult".
 */
/** @experimental */
export type AccountGetAllUsersResult = AccountAllUsers[];
/**
 * The number of running background agents (task-registry agents) that were cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionCancelAllBackgroundAgentsResult".
 */
/** @experimental */
export type SessionCancelAllBackgroundAgentsResult = number;
/**
 * Authentication accounts available to the internal session host.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionGitHubAuthGetAllAuthAvailableResult".
 */
/** @experimental */
export type SessionGitHubAuthGetAllAuthAvailableResult = SessionAuthStatus[];
/**
 * Whether the current authentication was logged out.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionGitHubAuthLogoutResult".
 */
/** @experimental */
export type SessionGitHubAuthLogoutResult = boolean;
/**
 * Whether the requested authentication was logged out.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionGitHubAuthLogoutUserResult".
 */
/** @experimental */
export type SessionGitHubAuthLogoutUserResult = boolean;

/**
 * Parameters for aborting the current turn
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AbortRequest".
 */
/** @experimental */
export interface AbortRequest {
  reason?: AbortReason;
}
/**
 * Result of aborting the current turn
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AbortResult".
 */
/** @experimental */
export interface AbortResult {
  /**
   * Whether the abort completed successfully
   */
  success: boolean;
  /**
   * Error message if the abort failed
   */
  error?: string;
}
/**
 * Authenticated account entry returned by `account.getAllUsers`.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountAllUsers".
 */
/** @experimental */
export interface AccountAllUsers {
  authInfo: AuthInfo;
  /**
   * Opaque identifier accepted by account and model selection APIs
   */
  selectionId?: string;
  /**
   * Associated token, if available
   */
  token?: string;
}
/**
 * Authentication-info input variant for GitHub-internal HMAC auth, carrying the public GitHub host and HMAC secret.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HMACAuthInfo".
 */
/** @experimental */
export interface HMACAuthInfo {
  /**
   * HMAC-based authentication used by GitHub-internal services.
   */
  type: "hmac";
  /**
   * Authentication host. HMAC auth always targets the public GitHub host.
   */
  host: "https://github.com";
  /**
   * HMAC secret used to sign requests.
   */
  hmac: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponse".
 */
/** @experimental */
export interface CopilotUserResponse {
  /**
   * GitHub login of the authenticated user.
   */
  login?: string;
  /**
   * Copilot access SKU identifier (e.g. `free_limited_copilot`, `copilot_for_business_seat_quota`) used to gate model and feature access.
   */
  access_type_sku?: string;
  /**
   * Opaque analytics tracking identifier for the user, forwarded from the Copilot API.
   */
  analytics_tracking_id?: string;
  /**
   * Date the Copilot seat was assigned to the user, if applicable.
   */
  assigned_date?:
    | (
        | {
            [k: string]: unknown | undefined;
          }
        | string
      )
    | null;
  /**
   * Whether the user is eligible to sign up for the free/limited Copilot tier.
   */
  can_signup_for_limited?: boolean;
  /**
   * Whether Copilot chat is enabled for the user.
   */
  chat_enabled?: boolean;
  /**
   * Copilot plan name for the user (e.g. `individual`, `business`, `enterprise`).
   */
  copilot_plan?: string;
  /**
   * Whether `.copilotignore` content-exclusion support is enabled for the user.
   */
  copilotignore_enabled?: boolean;
  endpoints?: CopilotUserResponseEndpoints;
  /**
   * Logins of the organizations the user belongs to.
   */
  organization_login_list?: string[];
  /**
   * Organizations the user belongs to, each with an optional login and display name.
   */
  organization_list?:
    | (
        | {
            [k: string]: unknown | undefined;
          }
        | ({
            /**
             * GitHub login of the organization.
             */
            login?:
              | (
                  | {
                      [k: string]: unknown | undefined;
                    }
                  | string
                )
              | null;
            /**
             * Display name of the organization.
             */
            name?:
              | (
                  | {
                      [k: string]: unknown | undefined;
                    }
                  | string
                )
              | null;
          } | null)[]
      )
    | null;
  /**
   * Whether the Codex agent is enabled for the user.
   */
  codex_agent_enabled?: boolean;
  /**
   * Whether MCP (Model Context Protocol) support is enabled for the user.
   */
  is_mcp_enabled?:
    | (
        | {
            [k: string]: unknown | undefined;
          }
        | boolean
      )
    | null;
  /**
   * Date the user's usage quota next resets, as a raw string from the Copilot API; see `quota_reset_date_utc` for the UTC-normalized value.
   */
  quota_reset_date?: string;
  quota_snapshots?: CopilotUserResponseQuotaSnapshots;
  /**
   * Whether the user's telemetry is subject to restricted-data handling.
   */
  restricted_telemetry?: boolean;
  /**
   * Whether the user is a GitHub/Microsoft staff member.
   */
  is_staff?: boolean;
  /**
   * Raw passthrough of the Copilot API `te` flag for the user (an opaque server-side eligibility signal surfaced in telemetry); not otherwise interpreted by the runtime.
   */
  te?: boolean;
  /**
   * Whether the account is on usage-based (token/AI-credit) billing rather than a fixed premium-request quota.
   */
  token_based_billing?: boolean;
  /**
   * Whether the user is able to upgrade their Copilot plan.
   */
  can_upgrade_plan?: boolean;
  /**
   * UTC-normalized form of `quota_reset_date` (the date the user's usage quota next resets).
   */
  quota_reset_date_utc?: string;
  /**
   * Per-category quota allotments for free/limited-tier users, keyed by quota category.
   */
  limited_user_quotas?: {
    [k: string]: number | undefined;
  };
  /**
   * Date the free/limited-tier user's quotas next reset, as a raw string from the Copilot API.
   */
  limited_user_reset_date?: string;
  /**
   * Per-category monthly quota allotments, keyed by quota category.
   */
  monthly_quotas?: {
    [k: string]: number | undefined;
  };
  /**
   * Whether cloud session storage is enabled for the user.
   */
  cloud_session_storage_enabled?: boolean;
  /**
   * Whether CLI remote control is enabled for the user.
   */
  cli_remote_control_enabled?: boolean;
}
/**
 * Endpoint URLs from the raw Copilot `/copilot_internal/v2/token` user-response passthrough.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseEndpoints".
 */
/** @experimental */
export interface CopilotUserResponseEndpoints {
  /**
   * Copilot API endpoint URL.
   */
  api?: string;
  /**
   * Origin-tracker endpoint URL.
   */
  "origin-tracker"?: string;
  /**
   * Copilot proxy endpoint URL.
   */
  proxy?: string;
  /**
   * Copilot telemetry endpoint URL.
   */
  telemetry?: string;
  /**
   * Experimental-service endpoint URL.
   */
  exp?: string;
}
/**
 * Quota snapshot map from the raw Copilot user-response passthrough, with chat, completions, premium-interactions, and other entries.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshots".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshots {
  chat?: CopilotUserResponseQuotaSnapshotsChat;
  completions?: CopilotUserResponseQuotaSnapshotsCompletions;
  premium_interactions?: CopilotUserResponseQuotaSnapshotsPremiumInteractions;
  [k: string]:
    | ({
        entitlement?: number;
        overage_count?: number;
        overage_permitted?: boolean;
        percent_remaining?: number;
        quota_id?: string;
        quota_remaining?: number;
        remaining?: number;
        unlimited?: boolean;
        timestamp_utc?: string;
        has_quota?: boolean;
        quota_reset_at?: number;
        token_based_billing?: boolean;
      } | null)
    | undefined;
}
/**
 * Chat quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshotsChat".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshotsChat {
  /**
   * Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement.
   */
  entitlement?: number;
  /**
   * Count of additional pay-per-request usage consumed this period beyond the entitlement.
   */
  overage_count?: number;
  /**
   * Whether usage may continue at pay-per-request rates once the entitlement is exhausted.
   */
  overage_permitted?: boolean;
  /**
   * Percentage of the entitlement remaining at the snapshot timestamp.
   */
  percent_remaining?: number;
  /**
   * Identifier of the quota bucket this snapshot describes.
   */
  quota_id?: string;
  /**
   * Amount of quota remaining at the snapshot timestamp.
   */
  quota_remaining?: number;
  /**
   * Remaining entitlement/quota amount at the snapshot timestamp.
   */
  remaining?: number;
  /**
   * Whether the entitlement for this category is unlimited.
   */
  unlimited?: boolean;
  /**
   * UTC timestamp when this snapshot was captured.
   */
  timestamp_utc?: string;
  /**
   * Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets.
   */
  has_quota?: boolean;
  /**
   * Unix epoch time, in seconds, when this quota next resets.
   */
  quota_reset_at?: number;
  /**
   * Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count.
   */
  token_based_billing?: boolean;
}
/**
 * Completions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshotsCompletions".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshotsCompletions {
  /**
   * Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement.
   */
  entitlement?: number;
  /**
   * Count of additional pay-per-request usage consumed this period beyond the entitlement.
   */
  overage_count?: number;
  /**
   * Whether usage may continue at pay-per-request rates once the entitlement is exhausted.
   */
  overage_permitted?: boolean;
  /**
   * Percentage of the entitlement remaining at the snapshot timestamp.
   */
  percent_remaining?: number;
  /**
   * Identifier of the quota bucket this snapshot describes.
   */
  quota_id?: string;
  /**
   * Amount of quota remaining at the snapshot timestamp.
   */
  quota_remaining?: number;
  /**
   * Remaining entitlement/quota amount at the snapshot timestamp.
   */
  remaining?: number;
  /**
   * Whether the entitlement for this category is unlimited.
   */
  unlimited?: boolean;
  /**
   * UTC timestamp when this snapshot was captured.
   */
  timestamp_utc?: string;
  /**
   * Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets.
   */
  has_quota?: boolean;
  /**
   * Unix epoch time, in seconds, when this quota next resets.
   */
  quota_reset_at?: number;
  /**
   * Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count.
   */
  token_based_billing?: boolean;
}
/**
 * Premium-interactions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotUserResponseQuotaSnapshotsPremiumInteractions".
 */
/** @experimental */
export interface CopilotUserResponseQuotaSnapshotsPremiumInteractions {
  /**
   * Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement.
   */
  entitlement?: number;
  /**
   * Count of additional pay-per-request usage consumed this period beyond the entitlement.
   */
  overage_count?: number;
  /**
   * Whether usage may continue at pay-per-request rates once the entitlement is exhausted.
   */
  overage_permitted?: boolean;
  /**
   * Percentage of the entitlement remaining at the snapshot timestamp.
   */
  percent_remaining?: number;
  /**
   * Identifier of the quota bucket this snapshot describes.
   */
  quota_id?: string;
  /**
   * Amount of quota remaining at the snapshot timestamp.
   */
  quota_remaining?: number;
  /**
   * Remaining entitlement/quota amount at the snapshot timestamp.
   */
  remaining?: number;
  /**
   * Whether the entitlement for this category is unlimited.
   */
  unlimited?: boolean;
  /**
   * UTC timestamp when this snapshot was captured.
   */
  timestamp_utc?: string;
  /**
   * Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets.
   */
  has_quota?: boolean;
  /**
   * Unix epoch time, in seconds, when this quota next resets.
   */
  quota_reset_at?: number;
  /**
   * Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count.
   */
  token_based_billing?: boolean;
}
/**
 * Authentication-info input variant for a token sourced from an environment variable, with host, optional login, token, and env var name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EnvAuthInfo".
 */
/** @experimental */
export interface EnvAuthInfo {
  /**
   * Personal access token (PAT) or server-to-server token sourced from an environment variable.
   */
  type: "env";
  /**
   * Authentication host (e.g. https://github.com or a GHES host).
   */
  host: string;
  /**
   * User login associated with the token. Undefined for server-to-server tokens (those starting with `ghs_`).
   */
  login?: string;
  /**
   * The token value itself. Treat as a secret.
   */
  token: string;
  /**
   * Name of the environment variable the token was sourced from.
   */
  envVar: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Authentication-info input variant for SDK-configured token authentication, carrying host and the secret token value.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TokenAuthInfo".
 */
/** @experimental */
export interface TokenAuthInfo {
  /**
   * SDK-side token authentication; the host configured the token directly via the SDK.
   */
  type: "token";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * The token value itself. Treat as a secret.
   */
  token: string;
  /**
   * Opaque native GitHub credential registration backing this token identity, when applicable.
   */
  registrationId?: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Authentication-info variant backed by an SDK GitHub token callback. It carries routing metadata but never a plaintext token.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TokenProviderAuthInfo".
 */
/** @experimental */
export interface TokenProviderAuthInfo {
  /**
   * SDK callback-backed GitHub token authentication.
   */
  type: "token-provider";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * Opaque SDK callback registration identifier.
   */
  registrationId: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Authentication-info variant for direct Copilot API token auth sourced from environment variables, with public GitHub host.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CopilotApiTokenAuthInfo".
 */
/** @experimental */
export interface CopilotApiTokenAuthInfo {
  /**
   * Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL` environment-variable pair. The token itself is read from the environment by the runtime, not carried in this struct.
   */
  type: "copilot-api-token";
  /**
   * Authentication host (always the public GitHub host).
   */
  host: "https://github.com";
  copilotUser?: CopilotUserResponse;
}
/**
 * Authentication-info variant for OAuth user auth, with host and login; the token remains in the runtime secret store.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserAuthInfo".
 */
/** @experimental */
export interface UserAuthInfo {
  /**
   * OAuth user authentication. The token itself is held in the runtime's secret token store (keyed by host+login) and is NOT carried in this struct.
   */
  type: "user";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * OAuth user login.
   */
  login: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Authentication-info input variant for GitHub CLI credentials, carrying host, login, and the `gh auth token` value.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GhCliAuthInfo".
 */
/** @experimental */
export interface GhCliAuthInfo {
  /**
   * Authentication via the `gh` CLI's saved credentials.
   */
  type: "gh-cli";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * User login as reported by `gh auth status`.
   */
  login: string;
  /**
   * The token returned by `gh auth token`. Treat as a secret.
   */
  token: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Authentication-info input variant for API-key authentication to a non-GitHub LLM provider, carrying the secret `apiKey` and host.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ApiKeyAuthInfo".
 */
/** @experimental */
export interface ApiKeyAuthInfo {
  /**
   * API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style).
   */
  type: "api-key";
  /**
   * The API key. Treat as a secret.
   */
  apiKey: string;
  /**
   * Authentication host.
   */
  host: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Current authentication state
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountGetCurrentAuthResult".
 */
/** @experimental */
export interface AccountGetCurrentAuthResult {
  authInfo?: AuthInfo;
  /**
   * Authentication errors from the last auth attempt, if any
   */
  authErrors?: string[];
}

/** @experimental */
export interface AccountGetQuotaRequest {
  /**
   * Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used.
   */
  selectionId?: string;
  /**
   * GitHub token accepted for compatibility with existing SDK clients. When provided, resolves this token instead of using the current account.
   */
  gitHubToken?: string;
}
/**
 * Quota usage snapshots for the resolved user, keyed by quota type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountGetQuotaResult".
 */
/** @experimental */
export interface AccountGetQuotaResult {
  /**
   * Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
   */
  quotaSnapshots: {
    [k: string]: AccountQuotaSnapshot | undefined;
  };
}
/**
 * Quota usage snapshot for a Copilot quota type, including entitlement, used requests, overage, reset date, and remaining percentage.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountQuotaSnapshot".
 */
/** @experimental */
export interface AccountQuotaSnapshot {
  /**
   * Whether the user has an unlimited usage entitlement
   */
  isUnlimitedEntitlement: boolean;
  /**
   * Number of requests included in the entitlement, or -1 for unlimited entitlements
   */
  entitlementRequests: number;
  /**
   * Number of requests used so far this period
   */
  usedRequests: number;
  /**
   * Whether usage is still permitted after quota exhaustion
   */
  usageAllowedWithExhaustedQuota: boolean;
  /**
   * Percentage of entitlement remaining
   */
  remainingPercentage: number;
  /**
   * Number of additional usage requests made this period
   */
  overage: number;
  /**
   * Whether additional usage is allowed when quota is exhausted
   */
  overageAllowedWithExhaustedQuota: boolean;
  /**
   * Date when the quota resets (ISO 8601 string)
   */
  resetDate?: string;
}
/**
 * Credentials to validate and store. Omit login to resolve the authenticated user from the token.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountLoginRequest".
 */
/** @experimental */
export interface AccountLoginRequest {
  /**
   * GitHub host URL
   */
  host: string;
  /**
   * User login/username. When omitted, the runtime validates the token and resolves the login from GitHub.
   */
  login?: string;
  /**
   * GitHub authentication token
   */
  token: string;
}
/**
 * Result of a successful login; throws on failure
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountLoginResult".
 */
/** @experimental */
export interface AccountLoginResult {
  /**
   * Whether the credential was persisted to a secure store (system keychain, or the config file when plaintext storage is enabled). False when no secure store was available and the token was not saved, so the consumer can decide how to proceed.
   */
  storedInVault: boolean;
}
/**
 * Logout result indicating if more users remain
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AccountLogoutResult".
 */
/** @experimental */
export interface AccountLogoutResult {
  /**
   * Whether other authenticated users remain after logout
   */
  hasMoreUsers: boolean;
}
/**
 * Canonical directory where custom agents can be discovered or created, with scope, preference, and optional project path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentDiscoveryPath".
 */
/** @experimental */
export interface AgentDiscoveryPath {
  /**
   * Absolute path of the search/create directory (may not exist on disk yet)
   */
  path: string;
  scope: AgentDiscoveryPathScope;
  /**
   * Whether this is the canonical directory to create a new agent in its tier. At most one entry per tier is preferred.
   */
  preferredForCreation: boolean;
  /**
   * The input project path this directory was derived from (only for project scope)
   */
  projectPath?: string;
}
/**
 * Canonical locations where custom agents can be created so the runtime will recognize them.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentDiscoveryPathList".
 */
/** @experimental */
export interface AgentDiscoveryPathList {
  /**
   * Canonical agent create/discovery directories, in priority order
   */
  paths: AgentDiscoveryPath[];
}
/**
 * The currently selected custom agent, or null when using the default agent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentGetCurrentResult".
 */
/** @experimental */
export interface AgentGetCurrentResult {
  /**
   * Currently selected custom agent, or null if using the default agent
   */
  agent?: AgentInfo | null;
}
/**
 * Agent metadata, including identifiers, display details, source, tools, model, models, MCP servers, skills, and file path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentInfo".
 */
/** @experimental */
export interface AgentInfo {
  /**
   * Name of the agent. Use `id` as the stable selection identifier.
   */
  name: string;
  /**
   * Human-readable display name
   */
  displayName: string;
  /**
   * Description of the agent's purpose
   */
  description: string;
  /**
   * Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.
   */
  path?: string;
  /**
   * Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned.
   */
  id: string;
  source?: AgentInfoSource;
  /**
   * Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only.
   */
  userInvocable?: boolean;
  /**
   * Allowed tool names for this agent. Empty array means none; omitted means inherit defaults.
   */
  tools?: string[];
  /**
   * Authored preferred model id for this agent. Runtime model selection may choose a different model; omitted means no authored preference.
   */
  model?: string;
  /**
   * Authored preferred model ids for this agent, in priority order. Runtime model selection chooses the first available model; omitted means no authored preference.
   */
  models?: string[];
  modelPolicy?: AgentModelPolicy;
  /**
   * MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema.
   *
   * @experimental
   */
  mcpServers?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Skill names preloaded into this agent's context. Omitted means none.
   */
  skills?: string[];
  /**
   * Authored base prompt for the agent. Runtime prompt assembly may add dynamic context at invocation time. Omitted from `session.agent.list` unless `includePrompt` is true.
   */
  prompt?: string;
}
/**
 * Agents available to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentList".
 */
/** @experimental */
export interface AgentList {
  /**
   * Available agents
   */
  agents: AgentInfo[];
}
/**
 * Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLiveTargetEntry".
 */
/** @experimental */
export interface AgentRegistryLiveTargetEntry {
  /**
   * Registry entry schema version (1 = ui-server, 2 = managed-server)
   */
  schemaVersion: number;
  kind: AgentRegistryLiveTargetEntryKind;
  /**
   * Operating-system pid of the process owning this entry
   */
  pid: number;
  /**
   * Bind host for the entry's JSON-RPC server
   */
  host: string;
  /**
   * TCP port the entry's JSON-RPC server is listening on
   */
  port: number;
  /**
   * Session ID of the foreground session for this entry
   */
  sessionId?: string;
  /**
   * Friendly session name (when set)
   */
  sessionName?: string;
  /**
   * Working directory of the session (when known)
   */
  cwd?: string;
  /**
   * Git branch of the session (when known)
   */
  branch?: string;
  /**
   * Model identifier currently selected for the session
   */
  model?: string;
  status?: AgentRegistryLiveTargetEntryStatus;
  attentionKind?: AgentRegistryLiveTargetEntryAttentionKind;
  /**
   * Monotonic per-publisher revision counter incremented on every status update. Lets watchers detect transient flips.
   */
  statusRevision?: number;
  lastTerminalEvent?: AgentRegistryLiveTargetEntryLastTerminalEvent;
  /**
   * ISO 8601 timestamp captured at registration
   */
  startedAt: string;
  /**
   * Copilot CLI version that wrote the entry
   */
  copilotVersion: string;
  /**
   * Wall-clock milliseconds since the watcher last observed this entry (heartbeat freshness)
   */
  lastSeenMs: number;
  /**
   * Connection token (null when the target is unauthenticated)
   *
   * @internal
   */
  token?: string | null;
}
/**
 * Per-spawn log-capture outcome; populated from spawnLiveTarget.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistryLogCapture".
 */
/** @experimental */
export interface AgentRegistryLogCapture {
  /**
   * Whether per-spawn log capture is on (false when env-disabled or open failed)
   */
  enabled: boolean;
  /**
   * Absolute path to the per-spawn log file (only set when enabled)
   */
  path?: string;
  /**
   * Human-readable open failure message (only set when enabled === false AND the env-disable opt-out was NOT used)
   */
  openError?: string;
  openErrorReason?: AgentRegistryLogCaptureOpenErrorReason;
}
/**
 * `child_process.spawn` itself failed before the child entered the registry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnError".
 */
/** @experimental */
export interface AgentRegistrySpawnError {
  /**
   * Discriminator: child_process.spawn itself failed
   */
  kind: "spawn-error";
  /**
   * Human-readable error message
   */
  message: string;
  /**
   * Underlying errno code (e.g. ENOENT, EACCES) when available
   */
  code?: string;
}
/**
 * Spawn succeeded but the child did not publish a matching managed-server entry within the timeout.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnRegistryTimeout".
 */
/** @experimental */
export interface AgentRegistrySpawnRegistryTimeout {
  /**
   * Discriminator: spawn succeeded but child never registered
   */
  kind: "registry-timeout";
  /**
   * Process ID of the orphaned child (so the caller can offer 'kill the pid' guidance)
   */
  childPid: number;
  logCapture?: AgentRegistryLogCapture;
}
/**
 * Inputs to spawn a managed-server child via the controller's spawn delegate.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnRequest".
 */
/** @experimental */
export interface AgentRegistrySpawnRequest {
  /**
   * Working directory for the spawned child (must be an existing directory)
   */
  cwd: string;
  /**
   * Custom or built-in agent name (e.g. 'explore'). When omitted, the child uses its own default.
   */
  agentName?: string;
  /**
   * Model identifier to apply to the new session
   */
  model?: string;
  /**
   * Friendly session name. Must satisfy validateSessionName: non-empty, no leading/trailing whitespace, <=100 chars, no control chars, no double quotes.
   */
  name?: string;
  permissionMode?: AgentRegistrySpawnPermissionMode;
  /**
   * Optional first user message. Forwarded to the caller (the CLI's spawn wrapper sends it post-attach via the standard LocalRpcSession.send path).
   */
  initialPrompt?: string;
}
/**
 * Managed-server child was spawned and registered successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnSpawned".
 */
/** @experimental */
export interface AgentRegistrySpawnSpawned {
  /**
   * Discriminator: managed-server child spawned successfully
   */
  kind: "spawned";
  entry: AgentRegistryLiveTargetEntry;
  /**
   * Whether the delegate already sent the initial prompt. Always omitted in the current wiring: the controller sends the prompt post-attach via the standard LocalRpcSession.send path.
   */
  initialPromptSent?: boolean;
  /**
   * If the delegate attempted to send the initial prompt and failed, the categorized error message.
   */
  initialPromptError?: string;
  logCapture?: AgentRegistryLogCapture;
}
/**
 * Synchronous pre-validation rejected the spawn request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentRegistrySpawnValidationError".
 */
/** @experimental */
export interface AgentRegistrySpawnValidationError {
  /**
   * Discriminator: synchronous pre-validation rejected the request
   */
  kind: "validation-error";
  reason: AgentRegistrySpawnValidationErrorReason;
  field?: AgentRegistrySpawnValidationErrorField;
  /**
   * Human-readable explanation; safe to surface in the UI banner. Never logged to unrestricted telemetry.
   */
  message: string;
}
/**
 * Custom agents available to the session after reloading definitions from disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentReloadResult".
 */
/** @experimental */
export interface AgentReloadResult {
  /**
   * Reloaded custom agents
   */
  agents: AgentInfo[];
}
/**
 * Optional project paths to include in agent discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentsDiscoverRequest".
 */
/** @experimental */
export interface AgentsDiscoverRequest {
  /**
   * Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan).
   */
  projectPaths?: string[];
  /**
   * When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments.
   */
  excludeHostAgents?: boolean;
}
/**
 * Name of the custom agent to select for subsequent turns.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentSelectRequest".
 */
/** @experimental */
export interface AgentSelectRequest {
  /**
   * Name of the custom agent to select
   */
  name: string;
}
/**
 * The newly selected custom agent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentSelectResult".
 */
/** @experimental */
export interface AgentSelectResult {
  agent: AgentInfo;
}
/**
 * An in-memory authored prompt override for an available agent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentSetPromptRequest".
 */
/** @experimental */
export interface AgentSetPromptRequest {
  /**
   * Stable effective agent id. Plugin namespace separators are normalized.
   */
  id: string;
  /**
   * Replacement authored prompt. Empty text is valid.
   */
  prompt: string;
}
/**
 * Optional project paths to include when enumerating agent discovery directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AgentsGetDiscoveryPathsRequest".
 */
/** @experimental */
export interface AgentsGetDiscoveryPathsRequest {
  /**
   * Optional list of project directory paths. When omitted or empty, only the user-level directory is returned.
   */
  projectPaths?: string[];
  /**
   * When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`).
   */
  excludeHostAgents?: boolean;
}
/**
 * Credential-free authentication identity safe to expose to hosts and user interfaces.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthIdentity".
 */
/** @experimental */
export interface AuthIdentity {
  type: AuthInfoType;
  /**
   * Authentication host
   */
  host: string;
  /**
   * Authenticated login, when available
   */
  login?: string;
  /**
   * Name of the environment variable that supplied the credential, when applicable
   */
  envVar?: string;
  /**
   * Opaque SDK GitHub credential registration backing this identity. Routing metadata only; never a credential.
   */
  registrationId?: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Validation error from an authentication attempt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "AuthValidationError".
 */
/** @experimental */
export interface AuthValidationError {
  /**
   * Authentication validation error message
   */
  message: string;
  /**
   * Optional message returned by GitHub
   */
  githubMessage?: string;
}
/**
 * The running runtime's complete catalog of well-known built-in model IDs, including supported models and additional IDs with built-in metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltInModelCatalog".
 */
/** @experimental */
export interface BuiltInModelCatalog {
  /**
   * Built-in model entries.
   */
  models: BuiltInModelCatalogEntry[];
}
/**
 * A well-known model in the runtime's built-in catalog.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltInModelCatalogEntry".
 */
/** @experimental */
export interface BuiltInModelCatalogEntry {
  /**
   * Well-known runtime model ID suitable for provider or provider-model metadata. This is not necessarily the provider-facing deployment or model name and does not indicate CAPI entitlement or provider availability.
   */
  id: string;
}
/**
 * Rust-owned metadata and input schema for a built-in tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolDescriptor".
 */
/** @experimental */
export interface BuiltinToolDescriptor {
  /**
   * Stable name used to invoke the built-in tool.
   */
  name: string;
  /**
   * Optional human-readable title for the tool.
   */
  title: string | null;
  /**
   * Model-facing description of the tool's behavior.
   */
  description: string;
  /**
   * JSON Schema for the tool input, or null when the tool uses a custom format.
   */
  inputSchema: BuiltinToolInputSchema | null;
  /**
   * Optional supplemental usage instructions for the tool.
   */
  instructions: string | null;
  /**
   * Optional tool category discriminator.
   */
  type: string | null;
  /**
   * Optional custom input format used instead of a JSON Schema.
   */
  format: BuiltinToolFormat | null;
  safeForTelemetry: BuiltinToolSafeForTelemetry;
  /**
   * Whether the tool executes commands in a terminal.
   */
  isTerminal: boolean;
  /**
   * Whether the tool provides a specialized intention summary.
   */
  hasSummariseIntention: boolean;
}
/**
 * JSON Schema object accepted by a built-in tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolInputSchema".
 */
/** @experimental */
export interface BuiltinToolInputSchema {
  type: BuiltinToolInputSchemaType;
  [k: string]: JsonValue | undefined;
}
/**
 * Custom grammar input format accepted by a built-in tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolFormat".
 */
/** @experimental */
export interface BuiltinToolFormat {
  type: BuiltinToolFormatType;
  /**
   * Grammar syntax used by the format definition.
   */
  syntax: string;
  /**
   * Grammar definition accepted by the tool.
   */
  definition: string;
}
/**
 * Per-field telemetry-safety policy for a built-in tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "BuiltinToolSafeTelemetryFields".
 */
/** @experimental */
export interface BuiltinToolSafeTelemetryFields {
  /**
   * Whether the tool name may be included in telemetry without obfuscation.
   */
  name?: boolean;
  /**
   * Whether tool input names may be included in telemetry without obfuscation.
   */
  inputsNames?: boolean;
}
/**
 * Cancellation result for a user-requested shell command.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CancelUserRequestedShellCommandResult".
 */
/** @experimental */
export interface CancelUserRequestedShellCommandResult {
  /**
   * Whether an in-flight execution was found and signalled to cancel
   */
  cancelled: boolean;
}
/**
 * Canvas action that the agent or host can invoke. To discover the input schema for a particular action, call the list_canvas_capabilities tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasAction".
 */
/** @experimental */
export interface CanvasAction {
  /**
   * Action name exposed by the canvas provider
   */
  name: string;
  /**
   * Description of the action
   */
  description?: string;
  inputSchema?: CanvasJsonSchema;
}
/**
 * Canvas action invocation parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasActionInvokeRequest".
 */
/** @experimental */
export interface CanvasActionInvokeRequest {
  /**
   * Open canvas instance identifier
   */
  instanceId: string;
  /**
   * Action name to invoke
   */
  actionName: string;
  /**
   * Action input
   */
  input?: JsonValue;
}
/**
 * Canvas close parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasCloseRequest".
 */
/** @experimental */
export interface CanvasCloseRequest {
  /**
   * Open canvas instance identifier
   */
  instanceId: string;
}
/**
 * Host context supplied by the runtime.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasHostContext".
 */
/** @experimental */
export interface CanvasHostContext {
  capabilities?: CanvasHostContextCapabilities;
}
/**
 * Host capabilities
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasHostContextCapabilities".
 */
/** @experimental */
export interface CanvasHostContextCapabilities {
  /**
   * Whether canvas rendering is supported
   */
  canvases?: boolean;
}
/**
 * Declared canvases available in this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasList".
 */
/** @experimental */
export interface CanvasList {
  /**
   * Declared canvases available in this session
   */
  canvases: DiscoveredCanvas[];
}
/**
 * Canvas available in the current session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredCanvas".
 */
/** @experimental */
export interface DiscoveredCanvas {
  /**
   * Human-readable canvas name
   */
  displayName: string;
  /**
   * Short, single-sentence description shown to the agent in canvas catalogs.
   */
  description: string;
  /**
   * Host-local PNG path for the canvas icon, when supplied
   */
  icon?: string;
  inputSchema?: CanvasJsonSchema;
  /**
   * Actions the agent or host may invoke on an open instance
   */
  actions?: CanvasAction[];
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Owning extension display name, when available
   */
  extensionName?: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
}
/**
 * Live open-canvas snapshot.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasListOpenResult".
 */
/** @experimental */
export interface CanvasListOpenResult {
  /**
   * Currently open canvas instances
   */
  openCanvases: OpenCanvasInstance[];
}
/**
 * Open canvas instance snapshot.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OpenCanvasInstance".
 */
/** @experimental */
export interface OpenCanvasInstance {
  /**
   * Stable caller-supplied canvas instance identifier
   */
  instanceId: string;
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Owning extension display name, when available
   */
  extensionName?: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Host-local PNG path for the canvas icon, when supplied
   */
  icon?: string;
  /**
   * Rendered title
   */
  title?: string;
  /**
   * Provider-supplied status text
   */
  status?: string;
  /**
   * URL for web-rendered canvases
   */
  url?: string;
  /**
   * Input supplied when the instance was opened
   */
  input?: JsonValue;
}
/**
 * Canvas open parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasOpenRequest".
 */
/** @experimental */
export interface CanvasOpenRequest {
  /**
   * Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId.
   */
  extensionId?: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Caller-supplied stable instance identifier
   */
  instanceId: string;
  /**
   * Canvas open input
   */
  input?: JsonValue;
}
/**
 * Canvas close parameters sent to the provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasProviderCloseRequest".
 */
/** @experimental */
export interface CanvasProviderCloseRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Canvas instance identifier
   */
  instanceId: string;
  host?: CanvasHostContext;
  session?: CanvasSessionContext;
}
/**
 * Session context supplied by the runtime.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasSessionContext".
 */
/** @experimental */
export interface CanvasSessionContext {
  /**
   * Active session working directory, when known.
   */
  workingDirectory?: string;
}
/**
 * Canvas action invocation parameters sent to the provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasProviderInvokeActionRequest".
 */
/** @experimental */
export interface CanvasProviderInvokeActionRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Canvas instance identifier
   */
  instanceId: string;
  /**
   * Action name to invoke
   */
  actionName: string;
  /**
   * Action input
   */
  input?: JsonValue;
  host?: CanvasHostContext;
  session?: CanvasSessionContext;
}
/**
 * Canvas open parameters sent to the provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasProviderOpenRequest".
 */
/** @experimental */
export interface CanvasProviderOpenRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Owning provider identifier
   */
  extensionId: string;
  /**
   * Provider-local canvas identifier
   */
  canvasId: string;
  /**
   * Stable caller-supplied canvas instance identifier
   */
  instanceId: string;
  /**
   * Canvas open input
   */
  input?: JsonValue;
  host?: CanvasHostContext;
  session?: CanvasSessionContext;
}
/**
 * Canvas open result returned by the provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasProviderOpenResult".
 */
/** @experimental */
export interface CanvasProviderOpenResult {
  /**
   * URL for web-rendered canvases
   */
  url?: string;
  /**
   * Provider-supplied title
   */
  title?: string;
  /**
   * Provider-supplied status text
   */
  status?: string;
}
/**
 * Internal canvas provider registration parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasProviderRegisterRequest".
 */
/** @experimental */
export interface CanvasProviderRegisterRequest {
  /**
   * Connection identifier for callback routing
   */
  connectionId: string;
  /**
   * Provider metadata supplied by the host
   */
  info: JsonValue;
  /**
   * Canvas contributions supplied by the provider
   */
  canvases: JsonValue[];
}
/**
 * Internal canvas provider unregistration parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CanvasProviderUnregisterRequest".
 */
/** @experimental */
export interface CanvasProviderUnregisterRequest {
  /**
   * Connection identifier to unregister
   */
  connectionId: string;
}
/**
 * Options scoped to the built-in CAPI (Copilot API) provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CapiSessionOptions".
 */
/** @experimental */
export interface CapiSessionOptions {
  autoTier?: AutoTier;
  /**
   * Whether to use WebSocket transport for the CAPI Responses API. Enabled by default when the model advertises `ws:/responses` support; set to `false` to force the HTTP Responses transport in environments where WebSockets are blocked (e.g. behind a proxy). Setting this to `false` is equivalent to the `COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES` environment variable.
   */
  enableWebSocketResponses?: boolean;
}
/**
 * Semantic digest of a strictly parsed and schema-validated JSON MCP card. Both URL-backed and embedded cards are canonicalised with RFC 8785 JSON Canonicalization Scheme, encoded as UTF-8, and hashed with SHA-256.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CardDigest".
 */
/** @experimental */
export interface CardDigest {
  algorithm: CardDigestAlgorithm;
  value: CardDigestValue;
}
/**
 * An inert AI skill catalog result. AI skills are discovery-only and cannot be represented as installable through this surface.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogAiSkillCandidate".
 */
/** @experimental */
export interface CatalogAiSkillCandidate {
  /**
   * Opaque, runtime-instance scoped, TTL-bound, single-use handle for this candidate. Carries no readable information and is rejected when stale, replayed, or presented to a different runtime instance. Never logged.
   */
  handle: string;
  /**
   * ISO 8601 timestamp after which the handle is stale and will be rejected.
   */
  handleExpiresAt: string;
  /**
   * Discriminator: this candidate describes an AI skill
   */
  kind: "ai-skill";
  /**
   * Media type of the underlying AI skill card
   */
  mediaType: "application/ai-skill";
  /**
   * AI skills are discovery-only and cannot be installed through this surface
   */
  installability: "not-installable-kind";
  /**
   * Display name taken verbatim from the card. Inert untrusted text.
   */
  displayName: string;
  /**
   * Description taken verbatim from the card. Inert untrusted text.
   */
  description?: string;
  /**
   * Publisher taken verbatim from the card. Inert untrusted text.
   */
  publisher?: string;
  source: CatalogCandidateSource;
  provenance: CatalogAiSkillCandidateProvenance;
}
/**
 * Candidate whose card is retrieved from a URL through the runtime's hardened fetch boundary.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCandidateSourceUrl".
 */
/** @experimental */
export interface CatalogCandidateSourceUrl {
  /**
   * Discriminator: the card is URL-backed, and carries no embedded data
   */
  kind: "url";
  /**
   * Card URL as advertised. Inert untrusted data: the runtime retrieves it only through its own hardened boundary, and it is never logged.
   */
  url: string;
}
/**
 * Candidate whose card reference arrived inline. The document and its content-derived properties stay behind the runtime boundary.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogCandidateSourceEmbedded".
 */
/** @experimental */
export interface CatalogCandidateSourceEmbedded {
  /**
   * Discriminator: the card is embedded, and carries no URL
   */
  kind: "embedded";
}
/**
 * Where and when an AI skill catalog reference was observed. Discovery provenance deliberately carries no content digest because search does not establish the exact validated content a later plan will bind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogAiSkillCandidateProvenance".
 */
/** @experimental */
export interface CatalogAiSkillCandidateProvenance {
  /**
   * Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data.
   */
  authority: string;
  /**
   * ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp.
   */
  observedAt: string;
  /**
   * Media type advertised for the referenced AI skill card
   */
  mediaType: "application/ai-skill";
}
/**
 * An optional catalog authentication exchange did not establish the caller's identity. Anonymous search remains supported; this refusal is reserved for an operation that cannot continue after the attempted exchange. It is distinct from `policy-rejected` and from a network failure, and the reason identifies the recovery action.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogAuthenticationRequiredError".
 */
/** @experimental */
export interface CatalogAuthenticationRequiredError {
  /**
   * Discriminator: the caller is not authenticated
   */
  kind: "authentication-required";
  reason: CatalogAuthenticationRequiredReason;
  /**
   * Human-readable explanation, safe to surface. Never contains a credential or token, nor a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * An inert MCP server catalog result. Every free-text field is untrusted external data and must never be treated as an instruction, and the handle is the only way to refer to the candidate in a later operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogMcpServerCandidate".
 */
/** @experimental */
export interface CatalogMcpServerCandidate {
  /**
   * Opaque, runtime-instance scoped, TTL-bound, single-use handle for this candidate. Carries no readable information and is rejected when stale, replayed, or presented to a different runtime instance. Never logged.
   */
  handle: string;
  /**
   * ISO 8601 timestamp after which the handle is stale and will be rejected.
   */
  handleExpiresAt: string;
  /**
   * Discriminator: this candidate describes an MCP server
   */
  kind: "mcp-server";
  mediaType: McpServerCardMediaType;
  installability: CatalogMcpServerInstallability;
  /**
   * Display name taken verbatim from the card. Inert untrusted text.
   */
  displayName: string;
  /**
   * Description taken verbatim from the card. Inert untrusted text.
   */
  description?: string;
  /**
   * Publisher taken verbatim from the card. Inert untrusted text.
   */
  publisher?: string;
  source: CatalogCandidateSource;
  provenance: CatalogMcpServerCandidateProvenance;
}
/**
 * Where and when an MCP server catalog reference was observed. Discovery provenance deliberately carries no content digest because search does not establish the exact validated content a later plan will bind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogMcpServerCandidateProvenance".
 */
/** @experimental */
export interface CatalogMcpServerCandidateProvenance {
  /**
   * Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data.
   */
  authority: string;
  /**
   * ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp.
   */
  observedAt: string;
  mediaType: McpServerCardMediaType;
}
/**
 * The protocol version and capability set a caller requires, supplied on every catalog request so negotiation cannot be skipped by omission.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogClientContract".
 */
/** @experimental */
export interface CatalogClientContract {
  /**
   * SDK protocol version the caller was generated against. A caller below the runtime's minimum supported version is refused rather than served a partial result.
   */
  protocolVersion: number;
  /**
   * Wire features the caller requires the runtime to understand. Identifiers are bounded but extensible so a newer caller can negotiate with an older runtime. Requiring an unknown feature yields a typed refusal listing what is understood, never a partial grant. A grant does not promise that a deployment has enabled the operation; typed unavailable results report that separately.
   *
   * @maxItems 32
   */
  requiredCapabilities: CatalogCapabilityId[];
}
/**
 * An upstream catalog response broke the wire contract. Most importantly, every result must carry exactly one of a URL or embedded data: a result carrying both, or neither, is refused here rather than being guessed at.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogContractViolationError".
 */
/** @experimental */
export interface CatalogContractViolationError {
  /**
   * Discriminator: the upstream response broke the contract
   */
  kind: "contract-violation";
  reason: CatalogContractViolationReason;
  /**
   * Human-readable explanation, safe to surface. Never echoes response content, nor a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * A presented handle was not accepted. Handles are runtime-instance scoped, TTL-bound, and single-use, so each way of failing is reported distinctly.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogHandleRejectedError".
 */
/** @experimental */
export interface CatalogHandleRejectedError {
  /**
   * Discriminator: a handle was rejected
   */
  kind: "handle-rejected";
  handleType: CatalogHandleType;
  reason: CatalogHandleRejectionReason;
  /**
   * Human-readable explanation, safe to surface. Never contains the handle itself, nor a query, URL, or secret.
   */
  message: string;
}
/**
 * The request was rejected before any work was done, because a bounded field fell outside its permitted range or a required field was unusable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogInvalidRequestError".
 */
/** @experimental */
export interface CatalogInvalidRequestError {
  /**
   * Discriminator: the request itself was invalid
   */
  kind: "invalid-request";
  field: CatalogInvalidRequestField;
  /**
   * Human-readable explanation, safe to surface. Never echoes the offending value, nor a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * A card could not be parsed or did not satisfy its declared media type's schema.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogMalformedCardError".
 */
/** @experimental */
export interface CatalogMalformedCardError {
  /**
   * Discriminator: the card was malformed
   */
  kind: "malformed-card";
  reason: CatalogMalformedCardReason;
  mediaType?: CatalogMediaType;
  /**
   * Human-readable explanation, safe to surface. Never echoes card content, nor a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * The protocol version and capability set the runtime actually honoured for a successful catalog operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNegotiatedContract".
 */
/** @experimental */
export interface CatalogNegotiatedContract {
  /**
   * Protocol version of the runtime that served the request.
   */
  runtimeProtocolVersion: number;
  /**
   * Wire features the runtime understood for this operation. Always a superset of the caller's required features, because any shortfall is a refusal instead. Operation availability remains a separate typed result.
   */
  grantedCapabilities: CatalogCapability[];
}
/**
 * The caller's protocol version or required capabilities cannot be honoured. Returned instead of a partial or ambiguous success.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNegotiationRefusedError".
 */
/** @experimental */
export interface CatalogNegotiationRefusedError {
  /**
   * Discriminator: capability or protocol-version negotiation failed
   */
  kind: "negotiation-refused";
  reason: CatalogNegotiationRefusedReason;
  /**
   * Protocol version of the runtime that refused the request.
   */
  runtimeProtocolVersion: number;
  /**
   * Lowest caller protocol version this runtime will serve.
   */
  minimumSupportedProtocolVersion: number;
  /**
   * Every wire feature this runtime understands, so the caller can retry within that contract. This list does not imply that every deployment has enabled every operation.
   */
  supportedCapabilities: CatalogCapability[];
  /**
   * The subset of the caller's bounded extensible capability identifiers this runtime cannot honour.
   *
   * @maxItems 32
   */
  unsupportedCapabilities: CatalogCapabilityId[];
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * The runtime could not reach the catalog authority or retrieve a card. Covers being offline as well as transport-level failure.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNetworkFailureError".
 */
/** @experimental */
export interface CatalogNetworkFailureError {
  /**
   * Discriminator: the network operation failed
   */
  kind: "network-failure";
  reason: CatalogNetworkFailureReason;
  /**
   * HTTP status code, when the failure was a rejected response.
   */
  statusCode?: number;
  /**
   * Bounded cooldown in seconds before another catalog request should be attempted, when the authority supplied a numeric Retry-After value or the runtime applied its documented fallback.
   */
  retryAfterSeconds?: number;
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * The candidate is discoverable but cannot be installed. `application/ai-skill` resolves here, because it stays searchable while remaining typed non-installable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogNotInstallableError".
 */
/** @experimental */
export interface CatalogNotInstallableError {
  /**
   * Discriminator: the candidate cannot be installed
   */
  kind: "not-installable";
  reason: CatalogNotInstallableReason;
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * Registry or enterprise policy refused the operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogPolicyRejectedError".
 */
/** @experimental */
export interface CatalogPolicyRejectedError {
  /**
   * Discriminator: policy refused the operation
   */
  kind: "policy-rejected";
  source: McpPlanPolicySource;
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * A bounded catalog search. Both the query length and the result count are capped by the schema so a caller cannot request an unbounded scan.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogSearchRequest".
 */
/** @experimental */
export interface CatalogSearchRequest {
  contract: CatalogClientContract;
  /**
   * Free-text search query. Persisted as tool input for session continuity, but omitted from telemetry.
   */
  query: string;
  /**
   * Maximum number of candidates to return. Defaults to 10 when omitted.
   */
  limit?: number;
  /**
   * Restrict results to these candidate kinds. When omitted, every kind the runtime supports is searched.
   *
   * @minItems 1
   * @maxItems 2
   */
  kinds?: [CatalogCandidateKind] | [CatalogCandidateKind, CatalogCandidateKind];
}
/**
 * A completed catalog search: inert candidate summaries, each carrying a single-use handle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogSearchSucceeded".
 */
/** @experimental */
export interface CatalogSearchSucceeded {
  /**
   * Discriminator: the search completed
   */
  kind: "succeeded";
  /**
   * Pseudonymous identifier for this search, issued by the runtime or by the catalog authority it queried and never by the caller, so it cannot be forged or replayed to attribute an install to a search that never happened. Always present on a success, so a result set can be tied to the installs it leads to. It identifies a search rather than a person: it is derived from no user, account, device, or query data, and must never be joined with user identity to re-identify anyone.
   */
  searchId: string;
  /**
   * Matching candidates, never more than the requested limit. All text is inert untrusted data.
   *
   * @maxItems 50
   */
  candidates: CatalogCandidate[];
  /**
   * Whether further matches existed beyond the requested limit.
   */
  truncated: boolean;
  negotiated: CatalogNegotiatedContract;
}
/**
 * The request asked for a candidate kind this runtime does not serve.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnsupportedKindError".
 */
/** @experimental */
export interface CatalogUnsupportedKindError {
  /**
   * Discriminator: an unsupported candidate kind was requested
   */
  kind: "unsupported-kind";
  /**
   * The kinds from the request that are not supported.
   */
  requestedKinds: CatalogCandidateKind[];
  /**
   * Every candidate kind this runtime can serve.
   */
  supportedKinds: CatalogCandidateKind[];
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * Retrieval was refused by the runtime's hardened fetch boundary before any request left the process, or before a redirect was followed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnsafeRetrievalError".
 */
/** @experimental */
export interface CatalogUnsafeRetrievalError {
  /**
   * Discriminator: retrieval was refused as unsafe
   */
  kind: "unsafe-retrieval";
  reason: CatalogUnsafeRetrievalReason;
  /**
   * Human-readable explanation, safe to surface. Never contains the refused URL, nor a query, handle, or secret.
   */
  message: string;
}
/**
 * The operation is not available on this runtime. Distinct from a network failure: nothing was attempted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnavailableError".
 */
/** @experimental */
export interface CatalogUnavailableError {
  /**
   * Discriminator: the operation is not available
   */
  kind: "unavailable";
  reason: CatalogUnavailableReason;
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * No transport this runtime can use is available for the requested server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CatalogUnavailableTransportError".
 */
/** @experimental */
export interface CatalogUnavailableTransportError {
  /**
   * Discriminator: no usable transport is available
   */
  kind: "unavailable-transport";
  reason: CatalogUnavailableTransportReason;
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  message: string;
}
/**
 * Slash commands available in the session, after applying any include/exclude filters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandList".
 */
/** @experimental */
export interface CommandList {
  /**
   * Commands available in this session
   */
  commands: SlashCommandInfo[];
}
/**
 * Slash-command metadata with name, aliases, description, kind, input hint, execution allowance, and schedulability.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInfo".
 */
/** @experimental */
export interface SlashCommandInfo {
  /**
   * Canonical command name without a leading slash
   */
  name: string;
  /**
   * Canonical aliases without leading slashes
   */
  aliases?: string[];
  /**
   * Human-readable command description
   */
  description: string;
  kind: SlashCommandKind;
  input?: SlashCommandInput;
  /**
   * Whether the command may run while an agent turn is active
   */
  allowDuringAgentExecution: boolean;
  /**
   * Whether the command is experimental
   */
  experimental?: boolean;
  /**
   * Whether the command may be the target of `/every` / `/after` schedules. Resolution happens at every tick, so only set this when the command is safe to re-invoke and produces an agent prompt.
   */
  schedulable?: boolean;
}
/**
 * Optional unstructured input hint
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInput".
 */
/** @experimental */
export interface SlashCommandInput {
  /**
   * Hint to display when command input has not been provided
   */
  hint: string;
  /**
   * Optional literal choices the input accepts, each with a human-facing description; clients may render these as selectable options
   */
  choices?: SlashCommandInputChoice[];
  /**
   * When true, the command requires non-empty input; clients should render the input hint as required
   */
  required?: boolean;
  completion?: SlashCommandInputCompletion;
  /**
   * When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace
   */
  preserveMultilineInput?: boolean;
}
/**
 * A literal choice the command input accepts, with a human-facing description
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandInputChoice".
 */
/** @experimental */
export interface SlashCommandInputChoice {
  /**
   * The literal choice value (e.g. 'on', 'off', 'show')
   */
  name: string;
  /**
   * Human-readable description shown alongside the choice
   */
  description: string;
}
/**
 * The pending slash-command invocation effect to finalize, plus whether the host applied or cancelled it.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsFinalizeInvocationEffectRequest".
 */
/** @experimental */
export interface CommandsFinalizeInvocationEffectRequest {
  /**
   * The slash-command result object that produced the pending effect, echoed back unchanged.
   */
  effect: {};
  outcome: CommandsInvocationEffectOutcome;
}
/**
 * Whether finalizing the invocation effect succeeded, and the failure reason when it did not.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsFinalizeInvocationEffectResult".
 */
/** @experimental */
export interface CommandsFinalizeInvocationEffectResult {
  /**
   * Whether the pending invocation effect was finalized successfully.
   */
  success: boolean;
  /**
   * Failure reason when the invocation effect could not be finalized.
   */
  error?: string;
}
/**
 * Pending command request ID and an optional error if the client handler failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsHandlePendingCommandRequest".
 */
/** @experimental */
export interface CommandsHandlePendingCommandRequest {
  /**
   * Request ID from the command invocation event
   */
  requestId: string;
  /**
   * Error message if the command handler failed
   */
  error?: string;
}
/**
 * Indicates whether the pending client-handled command was completed successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsHandlePendingCommandResult".
 */
/** @experimental */
export interface CommandsHandlePendingCommandResult {
  /**
   * Whether the command was handled successfully
   */
  success: boolean;
}
/**
 * Slash command name and optional raw input string to invoke.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsInvokeRequest".
 */
/** @experimental */
export interface CommandsInvokeRequest {
  /**
   * Command name. Leading slashes are stripped and the name is matched case-insensitively.
   */
  name: string;
  /**
   * Raw input after the command name
   */
  input?: string;
  origin?: CommandsInvocationOrigin;
}
/**
 * Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsRespondToQueuedCommandRequest".
 */
/** @experimental */
export interface CommandsRespondToQueuedCommandRequest {
  /**
   * Request ID from the `command.queued` event the host is responding to.
   */
  requestId: string;
  result: QueuedCommandResult;
}
/**
 * Queued-command response indicating the host executed the command, with an optional flag to stop queue processing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandHandled".
 */
/** @experimental */
export interface QueuedCommandHandled {
  /**
   * The host actually executed the queued command.
   */
  handled: true;
  /**
   * When true, the runtime will not process subsequent queued commands until a new request comes in.
   */
  stopProcessingQueue?: boolean;
}
/**
 * Queued-command response indicating the host did not execute the command and the queue may continue.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuedCommandNotHandled".
 */
/** @experimental */
export interface QueuedCommandNotHandled {
  /**
   * The host did not execute the queued command. Unblocks the queue without claiming the command was processed (e.g. when the handler threw before completing).
   */
  handled: false;
}
/**
 * Indicates whether the queued-command response was matched to a pending request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CommandsRespondToQueuedCommandResult".
 */
/** @experimental */
export interface CommandsRespondToQueuedCommandResult {
  /**
   * Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.
   */
  success: boolean;
}
/**
 * Characters that, when typed in the composer, should trigger a `completions.request`. Empty when the session has no host-driven completions (e.g. local sessions, or a relay host that does not advertise `completionTriggerCharacters`).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CompletionsGetTriggerCharactersResult".
 */
/** @experimental */
export interface CompletionsGetTriggerCharactersResult {
  /**
   * Trigger characters advertised by the host (e.g. `["@", "#"]`). Empty disables host-driven completions for the session.
   */
  triggerCharacters: string[];
}
/**
 * Request host-driven completions for the current composer input.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CompletionsRequestRequest".
 */
/** @experimental */
export interface CompletionsRequestRequest {
  /**
   * The full composed composer input.
   */
  text: string;
  /**
   * Cursor offset within `text`, in UTF-16 code units.
   */
  offset: number;
}
/**
 * Host-driven completion items for the current composer input. Empty when the host returns no items or does not support completions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CompletionsRequestResult".
 */
/** @experimental */
export interface CompletionsRequestResult {
  /**
   * Completion items in host-ranked order.
   */
  items: SessionCompletionItem[];
}
/**
 * A single host-driven completion. Accepting an item replaces `[rangeStart, rangeEnd)` (UTF-16 code units) in the composer with `insertText`; when the range is absent, the active token around the cursor is replaced.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionCompletionItem".
 */
/** @experimental */
export interface SessionCompletionItem {
  /**
   * Text spliced into the composer when the item is accepted.
   */
  insertText: string;
  /**
   * Start of the replacement range in `text`, in UTF-16 code units.
   */
  rangeStart?: number;
  /**
   * End (exclusive) of the replacement range in `text`, in UTF-16 code units.
   */
  rangeEnd?: number;
  /**
   * Primary display label for the picker row. Falls back to `insertText` when absent.
   */
  label?: string;
  /**
   * Render-kind hint for the picker row (e.g. `"document"`, `"directory"`), derived from the host's display kind.
   */
  kind?: string;
}
/**
 * Params to attach or detach an in-process ExtensionController delegate.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConfigureSessionExtensionsParams".
 */
/** @experimental */
/** @internal */
export interface ConfigureSessionExtensionsParams {
  /**
   * Session to attach the extension controller delegate to.
   */
  sessionId: string;
  /**
   * In-process ExtensionController delegate (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. The post-SDK extension surface exposes list/enable/disable/reload via dedicated RPCs served by the runtime.
   *
   * @internal
   *
   * @internal
   */
  controller?: OpaqueInProcessValue;
}
/**
 * Identity of the integrating host, declared once on the `server.connect` handshake so telemetry from this connection is attributed to a single, consistent surface. All fields are optional; omit them to keep the default attribution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectClientInfo".
 */
/** @experimental */
/** @internal */
export interface ConnectClientInfo {
  /**
   * Name of the host editor, e.g. `"vscode"`.
   */
  editorName?: string;
  /**
   * Version of the host editor, e.g. `"1.124.2"`. Ignored unless it looks like a version string.
   */
  editorVersion?: string;
  /**
   * Name of the Copilot extension within the host, e.g. `"copilot-chat"`.
   */
  extensionName?: string;
  /**
   * Version of the Copilot extension within the host, e.g. `"0.54.0"`. Ignored unless it looks like a version string.
   */
  extensionVersion?: string;
}
/**
 * Metadata for a connected remote session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectedRemoteSessionMetadata".
 */
/** @experimental */
export interface ConnectedRemoteSessionMetadata {
  /**
   * SDK session ID for the connected remote session.
   */
  sessionId: string;
  /**
   * Optional friendly session name.
   */
  name?: string;
  /**
   * Optional session summary.
   */
  summary?: string;
  /**
   * Session start time as an ISO 8601 string.
   */
  startTime: string;
  /**
   * Last session update time as an ISO 8601 string.
   */
  modifiedTime: string;
  repository: ConnectedRemoteSessionMetadataRepository;
  /**
   * Pull request number associated with the session.
   */
  pullRequestNumber?: number;
  /**
   * Original remote resource identifier.
   */
  resourceId?: string;
  kind: ConnectedRemoteSessionMetadataKind;
  /**
   * Remote session staleness deadline as an ISO 8601 string.
   */
  staleAt?: string;
  /**
   * Remote session state returned by the backing service.
   */
  state?: string;
}
/**
 * Repository associated with the connected remote session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectedRemoteSessionMetadataRepository".
 */
/** @experimental */
export interface ConnectedRemoteSessionMetadataRepository {
  /**
   * Repository owner or organization login.
   */
  owner: string;
  /**
   * Repository name.
   */
  name: string;
  /**
   * Branch associated with the remote session.
   */
  branch: string;
}
/**
 * Remote session connection parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectRemoteSessionParams".
 */
/** @experimental */
export interface ConnectRemoteSessionParams {
  /**
   * Session ID to connect to.
   */
  sessionId: string;
}
/**
 * Connection-level opt-ins for the `server.connect` handshake. Transport authentication is consumed by the native protocol boundary before dispatch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectRequest".
 */
/** @experimental */
/** @internal */
export interface ConnectRequest {
  /**
   * Opt this connection in to GitHub telemetry forwarding for its lifetime. When set, the runtime forwards every internal telemetry event it emits — across all sessions, plus sessionless events — to this connection over the `gitHubTelemetry.event` notification. Regular events are also written to the runtime's normal GitHub/CTS path (dual-write); host-only compatibility events are forward-only and intentionally skip that path. Intended for first-party hosts that re-emit the events into their own telemetry stores. Both unrestricted and restricted events are forwarded, each tagged with a `restricted` discriminator; a backstop drops restricted events when restricted telemetry is disabled — using the process-global gate for ordinary events and an explicit session-scoped decision for host-only events.
   */
  enableGitHubTelemetryForwarding?: boolean;
  clientInfo?: ConnectClientInfo;
  /**
   * Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN
   */
  token?: string;
}
/**
 * Handshake result reporting the server's protocol version and package version on success.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ConnectResult".
 */
/** @experimental */
/** @internal */
export interface ConnectResult {
  /**
   * Always true on success
   */
  ok: true;
  /**
   * Server protocol version number
   */
  protocolVersion: number;
  /**
   * Server package version
   */
  version: string;
}
/**
 * Local file system absolute paths within the session working directory to check against its content-exclusion policy.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ContentExclusionCheckPathsRequest".
 */
/** @experimental */
export interface ContentExclusionCheckPathsRequest {
  /**
   * Local file system absolute paths within the session working directory to check. Results are returned in the same order, including duplicates.
   */
  paths: string[];
}
/**
 * Batch content-exclusion result. Callers must fail closed when policy evaluation is unavailable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ContentExclusionCheckPathsResult".
 */
/** @experimental */
export interface ContentExclusionCheckPathsResult {
  /**
   * Whether the session's policy service was available for the complete batch. When false, checks is empty and callers must treat every requested path as excluded.
   */
  available: boolean;
  /**
   * Per-path decisions in request order. Empty when available is false.
   */
  checks: ContentExclusionPathCheck[];
}
/**
 * Content-exclusion decision for one requested path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ContentExclusionPathCheck".
 */
/** @experimental */
export interface ContentExclusionPathCheck {
  /**
   * The path supplied by the caller.
   */
  path: string;
  /**
   * Whether the session's complete content-exclusion policy excludes the path.
   */
  excluded: boolean;
}
/**
 * A single large message currently in context.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ContextHeaviestMessage".
 */
/** @experimental */
export interface ContextHeaviestMessage {
  /**
   * Stable identifier for this message within the snapshot.
   */
  id: string;
  /**
   * Human-readable source label, e.g. `tool: bash` or `skill: tmux`. Presentation-only.
   */
  label: string;
  /**
   * Role of the chat message (`user`, `assistant`, or `tool`).
   */
  role: string;
  /**
   * Token count currently in context for this individual message.
   */
  tokens: number;
}
/**
 * The currently selected model, reasoning effort, and context tier for the session. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CurrentModel".
 */
/** @experimental */
export interface CurrentModel {
  /**
   * Currently active model identifier
   */
  modelId?: string;
  /**
   * Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.
   */
  reasoningEffort?: string;
  contextTier?: ContextTier;
}
/**
 * Lightweight metadata for a currently initialized session tool
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "CurrentToolMetadata".
 */
/** @experimental */
export interface CurrentToolMetadata {
  /**
   * Model-facing tool name
   */
  name: string;
  /**
   * Optional MCP/config namespaced tool name
   */
  namespacedName?: string;
  /**
   * MCP server name for MCP-backed tools
   */
  mcpServerName?: string;
  /**
   * Raw MCP tool name for MCP-backed tools
   */
  mcpToolName?: string;
  /**
   * Tool description
   */
  description: string;
  /**
   * JSON Schema for tool input
   */
  input_schema?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Whether the tool is loaded on demand via tool search
   */
  deferLoading?: boolean;
}
/**
 * A file included in the redacted debug bundle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsCollectedEntry".
 */
/** @experimental */
export interface DebugCollectLogsCollectedEntry {
  /**
   * Relative path of the file in the staged bundle/archive.
   */
  bundlePath: string;
  source: DebugCollectLogsSource;
  /**
   * Redacted output size in bytes.
   */
  sizeBytes: number;
}
/**
 * A caller-provided server-local file or directory to include in the debug bundle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsEntry".
 */
/** @experimental */
export interface DebugCollectLogsEntry {
  kind: DebugCollectLogsEntryKind;
  /**
   * Server-local source path to read.
   */
  path: string;
  /**
   * Relative path to use inside the staged bundle/archive.
   */
  bundlePath: string;
  redaction?: DebugCollectLogsRedaction;
  /**
   * When true, collection fails if this entry cannot be read. Defaults to false, which records the entry in `skippedEntries`.
   */
  required?: boolean;
}
/**
 * Built-in session diagnostics to include in the bundle. Omitted fields default to true.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsInclude".
 */
/** @experimental */
export interface DebugCollectLogsInclude {
  /**
   * Include the session event log (`events.jsonl`). Defaults to true.
   */
  events?: boolean;
  /**
   * Include process logs for the session. Defaults to true.
   */
  processLogs?: boolean;
  /**
   * Include interactive shell logs written under the session's `shell-logs` directory. Defaults to true.
   */
  shellLogs?: boolean;
  /**
   * Server-local path to the session's events.jsonl file. Internal callers normally omit this and let the runtime derive it from the session.
   */
  eventsPath?: string;
  /**
   * Server-local path to the current process log. When set, it is included as `process.log` and its directory is searched for prior logs from the same session.
   */
  currentProcessLogPath?: string;
  /**
   * Server-local process log directory to search when `currentProcessLogPath` is unavailable, useful for collecting logs for inactive sessions.
   */
  processLogDirectory?: string;
  /**
   * Maximum number of previous process logs to include. Defaults to 5.
   */
  previousProcessLogLimit?: number;
}
/**
 * Options for collecting a redacted session debug bundle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsRequest".
 */
/** @experimental */
export interface DebugCollectLogsRequest {
  destination: DebugCollectLogsDestination;
  include?: DebugCollectLogsInclude;
  /**
   * Caller-provided server-local files or directories to include in addition to the runtime's built-in session diagnostics. This lets host applications add their own diagnostics without changing the API shape.
   */
  additionalEntries?: DebugCollectLogsEntry[];
}
/**
 * Result of collecting a redacted debug bundle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsResult".
 */
/** @experimental */
export interface DebugCollectLogsResult {
  kind: DebugCollectLogsResultKind;
  /**
   * Actual archive path or staging directory path written. This may differ from the requested path when no-overwrite suffixing or fallback-to-temp-directory was needed.
   */
  path: string;
  /**
   * Files included in the redacted bundle.
   */
  entries: DebugCollectLogsCollectedEntry[];
  /**
   * Optional files or directories that could not be included.
   */
  skippedEntries?: DebugCollectLogsSkippedEntry[];
}
/**
 * An optional debug bundle entry that could not be included.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DebugCollectLogsSkippedEntry".
 */
/** @experimental */
export interface DebugCollectLogsSkippedEntry {
  /**
   * Relative path requested for this bundle entry.
   */
  bundlePath: string;
  /**
   * Server-local source path that could not be read.
   */
  path?: string;
  /**
   * Reason the entry was skipped.
   */
  reason: string;
}
/**
 * Discovered extension metadata and persistent enablement state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtension".
 */
/** @experimental */
export interface DiscoveredExtension {
  /**
   * Source-qualified ID accepted by both server and session extension enablement methods
   */
  id: string;
  /**
   * Human-readable extension name
   */
  name: string;
  /**
   * Absolute path to the extension entry module, suitable for revealing it in a file manager
   */
  path: string;
  source: DiscoveredExtensionSource;
  /**
   * Whether this extension's persistent per-ID preference is enabled
   */
  enabled: boolean;
  plugin?: DiscoveredExtensionPlugin;
}
/**
 * Installed plugin that contributes a discovered extension.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtensionPlugin".
 */
/** @experimental */
export interface DiscoveredExtensionPlugin {
  /**
   * Installed plugin name
   */
  name: string;
}
/**
 * Extensions discovered from persisted Copilot home state and their effective loading mode. Launch-scoped additional plugins are not included.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtensions".
 */
/** @experimental */
export interface DiscoveredExtensions {
  /**
   * Discovered user and enabled installed-plugin extensions from persisted Copilot home state
   */
  extensions: DiscoveredExtension[];
  mode: DiscoveredExtensionMode;
}
/**
 * Source-qualified extension identifiers to persistently disable for future sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtensionsDisableRequest".
 */
/** @experimental */
export interface DiscoveredExtensionsDisableRequest {
  /**
   * Source-qualified user or plugin extension IDs to disable
   */
  ids: string[];
}
/**
 * Source-qualified extension identifiers to persistently enable for future sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredExtensionsEnableRequest".
 */
/** @experimental */
export interface DiscoveredExtensionsEnableRequest {
  /**
   * Source-qualified user or plugin extension IDs to enable
   */
  ids: string[];
}
/**
 * One server-discovered hook action from user, repository, plugin, or managed-policy configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredHook".
 */
/** @experimental */
export interface DiscoveredHook {
  /**
   * Deterministic identifier for this server-discovered action row. It remains stable while the project, origin, source, event, action content, and duplicate ordinal are unchanged. This is row identity, not the key persisted in disabledHooks.
   */
  id: string;
  hookType: HookType;
  origin: HookOrigin;
  /**
   * Human-readable source label, such as a hook file path, settings source, or plugin name.
   */
  source?: string;
  /**
   * Input project path for which this server-side action was resolved. Set on every row returned for project-scoped discovery, including repeated user and policy actions.
   */
  projectPath?: string;
  /**
   * Whether this action is enabled under the server-side discovery settings. Concrete sessions may differ because they can add session-specific directories, plugins, or trust. False when its disable key is present in the user's disabled-hooks setting or disable-all settings suppress the action.
   */
  enabled: boolean;
  /**
   * Durable content hash used by hook enablement. Identical actions may intentionally share this key. Omitted when changing the user's disabled-hooks setting cannot change the action's current server-discovered state, including managed-policy hooks, session-start prompt actions, actions suppressed by disable-all settings, and projectless plugin actions that require project-directory expansion.
   */
  disableKey?: string;
}
/**
 * MCP server discovered by `mcp.discover`, with config source, optional plugin source, transport type, and enabled state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "DiscoveredMcpServer".
 */
/** @experimental */
export interface DiscoveredMcpServer {
  /**
   * Server name (config key)
   */
  name: string;
  type?: DiscoveredMcpServerType;
  source: McpServerSource;
  /**
   * Plugin name that provided this server, when source is plugin.
   */
  sourcePlugin?: string;
  /**
   * Plugin version that provided this server, when source is plugin.
   */
  sourcePluginVersion?: string;
  /**
   * Whether the server is enabled (not in the disabled list)
   */
  enabled: boolean;
}
/**
 * Slash-prefixed command string to enqueue for FIFO processing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EnqueueCommandParams".
 */
/** @experimental */
export interface EnqueueCommandParams {
  /**
   * Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO with any in-flight items; if the session is idle, processing kicks off immediately.
   */
  command: string;
  /**
   * Optional user-facing text for the queue row. The command string is shown when omitted.
   */
  displayText?: string | null;
}
/**
 * Indicates whether the command was accepted into the local execution queue.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EnqueueCommandResult".
 */
/** @experimental */
export interface EnqueueCommandResult {
  /**
   * True when the command was accepted into the local execution queue. False when the call targets a session that does not support local command queueing (e.g. remote sessions).
   */
  queued: boolean;
}
/**
 * Cursor, batch size, and optional long-poll/filter parameters for reading session events.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogReadRequest".
 */
/** @experimental */
export interface EventLogReadRequest {
  /**
   * Opaque cursor returned by a previous read. Omit on the first call to start from the beginning of the session's persisted history.
   */
  cursor?: string;
  /**
   * Maximum number of events to return in this batch (1–1000, default 200).
   */
  max?: number;
  /**
   * Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur.
   */
  waitMs?: number;
  types?: EventLogTypes;
  agentScope?: EventsAgentScope;
  /**
   * Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope.
   *
   * @minItems 1
   */
  agentIds?: [string, ...string[]];
  direction?: EventsReadDirection;
  /**
   * When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only.
   */
  includeEphemeral?: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogReleaseInterestResult".
 */
/** @experimental */
export interface EventLogReleaseInterestResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventLogTailResult".
 */
/** @experimental */
export interface EventLogTailResult {
  /**
   * Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).
   */
  cursor: string;
}
/**
 * Batch of session events returned by a read, with cursor and continuation metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "EventsReadResult".
 */
/** @experimental */
export interface EventsReadResult {
  /**
   * Session events for this batch, merged into a single stream in creation order: durable (persisted) events and ephemeral events interleave exactly as they were emitted. Set `includeEphemeral: false` to receive only durable events. Ephemeral events are never replayable once pruned from the in-memory ring, so a consumer that needs them should keep reading with a non-zero `waitMs`. For a backward (tail-first) read, the returned window contains persisted events only, still in chronological (oldest-to-newest) append order.
   */
  events: SessionEvent[];
  /**
   * Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. For a backward read this cursor pages toward OLDER events; keep passing `direction: backward` with it (the cursor is also self-describing, so backward paging continues correctly).
   */
  cursor: string;
  /**
   * True when more events are available in the read's direction. For a forward read, true means the batch returned `max` events and more are available immediately. For a backward read, true means older persisted events remain before the returned window.
   */
  hasMore: boolean;
  cursorStatus: EventsCursorStatus;
}
/**
 * Slash command name and argument string to execute synchronously.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExecuteCommandParams".
 */
/** @experimental */
export interface ExecuteCommandParams {
  /**
   * Name of the slash command to invoke (without the leading '/').
   */
  commandName: string;
  /**
   * Argument string to pass to the command (empty string if none).
   */
  args: string;
}
/**
 * Error message produced while executing the command, if any.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExecuteCommandResult".
 */
/** @experimental */
export interface ExecuteCommandResult {
  /**
   * Error message produced while executing the command, if any. Omitted when the handler succeeded.
   */
  error?: string;
}
/**
 * Discovered extension metadata, including source-qualified ID, name, discovery source, status, and optional process ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Extension".
 */
/** @experimental */
export interface Extension {
  /**
   * Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper', 'plugin:my-plugin:my-ext')
   */
  id: string;
  /**
   * Extension name (directory name)
   */
  name: string;
  source: ExtensionSource;
  status: ExtensionStatus;
  /**
   * Process ID if the extension is running
   */
  pid?: number;
}
/**
 * Slim input shape for extension_context attachments; identity fields are runtime-derived.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionContextPushInput".
 */
/** @experimental */
export interface ExtensionContextPushInput {
  /**
   * Attachment type discriminator
   */
  type: "extension_context";
  /**
   * Human-readable composer pill label
   */
  title: string;
  /**
   * Caller-supplied JSON payload (required, may be null but not undefined)
   */
  payload: JsonValue;
}
/**
 * Opaque integrator-owned process launch profile for one extension entrypoint.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionLaunchProfile".
 */
/** @experimental */
export interface ExtensionLaunchProfile {
  /**
   * Executable used to launch the extension entrypoint.
   */
  executable: string;
  /**
   * Opaque integrator-defined arguments passed to the executable. The runtime does not append the extension entrypoint.
   */
  args: string[];
  /**
   * Opaque integrator-defined environment variables. Runtime-owned COPILOT_SDK_PATH, SESSION_ID, and COPILOT_EXTENSION_PARENT_PID values take precedence.
   */
  env: {
    [k: string]: string | undefined;
  };
}
/**
 * A discovered extension entrypoint that the registered integrator may classify and resolve to an opaque launch profile.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionLaunchProviderResolveRequest".
 */
/** @experimental */
export interface ExtensionLaunchProviderResolveRequest {
  /**
   * Source-qualified extension identifier.
   */
  id: string;
  /**
   * Human-readable extension name.
   */
  name: string;
  /**
   * Absolute path to the discovered extension entrypoint.
   */
  modulePath: string;
  source: ExtensionSource;
}
/**
 * The launch profile for a supported entrypoint. Omit launch when the provider does not support the entrypoint.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionLaunchProviderResolveResult".
 */
/** @experimental */
export interface ExtensionLaunchProviderResolveResult {
  launch?: ExtensionLaunchProfile;
}
/**
 * Extensions discovered for the session, with their current status.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionList".
 */
/** @experimental */
export interface ExtensionList {
  /**
   * Discovered extensions and their current status
   */
  extensions: Extension[];
}
/**
 * Source-qualified extension identifier to disable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionsDisableRequest".
 */
/** @experimental */
export interface ExtensionsDisableRequest {
  /**
   * Source-qualified extension ID to disable
   */
  id: string;
}
/**
 * Source-qualified extension identifier to enable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExtensionsEnableRequest".
 */
/** @experimental */
export interface ExtensionsEnableRequest {
  /**
   * Source-qualified extension ID to enable
   */
  id: string;
}
/**
 * Expanded external tool result payload
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlm".
 */
/** @experimental */
export interface ExternalToolTextResultForLlm {
  /**
   * Text result returned to the model
   */
  textResultForLlm: string;
  /**
   * Execution outcome classification. Optional for back-compat; normalized to 'success' (or 'failure' when error is present) when missing or unrecognized.
   */
  resultType?: string;
  /**
   * Optional error message for failed executions
   */
  error?: string;
  /**
   * Detailed log content for timeline display
   */
  sessionLog?: string;
  /**
   * Optional tool-specific telemetry
   */
  toolTelemetry?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Base64-encoded binary results returned to the model
   */
  binaryResultsForLlm?: ExternalToolTextResultForLlmBinaryResultsForLlm[];
  /**
   * Structured content blocks from the tool
   */
  contents?: ExternalToolTextResultForLlmContent[];
  /**
   * Tool references returned by a tool-search override: names of deferred tools to surface to the model. When set, the tool result is materialized as `tool_reference` content blocks (rather than plain text) so the model knows which deferred tools are now available.
   */
  toolReferences?: string[];
}
/**
 * Binary result returned by a tool for the model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmBinaryResultsForLlm".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmBinaryResultsForLlm {
  type: ExternalToolTextResultForLlmBinaryResultsForLlmType;
  /**
   * Base64-encoded binary data
   */
  data: string;
  /**
   * MIME type of the binary data
   */
  mimeType: string;
  /**
   * Human-readable description of the binary data
   */
  description?: string;
  /**
   * Optional metadata from the producing tool.
   */
  metadata?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * Plain text content block
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentText".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentText {
  /**
   * Content block type discriminator
   */
  type: "text";
  /**
   * The text content
   */
  text: string;
}
/**
 * Terminal/shell output content block with optional exit code and working directory
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentTerminal".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentTerminal {
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
/**
 * Shell command exit metadata with optional output preview
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentShellExit".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentShellExit {
  /**
   * Content block type discriminator
   */
  type: "shell_exit";
  /**
   * Shell id, as assigned by Copilot runtime
   */
  shellId: string;
  /**
   * Exit code from the completed shell command
   */
  exitCode: number;
  /**
   * Working directory where the shell command was executed
   */
  cwd?: string;
  /**
   * Output associated with this shell command, if available. May be partial, truncated, or a preview; not guaranteed to be full output.
   */
  outputPreview?: string;
  /**
   * Whether outputPreview is known to be incomplete or truncated
   */
  outputTruncated?: boolean;
  /**
   * Path reported in the shell session's filesystem namespace when shell output exceeded the configured large-output threshold.
   */
  outputFilePath?: string;
}
/**
 * Image content block with base64-encoded data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentImage".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentImage {
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
/**
 * Audio content block with base64-encoded data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentAudio".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentAudio {
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
/**
 * Resource link content block referencing an external resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceLink".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentResourceLink {
  /**
   * Icons associated with this resource
   */
  icons?: ExternalToolTextResultForLlmContentResourceLinkIcon[];
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
/**
 * Icon image for a resource
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResourceLinkIcon".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentResourceLinkIcon {
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
  theme?: ExternalToolTextResultForLlmContentResourceLinkIconTheme;
}
/**
 * Embedded resource content block with inline text or binary data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ExternalToolTextResultForLlmContentResource".
 */
/** @experimental */
export interface ExternalToolTextResultForLlmContentResource {
  /**
   * Content block type discriminator
   */
  type: "resource";
  resource: ExternalToolTextResultForLlmContentResourceDetails;
}
/**
 * Parameters for cooperatively aborting a factory body.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryAbortRequest".
 */
/** @experimental */
export interface FactoryAbortRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Factory run identifier.
   */
  runId: string;
}
/**
 * Acknowledgement that a factory request was accepted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryAckResult".
 */
/** @experimental */
export interface FactoryAckResult {}
/**
 * Options for one factory-scoped subagent call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryAgentOptions".
 */
/** @experimental */
export interface FactoryAgentOptions {
  /**
   * Optional label distinguishing otherwise identical memoized agent calls.
   */
  label?: string;
  /**
   * Optional JSON Schema for structured agent output.
   */
  schema?: JsonValue;
  /**
   * Optional model identifier for the subagent.
   */
  model?: string;
  /**
   * Optional reasoning effort for the subagent. This field is accepted but not yet honored.
   */
  reasoningEffort?: string;
  contextTier?: ContextTier;
  /**
   * Optional custom agent name for the subagent. This field is accepted but not yet honored.
   */
  agent?: string;
}
/**
 * Parameters for one factory-scoped subagent call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryAgentRequest".
 */
/** @experimental */
export interface FactoryAgentRequest {
  /**
   * Factory run identifier that owns the subagent.
   */
  factoryRunId: string;
  /**
   * Opaque token identifying the current factory execution attempt.
   */
  executionToken: string;
  /**
   * Prompt to send to the subagent.
   */
  prompt: string;
  opts: FactoryAgentOptions;
}
/**
 * Result of one factory-scoped subagent call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryAgentResult".
 */
/** @experimental */
export interface FactoryAgentResult {
  /**
   * Agent result, omitted when the agent produced no result.
   */
  result?: JsonValue;
}
/**
 * Prompt-safe durable identity and live status for a direct factory agent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryAgentSummary".
 */
/** @experimental */
export interface FactoryAgentSummary {
  /**
   * Stable direct-agent identifier.
   */
  agentId: string;
  /**
   * Tool-call identifier that launched the agent.
   */
  toolCallId: string;
  /**
   * Owning factory run identifier.
   */
  runId: string;
  /**
   * Phase identifier active when the agent was launched, or null.
   */
  phaseId: string | null;
  /**
   * Friendly, non-unique name intended for display
   */
  label: string;
  /**
   * Friendly, non-unique name intended for display
   */
  displayName?: string;
  /**
   * Registered agent type.
   */
  agentType: string;
  /**
   * Current durable or live agent status.
   */
  status: string;
  /**
   * Model requested when the agent was launched.
   */
  requestedModel?: string;
  /**
   * Concrete model resolved for the agent.
   */
  resolvedModel?: string;
  /**
   * Epoch milliseconds when the agent started.
   */
  startedAt?: number;
  /**
   * Epoch milliseconds when the agent completed.
   */
  completedAt?: number;
  /**
   * Accumulated active agent time in milliseconds.
   */
  activeMs: number;
  /**
   * Prompt-safe live activity text.
   */
  activity?: string;
}
/**
 * Parameters for cancelling a factory run.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryCancelRequest".
 */
/** @experimental */
export interface FactoryCancelRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
}
/**
 * Current factory phase identity.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryCurrentPhase".
 */
/** @experimental */
export interface FactoryCurrentPhase {
  /**
   * Current phase identifier.
   */
  id: string;
  /**
   * Zero-based declared phase ordinal, or null for an undeclared phase.
   */
  ordinal: number | null;
}
/**
 * Declared or approved factory resource ceilings.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryDeclaredLimits".
 */
/** @experimental */
export interface FactoryDeclaredLimits {
  /**
   * Maximum concurrently active subagents.
   */
  maxConcurrentSubagents?: number;
  /**
   * Maximum total subagents spawned by the run.
   */
  maxTotalSubagents?: number;
  /**
   * Maximum accumulated active execution time in seconds.
   */
  timeoutSeconds?: number;
  /**
   * Maximum AI credits consumed by subagents and descendants.
   */
  maxAiCredits?: number;
}
/**
 * Parameters sent to the owning extension to execute a factory closure.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryExecuteRequest".
 */
/** @experimental */
export interface FactoryExecuteRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Registered factory name.
   */
  name: string;
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Opaque token identifying this factory execution attempt.
   */
  executionToken: string;
  /**
   * Factory input value.
   */
  args: JsonValue;
}
/**
 * Result returned by an extension factory closure.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryExecuteResult".
 */
/** @experimental */
export interface FactoryExecuteResult {
  /**
   * Factory result value.
   */
  result?: JsonValue;
}
/**
 * Parameters for paging factory progress.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryGetRunProgressRequest".
 */
/** @experimental */
export interface FactoryGetRunProgressRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Optional phase identifier used to scope records and cursors.
   */
  phaseId?: string;
  /**
   * Exclusive forward cursor.
   */
  afterSeq?: number;
  /**
   * Exclusive backward cursor.
   */
  beforeSeq?: number;
  /**
   * Maximum records to return. Defaults to 200 and is capped at 500.
   */
  limit?: number;
}
/**
 * Parameters for retrieving a factory run.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryGetRunRequest".
 */
/** @experimental */
export interface FactoryGetRunRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
}
/**
 * Parameters for reading a factory journal entry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryJournalGetRequest".
 */
/** @experimental */
export interface FactoryJournalGetRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Opaque token identifying the current factory execution attempt.
   */
  executionToken: string;
  /**
   * Namespaced journal key.
   */
  key: string;
}
/**
 * Result of reading a factory journal entry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryJournalGetResult".
 */
/** @experimental */
export interface FactoryJournalGetResult {
  /**
   * Whether the journal contained the requested key.
   */
  hit: boolean;
  /**
   * Cached JSON result. The hit field distinguishes a cached JSON null from a miss.
   */
  resultJson?: JsonValue;
}
/**
 * Parameters for storing a factory journal entry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryJournalPutRequest".
 */
/** @experimental */
export interface FactoryJournalPutRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Opaque token identifying the current factory execution attempt.
   */
  executionToken: string;
  /**
   * Namespaced journal key.
   */
  key: string;
  /**
   * JSON result to memoize.
   */
  resultJson: JsonValue;
}
/**
 * Parameters for paging factory runs.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryListRunsRequest".
 */
/** @experimental */
export interface FactoryListRunsRequest {
  /**
   * Exclusive forward cursor.
   */
  afterSeq?: number;
  /**
   * Exclusive backward cursor.
   */
  beforeSeq?: number;
  /**
   * Maximum terminal runs to return. Defaults to 200 and is capped at 500.
   */
  limit?: number;
}
/**
 * A page of factory runs in durable creation order.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryListRunsResult".
 */
/** @experimental */
export interface FactoryListRunsResult {
  /**
   * Factory run summaries in durable creation order.
   */
  runs: FactoryRunSummary[];
  /**
   * Oldest terminal-run cursor in this page, or null when the terminal window is empty.
   */
  oldestSeq?: number | null;
  /**
   * Newest terminal-run cursor in this page, or null when the terminal window is empty.
   */
  newestSeq?: number | null;
  /**
   * Whether terminal runs newer than this page exist.
   */
  hasMoreNewer?: boolean;
  /**
   * Number of terminal runs older than this page.
   */
  omittedOlder?: number;
}
/**
 * Durable factory run summary with read-time live overlays.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunSummary".
 */
/** @experimental */
export interface FactoryRunSummary {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Registered factory name.
   */
  factoryName: string;
  /**
   * Human-readable factory description.
   */
  description: string;
  status: FactoryRunStatus;
  /**
   * Monotonic durable run revision.
   */
  revision: number;
  /**
   * Epoch milliseconds when the run was created.
   */
  createdAt: number;
  /**
   * Epoch milliseconds when execution first started, or null before start.
   */
  startedAt: number | null;
  /**
   * Epoch milliseconds when the durable run was last updated.
   */
  updatedAt: number;
  /**
   * Epoch milliseconds when the run completed, or null while nonterminal.
   */
  completedAt: number | null;
  /**
   * Current phase identity, or null before any phase is entered.
   */
  currentPhase: FactoryCurrentPhase | null;
  /**
   * Number of phases declared by the factory.
   */
  declaredPhaseCount: number;
  /**
   * Number of direct factory agents currently live.
   */
  liveAgentCount: number;
  /**
   * Total direct factory agents spawned across all attempts.
   */
  totalSpawnedAgentCount: number;
  consumed: FactoryRunConsumed;
  declaredLimits: FactoryDeclaredLimits;
  /**
   * Approved effective resource ceilings, or null until approved.
   */
  approved: FactoryDeclaredLimits | null;
  /**
   * Epoch milliseconds when this live-overlay snapshot was observed.
   */
  observedAt: number;
  /**
   * Epoch milliseconds when the current active segment started, or null while inactive.
   */
  activeSegmentStartedAt: number | null;
  /**
   * Terminal run outcome, or null while nonterminal.
   */
  terminal: FactoryRunTerminal | null;
}
/**
 * Durable factory resource consumption.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunConsumed".
 */
/** @experimental */
export interface FactoryRunConsumed {
  /**
   * Accumulated active execution time in milliseconds.
   */
  activeMs: number;
  /**
   * Total subagents spawned by the run.
   */
  subagents: number;
  /**
   * AI usage consumed by the run in nano-AIU.
   */
  nanoAiu: number;
}
/**
 * Prompt-safe terminal factory outcome.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunTerminal".
 */
/** @experimental */
export interface FactoryRunTerminal {
  /**
   * Human-readable terminal reason.
   */
  reason?: string;
  failure?: FactoryRunFailure;
  /**
   * Human-readable terminal error.
   */
  error?: string;
  /**
   * Prompt-safe preview of the completed result.
   */
  resultPreview?: string;
}
/**
 * One ordered factory progress line.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryLogLine".
 */
/** @experimental */
export interface FactoryLogLine {
  /**
   * Monotonic sequence number within the factory run.
   */
  seq: number;
  kind: FactoryLogLineKind;
  /**
   * Progress text.
   */
  text: string;
}
/**
 * Parameters for recording factory progress.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryLogRequest".
 */
/** @experimental */
export interface FactoryLogRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Opaque token identifying the current factory execution attempt.
   */
  executionToken: string;
  /**
   * Ordered progress lines to append.
   */
  lines: FactoryLogLine[];
}
/**
 * Durable lifecycle and timing for one factory phase.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryPhaseObservation".
 */
/** @experimental */
export interface FactoryPhaseObservation {
  /**
   * Phase identifier.
   */
  id: string;
  /**
   * Zero-based declared phase ordinal, or null for an undeclared phase.
   */
  ordinal: number | null;
  /**
   * Human-readable phase title.
   */
  title: string;
  /**
   * Optional human-readable phase detail.
   */
  detail?: string;
  status: FactoryPhaseStatus;
  /**
   * Most recent run attempt that entered this phase, or `0` if the phase has never been entered.
   */
  lastEnteredRunAttempt: number;
  /**
   * Number of times execution entered this phase.
   */
  entryCount: number;
  /**
   * Epoch milliseconds when this phase first started; for a skipped phase, the synthetic skip timestamp (equal to `completedAt`).
   */
  startedAt?: number;
  /**
   * Epoch milliseconds when this phase completed; for a skipped phase, the synthetic skip timestamp (equal to `startedAt`).
   */
  completedAt?: number;
  /**
   * Completed active time accumulated by this phase in milliseconds.
   */
  accumulatedActiveMs: number;
  /**
   * Current live active time for this phase in milliseconds.
   */
  currentActiveMs: number;
  /**
   * Total direct agents associated with this phase.
   */
  totalAgentCount: number;
  /**
   * Direct agents in this phase that are currently live.
   */
  liveAgentCount: number;
}
/**
 * One durable factory progress record.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryProgressLine".
 */
/** @experimental */
export interface FactoryProgressLine {
  /**
   * Global monotonic sequence number within the run.
   */
  seq: number;
  /**
   * Resume attempt that emitted this record.
   */
  attempt: number;
  /**
   * Phase active when the record was emitted, or null before any phase.
   */
  phaseId: string | null;
  /**
   * Epoch milliseconds when the record was persisted.
   */
  recordedAt: number;
  kind: FactoryLogLineKind;
  /**
   * Prompt-safe progress text.
   */
  text: string;
}
/**
 * A bidirectional page of factory progress.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryProgressPage".
 */
/** @experimental */
export interface FactoryProgressPage {
  /**
   * Progress records in sequence order.
   */
  records: FactoryProgressLine[];
  /**
   * Oldest sequence number in this page, or null when empty.
   */
  oldestSeq: number | null;
  /**
   * Newest sequence number in this page, or null when empty.
   */
  newestSeq: number | null;
  /**
   * Whether progress records older than this page exist.
   */
  hasMoreOlder: boolean;
  /**
   * Whether progress records newer than this page exist.
   */
  hasMoreNewer: boolean;
  /**
   * Run revision reflected by this page.
   */
  revision: number;
}
/**
 * Parameters for resuming a factory run from its persisted identity.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryResumeRequest".
 */
/** @experimental */
export interface FactoryResumeRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
  limits?: FactoryRunLimits;
  /**
   * Whether to notify the originating session when the factory completes.
   */
  notifyOnComplete?: boolean;
  /**
   * Whether to emit factory phase names to the session transcript.
   */
  logPhaseNames?: boolean;
}
/**
 * Wire-only per-invocation factory resource ceiling overrides.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunLimits".
 */
/** @experimental */
export interface FactoryRunLimits {
  /**
   * Maximum number of factory subagents that may run concurrently.
   */
  maxConcurrentSubagents?: number;
  /**
   * Maximum total number of factory subagents that may be admitted.
   */
  maxTotalSubagents?: number;
  /**
   * Maximum accumulated active-execution time in seconds. Active execution includes the entire extension body, subprocess waits, queued-agent waits, and sleeps; time between resumed attempts is not counted.
   */
  timeoutSeconds?: number;
  /**
   * Maximum AI credits consumed by factory subagents and their descendants. The post-paid ceiling is soft: parallel turns can settle beyond it before the run stops.
   */
  maxAiCredits?: number;
}
/**
 * Resolved persisted factory identity and resumed run envelope.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryResumeResult".
 */
/** @experimental */
export interface FactoryResumeResult {
  /**
   * Persisted factory name resolved for the resumed run.
   */
  factoryName: string;
  run: FactoryRunResult;
}
/**
 * Complete current or terminal factory run envelope.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunResult".
 */
/** @experimental */
export interface FactoryRunResult {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
   */
  attempt?: number;
  status: FactoryRunStatus;
  /**
   * Completed factory result.
   */
  result?: JsonValue;
  /**
   * Error message for an errored run.
   */
  error?: string;
  failure?: FactoryRunFailure;
  /**
   * Reason for a halted or cancelled run.
   */
  reason?: string;
  /**
   * Partial journal and progress snapshot for a halted, cancelled, or errored run.
   */
  snapshot?: JsonValue;
}
/**
 * Full factory run observability detail.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunDetail".
 */
/** @experimental */
export interface FactoryRunDetail {
  /**
   * Factory run identifier.
   */
  runId: string;
  /**
   * Registered factory name.
   */
  factoryName: string;
  /**
   * Human-readable factory description.
   */
  description: string;
  status: FactoryRunStatus;
  /**
   * Monotonic durable run revision.
   */
  revision: number;
  /**
   * Epoch milliseconds when the run was created.
   */
  createdAt: number;
  /**
   * Epoch milliseconds when execution first started, or null before start.
   */
  startedAt: number | null;
  /**
   * Epoch milliseconds when the durable run was last updated.
   */
  updatedAt: number;
  /**
   * Epoch milliseconds when the run completed, or null while nonterminal.
   */
  completedAt: number | null;
  /**
   * Current phase identity, or null before any phase is entered.
   */
  currentPhase: FactoryCurrentPhase | null;
  /**
   * Number of phases declared by the factory.
   */
  declaredPhaseCount: number;
  /**
   * Number of direct factory agents currently live.
   */
  liveAgentCount: number;
  /**
   * Total direct factory agents spawned across all attempts.
   */
  totalSpawnedAgentCount: number;
  consumed: FactoryRunConsumed;
  declaredLimits: FactoryDeclaredLimits;
  /**
   * Approved effective resource ceilings, or null until approved.
   */
  approved: FactoryDeclaredLimits | null;
  /**
   * Epoch milliseconds when this live-overlay snapshot was observed.
   */
  observedAt: number;
  /**
   * Epoch milliseconds when the current active segment started, or null while inactive.
   */
  activeSegmentStartedAt: number | null;
  /**
   * Terminal run outcome, or null while nonterminal.
   */
  terminal: FactoryRunTerminal | null;
  /**
   * Lifecycle and timing observations for each factory phase.
   */
  phases: FactoryPhaseObservation[];
  /**
   * Durable identities and live statuses for direct factory agents.
   */
  agents: FactoryAgentSummary[];
  progress: FactoryProgressPage;
}
/**
 * Parameters for invoking a registered factory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryRunRequest".
 */
/** @experimental */
export interface FactoryRunRequest {
  /**
   * Registered factory name.
   */
  name: string;
  /**
   * Factory input value.
   */
  args: JsonValue;
  options?: RunOptions;
}
/**
 * Options controlling factory invocation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RunOptions".
 */
/** @experimental */
export interface RunOptions {
  limits?: FactoryRunLimits;
  /**
   * Whether to notify the originating session when the factory completes.
   */
  notifyOnComplete?: boolean;
  /**
   * Whether to emit factory phase names to the session transcript.
   */
  logPhaseNames?: boolean;
  /**
   * Run identifier whose journal and progress should seed this resumed run.
   */
  resumeFromRunId?: string;
}
/**
 * Internal parameters for resuming a factory run from a tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryToolResumeRequest".
 */
/** @experimental */
/** @internal */
export interface FactoryToolResumeRequest {
  /**
   * Factory run identifier.
   */
  runId: string;
  limits?: FactoryRunLimits;
  /**
   * Opaque identifier of the originating tool call.
   */
  toolCallId?: string;
}
/**
 * Options for an internal tool-originated factory invocation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryToolRunOptions".
 */
/** @experimental */
/** @internal */
export interface FactoryToolRunOptions {
  limits?: FactoryRunLimits;
  /**
   * Run identifier whose journal and progress should seed this resumed run.
   */
  resumeFromRunId?: string;
}
/**
 * Internal parameters for invoking a registered factory from a tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FactoryToolRunRequest".
 */
/** @experimental */
/** @internal */
export interface FactoryToolRunRequest {
  /**
   * Registered factory name.
   */
  name: string;
  /**
   * Factory input value.
   */
  args: JsonValue;
  options?: FactoryToolRunOptions;
  /**
   * Opaque identifier of the originating tool call.
   */
  toolCallId?: string;
}
/**
 * Optional user prompt to combine with the fleet orchestration instructions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FleetStartRequest".
 */
/** @experimental */
export interface FleetStartRequest {
  /**
   * Optional user prompt to combine with fleet instructions
   */
  prompt?: string;
}
/**
 * Indicates whether fleet mode was successfully activated.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FleetStartResult".
 */
/** @experimental */
export interface FleetStartResult {
  /**
   * Whether fleet mode was successfully activated
   */
  started: boolean;
}
/**
 * Folder path to add to trusted folders.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FolderTrustAddParams".
 */
/** @experimental */
export interface FolderTrustAddParams {
  /**
   * Folder path to mark as trusted
   */
  path: string;
}
/**
 * Folder path to check for trust.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FolderTrustCheckParams".
 */
/** @experimental */
export interface FolderTrustCheckParams {
  /**
   * Folder path to check
   */
  path: string;
}
/**
 * Folder trust check result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "FolderTrustCheckResult".
 */
/** @experimental */
export interface FolderTrustCheckResult {
  /**
   * Whether the folder is trusted
   */
  trusted: boolean;
}
/**
 * Client environment metadata describing the process that produced a telemetry event.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GitHubTelemetryClientInfo".
 */
/** @experimental */
export interface GitHubTelemetryClientInfo {
  /**
   * Copilot CLI version string.
   */
  cli_version: string;
  /**
   * Operating system platform (e.g. darwin, linux, win32).
   */
  os_platform: string;
  /**
   * Operating system version string.
   */
  os_version: string;
  /**
   * Operating system architecture (e.g. arm64, x64).
   */
  os_arch: string;
  /**
   * Node.js runtime version string.
   */
  node_version: string;
  /**
   * Copilot subscription plan, when known.
   */
  copilot_plan?: string;
  /**
   * Type of client.
   */
  client_type?: string;
  /**
   * Name of the client application.
   */
  client_name?: string;
  /**
   * Whether the user is a GitHub/Microsoft staff member.
   */
  is_staff?: boolean;
  /**
   * Stable machine identifier for the device.
   */
  dev_device_id?: string;
  /**
   * Distinct CPU model names for the host, comma-separated.
   */
  cpu_model?: string;
  /**
   * Number of logical CPU cores on the host.
   */
  cpu_count?: number;
}
/**
 * A single telemetry event in the runtime's native GitHub-shaped telemetry format, forwarded verbatim to opted-in hosts. The `restricted` flag on the enclosing GitHubTelemetryNotification distinguishes standard from restricted events; the payload shape is identical for both.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GitHubTelemetryEvent".
 */
/** @experimental */
export interface GitHubTelemetryEvent {
  /**
   * Event type/kind (e.g. get_completion_with_tools_turn, tool_call_executed).
   */
  kind: string;
  /**
   * Timestamp when the event was created (ISO 8601 format).
   */
  created_at?: string;
  /**
   * Reference to the model call that produced this event.
   */
  model_call_id?: string;
  /**
   * String-valued properties as a map from key to value.
   */
  properties: {
    [k: string]: string | undefined;
  };
  /**
   * Numeric metrics as a map from key to value.
   */
  metrics: {
    [k: string]: number | undefined;
  };
  /**
   * Experiment assignment context.
   */
  exp_assignment_context?: string;
  /**
   * Feature flags enabled for this session, as a map from flag to value.
   */
  features?: {
    [k: string]: string | undefined;
  };
  /**
   * Session identifier the event belongs to.
   */
  session_id?: string;
  /**
   * Copilot tracking ID for user-level attribution.
   */
  copilot_tracking_id?: string;
  client?: GitHubTelemetryClientInfo;
}
/**
 * Payload for a `gitHubTelemetry.event` notification: a single GitHub telemetry event the runtime forwards to a host connection that opted into telemetry forwarding during the `server.connect` handshake.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GitHubTelemetryNotification".
 */
/** @experimental */
export interface GitHubTelemetryNotification {
  /**
   * Session the telemetry event belongs to, when it is session-scoped. Omitted for sessionless events (for example, `server.sendTelemetry` calls with no session id), which are still forwarded to opted-in connections.
   */
  sessionId?: string;
  /**
   * Whether this is a restricted telemetry event (cli.restricted_telemetry). Hosts must route restricted events to first-party Microsoft stores only.
   */
  restricted: boolean;
  event: GitHubTelemetryEvent;
}
/**
 * Asks the SDK client to acquire a GitHub access token from an opaque callback registration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "GitHubTokenAcquireRequest".
 */
/** @experimental */
export interface GitHubTokenAcquireRequest {
  /**
   * Opaque identifier generated by the SDK for this callback registration.
   */
  registrationId: string;
  /**
   * Effective GitHub host for which the callback must return a token.
   */
  host: string;
  /**
   * Session receiving the token. Absent only before a cloud session has been assigned its id.
   */
  sessionId?: string;
  reason: GitHubTokenAcquireReason;
}
/**
 * Pending external tool call request ID, with the tool result or an error describing why it failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HandlePendingToolCallRequest".
 */
/** @experimental */
export interface HandlePendingToolCallRequest {
  /**
   * Request ID of the pending tool call
   */
  requestId: string;
  result?: ExternalToolResult;
  /**
   * Error message if the tool call failed
   */
  error?: string;
}
/**
 * Indicates whether the external tool call result was handled successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HandlePendingToolCallResult".
 */
/** @experimental */
export interface HandlePendingToolCallResult {
  /**
   * Whether the tool call result was handled successfully
   */
  success: boolean;
}
/**
 * Indicates whether an in-progress manual compaction was aborted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryAbortManualCompactionResult".
 */
/** @experimental */
export interface HistoryAbortManualCompactionResult {
  /**
   * Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.
   */
  aborted: boolean;
}
/**
 * Indicates whether an in-progress background compaction was cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCancelBackgroundCompactionResult".
 */
/** @experimental */
export interface HistoryCancelBackgroundCompactionResult {
  /**
   * Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.
   */
  cancelled: boolean;
}
/**
 * Parameters for clearing the conversation and seeding the window that replaces it.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryClearContextRequest".
 */
/** @experimental */
export interface HistoryClearContextRequest {
  /**
   * First user message of the fresh context window. Required: a cleared window holding only system and developer messages is not a conversation a model can answer, so every clear seeds the window it creates. Delivered by the enclosing turn driver once the agentic loop exits, which is why the call must be made from inside a tool handler.
   */
  prompt: string;
}
/**
 * What a successful clear removed. A clear that could not be applied rejects instead of reporting a count.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryClearContextResult".
 */
/** @experimental */
export interface HistoryClearContextResult {
  /**
   * Number of non-system, non-developer messages that were removed from the conversation. Zero only when the window already held no conversation.
   */
  messagesCleared: number;
}
/**
 * Post-compaction context window usage breakdown
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactContextWindow".
 */
/** @experimental */
export interface HistoryCompactContextWindow {
  /**
   * Maximum token count for the model's context window
   */
  tokenLimit: number;
  /**
   * Current total tokens in the context window (system + conversation + tool definitions)
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
}
/**
 * Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryCompactResult".
 */
/** @experimental */
export interface HistoryCompactResult {
  /**
   * Whether compaction completed successfully
   */
  success: boolean;
  /**
   * Number of tokens freed by compaction
   */
  tokensRemoved: number;
  /**
   * Number of messages removed during compaction
   */
  messagesRemoved: number;
  /**
   * Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).
   */
  summaryContent?: string;
  contextWindow?: HistoryCompactContextWindow;
}
/**
 * Rewind points and file-change-tracking availability for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryListRewindPointsResult".
 */
/** @experimental */
export interface HistoryListRewindPointsResult {
  /**
   * Whether this session captured file changes from its first turn.
   */
  fileChangeTrackingEnabled: boolean;
  unavailableReason?: HistoryRewindUnavailableReason;
  /**
   * Root user turns in chronological order. Empty when `unavailableReason` is set.
   */
  points: HistoryRewindPoint[];
}
/**
 * A root user turn that the session can rewind to.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindPoint".
 */
/** @experimental */
export interface HistoryRewindPoint {
  /**
   * ID of the user.message event that begins the discarded suffix.
   */
  eventId: string;
  /**
   * User-visible message text for the turn.
   */
  userMessage: string;
  /**
   * ISO timestamp of the user turn.
   */
  timestamp: string;
  /**
   * Whether at least one file in this turn or a later turn can be restored.
   */
  canRestoreFiles: boolean;
  /**
   * Number of unique files in this turn and all later turns that have captured changes.
   */
  fileCount: number;
  /**
   * Whether this turn itself captured any file changes.
   */
  turnChangedFiles: boolean;
  /**
   * Lines added by this turn's captured file changes.
   */
  linesAdded: number;
  /**
   * Lines removed by this turn's captured file changes.
   */
  linesRemoved: number;
  /**
   * Whether this turn was an automatically injected autopilot continuation.
   */
  isAutopilotContinuation: boolean;
}
/**
 * Event boundary to preview for conversation-and-files rewind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryPreviewRewindRequest".
 */
/** @experimental */
export interface HistoryPreviewRewindRequest {
  /**
   * ID of the user.message event that begins the discarded suffix.
   */
  eventId: string;
}
/**
 * Files and aggregate changes for a prospective rewind.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryPreviewRewindResult".
 */
/** @experimental */
export interface HistoryPreviewRewindResult {
  /**
   * Whether file restore is available for this session. This is authoritative: switch on it and read `reason` only when it is false.
   */
  available: boolean;
  reason?: HistoryRewindUnavailableReason;
  /**
   * Number of unique files in the preview.
   */
  fileCount: number;
  /**
   * Files ordered by path.
   */
  files: HistoryRewindFilePreview[];
}
/**
 * A file that a conversation-and-files rewind would restore.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindFilePreview".
 */
/** @experimental */
export interface HistoryRewindFilePreview {
  /**
   * Absolute path of the captured file.
   */
  path: string;
  changeType: HistoryRewindChangeType;
  /**
   * Lines added across the discarded turns.
   */
  linesAdded: number;
  /**
   * Lines removed across the discarded turns.
   */
  linesRemoved: number;
}
/**
 * Boundary and mode for rewinding session history.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindRequest".
 */
/** @experimental */
export interface HistoryRewindRequest {
  /**
   * ID of the user.message event that begins the discarded suffix.
   */
  eventId: string;
  mode: HistoryRewindMode;
}
/**
 * Structured outcome of a rewind request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryRewindResult".
 */
/** @experimental */
export interface HistoryRewindResult {
  outcome: HistoryRewindOutcome;
  /**
   * Number of persisted events removed by conversation truncation. Present only when truncation succeeded (outcomes `success`, `checkpoint-cleanup-failed`, and `snapshot-prune-failed`); omitted for every unavailable outcome (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`) and for `truncation-failed`, `files-rolled-back`, and `rollback-incomplete`.
   */
  eventsRemoved?: number;
  /**
   * Absolute paths restored to their captured preimages. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it.
   */
  restoredFiles: string[];
  /**
   * Captured files intentionally left unchanged. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it.
   */
  skippedFiles: HistorySkippedFileRestore[];
  /**
   * Failure detail. Set only for the failure and partial-failure outcomes (`files-rolled-back`, `rollback-incomplete`, `truncation-failed`, `checkpoint-cleanup-failed`, `snapshot-prune-failed`); omitted for `success` and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`).
   */
  error?: string;
}
/**
 * A captured file that rewind intentionally left unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistorySkippedFileRestore".
 */
/** @experimental */
export interface HistorySkippedFileRestore {
  /**
   * Absolute path of the skipped file.
   */
  path: string;
  reason: HistoryFileRestoreSkipReason;
}
/**
 * Markdown summary of the conversation context (empty when not available).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistorySummarizeForHandoffResult".
 */
/** @experimental */
export interface HistorySummarizeForHandoffResult {
  /**
   * Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.
   */
  summary: string;
}
/**
 * Identifier of the event to truncate to; this event and all later events are removed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryTruncateRequest".
 */
/** @experimental */
export interface HistoryTruncateRequest {
  /**
   * Event ID to truncate to. This event and all events after it are removed from the session.
   */
  eventId: string;
}
/**
 * Number of events that were removed by the truncation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HistoryTruncateResult".
 */
/** @experimental */
export interface HistoryTruncateResult {
  /**
   * Number of events that were removed
   */
  eventsRemoved: number;
  /**
   * True when conversation truncation succeeded but post-truncation workspace checkpoint cleanup failed. History is already truncated; callers may still prune snapshots but should report a checkpoint-cleanup rather than a truncation failure.
   */
  checkpointCleanupFailed?: boolean;
  /**
   * Failure detail when checkpointCleanupFailed is true.
   */
  checkpointCleanupError?: string;
}
/**
 * Runtime-owned wire payload for a server-to-client hook callback invocation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HookInvokeRequest".
 */
/** @experimental */
/** @internal */
export interface HookInvokeRequest {
  sessionId: string;
  hookType: HookType;
  input: JsonValue;
}
/**
 * Optional output returned by an SDK callback hook.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HookInvokeResponse".
 */
/** @experimental */
/** @internal */
export interface HookInvokeResponse {
  output?: JsonValue;
}
/**
 * Optional project paths and host-exclusion behavior for server-scoped hook discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HooksDiscoverRequest".
 */
/** @experimental */
export interface HooksDiscoverRequest {
  /**
   * Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion.
   */
  projectPaths?: string[];
  /**
   * When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings.
   */
  excludeHostHooks?: boolean;
}
/**
 * Server-discovered hook actions and partial-load diagnostics from user, repository, plugin, and managed-policy sources. Concrete sessions may include additional session-specific hook sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "HooksDiscoverResult".
 */
/** @experimental */
export interface HooksDiscoverResult {
  /**
   * All discovered hook actions. Byte-identical actions remain separate rows even when they share a disable key.
   */
  hooks: DiscoveredHook[];
  /**
   * Non-fatal source-loading warnings. Discovery remains complete for the affected source, although the source had a recoverable issue. Repository-settings warnings are prefixed with their project path when attribution is available.
   */
  warnings: string[];
  /**
   * Errors for hook sources or actions that could not be loaded, making the result partially incomplete. Other valid actions are still returned. Project-resolution and repository-settings errors are prefixed with their project path.
   */
  errors: string[];
}
/**
 * Installed plugin record from global state, with marketplace, version, install time, enabled state, cache path, and source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPlugin".
 */
/** @experimental */
export interface InstalledPlugin {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from (empty string for direct repo installs)
   */
  marketplace: string;
  /**
   * Version installed (if available)
   */
  version?: string;
  /**
   * Installation timestamp
   */
  installed_at: string;
  /**
   * Whether the plugin is currently enabled
   */
  enabled: boolean;
  /**
   * Path where the plugin is cached locally
   */
  cache_path?: string;
  source?: InstalledPluginSource;
  /**
   * Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs.
   */
  source_sha?: string;
  /**
   * Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key.
   */
  installed_from?: string;
}
/**
 * Source descriptor for a direct GitHub plugin install, with `owner/repo`, optional ref or full commit SHA, and optional subpath.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSourceGitHub".
 */
/** @experimental */
export interface InstalledPluginSourceGitHub {
  /**
   * Constant value. Always "github".
   */
  source: "github";
  /**
   * GitHub repository in `owner/repo` form.
   */
  repo: string;
  /**
   * Optional Git ref to resolve.
   */
  ref?: string;
  /**
   * Optional full 40-character hexadecimal commit SHA.
   */
  sha?: string;
  /**
   * Optional repository-relative path to the plugin.
   */
  path?: string;
}
/**
 * Source descriptor for a direct URL plugin install, with URL, optional ref or full commit SHA, and optional subpath.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSourceUrl".
 */
/** @experimental */
export interface InstalledPluginSourceUrl {
  /**
   * Constant value. Always "url".
   */
  source: "url";
  /**
   * URL of the plugin source.
   */
  url: string;
  /**
   * Optional Git ref to resolve.
   */
  ref?: string;
  /**
   * Optional full 40-character hexadecimal commit SHA.
   */
  sha?: string;
  /**
   * Optional source-relative path to the plugin.
   */
  path?: string;
}
/**
 * Source descriptor for a direct local plugin install, with a local filesystem path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginSourceLocal".
 */
/** @experimental */
export interface InstalledPluginSourceLocal {
  /**
   * Constant value. Always "local".
   */
  source: "local";
  /**
   * Local filesystem path to the plugin.
   */
  path: string;
}
/**
 * Information about an installed plugin tracked in global state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstalledPluginInfo".
 */
/** @experimental */
export interface InstalledPluginInfo {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from. Empty string ("") for direct repo / URL / local installs.
   */
  marketplace: string;
  /**
   * Opaque, stable hash identifying a direct (non-marketplace) install source. Present only for direct repo / URL / local installs; absent for marketplace plugins. Same source yields the same id; distinct sources never collide.
   */
  directSourceId?: string;
  /**
   * Installed version (when reported by the plugin manifest)
   */
  version?: string;
  /**
   * Whether the plugin is currently enabled for new sessions
   */
  enabled: boolean;
  /**
   * Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — a plugin belonging to a directory/local marketplace, which is loaded from its real directory on every pass instead of a copy under the installed-plugins cache. Its presence is what marks a listed plugin as live: such a plugin is always present on disk, so `enabled` is its only meaningful state and it is never "not installed".
   */
  installedFrom?: string;
}
/**
 * Canonical file or directory where custom instructions can be discovered or created, with location, kind, preference, and project path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionDiscoveryPath".
 */
/** @experimental */
export interface InstructionDiscoveryPath {
  /**
   * Absolute path of the file or directory (may not exist on disk yet)
   */
  path: string;
  location: InstructionDiscoveryPathLocation;
  kind: InstructionDiscoveryPathKind;
  /**
   * Whether this is the canonical target to create new instructions in its tier. At most one entry per tier is preferred.
   */
  preferredForCreation: boolean;
  /**
   * The input project path this target was derived from (only for repository targets)
   */
  projectPath?: string;
}
/**
 * Canonical files and directories where custom instructions can be created so the runtime will recognize them.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionDiscoveryPathList".
 */
/** @experimental */
export interface InstructionDiscoveryPathList {
  /**
   * Canonical instruction create/discovery files and directories, in priority order
   */
  paths: InstructionDiscoveryPath[];
}
/**
 * Optional project paths to include in instruction discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsDiscoverRequest".
 */
/** @experimental */
export interface InstructionsDiscoverRequest {
  /**
   * Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan).
   */
  projectPaths?: string[];
  /**
   * When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments.
   */
  excludeHostInstructions?: boolean;
}
/**
 * Optional project paths to include when enumerating instruction discovery targets.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsGetDiscoveryPathsRequest".
 */
/** @experimental */
export interface InstructionsGetDiscoveryPathsRequest {
  /**
   * Optional list of project directory paths. When omitted or empty, only the user-level targets are returned.
   */
  projectPaths?: string[];
  /**
   * When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`).
   */
  excludeHostInstructions?: boolean;
}
/**
 * Instruction sources loaded for the session, in merge order.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionsGetSourcesResult".
 */
/** @experimental */
export interface InstructionsGetSourcesResult {
  /**
   * Instruction sources for the session
   */
  sources: InstructionSource[];
}
/**
 * Loaded instruction source for a session, including path, content, category, location, applicability, and optional description.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InstructionSource".
 */
/** @experimental */
export interface InstructionSource {
  /**
   * Unique identifier for this source (used for toggling)
   */
  id: string;
  /**
   * Human-readable label
   */
  label: string;
  /**
   * File path relative to repo or absolute for home
   */
  sourcePath: string;
  /**
   * Raw content of the instruction file
   */
  content: string;
  type: InstructionSourceType;
  location: InstructionSourceLocation;
  /**
   * Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files
   */
  applyTo?: string[];
  /**
   * Short description (body after frontmatter) for use in instruction tables
   */
  description?: string;
  /**
   * When true, this source starts disabled and must be toggled on by the user
   */
  defaultDisabled?: boolean;
  /**
   * The project path this source was discovered from. Only set by sessionless discovery for repository, working-directory, and project-scoped plugin sources, where it disambiguates sources across multiple workspace roots. The session-scoped getSources leaves it unset.
   */
  projectPath?: string;
}
/**
 * Parameters for interrupting the main agent turn.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InterruptMainTurnRequest".
 */
/** @experimental */
export interface InterruptMainTurnRequest {
  /**
   * When true, the user's queued prompts are preserved and run as the next turn once the interrupted turn unwinds; when false (the default), the queue is cleared like a plain abort.
   */
  flushQueued?: boolean;
}
/**
 * Result of interrupting the main agent turn.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "InterruptMainTurnResult".
 */
/** @experimental */
export interface InterruptMainTurnResult {
  /**
   * Whether an in-flight main agent turn was interrupted. False when the main loop was not processing.
   */
  interrupted: boolean;
}
/**
 * HTTP headers as a map from lowercased header name to a list of values. Multi-valued headers (e.g. Set-Cookie) preserve all values.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHeaders".
 */
/** @experimental */
export interface LlmInferenceHeaders {
  [k: string]: string[] | undefined;
}
/**
 * A request body chunk or cancellation signal.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpRequestChunkRequest".
 */
/** @experimental */
export interface LlmInferenceHttpRequestChunkRequest {
  /**
   * Matches the requestId from the originating httpRequestStart frame.
   */
  requestId: string;
  /**
   * Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty.
   */
  data: string;
  /**
   * When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text.
   */
  binary?: boolean;
  /**
   * When true, this is the final body chunk for the request. The SDK may rely on having received an end-marked chunk before treating the request body as complete.
   */
  end?: boolean;
  /**
   * When true, the runtime is cancelling the in-flight request (e.g. upstream consumer aborted). `data` is ignored. Implies end-of-request.
   */
  cancel?: boolean;
  /**
   * Optional human-readable reason for the cancellation, propagated for logging.
   */
  cancelReason?: string;
  /**
   * Identity of the agent invocation (one agentic loop) this body chunk belongs to, matching the `agentInvocationId` semantics on httpRequestStart. Carried per chunk so a persistent transport can attribute successive turns correctly: when a WebSocket connection is reused across turns, the httpRequestStart identity reflects only the turn that opened the connection, so each later turn stamps its own invocation id here. Absent when the runtime has no invocation context for the request, or on the plain-HTTP transport where every request has its own httpRequestStart.
   */
  agentInvocationId?: string;
}
/**
 * Acknowledgement. The SDK is free to ignore the ack and treat chunk delivery as fire-and-forget.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpRequestChunkResult".
 */
/** @experimental */
export interface LlmInferenceHttpRequestChunkResult {}
/**
 * The head of an outbound model-layer HTTP request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpRequestStartRequest".
 */
/** @experimental */
export interface LlmInferenceHttpRequestStartRequest {
  /**
   * Opaque runtime-minted id, unique per in-flight request. The SDK uses this to correlate httpRequestChunk frames and to address its httpResponseStart / httpResponseChunk replies back to the runtime.
   */
  requestId: string;
  /**
   * Id of the runtime session that triggered this request, when one is in scope. Absent for requests issued outside any session (e.g. startup model-catalog or capability resolution). This is a payload field — not a dispatch key — because the client-global API is registered process-wide rather than per session.
   */
  sessionId?: string;
  /**
   * HTTP method, e.g. GET, POST.
   */
  method: string;
  /**
   * Absolute request URL.
   */
  url: string;
  headers: LlmInferenceHeaders;
  transport?: LlmInferenceHttpRequestStartTransport;
  /**
   * Stable identity of the agent trajectory that issued this request. Present when the request originates from an agent turn; absent for requests outside any agent context. This is the same identity used by lifecycle and bridged session events and remains constant across turns and retries.
   */
  agentId?: string;
  /**
   * Stable identity of the immediate parent trajectory. Present for child trajectories such as subagents and conversation-sampling requests; absent for root-agent and non-agent requests.
   */
  parentAgentId?: string;
  /**
   * Identity of the agent invocation (one agentic loop) that issued this request. It remains fixed across physical retries within the invocation and is distinct from the stable trajectory `agentId`. A caller-supplied invocation id always takes precedence (this covers auxiliary calls that have no model call id). Otherwise, first-party CAPI requests fall back to the runtime's agent task id — the same value the runtime emits as the `X-Agent-Task-Id` header — while custom-provider requests fall back to the model call id.
   */
  agentInvocationId?: string;
  /**
   * Coarse classification of the interaction that produced this request. Open string for forward-compatibility; known values include `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, and `conversation-user`. Absent when the runtime did not classify the request. Comes from the runtime's per-request agent context independently of transport; on the CAPI transport the runtime derives the upstream `X-Interaction-Type` header from this same context.
   */
  interactionType?: string;
}
/**
 * Acknowledgement. Returning successfully simply means the SDK accepted the start frame; it does not imply the request will succeed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpRequestStartResult".
 */
/** @experimental */
export interface LlmInferenceHttpRequestStartResult {}
/**
 * Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpResponseChunkError".
 */
/** @experimental */
export interface LlmInferenceHttpResponseChunkError {
  /**
   * Human-readable failure description.
   */
  message: string;
  /**
   * Optional machine-readable error code.
   */
  code?: string;
}
/**
 * A response body chunk or terminal error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpResponseChunkRequest".
 */
/** @experimental */
export interface LlmInferenceHttpResponseChunkRequest {
  /**
   * Matches the requestId from the originating httpRequestStart frame.
   */
  requestId: string;
  /**
   * Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty (e.g. when the response body is empty: send a single chunk with empty data and end=true).
   */
  data: string;
  /**
   * When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text.
   */
  binary?: boolean;
  /**
   * When true, this is the final body chunk for the response. The runtime treats the response body as complete after receiving an end-marked chunk.
   */
  end?: boolean;
  error?: LlmInferenceHttpResponseChunkError;
}
/**
 * Whether the chunk was accepted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpResponseChunkResult".
 */
/** @experimental */
export interface LlmInferenceHttpResponseChunkResult {
  /**
   * True when the chunk was matched to a pending request; false when unknown.
   */
  accepted: boolean;
}
/**
 * Response head.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpResponseStartRequest".
 */
/** @experimental */
export interface LlmInferenceHttpResponseStartRequest {
  /**
   * Matches the requestId from the originating httpRequestStart frame.
   */
  requestId: string;
  /**
   * HTTP status code.
   */
  status: number;
  /**
   * Optional HTTP status reason phrase.
   */
  statusText?: string;
  headers: LlmInferenceHeaders;
}
/**
 * Whether the start frame was accepted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceHttpResponseStartResult".
 */
/** @experimental */
export interface LlmInferenceHttpResponseStartResult {
  /**
   * True when the response start was matched to a pending request; false when unknown.
   */
  accepted: boolean;
}
/**
 * Indicates whether the calling client was registered as the LLM inference provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LlmInferenceSetProviderResult".
 */
/** @experimental */
export interface LlmInferenceSetProviderResult {
  /**
   * Whether the provider was set successfully
   */
  success: boolean;
}
/**
 * Persisted local session metadata, including identifiers, timestamps, summary/name, client, context, detached state, and task ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LocalSessionMetadataValue".
 */
/** @experimental */
export interface LocalSessionMetadataValue {
  /**
   * Stable session identifier
   */
  sessionId: string;
  /**
   * Session creation time as an ISO 8601 timestamp
   */
  startTime: string;
  /**
   * Last-modified time of the session's persisted state, as ISO 8601
   */
  modifiedTime: string;
  /**
   * Short summary of the session, when one has been derived
   */
  summary?: string;
  /**
   * Optional human-friendly name set via /rename
   */
  name?: string;
  /**
   * Runtime client name that created/last resumed this session
   */
  clientName?: string;
  /**
   * Always false for local sessions.
   */
  isRemote: false;
  /**
   * True for detached maintenance sessions that should be hidden from normal resume lists.
   */
  isDetached?: boolean;
  context?: SessionContext;
  /**
   * GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.
   */
  mcTaskId?: string;
}
/**
 * Pre-resolved working-directory context for session startup.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionContext".
 */
/** @experimental */
export interface SessionContext {
  /**
   * Most recent working directory for this session
   */
  cwd: string;
  /**
   * Git repository root, if the cwd was inside a git repo
   */
  gitRoot?: string;
  /**
   * Repository slug in `owner/name` form, when known
   */
  repository?: string;
  hostType?: SessionContextHostType;
  /**
   * Active git branch
   */
  branch?: string;
}
/**
 * Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LogRequest".
 */
/** @experimental */
export interface LogRequest {
  /**
   * Human-readable message
   */
  message: string;
  level?: SessionLogLevel;
  /**
   * Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".
   */
  type?: string;
  /**
   * When true, the message is transient and not persisted to the session event log on disk
   */
  ephemeral?: boolean;
  /**
   * Optional URL the user can open in their browser for more details
   */
  url?: string;
  /**
   * Optional actionable tip displayed alongside the message. Only honored on `level: "info"`.
   */
  tip?: string;
}
/**
 * Identifier of the session event that was emitted for the log message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LogResult".
 */
/** @experimental */
export interface LogResult {
  /**
   * The unique identifier of the emitted session event
   */
  eventId: string;
}
/**
 * Parameters for (re)loading the merged LSP configuration set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "LspInitializeRequest".
 */
/** @experimental */
export interface LspInitializeRequest {
  /**
   * Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted.
   */
  workingDirectory?: string;
  /**
   * Git root used as the boundary when traversing for project-level LSP configs (supports monorepos).
   */
  gitRoot?: string;
  /**
   * Force re-initialization even when LSP configs were already loaded for the working directory.
   */
  force?: boolean;
}
/**
 * Validated device-managed settings discovered before a session exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ManagedSettingsReadResult".
 */
/** @experimental */
export interface ManagedSettingsReadResult {
  /**
   * Validated, canonical managed-settings JSON. Omitted when no managed settings were discovered or when discovered settings failed validation.
   */
  settingsJson?: JsonValue;
  /**
   * Discovery or validation error text when managed settings could not be read safely.
   */
  errorMessage?: string;
}
/**
 * Result of registering a new marketplace.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceAddResult".
 */
/** @experimental */
export interface MarketplaceAddResult {
  /**
   * Final name of the marketplace as resolved from its manifest
   */
  name: string;
}
/**
 * Plugins advertised by the marketplace.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceBrowseResult".
 */
/** @experimental */
export interface MarketplaceBrowseResult {
  /**
   * Plugins advertised by the marketplace
   */
  plugins: MarketplacePluginInfo[];
}
/**
 * Plugin entry advertised by a marketplace.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplacePluginInfo".
 */
/** @experimental */
export interface MarketplacePluginInfo {
  /**
   * Plugin name as listed in the marketplace catalog
   */
  name: string;
  /**
   * Short description from the marketplace catalog, when present
   */
  description?: string;
}
/**
 * Registered marketplace summary.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceInfo".
 */
/** @experimental */
export interface MarketplaceInfo {
  /**
   * Marketplace name (matches the @marketplace suffix in plugin specs)
   */
  name: string;
  /**
   * Human-readable description of where the marketplace data is fetched from (e.g. "GitHub: owner/repo").
   */
  source: string;
  /**
   * True when this is a default marketplace shipped with the runtime. Defaults are not removable.
   */
  isDefault?: boolean;
}
/**
 * All registered marketplaces, including built-in defaults.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceListResult".
 */
/** @experimental */
export interface MarketplaceListResult {
  /**
   * Registered marketplaces
   */
  marketplaces: MarketplaceInfo[];
}
/**
 * Per-marketplace refresh result, including marketplace name, success flag, and optional failure error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceRefreshEntry".
 */
/** @experimental */
export interface MarketplaceRefreshEntry {
  /**
   * Marketplace name that was refreshed
   */
  name: string;
  /**
   * Whether the refresh succeeded
   */
  success: boolean;
  /**
   * Error message (failure only)
   */
  error?: string;
}
/**
 * Result of refreshing one or more marketplace catalogs.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceRefreshResult".
 */
/** @experimental */
export interface MarketplaceRefreshResult {
  /**
   * Per-marketplace refresh results in deterministic order.
   */
  results: MarketplaceRefreshEntry[];
}
/**
 * Outcome of the remove attempt, including dependent-plugin info when applicable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MarketplaceRemoveResult".
 */
/** @experimental */
export interface MarketplaceRemoveResult {
  /**
   * True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set.
   */
  removed: boolean;
  /**
   * Names of installed plugins that prevented removal. Populated only when `removed=false`.
   */
  dependentPlugins?: string[];
}
/**
 * MCP server allowed by policy, with server name and optional PII-free explanatory note.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAllowedServer".
 */
/** @experimental */
export interface McpAllowedServer {
  /**
   * Allowed server name
   */
  name: string;
  /**
   * PII-free note explaining why the server was allowed
   */
  redactedNote?: string;
}
/**
 * MCP server, tool name, and arguments to invoke from an MCP App view.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsCallToolRequest".
 */
/** @experimental */
export interface McpAppsCallToolRequest {
  /**
   * MCP server hosting the tool
   */
  serverName: string;
  /**
   * MCP tool name
   */
  toolName: string;
  /**
   * Tool arguments
   */
  arguments?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
   */
  originServerName: string;
}
/**
 * Capability negotiation snapshot
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseCapability".
 */
/** @experimental */
export interface McpAppsDiagnoseCapability {
  /**
   * Whether the session has the `mcp-apps` capability
   */
  sessionHasMcpApps: boolean;
  /**
   * Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on
   */
  featureFlagEnabled: boolean;
  /**
   * Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers
   */
  advertised: boolean;
}
/**
 * MCP server to diagnose MCP Apps wiring for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseRequest".
 */
/** @experimental */
export interface McpAppsDiagnoseRequest {
  /**
   * MCP server to probe
   */
  serverName: string;
}
/**
 * Diagnostic snapshot of MCP Apps wiring for the named server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseResult".
 */
/** @experimental */
export interface McpAppsDiagnoseResult {
  capability: McpAppsDiagnoseCapability;
  server: McpAppsDiagnoseServer;
}
/**
 * What the server returned for this session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsDiagnoseServer".
 */
/** @experimental */
export interface McpAppsDiagnoseServer {
  /**
   * Whether the named server is currently connected
   */
  connected: boolean;
  /**
   * Total tools returned by the server's tools/list
   */
  toolCount: number;
  /**
   * Tools whose `_meta.ui` is populated (resourceUri and/or visibility set)
   */
  toolsWithUiMeta: number;
  /**
   * Up to 5 tool names with `_meta.ui` for quick inspection
   */
  sampleToolNames: string[];
}
/**
 * Current host context advertised to MCP App guests.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContext".
 */
/** @experimental */
export interface McpAppsHostContext {
  context: McpAppsHostContextDetails;
}
/**
 * Current host context
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsHostContextDetails".
 */
/** @experimental */
export interface McpAppsHostContextDetails {
  theme?: McpAppsHostContextDetailsTheme;
  /**
   * BCP-47 locale, e.g. 'en-US'
   */
  locale?: string;
  /**
   * IANA timezone, e.g. 'America/New_York'
   */
  timeZone?: string;
  displayMode?: McpAppsHostContextDetailsDisplayMode;
  /**
   * Display modes the host supports
   */
  availableDisplayModes?: McpAppsHostContextDetailsAvailableDisplayMode[];
  platform?: McpAppsHostContextDetailsPlatform;
  /**
   * Host application identifier
   */
  userAgent?: string;
  [k: string]: unknown | undefined;
}
/**
 * MCP server to list app-callable tools for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsListToolsRequest".
 */
/** @experimental */
export interface McpAppsListToolsRequest {
  /**
   * MCP server hosting the app
   */
  serverName: string;
  /**
   * **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
   */
  originServerName: string;
}
/**
 * App-callable tools from the named MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsListToolsResult".
 */
/** @experimental */
export interface McpAppsListToolsResult {
  /**
   * App-callable tools from the server
   */
  tools: {
    [k: string]: JsonValue | undefined;
  }[];
}
/**
 * MCP server and resource URI to fetch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsReadResourceRequest".
 */
/** @experimental */
export interface McpAppsReadResourceRequest {
  /**
   * Name of the MCP server hosting the resource
   */
  serverName: string;
  /**
   * Resource URI (typically ui://...)
   */
  uri: string;
}
/**
 * Resource contents returned by the MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsReadResourceResult".
 */
/** @experimental */
export interface McpAppsReadResourceResult {
  /**
   * Resource contents returned by the server
   */
  contents: McpAppsResourceContent[];
}
/**
 * MCP Apps resource content with URI, optional MIME type, text or base64 blob, and resource metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsResourceContent".
 */
/** @experimental */
export interface McpAppsResourceContent {
  /**
   * The resource URI (typically ui://...)
   */
  uri: string;
  /**
   * MIME type of the content
   */
  mimeType?: string;
  /**
   * Text content (e.g. HTML)
   */
  text?: string;
  /**
   * Base64-encoded binary content
   */
  blob?: string;
  /**
   * Resource-level metadata (CSP, permissions, etc.)
   */
  _meta?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * Host context advertised to MCP App guests
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextDetails".
 */
/** @experimental */
export interface McpAppsSetHostContextDetails {
  theme?: McpAppsSetHostContextDetailsTheme;
  /**
   * BCP-47 locale, e.g. 'en-US'
   */
  locale?: string;
  /**
   * IANA timezone, e.g. 'America/New_York'
   */
  timeZone?: string;
  displayMode?: McpAppsSetHostContextDetailsDisplayMode;
  /**
   * Display modes the host supports
   */
  availableDisplayModes?: McpAppsSetHostContextDetailsAvailableDisplayMode[];
  platform?: McpAppsSetHostContextDetailsPlatform;
  /**
   * Host application identifier
   */
  userAgent?: string;
  [k: string]: unknown | undefined;
}
/**
 * Host context to advertise to MCP App guests.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpAppsSetHostContextRequest".
 */
/** @experimental */
export interface McpAppsSetHostContextRequest {
  context: McpAppsSetHostContextDetails;
}
/**
 * The requestId previously passed to executeSampling that should be cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpCancelSamplingExecutionParams".
 */
/** @experimental */
export interface McpCancelSamplingExecutionParams {
  /**
   * The requestId previously passed to executeSampling that should be cancelled
   */
  requestId: string;
}
/**
 * Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpCancelSamplingExecutionResult".
 */
/** @experimental */
export interface McpCancelSamplingExecutionResult {
  /**
   * True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).
   */
  cancelled: boolean;
}
/**
 * MCP server name and configuration to add to user configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigAddRequest".
 */
/** @experimental */
export interface McpConfigAddRequest {
  /**
   * Unique name for the MCP server
   */
  name: string;
  config: McpSerializableServerConfig;
}
/**
 * Stdio MCP server configuration launched as a child process.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigStdio".
 */
/** @experimental */
export interface McpServerConfigStdio {
  /**
   * Optional human-readable server name.
   */
  displayName?: string;
  safeForTelemetry?: McpSafeForTelemetry;
  /**
   * Tools to include. Defaults to all tools if not specified.
   */
  tools?: string[];
  /**
   * Whether this server is a built-in fallback used when the user has not configured their own server.
   */
  isDefaultServer?: boolean;
  filterMapping?: FilterMapping;
  /**
   * Timeout in milliseconds for tool discovery and tool calls.
   */
  timeout?: number;
  oidc?: McpServerAuthConfig;
  auth?: McpServerAuthConfig;
  deferTools?: McpServerConfigDeferTools;
  /**
   * Set to true to disable persisted MCP tool snapshots for this server. Live tool discovery is unaffected.
   */
  disableToolCache?: boolean;
  /**
   * Whether secret masking is disabled for calls to this server.
   */
  disableSecretMasking?: boolean;
  /**
   * Tool names excluded after the include filter is applied.
   */
  excludeTools?: string[];
  /**
   * Event types this server receives as Copilot notifications.
   */
  events?: string[];
  /**
   * Copilot notification types this server may send to the host.
   */
  notifications?: string[];
  source?: McpServerSource;
  /**
   * Plugin that provided this server.
   */
  sourcePlugin?: string;
  /**
   * Version of the plugin that provided this server.
   */
  sourcePluginVersion?: string;
  /**
   * Whether the providing plugin uses the Open Plugin Spec.
   */
  sourcePluginSpec?: boolean;
  /**
   * Source file path recorded while loading the config.
   */
  sourcePath?: string;
  /**
   * Configuration warnings recorded while loading the server.
   */
  configWarnings?: string[];
  /**
   * Executable command used to start the Stdio MCP server process.
   */
  command: string;
  /**
   * Command-line arguments passed to the Stdio MCP server process.
   */
  args?: string[];
  /**
   * Working directory for the Stdio MCP server process.
   */
  cwd?: string;
  /**
   * Environment variables to pass to the Stdio MCP server process.
   */
  env?: {
    [k: string]: string | undefined;
  };
  type?: McpServerConfigStdioType;
}
/**
 * Per-field MCP telemetry-obfuscation policy.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSafeForTelemetryFields".
 */
/** @experimental */
export interface McpSafeForTelemetryFields {
  /**
   * Whether the MCP tool name may be included in telemetry without obfuscation.
   */
  name: boolean;
  /**
   * Whether MCP tool input names may be included in telemetry without obfuscation.
   */
  inputsNames: boolean;
}
/**
 * Authentication settings with optional redirect port configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerAuthConfigRedirectPort".
 */
/** @experimental */
export interface McpServerAuthConfigRedirectPort {
  /**
   * Fixed port for the OAuth redirect callback server.
   */
  redirectPort?: number;
}
/**
 * Remote MCP server configuration accessed over HTTP or SSE.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigHttp".
 */
/** @experimental */
export interface McpServerConfigHttp {
  /**
   * Optional human-readable server name.
   */
  displayName?: string;
  safeForTelemetry?: McpSafeForTelemetry;
  /**
   * Tools to include. Defaults to all tools if not specified.
   */
  tools?: string[];
  type?: McpServerConfigHttpType;
  /**
   * Whether this server is a built-in fallback used when the user has not configured their own server.
   */
  isDefaultServer?: boolean;
  filterMapping?: FilterMapping;
  /**
   * Timeout in milliseconds for tool discovery and tool calls.
   */
  timeout?: number;
  oidc?: McpServerAuthConfig;
  auth?: McpServerAuthConfig;
  deferTools?: McpServerConfigDeferTools;
  /**
   * Set to true to disable persisted MCP tool snapshots for this server. Live tool discovery is unaffected.
   */
  disableToolCache?: boolean;
  /**
   * Whether secret masking is disabled for calls to this server.
   */
  disableSecretMasking?: boolean;
  /**
   * Tool names excluded after the include filter is applied.
   */
  excludeTools?: string[];
  /**
   * Event types this server receives as Copilot notifications.
   */
  events?: string[];
  /**
   * Copilot notification types this server may send to the host.
   */
  notifications?: string[];
  source?: McpServerSource;
  /**
   * Plugin that provided this server.
   */
  sourcePlugin?: string;
  /**
   * Version of the plugin that provided this server.
   */
  sourcePluginVersion?: string;
  /**
   * Whether the providing plugin uses the Open Plugin Spec.
   */
  sourcePluginSpec?: boolean;
  /**
   * Source file path recorded while loading the config.
   */
  sourcePath?: string;
  /**
   * Configuration warnings recorded while loading the server.
   */
  configWarnings?: string[];
  /**
   * URL of the remote MCP server endpoint.
   */
  url: string;
  /**
   * HTTP headers to include in requests to the remote MCP server.
   */
  headers?: {
    [k: string]: string | undefined;
  };
  /**
   * Dynamic-header refresh cache lifetime in milliseconds.
   */
  headersRefreshTtlMs?: number;
  /**
   * OAuth client ID for a pre-registered remote MCP OAuth client.
   */
  oauthClientId?: string;
  /**
   * Whether the configured OAuth client is public and does not require a client secret.
   */
  oauthPublicClient?: boolean;
  oauthGrantType?: McpServerConfigHttpOauthGrantType;
}
/**
 * MCP server names to disable for new sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigDisableRequest".
 */
/** @experimental */
export interface McpConfigDisableRequest {
  /**
   * Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.
   */
  names: string[];
}
/**
 * MCP server names to enable for new sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigEnableRequest".
 */
/** @experimental */
export interface McpConfigEnableRequest {
  /**
   * Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.
   */
  names: string[];
}
/**
 * User-configured MCP servers, keyed by server name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigList".
 */
/** @experimental */
export interface McpConfigList {
  /**
   * All MCP servers from user config, keyed by name
   */
  servers: {
    [k: string]: McpSerializableServerConfig;
  };
}
/**
 * MCP server name to remove from user configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigRemoveRequest".
 */
/** @experimental */
export interface McpConfigRemoveRequest {
  /**
   * Name of the MCP server to remove
   */
  name: string;
}
/**
 * MCP server name and replacement configuration to write to user configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigUpdateRequest".
 */
/** @experimental */
export interface McpConfigUpdateRequest {
  /**
   * Name of the MCP server to update
   */
  name: string;
  config: McpSerializableServerConfig;
}
/**
 * Credential-free authentication identity used to configure GitHub MCP.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigureGitHubRequest".
 */
/** @experimental */
/** @internal */
export interface McpConfigureGitHubRequest {
  /**
   * Opaque runtime auth info for GitHub MCP configuration. Marked internal: an in-process runtime shape (configureGitHubMcp is a no-op over the wire).
   *
   * @internal
   */
  authInfo: OpaqueInProcessValue;
}
/**
 * Result of configuring GitHub MCP.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpConfigureGitHubResult".
 */
/** @experimental */
export interface McpConfigureGitHubResult {
  /**
   * Whether GitHub MCP configuration changed.
   */
  changed: boolean;
}
/**
 * Name of the MCP server to disable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpDisableRequest".
 */
/** @experimental */
export interface McpDisableRequest {
  /**
   * Name of the MCP server to disable
   */
  serverName: string;
}
/**
 * Optional working directory used as context for MCP server discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpDiscoverRequest".
 */
/** @experimental */
export interface McpDiscoverRequest {
  /**
   * Working directory used as context for discovery (e.g., plugin resolution)
   */
  workingDirectory?: string;
}
/**
 * MCP servers discovered from user, workspace, plugin, and built-in sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpDiscoverResult".
 */
/** @experimental */
export interface McpDiscoverResult {
  /**
   * MCP servers discovered from all sources
   */
  servers: DiscoveredMcpServer[];
}
/**
 * Name of the MCP server to enable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpEnableRequest".
 */
/** @experimental */
export interface McpEnableRequest {
  /**
   * Name of the MCP server to enable
   */
  serverName: string;
}
/**
 * Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpExecuteSamplingParams".
 */
/** @experimental */
export interface McpExecuteSamplingParams {
  /**
   * Caller-provided unique identifier for this sampling execution. Use this same ID with cancelSamplingExecution to cancel the in-flight call. Must be unique within the session for the lifetime of the call.
   */
  requestId: string;
  /**
   * Name of the MCP server that initiated the sampling request
   */
  serverName: string;
  /**
   * The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate the inference with the originating MCP request for telemetry; this is distinct from `requestId` (which is the schema-level cancellation handle).
   */
  mcpRequestId: JsonValue;
  request: McpExecuteSamplingRequest;
}
/**
 * Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpExecuteSamplingRequest".
 */
/** @experimental */
export interface McpExecuteSamplingRequest {
  [k: string]: unknown | undefined;
}
/**
 * MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpExecuteSamplingResult".
 */
/** @experimental */
export interface McpExecuteSamplingResult {
  [k: string]: unknown | undefined;
}
/**
 * MCP server whose connection attempt failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpFailedServer".
 */
/** @experimental */
export interface McpFailedServer {
  /**
   * The config key of the server that failed to connect.
   */
  name: string;
  /**
   * The captured connection failure detail.
   */
  error?: string;
}
/**
 * MCP server filtered by policy, with name, reason, and optional redacted reason.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpFilteredServer".
 */
/** @experimental */
export interface McpFilteredServer {
  /**
   * Filtered server name
   */
  name: string;
  /**
   * Human-readable filter reason
   */
  reason: string;
  /**
   * PII-free filter reason
   */
  redactedReason?: string;
  /**
   * @deprecated
   * Deprecated. This field is no longer populated.
   */
  enterpriseName?: string;
}
/**
 * MCP headers refresh request id and the host response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpHeadersHandlePendingHeadersRefreshRequestRequest".
 */
/** @experimental */
export interface McpHeadersHandlePendingHeadersRefreshRequestRequest {
  /**
   * Headers refresh request identifier from mcp.headers_refresh_required
   */
  requestId: string;
  result: McpHeadersHandlePendingHeadersRefreshRequest;
}
/**
 * Indicates whether the pending MCP headers refresh response was accepted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpHeadersHandlePendingHeadersRefreshRequestResult".
 */
/** @experimental */
export interface McpHeadersHandlePendingHeadersRefreshRequestResult {
  /**
   * Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
   */
  success: boolean;
}
/**
 * Host-level state, omitted when no MCP host is initialized.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpHostState".
 */
/** @experimental */
export interface McpHostState {
  /**
   * Whether third-party MCP servers are policy-enabled for this session.
   */
  mcp3pEnabled: boolean;
  /**
   * Configured servers that are explicitly disabled.
   */
  disabledServers: string[];
  /**
   * Configured servers filtered out by MCP server policy.
   */
  filteredServers: string[];
  /**
   * Names of currently-connected MCP clients.
   */
  clients: string[];
  /**
   * Names of servers with in-flight connection attempts.
   */
  pendingConnections: string[];
  /**
   * Map of server name to recorded connection failure.
   */
  failedServers: {
    [k: string]: McpServerFailureInfo | undefined;
  };
  /**
   * Map of server name to recorded pending-auth state.
   */
  needsAuthServers: {
    [k: string]: McpServerNeedsAuthInfo | undefined;
  };
}
/**
 * Recorded MCP server connection failure.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerFailureInfo".
 */
/** @experimental */
export interface McpServerFailureInfo {
  /**
   * Failure message produced when the MCP server connection failed.
   */
  message: string;
  /**
   * epoch-ms timestamp at which the failure was recorded.
   */
  timestamp: number;
}
/**
 * Recorded MCP server pending-auth state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerNeedsAuthInfo".
 */
/** @experimental */
export interface McpServerNeedsAuthInfo {
  /**
   * epoch-ms timestamp at which the server signalled it needs authentication.
   */
  timestamp: number;
}
/**
 * A normalised, inert description of what installing an MCP server would involve. Carries no raw card, no install specification, and no secret value.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpInstallPlan".
 */
/** @experimental */
export interface McpInstallPlan {
  /**
   * Opaque, runtime-instance scoped, TTL-bound, single-use handle for this plan. Rejected when stale, replayed, or presented to a different runtime instance. Never logged.
   */
  planHandle: string;
  /**
   * ISO 8601 timestamp after which the plan handle is stale and will be rejected. Abandoning a plan needs no call: an unused handle simply expires, so cancellation before commit is side-effect free.
   */
  planHandleExpiresAt: string;
  identity: McpPlanResourceIdentity;
  provenance: McpPlanProvenance;
  /**
   * Every eligible transport, so a host can present an explicit choice. A completed plan always has at least one; when none is eligible, planning returns `CatalogUnavailableTransportError` instead.
   *
   * @minItems 1
   * @maxItems 50
   */
  transportChoices: [McpPlanTransportChoice, ...McpPlanTransportChoice[]];
  /**
   * Identifier of the choice the runtime would pick by default. Omitted when there is no eligible transport, or when the runtime expresses no preference.
   */
  recommendedTransportChoiceId?: string;
  target: McpPlanTarget;
  policy: McpPlanPolicyResult;
  /**
   * The configuration changes installing would make, described rather than serialised, so the mutable configuration payload stays behind the runtime boundary.
   */
  configurationChanges: McpPlanConfigurationChange[];
  /**
   * Whether applying this plan would require an MCP reload to take effect. Planning itself never reloads.
   */
  reloadRequired: boolean;
  /**
   * Whether the plan cannot be applied without further input, because a required value has no default or a secret must be supplied.
   */
  requiresInteractiveConfiguration: boolean;
}
/**
 * Normalised identity of the MCP server a plan targets, independent of how the card spelled it.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanResourceIdentity".
 */
/** @experimental */
export interface McpPlanResourceIdentity {
  /**
   * Canonical, normalised name of the server, for example `io.github.owner/server`.
   */
  canonicalName: string;
  /**
   * Local configuration key the server would be recorded under.
   */
  serverName: string;
  /**
   * Version advertised by the card, when it declares one.
   */
  version?: string;
  /**
   * Registry identifier of the server, when it came from a registry.
   */
  registryId?: string;
}
/**
 * Provenance of the exact validated JSON MCP card content bound privately to a completed plan and its opaque handle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanProvenance".
 */
/** @experimental */
export interface McpPlanProvenance {
  /**
   * Authority associated with the validated card, without path, query, or credentials. Inert untrusted data.
   */
  authority: string;
  /**
   * ISO 8601 timestamp at which the runtime completed strict parsing and schema validation of the card content.
   */
  validatedAt: string;
  cardDigest: CardDigest;
  mediaType: McpServerCardMediaType;
}
/**
 * An eligible local-package transport choice. Package identity is required and a remote endpoint cannot be represented.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanTransportChoicePackage".
 */
/** @experimental */
export interface McpPlanTransportChoicePackage {
  /**
   * Stable identifier for this choice within the plan, used to select it when the plan is applied.
   */
  choiceId: string;
  transport: McpPlanPackageTransport;
  installMethod: McpPlanPackageInstallMethod;
  /**
   * Packaging ecosystem, for example `oci` or `npm`.
   */
  packageType: string;
  /**
   * Package identifier. Inert untrusted data.
   */
  packageIdentifier: string;
  /**
   * Typed values this choice requires, excluding secrets.
   */
  requiredValues: McpPlanRequiredValue[];
  /**
   * Secrets this choice requires, referenced by placeholder only.
   */
  secretPlaceholders: McpPlanSecretPlaceholder[];
}
/**
 * One non-secret scalar value a transport choice needs before it can be applied.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRequiredValueScalar".
 */
/** @experimental */
export interface McpPlanRequiredValueScalar {
  kind: McpPlanRequiredValueScalarKind;
  /**
   * Key the value is supplied under. Inert untrusted data.
   */
  key: string;
  category: McpPlanValueCategory;
  valueType: McpPlanScalarValueType;
  /**
   * Whether the value must be present for the plan to be applicable.
   */
  required: boolean;
  /**
   * Default supplied by the card, when the value can be resolved without input. Presence is the authoritative indication that a default exists. Inert untrusted data.
   */
  defaultValue?: string;
  /**
   * Human-readable label from the card. Inert untrusted text.
   */
  title?: string;
  /**
   * Human-readable explanation from the card. Inert untrusted text.
   */
  description?: string;
  /**
   * Whether the value may be supplied more than once.
   */
  isRepeated: boolean;
}
/**
 * One enumerated non-secret value a transport choice needs before it can be applied. The permitted values are structurally required.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanRequiredValueEnum".
 */
/** @experimental */
export interface McpPlanRequiredValueEnum {
  kind: McpPlanRequiredValueEnumKind;
  /**
   * Key the value is supplied under. Inert untrusted data.
   */
  key: string;
  category: McpPlanValueCategory;
  valueType: McpPlanEnumValueType;
  /**
   * Whether the value must be present for the plan to be applicable.
   */
  required: boolean;
  /**
   * Default supplied by the card, when the value can be resolved without input. Presence is the authoritative indication that a default exists. Inert untrusted data.
   */
  defaultValue?: string;
  /**
   * Human-readable label from the card. Inert untrusted text.
   */
  title?: string;
  /**
   * Human-readable explanation from the card. Inert untrusted text.
   */
  description?: string;
  /**
   * Non-empty permitted value set. Inert untrusted data.
   *
   * @minItems 1
   */
  enumValues: [string, ...string[]];
  /**
   * Whether the value may be supplied more than once.
   */
  isRepeated: boolean;
}
/**
 * A secret a transport choice needs, referenced by placeholder. No secret value ever appears in a plan, and the placeholder resolves against the keychain only when a plan is applied.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanSecretPlaceholder".
 */
/** @experimental */
export interface McpPlanSecretPlaceholder {
  /**
   * Key the secret is supplied under. Inert untrusted data.
   */
  key: string;
  placeholder: McpPlanSecretReference;
  /**
   * Human-readable label from the card. Inert untrusted text.
   */
  title?: string;
}
/**
 * An eligible remote-endpoint transport choice. The endpoint is required and package identity cannot be represented.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanTransportChoiceRemote".
 */
/** @experimental */
export interface McpPlanTransportChoiceRemote {
  /**
   * Stable identifier for this choice within the plan, used to select it when the plan is applied.
   */
  choiceId: string;
  transport: McpPlanRemoteTransport;
  installMethod: McpPlanRemoteInstallMethod;
  /**
   * Endpoint URL. Inert untrusted data.
   */
  endpoint: string;
  /**
   * Typed values this choice requires, excluding secrets.
   */
  requiredValues: McpPlanRequiredValue[];
  /**
   * Secrets this choice requires, referenced by placeholder only.
   */
  secretPlaceholders: McpPlanSecretPlaceholder[];
}
/**
 * Where a plan would be written.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanTarget".
 */
/** @experimental */
export interface McpPlanTarget {
  scope: McpPlanScope;
  /**
   * Configuration key the server would be recorded under within that scope.
   */
  configKey: string;
}
/**
 * Outcome of evaluating the planned server against registry and enterprise policy. Evaluation is read-only.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanPolicyResult".
 */
/** @experimental */
export interface McpPlanPolicyResult {
  decision: McpPlanPolicyDecision;
  source: McpPlanPolicySource;
  /**
   * Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
   */
  reason?: string;
}
/**
 * One change applying the plan would make, described rather than serialised so the configuration payload stays behind the runtime boundary.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanConfigurationChange".
 */
/** @experimental */
export interface McpPlanConfigurationChange {
  operation: McpPlanConfigurationOperation;
  scope: McpPlanScope;
  /**
   * Configuration key the change applies to.
   */
  configKey: string;
  /**
   * Names of the configuration fields the change would set, without their values.
   */
  changedFields: string[];
  /**
   * Secret placeholders the written configuration would reference. The constrained placeholder type cannot carry a literal secret value.
   */
  secretReferences: McpPlanSecretReference[];
}
/**
 * Server name to check running status for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpIsServerRunningRequest".
 */
/** @experimental */
export interface McpIsServerRunningRequest {
  /**
   * Name of the MCP server to check
   */
  serverName: string;
}
/**
 * Whether the named MCP server is running.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpIsServerRunningResult".
 */
/** @experimental */
export interface McpIsServerRunningResult {
  /**
   * True if the server has an active client and transport.
   */
  running: boolean;
}
/**
 * Server name whose tool list should be returned.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpListToolsRequest".
 */
/** @experimental */
export interface McpListToolsRequest {
  /**
   * Name of the connected MCP server whose tools to list.
   */
  serverName: string;
}
/**
 * Tools exposed by the connected MCP server. Throws when the server is not connected.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpListToolsResult".
 */
/** @experimental */
export interface McpListToolsResult {
  /**
   * Tools exposed by the server.
   */
  tools: McpTools[];
}
/**
 * MCP tool metadata with tool name, optional description, and normalized MCP Apps discovery metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpTools".
 */
/** @experimental */
export interface McpTools {
  /**
   * Tool name.
   */
  name: string;
  /**
   * Tool description, when provided.
   */
  description?: string;
  ui?: McpToolUi;
}
/**
 * Normalized MCP Apps discovery metadata from a tool's `_meta.ui` block.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpToolUi".
 */
/** @experimental */
export interface McpToolUi {
  /**
   * URI of the tool's MCP App resource, typically a `ui://` resource identifier. Use `session.mcp.resources.read` to fetch its HTML and resource metadata.
   */
  resourceUri?: string;
  /**
   * Tool visibility advertised by the server. When absent, MCP Apps defaults apply.
   */
  visibility?: McpToolUiVisibility[];
}
/**
 * Identifies the MCP server whose persisted OAuth credentials were updated.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthAuthenticationStateChangedRequest".
 */
/** @experimental */
export interface McpOauthAuthenticationStateChangedRequest {
  /**
   * Name of the MCP server whose OAuth credentials were updated. Omit only when the host cannot identify the server.
   */
  serverName?: string;
  /**
   * Whether the target session must mint a session-scoped access token instead of reusing a shared access token persisted by another session.
   */
  refreshSessionToken?: boolean;
}
/**
 * Pending MCP OAuth request ID and host-provided token or cancellation response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthHandlePendingRequest".
 */
/** @experimental */
export interface McpOauthHandlePendingRequest {
  /**
   * OAuth request identifier from the mcp.oauth_required event
   */
  requestId: string;
  result: McpOauthPendingRequestResponse;
}
/**
 * Indicates whether the pending MCP OAuth response was accepted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthHandlePendingResult".
 */
/** @experimental */
export interface McpOauthHandlePendingResult {
  /**
   * Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
   */
  success: boolean;
}
/**
 * Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, callback success-page copy, and static OAuth client selection.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthLoginRequest".
 */
/** @experimental */
export interface McpOauthLoginRequest {
  /**
   * Name of the remote MCP server to authenticate
   */
  serverName: string;
  /**
   * When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
   */
  forceReauth?: boolean;
  /**
   * Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
   */
  clientName?: string;
  /**
   * Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
   */
  callbackSuccessMessage?: string;
  /**
   * Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration.
   */
  clientId?: string;
  /**
   * Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it.
   */
  clientSecret?: string;
  /**
   * Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store.
   */
  publicClient?: boolean;
  grantType?: McpOauthLoginGrantType;
}
/**
 * OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthLoginResult".
 */
/** @experimental */
export interface McpOauthLoginResult {
  /**
   * URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
   */
  authorizationUrl?: string;
}
/**
 * Remote MCP server name for a passive OAuth status probe.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthProbeRequest".
 */
/** @experimental */
export interface McpOauthProbeRequest {
  /**
   * Name of the configured remote MCP server to probe.
   */
  serverName: string;
}
/**
 * Pending MCP OAuth request id to respond to.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthRespondRequest".
 */
/** @experimental */
export interface McpOauthRespondRequest {
  /**
   * OAuth request identifier from the mcp.oauth_required event
   */
  requestId: string;
}
/**
 * Indicates whether the pending MCP OAuth response was accepted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpOauthRespondResult".
 */
/** @experimental */
export interface McpOauthRespondResult {
  /**
   * Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
   */
  success: boolean;
}
/**
 * A computed MCP install plan. Nothing has been applied: the plan describes what installing would change, and the plan handle is what a later apply operation would consume.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallPlanned".
 */
/** @experimental */
export interface McpPlanInstallPlanned {
  /**
   * Discriminator: a plan was computed and nothing was changed
   */
  kind: "planned";
  plan: McpInstallPlan;
  negotiated: CatalogNegotiatedContract;
}
/**
 * A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallRequest".
 */
/** @experimental */
export interface McpPlanInstallRequest {
  contract: CatalogClientContract;
  source: McpPlanInstallSource;
  scope?: McpPlanScope;
}
/**
 * Plan from a candidate returned by a previous catalog search.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallSourceCandidate".
 */
/** @experimental */
export interface McpPlanInstallSourceCandidate {
  kind: McpPlanInstallSourceCandidateKind;
  /**
   * Single-use candidate handle. Consumed by this call, so a replay of the same handle is rejected.
   */
  candidateHandle: string;
  /**
   * The runtime- or authority-minted `searchId` returned with the search that produced this candidate. A search implementation binds it to private candidate-handle context; a planning implementation must verify that context before returning a plan. The unavailable planning implementation in this contract layer validates presence but does not claim the verification has occurred. It identifies a search rather than a person and must never be joined with user identity to re-identify anyone.
   */
  searchId: string;
}
/**
 * Plan from a card supplied directly by the caller, without a preceding search.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpPlanInstallSourceCard".
 */
/** @experimental */
export interface McpPlanInstallSourceCard {
  kind: McpPlanInstallSourceCardKind;
  card: McpServerCardReference;
}
/**
 * An MCP server card to be retrieved from a URL through the runtime's hardened fetch boundary.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerCardUrl".
 */
/** @experimental */
export interface McpServerCardUrl {
  kind: McpServerCardUrlKind;
  mediaType: McpServerCardMediaType;
  /**
   * Card URL. Retrieved only through the runtime's hardened boundary, with scheme, credential, address-range, redirect, timeout, and response-size controls applied. Never logged.
   */
  url: string;
}
/**
 * An MCP server card supplied inline as an inert document.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerCardEmbedded".
 */
/** @experimental */
export interface McpServerCardEmbedded {
  kind: McpServerCardEmbeddedKind;
  mediaType: McpServerCardMediaType;
  /**
   * The card document verbatim, treated as inert untrusted bytes. The runtime parses and validates it; the host is not expected to interpret it. Never logged.
   */
  data: string;
}
/**
 * Registration parameters for an external MCP client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpRegisterExternalClientRequest".
 */
/** @experimental */
/** @internal */
export interface McpRegisterExternalClientRequest {
  /**
   * Logical server name for the external client
   */
  serverName: string;
  /**
   * In-process MCP Client instance. Marked internal: cannot be serialized across the JSON-RPC boundary.
   *
   * @internal
   */
  client: OpaqueInProcessValue;
  /**
   * In-process MCP Transport instance. Marked internal: cannot be serialized across the JSON-RPC boundary.
   *
   * @internal
   */
  transport: OpaqueInProcessValue;
  /**
   * In-process server config (MCPServerConfig) paired with the in-process client/transport. Marked internal alongside its companions.
   *
   * @internal
   */
  config: OpaqueInProcessValue;
}
/**
 * In-process MCP reload configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpReloadConfig".
 */
/** @experimental */
/** @internal */
export interface McpReloadConfig {
  mcpServers: {
    [k: string]: McpServerConfig | undefined;
  };
  disabledServers?: string[];
  enabledServers?: string[];
  /**
   * Server names the CLI enabled for this session via `--enable-mcp-server`.
   */
  cliEnabledServers?: string[];
  mcp3pEnabled?: boolean;
  includeWorkspaceSources?: boolean;
  configFilter?: OpaqueInProcessValue;
  githubMcpToolOptions?: OpaqueInProcessValue;
  githubMcpUserOverride?: boolean;
  secretStore?: OpaqueInProcessValue;
  activeGitHubToken?: string;
  useCachedToolSnapshots?: boolean;
  forceRestart?: boolean;
}
/**
 * Opaque MCP reload configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpReloadWithConfigRequest".
 */
/** @experimental */
/** @internal */
export interface McpReloadWithConfigRequest {
  config: unknown;
}
/**
 * Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpRemoveGitHubResult".
 */
/** @experimental */
export interface McpRemoveGitHubResult {
  /**
   * True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).
   */
  removed: boolean;
}
/**
 * An MCP resource descriptor (spec `Resource`): URI, name, and optional title, description, MIME type, size, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResource".
 */
/** @experimental */
export interface McpResource {
  /**
   * The resource URI (e.g. ui://... or file:///...)
   */
  uri: string;
  /**
   * The programmatic name of the resource
   */
  name: string;
  /**
   * Optional human-readable display title
   */
  title?: string;
  /**
   * Optional description of what this resource represents
   */
  description?: string;
  /**
   * MIME type of the resource, if known
   */
  mimeType?: string;
  /**
   * Resource size in bytes, when known
   */
  size?: number;
  /**
   * Icons associated with this resource
   */
  icons?: McpResourceIcon[];
  annotations?: McpResourceAnnotations;
  /**
   * Resource-level metadata
   */
  _meta?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Server-provided non-standard descriptor fields preserved from the MCP response
   */
  additionalProperties?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * A resource icon descriptor plus preserved non-standard icon fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourceIcon".
 */
/** @experimental */
export interface McpResourceIcon {
  /**
   * Icon URI
   */
  src: string;
  /**
   * Icon MIME type, when known
   */
  mimeType?: string;
  /**
   * Icon sizes hint
   */
  sizes?: string;
  /**
   * Theme hint for this icon
   */
  theme?: string;
  /**
   * Server-provided non-standard icon fields preserved from the MCP response
   */
  additionalProperties?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * Standard MCP resource annotations plus preserved non-standard annotation fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourceAnnotations".
 */
/** @experimental */
export interface McpResourceAnnotations {
  /**
   * Intended audience roles for this resource
   */
  audience?: string[];
  /**
   * Priority hint for model/client use
   */
  priority?: number;
  /**
   * Last-modified timestamp hint
   */
  lastModified?: string;
  /**
   * Server-provided non-standard annotation fields preserved from the MCP response
   */
  additionalProperties?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * MCP resource content with URI, optional MIME type, text or base64 blob, and resource metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourceContent".
 */
/** @experimental */
export interface McpResourceContent {
  /**
   * The resource URI
   */
  uri: string;
  /**
   * MIME type of the content
   */
  mimeType?: string;
  /**
   * Text content (e.g. HTML)
   */
  text?: string;
  /**
   * Base64-encoded binary content
   */
  blob?: string;
  /**
   * Resource-level metadata (CSP, permissions, etc.)
   */
  _meta?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * MCP server whose resources to enumerate.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourcesListRequest".
 */
/** @experimental */
export interface McpResourcesListRequest {
  /**
   * Name of the MCP server whose resources to enumerate
   */
  serverName: string;
  /**
   * Opaque MCP pagination cursor from a prior `nextCursor` value
   */
  cursor?: string;
}
/**
 * One page of resources advertised by the named MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourcesListResult".
 */
/** @experimental */
export interface McpResourcesListResult {
  /**
   * Resources advertised by the server (proxied MCP `resources/list`)
   */
  resources: McpResource[];
  /**
   * Opaque cursor for the next page, if the server has more resources
   */
  nextCursor?: string;
}
/**
 * MCP server whose resource templates to enumerate.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourcesListTemplatesRequest".
 */
/** @experimental */
export interface McpResourcesListTemplatesRequest {
  /**
   * Name of the MCP server whose resource templates to enumerate
   */
  serverName: string;
  /**
   * Opaque MCP pagination cursor from a prior `nextCursor` value
   */
  cursor?: string;
}
/**
 * One page of resource templates advertised by the named MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourcesListTemplatesResult".
 */
/** @experimental */
export interface McpResourcesListTemplatesResult {
  /**
   * Resource templates advertised by the server (proxied MCP `resources/templates/list`)
   */
  resourceTemplates: McpResourceTemplate[];
  /**
   * Opaque cursor for the next page, if the server has more resource templates
   */
  nextCursor?: string;
}
/**
 * An MCP resource template descriptor (spec `ResourceTemplate`): an RFC 6570 URI template, name, and optional title, description, MIME type, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourceTemplate".
 */
/** @experimental */
export interface McpResourceTemplate {
  /**
   * An RFC 6570 URI template for constructing resource URIs
   */
  uriTemplate: string;
  /**
   * The programmatic name of the resource template
   */
  name: string;
  /**
   * Optional human-readable display title
   */
  title?: string;
  /**
   * Optional description of what this template is for
   */
  description?: string;
  /**
   * MIME type for resources matching this template, if uniform
   */
  mimeType?: string;
  /**
   * Icons associated with resources matching this template
   */
  icons?: McpResourceIcon[];
  annotations?: McpResourceAnnotations;
  /**
   * Resource-template-level metadata
   */
  _meta?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Server-provided non-standard descriptor fields preserved from the MCP response
   */
  additionalProperties?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * MCP server and resource URI to fetch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourcesReadRequest".
 */
/** @experimental */
export interface McpResourcesReadRequest {
  /**
   * Name of the MCP server hosting the resource
   */
  serverName: string;
  /**
   * Resource URI
   */
  uri: string;
}
/**
 * Resource contents returned by the MCP server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpResourcesReadResult".
 */
/** @experimental */
export interface McpResourcesReadResult {
  /**
   * Resource contents returned by the server
   */
  contents: McpResourceContent[];
}
/**
 * Server name and optional replacement configuration for an individual MCP server restart. Omit `config` for a config-free restart-by-name of an already-configured server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpRestartServerRequest".
 */
/** @experimental */
export interface McpRestartServerRequest {
  /**
   * Name of the MCP server to restart
   */
  serverName: string;
  config?: McpSerializableServerConfig;
}
/**
 * Outcome of an MCP sampling execution: success result, failure error, or cancellation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSamplingExecutionResult".
 */
/** @experimental */
export interface McpSamplingExecutionResult {
  action: McpSamplingExecutionAction;
  result?: McpExecuteSamplingResult;
  /**
   * Error description, present when action='failure'.
   */
  error?: string;
}
/**
 * MCP server status entry, including config source/plugin source and any connection error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServer".
 */
/** @experimental */
export interface McpServer {
  /**
   * Server name (config key)
   */
  name: string;
  status: McpServerStatus;
  source?: McpServerSource;
  /**
   * Plugin name that provided this server, when source is plugin.
   */
  sourcePlugin?: string;
  /**
   * Plugin version that provided this server, when source is plugin.
   */
  sourcePluginVersion?: string;
  /**
   * Error message if the server failed to connect
   */
  error?: string;
}
/**
 * In-process MCP server configuration used by embedded SDK clients.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerConfigMemory".
 */
/** @experimental */
/** @internal */
export interface McpServerConfigMemory {
  type: McpServerConfigMemoryType;
  /**
   * In-process MCP server instance. This value cannot cross a JSON-RPC boundary.
   *
   * @internal
   */
  serverInstance: OpaqueInProcessValue;
  /**
   * Tools to include. Defaults to all tools if not specified.
   */
  tools?: string[];
  /**
   * Optional human-readable server name.
   */
  displayName?: string;
  /**
   * Whether this server is a built-in fallback.
   */
  isDefaultServer?: boolean;
  filterMapping?: FilterMapping;
  safeForTelemetry?: McpSafeForTelemetry;
  /**
   * Timeout in milliseconds for tool discovery and tool calls.
   */
  timeout?: number;
  oidc?: McpServerAuthConfig;
  deferTools?: McpServerConfigDeferTools;
  /**
   * Whether persisted tool snapshots are disabled.
   */
  disableToolCache?: boolean;
  /**
   * Whether secret masking is disabled for calls to this server.
   */
  disableSecretMasking?: boolean;
  /**
   * Tool names excluded after the include filter is applied.
   */
  excludeTools?: string[];
  /**
   * Event types this server receives as Copilot notifications.
   */
  events?: string[];
  /**
   * Copilot notification types this server may send to the host.
   */
  notifications?: string[];
  source?: McpServerSource;
  /**
   * Plugin that provided this server.
   */
  sourcePlugin?: string;
  /**
   * Version of the plugin that provided this server.
   */
  sourcePluginVersion?: string;
  /**
   * Whether the providing plugin uses the Open Plugin Spec.
   */
  sourcePluginSpec?: boolean;
  /**
   * Source file path recorded while loading the config.
   */
  sourcePath?: string;
  /**
   * Configuration warnings recorded while loading the server.
   */
  configWarnings?: string[];
}
/**
 * MCP servers configured for the session, with their connection status and host-level state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpServerList".
 */
/** @experimental */
export interface McpServerList {
  /**
   * Configured MCP servers
   */
  servers: McpServer[];
  host?: McpHostState;
}
/**
 * Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSetEnvValueModeParams".
 */
/** @experimental */
export interface McpSetEnvValueModeParams {
  mode: McpSetEnvValueModeDetails;
}
/**
 * Env-value mode recorded on the session after the update.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpSetEnvValueModeResult".
 */
/** @experimental */
export interface McpSetEnvValueModeResult {
  mode: McpSetEnvValueModeDetails;
}
/**
 * Server name and optional configuration for an individual MCP server start. Omit `config` for a config-free start-by-name of an already-configured server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpStartServerRequest".
 */
/** @experimental */
export interface McpStartServerRequest {
  /**
   * Name of the MCP server to start
   */
  serverName: string;
  config?: McpSerializableServerConfig;
}
/**
 * MCP server startup filtering result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpStartServersResult".
 */
/** @experimental */
export interface McpStartServersResult {
  /**
   * Servers filtered out before startup
   */
  filteredServers: McpFilteredServer[];
  /**
   * Non-default servers allowed by policy
   */
  allowedServers?: McpAllowedServer[];
  /**
   * Servers whose connection attempt failed.
   */
  failedServers?: McpFailedServer[];
}
/**
 * Server name for an individual MCP server stop.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpStopServerRequest".
 */
/** @experimental */
export interface McpStopServerRequest {
  /**
   * Name of the MCP server to stop
   */
  serverName: string;
}
/**
 * Metadata controlling an MCP task's lifetime.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpTaskMetadata".
 */
/** @experimental */
export interface McpTaskMetadata {
  /**
   * Task time-to-live.
   */
  ttl?: number;
}
/**
 * Server name identifying the external client to remove.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "McpUnregisterExternalClientRequest".
 */
/** @experimental */
/** @internal */
export interface McpUnregisterExternalClientRequest {
  /**
   * Server name of the external client to unregister
   */
  serverName: string;
}
/**
 * Memory configuration for this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MemoryConfiguration".
 */
/** @experimental */
export interface MemoryConfiguration {
  /**
   * Whether memory is enabled for the session.
   */
  enabled: boolean;
}
/**
 * Per-source attribution breakdown for the session's current context window, or null if uninitialized.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextAttributionResult".
 */
/** @experimental */
export interface MetadataContextAttributionResult {
  /**
   * Per-source context-window attribution, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
   */
  contextAttribution?: SessionContextAttribution | null;
}
/**
 * Parameters for the heaviest-messages query.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextHeaviestMessagesRequest".
 */
/** @experimental */
export interface MetadataContextHeaviestMessagesRequest {
  /**
   * Maximum number of messages to return, most-expensive first. Omit for the server default.
   */
  limit?: number;
}
/**
 * The heaviest individual messages in the session's context window, most-expensive first.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextHeaviestMessagesResult".
 */
/** @experimental */
export interface MetadataContextHeaviestMessagesResult {
  /**
   * Total token count of the current context window, so callers can compute each message's share without a second call.
   */
  totalTokens: number;
  /**
   * Heaviest messages, most-expensive first.
   */
  messages: ContextHeaviestMessage[];
}
/**
 * Model identifier and token limits used to compute the context-info breakdown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextInfoRequest".
 */
/** @experimental */
export interface MetadataContextInfoRequest {
  /**
   * Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default.
   */
  promptTokenLimit: number;
  /**
   * Maximum output tokens allowed by the target model. Pass 0 if unknown.
   */
  outputTokenLimit: number;
  /**
   * Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.
   */
  selectedModel?: string;
}
/**
 * Token breakdown for the session's current context window, or null if uninitialized.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataContextInfoResult".
 */
/** @experimental */
export interface MetadataContextInfoResult {
  /**
   * Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
   */
  contextInfo?: SessionContextInfo | null;
}
/**
 * Indicates whether the local session is currently processing a turn or background continuation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataIsProcessingResult".
 */
/** @experimental */
export interface MetadataIsProcessingResult {
  /**
   * Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.
   */
  processing: boolean;
}
/**
 * Model identifier to use when re-tokenizing the session's existing messages.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecomputeContextTokensRequest".
 */
/** @experimental */
export interface MetadataRecomputeContextTokensRequest {
  /**
   * Model identifier used for tokenization. The runtime token-counts both chat-context and system-context messages against this model.
   */
  modelId: string;
}
/**
 * Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecomputeContextTokensResult".
 */
/** @experimental */
export interface MetadataRecomputeContextTokensResult {
  /**
   * Sum of tokens across chat-context and system-context messages currently held by the session.
   */
  totalTokens: number;
  /**
   * Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).
   */
  messagesTokenCount: number;
  /**
   * Tokens contributed by system/developer prompt snapshots.
   */
  systemTokenCount: number;
}
/**
 * Updated working-directory/git context to record on the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecordContextChangeRequest".
 */
/** @experimental */
export interface MetadataRecordContextChangeRequest {
  context: SessionWorkingDirectoryContext;
}
/**
 * Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionWorkingDirectoryContext".
 */
/** @experimental */
export interface SessionWorkingDirectoryContext {
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
  hostType?: SessionWorkingDirectoryContextHostType;
  /**
   * Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com")
   */
  repositoryHost?: string;
  /**
   * Current git branch name
   */
  branch?: string;
  /**
   * Head commit of the current git branch
   */
  headCommit?: string;
  /**
   * Merge-base commit SHA (fork point from the remote default branch)
   */
  baseCommit?: string;
}
/**
 * Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode). For a local session, a report whose `cwd` diverges from the session's current working directory is ignored (the call still succeeds but records nothing and emits no event); move a local session's working directory via `metadata.setWorkingDirectory` instead.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataRecordContextChangeResult".
 */
/** @experimental */
export interface MetadataRecordContextChangeResult {}
/**
 * Absolute path to set as the session's new working directory. For local sessions the path must be absolute and exist on disk: it is validated before any session state changes, and a failing validation rejects the call with nothing mutated, persisted, or emitted. Remote sessions record the path as-is.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSetWorkingDirectoryRequest".
 */
/** @experimental */
export interface MetadataSetWorkingDirectoryRequest {
  /**
   * Absolute path to set as the session's working directory. The runtime updates the session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry) anchor to it.
   */
  workingDirectory: string;
}
/**
 * Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for any related side-effects (file index, etc.); it does NOT change the process working directory (a session's cwd is per-session, not process-global). For local sessions the runtime validates the target first (an absolute path that exists on disk) and re-bases the permission primary directory; a rejected validation fails the call before anything is mutated, persisted, or emitted. Location-scoped permission rules are then re-keyed to the new directory (best-effort). Remote sessions only record the path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSetWorkingDirectoryResult".
 */
/** @experimental */
export interface MetadataSetWorkingDirectoryResult {
  /**
   * Working directory after the update
   */
  workingDirectory: string;
}
/**
 * Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotRemoteMetadata".
 */
/** @experimental */
export interface MetadataSnapshotRemoteMetadata {
  /**
   * The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent.
   */
  resourceId?: string;
  repository: MetadataSnapshotRemoteMetadataRepository;
  /**
   * The pull request number the remote session is associated with, if any.
   */
  pullRequestNumber?: number;
  taskType?: MetadataSnapshotRemoteMetadataTaskType;
}
/**
 * The repository the remote session targets.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MetadataSnapshotRemoteMetadataRepository".
 */
/** @experimental */
export interface MetadataSnapshotRemoteMetadataRepository {
  /**
   * The GitHub owner (user or organization) of the target repository.
   */
  owner: string;
  /**
   * The GitHub repository name (without owner).
   */
  name: string;
  /**
   * The branch the remote session is operating on.
   */
  branch: string;
}
/**
 * Copilot model metadata, including identifier, display name, capabilities, policy, billing, reasoning efforts, and picker categories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Model".
 */
/** @experimental */
export interface Model {
  /**
   * Model identifier (e.g., "claude-sonnet-4.5")
   */
  id: string;
  /**
   * Display name
   */
  name: string;
  capabilities: ModelCapabilities;
  policy?: ModelPolicy;
  billing?: ModelBilling;
  /**
   * Supported reasoning effort levels (only present if model supports reasoning effort)
   */
  supportedReasoningEfforts?: string[];
  /**
   * Default reasoning effort level (only present if model supports reasoning effort)
   */
  defaultReasoningEffort?: string;
  /**
   * Context-window tiers this model offers, when the provider advertises them independently of tiered token pricing. Copilot models carry their tiers in `billing.tokenPrices`; a provider that has no pricing to publish (an agent host reached over AHP, for example) declares them here instead, so the model picker can still offer the tier toggle.
   */
  supportedContextTiers?: string[];
  modelPickerCategory?: ModelPickerCategory;
  modelPickerPriceCategory?: ModelPickerPriceCategory;
  warningText?: ModelWarningText;
  /**
   * Informational notices the service published for this model, such as an upcoming change or a recommended alternative. Present only when the service published at least one notice. Hosts should surface these without implying anything is wrong with the model.
   */
  infoMessages?: ModelMessage[];
  /**
   * Warnings the service published for this model, such as a deprecated client version. Present only when the service published at least one warning. The model remains usable; hosts should surface these as advisory rather than blocking.
   */
  warningMessages?: ModelMessage[];
}
/**
 * Model capabilities and limits
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilities".
 */
/** @experimental */
export interface ModelCapabilities {
  supports?: ModelCapabilitiesSupports;
  limits?: ModelCapabilitiesLimits;
}
/**
 * Feature flags indicating what the model supports
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesSupports".
 */
/** @experimental */
export interface ModelCapabilitiesSupports {
  /**
   * Whether this model supports vision/image input
   */
  vision?: boolean;
  /**
   * Whether this model supports reasoning effort configuration
   */
  reasoningEffort?: boolean;
  adaptive_thinking?: AdaptiveThinkingSupport;
}
/**
 * Token limits for prompts, outputs, and context window
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesLimits".
 */
/** @experimental */
export interface ModelCapabilitiesLimits {
  /**
   * Maximum number of prompt/input tokens
   */
  max_prompt_tokens?: number;
  /**
   * Maximum number of output/completion tokens
   */
  max_output_tokens?: number;
  /**
   * Maximum total context window size in tokens
   */
  max_context_window_tokens?: number;
  vision?: ModelCapabilitiesLimitsVision;
}
/**
 * Vision-specific limits
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesLimitsVision".
 */
/** @experimental */
export interface ModelCapabilitiesLimitsVision {
  /**
   * MIME types the model accepts
   */
  supported_media_types: string[];
  /**
   * Maximum number of images per prompt
   */
  max_prompt_images: number;
  /**
   * Maximum image size in bytes
   */
  max_prompt_image_size: number;
}
/**
 * Policy state (if applicable)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPolicy".
 */
/** @experimental */
export interface ModelPolicy {
  state: ModelPolicyState;
  /**
   * Usage terms or conditions for this model
   */
  terms?: string;
}
/**
 * Billing information
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBilling".
 */
/** @experimental */
export interface ModelBilling {
  /**
   * Billing cost multiplier relative to the base rate
   */
  multiplier?: number;
  tokenPrices?: ModelBillingTokenPrices;
  /**
   * Whole-number percentage discount (0-100) applied to usage billed through this model. Populated for the synthetic `auto` model, where requests routed by auto-mode are billed at a reduced rate; absent for concrete models.
   */
  discountPercent?: number;
  promo?: ModelBillingPromo;
}
/**
 * Token-level pricing information for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBillingTokenPrices".
 */
/** @experimental */
export interface ModelBillingTokenPrices {
  /**
   * AI Credits cost per billing batch of input tokens
   */
  inputPrice?: number;
  /**
   * AI Credits cost per billing batch of output tokens
   */
  outputPrice?: number;
  /**
   * @deprecated
   * Use cacheReadPrice instead. AI Credits cost per billing batch of cached tokens
   */
  cachePrice?: number;
  /**
   * AI Credits cost per billing batch of cached (read) tokens
   */
  cacheReadPrice?: number;
  /**
   * AI Credits cost per billing batch of cache-write (cache creation) tokens.
   */
  cacheWritePrice?: number;
  /**
   * AI Credits cost per billing batch of 1-hour cache-write (cache creation) tokens.
   */
  cacheWrite1hPrice?: number;
  /**
   * Number of tokens per standard billing batch
   */
  batchSize?: number;
  /**
   * @deprecated
   * Use maxPromptTokens instead. Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens.
   */
  contextMax?: number;
  /**
   * Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens.
   */
  maxPromptTokens?: number;
  longContext?: ModelBillingTokenPricesLongContext;
}
/**
 * Long context tier pricing (available for models with extended context windows)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBillingTokenPricesLongContext".
 */
/** @experimental */
export interface ModelBillingTokenPricesLongContext {
  /**
   * AI Credits cost per billing batch of input tokens
   */
  inputPrice?: number;
  /**
   * AI Credits cost per billing batch of output tokens
   */
  outputPrice?: number;
  /**
   * @deprecated
   * Use cacheReadPrice instead. AI Credits cost per billing batch of cached tokens
   */
  cachePrice?: number;
  /**
   * AI Credits cost per billing batch of cached (read) tokens
   */
  cacheReadPrice?: number;
  /**
   * AI Credits cost per billing batch of cache-write (cache creation) tokens.
   */
  cacheWritePrice?: number;
  /**
   * AI Credits cost per billing batch of 1-hour cache-write (cache creation) tokens.
   */
  cacheWrite1hPrice?: number;
  /**
   * @deprecated
   * Use maxPromptTokens instead. Prompt token budget for the long context tier. The total context window is this value plus the model's max_output_tokens.
   */
  contextMax?: number;
  /**
   * Prompt token budget for the long context tier. The total context window is this value plus the model's max_output_tokens.
   */
  maxPromptTokens?: number;
}
/**
 * Active server-driven promotion for a model, including its discount and optional expiry.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelBillingPromo".
 */
/** @experimental */
export interface ModelBillingPromo {
  /**
   * Stable identifier for the promotion campaign.
   */
  id?: string;
  /**
   * Percentage discount (0-100) applied while the promotion is active. May be fractional.
   */
  discountPercent?: number;
  /**
   * UTC ISO 8601 timestamp marking when the promotion ends. Optional: an open-ended promotion omits this field. When present, the API only surfaces a promo whose expiry parses and is in the future, so consumers should treat a past value as expired.
   */
  endsAt?: string;
  /**
   * Human-readable promotion message. Does not include the expiry timestamp; consumers may format endsAt and append it when present.
   */
  message?: string;
  /**
   * Whether the service asked hosts to give this promotion a prominent surface, such as a dedicated banner, in addition to listing it with the model. `true` requests that surface and `false` asks for the model list only. Absent means the service expressed no preference — for example a response that predates the field — so hosts should apply their own default rather than read it as `false`.
   */
  showBanner?: boolean;
}
/**
 * Service-published warning text that hosts should display when presenting a model.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelWarningText".
 */
/** @experimental */
export interface ModelWarningText {
  /**
   * Data-retention warning for the model. The text may contain Markdown links and should be rendered as Markdown when supported.
   */
  dataRetention?: string;
}
/**
 * A service-published message about a model, carrying a stable machine-readable code alongside human-readable text.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelMessage".
 */
/** @experimental */
export interface ModelMessage {
  /**
   * Stable machine-readable identifier for the message, such as `client_version_deprecated`. Hosts can key custom presentation off this; unrecognized codes should fall back to displaying `message`.
   */
  code: string;
  /**
   * Human-readable message text intended for display to the user.
   */
  message: string;
}
/**
 * Managed, repository, and CLI model overrides to overlay onto the session at startup.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelApplyStartupOverlayRequest".
 */
/** @experimental */
export interface ModelApplyStartupOverlayRequest {
  /**
   * Model required by device-managed policy, when configured.
   */
  deviceManagedModel?: string;
  /**
   * Model required by server-managed policy, when configured.
   */
  serverManagedModel?: string;
  /**
   * Startup default model from the enterprise policy helper, when configured. Weakest of the managed sources: it applies only when neither device nor server policy names a model, and an explicit user selection still wins.
   */
  policyHelperModel?: string;
  /**
   * Model selected by repository settings, when configured.
   */
  repoModel?: string;
  /**
   * Reasoning effort selected by repository settings, when configured.
   */
  repoReasoningEffort?: string;
  /**
   * Context tier selected by repository settings, when configured.
   */
  repoContextTier?: string;
  /**
   * Model explicitly selected by the CLI, when provided.
   */
  cliModel?: string;
  /**
   * Whether the overlay is being applied while resuming a deferred session.
   */
  deferredResume?: boolean;
}
/**
 * Optional capability overrides (vision, tool_calls, reasoning, etc.).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverride".
 */
/** @experimental */
export interface ModelCapabilitiesOverride {
  supports?: ModelCapabilitiesOverrideSupports;
  limits?: ModelCapabilitiesOverrideLimits;
}
/**
 * Feature flags indicating what the model supports
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverrideSupports".
 */
/** @experimental */
export interface ModelCapabilitiesOverrideSupports {
  /**
   * Whether this model supports vision/image input
   */
  vision?: boolean;
  /**
   * Whether this model supports reasoning effort configuration
   */
  reasoningEffort?: boolean;
  adaptive_thinking?: AdaptiveThinkingSupport;
}
/**
 * Token limits for prompts, outputs, and context window
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverrideLimits".
 */
/** @experimental */
export interface ModelCapabilitiesOverrideLimits {
  /**
   * Maximum number of prompt/input tokens
   */
  max_prompt_tokens?: number;
  /**
   * Maximum number of output/completion tokens
   */
  max_output_tokens?: number;
  /**
   * Maximum total context window size in tokens
   */
  max_context_window_tokens?: number;
  vision?: ModelCapabilitiesOverrideLimitsVision;
}
/**
 * Vision-specific limits
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelCapabilitiesOverrideLimitsVision".
 */
/** @experimental */
export interface ModelCapabilitiesOverrideLimitsVision {
  /**
   * MIME types the model accepts
   */
  supported_media_types?: string[];
  /**
   * Maximum number of images per prompt
   */
  max_prompt_images?: number;
  /**
   * Maximum image size in bytes
   */
  max_prompt_image_size?: number;
}
/**
 * List of Copilot models available to the resolved user, including capabilities and billing metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelList".
 */
/** @experimental */
export interface ModelList {
  /**
   * List of available models with full metadata
   */
  models: Model[];
}

/** @experimental */
export interface ModelPickerPersistenceRequest {
  settingsContext: ModelPickerSettingsContext;
  /**
   * Whether reasoning effort was explicitly selected and should be persisted.
   */
  reasoningEffortExplicit?: boolean;
  /**
   * Whether context tier was explicitly selected and should be persisted.
   */
  contextTierExplicit?: boolean;
}
/**
 * Filesystem and environment context used to resolve model-picker settings.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelPickerSettingsContext".
 */
/** @experimental */
export interface ModelPickerSettingsContext {
  /**
   * Optional Copilot configuration directory containing persisted settings.
   */
  configDir?: string;
  /**
   * User home directory used when resolving persisted settings.
   */
  homeDirectory: string;
  /**
   * Environment variables consulted while resolving model-picker settings.
   */
  environment: {};
}
/**
 * Reasoning effort level to apply to the currently selected model.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSetReasoningEffortRequest".
 */
/** @experimental */
export interface ModelSetReasoningEffortRequest {
  /**
   * Reasoning effort level to apply to the currently selected model. The host is responsible for validating the value against the model's supported levels before calling.
   */
  reasoningEffort: string;
}
/**
 * Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSetReasoningEffortResult".
 */
/** @experimental */
export interface ModelSetReasoningEffortResult {
  /**
   * Reasoning effort level recorded on the session after the update
   */
  reasoningEffort: string;
}

/** @experimental */
export interface ModelsListRequest {
  /**
   * Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used.
   */
  selectionId?: string;
  /**
   * GitHub token accepted for compatibility with existing SDK clients. When provided, resolves this token instead of using the current account.
   */
  gitHubToken?: string;
}

/** @experimental */
export interface ModelSwitchConfirmation {
  /**
   * Display name of the model that requires compaction confirmation.
   */
  targetModelDisplayName: string;
  /**
   * Current conversation token count before switching models.
   */
  currentTokens: number;
  /**
   * Target model token limit used by the compaction preflight.
   */
  targetLimit: number;
}
/**
 * Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSwitchToRequest".
 */
/** @experimental */
export interface ModelSwitchToRequest {
  /**
   * Model selection id to switch to, as returned by `list`. A bare id (e.g. `claude-sonnet-4.6`) names a Copilot (CAPI) model; a provider-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a registry BYOK model.
   */
  modelId: string;
  /**
   * Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. When omitted, no effort override is applied.
   */
  reasoningEffort?: string;
  reasoningSummary?: ReasoningSummary;
  verbosity?: Verbosity;
  modelCapabilities?: ModelCapabilitiesOverride;
  contextTier?: ContextTier;
  source?: ModelChangeSource;
  /**
   * When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active).
   */
  deferIfModelChangeQueued?: boolean;
  /**
   * Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary.
   */
  compactionDecision?: string;
  /**
   * When true, evaluate context-window compaction policy before applying the switch.
   */
  runCompactionPreflight?: boolean;
  /**
   * Optional repository settings scope to persist after the switch commits.
   */
  repoScope?: string;
  /**
   * Settings scope used when persisting the selected model.
   */
  modelChangeScope?: string;
  /**
   * Require the target to be currently available and enabled before applying the switch.
   */
  requireAvailable?: boolean;
  pickerPersistence?: ModelPickerPersistenceRequest;
}
/**
 * The model identifier active on the session after the switch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModelSwitchToResult".
 */
/** @experimental */
export interface ModelSwitchToResult {
  /**
   * Currently active model identifier after the switch
   */
  modelId?: string;
  /**
   * True when the switch was deferred (enqueued as a cancellable `/model` command) because a turn was active or another model change was already queued, rather than applied immediately. When true, the session's live model is unchanged until the queued change drains.
   */
  deferred?: boolean;
  /**
   * Lifecycle result for the requested switch
   */
  status?: string;
  confirmation?: ModelSwitchConfirmation;
  /**
   * Persistence failure encountered after applying the model switch.
   */
  persistenceError?: string;
  /**
   * User-facing outcome message for the model switch.
   */
  message?: string;
  /**
   * User-facing warning produced while applying the model switch.
   */
  warning?: string;
  /**
   * Deprecation warnings associated with the selected model or options.
   */
  deprecationWarnings?: string[];
}
/**
 * Agent interaction mode to apply to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModeSetRequest".
 */
/** @experimental */
export interface ModeSetRequest {
  mode: SessionMode;
  /**
   * Session whose plan-mode base state should be inherited.
   */
  inheritPlanBaseFromSessionId?: string;
  /**
   * Whether a dedicated plan model is configured.
   */
  planModelConfigured?: boolean;
  /**
   * Dedicated model to use in plan mode, when configured.
   */
  planModel?: string;
  /**
   * Reasoning effort to use with the dedicated plan model.
   */
  planReasoningEffort?: string;
  /**
   * Context tier to use with the dedicated plan model.
   */
  planContextTier?: string;
  /**
   * Explicit response to a model-switch compaction preflight.
   */
  compactionDecision?: string;
  /**
   * Whether leaving plan mode should restore the session's previous model.
   */
  restorePlanModel?: boolean;
  /**
   * Whether the selected plan model should be persisted.
   */
  persistPlanSelection?: boolean;
  pickerSettingsContext?: ModelPickerSettingsContext;
  /**
   * Action to perform when leaving plan mode.
   */
  planExitAction?: string;
}
/**
 * Outcome of a session mode change, including any model switch it triggered and follow-up the host must perform.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ModeSetResult".
 */
/** @experimental */
export interface ModeSetResult {
  /**
   * Lifecycle status of the requested mode change.
   */
  status: string;
  /**
   * Whether applying the mode changed the active model.
   */
  modelChanged: boolean;
  confirmation?: ModelSwitchConfirmation;
  /**
   * User-facing warning produced while applying the mode change.
   */
  warning?: string;
  /**
   * User-facing outcome message for the model switch triggered by the mode change.
   */
  message?: string;
  /**
   * Deprecation warnings associated with the model selected by the mode change.
   */
  deprecationWarnings?: string[];
  /**
   * Whether the host must defer implementing the requested mode change.
   */
  deferImplementation?: boolean;
  /**
   * Whether the host should arm an interactive continuation after the mode change.
   */
  armInteractiveContinuation?: boolean;
}
/**
 * Result of moving in-flight MCP loading to the background.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "MoveMcpLoadingToBackgroundResult".
 */
/** @experimental */
export interface MoveMcpLoadingToBackgroundResult {
  /**
   * Whether an in-flight MCP load was moved to the background, releasing turns that were waiting on it. False when no MCP load was in flight or the waiting turns had already been released.
   */
  movedToBackground: boolean;
}
/**
 * External SDK input for a named custom model provider. Ingested by the native protocol boundary before host dispatch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NamedProviderConfig".
 */
/** @experimental */
export interface NamedProviderConfig {
  /**
   * Unique provider name used to qualify model selection IDs.
   */
  name: string;
  type?: ProviderConfigType;
  wireApi?: ProviderConfigWireApi;
  transport?: ProviderConfigTransport;
  /**
   * Base URL for provider API requests.
   */
  baseUrl: string;
  /**
   * Static API key used to authenticate provider requests.
   */
  apiKey?: string;
  /**
   * Static bearer token used to authenticate provider requests.
   */
  bearerToken?: string;
  azure?: ProviderConfigAzure;
  /**
   * Additional HTTP headers included with provider requests.
   */
  headers?: {
    [k: string]: string | undefined;
  };
  /**
   * Whether the host supplies bearer tokens dynamically.
   */
  hasBearerTokenProvider?: boolean;
}
/**
 * Azure-specific provider options.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderConfigAzure".
 */
/** @experimental */
export interface ProviderConfigAzure {
  /**
   * API version. When set, uses the versioned deployment route. When omitted, uses the GA versionless v1 route.
   */
  apiVersion?: string;
}
/**
 * The session's friendly name, or null when not yet set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameGetResult".
 */
/** @experimental */
export interface NameGetResult {
  /**
   * The session name (user-set or auto-generated), or null if not yet set
   */
  name: string | null;
}
/**
 * Auto-generated session summary to apply as the session's name when no user-set name exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetAutoRequest".
 */
/** @experimental */
export interface NameSetAutoRequest {
  /**
   * Auto-generated session summary. Empty/whitespace-only values are ignored; values are trimmed before persisting.
   */
  summary: string;
}
/**
 * Indicates whether the auto-generated summary was applied as the session's name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetAutoResult".
 */
/** @experimental */
export interface NameSetAutoResult {
  /**
   * Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.
   */
  applied: boolean;
}
/**
 * New friendly name to apply to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "NameSetRequest".
 */
/** @experimental */
export interface NameSetRequest {
  /**
   * New session name (1–100 characters, trimmed of leading/trailing whitespace)
   */
  name: string;
}
/**
 * Content-exclusion policy supplied to `session.options.update`, with rules, last-updated data, and scope.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateAdditionalContentExclusionPolicy".
 */
/** @experimental */
export interface OptionsUpdateAdditionalContentExclusionPolicy {
  /**
   * Content-exclusion rules to apply.
   */
  rules: OptionsUpdateAdditionalContentExclusionPolicyRule[];
  /**
   * Opaque policy update timestamp supplied by the host.
   */
  last_updated_at: JsonValue;
  scope: OptionsUpdateAdditionalContentExclusionPolicyScope;
}
/**
 * Single content-exclusion rule supplied to `session.options.update`, with paths, match conditions, and source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateAdditionalContentExclusionPolicyRule".
 */
/** @experimental */
export interface OptionsUpdateAdditionalContentExclusionPolicyRule {
  /**
   * Path patterns covered by this rule.
   */
  paths: string[];
  /**
   * Conditions of which at least one must match.
   */
  ifAnyMatch?: string[];
  /**
   * Conditions none of which may match.
   */
  ifNoneMatch?: string[];
  source: OptionsUpdateAdditionalContentExclusionPolicyRuleSource;
}
/**
 * Source descriptor for a `session.options.update` content-exclusion rule, with source name and type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "OptionsUpdateAdditionalContentExclusionPolicyRuleSource".
 */
/** @experimental */
export interface OptionsUpdateAdditionalContentExclusionPolicyRuleSource {
  /**
   * Name of the policy source.
   */
  name: string;
  /**
   * Type of the policy source.
   */
  type: string;
}
/**
 * Pending permission prompt reconstructed from event history, with request ID and user-facing prompt details.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PendingPermissionRequest".
 */
/** @experimental */
export interface PendingPermissionRequest {
  /**
   * Unique identifier for the pending permission request
   */
  requestId: string;
  request: PermissionPromptRequest;
}
/**
 * List of pending permission requests reconstructed from event history.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PendingPermissionRequestList".
 */
/** @experimental */
export interface PendingPermissionRequestList {
  /**
   * Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.
   */
  items: PendingPermissionRequest[];
}
/**
 * Permission-decision request variant to approve only the current permission request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveOnce".
 */
/** @experimental */
export interface PermissionDecisionApproveOnce {
  /**
   * Approve this single request only
   */
  kind: "approve-once";
  /**
   * True only when a host surfaced this request to a user who approved it.
   */
  approvedInteractively?: boolean;
}
/**
 * Permission-decision request variant to approve for the rest of the session, with optional tool approval or URL domain.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSession".
 */
/** @experimental */
export interface PermissionDecisionApproveForSession {
  /**
   * Approve and remember for the rest of the session
   */
  kind: "approve-for-session";
  approval?: PermissionDecisionApproveForSessionApproval;
  /**
   * URL domain to approve for the rest of the session (URL prompts only)
   */
  domain?: string;
}
/**
 * Session-scoped approval details for specific command identifiers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalCommands".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalCommands {
  /**
   * Approval scoped to specific command identifiers.
   */
  kind: "commands";
  /**
   * Command identifiers covered by this approval.
   */
  commandIdentifiers: string[];
}
/**
 * Session-scoped approval details for read-only filesystem operations.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalRead".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalRead {
  /**
   * Approval covering read-only filesystem operations.
   */
  kind: "read";
}
/**
 * Session-scoped approval details for filesystem write operations.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalWrite".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalWrite {
  /**
   * Approval covering filesystem write operations.
   */
  kind: "write";
}
/**
 * Session-scoped approval details for an MCP server tool, or all tools on the server when `toolName` is null.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalMcp".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalMcp {
  /**
   * Approval covering an MCP tool.
   */
  kind: "mcp";
  /**
   * MCP server name.
   */
  serverName: string;
  /**
   * MCP tool name, or null to cover every tool on the server.
   */
  toolName: string | null;
}
/**
 * Session-scoped approval details for MCP sampling requests from a server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalMcpSampling".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalMcpSampling {
  /**
   * Approval covering MCP sampling requests for a server.
   */
  kind: "mcp-sampling";
  /**
   * MCP server name.
   */
  serverName: string;
}
/**
 * Session-scoped approval details for writes to long-term memory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalMemory".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalMemory {
  /**
   * Approval covering writes to long-term memory.
   */
  kind: "memory";
}
/**
 * Session-scoped approval details for a custom tool, keyed by tool name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalCustomTool".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalCustomTool {
  /**
   * Approval covering a custom tool.
   */
  kind: "custom-tool";
  /**
   * Custom tool name.
   */
  toolName: string;
}
/**
 * Session-scoped approval details for extension-management operations, optionally narrowed by operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalExtensionManagement".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalExtensionManagement {
  /**
   * Approval covering extension lifecycle operations such as enable, disable, or reload.
   */
  kind: "extension-management";
  /**
   * Optional operation identifier; when omitted, the approval covers all extension management operations.
   */
  operation?: string;
}
/**
 * Session-scoped factory approval, optionally narrowed by approval key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalFactory".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalFactory {
  /**
   * Approval covering factory operations.
   */
  kind: "factory";
  /**
   * Optional factory operation name or canonical approval key; when omitted, the approval covers all factory operations.
   */
  approvalKey?: string;
}
/**
 * Session-scoped approval details for an extension's permission-gated capability access, keyed by extension name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess {
  /**
   * Approval covering an extension's request to access a permission-gated capability.
   */
  kind: "extension-permission-access";
  /**
   * Extension name.
   */
  extensionName: string;
}
/**
 * Session-scoped approval details for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForSessionApprovalExtensionEnvAccess".
 */
/** @experimental */
export interface PermissionDecisionApproveForSessionApprovalExtensionEnvAccess {
  /**
   * Approval covering an extension's request to read sensitive environment variables.
   */
  kind: "extension-env-access";
  /**
   * Extension name.
   */
  extensionName: string;
  /**
   * Names of the sensitive environment variables this approval covers. Values are never persisted.
   *
   * @minItems 1
   */
  environmentVariables: [string, ...string[]];
}
/**
 * Permission-decision request variant to approve and persist a permission for a project location, with approval details and location key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocation".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocation {
  /**
   * Approve and persist for this project location
   */
  kind: "approve-for-location";
  approval: PermissionDecisionApproveForLocationApproval;
  /**
   * Location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
}
/**
 * Location-scoped approval details for specific command identifiers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalCommands".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalCommands {
  /**
   * Approval scoped to specific command identifiers.
   */
  kind: "commands";
  /**
   * Command identifiers covered by this approval.
   */
  commandIdentifiers: string[];
}
/**
 * Location-scoped approval details for read-only filesystem operations.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalRead".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalRead {
  /**
   * Approval covering read-only filesystem operations.
   */
  kind: "read";
}
/**
 * Location-scoped approval details for filesystem write operations.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalWrite".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalWrite {
  /**
   * Approval covering filesystem write operations.
   */
  kind: "write";
}
/**
 * Location-scoped approval details for an MCP server tool, or all tools on the server when `toolName` is null.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalMcp".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalMcp {
  /**
   * Approval covering an MCP tool.
   */
  kind: "mcp";
  /**
   * MCP server name.
   */
  serverName: string;
  /**
   * MCP tool name, or null to cover every tool on the server.
   */
  toolName: string | null;
}
/**
 * Location-scoped approval details for MCP sampling requests from a server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalMcpSampling".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalMcpSampling {
  /**
   * Approval covering MCP sampling requests for a server.
   */
  kind: "mcp-sampling";
  /**
   * MCP server name.
   */
  serverName: string;
}
/**
 * Location-scoped approval details for writes to long-term memory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalMemory".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalMemory {
  /**
   * Approval covering writes to long-term memory.
   */
  kind: "memory";
}
/**
 * Location-scoped approval details for a custom tool, keyed by tool name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalCustomTool".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalCustomTool {
  /**
   * Approval covering a custom tool.
   */
  kind: "custom-tool";
  /**
   * Custom tool name.
   */
  toolName: string;
}
/**
 * Location-scoped approval details for extension-management operations, optionally narrowed by operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalExtensionManagement".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalExtensionManagement {
  /**
   * Approval covering extension lifecycle operations such as enable, disable, or reload.
   */
  kind: "extension-management";
  /**
   * Optional operation identifier; when omitted, the approval covers all extension management operations.
   */
  operation?: string;
}
/**
 * Location-scoped factory approval, optionally narrowed by approval key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalFactory".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalFactory {
  /**
   * Approval covering factory operations.
   */
  kind: "factory";
  /**
   * Optional factory operation name or canonical approval key; when omitted, the approval covers all factory operations.
   */
  approvalKey?: string;
}
/**
 * Location-scoped approval details for an extension's permission-gated capability access, keyed by extension name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess {
  /**
   * Approval covering an extension's request to access a permission-gated capability.
   */
  kind: "extension-permission-access";
  /**
   * Extension name.
   */
  extensionName: string;
}
/**
 * Location-scoped approval details for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproveForLocationApprovalExtensionEnvAccess".
 */
/** @experimental */
export interface PermissionDecisionApproveForLocationApprovalExtensionEnvAccess {
  /**
   * Approval covering an extension's request to read sensitive environment variables.
   */
  kind: "extension-env-access";
  /**
   * Extension name.
   */
  extensionName: string;
  /**
   * Names of the sensitive environment variables this approval covers. Values are never persisted.
   *
   * @minItems 1
   */
  environmentVariables: [string, ...string[]];
}
/**
 * Permission-decision request variant to permanently approve a URL domain across sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApprovePermanently".
 */
/** @experimental */
export interface PermissionDecisionApprovePermanently {
  /**
   * Approve and persist across sessions (URL prompts only)
   */
  kind: "approve-permanently";
  /**
   * URL domain to approve permanently
   */
  domain: string;
}
/**
 * Permission-decision request variant to reject a pending permission request, with optional feedback.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionReject".
 */
/** @experimental */
export interface PermissionDecisionReject {
  /**
   * Reject the request
   */
  kind: "reject";
  /**
   * Optional feedback explaining the rejection
   */
  feedback?: string;
}
/**
 * Permission-decision variant indicating no user was available to confirm the request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionUserNotAvailable".
 */
/** @experimental */
export interface PermissionDecisionUserNotAvailable {
  /**
   * No user is available to confirm the request
   */
  kind: "user-not-available";
}
/**
 * Permission-decision variant indicating the request was approved.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApproved".
 */
/** @experimental */
export interface PermissionDecisionApproved {
  /**
   * The permission request was approved
   */
  kind: "approved";
}
/**
 * Permission-decision variant indicating approval was remembered for the session, with approval details.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApprovedForSession".
 */
/** @experimental */
export interface PermissionDecisionApprovedForSession {
  /**
   * Approved and remembered for the rest of the session
   */
  kind: "approved-for-session";
  approval: UserToolSessionApproval;
}
/**
 * Permission-decision variant indicating approval was persisted for a project location, with approval details and location key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionApprovedForLocation".
 */
/** @experimental */
export interface PermissionDecisionApprovedForLocation {
  /**
   * Approved and persisted for this project location
   */
  kind: "approved-for-location";
  approval: UserToolSessionApproval;
  /**
   * The location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
}
/**
 * Permission-decision variant indicating the request was cancelled before use, with an optional reason.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionCancelled".
 */
/** @experimental */
export interface PermissionDecisionCancelled {
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
 * Permission-decision variant indicating explicit denial by permission rules, with the matching rules.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedByRules".
 */
/** @experimental */
export interface PermissionDecisionDeniedByRules {
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
 * Permission-decision variant indicating no approval rule matched and user confirmation was unavailable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser".
 */
/** @experimental */
export interface PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser {
  /**
   * Denied because no approval rule matched and user confirmation was unavailable
   */
  kind: "denied-no-approval-rule-and-could-not-request-from-user";
}
/**
 * Permission-decision variant indicating the user denied an interactive prompt, with optional feedback and force-reject flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedInteractivelyByUser".
 */
/** @experimental */
export interface PermissionDecisionDeniedInteractivelyByUser {
  /**
   * Denied by the user during an interactive prompt
   */
  kind: "denied-interactively-by-user";
  /**
   * Optional feedback from the user explaining the denial
   */
  feedback?: string;
  /**
   * Whether to force-reject the current agent turn
   */
  forceReject?: boolean;
}
/**
 * Permission-decision variant indicating denial by content-exclusion policy, with path and message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedByContentExclusionPolicy".
 */
/** @experimental */
export interface PermissionDecisionDeniedByContentExclusionPolicy {
  /**
   * Denied by the organization's content exclusion policy
   */
  kind: "denied-by-content-exclusion-policy";
  /**
   * File path that triggered the exclusion
   */
  path: string;
  /**
   * Human-readable explanation of why the path was excluded
   */
  message: string;
}
/**
 * Permission-decision variant indicating denial by a permission request hook, with optional message and interrupt flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionDeniedByPermissionRequestHook".
 */
/** @experimental */
export interface PermissionDecisionDeniedByPermissionRequestHook {
  /**
   * Denied by a permission request hook registered by an extension or plugin
   */
  kind: "denied-by-permission-request-hook";
  /**
   * Optional message from the hook explaining the denial
   */
  message?: string;
  /**
   * Whether to interrupt the current agent turn
   */
  interrupt?: boolean;
}
/**
 * Optional informational context describing how and where the permission decision was made. This does not affect permission behavior.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionContext".
 */
/** @experimental */
export interface PermissionDecisionContext {
  outcome: PermissionDecisionOutcome;
  source: PermissionDecisionSource;
  surface: PermissionDecisionSurface;
  responseCapability?: PermissionResponseCapability;
}
/**
 * Pending permission request ID and the decision to apply (approve/reject and scope).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionDecisionRequest".
 */
/** @experimental */
export interface PermissionDecisionRequest {
  /**
   * Request ID of the pending permission request
   */
  requestId: string;
  result: PermissionDecision;
  decisionContext?: PermissionDecisionContext;
}
/**
 * Location-scoped tool approval to persist.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationAddToolApprovalParams".
 */
/** @experimental */
export interface PermissionLocationAddToolApprovalParams {
  /**
   * Location key (git root or cwd) to persist the approval to
   */
  locationKey: string;
  approval: PermissionsLocationsAddToolApprovalDetails;
}
/**
 * Location-persisted tool approval details for specific command identifiers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsCommands".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsCommands {
  /**
   * Approval scoped to specific command identifiers.
   */
  kind: "commands";
  /**
   * Command identifiers covered by this approval.
   */
  commandIdentifiers: string[];
}
/**
 * Location-persisted tool approval details for read-only filesystem operations.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsRead".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsRead {
  /**
   * Approval covering read-only filesystem operations.
   */
  kind: "read";
}
/**
 * Location-persisted tool approval details for filesystem write operations.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsWrite".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsWrite {
  /**
   * Approval covering filesystem write operations.
   */
  kind: "write";
}
/**
 * Location-persisted tool approval details for an MCP server tool, or all tools when `toolName` is null.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsMcp".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsMcp {
  /**
   * Approval covering an MCP tool.
   */
  kind: "mcp";
  /**
   * MCP server name.
   */
  serverName: string;
  /**
   * MCP tool name, or null to cover every tool on the server.
   */
  toolName: string | null;
}
/**
 * Location-persisted tool approval details for MCP sampling requests from a server.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsMcpSampling".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsMcpSampling {
  /**
   * Approval covering MCP sampling requests for a server.
   */
  kind: "mcp-sampling";
  /**
   * MCP server name.
   */
  serverName: string;
}
/**
 * Location-persisted tool approval details for writes to long-term memory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsMemory".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsMemory {
  /**
   * Approval covering writes to long-term memory.
   */
  kind: "memory";
}
/**
 * Location-persisted tool approval details for a custom tool, keyed by tool name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsCustomTool".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsCustomTool {
  /**
   * Approval covering a custom tool.
   */
  kind: "custom-tool";
  /**
   * Custom tool name.
   */
  toolName: string;
}
/**
 * Location-persisted tool approval details for extension-management operations, optionally narrowed by operation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsExtensionManagement".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsExtensionManagement {
  /**
   * Approval covering extension lifecycle operations such as enable, disable, or reload.
   */
  kind: "extension-management";
  /**
   * Optional operation identifier; when omitted, the approval covers all extension management operations.
   */
  operation?: string;
}
/**
 * Location-persisted factory approval, optionally narrowed by approval key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsFactory".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsFactory {
  /**
   * Approval covering factory operations.
   */
  kind: "factory";
  /**
   * Optional factory operation name or canonical approval key; when omitted, the approval covers all factory operations.
   */
  approvalKey?: string;
}
/**
 * Location-persisted tool approval details for an extension's permission-gated capability access, keyed by extension name.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess {
  /**
   * Approval covering an extension's request to access a permission-gated capability.
   */
  kind: "extension-permission-access";
  /**
   * Extension name.
   */
  extensionName: string;
}
/**
 * Location-persisted tool approval details for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccess".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccess {
  /**
   * Approval covering an extension's request to read sensitive environment variables.
   */
  kind: "extension-env-access";
  /**
   * Extension name.
   */
  extensionName: string;
  /**
   * Names of the sensitive environment variables this approval covers. Values are never persisted.
   *
   * @minItems 1
   */
  environmentVariables: [string, ...string[]];
}
/**
 * Working directory to load persisted location permissions for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationApplyParams".
 */
/** @experimental */
export interface PermissionLocationApplyParams {
  /**
   * Working directory whose persisted location permissions should be applied
   */
  workingDirectory: string;
}
/**
 * Summary of persisted location permissions applied to the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationApplyResult".
 */
/** @experimental */
export interface PermissionLocationApplyResult {
  /**
   * Location key used in the location-permissions store
   */
  locationKey: string;
  locationType: PermissionLocationType;
  /**
   * Whether a different location was applied since the previous apply call
   */
  changed: boolean;
  /**
   * Number of location-scoped rules added to the live permission service
   */
  appliedRuleCount: number;
  /**
   * Number of persisted allowed directories added to the live path manager
   */
  appliedDirectoryCount: number;
  /**
   * Location-scoped rules applied to the live permission service
   */
  appliedRules: PermissionRule[];
}
/**
 * Working directory to resolve into a location-permissions key.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationResolveParams".
 */
/** @experimental */
export interface PermissionLocationResolveParams {
  /**
   * Working directory whose permission location should be resolved
   */
  workingDirectory: string;
}
/**
 * Resolved location-permissions key and type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionLocationResolveResult".
 */
/** @experimental */
export interface PermissionLocationResolveResult {
  /**
   * Location key used in the location-permissions store
   */
  locationKey: string;
  locationType: PermissionLocationType;
}
/**
 * Directory path to add to the session's allowed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsAddParams".
 */
/** @experimental */
export interface PermissionPathsAddParams {
  /**
   * Directory to add to the allow-list. The runtime resolves and validates the path before adding, then loads conventional `.github/skills/` and `.github/agents/` definitions under it when their subsystem gates are enabled. Adding the directory is therefore also a trust decision for configuration stored there.
   */
  path: string;
}
/**
 * Path to evaluate against the session's allowed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsAllowedCheckParams".
 */
/** @experimental */
export interface PermissionPathsAllowedCheckParams {
  /**
   * Path to check against the session's allowed directories
   */
  path: string;
}
/**
 * Indicates whether the supplied path is within the session's allowed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsAllowedCheckResult".
 */
/** @experimental */
export interface PermissionPathsAllowedCheckResult {
  /**
   * Whether the path is within the session's allowed directories
   */
  allowed: boolean;
}
/**
 * If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsConfig".
 */
/** @experimental */
export interface PermissionPathsConfig {
  /**
   * If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager.
   */
  unrestricted?: boolean;
  /**
   * Additional directories to allow tool access to (in addition to the session's working directory). Conventional `.github/skills/` and `.github/agents/` definitions under them also join the session catalogs when their subsystem gates are enabled, so supplying a directory is a trust decision for configuration stored there. When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion).
   */
  additionalDirectories?: string[];
  /**
   * Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true.
   */
  includeTempDirectory?: boolean;
  /**
   * Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true.
   */
  workspacePath?: string;
}
/**
 * Snapshot of the session's allow-listed directories and primary working directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsList".
 */
/** @experimental */
export interface PermissionPathsList {
  /**
   * All directories currently allowed for tool access on this session.
   */
  directories: string[];
  /**
   * The primary working directory for this session.
   */
  primary: string;
}
/**
 * Directory path to set as the session's new primary working directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsUpdatePrimaryParams".
 */
/** @experimental */
export interface PermissionPathsUpdatePrimaryParams {
  /**
   * Directory to set as the new primary working directory for the session's permission policy.
   */
  path: string;
}
/**
 * Path to evaluate against the session's workspace (primary) directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsWorkspaceCheckParams".
 */
/** @experimental */
export interface PermissionPathsWorkspaceCheckParams {
  /**
   * Path to check against the session workspace directory
   */
  path: string;
}
/**
 * Indicates whether the supplied path is within the session's workspace directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPathsWorkspaceCheckResult".
 */
/** @experimental */
export interface PermissionPathsWorkspaceCheckResult {
  /**
   * Whether the path is within the session workspace directory
   */
  allowed: boolean;
}
/**
 * Notification payload describing the permission prompt that the client just rendered.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionPromptShownNotification".
 */
/** @experimental */
export interface PermissionPromptShownNotification {
  /**
   * Human-readable description of the prompt the user is being asked to approve. Used by the runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell, desktop notification).
   */
  message: string;
}
/**
 * Indicates whether the permission decision was applied; false when the request was already resolved.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionRequestResult".
 */
/** @experimental */
export interface PermissionRequestResult {
  /**
   * Whether the permission request was handled successfully
   */
  success: boolean;
}
/**
 * If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionRulesSet".
 */
/** @experimental */
export interface PermissionRulesSet {
  /**
   * Rules that auto-approve matching requests
   */
  approved: PermissionRule[];
  /**
   * Rules that auto-deny matching requests
   */
  denied: PermissionRule[];
}
/**
 * Content-exclusion policy supplied to `session.permissions.configure`, with rules, last-updated data, and scope.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicy".
 */
/** @experimental */
export interface PermissionsConfigureAdditionalContentExclusionPolicy {
  /**
   * Content-exclusion rules to apply.
   */
  rules: PermissionsConfigureAdditionalContentExclusionPolicyRule[];
  /**
   * Opaque policy update timestamp supplied by the host.
   */
  last_updated_at: JsonValue;
  scope: PermissionsConfigureAdditionalContentExclusionPolicyScope;
}
/**
 * Single content-exclusion rule supplied to `session.permissions.configure`, with paths, match conditions, and source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicyRule".
 */
/** @experimental */
export interface PermissionsConfigureAdditionalContentExclusionPolicyRule {
  /**
   * Path patterns covered by this rule.
   */
  paths: string[];
  /**
   * Conditions of which at least one must match.
   */
  ifAnyMatch?: string[];
  /**
   * Conditions none of which may match.
   */
  ifNoneMatch?: string[];
  source: PermissionsConfigureAdditionalContentExclusionPolicyRuleSource;
}
/**
 * Source descriptor for a `session.permissions.configure` content-exclusion rule, with source name and type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureAdditionalContentExclusionPolicyRuleSource".
 */
/** @experimental */
export interface PermissionsConfigureAdditionalContentExclusionPolicyRuleSource {
  /**
   * Name of the policy source.
   */
  name: string;
  /**
   * Type of the policy source.
   */
  type: string;
}
/**
 * Patch of permission policy fields to apply (omit a field to leave it unchanged).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureParams".
 */
/** @experimental */
export interface PermissionsConfigureParams {
  /**
   * If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
   */
  approveAllToolPermissionRequests?: boolean;
  /**
   * If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
   */
  approveAllReadPermissionRequests?: boolean;
  rules?: PermissionRulesSet;
  paths?: PermissionPathsConfig;
  urls?: PermissionUrlsConfig;
  /**
   * If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
   */
  additionalContentExclusionPolicies?: PermissionsConfigureAdditionalContentExclusionPolicy[];
}
/**
 * If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionUrlsConfig".
 */
/** @experimental */
export interface PermissionUrlsConfig {
  /**
   * If true, the runtime allows access to all URLs without prompting. Initial allow-list is ignored when this is true.
   */
  unrestricted?: boolean;
  /**
   * Initial list of allowed URL/domain patterns. Patterns may include path components. Ignored when `unrestricted` is true.
   */
  initialAllowed?: string[];
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsConfigureResult".
 */
/** @experimental */
export interface PermissionsConfigureResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsFolderTrustAddTrustedResult".
 */
/** @experimental */
export interface PermissionsFolderTrustAddTrustedResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * No parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsGetModeRequest".
 */
/** @experimental */
export interface PermissionsGetModeRequest {}
/**
 * Current permission mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsGetModeResult".
 */
/** @experimental */
export interface PermissionsGetModeResult {
  mode: PermissionMode;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsLocationsAddToolApprovalResult".
 */
/** @experimental */
export interface PermissionsLocationsAddToolApprovalResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Scope and add/remove instructions for modifying session- or location-scoped permission rules.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsModifyRulesParams".
 */
/** @experimental */
export interface PermissionsModifyRulesParams {
  scope: PermissionsModifyRulesScope;
  /**
   * Rules to add to the scope. Applied before `remove`/`removeAll`.
   */
  add?: PermissionRule[];
  /**
   * Specific rules to remove from the scope. Ignored when `removeAll` is true.
   */
  remove?: PermissionRule[];
  /**
   * When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale.
   */
  removeAll?: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsModifyRulesResult".
 */
/** @experimental */
export interface PermissionsModifyRulesResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsNotifyPromptShownResult".
 */
/** @experimental */
export interface PermissionsNotifyPromptShownResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPathsAddResult".
 */
/** @experimental */
export interface PermissionsPathsAddResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * No parameters; returns the session's allow-listed directories.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPathsListRequest".
 */
/** @experimental */
export interface PermissionsPathsListRequest {}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPathsUpdatePrimaryResult".
 */
/** @experimental */
export interface PermissionsPathsUpdatePrimaryResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * No parameters; returns currently-pending permission requests for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsPendingRequestsRequest".
 */
/** @experimental */
export interface PermissionsPendingRequestsRequest {}
/**
 * Clears session-scoped tool permission approvals, and optionally the location-scoped ones.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsResetSessionApprovalsRequest".
 */
/** @experimental */
export interface PermissionsResetSessionApprovalsRequest {
  /**
   * Whether location-scoped approvals are cleared too. Defaults to `true`.
   */
  includeLocation?: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsResetSessionApprovalsResult".
 */
/** @experimental */
export interface PermissionsResetSessionApprovalsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Allow-all toggle for tool permission requests, with an optional telemetry source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllRequest".
 */
/** @experimental */
export interface PermissionsSetApproveAllRequest {
  /**
   * Whether to auto-approve all tool permission requests
   */
  enabled: boolean;
  source?: PermissionsSetApproveAllSource;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetApproveAllResult".
 */
/** @experimental */
export interface PermissionsSetApproveAllResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Permission mode to apply for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetModeRequest".
 */
/** @experimental */
export interface PermissionsSetModeRequest {
  mode: PermissionMode;
  /**
   * Optional judge model id for assisted mode. When omitted, the session resolves the provider default: `gpt-5.5` for CAPI sessions and the active session model for BYOK sessions.
   */
  assistedApprovalModel?: string;
  source?: PermissionModeSource;
}
/**
 * Indicates whether the requested permission mode was applied and reports the authoritative post-mutation mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetModeResult".
 */
/** @experimental */
export interface PermissionsSetModeResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
  mode: PermissionMode;
}
/**
 * Toggles whether permission prompts should be bridged into session events for this client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetRequiredRequest".
 */
/** @experimental */
export interface PermissionsSetRequiredRequest {
  /**
   * Whether the client wants `permission.requested` events bridged from the session-owned permission service. CLI clients that render prompt UI set this to `true` for as long as their listener is mounted; headless callers leave it unset (the default is `false`).
   */
  required: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsSetRequiredResult".
 */
/** @experimental */
export interface PermissionsSetRequiredResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Indicates whether the operation succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionsUrlsSetUnrestrictedModeResult".
 */
/** @experimental */
export interface PermissionsUrlsSetUnrestrictedModeResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
}
/**
 * Whether the URL-permission policy should run in unrestricted mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PermissionUrlsSetUnrestrictedModeParams".
 */
/** @experimental */
export interface PermissionUrlsSetUnrestrictedModeParams {
  /**
   * Whether to allow access to all URLs without prompting. Toggles the runtime's URL-permission policy in place.
   */
  enabled: boolean;
}
/**
 * Optional message to echo back to the caller.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PingRequest".
 */
/** @experimental */
export interface PingRequest {
  /**
   * Optional message to echo back
   */
  message?: string;
}
/**
 * Server liveness response, including the echoed message, current server timestamp, and protocol version.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PingResult".
 */
/** @experimental */
export interface PingResult {
  /**
   * Echoed message (or default greeting)
   */
  message: string;
  /**
   * ISO 8601 timestamp when the server handled the ping
   */
  timestamp: string;
  /**
   * Server protocol version number
   */
  protocolVersion: number;
}
/**
 * Existence, contents, and resolved path of the session plan file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanReadResult".
 */
/** @experimental */
export interface PlanReadResult {
  /**
   * Whether the plan file exists in the workspace
   */
  exists: boolean;
  /**
   * The content of the plan file, or null if it does not exist
   */
  content: string | null;
  /**
   * Absolute file path of the plan file, or null if workspace is not enabled
   */
  path: string | null;
}
/**
 * Todo rows read from the session SQL database. Empty when no session database is available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanReadSqlTodosResult".
 */
/** @experimental */
export interface PlanReadSqlTodosResult {
  /**
   * Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables.
   */
  rows: PlanSqlTodosRow[];
}
/**
 * A single todo row read from the session SQL `todos` table. All fields are optional because the SQL schema is best-effort and the agent may not have populated every column.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanSqlTodosRow".
 */
/** @experimental */
export interface PlanSqlTodosRow {
  /**
   * Todo identifier.
   */
  id?: string;
  /**
   * Todo title.
   */
  title?: string;
  /**
   * Todo description.
   */
  description?: string;
  /**
   * Todo status.
   */
  status?: string;
  /**
   * Todo creation time, as stored by the session SQL schema's `datetime('now')` default: `YYYY-MM-DD HH:MM:SS` in UTC. Lets clients attribute todos to the work item that created them (e.g. scoping a goal's progress to the todos it produced) rather than to the whole session.
   */
  createdAt?: string;
}
/**
 * Todo rows + dependency edges read from the session SQL database.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanReadSqlTodosWithDependenciesResult".
 */
/** @experimental */
export interface PlanReadSqlTodosWithDependenciesResult {
  /**
   * Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables. Empty when no database, no todos table, or the SELECT failed.
   */
  rows: PlanSqlTodosRow[];
  /**
   * Edges from the session SQL todo_deps table. Empty when no database, no todo_deps table, or the SELECT failed. Read independently from `rows`, so a broken todo_deps table does not affect the rows result and vice versa.
   */
  dependencies: PlanSqlTodoDependency[];
}
/**
 * A single dependency edge read from the session SQL `todo_deps` table, indicating that one todo must complete before another.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanSqlTodoDependency".
 */
/** @experimental */
export interface PlanSqlTodoDependency {
  /**
   * ID of the todo that has the dependency.
   */
  todoId: string;
  /**
   * ID of the todo it depends on.
   */
  dependsOn: string;
}
/**
 * Replacement contents to write to the session plan file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PlanUpdateRequest".
 */
/** @experimental */
export interface PlanUpdateRequest {
  /**
   * The new content for the plan file
   */
  content: string;
}
/**
 * Session plugin metadata, with name, marketplace, optional version, and enabled state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Plugin".
 */
/** @experimental */
export interface Plugin {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from
   */
  marketplace: string;
  /**
   * Installed version
   */
  version?: string;
  /**
   * Whether the plugin is currently enabled
   */
  enabled: boolean;
}
/**
 * Result of installing a plugin.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginInstallResult".
 */
/** @experimental */
export interface PluginInstallResult {
  plugin: InstalledPluginInfo;
  /**
   * Number of skills discovered and installed from the plugin
   */
  skillsInstalled: number;
  /**
   * Optional post-install message provided by the plugin (e.g. setup instructions)
   */
  postInstallMessage?: string;
  /**
   * Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
   */
  deprecationWarning?: string;
}
/**
 * Plugins installed for the session, with their enabled state and version metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginList".
 */
/** @experimental */
export interface PluginList {
  /**
   * Installed plugins
   */
  plugins: Plugin[];
}
/**
 * Plugins installed in user/global state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginListResult".
 */
/** @experimental */
export interface PluginListResult {
  /**
   * Installed plugins
   */
  plugins: InstalledPluginInfo[];
}
/**
 * Trusted built-in plugin directories to use for this runtime process.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsBuiltinSetRequest".
 */
/** @experimental */
export interface PluginsBuiltinSetRequest {
  /**
   * Complete replacement set of trusted built-in plugin directories. Every entry must be an absolute local filesystem path no longer than 4096 characters.
   *
   * @maxItems 64
   */
  paths: string[];
}
/**
 * Plugin names (or specs) to disable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsDisableRequest".
 */
/** @experimental */
export interface PluginsDisableRequest {
  /**
   * Plugin names or "plugin@marketplace" specs to disable. Unknown names are ignored. Non-marketplace direct installs cannot be disabled via this API; uninstall them instead. Plugin-owned MCP servers are stopped in active sessions immediately; other plugin contributions remain available until each session reloads plugins.
   */
  names: string[];
}
/**
 * Plugin names (or specs) to enable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsEnableRequest".
 */
/** @experimental */
export interface PluginsEnableRequest {
  /**
   * Plugin names or "plugin@marketplace" specs to enable. Unknown names are ignored. Non-marketplace direct installs are always enabled and cannot be toggled via this API.
   */
  names: string[];
}
/**
 * Plugin source and optional working directory for relative-path resolution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsInstallRequest".
 */
/** @experimental */
export interface PluginsInstallRequest {
  /**
   * Plugin install spec. Accepts the same forms as the CLI: "plugin@marketplace" (marketplace install), "owner/repo" or "owner/repo:subpath" (GitHub direct), an http/https/ssh URL, or a local path. Direct (non-marketplace) installs are deprecated and will produce a deprecationWarning in the result.
   */
  source: string;
  /**
   * Working directory used to resolve relative local paths in `source`. Defaults to the server's current working directory.
   */
  workingDirectory?: string;
}
/**
 * Marketplace source and optional working directory for relative-path resolution.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsMarketplacesAddRequest".
 */
/** @experimental */
export interface PluginsMarketplacesAddRequest {
  /**
   * Marketplace source. Accepts the same forms as the CLI: "owner/repo" or "owner/repo#ref" (GitHub), an http/https/ssh URL (optionally with #ref), a git scp-style URL (user@host:path), or a local path. The marketplace's own name (from its manifest) is used as the registration key.
   */
  source: string;
  /**
   * Working directory used to resolve relative local paths in `source`. Defaults to the server's current working directory.
   */
  workingDirectory?: string;
}
/**
 * Name of the marketplace whose plugin catalog to fetch.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsMarketplacesBrowseRequest".
 */
/** @experimental */
export interface PluginsMarketplacesBrowseRequest {
  /**
   * Marketplace name to browse
   */
  name: string;
}

/** @experimental */
export interface PluginsMarketplacesRefreshRequest {
  /**
   * Marketplace name to refresh. When omitted, every registered marketplace is refreshed.
   */
  name?: string;
}
/**
 * Name of the marketplace to remove and an optional force flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsMarketplacesRemoveRequest".
 */
/** @experimental */
export interface PluginsMarketplacesRemoveRequest {
  /**
   * Marketplace name to remove
   */
  name: string;
  /**
   * When true, also uninstall every plugin sourced from this marketplace. When false (default), removal is a no-op if any plugin from this marketplace is installed and the dependent plugin names are returned in the result.
   */
  force?: boolean;
}
/**
 * Name (or spec) of the plugin to uninstall.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsUninstallRequest".
 */
/** @experimental */
export interface PluginsUninstallRequest {
  /**
   * Plugin name or "plugin@marketplace" spec to uninstall. When ambiguous, prefer the fully-qualified spec.
   */
  name: string;
  /**
   * Stable source identity for a direct (non-marketplace) install. Disambiguates uninstall when multiple installed plugins share the same name.
   */
  directSourceId?: string | null;
}
/**
 * Name (or spec) of the plugin to update.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginsUpdateRequest".
 */
/** @experimental */
export interface PluginsUpdateRequest {
  /**
   * Plugin name or "plugin@marketplace" spec to update.
   */
  name: string;
}
/**
 * Per-plugin result from updating all plugins, with versions, skills installed, success flag, and optional error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginUpdateAllEntry".
 */
/** @experimental */
export interface PluginUpdateAllEntry {
  /**
   * Plugin name that was updated
   */
  name: string;
  /**
   * Marketplace the plugin came from. Empty string ("") for direct installs.
   */
  marketplace: string;
  /**
   * Whether the update succeeded for this plugin
   */
  success: boolean;
  /**
   * Previously installed version, when available
   */
  previousVersion?: string;
  /**
   * Version after the update, when available
   */
  newVersion?: string;
  /**
   * Number of skills installed after the update (success only)
   */
  skillsInstalled?: number;
  /**
   * Error message (failure only)
   */
  error?: string;
}
/**
 * Result of updating all installed plugins.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginUpdateAllResult".
 */
/** @experimental */
export interface PluginUpdateAllResult {
  /**
   * Per-plugin update results in deterministic order.
   */
  results: PluginUpdateAllEntry[];
}
/**
 * Result of updating a single plugin.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PluginUpdateResult".
 */
/** @experimental */
export interface PluginUpdateResult {
  /**
   * Version that was previously installed, when available
   */
  previousVersion?: string;
  /**
   * Version after the update, when reported by the plugin manifest
   */
  newVersion?: string;
  /**
   * Number of skills discovered and installed after the update
   */
  skillsInstalled: number;
}
/**
 * Serializable definition of a caller-implemented tool whose execution is handled over the SDK connection.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProtocolExternalToolDefinition".
 */
/** @experimental */
export interface ProtocolExternalToolDefinition {
  /**
   * Unique model-visible tool name.
   */
  name: string;
  /**
   * Model-visible explanation of what the tool does.
   */
  description: string;
  /**
   * Optional human-readable display title.
   */
  title?: string;
  /**
   * JSON Schema describing the tool's input arguments.
   */
  parameters?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Whether this definition replaces a built-in tool with the same name.
   */
  overridesBuiltInTool?: boolean;
  /**
   * Whether execution bypasses the normal tool permission prompt.
   */
  skipPermission?: boolean;
  defer?: ProtocolExternalToolDefer;
  /**
   * Whether the tool executes commands in a terminal.
   */
  isTerminal?: boolean;
  /**
   * Optional caller-defined metadata associated with the tool.
   */
  metadata?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * BYOK providers and/or models to add to the session's registry at runtime. Both fields are optional; provide providers, models, or both.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderAddRequest".
 */
/** @experimental */
export interface ProviderAddRequest {
  /**
   * Named BYOK provider connections to register, additive to any providers already in the registry. Each name must be unique across the registry and must not contain '/'.
   */
  providers?: NamedProviderConfig[];
  /**
   * BYOK model definitions to register. Each must reference a provider that is already registered or included in this same call. Selection ids (`provider/id`) must be unique across the registry.
   */
  models?: ProviderModelConfig[];
}
/**
 * A BYOK model definition referencing a named provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderModelConfig".
 */
/** @experimental */
export interface ProviderModelConfig {
  /**
   * Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`.
   */
  id: string;
  /**
   * Name of the configured provider that serves this model.
   */
  provider: string;
  /**
   * The model name sent to the provider API for inference. Defaults to `id`.
   */
  wireModel?: string;
  /**
   * Well-known base model id used for behavior/capability/config lookup. Defaults to `id`.
   */
  modelId?: string;
  /**
   * Display name for model pickers. Defaults to the provider-qualified selection id (`provider/id`).
   */
  name?: string;
  /**
   * Maximum prompt/input tokens for the model.
   */
  maxPromptTokens?: number;
  /**
   * Maximum context window tokens for the model.
   */
  maxContextWindowTokens?: number;
  /**
   * Maximum output tokens for the model.
   */
  maxOutputTokens?: number;
  capabilities?: ModelCapabilitiesOverride;
}
/**
 * The selectable model entries synthesized for the models added by this call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderAddResult".
 */
/** @experimental */
export interface ProviderAddResult {
  /**
   * Synthesized selectable model entries for the newly added BYOK models, each under its provider-qualified selection id (`provider/id`). Empty when only providers were added.
   */
  models: JsonValue[];
}
/**
 * Custom model-provider configuration (BYOK).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderConfig".
 */
/** @experimental */
export interface ProviderConfig {
  type?: ProviderConfigType;
  wireApi?: ProviderConfigWireApi;
  transport?: ProviderConfigTransport;
  /**
   * API endpoint URL.
   */
  baseUrl: string;
  /**
   * API key. Optional for local providers like Ollama.
   */
  apiKey?: string;
  /**
   * Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set.
   */
  bearerToken?: string;
  azure?: ProviderConfigAzure;
  /**
   * Well-known model ID used for capability lookup. When set, agent behavior config and token limits are inferred from this model.
   */
  modelId?: string;
  modelCapabilities?: ModelCapabilitiesOverride;
  /**
   * Provider name used for model and telemetry attribution.
   */
  providerName?: string;
  /**
   * The model identifier sent to the provider API for inference (the "wire" model), as opposed to modelId which is the well-known base.
   */
  wireModel?: string;
  /**
   * Maximum prompt/input tokens for the model.
   */
  maxPromptTokens?: number;
  /**
   * Maximum context window tokens for the model.
   */
  maxContextWindowTokens?: number;
  /**
   * Maximum output tokens for the model.
   */
  maxOutputTokens?: number;
  /**
   * Custom HTTP headers to include in all outbound requests to the provider.
   */
  headers?: {
    [k: string]: string | undefined;
  };
  /**
   * When true, the SDK client supplies bearer tokens on demand: the runtime calls the client-session `providerToken.getToken` callback before each request and applies the returned token as an `Authorization: Bearer <token>` header. This is the bearer/OAuth scheme used by Azure AD / managed-identity tokens and provider OAuth access tokens (including Anthropic's), not a provider-specific API-key header such as Anthropic's `x-api-key`. The token-acquiring function itself stays on the SDK side and is never serialized; only this flag crosses the wire. When set alongside `apiKey`/`bearerToken`, the callback takes precedence: the runtime applies the token returned by `providerToken.getToken` as the `Authorization: Bearer` header for each request and does not send the static credential.
   */
  hasBearerTokenProvider?: boolean;
}
/**
 * A snapshot of the provider endpoint the session is currently configured to talk to.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderEndpoint".
 */
/** @experimental */
export interface ProviderEndpoint {
  type: ProviderEndpointType;
  wireApi?: ProviderEndpointWireApi;
  transport?: ProviderEndpointTransport;
  /**
   * Base URL to pass to the LLM client library.
   */
  baseUrl: string;
  /**
   * A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests.
   */
  apiKey?: string;
  /**
   * HTTP headers the caller must include on every outbound request.
   */
  headers: {
    [k: string]: string | undefined;
  };
  sessionToken?: ProviderSessionToken;
}
/**
 * Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderSessionToken".
 */
/** @experimental */
export interface ProviderSessionToken {
  /**
   * The short-lived token value.
   */
  token: string;
  /**
   * HTTP header name the token must be sent under.
   */
  header: string;
  /**
   * The model the token is bound to, when applicable. When set, the token is only valid for requests against this model.
   */
  model?: string;
  /**
   * When the token expires, if known. Callers should refresh by calling `getEndpoint` again before this time, or reactively on any 401/403 response from `baseUrl`.
   */
  expiresAt?: string;
}
/**
 * Asks the SDK client to acquire a bearer token for a BYOK provider whose config set `hasBearerTokenProvider: true`. Issued by the runtime before each outbound model request; the runtime does no caching, so this is sent once per request.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderTokenAcquireRequest".
 */
/** @experimental */
export interface ProviderTokenAcquireRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Name of the BYOK provider needing a token. For the legacy whole-session provider this is the implicit provider name; for named providers it is the configured provider name.
   */
  providerName: string;
}
/**
 * A bearer token supplied by the SDK client for a BYOK provider. The runtime sets it as `Authorization: Bearer <token>` on the outbound request and does no caching; the SDK consumer owns token caching and refresh.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ProviderTokenAcquireResult".
 */
/** @experimental */
export interface ProviderTokenAcquireResult {
  /**
   * The bearer token value (without the `Bearer ` prefix).
   */
  token: string;
}
/**
 * File attachment
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentFile".
 */
/** @experimental */
export interface PushAttachmentFile {
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
  lineRange?: PushAttachmentFileLineRange;
}
/**
 * Optional line range to scope the attachment to a specific section of the file
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentFileLineRange".
 */
/** @experimental */
export interface PushAttachmentFileLineRange {
  /**
   * Start line number (1-based)
   */
  start: number;
  /**
   * End line number (1-based, inclusive)
   */
  end: number;
}
/**
 * Directory attachment
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentDirectory".
 */
/** @experimental */
export interface PushAttachmentDirectory {
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
/**
 * Code selection attachment from an editor
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentSelection".
 */
/** @experimental */
export interface PushAttachmentSelection {
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
  selection: PushAttachmentSelectionDetails;
}
/**
 * Position range of the selection within the file
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentSelectionDetails".
 */
/** @experimental */
export interface PushAttachmentSelectionDetails {
  start: PushAttachmentSelectionDetailsStart;
  end: PushAttachmentSelectionDetailsEnd;
}
/**
 * Start position of the selection
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentSelectionDetailsStart".
 */
/** @experimental */
export interface PushAttachmentSelectionDetailsStart {
  /**
   * Start line number (0-based)
   */
  line: number;
  /**
   * Start character offset within the line (0-based)
   */
  character: number;
}
/**
 * End position of the selection
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentSelectionDetailsEnd".
 */
/** @experimental */
export interface PushAttachmentSelectionDetailsEnd {
  /**
   * End line number (0-based)
   */
  line: number;
  /**
   * End character offset within the line (0-based)
   */
  character: number;
}
/**
 * GitHub issue, pull request, or discussion reference
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubReference".
 */
/** @experimental */
export interface PushAttachmentGitHubReference {
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
  referenceType: PushAttachmentGitHubReferenceType;
  /**
   * Current state of the referenced item (e.g., open, closed, merged)
   */
  state: string;
  /**
   * URL to the referenced item on GitHub
   */
  url: string;
}
/**
 * Pointer to a GitHub commit.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubCommit".
 */
/** @experimental */
export interface PushAttachmentGitHubCommit {
  /**
   * Attachment type discriminator
   */
  type: "github_commit";
  repo: PushGitHubRepoRef;
  /**
   * Full commit SHA
   */
  oid: string;
  /**
   * First line of the commit message
   */
  message: string;
  /**
   * URL to the commit on GitHub
   */
  url: string;
}
/**
 * Pointer to a GitHub repository.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushGitHubRepoRef".
 */
/** @experimental */
export interface PushGitHubRepoRef {
  /**
   * Numeric GitHub repository id
   */
  id?: number;
  /**
   * Repository name (without owner)
   */
  name: string;
  /**
   * Repository owner login (user or organization)
   */
  owner: string;
}
/**
 * Pointer to a GitHub release.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubRelease".
 */
/** @experimental */
export interface PushAttachmentGitHubRelease {
  /**
   * Attachment type discriminator
   */
  type: "github_release";
  repo: PushGitHubRepoRef;
  /**
   * Git tag the release is anchored to
   */
  tagName: string;
  /**
   * Human-readable release name
   */
  name: string;
  /**
   * URL to the release on GitHub
   */
  url: string;
}
/**
 * Pointer to a GitHub Actions job.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubActionsJob".
 */
/** @experimental */
export interface PushAttachmentGitHubActionsJob {
  /**
   * Attachment type discriminator
   */
  type: "github_actions_job";
  repo: PushGitHubRepoRef;
  /**
   * Job id within the workflow run
   */
  jobId: number;
  /**
   * Display name of the job
   */
  jobName: string;
  /**
   * Display name of the workflow the job ran in
   */
  workflowName: string;
  /**
   * URL to the job on GitHub
   */
  url: string;
  /**
   * Terminal conclusion of the job when finished (e.g., success, failure, cancelled). Absent for in-progress jobs.
   */
  conclusion?: string;
}
/**
 * Pointer to a GitHub repository.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubRepository".
 */
/** @experimental */
export interface PushAttachmentGitHubRepository {
  /**
   * Attachment type discriminator
   */
  type: "github_repository";
  repo: PushGitHubRepoRef;
  /**
   * URL to the repository on GitHub
   */
  url: string;
  /**
   * Short description of the repository
   */
  description?: string;
  /**
   * Git ref this attachment is anchored at (branch, tag, or commit). When absent the default branch is implied.
   */
  ref?: string;
}
/**
 * Pointer to a single-file diff. At least one of `head` and `base` must be present.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubFileDiff".
 */
/** @experimental */
export interface PushAttachmentGitHubFileDiff {
  /**
   * Attachment type discriminator
   */
  type: "github_file_diff";
  /**
   * URL to the diff on GitHub (e.g., a commit, compare, or PR-file URL)
   */
  url: string;
  head?: PushAttachmentGitHubFileDiffSide;
  base?: PushAttachmentGitHubFileDiffSide;
}
/**
 * One side of a file diff (head or base)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubFileDiffSide".
 */
/** @experimental */
export interface PushAttachmentGitHubFileDiffSide {
  repo: PushGitHubRepoRef;
  /**
   * Git ref (branch, tag, or commit SHA) the file is read at
   */
  ref: string;
  /**
   * Repository-relative path to the file
   */
  path: string;
}
/**
 * Pointer to a comparison between two git revisions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubTreeComparison".
 */
/** @experimental */
export interface PushAttachmentGitHubTreeComparison {
  /**
   * Attachment type discriminator
   */
  type: "github_tree_comparison";
  /**
   * URL to the comparison on GitHub
   */
  url: string;
  base: PushAttachmentGitHubTreeComparisonSide;
  head: PushAttachmentGitHubTreeComparisonSide;
}
/**
 * One side of a tree comparison (head or base)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubTreeComparisonSide".
 */
/** @experimental */
export interface PushAttachmentGitHubTreeComparisonSide {
  repo: PushGitHubRepoRef;
  /**
   * Git revision (branch, tag, or commit SHA)
   */
  revision: string;
}
/**
 * Generic GitHub URL reference.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubUrl".
 */
/** @experimental */
export interface PushAttachmentGitHubUrl {
  /**
   * Attachment type discriminator
   */
  type: "github_url";
  /**
   * URL to the GitHub resource
   */
  url: string;
}
/**
 * Pointer to a file in a GitHub repository at a specific ref.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubFile".
 */
/** @experimental */
export interface PushAttachmentGitHubFile {
  /**
   * Attachment type discriminator
   */
  type: "github_file";
  repo: PushGitHubRepoRef;
  /**
   * Git ref the file is read at (branch, tag, or commit SHA)
   */
  ref: string;
  /**
   * Repository-relative path to the file
   */
  path: string;
  /**
   * URL to the file on GitHub
   */
  url: string;
}
/**
 * Pointer to a line range inside a file in a GitHub repository.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentGitHubSnippet".
 */
/** @experimental */
export interface PushAttachmentGitHubSnippet {
  /**
   * Attachment type discriminator
   */
  type: "github_snippet";
  repo: PushGitHubRepoRef;
  /**
   * Git ref the file is read at (branch, tag, or commit SHA)
   */
  ref: string;
  /**
   * Repository-relative path to the file
   */
  path: string;
  /**
   * URL to the snippet on GitHub (with line anchor)
   */
  url: string;
  lineRange: PushAttachmentFileLineRange;
}
/**
 * Blob attachment with inline base64-encoded data
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "PushAttachmentBlob".
 */
/** @experimental */
export interface PushAttachmentBlob {
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
/**
 * Inputs for starting a deferred-idle drain.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueBeginDeferredIdleDrainRequest".
 */
/** @experimental */
export interface QueueBeginDeferredIdleDrainRequest {
  /**
   * Whether the host still has active background work.
   */
  activeBackgroundWork: boolean;
}
/**
 * Whether a deferred-idle drain should run.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueBeginDeferredIdleDrainResult".
 */
/** @experimental */
export interface QueueBeginDeferredIdleDrainResult {
  /**
   * True when the host should run finishDeferredIdleDrain asynchronously.
   */
  shouldDrain: boolean;
}
/**
 * Internal filter for consuming queued system notifications.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueConsumeSystemNotificationsRequest".
 */
/** @experimental */
export interface QueueConsumeSystemNotificationsRequest {
  /**
   * Opaque runtime-owned filter object.
   */
  filter: JsonValue;
}
/**
 * Inputs for marking session.idle deferred in native state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueDeferSessionIdleRequest".
 */
/** @experimental */
export interface QueueDeferSessionIdleRequest {
  /**
   * Whether the deferred idle was caused by an aborted foreground turn.
   */
  aborted: boolean;
}
/**
 * Parameters for duplicating a queued item.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueDuplicateAtRequest".
 */
/** @experimental */
export interface QueueDuplicateAtRequest {
  /**
   * Stable opaque ID of the queued item to duplicate.
   */
  id: string;
}
/**
 * Result of duplicating a queued item.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueDuplicateAtResult".
 */
/** @experimental */
export interface QueueDuplicateAtResult {
  /**
   * Fresh stable opaque id assigned to the duplicate.
   */
  id: string;
}
/**
 * Result of enqueueing the resume-pending wake item.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueEnqueueResumePendingResult".
 */
/** @experimental */
export interface QueueEnqueueResumePendingResult {
  /**
   * True when a wake item was newly queued.
   */
  queued: boolean;
}
/**
 * Inputs for completing a deferred-idle drain.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueFinishDeferredIdleDrainRequest".
 */
/** @experimental */
export interface QueueFinishDeferredIdleDrainRequest {
  /**
   * Whether the host still has active background work.
   */
  activeBackgroundWork: boolean;
  /**
   * Whether native queued work remains.
   */
  hasPending: boolean;
}
/**
 * Action selected by the native deferred-idle drain.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueFinishDeferredIdleDrainResult".
 */
/** @experimental */
export interface QueueFinishDeferredIdleDrainResult {
  /**
   * One of none, processQueue, or emitSessionIdle.
   */
  action: string;
  /**
   * Whether the deferred idle was caused by an aborted foreground turn.
   */
  aborted: boolean;
}
/**
 * Whether the native queue has pending work.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueHasPendingResult".
 */
/** @experimental */
export interface QueueHasPendingResult {
  /**
   * True when queued or immediate native work is pending.
   */
  hasPending: boolean;
}
/**
 * Parameters for inserting a queued message at a public visible position.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueInsertAtRequest".
 */
/** @experimental */
export interface QueueInsertAtRequest {
  /**
   * Zero-based position in the public visible queue. Values outside the queue clamp to an end.
   */
  position: number;
  message: QueueInsertMessage;
}
/**
 * Serializable message fields accepted by queue.insertAt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueInsertMessage".
 */
/** @experimental */
export interface QueueInsertMessage {
  /**
   * The user message text.
   */
  prompt: string;
  /**
   * Optional user-facing display text.
   */
  displayPrompt?: string;
  /**
   * Optional attachments for the message.
   */
  attachments?: Attachment[];
  agentMode?: SendAgentMode;
  /**
   * Optional provenance source. `system` is rejected: it would hide the inserted row from `pendingItems` and make it unaddressable while still executing, so inserted items must stay visible.
   */
  source?: string;
  /**
   * Whether the message is billable.
   */
  billable?: boolean;
  /**
   * Required tool name for the turn, when any.
   */
  requiredTool?: string;
  /**
   * Per-turn request headers.
   */
  requestHeaders?: {
    [k: string]: string | undefined;
  };
  mode?: SendMode;
  /**
   * Accepted for SendOptions compatibility but ignored; the requested public position controls placement.
   */
  prepend?: boolean;
  /**
   * Accepted for SendOptions compatibility but ignored; insertion scheduling is controlled by the queue drain state.
   */
  wait?: boolean;
  /**
   * Accepted for internal SendOptions compatibility but ignored; delivery is derived from current session activity.
   */
  delivery?: string;
}
/**
 * Result of inserting a queued message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueInsertAtResult".
 */
/** @experimental */
export interface QueueInsertAtResult {
  /**
   * Fresh stable opaque id assigned to the inserted item.
   */
  id: string;
}
/**
 * Parameters for moving a queued item by stable id.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueMoveItemRequest".
 */
/** @experimental */
export interface QueueMoveItemRequest {
  /**
   * Stable opaque queued-item id.
   */
  id: string;
  /**
   * Zero-based target position in the public visible queue. Values outside the queue clamp to an end.
   */
  toPosition: number;
}
/**
 * Result of moving a queued item.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueMoveItemResult".
 */
/** @experimental */
export interface QueueMoveItemResult {
  /**
   * True when the item changed position; false when it was already at the requested position.
   */
  changed: boolean;
}
/**
 * User-facing pending queue entry, with kind and display text for a queued message, slash command, or model change.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuePendingItems".
 */
/** @experimental */
export interface QueuePendingItems {
  /**
   * Stable opaque id for the canonical queued item. Batch rows share one id.
   */
  id: string;
  /**
   * Stable identity of the queued user message. Present for message rows and absent for slash commands and model changes.
   */
  messageId?: string;
  kind: QueuePendingItemsKind;
  /**
   * Human-readable text to display for this queue entry in the UI
   */
  displayText: string;
  agentMode: SendAgentMode;
}
/**
 * Snapshot of the session's pending queued items and immediate-steering messages.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueuePendingItemsResult".
 */
/** @experimental */
export interface QueuePendingItemsResult {
  /**
   * Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.
   */
  items: QueuePendingItems[];
  /**
   * Display text for messages currently in the immediate steering queue (interjections sent during a running turn).
   */
  steeringMessages: string[];
  /**
   * How many leading entries of `steeringMessages` have already been folded into the running turn (and so have an emitted `user.message`), as opposed to still waiting for one. Absent for hosts that do not distinguish the two.
   */
  inFlightSteeringCount?: number;
}
/**
 * Parameters for removing a queued item by stable id.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueRemoveAtRequest".
 */
/** @experimental */
export interface QueueRemoveAtRequest {
  /**
   * Stable opaque ID of the queued item to remove.
   */
  id: string;
}
/**
 * Result of removing a queued item.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueRemoveAtResult".
 */
/** @experimental */
export interface QueueRemoveAtResult {
  /**
   * True when the addressed item was removed.
   */
  removed: boolean;
}
/**
 * Indicates whether a user-facing pending item was removed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueRemoveMostRecentResult".
 */
/** @experimental */
export interface QueueRemoveMostRecentResult {
  /**
   * True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
   */
  removed: boolean;
}
/**
 * Parameters for steering a queued message into a live turn.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueSendNowRequest".
 */
/** @experimental */
export interface QueueSendNowRequest {
  /**
   * Stable opaque ID of the queued item to steer into the live turn.
   */
  id: string;
}
/**
 * Result of trying to steer a queued message into a live turn.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueSendNowResult".
 */
/** @experimental */
export interface QueueSendNowResult {
  /**
   * True when the item was accepted into the steering lane; false when no main turn was live.
   */
  steered: boolean;
}
/**
 * Parameters for acquiring or releasing the queued-lane drain pause. Acquisition is exclusive and non-idempotent: `paused: true` against an already-paused session fails with `queue_already_paused`. The pause is never released automatically — it is not tied to the caller's lifetime, so a client that exits without sending `paused: false` leaves the lane frozen. Release is unowned: `paused: false` clears the pause for any caller, including one that never acquired it.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueSetDrainPausedRequest".
 */
/** @experimental */
export interface QueueSetDrainPausedRequest {
  /**
   * Whether queued-lane draining should be paused.
   */
  paused: boolean;
}
/**
 * Internal snapshot of native queue state for local session orchestration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueSnapshotResult".
 */
/** @experimental */
export interface QueueSnapshotResult {
  /**
   * User-facing pending items in FIFO order.
   */
  items: QueuePendingItems[];
  /**
   * Immediate steering messages waiting for an active turn.
   */
  steeringMessages: string[];
  /**
   * Insertion orders for queued items, aligned with `items`.
   */
  itemOrders?: number[];
  /**
   * Insertion orders for immediate steering messages, aligned with `steeringMessages`.
   */
  steeringMessageOrders?: number[];
}
/**
 * Parameters for editing a single queued message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueUpdateTextRequest".
 */
/** @experimental */
export interface QueueUpdateTextRequest {
  /**
   * Stable opaque ID of the queued item to edit.
   */
  id: string;
  /**
   * Replacement prompt sent to the model.
   */
  prompt: string;
  /**
   * Optional replacement prompt displayed to the user.
   */
  displayPrompt?: string;
}
/**
 * Result of editing a queued message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "QueueUpdateTextResult".
 */
/** @experimental */
export interface QueueUpdateTextResult {
  /**
   * True when the stored text changed.
   */
  updated: boolean;
}
/**
 * Event type to register consumer interest for, used by runtime gating logic.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RegisterEventInterestParams".
 */
/** @experimental */
export interface RegisterEventInterestParams {
  /**
   * The event type the consumer wants the runtime to treat as 'observed' for behavior-switching gating. Some runtime code paths inspect whether any consumer is interested in a specific event type and choose a different implementation accordingly (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates interactive OAuth token acquisition to the consumer via `mcp.oauth_required` events; when no interest is registered the runtime still attempts non-interactive reconnect from cached or refreshable tokens, and only marks the server `needs-auth` if usable credentials are unavailable — it does not open a browser or start interactive OAuth without a consumer). SDK clients that long-poll events do NOT automatically appear as listeners to these gating checks — they must explicitly call `registerInterest` for each event type they want the runtime to count as having a consumer. Multiple registrations for the same event type from the same or different consumers are tracked independently and must each be released. See: `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`, `session_limits_exhausted.requested`, `user_input.requested`, `elicitation.requested`, `command.queued`, `exit_plan_mode.requested`.
   */
  eventType: string;
}
/**
 * Opaque handle representing an event-type interest registration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RegisterEventInterestResult".
 */
/** @experimental */
export interface RegisterEventInterestResult {
  /**
   * Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.
   */
  handle: string;
}
/**
 * Params to attach an extension loader's tools to a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RegisterExtensionToolsParams".
 */
/** @experimental */
/** @internal */
export interface RegisterExtensionToolsParams {
  /**
   * Session to register extension tools on.
   */
  sessionId: string;
  /**
   * In-process ExtensionLoader handle used only by the CLI and excluded from the public SDK surface.
   *
   * @internal
   *
   * @internal
   */
  loader: OpaqueInProcessValue;
  options?: SessionsRegisterExtensionToolsOnSessionOptions;
}
/**
 * Optional registration options.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsRegisterExtensionToolsOnSessionOptions".
 */
/** @experimental */
export interface SessionsRegisterExtensionToolsOnSessionOptions {
  /**
   * In-process `() => boolean` gating callback used only by the CLI.
   *
   * @internal
   */
  enabled?: OpaqueInProcessValue;
}
/**
 * Handle for releasing the extension tool registration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RegisterExtensionToolsResult".
 */
/** @experimental */
/** @internal */
export interface RegisterExtensionToolsResult {
  /**
   * In-process unsubscribe function used only by the CLI.
   *
   * @internal
   *
   * @internal
   */
  unsubscribe: OpaqueInProcessValue;
}
/**
 * Opaque handle previously returned by `registerInterest` to release.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ReleaseEventInterestParams".
 */
/** @experimental */
export interface ReleaseEventInterestParams {
  /**
   * Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown or already-released handle is a no-op (returns success). When the last outstanding handle for an event type is released, the runtime reverts to its 'no consumer' code path for that event type.
   */
  handle: string;
}
/**
 * Configuration for the runtime-managed remote-control singleton.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlConfig".
 */
/** @experimental */
export interface RemoteControlConfig {
  /**
   * Whether remote export should be enabled.
   */
  remote: boolean;
  /**
   * Whether the MC session may steer the local session (write mode).
   */
  steerable: boolean;
  /**
   * Whether the user explicitly requested remote (vs. implicit session-sync). Controls warning surfacing for missing-repo cases.
   */
  explicit: boolean;
  /**
   * When true, suppresses timeline messages on successful setup.
   */
  silent: boolean;
  /**
   * Existing Mission Control task ID to attach the exported session to.
   */
  taskId?: string;
  existingMcSession?: RemoteControlConfigExistingMcSession;
}
/**
 * Reattach to an existing MC session without creating a new one.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlConfigExistingMcSession".
 */
/** @experimental */
export interface RemoteControlConfigExistingMcSession {
  /**
   * Existing MC session ID to reattach to.
   */
  mcSessionId: string;
  /**
   * Existing MC task ID for the reattached session.
   */
  mcTaskId: string;
}
/**
 * Remote control is not connected.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStatusOff".
 */
/** @experimental */
export interface RemoteControlStatusOff {
  /**
   * Remote control state tag: not connected.
   */
  state: "off";
}
/**
 * Remote control is in the middle of initial setup.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStatusConnecting".
 */
/** @experimental */
export interface RemoteControlStatusConnecting {
  /**
   * Remote control state tag: connecting.
   */
  state: "connecting";
  /**
   * Session id the connection is attaching to.
   */
  attachedSessionId: string;
}
/**
 * Remote control is connected to a local session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStatusActive".
 */
/** @experimental */
export interface RemoteControlStatusActive {
  /**
   * Remote control state tag: active.
   */
  state: "active";
  /**
   * Session id remote control is pointed at.
   */
  attachedSessionId: string;
  /**
   * MC frontend URL for this session, when known.
   */
  frontendUrl?: string;
  /**
   * Whether the MC session may steer this session.
   */
  isSteerable: boolean;
  /**
   * In-process prompt-manager handle (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. Retained as an optional compatibility field; native remote control does not populate or consume it.
   *
   * @internal
   */
  promptManager?: OpaqueInProcessValue;
  /**
   * True while a read-only/session-sync export is deferred, awaiting the first `user.message` before its MC session exists. Marked internal: this field is excluded from the public SDK surface and is populated only on the CLI in-process path.
   *
   * @internal
   */
  awaitingFirstMessage?: boolean;
}
/**
 * The last setup attempt failed. The singleton is otherwise off.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStatusError".
 */
/** @experimental */
export interface RemoteControlStatusError {
  /**
   * Remote control state tag: setup failed.
   */
  state: "error";
  /**
   * Human-readable error message from the last setup attempt.
   */
  error: string;
  /**
   * Session id the failing setup attempt targeted, when known.
   */
  attachedSessionId?: string;
}
/**
 * Wrapper for the singleton's current status.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStatusResult".
 */
/** @experimental */
export interface RemoteControlStatusResult {
  status: RemoteControlStatus;
}
/**
 * Outcome of a stopRemoteControl call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlStopResult".
 */
/** @experimental */
export interface RemoteControlStopResult {
  status: RemoteControlStatus;
  /**
   * Whether the singleton was actually torn down by this call.
   */
  stopped: boolean;
}
/**
 * Outcome of a transferRemoteControl call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteControlTransferResult".
 */
/** @experimental */
export interface RemoteControlTransferResult {
  status: RemoteControlStatus;
  /**
   * Whether the rebinding actually happened.
   */
  transferred: boolean;
}
/**
 * Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteEnableRequest".
 */
/** @experimental */
export interface RemoteEnableRequest {
  mode?: RemoteSessionMode;
}
/**
 * GitHub URL for the session and a flag indicating whether remote steering is enabled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteEnableResult".
 */
/** @experimental */
export interface RemoteEnableResult {
  /**
   * GitHub frontend URL for this session
   */
  url?: string;
  /**
   * Whether remote steering is enabled
   */
  remoteSteerable: boolean;
}
/**
 * New remote-steerability state to persist as a `session.remote_steerable_changed` event.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteNotifySteerableChangedRequest".
 */
/** @experimental */
export interface RemoteNotifySteerableChangedRequest {
  /**
   * Whether the session now supports remote steering via GitHub. The runtime persists this as a `session.remote_steerable_changed` event so resume/replay sees the up-to-date capability.
   */
  remoteSteerable: boolean;
}
/**
 * Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteNotifySteerableChangedResult".
 */
/** @experimental */
export interface RemoteNotifySteerableChangedResult {}
/**
 * Remote session connection result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionConnectionResult".
 */
/** @experimental */
export interface RemoteSessionConnectionResult {
  /**
   * SDK session ID for the connected remote session.
   */
  sessionId: string;
  metadata: ConnectedRemoteSessionMetadata;
}
/**
 * GitHub repository the remote session belongs to.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionMetadataRepository".
 */
/** @experimental */
export interface RemoteSessionMetadataRepository {
  /**
   * Repository owner.
   */
  owner: string;
  /**
   * Repository name.
   */
  name: string;
  /**
   * Branch associated with the remote session.
   */
  branch: string;
}
/**
 * Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionMetadataValue".
 */
/** @experimental */
export interface RemoteSessionMetadataValue {
  /**
   * Stable session identifier.
   */
  sessionId: string;
  /**
   * Session creation time as an ISO 8601 timestamp.
   */
  startTime: string;
  /**
   * Last-modified time as an ISO 8601 timestamp.
   */
  modifiedTime: string;
  /**
   * Short summary of the session, when one has been derived.
   */
  summary?: string;
  /**
   * Optional human-friendly name set via /rename.
   */
  name?: string;
  /**
   * Always true for remote sessions.
   */
  isRemote: true;
  context?: SessionContext;
  repository: RemoteSessionMetadataRepository;
  /**
   * Backing remote session IDs (most recent first).
   */
  remoteSessionIds: string[];
  /**
   * Pull request number associated with the session.
   */
  pullRequestNumber?: number;
  /**
   * Original remote resource identifier (task ID or PR node ID).
   */
  resourceId?: string;
  taskType?: RemoteSessionMetadataTaskType;
  /**
   * Deadline (ISO 8601) at which a CLI remote session becomes stale without further heartbeats.
   */
  staleAt?: string;
  /**
   * Server-side task state returned by GitHub.
   */
  state?: string;
  hostStatus?: RemoteSessionHostStatus;
  /**
   * Host-supplied human description of what the session is doing right now ("running tests", "waiting for approval"). Optional in the protocol and absent on hosts that do not publish it, so never rely on it -- it enriches `hostStatus`, it does not replace it.
   */
  hostActivity?: string;
}
/**
 * Repository context for the remote session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "RemoteSessionRepository".
 */
/** @experimental */
export interface RemoteSessionRepository {
  /**
   * Repository owner or organization login.
   */
  owner: string;
  /**
   * Repository name.
   */
  name: string;
  /**
   * Optional branch associated with the remote session.
   */
  branch?: string;
}
/**
 * Resolved sandbox configuration.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfig".
 */
/** @experimental */
export interface SandboxConfig {
  /**
   * Whether sandboxing is enabled for the session.
   */
  enabled: boolean;
  userPolicy?: SandboxConfigUserPolicy;
  /**
   * Whether to auto-add the current working directory to readwritePaths. Default: true.
   */
  addCurrentWorkingDirectory?: boolean;
  auth?: SandboxConfigAuth;
  /**
   * Whether to auto-grant read access to tool directories discovered on PATH and in toolchain environment variables (GOROOT, JAVA_HOME, VIRTUAL_ENV, and similar), and to common developer-tool caches, config, and toolchains. Writable grants cover scratch caches, the Unix GitHub CLI cache, and Cargo's registry, git store, and lock/tracker files. A relocated CARGO_HOME gets the same narrow split: registry and git are read-write; bin is read-only; the home root, config.toml, and credentials.toml stay ungranted. Set to false to disable every grant listed above; user-installed toolchains and caches then need explicit userPolicy.filesystem readonlyPaths and readwritePaths entries. The working directory (see addCurrentWorkingDirectory), temporary storage, session log paths, and system locations follow their own rules and stay granted. Default: true (enabled by default; set to false to opt out).
   */
  allowDevToolAccess?: boolean;
}
/**
 * User-managed sandbox policy fragment merged into the auto-discovered base policy.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicy".
 */
/** @experimental */
export interface SandboxConfigUserPolicy {
  filesystem?: SandboxConfigUserPolicyFilesystem;
  network?: SandboxConfigUserPolicyNetwork;
  seatbelt?: SandboxConfigUserPolicySeatbelt;
  experimental?: SandboxConfigUserPolicyExperimental;
}
/**
 * Filesystem rules to merge into the base policy.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicyFilesystem".
 */
/** @experimental */
export interface SandboxConfigUserPolicyFilesystem {
  /**
   * Paths granted read/write access.
   */
  readwritePaths?: string[];
  /**
   * Paths granted read-only access.
   */
  readonlyPaths?: string[];
  /**
   * Paths explicitly denied.
   */
  deniedPaths?: string[];
  /**
   * Whether to clear the policy when the session exits.
   */
  clearPolicyOnExit?: boolean;
}
/**
 * Network rules to merge into the base policy.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicyNetwork".
 */
/** @experimental */
export interface SandboxConfigUserPolicyNetwork {
  /**
   * Whether outbound network traffic is allowed at all.
   */
  allowOutbound?: boolean;
  /**
   * Whether traffic to local/loopback addresses is allowed.
   */
  allowLocalNetwork?: boolean;
  proxy?: SandboxConfigUserPolicyNetworkProxy;
}
/**
 * HTTP proxy configuration for sandboxed traffic.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicyNetworkProxy".
 */
/** @experimental */
export interface SandboxConfigUserPolicyNetworkProxy {
  /**
   * Proxy URL (e.g. http://proxy.example.com:8080). The port is optional and defaults to the scheme's standard port when omitted; an explicit port must be between 1 and 65535. Credentials must not be embedded here — a `user:pass@` authority is rejected; put them in the separate `username`/`password` fields. A credential-free http:// loopback proxy URL is routed through the localhost proxy automatically; loopback covers localhost and any *.localhost subdomain, the whole 127.0.0.0/8 range, ::1, and IPv4-mapped loopback (::ffff:127.0.0.1). An https:// URL, or one with a username/password set, is used as-is.
   */
  url: string;
  /**
   * Optional username for proxy authentication. Combined with the URL (and `password`) into `user:pass@host` when the sandboxed process routes through the proxy.
   */
  username?: string;
  /**
   * Optional password for proxy authentication, combined with the URL at spawn time. The persisted value may be a literal password, a `${secret:…}` reference resolved from the OS keychain, or a `${VAR}`/`$VAR` environment reference; it is resolved just before the sandboxed process routes through the proxy. The /sandbox dialog stores a real password in the OS keychain and persists only a `${secret:…}` placeholder (never plaintext in settings.json); the field is masked in the dialog and redacted by /settings show.
   */
  password?: string;
}
/**
 * macOS seatbelt-specific options.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicySeatbelt".
 */
/** @experimental */
export interface SandboxConfigUserPolicySeatbelt {
  /**
   * Whether the macOS seatbelt profile may access the keychain.
   */
  keychainAccess?: boolean;
}
/**
 * Platform-specific experimental policy fields.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicyExperimental".
 */
/** @experimental */
export interface SandboxConfigUserPolicyExperimental {
  seatbelt?: SandboxConfigUserPolicyExperimentalSeatbelt;
}
/**
 * macOS seatbelt experimental options.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigUserPolicyExperimentalSeatbelt".
 */
/** @experimental */
export interface SandboxConfigUserPolicyExperimentalSeatbelt {
  /**
   * Whether the macOS seatbelt profile may access the keychain.
   */
  keychainAccess?: boolean;
}
/**
 * Credential-injection capability flags applied while the sandbox is enabled. For the same capability independent of sandboxing, and matched to the credential's GitHub host, see `shell.credentials`; the two are additive.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxConfigAuth".
 */
/** @experimental */
export interface SandboxConfigAuth {
  /**
   * Whether to inject git credentials as an `http.<url>.extraheader` so authenticated HTTPS git works inside the sandbox without the shell-based credential helper the sandbox blocks. github.com is served by the Copilot token; every other forge (Azure DevOps, GitHub Enterprise Server, GitLab, ...) by a credential the host resolves from the user's own helper before the sandbox is applied. Default: false (opt-in).
   */
  git?: boolean;
  /**
   * Whether to export `GH_TOKEN` so the `gh` CLI authenticates inside the sandbox without the OS keyring the sandbox blocks. Default: false (opt-in).
   */
  gh?: boolean;
}
/**
 * Managed sandbox enforcement state for a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SandboxEnforcementStatus".
 */
/** @experimental */
export interface SandboxEnforcementStatus {
  /**
   * Whether the effective managed policy requires an available sandbox backend.
   */
  required: boolean;
  /**
   * Whether an enforcement failure has permanently blocked the session.
   */
  blocked: boolean;
  /**
   * The first sandbox enforcement failure that blocked the session.
   */
  reason?: string;
}
/**
 * Register an absolute-time scheduled prompt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleAddAtRequest".
 */
/** @experimental */
export interface ScheduleAddAtRequest {
  /**
   * Epoch milliseconds when the prompt should fire.
   */
  at: number;
  /**
   * Prompt text to enqueue when the schedule fires.
   */
  prompt: string;
  /**
   * Whether the schedule should re-arm after each tick. Defaults to false.
   */
  recurring?: boolean;
  /**
   * Optional display-only prompt label.
   */
  displayPrompt?: string;
}
/**
 * Register a cron scheduled prompt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleAddCronRequest".
 */
/** @experimental */
export interface ScheduleAddCronRequest {
  /**
   * 5-field cron expression.
   */
  cron: string;
  /**
   * Prompt text to enqueue when the schedule fires.
   */
  prompt: string;
  /**
   * Whether the schedule should re-arm after each tick. Defaults to true.
   */
  recurring?: boolean;
  /**
   * Optional display-only prompt label.
   */
  displayPrompt?: string;
  /**
   * IANA timezone for evaluating the cron expression.
   */
  tz?: string;
}
/**
 * Register a relative-interval scheduled prompt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleAddRequest".
 */
/** @experimental */
export interface ScheduleAddRequest {
  /**
   * Human-readable interval such as `30s`, `5m`, or `2h`.
   */
  interval: string;
  /**
   * Prompt text to enqueue when the schedule fires.
   */
  prompt: string;
  /**
   * Whether the schedule should re-arm after each tick. Defaults to true.
   */
  recurring?: boolean;
  /**
   * Optional display-only prompt label.
   */
  displayPrompt?: string;
}
/**
 * Result of registering or re-arming a scheduled prompt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleAddResult".
 */
/** @experimental */
export interface ScheduleAddResult {
  entry?: ScheduleEntry;
  /**
   * User-facing validation error, when registration failed.
   */
  error?: string;
}
/**
 * Scheduled prompt entry with ID, timing (`intervalMs`, `cron`, or `at`), prompt text, recurrence, and next run time.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleEntry".
 */
/** @experimental */
export interface ScheduleEntry {
  /**
   * Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt from the event log).
   */
  id: number;
  /**
   * Interval between scheduled ticks, in milliseconds (relative-interval schedules).
   */
  intervalMs?: number;
  /**
   * 5-field cron expression for a recurring calendar schedule, evaluated in `tz`.
   */
  cron?: string;
  /**
   * IANA timezone the `cron` expression is evaluated in.
   */
  tz?: string;
  /**
   * Absolute fire time (epoch milliseconds) for a one-shot calendar schedule.
   */
  at?: number;
  /**
   * Prompt text that gets enqueued on every tick.
   */
  prompt: string;
  /**
   * Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`).
   */
  recurring: boolean;
  /**
   * True for a self-paced (`dynamic`) schedule: no fixed cadence; the model arms each next run via the `manage_schedule` `wakeup` action. `nextRunAt` is model-controlled.
   */
  selfPaced?: boolean;
  /**
   * Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a skill-invocation schedule). The actual enqueued prompt is `prompt`.
   */
  displayPrompt?: string;
  /**
   * ISO 8601 timestamp when the next tick is scheduled to fire.
   */
  nextRunAt: string;
}
/**
 * Register a self-paced scheduled prompt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleAddSelfPacedRequest".
 */
/** @experimental */
export interface ScheduleAddSelfPacedRequest {
  /**
   * Prompt text to enqueue when the schedule fires.
   */
  prompt: string;
  /**
   * Optional display-only prompt label.
   */
  displayPrompt?: string;
}
/**
 * Whether the session currently has an active self-paced schedule.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleHasSelfPacedResult".
 */
/** @experimental */
export interface ScheduleHasSelfPacedResult {
  /**
   * True when at least one active schedule is self-paced.
   */
  hasSelfPaced: boolean;
}
/**
 * Snapshot of the currently active recurring prompts for this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleList".
 */
/** @experimental */
export interface ScheduleList {
  /**
   * Active scheduled prompts, ordered by id.
   */
  entries: ScheduleEntry[];
}
/**
 * Re-arm a self-paced scheduled prompt.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleRearmSelfPacedRequest".
 */
/** @experimental */
export interface ScheduleRearmSelfPacedRequest {
  /**
   * Id of the self-paced scheduled prompt.
   */
  id: number;
  /**
   * Epoch milliseconds when the prompt should next fire.
   */
  at: number;
}
/**
 * Identifier of the scheduled prompt to remove.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleStopRequest".
 */
/** @experimental */
export interface ScheduleStopRequest {
  /**
   * Id of the scheduled prompt to remove.
   */
  id: number;
}
/**
 * Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ScheduleStopResult".
 */
/** @experimental */
export interface ScheduleStopResult {
  entry?: ScheduleEntry;
}
/**
 * Secret values to add to the redaction filter.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SecretsAddFilterValuesRequest".
 */
/** @experimental */
export interface SecretsAddFilterValuesRequest {
  /**
   * Raw secret values to register for redaction
   */
  values: string[];
}
/**
 * Confirmation that the secret values were registered.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SecretsAddFilterValuesResult".
 */
/** @experimental */
export interface SecretsAddFilterValuesResult {
  /**
   * Whether the values were successfully registered
   */
  ok: true;
}
/**
 * Parameters for session.extensions.sendAttachmentsToMessage.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendAttachmentsToMessageParams".
 */
/** @experimental */
export interface SendAttachmentsToMessageParams {
  /**
   * Optional canvas instance binding the push for provenance. When supplied, the runtime resolves the canvas, verifies it is owned by the calling extension, and stamps canvasId/instanceId onto each extension_context entry. When omitted, no resolution runs and those fields stay unset on the attachment.
   */
  instanceId?: string;
  /**
   * Attachments to push into the next user-message turn. extension_context entries take the slim shape; standard variants take their full AttachmentSchema shape.
   */
  attachments: PushAttachment[];
}
/**
 * A single user message to append to the session as part of a `session.sendMessages` turn
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendMessageItem".
 */
/** @experimental */
export interface SendMessageItem {
  /**
   * The user message text
   */
  prompt: string;
  /**
   * If provided, this is shown in the timeline instead of `prompt`
   */
  displayPrompt?: string;
  /**
   * Optional attachments (files, directories, selections, blobs, GitHub references) to include with this message
   */
  attachments?: Attachment[];
  /**
   * If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
   *
   * @internal
   */
  billable?: boolean;
  /**
   * If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
   */
  requiredTool?: string;
  /**
   * Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent.
   *
   * @internal
   */
  source?: string;
}
/**
 * Parameters for sending zero or more user messages to the session in a single turn. Remote-backed (Mission Control) sessions do not support this method and will return an error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendMessagesRequest".
 */
/** @experimental */
export interface SendMessagesRequest {
  /**
   * The user messages to append to the conversation, in order. May be empty, in which case a single turn runs over the existing history with no new user message.
   */
  messages: SendMessageItem[];
  mode?: SendMode;
  /**
   * If true, adds the messages to the front of the queue instead of the end
   */
  prepend?: boolean;
  agentMode?: SendAgentMode;
  /**
   * Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
   */
  requestHeaders?: {
    [k: string]: string | undefined;
  };
  /**
   * W3C Trace Context traceparent header for distributed tracing of this agent turn
   */
  traceparent?: string;
  /**
   * W3C Trace Context tracestate header for distributed tracing
   */
  tracestate?: string;
  /**
   * If true, await completion of the agentic loop for this turn before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageIds`; the caller can rely on the agent having processed the messages before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
   */
  wait?: boolean;
}
/**
 * Result of sending zero or more user messages
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendMessagesResult".
 */
/** @experimental */
export interface SendMessagesResult {
  /**
   * Unique identifiers assigned to the messages, one per provided message in order. Empty when no messages were provided.
   */
  messageIds: string[];
}
/**
 * Parameters for sending a user message to the session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendRequest".
 */
/** @experimental */
export interface SendRequest {
  /**
   * The user message text
   */
  prompt: string;
  /**
   * If provided, this is shown in the timeline instead of `prompt`
   */
  displayPrompt?: string;
  /**
   * Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message
   */
  attachments?: Attachment[];
  mode?: SendMode;
  /**
   * If true, adds the message to the front of the queue instead of the end
   */
  prepend?: boolean;
  /**
   * If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
   */
  billable?: boolean;
  /**
   * If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
   */
  requiredTool?: string;
  /**
   * Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent.
   *
   * @internal
   */
  source?: string;
  agentMode?: SendAgentMode;
  /**
   * Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
   */
  requestHeaders?: {
    [k: string]: string | undefined;
  };
  /**
   * W3C Trace Context traceparent header for distributed tracing of this agent turn
   */
  traceparent?: string;
  /**
   * W3C Trace Context tracestate header for distributed tracing
   */
  tracestate?: string;
  /**
   * If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
   */
  wait?: boolean;
}
/**
 * Result of sending a user message
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendResult".
 */
/** @experimental */
export interface SendResult {
  /**
   * Unique identifier assigned to the message
   */
  messageId: string;
}
/**
 * Internal request for sending a system notification.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SendSystemNotificationRequest".
 */
/** @experimental */
export interface SendSystemNotificationRequest {
  /**
   * Notification text to deliver to the model.
   */
  message: string;
  /**
   * Optional structured notification kind.
   */
  kind?: JsonValue;
  /**
   * Internal delivery options, including passive policy.
   */
  options?: JsonValue;
}
/**
 * Agents discovered across user, project, plugin, and remote sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ServerAgentList".
 */
/** @experimental */
export interface ServerAgentList {
  /**
   * All discovered agents across all sources
   */
  agents: AgentInfo[];
}
/**
 * Instruction sources discovered across user, repository, and plugin sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ServerInstructionSourceList".
 */
/** @experimental */
export interface ServerInstructionSourceList {
  /**
   * All discovered instruction sources
   */
  sources: InstructionSource[];
}
/**
 * Server-side skill metadata, including name, description, source, enabled/invocable state, path, project path, and argument hint.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ServerSkill".
 */
/** @experimental */
export interface ServerSkill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Canonical slash command name used to invoke the skill, without the leading '/'
   */
  commandName?: string;
  /**
   * Description of what the skill does
   */
  description: string;
  source: SkillSource;
  /**
   * Whether the skill can be invoked by the user as a slash command
   */
  userInvocable: boolean;
  /**
   * Whether the skill is currently enabled (based on global config)
   */
  enabled: boolean;
  /**
   * Absolute path to the skill file
   */
  path?: string;
  /**
   * The project path this skill belongs to (only for project/inherited skills)
   */
  projectPath?: string;
  /**
   * Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
   */
  argumentHint?: string;
}
/**
 * Skills discovered across global and project sources.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ServerSkillList".
 */
/** @experimental */
export interface ServerSkillList {
  /**
   * All discovered skills across all sources
   */
  skills: ServerSkill[];
  /**
   * Messages for skills that failed to load (e.g. malformed SKILL.md). Empty when host skills are excluded so host-local paths are not disclosed to multitenant callers.
   */
  errors?: string[];
}
/**
 * Current activity flags for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionActivity".
 */
/** @experimental */
export interface SessionActivity {
  /**
   * Whether an in-flight operation can currently be aborted.
   */
  abortable: boolean;
  /**
   * Whether the session currently has active work, including running turns or tasks.
   */
  hasActiveWork: boolean;
}
/**
 * Internal GitHub login parameters.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionAuthLoginRequest".
 */
/** @experimental */
export interface SessionAuthLoginRequest {
  /**
   * GitHub host URL
   */
  host: string;
  /**
   * GitHub login
   */
  login: string;
  /**
   * GitHub authentication token
   */
  token: string;
  /**
   * Whether to persist the token after login
   */
  persist?: boolean;
}
/**
 * Parameters identifying a GitHub authentication to log out.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionAuthLogoutUserRequest".
 */
/** @experimental */
export interface SessionAuthLogoutUserRequest {
  authInfo: AuthInfo;
}
/**
 * Authentication status and account metadata for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionAuthStatus".
 */
/** @experimental */
export interface SessionAuthStatus {
  /**
   * Whether the session has resolved authentication
   */
  isAuthenticated: boolean;
  authType?: AuthInfoType;
  /**
   * Authentication host URL
   */
  host?: string;
  /**
   * Authenticated login/username, if available
   */
  login?: string;
  /**
   * Human-readable authentication status description
   */
  statusMessage?: string;
  /**
   * Copilot plan tier (e.g., individual_pro, business)
   */
  copilotPlan?: string;
}
/**
 * Parameters for switching the session's active authentication.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionAuthSwitchRequest".
 */
/** @experimental */
export interface SessionAuthSwitchRequest {
  authInfo: AuthInfo;
  /**
   * Optional token paired with the authentication information
   */
  token?: string;
}
/**
 * Map of sessionId -> bytes freed by removing the session's workspace directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionBulkDeleteResult".
 */
/** @experimental */
export interface SessionBulkDeleteResult {
  /**
   * Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).
   */
  freedBytes: {
    [k: string]: number | undefined;
  };
}
/**
 * The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionEnrichMetadataResult".
 */
/** @experimental */
export interface SessionEnrichMetadataResult {
  /**
   * Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted.
   */
  sessions: LocalSessionMetadataValue[];
}
/**
 * File path, content to append, and optional mode for the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsAppendFileRequest".
 */
/** @experimental */
export interface SessionFsAppendFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Content to append
   */
  content: string;
  /**
   * Optional POSIX-style mode for newly created files
   */
  mode?: number;
}
/**
 * Describes a filesystem error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsError".
 */
/** @experimental */
export interface SessionFsError {
  code: SessionFsErrorCode;
  /**
   * Free-form detail about the error, for logging/diagnostics
   */
  message?: string;
}
/**
 * Path to test for existence in the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsExistsRequest".
 */
/** @experimental */
export interface SessionFsExistsRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Indicates whether the requested path exists in the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsExistsResult".
 */
/** @experimental */
export interface SessionFsExistsResult {
  /**
   * Whether the path exists
   */
  exists: boolean;
}
/**
 * Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsMkdirRequest".
 */
/** @experimental */
export interface SessionFsMkdirRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Create parent directories as needed
   */
  recursive?: boolean;
  /**
   * Optional POSIX-style mode for newly created directories
   */
  mode?: number;
}
/**
 * Directory path whose entries should be listed from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirRequest".
 */
/** @experimental */
export interface SessionFsReaddirRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Names of entries in the requested directory, or a filesystem error if the read failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirResult".
 */
/** @experimental */
export interface SessionFsReaddirResult {
  /**
   * Entry names in the directory
   */
  entries: string[];
  error?: SessionFsError;
}
/**
 * Directory entry returned by session filesystem `readdirWithTypes`, with name and entry type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesEntry".
 */
/** @experimental */
export interface SessionFsReaddirWithTypesEntry {
  /**
   * Entry name
   */
  name: string;
  type: SessionFsReaddirWithTypesEntryType;
}
/**
 * Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesRequest".
 */
/** @experimental */
export interface SessionFsReaddirWithTypesRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReaddirWithTypesResult".
 */
/** @experimental */
export interface SessionFsReaddirWithTypesResult {
  /**
   * Directory entries with type information
   */
  entries: SessionFsReaddirWithTypesEntry[];
  error?: SessionFsError;
}
/**
 * Path of the file to read from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReadFileRequest".
 */
/** @experimental */
export interface SessionFsReadFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * File content as a UTF-8 string, or a filesystem error if the read failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsReadFileResult".
 */
/** @experimental */
export interface SessionFsReadFileResult {
  /**
   * File content as UTF-8 string
   */
  content: string;
  error?: SessionFsError;
}
/**
 * Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsRenameRequest".
 */
/** @experimental */
export interface SessionFsRenameRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Source path using SessionFs conventions
   */
  src: string;
  /**
   * Destination path using SessionFs conventions
   */
  dest: string;
}
/**
 * Path to remove from the client-provided session filesystem, with options for recursive removal and force.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsRmRequest".
 */
/** @experimental */
export interface SessionFsRmRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Remove directories and their contents recursively
   */
  recursive?: boolean;
  /**
   * Ignore errors if the path does not exist
   */
  force?: boolean;
}
/**
 * Optional capabilities declared by the provider
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderCapabilities".
 */
/** @experimental */
export interface SessionFsSetProviderCapabilities {
  /**
   * Whether the provider supports SQLite query/exists operations
   */
  sqlite?: boolean;
}
/**
 * Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderRequest".
 */
/** @experimental */
export interface SessionFsSetProviderRequest {
  /**
   * Initial working directory for sessions
   */
  initialCwd: string;
  /**
   * Path within each session's SessionFs where the runtime stores files for that session
   */
  sessionStatePath: string;
  conventions: SessionFsSetProviderConventions;
  capabilities?: SessionFsSetProviderCapabilities;
}
/**
 * Indicates whether the calling client was registered as the session filesystem provider.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSetProviderResult".
 */
/** @experimental */
export interface SessionFsSetProviderResult {
  /**
   * Whether the provider was set successfully
   */
  success: boolean;
}
/**
 * Indicates whether the per-session SQLite database already exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteExistsResult".
 */
/** @experimental */
export interface SessionFsSqliteExistsResult {
  /**
   * Whether the session database already exists
   */
  exists: boolean;
}
/**
 * SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database. The provider applies its SQLite busy timeout for every call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryRequest".
 */
/** @experimental */
export interface SessionFsSqliteQueryRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * SQL query to execute
   */
  query: string;
  queryType: SessionFsSqliteQueryType;
  /**
   * Optional named bind parameters
   */
  params?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteQueryResult".
 */
/** @experimental */
export interface SessionFsSqliteQueryResult {
  /**
   * For SELECT: array of row objects. For others: empty array.
   */
  rows: {
    [k: string]: JsonValue | undefined;
  }[];
  /**
   * Column names from the result set
   */
  columns: string[];
  /**
   * Number of rows affected (for INSERT/UPDATE/DELETE)
   */
  rowsAffected: number;
  /**
   * SQLite last_insert_rowid() value for INSERT.
   */
  lastInsertRowid?: number;
  error?: SessionFsError;
}
/**
 * Classified SQLite transaction failure. busyOrLocked guarantees rollback; postCommitAmbiguous must never be retried.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteTransactionError".
 */
/** @experimental */
export interface SessionFsSqliteTransactionError {
  errorClass: SessionFsSqliteTransactionErrorClass;
  /**
   * Human-readable transaction failure message.
   */
  message: string;
}
/**
 * Statements to execute atomically. Providers apply busy handling for every call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteTransactionRequest".
 */
/** @experimental */
export interface SessionFsSqliteTransactionRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Ordered SQL statements to execute in one transaction.
   */
  statements: SessionFsSqliteTransactionStatement[];
}
/**
 * One statement in an atomic SQLite transaction.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteTransactionStatement".
 */
/** @experimental */
export interface SessionFsSqliteTransactionStatement {
  /**
   * SQL statement to execute.
   */
  query: string;
  queryType: SessionFsSqliteQueryType;
  /**
   * Optional named bind parameters.
   */
  params?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * Per-statement results, or a classified transaction error.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteTransactionResult".
 */
/** @experimental */
export interface SessionFsSqliteTransactionResult {
  /**
   * Per-statement query results in input order.
   */
  results: SessionFsSqliteQueryResult[];
  error?: SessionFsSqliteTransactionError;
}
/**
 * Path whose metadata should be returned from the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsStatRequest".
 */
/** @experimental */
export interface SessionFsStatRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
}
/**
 * Filesystem metadata for the requested path, or a filesystem error if the stat failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsStatResult".
 */
/** @experimental */
export interface SessionFsStatResult {
  /**
   * Whether the path is a file
   */
  isFile: boolean;
  /**
   * Whether the path is a directory
   */
  isDirectory: boolean;
  /**
   * File size in bytes
   */
  size: number;
  /**
   * ISO 8601 timestamp of last modification
   */
  mtime: string;
  /**
   * ISO 8601 timestamp of creation
   */
  birthtime: string;
  error?: SessionFsError;
}
/**
 * File path, content to write, and optional mode for the client-provided session filesystem.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsWriteFileRequest".
 */
/** @experimental */
export interface SessionFsWriteFileRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
  /**
   * Path using SessionFs conventions
   */
  path: string;
  /**
   * Content to write
   */
  content: string;
  /**
   * Optional POSIX-style mode for newly created files
   */
  mode?: number;
}
/**
 * Installed plugin record for a session, with marketplace, version, install time, enabled state, cache path, and source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPlugin".
 */
/** @experimental */
export interface SessionInstalledPlugin {
  /**
   * Plugin name
   */
  name: string;
  /**
   * Marketplace the plugin came from (empty string for direct repo installs)
   */
  marketplace: string;
  /**
   * Installed version, if known
   */
  version?: string;
  /**
   * Installation timestamp (ISO-8601)
   */
  installed_at: string;
  /**
   * Whether the plugin is currently enabled
   */
  enabled: boolean;
  /**
   * Path where the plugin is cached locally
   */
  cache_path?: string;
  source?: SessionInstalledPluginSource;
  /**
   * Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs.
   */
  source_sha?: string;
  /**
   * Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key.
   */
  installed_from?: string;
}
/**
 * Source descriptor for a direct GitHub plugin install, with `owner/repo`, optional ref or full commit SHA, and optional subpath.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSourceGitHub".
 */
/** @experimental */
export interface SessionInstalledPluginSourceGitHub {
  /**
   * Constant value. Always "github".
   */
  source: "github";
  /**
   * GitHub repository in `owner/repo` form.
   */
  repo: string;
  /**
   * Optional Git ref to resolve.
   */
  ref?: string;
  /**
   * Optional full 40-character hexadecimal commit SHA.
   */
  sha?: string;
  /**
   * Optional repository-relative path to the plugin.
   */
  path?: string;
}
/**
 * Source descriptor for a direct URL plugin install, with URL, optional ref or full commit SHA, and optional subpath.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSourceUrl".
 */
/** @experimental */
export interface SessionInstalledPluginSourceUrl {
  /**
   * Constant value. Always "url".
   */
  source: "url";
  /**
   * URL of the plugin source.
   */
  url: string;
  /**
   * Optional Git ref to resolve.
   */
  ref?: string;
  /**
   * Optional full 40-character hexadecimal commit SHA.
   */
  sha?: string;
  /**
   * Optional source-relative path to the plugin.
   */
  path?: string;
}
/**
 * Source descriptor for a direct local plugin install, with a local filesystem path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionInstalledPluginSourceLocal".
 */
/** @experimental */
export interface SessionInstalledPluginSourceLocal {
  /**
   * Constant value. Always "local".
   */
  source: "local";
  /**
   * Local filesystem path to the plugin.
   */
  path: string;
}
/**
 * Baseline data provenance for a prediction.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionBaselineData".
 */
/** @experimental */
export interface SessionLimitPredictionBaselineData {
  /**
   * Start of the baseline data slice.
   */
  windowStart: string;
  /**
   * End of the baseline data slice.
   */
  windowEnd: string;
}
/**
 * Explainable AI-credit session-limit prediction.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionDetails".
 */
/** @experimental */
export interface SessionLimitPredictionDetails {
  clientType: SessionLimitPredictionClientType;
  /**
   * Model identifier used for lookup.
   */
  modelId: string;
  source: SessionLimitPredictionSource;
  /**
   * Key matched at the source level, such as a model id, family id, or `global`.
   */
  sourceKey: string;
  /**
   * Resolved model family when known.
   */
  family?: string;
  /**
   * Ordered usage tiers and their AI-credit caps.
   */
  tiers: SessionLimitPredictionTierOption[];
  baselineData: SessionLimitPredictionBaselineData;
  recommendedTier: SessionLimitPredictionTier;
  /**
   * Recommended maximum AI credits for this session.
   */
  recommendedCap: number;
}
/**
 * Semantic usage tier and its AI-credit cap.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLimitPredictionTierOption".
 */
/** @experimental */
export interface SessionLimitPredictionTierOption {
  tier: SessionLimitPredictionTier;
  /**
   * AI-credit cap for this tier.
   */
  cap: number;
}
/**
 * Sessions matching the filter, ordered most-recently-modified first.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionList".
 */
/** @experimental */
export interface SessionList {
  /**
   * Sessions ordered most-recently-modified first. Discriminated by `isRemote`.
   */
  sessions: SessionListEntry[];
}
/**
 * Optional filter applied to the returned sessions
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionListFilter".
 */
/** @experimental */
export interface SessionListFilter {
  /**
   * Match sessions whose context.cwd equals this value
   */
  cwd?: string;
  /**
   * Match sessions whose context.gitRoot equals this value
   */
  gitRoot?: string;
  /**
   * Match sessions whose context.repository equals this value
   */
  repository?: string;
  /**
   * Match sessions whose context.branch equals this value
   */
  branch?: string;
}
/**
 * Queued repo-level startup prompts and the total hook command count after loading.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionLoadDeferredRepoHooksResult".
 */
/** @experimental */
export interface SessionLoadDeferredRepoHooksResult {
  /**
   * Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.
   */
  startupPrompts: string[];
  /**
   * Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.
   */
  hookCount: number;
}
/**
 * Enterprise permission policy expressed with the runtime's managed permission-rule syntax.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionManagedPermissions".
 */
/** @experimental */
export interface SessionManagedPermissions {
  /**
   * When set to `disable`, prevents bypass/allow-all permission modes. `allow-auto-only` blocks full allow-all but permits advisory auto-approval. Any other value is accepted rather than failing the session, but is enforced as `disable`: the key is only present to restrict something, so a mode this runtime cannot interpret fails closed to the most restrictive one it knows. Omit the key entirely to impose no restriction.
   */
  disableBypassPermissionsMode?: string;
  /**
   * Permission rules that block matching operations. Deny has highest precedence.
   */
  deny?: string[];
  /**
   * Permission rules that require explicit human approval.
   */
  ask?: string[];
  /**
   * Permission rules that allow matching operations unless another managed source, deny, or ask rule restricts them.
   */
  allow?: string[];
}
/**
 * Managed settings an SDK host may inject at session startup. Only permissions are accepted in this initial contract.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionManagedSettings".
 */
/** @experimental */
export interface SessionManagedSettings {
  permissions?: SessionManagedPermissions;
}
/**
 * Point-in-time snapshot of slow-changing session identifier and state fields
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionMetadataSnapshot".
 */
/** @experimental */
export interface SessionMetadataSnapshot {
  /**
   * The unique identifier of the session
   */
  sessionId: string;
  /**
   * ISO 8601 timestamp of when the session started
   */
  startTime: string;
  /**
   * ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.
   */
  modifiedTime: string;
  /**
   * Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process)
   */
  isRemote: boolean;
  /**
   * True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.
   */
  alreadyInUse: boolean;
  /**
   * Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace
   */
  workspacePath: string | null;
  /**
   * User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.
   */
  initialName?: string;
  /**
   * Runtime client name associated with the session (telemetry identifier).
   */
  clientName?: string;
  remoteMetadata?: MetadataSnapshotRemoteMetadata;
  /**
   * Short human-readable summary of the session, if known. Omitted when no summary has been generated.
   */
  summary?: string;
  /**
   * Absolute path to the session's current working directory
   */
  workingDirectory: string;
  currentMode: MetadataSnapshotCurrentMode;
  /**
   * Currently selected model identifier, if any
   */
  selectedModel?: string;
  /**
   * Current session limits, or null when no limits are active
   */
  sessionLimits: SessionLimitsConfig | null;
  /**
   * Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
   */
  workspace?: WorkspaceSummary | null;
}
/**
 * The list of models available to this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionModelList".
 */
/** @experimental */
export interface SessionModelList {
  /**
   * Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`).
   */
  list: JsonValue[];
  /**
   * Cost categories for the full CAPI catalog, including picker-disabled models that Auto may select. Metadata only; entries absent from `list` are not manually selectable.
   */
  modelPriceCategories?: SessionModelPriceCategory[];
  /**
   * Per-quota snapshots returned alongside the model list, keyed by quota type.
   */
  quotaSnapshots?: {
    [k: string]: JsonValue | undefined;
  };
}
/**
 * Cost-category metadata for a CAPI model.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionModelPriceCategory".
 */
/** @experimental */
export interface SessionModelPriceCategory {
  /**
   * CAPI model identifier.
   */
  id: string;
  priceCategory: ModelPickerPriceCategory;
}
/**
 * Session construction options.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptions".
 */
/** @experimental */
export interface SessionOpenOptions {
  /**
   * Optional stable session identifier to use for a new session.
   */
  sessionId?: string;
  /**
   * Optional human-friendly session name.
   */
  name?: string;
  /**
   * Initial model identifier.
   */
  model?: string;
  /**
   * Initial reasoning effort level. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied.
   */
  reasoningEffort?: string;
  reasoningSummary?: SessionOpenOptionsReasoningSummary;
  verbosity?: Verbosity;
  /**
   * Identifier of the client driving the session.
   */
  clientName?: string;
  /**
   * Structured client kind used for runtime behavior gates.
   */
  clientKind?: string;
  /**
   * Identifier sent to LSP-style integrations.
   */
  lspClientName?: string;
  /**
   * Stable integration identifier for analytics.
   */
  integrationId?: string;
  /**
   * ExP assignment ('flight') data injected by an SDK integrator, in the same JSON shape the Copilot CLI fetches from the experimentation service (CopilotExpAssignmentResponse). When supplied this is fed into the FeatureFlagService exactly like CLI-fetched assignments and ExP-backed flags wait for it. When absent the session does not block on ExP.
   *
   * @internal
   */
  expAssignments?: JsonValue;
  /**
   * Opt-in: self-fetch and enforce enterprise managed settings at session bootstrap.
   */
  enableManagedSettings?: boolean;
  managedSettings?: SessionManagedSettings;
  /**
   * Opt in to capturing file changes for session rewind and session diff. Capture cannot reconstruct changes made before it was enabled. On create it starts capture from the first turn. It is also honored on resume: for a session that already has tracked prior turns, tracking continues automatically even if this is omitted; passing it on resume additionally enables tracking for an eligible session that has no prior root turn yet. Resuming a session whose prior root turns were never tracked has no restorable baseline, so tracking stays disabled for it and rewind reports file change tracking as unavailable; the resume itself still succeeds, so sessions that predate tracking remain loadable. The opt-in is only rejected when the session can never track (a subagent session, or one without local session storage). It is intentionally absent from the mutable options update because enabling it after edits have occurred would create an incomplete, misleading baseline. Subagents share the parent session's capture store and are not tracked as separate rewind points: a file a subagent writes is attributed to whichever root user turn was open when the capture was staged, just before the tool body ran. A turn cannot open while a staged capture is still in flight, so a subagent tool that staged under the spawning turn stays attributed to it however late the write lands, while a capture it stages after the user's next message belongs to that later turn. Attribution decides which turn's rewind point counts and file preview include that write; it does not narrow which rewinds revert it, because a rewind restores every capture from the selected turn onward, so the earlier spawning turn reverts it as well.
   */
  enableFileChangeTracking?: boolean;
  /**
   * Feature-flag values resolved by the host.
   */
  featureFlags?: {
    [k: string]: boolean | undefined;
  };
  /**
   * Whether experimental behavior is enabled.
   */
  isExperimentalMode?: boolean;
  authInfo?: AuthInfo;
  provider?: ProviderConfig;
  capi?: CapiSessionOptions;
  /**
   * Named BYOK provider connections, additive to CAPI auth. Combining with `provider` is rejected.
   *
   * @experimental
   */
  providers?: NamedProviderConfig[];
  /**
   * BYOK model definitions added to the selectable model list, each referencing a provider name.
   *
   * @experimental
   */
  models?: ProviderModelConfig[];
  /**
   * Working directory to anchor the session.
   */
  workingDirectory?: string;
  /**
   * Additional directories the agent may access beyond the working directory. Each entry is granted to the session's file-access allow-list and surfaced to the model (system prompt context and `@`-mention completion). Conventional `.github/skills/` and `.github/agents/` definitions under each directory also join the session's project catalogs when their existing subsystem gates are enabled: added-root skills require both `enableConfigDiscovery` and effective `enableSkills`; added-root agents require `enableConfigDiscovery`. Supplying a directory therefore activates configuration from it and should be treated as a trust decision. Absolute paths are recommended; a relative path is resolved against the session's working directory. Nonexistent or unresolvable entries are skipped with a warning. This is applied during session creation and cold resume and is not persisted, so a cold resume must re-supply the directories.
   */
  additionalDirectories?: string[];
  workingDirectoryContext?: SessionContext;
  /**
   * Whether this session supports remote steering.
   */
  remoteSteerable?: boolean;
  /**
   * Telemetry-only remote exporting flag.
   */
  remoteExporting?: boolean;
  /**
   * Telemetry-only remote-defaulted flag.
   */
  remoteDefaultedOn?: boolean;
  /**
   * Parent session ID for detached child telemetry rollup.
   */
  detachedFromSpawningParentSessionId?: string;
  /**
   * Parent engagement ID for detached child telemetry rollup.
   */
  detachedFromSpawningParentEngagementId?: string;
  /**
   * Allowlist of available tool names.
   */
  availableTools?: string[];
  /**
   * Denylist of tool names.
   */
  excludedTools?: string[];
  /**
   * Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available.
   */
  includedBuiltinAgents?: string[];
  /**
   * Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available.
   */
  excludedBuiltinAgents?: string[];
  /**
   * Whether shell-script safety heuristics are enabled.
   */
  enableScriptSafety?: boolean;
  shell?: ShellOptions;
  /**
   * @deprecated
   * Use shell.initProfile instead. Shell init profile.
   */
  shellInitProfile?: string;
  /**
   * PowerShell process flags applied to built-in and user-requested shell commands.
   */
  shellProcessFlags?: string[];
  sandboxConfig?: SandboxConfig;
  /**
   * Origin of the sandbox choice. The runtime uses this only for internal telemetry provenance; managed policy is derived independently.
   *
   * @internal
   */
  sandboxConfigSource?: SandboxConfigSource;
  /**
   * Whether interactive shell sessions are logged.
   */
  logInteractiveShells?: boolean;
  envValueMode?: SessionOpenOptionsEnvValueMode;
  /**
   * MCP server names disabled for this session. Disabled servers are not started or authenticated on create or cold resume.
   */
  disabledMcpServers?: string[];
  /**
   * Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers.
   */
  allowAllMcpServerInstructions?: boolean;
  /**
   * Additional directories to search for skills.
   */
  skillDirectories?: string[];
  /**
   * Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available.
   */
  includedBuiltinSkills?: string[];
  /**
   * Skill IDs disabled for this session.
   */
  disabledSkills?: string[];
  /**
   * Installed plugins visible to the session.
   */
  installedPlugins?: InstalledPlugin[];
  /**
   * Whether custom agents default to local-only execution.
   */
  customAgentsLocalOnly?: boolean;
  /**
   * Whether to skip custom instruction sources.
   */
  skipCustomInstructions?: boolean;
  /**
   * Instruction source IDs disabled for this session.
   */
  disabledInstructionSources?: string[];
  /**
   * Whether commit-message coauthor trailers are enabled.
   */
  coauthorEnabled?: boolean;
  /**
   * Optional trajectory output file path.
   */
  trajectoryFile?: string;
  /**
   * Whether model responses stream as delta events.
   */
  enableStreaming?: boolean;
  /**
   * Experimental: enable native model citations for supported Anthropic and OpenAI models, normalized onto the `assistant.message` event. Off by default; may change or be removed while the citations surface is experimental.
   *
   * @experimental
   */
  enableCitations?: boolean;
  /**
   * Override URL for the Copilot API endpoint.
   */
  copilotUrl?: string;
  /**
   * Whether ask_user is explicitly disabled.
   */
  askUserDisabled?: boolean;
  /**
   * Whether auto-mode continuation is enabled.
   */
  continueOnAutoMode?: boolean;
  /**
   * Whether the host is an interactive UI.
   */
  runningInInteractiveMode?: boolean;
  /**
   * Whether on-demand custom instruction discovery is enabled.
   */
  enableOnDemandInstructionDiscovery?: boolean;
  /**
   * Maximum decoded byte size of a single inline model-facing binary tool result persisted in session events (default 10 MB).
   */
  maxInlineBinaryBytes?: number;
  modelCapabilitiesOverrides?: ModelCapabilitiesOverride;
  sessionLimits?: SessionLimitsConfig;
  /**
   * Runtime context discriminator for agent filtering.
   */
  agentContext?: string;
  /**
   * Override directory for session event logs.
   */
  eventsLogDirectory?: string;
  /**
   * Whether subagent callback events should be forwarded into the session event log sink.
   */
  eventsLogIncludesSubagents?: boolean;
  /**
   * Override Copilot configuration directory.
   */
  configDir?: string;
  /**
   * Additional content-exclusion policies to merge into the session policy set.
   *
   * @experimental
   */
  additionalContentExclusionPolicies?: SessionOpenOptionsAdditionalContentExclusionPolicy[];
  memory?: MemoryConfiguration;
  /**
   * Capabilities enabled for this session.
   */
  sessionCapabilities?: SessionCapability[];
}
/**
 * Per-session settings for built-in shell tools.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellOptions".
 */
/** @experimental */
export interface ShellOptions {
  initProfile?: ShellInitProfile;
  /**
   * Ordered host-provided script paths sourced before each built-in shell command when the
   * entry's shell target matches the active shell. Use these for rc files, environment setup scripts,
   * or other custom scripts. A script that returns a nonzero status is reported, and later scripts
   * and the user command continue while the shell remains running. Because scripts are sourced into
   * the command shell, `exit`, `exec`, failures under `set -e`, or other shell-terminating behavior
   * can prevent continuation. Script standard output is preserved; Bash script stderr is discarded,
   * PowerShell exception messages are replaced, and runtime-generated failure notices omit
   * configured script paths. When sandboxing is enabled, each script must already be readable under
   * the active sandbox filesystem policy. Pass an empty array to clear the list.
   */
  initScripts?: ShellInitScript[];
  /**
   * Flags passed to the active built-in shell process on startup, replacing its default flags.
   * When omitted, the built-in Bash shell uses `--norc --noprofile`,
   * and the built-in PowerShell shell uses `-NoProfile -NoLogo`.
   */
  processFlags?: string[];
  credentials?: ShellCredentials;
}
/**
 * A host-provided script sourced before each built-in shell command when its shell target matches the active shell.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellInitScript".
 */
/** @experimental */
export interface ShellInitScript {
  /**
   * Path to the script to source.
   */
  path: string;
  shell: ShellInitScriptShell;
}
/**
 * Command-scoped GitHub credential injection for the shell commands an agent runs.
 *
 * Each channel is opt-in and independent, and injection is scoped to the individual command
 * spawn: the credential is resolved from the session's *current* authentication at every spawn
 * and reaches only spawns whose script actually invokes `git` or `gh`. Because nothing is
 * retained between spawns, replacing the session credential (`session.gitHubAuth.setCredentials`)
 * changes what the next spawned command presents — which seeding a credential into the runtime
 * process's own environment cannot do, since a child's environment is fixed at `exec`.
 *
 * The credential is matched to the host it authenticates to, so a github.com credential is never
 * presented to a GitHub Enterprise host and vice versa. Where a channel cannot express that
 * boundary it injects nothing rather than crossing it -- see `gh` below.
 *
 * This is independent of `sandboxConfig`: it is a decision about which identity the agent
 * presents, not about what the agent may touch, and it works on every platform whether or not
 * an OS sandboxing backend is available. `sandboxConfig.auth` remains the sandbox-scoped
 * spelling and is additive with this one.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellCredentials".
 */
/** @experimental */
export interface ShellCredentials {
  /**
   * Whether to authenticate the agent's `git` commands as the session's GitHub credential, by
   * injecting an `http.<host>.extraheader` (plus `insteadOf` rewrites so SSH-spelled remotes for
   * that host use the authenticated HTTPS transport). Applied only to a spawn that runs a
   * remote-contacting `git` subcommand. Default: false (opt-in).
   */
  git?: boolean;
  /**
   * Whether to authenticate the agent's `gh` commands as the session's GitHub credential, by
   * exporting `GH_TOKEN` to a spawn that runs `gh`. Any inherited `gh` credential is removed from
   * spawns that do not, so the credential stays command-scoped.
   *
   * Applies to a github.com credential only. `gh` picks its credential variable from the host a
   * command targets rather than the one the credential belongs to, and the command can choose that
   * target, so `GH_ENTERPRISE_TOKEN` would offer a single-tenant enterprise credential to every
   * other enterprise host. A session whose credential is enterprise-scoped therefore runs `gh`
   * unauthenticated; its `git` commands are unaffected, because `http.<host>.extraheader` is scoped
   * to one host by construction. Default: false (opt-in).
   */
  gh?: boolean;
}
/**
 * Content-exclusion policy supplied to `sessions.open` options, with rules, last-updated data, and scope.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptionsAdditionalContentExclusionPolicy".
 */
/** @experimental */
export interface SessionOpenOptionsAdditionalContentExclusionPolicy {
  /**
   * Content-exclusion rules to apply.
   */
  rules: SessionOpenOptionsAdditionalContentExclusionPolicyRule[];
  /**
   * Opaque policy update timestamp supplied by the host.
   */
  last_updated_at: JsonValue;
  scope: SessionOpenOptionsAdditionalContentExclusionPolicyScope;
}
/**
 * Single content-exclusion rule supplied to `sessions.open` options, with paths, match conditions, and source.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptionsAdditionalContentExclusionPolicyRule".
 */
/** @experimental */
export interface SessionOpenOptionsAdditionalContentExclusionPolicyRule {
  /**
   * Path patterns covered by this rule.
   */
  paths: string[];
  /**
   * Conditions of which at least one must match.
   */
  ifAnyMatch?: string[];
  /**
   * Conditions none of which may match.
   */
  ifNoneMatch?: string[];
  source: SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource;
}
/**
 * Source descriptor for a `sessions.open` content-exclusion rule, with source name and type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource".
 */
/** @experimental */
export interface SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource {
  /**
   * Name of the policy source.
   */
  name: string;
  /**
   * Type of the policy source.
   */
  type: string;
}
/**
 * Parameters for creating a new local session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenCreate".
 */
/** @experimental */
export interface SessionsOpenCreate {
  /**
   * Create a new local session.
   */
  kind: "create";
  options?: SessionOpenOptions;
  /**
   * Whether to emit session.start during creation. Defaults to true.
   */
  emitStart?: boolean;
}
/**
 * Parameters for resuming a specific local session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenResume".
 */
/** @experimental */
export interface SessionsOpenResume {
  /**
   * Resume a specific local session by ID or prefix.
   */
  kind: "resume";
  /**
   * Session ID or unique prefix to resume.
   */
  sessionId: string;
  options?: SessionOpenOptions;
  /**
   * Whether to emit session.resume after loading. Defaults to true.
   */
  resume?: boolean;
  /**
   * Suppress workspace.yaml metadata writeback when resuming from an incidental cwd.
   */
  suppressResumeWorkspaceMetadataWriteback?: boolean;
}
/**
 * Parameters for resuming the most relevant local session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenResumeLast".
 */
/** @experimental */
export interface SessionsOpenResumeLast {
  /**
   * Resume the most relevant existing local session.
   */
  kind: "resumeLast";
  context?: SessionContext;
  options?: SessionOpenOptions;
  /**
   * Suppress workspace.yaml metadata writeback when resuming from an incidental cwd.
   */
  suppressResumeWorkspaceMetadataWriteback?: boolean;
}
/**
 * Parameters for attaching to an already-active session by ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenAttach".
 */
/** @experimental */
export interface SessionsOpenAttach {
  /**
   * Attach to an already-active in-process session by ID. Unlike `resume`, this does NOT re-load from disk; the session must already be loaded by an earlier `create`/`resume` call. Returns `status: 'not_found'` when no active session matches the id. Useful for in-process consumers that need a fresh API handle to a session opened elsewhere (e.g., a peer foreground-session switch).
   */
  kind: "attach";
  /**
   * Session ID to attach to.
   */
  sessionId: string;
}
/**
 * Parameters for connecting to a live remote session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenRemote".
 */
/** @experimental */
export interface SessionsOpenRemote {
  /**
   * Connect to a live remote session.
   */
  kind: "remote";
  /**
   * Remote session identifier to connect to.
   */
  remoteSessionId: string;
  repository?: RemoteSessionRepository;
  options?: SessionOpenOptions;
}
/**
 * Parameters for creating a new cloud session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenCloud".
 */
/** @experimental */
export interface SessionsOpenCloud {
  /**
   * Create a new cloud (coding-agent) session.
   */
  kind: "cloud";
  repository?: RemoteSessionRepository;
  /**
   * Optional owner (user or organization login) to associate with the cloud session when no repository is provided. Ignored when `repository` is set (the repo's owner takes precedence).
   */
  owner?: string;
  options?: SessionOpenOptions;
  /**
   * In-process callback invoked when the cloud task is created, before connection. Internal because function references cannot cross the JSON-RPC boundary.
   *
   * @internal
   */
  onTaskCreated?: OpaqueInProcessValue;
}
/**
 * Parameters for fetching a remote session and handing it off to a new local session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenHandoff".
 */
/** @experimental */
export interface SessionsOpenHandoff {
  /**
   * Fetch a remote session and hand it off to a new local session.
   */
  kind: "handoff";
  metadata: RemoteSessionMetadataValue;
  options?: SessionOpenOptions;
  taskType?: SessionsOpenHandoffTaskType;
  /**
   * In-process progress callback `(update) => void` invoked for each handoff step. Marked internal because a function reference cannot cross the JSON-RPC boundary. The host-side `handoffSession` is already declared as `AsyncGenerator<HandoffProgress, HandoffResult>`; the schema layer flattens it because it does not yet support streaming methods. The wire-clean replacement is to expose the AsyncGenerator directly (or use vscode-jsonrpc `$/progress` notifications) once the schema/transport layer supports it.
   *
   * @internal
   */
  onProgress?: OpaqueInProcessValue;
  /**
   * In-process confirmation callback `(request) => boolean | Promise<boolean>` invoked when the handoff needs the caller to confirm a non-fatal blocker (e.g. a repository mismatch between the current working directory and the remote session). Returning `true` proceeds with the handoff; returning `false` (or omitting the callback) aborts it. Marked internal because a function reference cannot cross the JSON-RPC boundary, for the same reasons as `onProgress`.
   *
   * @internal
   */
  onConfirm?: OpaqueInProcessValue;
}
/**
 * Result of opening a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionOpenResult".
 */
/** @experimental */
export interface SessionOpenResult {
  status: SessionsOpenStatus;
  /**
   * Opened session ID. Omitted when status is `not_found`.
   */
  sessionId?: string;
  /**
   * In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead.
   *
   * @internal
   *
   * @internal
   */
  sessionApi?: OpaqueInProcessValue;
  /**
   * Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array.
   */
  startupPrompts?: string[];
  /**
   * Remote session ID, present when status is `connected`.
   */
  remoteSessionId?: string;
  metadata?: RemoteSessionMetadataValue;
  /**
   * Handoff progress steps, present when status is `handed_off`.
   */
  progress?: SessionsOpenProgress[];
}
/**
 * `sessions.open` handoff progress update with step, status, and optional message.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsOpenProgress".
 */
/** @experimental */
export interface SessionsOpenProgress {
  step: SessionsOpenProgressStep;
  status: SessionsOpenProgressStatus;
  /**
   * Optional step message.
   */
  message?: string;
}
/**
 * Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionPruneResult".
 */
/** @experimental */
export interface SessionPruneResult {
  /**
   * Session IDs that were deleted (always empty in dry-run mode)
   */
  deleted: string[];
  /**
   * Session IDs that would be deleted in dry-run mode (always empty otherwise)
   */
  candidates: string[];
  /**
   * Session IDs that were skipped (e.g., named sessions)
   */
  skipped: string[];
  /**
   * Total bytes freed (actual when not dry-run, projected when dry-run)
   */
  freedBytes: number;
  /**
   * True when no deletions were actually performed
   */
  dryRun: boolean;
}
/**
 * Session IDs to close, deactivate, and delete from disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsBulkDeleteRequest".
 */
/** @experimental */
export interface SessionsBulkDeleteRequest {
  /**
   * Session IDs to close, deactivate, and delete from disk
   */
  sessionIds: string[];
}
/**
 * Session IDs to test for live in-use locks.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCheckInUseRequest".
 */
/** @experimental */
export interface SessionsCheckInUseRequest {
  /**
   * Session IDs to test for live in-use locks
   */
  sessionIds: string[];
}
/**
 * Session IDs from the input set that are currently in use by another process.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCheckInUseResult".
 */
/** @experimental */
export interface SessionsCheckInUseResult {
  /**
   * Session IDs from the input set that are currently held by another running process via an alive lock file
   */
  inUse: string[];
}
/**
 * Session ID to close.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCloseRequest".
 */
/** @experimental */
export interface SessionsCloseRequest {
  /**
   * Session ID to close
   */
  sessionId: string;
}
/**
 * Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsCloseResult".
 */
/** @experimental */
export interface SessionsCloseResult {}
/**
 * Session ID to delete from disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsDeleteRequest".
 */
/** @experimental */
export interface SessionsDeleteRequest {
  /**
   * Session ID to delete
   */
  sessionId: string;
  /**
   * Internal resolved session directory path to delete
   */
  sessionPath?: string | null;
}
/**
 * Session metadata records to enrich with summary and context information.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsEnrichMetadataRequest".
 */
/** @experimental */
export interface SessionsEnrichMetadataRequest {
  /**
   * Session metadata records to enrich. Records that already have summary and context are returned unchanged.
   */
  sessions: LocalSessionMetadataValue[];
}
/**
 * New auth credentials to install on the session. Omit to leave credentials unchanged.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSetCredentialsParams".
 */
/** @experimental */
export interface SessionSetCredentialsParams {
  credentials?: SettableAuthInfo;
}
/**
 * Token authentication accepted by session.gitHubAuth.setCredentials.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SettableTokenAuthInfo".
 */
/** @experimental */
export interface SettableTokenAuthInfo {
  /**
   * SDK-side token authentication; the host configured the token directly via the SDK.
   */
  type: "token";
  /**
   * Authentication host.
   */
  host: string;
  /**
   * The token value itself. Treat as a secret.
   */
  token: string;
  copilotUser?: CopilotUserResponse;
}
/**
 * Indicates whether the credential update succeeded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSetCredentialsResult".
 */
/** @experimental */
export interface SessionSetCredentialsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
  /**
   * Whether the session ended up with a populated `copilotUser` for the installed credentials. `true` when the supplied credential already carried `copilotUser` or it was successfully re-resolved server-side. `false` when the credential is installed without `copilotUser` — either re-resolution failed, or the variant cannot be re-resolved from the credential alone (only the raw-token variants `token`, `env`, and `gh-cli` can). In both `false` cases the token swap still applied, but plan/quota/billing metadata is degraded. Present whenever a credential was supplied; omitted only when no credential was supplied (no-op call).
   */
  copilotUserResolved?: boolean;
}
/**
 * Availability of built-in job tools surfaced to boundary consumers.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsBuiltInToolAvailabilitySnapshot".
 */
/** @experimental */
export interface SessionSettingsBuiltInToolAvailabilitySnapshot {
  /**
   * Whether the report-progress tool is available.
   */
  reportProgress?: boolean;
  /**
   * Whether the create-pull-request tool is available.
   */
  createPullRequest?: boolean;
}
/**
 * Named Rust-owned settings predicate to evaluate for this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsEvaluatePredicateRequest".
 */
/** @experimental */
export interface SessionSettingsEvaluatePredicateRequest {
  name: SessionSettingsPredicateName;
  /**
   * Tool name for tool-scoped predicates such as trivial-change handling.
   */
  toolName?: string;
}
/**
 * Result of evaluating a Rust-owned settings predicate.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsEvaluatePredicateResult".
 */
/** @experimental */
export interface SessionSettingsEvaluatePredicateResult {
  /**
   * Whether the named settings predicate evaluated to enabled.
   */
  enabled: boolean;
}
/**
 * Redacted job settings for a session. The job nonce is excluded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsJobSnapshot".
 */
/** @experimental */
export interface SessionSettingsJobSnapshot {
  /**
   * GitHub Actions event type for the job.
   */
  eventType?: string;
  /**
   * Whether this is the workflow's trigger job.
   */
  isTriggerJob?: boolean;
  builtInToolAvailability?: SessionSettingsBuiltInToolAvailabilitySnapshot;
}
/**
 * Redacted model routing settings for a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsModelSnapshot".
 */
/** @experimental */
export interface SessionSettingsModelSnapshot {
  /**
   * Selected model identifier.
   */
  model?: string;
  /**
   * Default reasoning effort for the selected model.
   */
  defaultReasoningEffort?: string;
  /**
   * Agent job identifier for the session.
   */
  instanceId?: string;
  /**
   * Agent service callback URL for job and progress updates.
   */
  callbackUrl?: string;
}
/**
 * Online-evaluation settings safe to expose across the SDK boundary.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsOnlineEvaluationSnapshot".
 */
/** @experimental */
export interface SessionSettingsOnlineEvaluationSnapshot {
  /**
   * Whether online evaluation is disabled.
   */
  disableOnlineEvaluation?: boolean;
  /**
   * Whether online-evaluation output-file generation is enabled.
   */
  enableOnlineEvaluationOutputFile?: boolean;
}
/**
 * Redacted repository and GitHub host settings for a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsRepoSnapshot".
 */
/** @experimental */
export interface SessionSettingsRepoSnapshot {
  /**
   * Repository name.
   */
  name?: string;
  /**
   * GitHub repository database ID.
   */
  id?: number;
  /**
   * Checked-out repository branch.
   */
  branch?: string;
  /**
   * Checked-out commit SHA.
   */
  commit?: string;
  /**
   * Whether the repository is writable.
   */
  readWrite?: boolean;
  /**
   * Repository owner login.
   */
  ownerName?: string;
  /**
   * GitHub repository owner database ID.
   */
  ownerId?: number;
  /**
   * GitHub server base URL.
   */
  serverUrl?: string;
  /**
   * GitHub server host name.
   */
  host?: string;
  /**
   * Protocol used to access the GitHub host.
   */
  hostProtocol?: string;
  /**
   * GitHub secret-scanning service URL.
   */
  secretScanningUrl?: string;
  /**
   * Number of commits in the pull request.
   */
  prCommitCount?: number;
}
/**
 * Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsSnapshot".
 */
/** @experimental */
export interface SessionSettingsSnapshot {
  /**
   * Agent runtime version selector copied from the session settings, such as `latest` or a runtime release identifier.
   */
  version?: string;
  /**
   * Name of the SDK client that created the session.
   */
  clientName?: string;
  /**
   * Session timeout in milliseconds.
   */
  timeoutMs?: number;
  /**
   * Session start time as Unix epoch milliseconds.
   */
  startTimeMs?: number;
  repo: SessionSettingsRepoSnapshot;
  model: SessionSettingsModelSnapshot;
  validation: SessionSettingsValidationSnapshot;
  job: SessionSettingsJobSnapshot;
  onlineEvaluation: SessionSettingsOnlineEvaluationSnapshot;
}
/**
 * Redacted validation and memory-tool settings for a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSettingsValidationSnapshot".
 */
/** @experimental */
export interface SessionSettingsValidationSnapshot {
  /**
   * General validation timeout budget in seconds.
   */
  timeout?: number;
  /**
   * Dependabot validation timeout budget in seconds.
   */
  dependabotTimeout?: number;
  /**
   * Whether CodeQL validation is enabled.
   */
  codeqlEnabled?: boolean;
  /**
   * Whether code-review validation is enabled.
   */
  codeReviewEnabled?: boolean;
  /**
   * Model used for code-review validation.
   */
  codeReviewModel?: string;
  /**
   * Whether advisory validation is enabled.
   */
  advisoryEnabled?: boolean;
  /**
   * Whether secret-scanning validation is enabled.
   */
  secretScanningEnabled?: boolean;
  /**
   * Whether the memory-store tool is enabled.
   */
  memoryStoreEnabled?: boolean;
  /**
   * Whether the memory-vote tool is enabled.
   */
  memoryVoteEnabled?: boolean;
}
/**
 * UUID prefix to resolve to a unique session ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByPrefixRequest".
 */
/** @experimental */
export interface SessionsFindByPrefixRequest {
  /**
   * UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.
   */
  prefix: string;
}
/**
 * Session ID matching the prefix, omitted when no unique match exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByPrefixResult".
 */
/** @experimental */
export interface SessionsFindByPrefixResult {
  /**
   * Omitted when no unique session matches the prefix (no match or ambiguous)
   */
  sessionId?: string;
}
/**
 * GitHub task ID to look up.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByTaskIDRequest".
 */
/** @experimental */
export interface SessionsFindByTaskIDRequest {
  /**
   * GitHub task ID to look up
   */
  taskId: string;
}
/**
 * ID of the local session bound to the given GitHub task, or omitted when none.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsFindByTaskIDResult".
 */
/** @experimental */
export interface SessionsFindByTaskIDResult {
  /**
   * Omitted when no local session is bound to that GitHub task
   */
  sessionId?: string;
}
/**
 * Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsForkRequest".
 */
/** @experimental */
export interface SessionsForkRequest {
  /**
   * Source session ID to fork from
   */
  sessionId: string;
  /**
   * Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
   */
  toEventId?: string;
  /**
   * Optional friendly name to assign to the forked session.
   */
  name?: string;
}
/**
 * Identifier and optional friendly name assigned to the newly forked session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsForkResult".
 */
/** @experimental */
export interface SessionsForkResult {
  /**
   * The new forked session's ID
   */
  sessionId: string;
  /**
   * Friendly name assigned to the forked session, if any.
   */
  name?: string;
}
/**
 * Session ID whose board entry count should be returned.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetBoardEntryCountRequest".
 */
/** @experimental */
export interface SessionsGetBoardEntryCountRequest {
  /**
   * Session ID whose board entry count should be returned.
   */
  sessionId: string;
}
/**
 * Dynamic-context board entry count, when available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetBoardEntryCountResult".
 */
/** @experimental */
export interface SessionsGetBoardEntryCountResult {
  /**
   * Board entry count, when available.
   */
  count?: number;
}
/**
 * Session ID whose event-log file path to compute.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetEventFilePathRequest".
 */
/** @experimental */
export interface SessionsGetEventFilePathRequest {
  /**
   * Session ID whose event-log file path to compute
   */
  sessionId: string;
}
/**
 * Absolute path to the session's events.jsonl file on disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetEventFilePathResult".
 */
/** @experimental */
export interface SessionsGetEventFilePathResult {
  /**
   * Absolute path to the session's events.jsonl file
   */
  filePath: string;
}
/**
 * Optional working-directory context used to score session relevance.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetLastForContextRequest".
 */
/** @experimental */
export interface SessionsGetLastForContextRequest {
  context?: SessionContext;
}
/**
 * Most-relevant session ID for the supplied context, or omitted when no sessions exist.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetLastForContextResult".
 */
/** @experimental */
export interface SessionsGetLastForContextResult {
  /**
   * Most-relevant session ID for the supplied context, or omitted when no sessions exist
   */
  sessionId?: string;
}
/**
 * Session ID whose persisted metadata should be read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetMetadataRequest".
 */
/** @experimental */
export interface SessionsGetMetadataRequest {
  /**
   * Session ID to inspect
   */
  sessionId: string;
}
/**
 * Persisted local session metadata when the session exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetMetadataResult".
 */
/** @experimental */
export interface SessionsGetMetadataResult {
  session?: LocalSessionMetadataValue;
}
/**
 * Session ID to look up the persisted remote-steerable flag for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetPersistedRemoteSteerableRequest".
 */
/** @experimental */
export interface SessionsGetPersistedRemoteSteerableRequest {
  /**
   * Session ID to look up the persisted remote-steerable flag for
   */
  sessionId: string;
}
/**
 * The session's persisted remote-steerable flag, or omitted when no value has been persisted.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsGetPersistedRemoteSteerableResult".
 */
/** @experimental */
export interface SessionsGetPersistedRemoteSteerableResult {
  /**
   * The session's persisted remote-steerable flag if recorded; omitted when no value has been persisted
   */
  remoteSteerable?: boolean;
}
/**
 * Map of sessionId -> on-disk size in bytes for each session's workspace directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionSizes".
 */
/** @experimental */
export interface SessionSizes {
  /**
   * Map of sessionId -> on-disk size in bytes for the session's workspace directory
   */
  sizes: {
    [k: string]: number | undefined;
  };
}
/**
 * Limit for non-empty local session IDs.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsListNonEmptySessionIdsRequest".
 */
/** @experimental */
export interface SessionsListNonEmptySessionIdsRequest {
  /**
   * Maximum number of session IDs to return.
   */
  limit?: number;
}
/**
 * Recent local session IDs that contain user-visible history.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsListNonEmptySessionIdsResult".
 */
/** @experimental */
export interface SessionsListNonEmptySessionIdsResult {
  /**
   * Session IDs ordered newest-first.
   */
  sessionIds: string[];
}
/**
 * Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsListRequest".
 */
/** @experimental */
export interface SessionsListRequest {
  source?: SessionSource;
  /**
   * When provided, only the first N local sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every local session. Has no effect on remote entries (which always carry their full shape).
   */
  metadataLimit?: number;
  filter?: SessionListFilter;
  /**
   * When true, include detached maintenance sessions. Defaults to false for user-facing session lists.
   */
  includeDetached?: boolean;
  /**
   * Only meaningful when `source` includes remote. When true, propagates errors from the remote service instead of silently returning an empty remote list. Defaults to false.
   */
  throwOnError?: boolean;
}
/**
 * Active session ID whose deferred repo-level hooks should be loaded.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsLoadDeferredRepoHooksRequest".
 */
/** @experimental */
export interface SessionsLoadDeferredRepoHooksRequest {
  /**
   * Active session ID whose deferred repo-level hooks should be loaded
   */
  sessionId: string;
}
/**
 * Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsPruneOldRequest".
 */
/** @experimental */
export interface SessionsPruneOldRequest {
  /**
   * Delete sessions whose modifiedTime is at least this many days old
   */
  olderThanDays: number;
  /**
   * When true, only report what would be deleted without performing any deletion
   */
  dryRun?: boolean;
  /**
   * When true, named sessions (set via /rename) are also eligible for pruning
   */
  includeNamed?: boolean;
  /**
   * Session IDs that should never be considered for pruning
   */
  excludeSessionIds?: string[];
}
/**
 * Pagination options for reading an inactive or active local session's persisted event journal.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReadPersistedEventsRequest".
 */
/** @experimental */
export interface SessionsReadPersistedEventsRequest {
  /**
   * Session ID whose persisted event journal should be read.
   */
  sessionId: string;
  /**
   * Opaque cursor returned by a previous persisted-event read. Omit on the first call.
   */
  cursor?: string;
  /**
   * Maximum number of events to return in this batch (1–1000, default 200).
   */
  max?: number;
  direction?: EventsReadDirection;
}
/**
 * Session ID whose in-use lock should be released.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReleaseLockRequest".
 */
/** @experimental */
export interface SessionsReleaseLockRequest {
  /**
   * Session ID whose in-use lock should be released
   */
  sessionId: string;
}
/**
 * Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReleaseLockResult".
 */
/** @experimental */
export interface SessionsReleaseLockResult {}
/**
 * Active session ID and an optional flag for deferring repo-level hooks until folder trust.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReloadPluginHooksRequest".
 */
/** @experimental */
export interface SessionsReloadPluginHooksRequest {
  /**
   * Active session ID to reload hooks for
   */
  sessionId: string;
  /**
   * When true, skip repo-level hooks. Use before folder trust is confirmed; loadDeferredRepoHooks loads them post-trust.
   */
  deferRepoHooks?: boolean;
}
/**
 * Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsReloadPluginHooksResult".
 */
/** @experimental */
export interface SessionsReloadPluginHooksResult {}
/**
 * Session ID whose pending events should be flushed to disk.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSaveRequest".
 */
/** @experimental */
export interface SessionsSaveRequest {
  /**
   * Session ID whose pending events should be flushed to disk
   */
  sessionId: string;
}
/**
 * Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSaveResult".
 */
/** @experimental */
export interface SessionsSaveResult {}
/**
 * Manager-wide additional plugins to register; replaces any previously-configured set.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSetAdditionalPluginsRequest".
 */
/** @experimental */
export interface SessionsSetAdditionalPluginsRequest {
  /**
   * Manager-wide additional plugins to register. Replaces any previously-configured set. Pass an empty array to clear.
   */
  plugins: InstalledPlugin[];
}
/**
 * Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSetAdditionalPluginsResult".
 */
/** @experimental */
export interface SessionsSetAdditionalPluginsResult {}
/**
 * Patch for the singleton's steering state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsSetRemoteControlSteeringRequest".
 */
/** @experimental */
export interface SessionsSetRemoteControlSteeringRequest {
  /**
   * Target steering state. Today only `true` is actionable on the underlying exporter; `false` is reserved for future use.
   */
  enabled: boolean;
}
/**
 * Parameters for attaching the remote-control singleton to a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsStartRemoteControlRequest".
 */
/** @experimental */
export interface SessionsStartRemoteControlRequest {
  /**
   * Local session id to attach remote control to.
   */
  sessionId: string;
  config: RemoteControlConfig;
}

/** @experimental */
export interface SessionsStopRemoteControlRequest {
  /**
   * When provided, the stop is rejected unless the singleton currently points at this session id (compare-and-swap semantics).
   */
  expectedSessionId?: string;
  /**
   * When true, the singleton is unconditionally torn down regardless of `expectedSessionId`. Use during shutdown or explicit `/remote off`.
   */
  force?: boolean;
}
/**
 * Parameters for atomically rebinding the remote-control singleton.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionsTransferRemoteControlRequest".
 */
/** @experimental */
export interface SessionsTransferRemoteControlRequest {
  /**
   * Local session id to point remote control at.
   */
  toSessionId: string;
  /**
   * When provided, the transfer is rejected unless the singleton currently points at this session id (compare-and-swap semantics to avoid clobbering newer state).
   */
  expectedFromSessionId?: string;
}
/**
 * Telemetry engagement ID for the session, when available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionTelemetryEngagement".
 */
/** @experimental */
export interface SessionTelemetryEngagement {
  /**
   * Current telemetry engagement ID, when available.
   */
  engagementId?: string;
}
/**
 * Patch of mutable session options to apply to the running session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionUpdateOptionsParams".
 */
/** @experimental */
export interface SessionUpdateOptionsParams {
  /**
   * The model ID to use for assistant turns.
   */
  model?: string;
  modelCapabilitiesOverrides?: ModelCapabilitiesOverride;
  /**
   * Reasoning effort for the selected model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied.
   */
  reasoningEffort?: string;
  reasoningSummary?: OptionsUpdateReasoningSummary;
  verbosity?: Verbosity;
  /**
   * Identifier of the client driving the session.
   */
  clientName?: string;
  /**
   * Identifier sent to LSP-style integrations.
   */
  lspClientName?: string;
  /**
   * Stable integration identifier used for analytics and rate-limit attribution.
   */
  integrationId?: string;
  /**
   * Map of feature-flag IDs to their boolean enabled state.
   */
  featureFlags?: {
    [k: string]: boolean | undefined;
  };
  /**
   * Whether experimental capabilities are enabled.
   */
  isExperimentalMode?: boolean;
  provider?: ProviderConfig;
  capi?: CapiSessionOptions;
  /**
   * Absolute working-directory path for shell tools.
   */
  workingDirectory?: string;
  /**
   * Allowlist of tool names available to this session.
   */
  availableTools?: string[];
  /**
   * Denylist of tool names for this session.
   */
  excludedTools?: string[];
  /**
   * Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available. Set to null to remove the allowlist restriction.
   */
  includedBuiltinAgents?: string[] | null;
  /**
   * Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available.
   */
  excludedBuiltinAgents?: string[];
  toolFilterPrecedence?: OptionsUpdateToolFilterPrecedence;
  /**
   * Whether shell-script safety heuristics are enabled.
   */
  enableScriptSafety?: boolean;
  shell?: ShellOptions;
  /**
   * @deprecated
   * Use shell.initProfile instead. Shell init profile (`None` or `NonInteractive`).
   */
  shellInitProfile?: string;
  /**
   * PowerShell process flags applied to built-in and user-requested shell commands.
   */
  shellProcessFlags?: string[];
  sandboxConfig?: SandboxConfig;
  /**
   * Origin of the sandbox choice. The runtime uses this only for internal telemetry provenance; managed policy is derived independently.
   *
   * @internal
   */
  sandboxConfigSource?: SandboxConfigSource;
  /**
   * Whether interactive shell sessions are logged.
   */
  logInteractiveShells?: boolean;
  envValueMode?: OptionsUpdateEnvValueMode;
  /**
   * Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers.
   */
  allowAllMcpServerInstructions?: boolean;
  /**
   * Additional directories to search for skills.
   */
  skillDirectories?: string[];
  /**
   * Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available. Set to null to remove the allowlist restriction.
   */
  includedBuiltinSkills?: string[] | null;
  /**
   * Skill IDs that should be excluded from this session.
   */
  disabledSkills?: string[];
  /**
   * Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions`.
   */
  enableOnDemandInstructionDiscovery?: boolean;
  /**
   * Maximum decoded byte size of a single model-facing binary tool result (e.g. an image) persisted inline in session events and re-presented to the model on later turns / resume. Larger results are persisted as a metadata-only marker and shown to the model as a short text note. Defaults to 10 MB.
   */
  maxInlineBinaryBytes?: number;
  /**
   * Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes.
   */
  installedPlugins?: SessionInstalledPlugin[];
  /**
   * Whether to default custom agents to local-only execution.
   */
  customAgentsLocalOnly?: boolean;
  /**
   * When true, the selected custom agent's prompt is not injected into the user message (skill context is still injected). Used by automation triggers where the agent prompt is already in the problem statement.
   */
  suppressCustomAgentPrompt?: boolean;
  /**
   * Whether to skip loading custom instruction sources.
   */
  skipCustomInstructions?: boolean;
  /**
   * Instruction source IDs to exclude from the system prompt.
   */
  disabledInstructionSources?: string[];
  /**
   * Whether to include the `Co-authored-by` trailer in commit messages.
   */
  coauthorEnabled?: boolean;
  /**
   * Optional path for trajectory output.
   */
  trajectoryFile?: string;
  /**
   * Whether to stream model responses.
   */
  enableStreaming?: boolean;
  /**
   * Override URL for the Copilot API endpoint.
   */
  copilotUrl?: string;
  /**
   * Whether to disable the `ask_user` tool (encourages autonomous behavior).
   */
  askUserDisabled?: boolean;
  /**
   * Whether to allow auto-mode continuation across turns.
   */
  continueOnAutoMode?: boolean;
  /**
   * Whether the session is running in an interactive UI.
   */
  runningInInteractiveMode?: boolean;
  /**
   * Whether to surface reasoning-summary events from the model.
   */
  enableReasoningSummaries?: boolean;
  /**
   * Runtime context discriminator (e.g., `cli`, `actions`).
   */
  agentContext?: string;
  /**
   * Override directory for the session-events log. When unset, the runtime's default events log directory is used.
   */
  eventsLogDirectory?: string;
  /**
   * Whether subagent callback events should be forwarded into the session event log sink.
   */
  eventsLogIncludesSubagents?: boolean;
  /**
   * Additional content-exclusion policies to merge into the session's policy set.
   *
   * @experimental
   */
  additionalContentExclusionPolicies?: OptionsUpdateAdditionalContentExclusionPolicy[];
  /**
   * Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users).
   */
  manageScheduleEnabled?: boolean;
  /**
   * Replaces the session's capability set with the given list. Use to enable or disable capabilities mid-session (e.g., remove `memory` for reproducible scripted runs). Omit the field to leave the existing capability set unchanged.
   */
  sessionCapabilities?: SessionCapability[];
  /**
   * Whether to skip embedding retrieval pipeline initialization and execution.
   */
  skipEmbeddingRetrieval?: boolean;
  /**
   * Organization-level custom instructions to inject into the system prompt.
   */
  organizationCustomInstructions?: string;
  /**
   * Whether to enable loading of `.github/hooks/` filesystem hooks. Separate from the SDK callback hook mechanism.
   */
  enableFileHooks?: boolean;
  /**
   * Whether to enable host git operations (context resolution, child repo scanning, git info in system prompt).
   */
  enableHostGitOperations?: boolean;
  /**
   * Whether to enable cross-session store writes and reads.
   */
  enableSessionStore?: boolean;
  /**
   * Whether to enable skill directory scanning and loading. Falls back to enableConfigDiscovery when unset.
   */
  enableSkills?: boolean;
  contextTier?: OptionsUpdateContextTier;
  /**
   * Optional session limits. Pass null to clear the session limits.
   */
  sessionLimits?: SessionLimitsConfig | null;
}
/**
 * Indicates whether the session options patch was applied successfully.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionUpdateOptionsResult".
 */
/** @experimental */
export interface SessionUpdateOptionsResult {
  /**
   * Whether the operation succeeded
   */
  success: boolean;
  /**
   * Number of hooks loaded from installed plugins, returned when installedPlugins is updated
   */
  pluginHookCount?: number;
}
/**
 * User-requested shell execution cancellation handle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellCancelUserRequestedRequest".
 */
/** @experimental */
export interface ShellCancelUserRequestedRequest {
  /**
   * Request ID previously passed to executeUserRequested
   */
  requestId: string;
}
/**
 * Shell command to run, with optional working directory and timeout in milliseconds.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellExecRequest".
 */
/** @experimental */
export interface ShellExecRequest {
  /**
   * Shell command to execute
   */
  command: string;
  /**
   * Working directory (defaults to session working directory)
   */
  cwd?: string;
  /**
   * Timeout in milliseconds (default: 30000)
   */
  timeout?: number;
}
/**
 * Identifier of the spawned process, used to correlate streamed output and exit notifications.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellExecResult".
 */
/** @experimental */
export interface ShellExecResult {
  /**
   * Unique identifier for tracking streamed output
   */
  processId: string;
}
/**
 * User-requested shell command and cancellation handle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellExecuteUserRequestedRequest".
 */
/** @experimental */
export interface ShellExecuteUserRequestedRequest {
  /**
   * Caller-provided cancellation handle for this execution
   */
  requestId: string;
  /**
   * Shell command to execute
   */
  command: string;
}
/**
 * Identifier of a process previously returned by "shell.exec" and the signal to send.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillRequest".
 */
/** @experimental */
export interface ShellKillRequest {
  /**
   * Process identifier returned by shell.exec
   */
  processId: string;
  signal?: ShellKillSignal;
}
/**
 * Indicates whether the signal was delivered; false if the process was unknown or already exited.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShellKillResult".
 */
/** @experimental */
export interface ShellKillResult {
  /**
   * Whether the signal was sent successfully
   */
  killed: boolean;
}
/**
 * Parameters for shutting down the session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ShutdownRequest".
 */
/** @experimental */
export interface ShutdownRequest {
  type?: ShutdownType;
  /**
   * Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'.
   */
  reason?: string;
}
/**
 * Skill metadata available to a session, with name, description, source, enabled/invocable state, path, plugin, and argument hint.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Skill".
 */
/** @experimental */
export interface Skill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Canonical slash command name used to invoke the skill, without the leading '/'
   */
  commandName?: string;
  /**
   * Description of what the skill does
   */
  description: string;
  source: SkillSource;
  /**
   * Whether the skill can be invoked by the user as a slash command
   */
  userInvocable: boolean;
  /**
   * Whether the skill is currently enabled
   */
  enabled: boolean;
  /**
   * Absolute path to the skill file
   */
  path?: string;
  /**
   * Name of the plugin that provides the skill, when source is 'plugin'
   */
  pluginName?: string;
  /**
   * Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
   */
  argumentHint?: string;
}
/**
 * Canonical directory where skills can be discovered or created, with scope, preference, and optional project path.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillDiscoveryPath".
 */
/** @experimental */
export interface SkillDiscoveryPath {
  /**
   * Absolute path of the create/discovery target (may not exist on disk yet)
   */
  path: string;
  scope: SkillDiscoveryScope;
  /**
   * Whether this is the canonical directory to create a new skill in its tier. At most one entry per tier is preferred; the `personal-agents` and `custom` scopes are never preferred.
   */
  preferredForCreation: boolean;
  /**
   * The input project path this directory was derived from (only for project scope)
   */
  projectPath?: string;
}
/**
 * Canonical locations where skills can be created so the runtime will recognize them.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillDiscoveryPathList".
 */
/** @experimental */
export interface SkillDiscoveryPathList {
  /**
   * Canonical skill create/discovery directories, in priority order
   */
  paths: SkillDiscoveryPath[];
}
/**
 * Skills available to the session, with their enabled state.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillList".
 */
/** @experimental */
export interface SkillList {
  /**
   * Available skills
   */
  skills: Skill[];
}
/**
 * Skill names to mark as disabled in global configuration, replacing any previous list.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsConfigSetDisabledSkillsRequest".
 */
/** @experimental */
export interface SkillsConfigSetDisabledSkillsRequest {
  /**
   * List of skill names to disable
   */
  disabledSkills: string[];
}
/**
 * Adds or removes a single skill from the global disabled list, leaving every other entry untouched.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsConfigSetSkillDisabledRequest".
 */
/** @experimental */
export interface SkillsConfigSetSkillDisabledRequest {
  /**
   * Name of the skill to add to or remove from the disabled list
   */
  name: string;
  /**
   * True to disable the skill, false to enable it
   */
  disabled: boolean;
}
/**
 * Name of the skill to disable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsDisableRequest".
 */
/** @experimental */
export interface SkillsDisableRequest {
  /**
   * Name of the skill to disable
   */
  name: string;
}
/**
 * Optional project paths and additional skill directories to include in discovery.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsDiscoverRequest".
 */
/** @experimental */
export interface SkillsDiscoverRequest {
  /**
   * Optional list of project directory paths to scan for project-scoped skills
   */
  projectPaths?: string[];
  /**
   * Optional list of additional skill directory paths to include
   */
  skillDirectories?: string[];
  /**
   * When true, omit skills from the host's global sources (personal, custom, plugin, and built-in), returning only project-scoped skills. For multitenant deployments.
   */
  excludeHostSkills?: boolean;
}
/**
 * Name of the skill to enable for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsEnableRequest".
 */
/** @experimental */
export interface SkillsEnableRequest {
  /**
   * Name of the skill to enable
   */
  name: string;
}
/**
 * Optional project paths to enumerate.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsGetDiscoveryPathsRequest".
 */
/** @experimental */
export interface SkillsGetDiscoveryPathsRequest {
  /**
   * Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned.
   */
  projectPaths?: string[];
  /**
   * When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments.
   */
  excludeHostSkills?: boolean;
}
/**
 * Skills invoked during this session, ordered by invocation time (most recent last).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsGetInvokedResult".
 */
/** @experimental */
export interface SkillsGetInvokedResult {
  /**
   * Skills invoked during this session, ordered by invocation time (most recent last)
   */
  skills: SkillsInvokedSkill[];
}
/**
 * Skill invocation record with name, path, content, allowed tools, and turn number.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsInvokedSkill".
 */
/** @experimental */
export interface SkillsInvokedSkill {
  /**
   * Unique identifier for the skill
   */
  name: string;
  /**
   * Path to the SKILL.md file
   */
  path: string;
  /**
   * Full content of the skill file
   */
  content: string;
  /**
   * Tools that should be auto-approved when this skill is active, captured at invocation time
   */
  allowedTools?: string[];
  /**
   * Turn number when the skill was invoked
   */
  invokedAtTurn: number;
}
/**
 * Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SkillsLoadDiagnostics".
 */
/** @experimental */
export interface SkillsLoadDiagnostics {
  /**
   * Warnings emitted while loading skills (e.g. skills that loaded but had issues)
   */
  warnings: string[];
  /**
   * Errors emitted while loading skills (e.g. skills that failed to load entirely)
   */
  errors: string[];
}

/** @experimental */
export interface SlashCommandAddTimelineEntryResult {
  /**
   * Discriminator for an add-timeline-entry result.
   */
  kind: "add-timeline-entry";
  entry: SlashCommandTimelineEntry;
  /**
   * Optional text the host should prefill into the input editor.
   */
  prefillInput?: string;
  /**
   * Whether command execution changed persisted runtime settings.
   */
  runtimeSettingsChanged?: boolean;
}

/** @experimental */
export interface SlashCommandTimelineEntry {
  /**
   * Timeline entry presentation type.
   */
  type: string;
  /**
   * Text displayed for the timeline entry.
   */
  text: string;
  /**
   * Optional URL associated with the timeline entry.
   */
  url?: string;
}
/**
 * Slash-command invocation result that submits an agent prompt, with display prompt, optional mode, optional user-facing notice, and settings-change flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandAgentPromptResult".
 */
/** @experimental */
export interface SlashCommandAgentPromptResult {
  /**
   * Agent prompt result discriminator
   */
  kind: "agent-prompt";
  /**
   * Prompt to submit to the agent
   */
  prompt: string;
  /**
   * Prompt text to display to the user
   */
  displayPrompt: string;
  mode?: SessionMode;
  /**
   * Optional user-facing notice to show before the prompt is submitted
   */
  notice?: string;
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Slash-command invocation result indicating completion, with optional message and settings-change flag.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandCompletedResult".
 */
/** @experimental */
export interface SlashCommandCompletedResult {
  /**
   * Completed result discriminator
   */
  kind: "completed";
  /**
   * Optional user-facing message describing the completed command
   */
  message?: string;
  mode?: SessionMode;
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Slash-command invocation result containing text output plus Markdown/ANSI rendering flags.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandTextResult".
 */
/** @experimental */
export interface SlashCommandTextResult {
  /**
   * Text result discriminator
   */
  kind: "text";
  /**
   * Text output for the client to render
   */
  text: string;
  /**
   * Whether text contains Markdown
   */
  markdown?: boolean;
  /**
   * Whether ANSI sequences should be preserved
   */
  preserveAnsi?: boolean;
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Slash-command invocation result asking the client to present subcommand options for a parent command.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandSelectSubcommandResult".
 */
/** @experimental */
export interface SlashCommandSelectSubcommandResult {
  /**
   * Select subcommand result discriminator
   */
  kind: "select-subcommand";
  /**
   * Parent command name that requires subcommand selection
   */
  command: string;
  /**
   * Human-readable title for the selection UI
   */
  title: string;
  /**
   * Available subcommand options for the client to present
   */
  options: SlashCommandSelectSubcommandOption[];
  /**
   * True when the invocation mutated user runtime settings; consumers caching settings should refresh
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Selectable slash-command subcommand option with name, description, and optional group label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SlashCommandSelectSubcommandOption".
 */
/** @experimental */
export interface SlashCommandSelectSubcommandOption {
  /**
   * Subcommand name to invoke
   */
  name: string;
  /**
   * Human-readable description of the subcommand
   */
  description: string;
  /**
   * Optional group label for organizing options
   */
  group?: string;
}

/** @experimental */
export interface SlashCommandShowDialogResult {
  /**
   * Discriminator for a show-dialog result.
   */
  kind: "show-dialog";
  dialog: SlashCommandModelPickerDialog;
  /**
   * Whether command execution changed persisted runtime settings.
   */
  runtimeSettingsChanged?: boolean;
}

/** @experimental */
export interface SlashCommandModelPickerDialog {
  /**
   * Discriminator for a model-picker dialog.
   */
  kind: "model-picker";
  /**
   * Model that should be enabled before it can be selected.
   */
  modelToEnable?: string;
  /**
   * Settings scope the picker should modify.
   */
  scope?: string;
  /**
   * Model-selection target represented by the picker.
   */
  target?: string;
}

/** @experimental */
export interface SlashCommandSetModelResult {
  /**
   * Discriminator for a set-model result.
   */
  kind: "set-model";
  /**
   * Model selected by the command.
   */
  model: string;
  /**
   * Settings scope modified by the command.
   */
  scope?: string;
  /**
   * User-facing warning produced while selecting the model.
   */
  warning?: string;
  /**
   * Reasoning effort selected for the model.
   */
  reasoningEffort?: string;
  /**
   * User-settings snapshot to restore if the host cancels the model switch.
   */
  revertOnCancel?: {};
  /**
   * Repository settings scope modified by the command.
   */
  repoScope?: string;
  /**
   * Whether command execution changed persisted runtime settings.
   */
  runtimeSettingsChanged?: boolean;
}

/** @experimental */
export interface SlashCommandSetPlanModelResult {
  /**
   * Discriminator for a set-plan-model result.
   */
  kind: "set-plan-model";
  /**
   * Dedicated model selected for plan mode.
   */
  planModel?: string;
  /**
   * User-facing confirmation message for the plan-model selection.
   */
  message: string;
  /**
   * Whether command execution changed persisted runtime settings.
   */
  runtimeSettingsChanged?: boolean;
}
/**
 * Subagent model, reasoning effort, and context tier settings
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SubagentSettingsEntry".
 */
/** @experimental */
export interface SubagentSettingsEntry {
  /**
   * Model override for matching subagents
   */
  model?: string;
  modelPolicy?: AgentModelPolicy;
  /**
   * Reasoning effort override for matching subagents
   */
  effortLevel?: string;
  contextTier?: SubagentSettingsEntryContextTier;
}
/**
 * Tracked background agent task metadata, including IDs, status, timing, agent type, prompt, model, result, and latest response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentInfo".
 */
/** @experimental */
export interface TaskAgentInfo {
  /**
   * Task kind
   */
  type: "agent";
  /**
   * Unique task identifier
   */
  id: string;
  /**
   * Tool call ID associated with this agent task
   */
  toolCallId: string;
  /**
   * Friendly, non-unique name intended for display
   */
  displayName?: string;
  /**
   * Short description of the task
   */
  description: string;
  status: TaskStatus;
  /**
   * ISO 8601 timestamp when the task was started
   */
  startedAt: string;
  /**
   * ISO 8601 timestamp when the task finished
   */
  completedAt?: string;
  /**
   * Accumulated active execution time in milliseconds
   */
  activeTimeMs?: number;
  /**
   * ISO 8601 timestamp when the current active period began
   */
  activeStartedAt?: string;
  /**
   * Error message when the task failed
   */
  error?: string;
  /**
   * Type of agent running this task
   */
  agentType: string;
  /**
   * Most recent prompt delivered to the agent. Updated whenever the agent receives a follow-up message.
   */
  prompt: string;
  /**
   * Result text from the task when available
   */
  result?: string;
  /**
   * Requested model override for the task when specified
   */
  model?: string;
  /**
   * Runtime model resolved for the task when available
   */
  resolvedModel?: string;
  executionMode?: TaskExecutionMode;
  /**
   * Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter.
   */
  canPromoteToBackground?: boolean;
  /**
   * Most recent response text from the agent
   */
  latestResponse?: string;
  /**
   * ISO 8601 timestamp when the agent entered idle state
   */
  idleSince?: string;
}
/**
 * Progress snapshot for an agent task, with recent activity lines and optional latest intent.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskAgentProgress".
 */
/** @experimental */
export interface TaskAgentProgress {
  /**
   * Progress kind
   */
  type: "agent";
  /**
   * Recent tool execution events converted to display lines
   */
  recentActivity: TaskProgressLine[];
  /**
   * The most recent intent reported by the agent
   */
  latestIntent?: string;
}
/**
 * Timestamped display line for task progress output or recent agent activity.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskProgressLine".
 */
/** @experimental */
export interface TaskProgressLine {
  /**
   * Display message, e.g., "▸ bash", "✓ edit src/foo.ts"
   */
  message: string;
  /**
   * ISO 8601 timestamp when this event occurred
   */
  timestamp: string;
}

/** @experimental */
export interface TaskCompletionDecision {
  outcome: TaskCompletionOutcome;
  /**
   * Rationale for the completion decision, when one is available.
   */
  reason?: string;
  /**
   * Whether the rationale was derived from completion-reviewer output.
   */
  reviewerDerived?: boolean;
  /**
   * Information-flow metadata captured from the completion reviewer.
   */
  reviewerResultMeta?: JsonValue;
  /**
   * Active autopilot objective evaluated by the completion reviewer.
   */
  objectiveId?: number;
  /**
   * Whether completion was accepted after the reviewer-rejection budget was exhausted.
   */
  completionRejectionBudgetExhausted?: boolean;
  /**
   * Objective eligibility token captured when the decision was evaluated.
   */
  completionEligibilityToken?: number;
}
/**
 * Tracked shell task metadata, including ID, command, status, timing, attachment/execution mode, log path, and PID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellInfo".
 */
/** @experimental */
export interface TaskShellInfo {
  /**
   * Task kind
   */
  type: "shell";
  /**
   * Unique task identifier
   */
  id: string;
  /**
   * Short description of the task
   */
  description: string;
  status: TaskStatus;
  /**
   * ISO 8601 timestamp when the task was started
   */
  startedAt: string;
  /**
   * ISO 8601 timestamp when the task finished
   */
  completedAt?: string;
  /**
   * Command being executed
   */
  command: string;
  attachmentMode: TaskShellInfoAttachmentMode;
  executionMode?: TaskExecutionMode;
  /**
   * Whether this shell task can be promoted to background mode
   */
  canPromoteToBackground?: boolean;
  /**
   * Path to the detached shell log, when available
   */
  logPath?: string;
  /**
   * Process ID when available
   */
  pid?: number;
}
/**
 * Background tasks currently tracked by the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskList".
 */
/** @experimental */
export interface TaskList {
  /**
   * Currently tracked tasks
   */
  tasks: TaskInfo[];
}
/**
 * Progress snapshot for a shell task, with recent stdout/stderr output and optional process ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TaskShellProgress".
 */
/** @experimental */
export interface TaskShellProgress {
  /**
   * Progress kind
   */
  type: "shell";
  /**
   * Recent stdout/stderr lines from the running shell command
   */
  recentOutput: string;
  /**
   * Process ID when available
   */
  pid?: number;
}
/**
 * Identifier of the background task to cancel.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksCancelRequest".
 */
/** @experimental */
export interface TasksCancelRequest {
  /**
   * Task identifier
   */
  id: string;
}
/**
 * Indicates whether the background task was successfully cancelled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksCancelResult".
 */
/** @experimental */
export interface TasksCancelResult {
  /**
   * Whether the task was successfully cancelled
   */
  cancelled: boolean;
}
/**
 * The first sync-waiting task that can currently be promoted to background mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksGetCurrentPromotableResult".
 */
/** @experimental */
export interface TasksGetCurrentPromotableResult {
  task?: TaskInfo;
}
/**
 * Identifier of the background task to fetch progress for.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksGetProgressRequest".
 */
/** @experimental */
export interface TasksGetProgressRequest {
  /**
   * Task identifier (agent ID or shell ID)
   */
  id: string;
}
/**
 * Progress information for the task, or null when no task with that ID is tracked.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksGetProgressResult".
 */
/** @experimental */
export interface TasksGetProgressResult {
  /**
   * Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
   */
  progress?: TaskProgress | null;
}
/**
 * The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksPromoteCurrentToBackgroundResult".
 */
/** @experimental */
export interface TasksPromoteCurrentToBackgroundResult {
  task?: TaskInfo;
}
/**
 * Identifier of the task to promote to background mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksPromoteToBackgroundRequest".
 */
/** @experimental */
export interface TasksPromoteToBackgroundRequest {
  /**
   * Task identifier
   */
  id: string;
}
/**
 * Indicates whether the task was successfully promoted to background mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksPromoteToBackgroundResult".
 */
/** @experimental */
export interface TasksPromoteToBackgroundResult {
  /**
   * Whether the task was successfully promoted to background mode
   */
  promoted: boolean;
}
/**
 * Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksRefreshResult".
 */
/** @experimental */
export interface TasksRefreshResult {}
/**
 * Identifier of the completed or cancelled task to remove from tracking.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksRemoveRequest".
 */
/** @experimental */
export interface TasksRemoveRequest {
  /**
   * Task identifier
   */
  id: string;
}
/**
 * Indicates whether the task was removed. False when the task does not exist or is still running/idle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksRemoveResult".
 */
/** @experimental */
export interface TasksRemoveResult {
  /**
   * Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
   */
  removed: boolean;
}
/**
 * Identifier of the target agent task, message content, and optional sender agent ID.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksSendMessageRequest".
 */
/** @experimental */
export interface TasksSendMessageRequest {
  /**
   * Agent task identifier
   */
  id: string;
  /**
   * Message content to send to the agent
   */
  message: string;
  /**
   * Agent ID of the sender, if sent on behalf of another agent
   */
  fromAgentId?: string;
}
/**
 * Indicates whether the message was delivered, with an error message when delivery failed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksSendMessageResult".
 */
/** @experimental */
export interface TasksSendMessageResult {
  /**
   * Whether the message was successfully delivered or steered
   */
  sent: boolean;
  /**
   * Error message if delivery failed
   */
  error?: string;
}
/**
 * Agent type, prompt, name, and optional description and model override for the new task.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksStartAgentRequest".
 */
/** @experimental */
export interface TasksStartAgentRequest {
  /**
   * Type of agent to start (e.g., 'explore', 'task', 'general-purpose')
   */
  agentType: string;
  /**
   * Task prompt for the agent
   */
  prompt: string;
  /**
   * Friendly, non-unique name used when displaying the agent
   */
  name: string;
  /**
   * Short description of the task
   */
  description?: string;
  /**
   * Optional model override
   */
  model?: string;
}
/**
 * Identifier assigned to the newly started background agent task.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksStartAgentResult".
 */
/** @experimental */
export interface TasksStartAgentResult {
  /**
   * Generated agent ID for the background task
   */
  agentId: string;
}
/**
 * Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TasksWaitForPendingResult".
 */
/** @experimental */
export interface TasksWaitForPendingResult {}
/**
 * Feature override key/value pairs to attach to subsequent telemetry events from this session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "TelemetrySetFeatureOverridesRequest".
 */
/** @experimental */
export interface TelemetrySetFeatureOverridesRequest {
  /**
   * Override key/value pairs to attach to subsequent telemetry events from this session. Replaces any previously-set overrides.
   */
  features: {
    [k: string]: string | undefined;
  };
}
/**
 * Built-in tool metadata with identifier, optional namespaced name, description, input-parameter schema, and usage instructions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "Tool".
 */
/** @experimental */
export interface Tool {
  /**
   * Tool identifier (e.g., "bash", "grep", "str_replace_editor")
   */
  name: string;
  /**
   * Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)
   */
  namespacedName?: string;
  /**
   * Description of what the tool does
   */
  description: string;
  /**
   * JSON Schema for the tool's input parameters
   */
  parameters?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Optional instructions for how to use this tool effectively
   */
  instructions?: string;
}
/**
 * Built-in tools available for the requested model, with their parameters and instructions.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolList".
 */
/** @experimental */
export interface ToolList {
  /**
   * List of available built-in tools with metadata
   */
  tools: Tool[];
}
/**
 * Expanded canonical result returned by a session tool.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolResultExpanded".
 */
/** @experimental */
export interface ToolResultExpanded {
  /**
   * Text result returned to the model.
   */
  textResultForLlm: string;
  resultType: ToolResultType;
  /**
   * Base64-encoded binary results returned to the model.
   */
  binaryResultsForLlm?: ExternalToolTextResultForLlmBinaryResultsForLlm[];
  /**
   * Detailed log content available for session display.
   */
  sessionLog?: string;
  /**
   * Error message for an unsuccessful execution.
   */
  error?: string;
  /**
   * Tool-specific telemetry payload.
   */
  toolTelemetry?: JsonValue;
  /**
   * Whether large-output post-processing should be skipped.
   */
  skipLargeOutputProcessing?: boolean;
  /**
   * Messages to inject after the tool result.
   */
  newMessages?: ToolResultNewMessage[];
  /**
   * Structured content blocks returned to the model.
   */
  contents?: ExternalToolTextResultForLlmContent[];
  /**
   * Deferred tool names made available by this result.
   */
  toolReferences?: string[];
  /**
   * Sources returned by the tool that the model may cite.
   */
  citableSources?: JsonValue[];
  /**
   * Skill invocation metadata produced by the tool.
   */
  skillInvocation?: JsonValue;
  /**
   * Whether post-tool-use failure hooks have already processed this result.
   */
  postToolUseFailureHooksProcessed?: boolean;
  /**
   * Optional UI resource produced by the tool.
   */
  uiResource?: JsonValue;
  /**
   * Metadata propagated with the tool result, including information-flow labels.
   */
  mcpMeta?: {
    [k: string]: JsonValue | undefined;
  };
  /**
   * Structured result content in addition to the model-facing text.
   */
  structuredContent?: JsonValue;
  taskCompletionDecision?: TaskCompletionDecision;
}
/**
 * A message injected by a tool result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolResultNewMessage".
 */
/** @experimental */
export interface ToolResultNewMessage {
  /**
   * Message content to inject after the tool result.
   */
  content: string;
  /**
   * Source attributed to the injected message.
   */
  source: string;
}
/**
 * A tool name and arguments to execute through the session's native invocation pipeline.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsExecuteRequest".
 */
/** @experimental */
export interface ToolsExecuteRequest {
  /**
   * Name of the currently offered tool to execute.
   */
  name: string;
  /**
   * Arguments supplied to the tool.
   */
  arguments: JsonValue;
  /**
   * Optional identifier used to correlate this invocation with its tool call.
   */
  toolCallId?: string;
}
/**
 * Options controlling how Rust-owned built-in tool descriptors are materialized.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsGetBuiltinDescriptorsRequest".
 */
/** @experimental */
export interface ToolsGetBuiltinDescriptorsRequest {
  /**
   * Whether descriptors should favor fewer user-intervention prompts.
   */
  reduceUserIntervention?: boolean;
  /**
   * Whether tool descriptors should include authoring metadata.
   */
  includeAuthor?: boolean;
  /**
   * Whether semantic skill lookup is available.
   */
  skillEmbeddingEnabled?: boolean;
  shellConfig?: ToolsShellDescriptorConfig;
  /**
   * Whether the configured shell supports PowerShell 7 syntax.
   */
  shellSupportsPowerShell7Syntax?: boolean;
  /**
   * Default shell timeout in milliseconds.
   */
  shellTimeoutMs?: number;
  /**
   * Whether background task completion notifications are enabled.
   */
  backgroundTaskNotificationsEnabled?: boolean;
}
/**
 * Shell-specific names and description lines used to materialize built-in shell tool descriptors.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsShellDescriptorConfig".
 */
/** @experimental */
export interface ToolsShellDescriptorConfig {
  /**
   * Stable shell type identifier.
   */
  shellType: string;
  /**
   * Human-readable shell name.
   */
  displayName: string;
  /**
   * Tool name used to start shell commands.
   */
  shellToolName: string;
  /**
   * Tool name used to read shell output.
   */
  readShellToolName: string;
  /**
   * Tool name used to stop shell commands.
   */
  stopShellToolName: string;
  /**
   * Tool name used to list active shells.
   */
  listShellsToolName: string;
  /**
   * Additional model-facing shell description lines.
   */
  descriptionLines: string[];
}
/**
 * Rust-owned built-in tool descriptors for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsGetBuiltinDescriptorsResult".
 */
/** @experimental */
export interface ToolsGetBuiltinDescriptorsResult {
  /**
   * Built-in tool descriptors materialized for the session.
   */
  tools: BuiltinToolDescriptor[];
}
/**
 * Current lightweight tool metadata snapshot for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsGetCurrentMetadataResult".
 */
/** @experimental */
export interface ToolsGetCurrentMetadataResult {
  /**
   * Current tool metadata, or null when tools have not been initialized yet
   */
  tools: CurrentToolMetadata[] | null;
}
/**
 * Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsInitializeAndValidateResult".
 */
/** @experimental */
export interface ToolsInitializeAndValidateResult {}
/**
 * Optional model identifier whose tool overrides should be applied to the listing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsListRequest".
 */
/** @experimental */
export interface ToolsListRequest {
  /**
   * Optional model ID — when provided, the returned tool list reflects model-specific overrides
   */
  model?: string;
}
/**
 * Complete externally implemented tool list for the calling connection. An empty list removes every tool previously supplied by that connection.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsSetRequest".
 */
/** @experimental */
export interface ToolsSetRequest {
  /**
   * Complete replacement list for the calling connection.
   */
  tools: ProtocolExternalToolDefinition[];
}
/**
 * Empty result after replacing the calling connection's externally implemented tools.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsSetResult".
 */
/** @experimental */
export interface ToolsSetResult {}
/**
 * Task-completion tool arguments and final result used to build a label-safe session event payload.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsTaskCompleteEventDataRequest".
 */
/** @experimental */
export interface ToolsTaskCompleteEventDataRequest {
  /**
   * Arguments supplied to the completed task_complete tool call.
   */
  toolArgs: JsonValue;
  finalResult: ToolResultExpanded;
}
/**
 * Empty result after applying subagent settings
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "ToolsUpdateSubagentSettingsResult".
 */
/** @experimental */
export interface ToolsUpdateSubagentSettingsResult {}
/**
 * Multi-select string field where each option pairs a value with a display label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayAnyOfField".
 */
/** @experimental */
export interface UIElicitationArrayAnyOfField {
  /**
   * Type discriminator. Always "array".
   */
  type: "array";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum number of items the user must select.
   */
  minItems?: number;
  /**
   * Maximum number of items the user may select.
   */
  maxItems?: number;
  items: UIElicitationArrayAnyOfFieldItems;
  /**
   * Default values selected when the form is first shown.
   */
  default?: string[];
}
/**
 * Schema applied to each item in the array.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayAnyOfFieldItems".
 */
/** @experimental */
export interface UIElicitationArrayAnyOfFieldItems {
  /**
   * Selectable options, each with a value and a display label.
   */
  anyOf: UIElicitationArrayAnyOfFieldItemsAnyOf[];
}
/**
 * Selectable option for a UI elicitation multi-select array item, with submitted value and display label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayAnyOfFieldItemsAnyOf".
 */
/** @experimental */
export interface UIElicitationArrayAnyOfFieldItemsAnyOf {
  /**
   * Value submitted when this option is selected.
   */
  const: string;
  /**
   * Display label for this option.
   */
  title: string;
}
/**
 * Multi-select string field whose allowed values are defined inline.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayEnumField".
 */
/** @experimental */
export interface UIElicitationArrayEnumField {
  /**
   * Type discriminator. Always "array".
   */
  type: "array";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum number of items the user must select.
   */
  minItems?: number;
  /**
   * Maximum number of items the user may select.
   */
  maxItems?: number;
  items: UIElicitationArrayEnumFieldItems;
  /**
   * Default values selected when the form is first shown.
   */
  default?: string[];
}
/**
 * Schema applied to each item in the array.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationArrayEnumFieldItems".
 */
/** @experimental */
export interface UIElicitationArrayEnumFieldItems {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Allowed string values for each selected item.
   */
  enum: string[];
}
/**
 * Prompt message and JSON schema describing the form fields to elicit from the user.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationRequest".
 */
/** @experimental */
export interface UIElicitationRequest {
  mode?: McpElicitationFormMode;
  /**
   * Message describing what information is needed from the user
   */
  message: string;
  requestedSchema: UIElicitationSchema;
  /**
   * MCP request metadata.
   */
  _meta?: {
    [k: string]: unknown | undefined;
  };
  task?: McpTaskMetadata;
  [k: string]: unknown | undefined;
}
/**
 * JSON Schema describing the form fields to present to the user
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchema".
 */
/** @experimental */
export interface UIElicitationSchema {
  /**
   * Schema type indicator (always 'object')
   */
  type: "object";
  /**
   * Form field definitions, keyed by field name
   */
  properties: {
    [k: string]: UIElicitationSchemaProperty | undefined;
  };
  /**
   * List of required field names
   */
  required?: string[];
}
/**
 * Single-select string field whose allowed values are defined inline.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationStringEnumField".
 */
/** @experimental */
export interface UIElicitationStringEnumField {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Allowed string values.
   */
  enum: string[];
  /**
   * Optional display labels for each enum value, in the same order as `enum`.
   */
  enumNames?: string[];
  /**
   * Default value selected when the form is first shown.
   */
  default?: string;
}
/**
 * Single-select string field where each option pairs a value with a display label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationStringOneOfField".
 */
/** @experimental */
export interface UIElicitationStringOneOfField {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Selectable options, each with a value and a display label.
   */
  oneOf: UIElicitationStringOneOfFieldOneOf[];
  /**
   * Default value selected when the form is first shown.
   */
  default?: string;
}
/**
 * Selectable option for a UI elicitation single-select string field, with submitted value and display label.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationStringOneOfFieldOneOf".
 */
/** @experimental */
export interface UIElicitationStringOneOfFieldOneOf {
  /**
   * Value submitted when this option is selected.
   */
  const: string;
  /**
   * Display label for this option.
   */
  title: string;
}
/**
 * Boolean field rendered as a yes/no toggle.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyBoolean".
 */
/** @experimental */
export interface UIElicitationSchemaPropertyBoolean {
  /**
   * Type discriminator. Always "boolean".
   */
  type: "boolean";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Default value selected when the form is first shown.
   */
  default?: boolean;
}
/**
 * Free-text string field with optional length and format constraints.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyString".
 */
/** @experimental */
export interface UIElicitationSchemaPropertyString {
  /**
   * Type discriminator. Always "string".
   */
  type: "string";
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum number of characters required.
   */
  minLength?: number;
  /**
   * Maximum number of characters allowed.
   */
  maxLength?: number;
  format?: UIElicitationSchemaPropertyStringFormat;
  /**
   * Default value populated in the input when the form is first shown.
   */
  default?: string;
}
/**
 * Numeric field accepting either a number or an integer.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationSchemaPropertyNumber".
 */
/** @experimental */
export interface UIElicitationSchemaPropertyNumber {
  type: UIElicitationSchemaPropertyNumberType;
  /**
   * Human-readable label for the field.
   */
  title?: string;
  /**
   * Help text describing the field.
   */
  description?: string;
  /**
   * Minimum allowed value (inclusive).
   */
  minimum?: number;
  /**
   * Maximum allowed value (inclusive).
   */
  maximum?: number;
  /**
   * Default value populated in the input when the form is first shown.
   */
  default?: number;
}
/**
 * The elicitation response (accept with form values, decline, or cancel)
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponse".
 */
/** @experimental */
export interface UIElicitationResponse {
  action: UIElicitationResponseAction;
  content?: UIElicitationResponseContent;
  /**
   * MCP response metadata.
   */
  _meta?: {
    [k: string]: unknown | undefined;
  };
  [k: string]: unknown | undefined;
}
/**
 * The form values submitted by the user (present when action is 'accept')
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResponseContent".
 */
/** @experimental */
export interface UIElicitationResponseContent {
  [k: string]: UIElicitationFieldValue;
}
/**
 * Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIElicitationResult".
 */
/** @experimental */
export interface UIElicitationResult {
  /**
   * Whether the response was accepted. False if the request was already resolved by another client.
   */
  success: boolean;
}
/**
 * Transient question to answer without adding it to conversation history.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIEphemeralQueryRequest".
 */
/** @experimental */
export interface UIEphemeralQueryRequest {
  /**
   * Question to answer from the current conversation context.
   */
  question: string;
  /**
   * In-process streaming callback `(text) => void` invoked with each token as the model emits it. Internal and excluded from the public SDK surface.
   *
   * @internal
   */
  onChunk?: OpaqueInProcessValue;
  /**
   * In-process `AbortSignal` forwarded to the model client to cancel an in-flight request. Internal and excluded from the public SDK surface.
   *
   * @internal
   */
  abortSignal?: OpaqueInProcessValue;
}
/**
 * Completed transient query. Ordered chunks and the terminal outcome are also delivered through `ui.ephemeral_query` session events while it runs.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIEphemeralQueryResult".
 */
/** @experimental */
export interface UIEphemeralQueryResult {
  /**
   * Answer returned by the model
   */
  answer: string;
}
/**
 * User response for a pending exit-plan-mode request, with approval state, selected action, auto-approve flag, and feedback.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIExitPlanModeResponse".
 */
/** @experimental */
export interface UIExitPlanModeResponse {
  /**
   * Whether the plan was approved.
   */
  approved: boolean;
  selectedAction?: UIExitPlanModeAction;
  /**
   * Whether subsequent edits should be auto-approved without confirmation.
   */
  autoApproveEdits?: boolean;
  /**
   * Feedback from the user when they declined the plan or requested changes.
   */
  feedback?: string;
  /**
   * When true, the agent is instructed to end its turn without starting implementation so the client can restore the session model and auto-submit a fresh implementation turn on it. Set only when a distinct plan configuration (a different model, reasoning effort, or context tier) actually ran the planning turn.
   */
  deferImplementation?: boolean;
}
/**
 * Request ID of a pending `auto_mode_switch.requested` event and the user's response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingAutoModeSwitchRequest".
 */
/** @experimental */
export interface UIHandlePendingAutoModeSwitchRequest {
  /**
   * The unique request ID from the auto_mode_switch.requested event
   */
  requestId: string;
  response: UIAutoModeSwitchResponse;
}
/**
 * Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingElicitationRequest".
 */
/** @experimental */
export interface UIHandlePendingElicitationRequest {
  /**
   * The unique request ID from the elicitation.requested event
   */
  requestId: string;
  result: UIElicitationResponse;
}
/**
 * Request ID of a pending `exit_plan_mode.requested` event and the user's response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingExitPlanModeRequest".
 */
/** @experimental */
export interface UIHandlePendingExitPlanModeRequest {
  /**
   * The unique request ID from the exit_plan_mode.requested event
   */
  requestId: string;
  response: UIExitPlanModeResponse;
}
/**
 * Indicates whether the pending UI request was resolved by this call.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingResult".
 */
/** @experimental */
export interface UIHandlePendingResult {
  /**
   * True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
   */
  success: boolean;
}
/**
 * Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingSamplingRequest".
 */
/** @experimental */
export interface UIHandlePendingSamplingRequest {
  /**
   * The unique request ID from the sampling.requested event
   */
  requestId: string;
  response?: UIHandlePendingSamplingResponse;
}
/**
 * Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingSamplingResponse".
 */
/** @experimental */
export interface UIHandlePendingSamplingResponse {
  [k: string]: unknown | undefined;
}
/**
 * Request ID of a pending `session_limits_exhausted.requested` event and the user's selected limit action.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingSessionLimitsExhaustedRequest".
 */
/** @experimental */
export interface UIHandlePendingSessionLimitsExhaustedRequest {
  /**
   * The unique request ID from the session_limits_exhausted.requested event
   */
  requestId: string;
  response: UISessionLimitsExhaustedResponse;
}
/**
 * The user's selected action for an exhausted session limit.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UISessionLimitsExhaustedResponse".
 */
/** @experimental */
export interface UISessionLimitsExhaustedResponse {
  action: UISessionLimitsExhaustedResponseAction;
  /**
   * AI Credits to add to the current max when action is 'add'.
   */
  additionalAiCredits?: number;
  /**
   * New absolute max AI Credits when action is 'set'.
   */
  maxAiCredits?: number;
}
/**
 * Request ID of a pending `user_input.requested` event and the user's response.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIHandlePendingUserInputRequest".
 */
/** @experimental */
export interface UIHandlePendingUserInputRequest {
  /**
   * The unique request ID from the user_input.requested event
   */
  requestId: string;
  response: UIUserInputResponse;
}
/**
 * User response for a pending user-input request, with answer text and whether it was typed freeform.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIUserInputResponse".
 */
/** @experimental */
export interface UIUserInputResponse {
  /**
   * The user's answer text
   */
  answer: string;
  /**
   * True if the user typed a freeform response, false if they selected a presented choice. Used by telemetry to differentiate between free text input and choice selection.
   */
  wasFreeform: boolean;
}
/**
 * Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIRegisterDirectAutoModeSwitchHandlerResult".
 */
/** @experimental */
export interface UIRegisterDirectAutoModeSwitchHandlerResult {
  /**
   * Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
   */
  handle: string;
}
/**
 * Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIUnregisterDirectAutoModeSwitchHandlerRequest".
 */
/** @experimental */
export interface UIUnregisterDirectAutoModeSwitchHandlerRequest {
  /**
   * Handle previously returned by `registerDirectAutoModeSwitchHandler`
   */
  handle: string;
}
/**
 * Indicates whether the handle was active and the registration count was decremented.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UIUnregisterDirectAutoModeSwitchHandlerResult".
 */
/** @experimental */
export interface UIUnregisterDirectAutoModeSwitchHandlerResult {
  /**
   * True if the handle was active and decremented the counter; false if the handle was unknown.
   */
  unregistered: boolean;
}
/**
 * Subagent settings to apply to the current session
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UpdateSubagentSettingsRequest".
 */
/** @experimental */
export interface UpdateSubagentSettingsRequest {
  /**
   * Subagent settings to apply, or null to clear the live session override
   */
  subagents?: SubagentSettings | null;
}
/**
 * Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageGetMetricsResult".
 */
/** @experimental */
export interface UsageGetMetricsResult {
  /**
   * Total user-initiated premium request cost across all models (may be fractional due to multipliers)
   */
  totalPremiumRequestCost: number;
  /**
   * Raw count of user-initiated API requests
   */
  totalUserRequests: number;
  /**
   * Session-wide accumulated nano-AI units cost
   */
  totalNanoAiu?: number;
  /**
   * Session-wide per-token-type accumulated token counts
   */
  tokenDetails?: {
    [k: string]: UsageMetricsTokenDetail | undefined;
  };
  /**
   * Total time spent in model API calls (milliseconds)
   */
  totalApiDurationMs: number;
  /**
   * ISO 8601 timestamp when the session started
   */
  sessionStartTime: string;
  codeChanges: UsageMetricsCodeChanges;
  /**
   * Per-model token and request metrics, keyed by model identifier
   */
  modelMetrics: {
    [k: string]: UsageMetricsModelMetric | undefined;
  };
  /**
   * Per-agent usage metrics, keyed by agent instance identifier. The main conversation uses the stable key `main`.
   */
  agentMetrics?: {
    [k: string]: UsageMetricsAgentMetric | undefined;
  };
  /**
   * Currently active model identifier
   */
  currentModel?: string;
  /**
   * Input tokens from the most recent main-agent API call
   */
  lastCallInputTokens: number;
  /**
   * Output tokens from the most recent main-agent API call
   */
  lastCallOutputTokens: number;
}
/**
 * Session-wide token-detail entry containing the accumulated token count for one token type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsTokenDetail".
 */
/** @experimental */
export interface UsageMetricsTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Aggregated code change metrics
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsCodeChanges".
 */
/** @experimental */
export interface UsageMetricsCodeChanges {
  /**
   * Total lines of code added
   */
  linesAdded: number;
  /**
   * Total lines of code removed
   */
  linesRemoved: number;
  /**
   * Number of distinct files modified
   */
  filesModifiedCount: number;
  /**
   * Distinct file paths modified during the session
   */
  filesModified: string[];
}
/**
 * Per-model usage metrics, including request counts/costs, token usage, nano-AI units, and per-token-type details.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetric".
 */
/** @experimental */
export interface UsageMetricsModelMetric {
  requests: UsageMetricsModelMetricRequests;
  usage: UsageMetricsModelMetricUsage;
  /**
   * Latest known prompt-cache expiration for this model. A timestamp in the past indicates that the observed cache has expired.
   */
  cacheExpiresAt?: string;
  /**
   * Accumulated nano-AI units cost for this model
   */
  totalNanoAiu?: number;
  /**
   * Token count details per type
   */
  tokenDetails?: {
    [k: string]: UsageMetricsModelMetricTokenDetail | undefined;
  };
}
/**
 * Request count and cost metrics for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricRequests".
 */
/** @experimental */
export interface UsageMetricsModelMetricRequests {
  /**
   * Number of API requests made with this model
   */
  count: number;
  /**
   * User-initiated premium request cost (with multiplier applied)
   */
  cost: number;
}
/**
 * Token usage metrics for this model
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricUsage".
 */
/** @experimental */
export interface UsageMetricsModelMetricUsage {
  /**
   * Total input tokens consumed
   */
  inputTokens: number;
  /**
   * Total output tokens produced
   */
  outputTokens: number;
  /**
   * Total tokens read from prompt cache
   */
  cacheReadTokens: number;
  /**
   * Total tokens written to prompt cache
   */
  cacheWriteTokens: number;
  /**
   * Total output tokens used for reasoning
   */
  reasoningTokens?: number;
}
/**
 * Per-model token-detail entry containing the accumulated token count for one token type.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsModelMetricTokenDetail".
 */
/** @experimental */
export interface UsageMetricsModelMetricTokenDetail {
  /**
   * Accumulated token count for this token type
   */
  tokenCount: number;
}
/**
 * Usage attributed to one agent instance, including its identity, API duration, AI units, and per-model breakdown.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UsageMetricsAgentMetric".
 */
/** @experimental */
export interface UsageMetricsAgentMetric {
  /**
   * Configured agent name, when this is a subagent
   */
  agentName?: string;
  /**
   * Human-readable label for this subagent invocation, copied from the originating `subagent.started` event. For task-tool subagents this is the invocation's task description rather than the agent's configured display name, so group by `agentName` for stable per-agent labels.
   */
  agentDisplayName?: string;
  /**
   * Time spent in model API calls by this agent, in milliseconds
   */
  totalApiDurationMs: number;
  /**
   * Accumulated nano-AI units cost for this agent
   */
  totalNanoAiu: number;
  /**
   * Per-model usage for this agent, keyed by model identifier
   */
  modelMetrics: {
    [k: string]: UsageMetricsModelMetric | undefined;
  };
}
/**
 * Result of a user-requested shell command.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserRequestedShellCommandResult".
 */
/** @experimental */
export interface UserRequestedShellCommandResult {
  /**
   * Tool call id emitted for the shell execution
   */
  toolCallId: string;
  /**
   * Whether the command completed successfully
   */
  success: boolean;
  /**
   * Captured command output
   */
  output: string;
  /**
   * Process exit code, when available
   */
  exitCode?: number | null;
  /**
   * Error output when the execution failed
   */
  error?: string;
}
/**
 * A single user setting's effective value alongside its default, so consumers can render settings left at their default.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserSettingMetadata".
 */
/** @experimental */
export interface UserSettingMetadata {
  /**
   * The effective value: the user's value if set, otherwise the default.
   */
  value: JsonValue;
  /**
   * The centrally-known default for this setting (null when no default is registered).
   */
  default: JsonValue;
  /**
   * True when the user has not set an explicit value for this setting (i.e. it is left at its default). Reflects whether the user has overridden the key, not whether the effective value happens to equal the default — a key explicitly set to a value identical to the default still reports false.
   */
  isDefault: boolean;
}
/**
 * Per-key metadata for every known user setting (settings.json overlaid with the legacy config.json, config.json wins), including settings left at their default. Excludes repository- and enterprise-managed overrides.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserSettingsGetResult".
 */
/** @experimental */
export interface UserSettingsGetResult {
  /**
   * Every known user setting keyed by setting name, each with its effective value, default, and whether it is at the default.
   */
  settings: {
    [k: string]: UserSettingMetadata;
  };
}
/**
 * Partial user settings to write to settings.json. Each top-level key is written individually, replacing the existing value; a key whose value is null is removed.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserSettingsSetRequest".
 */
/** @experimental */
export interface UserSettingsSetRequest {
  /**
   * Partial user settings to write, as a free-form object keyed by setting name
   */
  settings: JsonValue;
}
/**
 * Outcome of writing user settings.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "UserSettingsSetResult".
 */
/** @experimental */
export interface UserSettingsSetResult {
  /**
   * Top-level keys whose write landed in settings.json but is shadowed by a value still present in the legacy config.json (config.json wins on read). The write does not take effect until the legacy value is removed.
   */
  shadowedKeys: string[];
}
/**
 * Current sharing status and shareable GitHub URL for a session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "VisibilityGetResult".
 */
/** @experimental */
export interface VisibilityGetResult {
  /**
   * Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the session cannot be shared and `status`/`shareUrl` are absent.
   */
  synced: boolean;
  status?: SessionVisibilityStatus;
  /**
   * Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved.
   */
  shareUrl?: string;
}
/**
 * Desired sharing status for the session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "VisibilitySetRequest".
 */
/** @experimental */
export interface VisibilitySetRequest {
  status: SessionVisibilityStatus;
}
/**
 * Effective sharing status and shareable GitHub URL after updating session visibility.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "VisibilitySetResult".
 */
/** @experimental */
export interface VisibilitySetResult {
  /**
   * Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the visibility change could not be applied and `status`/`shareUrl` are absent.
   */
  synced: boolean;
  status?: SessionVisibilityStatus;
  /**
   * Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved.
   */
  shareUrl?: string;
}
/**
 * A single changed file and its unified diff.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceDiffFileChange".
 */
/** @experimental */
export interface WorkspaceDiffFileChange {
  /**
   * Path to the changed file, relative to the workspace root when the file lives under it. A file changed outside the workspace root keeps a `../`-relative path, or an absolute path when no relative path exists (for example a different Windows drive).
   */
  path: string;
  /**
   * Unified diff content for the file. Empty when the diff was truncated.
   */
  diff: string;
  changeType: WorkspaceDiffFileChangeType;
  /**
   * Original file path for renamed files.
   */
  oldPath?: string;
  /**
   * Whether the diff content was omitted because it exceeded the per-file size limit.
   */
  isTruncated?: boolean;
}
/**
 * Workspace diff result for the requested mode.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspaceDiffResult".
 */
/** @experimental */
export interface WorkspaceDiffResult {
  requestedMode: WorkspaceDiffMode;
  mode: WorkspaceDiffMode;
  /**
   * Changed files and their unified diffs.
   */
  changes: WorkspaceDiffFileChange[];
  /**
   * Default branch used for a branch diff, when branch mode was requested.
   */
  baseBranch?: string;
  /**
   * Whether the requested diff fell back to unstaged changes, either because branch diff failed or session diff was unavailable.
   */
  isFallback: boolean;
  unavailableReason?: HistoryRewindUnavailableReason;
}
/**
 * Compaction summary checkpoint to persist.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesAddSummaryRequest".
 */
/** @experimental */
export interface WorkspacesAddSummaryRequest {
  /**
   * Summary title shown in checkpoint listings.
   */
  title: string;
  /**
   * Markdown summary content to persist.
   */
  content: string;
}
/**
 * Persisted summary metadata and refreshed workspace metadata.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesAddSummaryResult".
 */
/** @experimental */
export interface WorkspacesAddSummaryResult {
  /**
   * Metadata for the persisted summary.
   */
  summary?: {};
  /**
   * Refreshed metadata for the containing workspace.
   */
  workspace?: {};
  [k: string]: unknown | undefined;
}
/**
 * Whether the autopilot objective file exists.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesAutopilotObjectiveExistsResult".
 */
/** @experimental */
export interface WorkspacesAutopilotObjectiveExistsResult {
  /**
   * True when the objective file exists.
   */
  exists: boolean;
}
/**
 * Workspace checkpoint metadata with assigned number, human-readable title, and checkpoint filename.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesCheckpoints".
 */
/** @experimental */
export interface WorkspacesCheckpoints {
  /**
   * Checkpoint number assigned by the workspace manager
   */
  number: number;
  /**
   * Human-readable checkpoint title
   */
  title: string;
  /**
   * Filename of the checkpoint within the workspace checkpoints directory
   */
  filename: string;
}
/**
 * Relative path and UTF-8 content for the workspace file to create or overwrite.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesCreateFileRequest".
 */
/** @experimental */
export interface WorkspacesCreateFileRequest {
  /**
   * Relative path within the workspace files directory
   */
  path: string;
  /**
   * File content to write as a UTF-8 string
   */
  content: string;
}
/**
 * Result of deleting the autopilot objective file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesDeleteAutopilotObjectiveResult".
 */
/** @experimental */
export interface WorkspacesDeleteAutopilotObjectiveResult {
  /**
   * True when a file was deleted.
   */
  deleted: boolean;
}
/**
 * Parameters for computing a workspace diff.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesDiffRequest".
 */
/** @experimental */
export interface WorkspacesDiffRequest {
  mode: WorkspaceDiffMode;
  /**
   * When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false.
   */
  ignoreWhitespace?: boolean;
}
/**
 * Optional session context used when creating a local workspace.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesEnsureRequest".
 */
/** @experimental */
export interface WorkspacesEnsureRequest {
  /**
   * Opaque workspace context supplied by the session host.
   */
  context?: JsonValue;
}
/**
 * Current workspace metadata for the session, including its absolute filesystem path when available.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesGetWorkspaceResult".
 */
/** @experimental */
export interface WorkspacesGetWorkspaceResult {
  /**
   * Current workspace metadata, or null if not available
   */
  workspace: {
    /**
     * Stable workspace identifier.
     */
    id: string;
    /**
     * Current working directory associated with the workspace.
     */
    cwd?: string;
    /**
     * Git repository root associated with the workspace.
     */
    git_root?: string;
    /**
     * Repository identifier associated with the workspace.
     */
    repository?: string;
    host_type?: WorkspacesWorkspaceDetailsHostType;
    /**
     * Current Git branch.
     */
    branch?: string;
    /**
     * Workspace display name.
     */
    name?: string;
    /**
     * Name of the client that created the workspace.
     */
    client_name?: string;
    /**
     * Whether the workspace name was explicitly chosen by the user.
     */
    user_named?: boolean;
    /**
     * Number of persisted summaries in the workspace.
     */
    summary_count?: number;
    /**
     * Timestamp when the workspace was created.
     */
    created_at?: string;
    /**
     * Timestamp when the workspace was last updated.
     */
    updated_at?: string;
    /**
     * Whether the workspace session can be steered remotely.
     */
    remote_steerable?: boolean;
    /**
     * Mission Control task identifier associated with the workspace.
     */
    mc_task_id?: string;
    /**
     * Mission Control session identifier associated with the workspace.
     */
    mc_session_id?: string;
    /**
     * Most recent Mission Control event identifier observed for the workspace.
     */
    mc_last_event_id?: string;
    /**
     * Whether the per-session Chronicle upgrade prompt was dismissed for the workspace.
     */
    chronicle_sync_dismissed?: boolean;
  } | null;
  /**
   * Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
   */
  path?: string;
}
/**
 * Workspace checkpoints in chronological order; empty when the workspace is not enabled.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesListCheckpointsResult".
 */
/** @experimental */
export interface WorkspacesListCheckpointsResult {
  /**
   * Workspace checkpoints in chronological order. Empty when workspace is not enabled.
   */
  checkpoints: WorkspacesCheckpoints[];
}
/**
 * Relative paths of files stored in the session workspace files directory.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesListFilesResult".
 */
/** @experimental */
export interface WorkspacesListFilesResult {
  /**
   * Relative file paths in the workspace files directory
   */
  files: string[];
}
/**
 * Autopilot objective file content, or null when missing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadAutopilotObjectiveResult".
 */
/** @experimental */
export interface WorkspacesReadAutopilotObjectiveResult {
  /**
   * Autopilot objective file content, or null when missing.
   */
  content: string | null;
}
/**
 * Checkpoint number to read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadCheckpointRequest".
 */
/** @experimental */
export interface WorkspacesReadCheckpointRequest {
  /**
   * Checkpoint number to read
   */
  number: number;
}
/**
 * Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadCheckpointResult".
 */
/** @experimental */
export interface WorkspacesReadCheckpointResult {
  /**
   * Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing
   */
  content: string | null;
}
/**
 * Relative path of the workspace file to read.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadFileRequest".
 */
/** @experimental */
export interface WorkspacesReadFileRequest {
  /**
   * Relative path within the workspace files directory
   */
  path: string;
}
/**
 * Contents of the requested workspace file as a UTF-8 string.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesReadFileResult".
 */
/** @experimental */
export interface WorkspacesReadFileResult {
  /**
   * File content as a UTF-8 string
   */
  content: string;
}
/**
 * Pasted content to save as a UTF-8 file in the session workspace.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesSaveLargePasteRequest".
 */
/** @experimental */
export interface WorkspacesSaveLargePasteRequest {
  /**
   * Pasted content to save as a UTF-8 file
   */
  content: string;
}
/**
 * Descriptor for the saved paste file, or null when the workspace is unavailable.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesSaveLargePasteResult".
 */
/** @experimental */
export interface WorkspacesSaveLargePasteResult {
  /**
   * Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions)
   */
  saved: {
    /**
     * Absolute filesystem path to the saved paste file
     */
    filePath: string;
    /**
     * Filename within the workspace files directory
     */
    filename: string;
    /**
     * Size of the saved file in bytes
     */
    sizeBytes: number;
  } | null;
}
/**
 * Rollback point for local workspace summaries.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesTruncateSummariesRequest".
 */
/** @experimental */
export interface WorkspacesTruncateSummariesRequest {
  /**
   * Number of newest summaries to keep.
   */
  keepCount: number;
}
/**
 * Workspace metadata fields to update.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesUpdateMetadataRequest".
 */
/** @experimental */
export interface WorkspacesUpdateMetadataRequest {
  /**
   * Opaque workspace context supplied by the session host.
   */
  context?: JsonValue;
  /**
   * Optional workspace display name override.
   */
  name?: string;
}
/**
 * Autopilot objective file content to persist.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesWriteAutopilotObjectiveRequest".
 */
/** @experimental */
export interface WorkspacesWriteAutopilotObjectiveRequest {
  /**
   * Autopilot objective file content.
   */
  content: string;
}
/**
 * Result of writing the autopilot objective file.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "WorkspacesWriteAutopilotObjectiveResult".
 */
/** @experimental */
export interface WorkspacesWriteAutopilotObjectiveResult {
  /**
   * Filesystem operation performed.
   */
  operation: string;
}

/** @experimental */
export interface SessionModelListRequest {
  /**
   * If true, bypasses the per-session model list cache and re-fetches from CAPI.
   */
  skipCache?: boolean;
}

/** @experimental */
export interface SessionAgentListRequest {
  /**
   * When true, request the session's configured built-in agents alongside custom agents. Listing applies feature, context, inclusion, exclusion, and user-disabled-agent policy, but does not evaluate transient invocation requirements such as model availability. Built-in metadata may be omitted when the session cannot project it, such as a relay session.
   */
  includeBuiltInAgents?: boolean;
  /**
   * When true, request authored base prompt text on each AgentInfo. Prompt text may be omitted when unavailable, such as for agents projected through a relay session.
   */
  includePrompt?: boolean;
}
/**
 * Standard MCP CallToolResult
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionMcpAppsCallToolResult".
 */
/** @experimental */
export interface SessionMcpAppsCallToolResult {
  [k: string]: JsonValue | undefined;
}

/** @experimental */
export interface SessionPluginsReloadRequest {
  /**
   * Reload MCP server connections after refreshing plugins. Defaults to true.
   */
  reloadMcp?: boolean;
  /**
   * Re-run custom-agent discovery after refreshing plugins. Defaults to true.
   */
  reloadCustomAgents?: boolean;
  /**
   * Re-load user, plugin, and (subject to `deferRepoHooks`) repo hooks. Defaults to true. Has no effect when the host has not registered a hook reloader (e.g. remote sessions).
   */
  reloadHooks?: boolean;
  /**
   * Re-discover and relaunch subprocess extensions (including plugin-shipped extensions) after refreshing plugins. Defaults to true. Has no effect when the session has no active extension controller (e.g. extensions were not requested for the session).
   */
  reloadExtensions?: boolean;
  /**
   * When true, skip repo-level hooks during the hook reload. Use before folder trust is confirmed; load them post-trust via `sessions.loadDeferredRepoHooks`.
   */
  deferRepoHooks?: boolean;
}

/** @experimental */
export interface SessionProviderGetEndpointRequest {
  /**
   * Model identifier the caller intends to use against the returned endpoint. Used to pick the correct wire shape. Omit to use whichever model the session is currently using.
   */
  modelId?: string;
}

/** @experimental */
export interface SessionCommandsListRequest {
  /**
   * Include runtime built-in commands
   */
  includeBuiltins?: boolean;
  /**
   * Include enabled user-invocable skills and commands
   */
  includeSkills?: boolean;
  /**
   * Include commands registered by protocol clients, including SDK clients and extensions
   */
  includeClientCommands?: boolean;
}

/** @experimental */
export interface SessionHistoryCompactRequest {
  /**
   * Optional user-provided instructions to focus the compaction summary
   */
  customInstructions?: string;
  /**
   * What initiated this compaction request, recorded as the `trigger` on the persisted `session.compaction_start` / `session.compaction_complete` events. When absent, the compaction is persisted without trigger attribution (initiator unknown).
   */
  trigger?: /** User-requested compaction, e.g. the /compact command or a direct history.compact call. */
    | "manual"
    /** Compaction requested while switching to a model with a smaller context window. */
    | "model_switch";
  /**
   * Context window token limit this compaction is targeting, recorded as the `tokenLimit` on the persisted `session.compaction_start` / `session.compaction_complete` events. Set it when the compaction targets a window other than the compacting model's own, e.g. switching to a model with a smaller context window: the compaction still runs on the current model, so the limit that motivated it would otherwise be lost. When absent, the events record the compacting model's own resolved limit. Attribution metadata only - it does not change how much the compaction removes.
   */
  tokenLimit?: number;
}

/** @experimental */
export interface SessionLimitPredictionPredictRequest {
  /**
   * Optional model identifier override. If omitted, the session's current model is used.
   */
  modelId?: string;
  clientType?: SessionLimitPredictionClientType;
}
/**
 * Identifies the target session.
 *
 * This interface was referenced by `_RpcSchemaRoot`'s JSON-Schema
 * via the `definition` "SessionFsSqliteExistsRequest".
 */
/** @experimental */
export interface SessionFsSqliteExistsRequest {
  /**
   * Target session identifier
   */
  sessionId: string;
}

/** Create typed server-scoped RPC methods (no session required). */
export function createServerRpc(connection: MessageConnection) {
    return {
        /**
         * Checks server responsiveness and returns protocol information.
         *
         * @param params Optional message to echo back to the caller.
         *
         * @returns Server liveness response, including the echoed message, current server timestamp, and protocol version.
         *
         * @experimental
         */
        ping: async (params: PingRequest): Promise<PingResult> =>
            connection.sendRequest("ping", params),
        /** @experimental */
        hooks: {
            /**
             * Discovers hook actions enabled under server-side discovery settings from user, repository, plugin, and managed-policy sources.
             *
             * @param params Optional project paths and host-exclusion behavior for server-scoped hook discovery.
             *
             * @returns Server-discovered hook actions and partial-load diagnostics from user, repository, plugin, and managed-policy sources. Concrete sessions may include additional session-specific hook sources.
             */
            discover: async (params: HooksDiscoverRequest): Promise<HooksDiscoverResult> =>
                connection.sendRequest("hooks.discover", params),
        },
        /** @experimental */
        models: {
            /**
             * Lists Copilot models available to the authenticated user.
             *
             * @param params Optional opaque account selection or compatibility GitHub token used to list models.
             *
             * @returns List of Copilot models available to the resolved user, including capabilities and billing metadata.
             */
            list: async (params: ModelsListRequest): Promise<ModelList> =>
                connection.sendRequest("models.list", params),
            /**
             * Returns the running runtime's complete catalog of well-known built-in model IDs without authentication or network access.
             *
             * @returns The running runtime's complete catalog of well-known built-in model IDs, including supported models and additional IDs with built-in metadata.
             */
            getBuiltInCatalog: async (): Promise<BuiltInModelCatalog> =>
                connection.sendRequest("models.getBuiltInCatalog", {}),
        },
        /** @experimental */
        tools: {
            /**
             * Lists built-in tools available for a model.
             *
             * @param params Optional model identifier whose tool overrides should be applied to the listing.
             *
             * @returns Built-in tools available for the requested model, with their parameters and instructions.
             */
            list: async (params: ToolsListRequest): Promise<ToolList> =>
                connection.sendRequest("tools.list", params),
        },
        /** @experimental */
        account: {
            /**
             * Gets Copilot quota usage for the current or opaquely selected authenticated user.
             *
             * @param params Optional opaque account selection or compatibility GitHub token used to look up quota.
             *
             * @returns Quota usage snapshots for the resolved user, keyed by quota type.
             */
            getQuota: async (params: AccountGetQuotaRequest): Promise<AccountGetQuotaResult> =>
                connection.sendRequest("account.getQuota", params),
            /**
             * Gets the currently active authentication credentials from the global auth manager.
             *
             * @returns Current authentication state
             */
            getCurrentAuth: async (): Promise<AccountGetCurrentAuthResult> =>
                connection.sendRequest("account.getCurrentAuth", {}),
            /**
             * Gets all authenticated users available for account switching.
             *
             * @returns List of all authenticated users
             */
            getAllUsers: async (): Promise<AccountGetAllUsersResult> =>
                connection.sendRequest("account.getAllUsers", {}),
            /**
             * Validates and stores authentication credentials. When login is omitted, resolves the authenticated user from the token before persistence.
             *
             * @param params Credentials to validate and store. Omit login to resolve the authenticated user from the token.
             *
             * @returns Result of a successful login; throws on failure
             */
            login: async (params: AccountLoginRequest): Promise<AccountLoginResult> =>
                connection.sendRequest("account.login", params),
            /**
             * Removes user authentication from keychain and persisted state.
             *
             * @param params User to log out
             *
             * @returns Logout result indicating if more users remain
             */
            logout: async (params: AccountLogoutRequest): Promise<AccountLogoutResult> =>
                connection.sendRequest("account.logout", params),
        },
        /** @experimental */
        secrets: {
            /**
             * Registers secret values for redaction in session logs and exports. The SDK calls this to inject dynamically generated secret values (e.g., OIDC tokens).
             *
             * @param params Secret values to add to the redaction filter.
             *
             * @returns Confirmation that the secret values were registered.
             */
            addFilterValues: async (params: SecretsAddFilterValuesRequest): Promise<SecretsAddFilterValuesResult> =>
                connection.sendRequest("secrets.addFilterValues", params),
        },
        /** @experimental */
        mcp: {
            /** @experimental */
            config: {
                /**
                 * Lists MCP servers from user configuration.
                 *
                 * @returns User-configured MCP servers, keyed by server name.
                 */
                list: async (): Promise<McpConfigList> =>
                    connection.sendRequest("mcp.config.list", {}),
                /**
                 * Adds an MCP server to user configuration.
                 *
                 * @param params MCP server name and configuration to add to user configuration.
                 */
                add: async (params: McpConfigAddRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.add", params),
                /**
                 * Updates an MCP server in user configuration.
                 *
                 * @param params MCP server name and replacement configuration to write to user configuration.
                 */
                update: async (params: McpConfigUpdateRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.update", params),
                /**
                 * Removes an MCP server from user configuration.
                 *
                 * @param params MCP server name to remove from user configuration.
                 */
                remove: async (params: McpConfigRemoveRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.remove", params),
                /**
                 * Enables MCP servers in user configuration for new sessions.
                 *
                 * @param params MCP server names to enable for new sessions.
                 */
                enable: async (params: McpConfigEnableRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.enable", params),
                /**
                 * Disables MCP servers in user configuration for new sessions.
                 *
                 * @param params MCP server names to disable for new sessions.
                 */
                disable: async (params: McpConfigDisableRequest): Promise<void> =>
                    connection.sendRequest("mcp.config.disable", params),
                /**
                 * Drops this runtime process's in-memory MCP server-definition cache so the next MCP config read observes disk.
                 */
                reload: async (): Promise<void> =>
                    connection.sendRequest("mcp.config.reload", {}),
            },
            /**
             * Discovers MCP servers from user, workspace, plugin, and builtin sources.
             *
             * @param params Optional working directory used as context for MCP server discovery.
             *
             * @returns MCP servers discovered from user, workspace, plugin, and built-in sources.
             */
            discover: async (params: McpDiscoverRequest): Promise<McpDiscoverResult> =>
                connection.sendRequest("mcp.discover", params),
            /**
             * Requests a side-effect-free MCP install plan from a catalog candidate handle or a caller-supplied card. This host-implemented server method is available through SDK/TUI hosts; standalone and C-ABI runtimes whose host does not implement server-method dispatch return JSON-RPC MethodNotFound. A runtime with planning available returns a normalised plan and opaque single-use plan handle; a runtime without it returns the typed planning-unavailable result. A completed plan reports resource identity, provenance, eligible transport choices, the user-scope target, required typed values and secret placeholders, the policy result, the configuration changes installing would make, and whether a reload would be needed. Planning never writes configuration, stores a secret, or reloads MCP servers, so abandoning a plan needs no call and leaves nothing behind.
             *
             * @param params A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
             *
             * @returns Outcome of an mcp.planInstall call: either a normalised plan, or one typed refusal. Nothing is written in either case.
             */
            planInstall: async (params: McpPlanInstallRequest): Promise<McpPlanInstallResult> =>
                connection.sendRequest("mcp.planInstall", params),
        },
        /** @experimental */
        extensions: {
            /**
             * Discovers user and enabled installed-plugin extensions from persisted Copilot home state, including enablement preferences. Launch-scoped additional plugins are not included.
             *
             * @returns Extensions discovered from persisted Copilot home state and their effective loading mode. Launch-scoped additional plugins are not included.
             */
            discover: async (): Promise<DiscoveredExtensions> =>
                connection.sendRequest("extensions.discover", {}),
            /**
             * Persistently enables extension IDs for future sessions. Active sessions are unchanged; use session.extensions.enable to update them.
             *
             * @param params Source-qualified extension identifiers to persistently enable for future sessions.
             */
            enable: async (params: DiscoveredExtensionsEnableRequest): Promise<void> =>
                connection.sendRequest("extensions.enable", params),
            /**
             * Persistently disables extension IDs for future sessions. Active sessions are unchanged; use session.extensions.disable to update them.
             *
             * @param params Source-qualified extension identifiers to persistently disable for future sessions.
             */
            disable: async (params: DiscoveredExtensionsDisableRequest): Promise<void> =>
                connection.sendRequest("extensions.disable", params),
        },
        /**
         * Registers the calling SDK client as the per-entrypoint extension launch provider. Call before creating any sessions. When omitted, the runtime uses its built-in extension launcher.
         *
         * @experimental
         */
        registerExtensionLaunchProvider: async (): Promise<void> =>
            connection.sendRequest("registerExtensionLaunchProvider", {}),
        /** @experimental */
        catalog: {
            /**
             * Requests a bounded catalog search. This host-implemented server method is available through SDK/TUI hosts; standalone and C-ABI runtimes whose host does not implement server-method dispatch return JSON-RPC MethodNotFound. A runtime with search available returns inert candidate summaries, each with an opaque single-use handle scoped to this runtime instance; a runtime without it returns the typed search-unavailable result. Public authorities may be searched anonymously, while an authority that requires credentials yields the typed authentication-required result. All returned text, URLs, and package metadata are untrusted external data and can never trigger instructions, tools, or installation. Read-only: nothing is installed, configured, or persisted.
             *
             * @param params A bounded catalog search. Both the query length and the result count are capped by the schema so a caller cannot request an unbounded scan.
             *
             * @returns Outcome of a catalog.search call: either bounded inert candidates, or one typed refusal. Never a partial success.
             */
            search: async (params: CatalogSearchRequest): Promise<CatalogSearchResult> =>
                connection.sendRequest("catalog.search", params),
        },
        /** @experimental */
        plugins: {
            /**
             * Lists plugins installed in user/global state.
             *
             * @returns Plugins installed in user/global state.
             */
            list: async (): Promise<PluginListResult> =>
                connection.sendRequest("plugins.list", {}),
            /**
             * Installs a plugin from a marketplace, GitHub repo, URL, or local path.
             *
             * @param params Plugin source and optional working directory for relative-path resolution.
             *
             * @returns Result of installing a plugin.
             */
            install: async (params: PluginsInstallRequest): Promise<PluginInstallResult> =>
                connection.sendRequest("plugins.install", params),
            /**
             * Uninstalls an installed plugin.
             *
             * @param params Name (or spec) of the plugin to uninstall.
             */
            uninstall: async (params: PluginsUninstallRequest): Promise<void> =>
                connection.sendRequest("plugins.uninstall", params),
            /**
             * Updates an installed plugin to its latest published version.
             *
             * @param params Name (or spec) of the plugin to update.
             *
             * @returns Result of updating a single plugin.
             */
            update: async (params: PluginsUpdateRequest): Promise<PluginUpdateResult> =>
                connection.sendRequest("plugins.update", params),
            /**
             * Updates every installed plugin to its latest published version.
             *
             * @returns Result of updating all installed plugins.
             */
            updateAll: async (): Promise<PluginUpdateAllResult> =>
                connection.sendRequest("plugins.updateAll", {}),
            /**
             * Enables installed plugins for new sessions.
             *
             * @param params Plugin names (or specs) to enable.
             */
            enable: async (params: PluginsEnableRequest): Promise<void> =>
                connection.sendRequest("plugins.enable", params),
            /**
             * Disables installed plugins for new sessions.
             *
             * @param params Plugin names (or specs) to disable.
             */
            disable: async (params: PluginsDisableRequest): Promise<void> =>
                connection.sendRequest("plugins.disable", params),
            /** @experimental */
            builtin: {
                /**
                 * Replaces this server's trusted built-in plugin directories while no sessions are active.
                 *
                 * @param params Trusted built-in plugin directories to use for this runtime process.
                 */
                set: async (params: PluginsBuiltinSetRequest): Promise<void> =>
                    connection.sendRequest("plugins.builtin.set", params),
            },
            /** @experimental */
            marketplaces: {
                /**
                 * Lists all registered marketplaces (defaults + user-added).
                 *
                 * @returns All registered marketplaces, including built-in defaults.
                 */
                list: async (): Promise<MarketplaceListResult> =>
                    connection.sendRequest("plugins.marketplaces.list", {}),
                /**
                 * Registers a new marketplace from a source (owner/repo, URL, or local path).
                 *
                 * @param params Marketplace source and optional working directory for relative-path resolution.
                 *
                 * @returns Result of registering a new marketplace.
                 */
                add: async (params: PluginsMarketplacesAddRequest): Promise<MarketplaceAddResult> =>
                    connection.sendRequest("plugins.marketplaces.add", params),
                /**
                 * Removes a previously-registered marketplace. When the marketplace has dependent plugins and `force` is not set, the marketplace is left intact and the result lists the dependents so the caller can decide whether to retry with `force=true`.
                 *
                 * @param params Name of the marketplace to remove and an optional force flag.
                 *
                 * @returns Outcome of the remove attempt, including dependent-plugin info when applicable.
                 */
                remove: async (params: PluginsMarketplacesRemoveRequest): Promise<MarketplaceRemoveResult> =>
                    connection.sendRequest("plugins.marketplaces.remove", params),
                /**
                 * Lists plugins advertised by a registered marketplace.
                 *
                 * @param params Name of the marketplace whose plugin catalog to fetch.
                 *
                 * @returns Plugins advertised by the marketplace.
                 */
                browse: async (params: PluginsMarketplacesBrowseRequest): Promise<MarketplaceBrowseResult> =>
                    connection.sendRequest("plugins.marketplaces.browse", params),
                /**
                 * Re-fetches one or all registered marketplace catalogs.
                 *
                 * @param params Optional marketplace name; omit to refresh all.
                 *
                 * @returns Result of refreshing one or more marketplace catalogs.
                 */
                refresh: async (params: PluginsMarketplacesRefreshRequest): Promise<MarketplaceRefreshResult> =>
                    connection.sendRequest("plugins.marketplaces.refresh", params),
            },
        },
        /** @experimental */
        skills: {
            /** @experimental */
            config: {
                /**
                 * Replaces the global list of disabled skills.
                 *
                 * @param params Skill names to mark as disabled in global configuration, replacing any previous list.
                 */
                setDisabledSkills: async (params: SkillsConfigSetDisabledSkillsRequest): Promise<void> =>
                    connection.sendRequest("skills.config.setDisabledSkills", params),
                /**
                 * Atomically adds or removes one skill from the disabled list.
                 *
                 * @param params Adds or removes a single skill from the global disabled list, leaving every other entry untouched.
                 */
                setSkillDisabled: async (params: SkillsConfigSetSkillDisabledRequest): Promise<void> =>
                    connection.sendRequest("skills.config.setSkillDisabled", params),
            },
            /**
             * Discovers skills across global and project sources.
             *
             * @param params Optional project paths and additional skill directories to include in discovery.
             *
             * @returns Skills discovered across global and project sources.
             */
            discover: async (params: SkillsDiscoverRequest): Promise<ServerSkillList> =>
                connection.sendRequest("skills.discover", params),
            /**
             * Returns the canonical directories where a client may create skills that the runtime will recognize, including ones that do not exist yet. Project directories become active once created.
             *
             * @param params Optional project paths to enumerate.
             *
             * @returns Canonical locations where skills can be created so the runtime will recognize them.
             */
            getDiscoveryPaths: async (params: SkillsGetDiscoveryPathsRequest): Promise<SkillDiscoveryPathList> =>
                connection.sendRequest("skills.getDiscoveryPaths", params),
        },
        /** @experimental */
        agents: {
            /**
             * Discovers custom agents across user, project, plugin, and remote sources.
             *
             * @param params Optional project paths to include in agent discovery.
             *
             * @returns Agents discovered across user, project, plugin, and remote sources.
             */
            discover: async (params: AgentsDiscoverRequest): Promise<ServerAgentList> =>
                connection.sendRequest("agents.discover", params),
            /**
             * Returns the canonical directories where a client may create custom agents that the runtime will recognize, including ones that do not exist yet. Project directories become active once created.
             *
             * @param params Optional project paths to include when enumerating agent discovery directories.
             *
             * @returns Canonical locations where custom agents can be created so the runtime will recognize them.
             */
            getDiscoveryPaths: async (params: AgentsGetDiscoveryPathsRequest): Promise<AgentDiscoveryPathList> =>
                connection.sendRequest("agents.getDiscoveryPaths", params),
        },
        /** @experimental */
        instructions: {
            /**
             * Discovers instruction sources across user, repository, and plugin sources.
             *
             * @param params Optional project paths to include in instruction discovery.
             *
             * @returns Instruction sources discovered across user, repository, and plugin sources.
             */
            discover: async (params: InstructionsDiscoverRequest): Promise<ServerInstructionSourceList> =>
                connection.sendRequest("instructions.discover", params),
            /**
             * Returns the canonical files and directories where a client may create custom instructions that the runtime will recognize, including ones that do not exist yet. Repository targets become active once created.
             *
             * @param params Optional project paths to include when enumerating instruction discovery targets.
             *
             * @returns Canonical files and directories where custom instructions can be created so the runtime will recognize them.
             */
            getDiscoveryPaths: async (params: InstructionsGetDiscoveryPathsRequest): Promise<InstructionDiscoveryPathList> =>
                connection.sendRequest("instructions.getDiscoveryPaths", params),
        },
        /** @experimental */
        commands: {
            /**
             * Lists the well-known built-in slash commands that work as the first message in a new session (e.g. /plan, /env), without requiring an active session. Commands that depend on session state, authentication, or a synced session are omitted.
             *
             * @returns Slash commands available in the session, after applying any include/exclude filters.
             */
            list: async (): Promise<CommandList> =>
                connection.sendRequest("commands.list", {}),
        },
        /** @experimental */
        user: {
            /** @experimental */
            settings: {
                /**
                 * Drops this runtime process's in-memory user settings cache so the next settings read observes disk.
                 */
                reload: async (): Promise<void> =>
                    connection.sendRequest("user.settings.reload", {}),
                /**
                 * Lists every known user setting (settings.json overlaid with the legacy config.json, config.json wins), each with its effective value, its default, and whether it is at the default — so settings the user has never set still appear with their default value. Does not include repository- or enterprise-managed overrides that the runtime layers on top at session time.
                 *
                 * @returns Per-key metadata for every known user setting (settings.json overlaid with the legacy config.json, config.json wins), including settings left at their default. Excludes repository- and enterprise-managed overrides.
                 */
                get: async (): Promise<UserSettingsGetResult> =>
                    connection.sendRequest("user.settings.get", {}),
                /**
                 * Writes one or more user settings to settings.json, replacing each provided top-level key. A key whose value is null is removed. Returns the keys whose new value is shadowed by a legacy config.json entry (config.json wins on read), which the runtime leaves in place — such writes do not take effect until the legacy value is removed.
                 *
                 * @param params Partial user settings to write to settings.json. Each top-level key is written individually, replacing the existing value; a key whose value is null is removed.
                 *
                 * @returns Outcome of writing user settings.
                 */
                set: async (params: UserSettingsSetRequest): Promise<UserSettingsSetResult> =>
                    connection.sendRequest("user.settings.set", params),
            },
        },
        /** @experimental */
        managedSettings: {
            /**
             * Discovers device-managed settings from production MDM and managed-file sources, validates them against the runtime-owned managed-settings schema, and returns the canonical JSON without requiring a session.
             *
             * @returns Validated device-managed settings discovered before a session exists.
             */
            read: async (): Promise<ManagedSettingsReadResult> =>
                connection.sendRequest("managedSettings.read", {}),
        },
        /** @experimental */
        runtime: {
            /**
             * Gracefully shuts down an SDK-owned runtime. The response is sent only after cleanup completes; callers may then terminate the owned runtime process.
             */
            shutdown: async (): Promise<void> =>
                connection.sendRequest("runtime.shutdown", {}),
        },
        /** @experimental */
        sessionFs: {
            /**
             * Registers an SDK client as the session filesystem provider.
             *
             * @param params Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
             *
             * @returns Indicates whether the calling client was registered as the session filesystem provider.
             */
            setProvider: async (params: SessionFsSetProviderRequest): Promise<SessionFsSetProviderResult> =>
                connection.sendRequest("sessionFs.setProvider", params),
        },
        /** @experimental */
        llmInference: {
            /**
             * Registers an SDK client as the LLM inference callback provider.
             *
             * @returns Indicates whether the calling client was registered as the LLM inference provider.
             */
            setProvider: async (): Promise<LlmInferenceSetProviderResult> =>
                connection.sendRequest("llmInference.setProvider", {}),
            /**
             * Delivers the response head (status + headers) for an in-flight request, correlated by the requestId the runtime supplied in httpRequestStart. Must be called exactly once per request before any httpResponseChunk frames.
             *
             * @param params Response head.
             *
             * @returns Whether the start frame was accepted.
             */
            httpResponseStart: async (params: LlmInferenceHttpResponseStartRequest): Promise<LlmInferenceHttpResponseStartResult> =>
                connection.sendRequest("llmInference.httpResponseStart", params),
            /**
             * Delivers a body byte range (or a terminal transport error) for an in-flight response, correlated by requestId. Set `end` true on the last chunk. When `error` is set the response terminates with a transport-level failure and the runtime raises an APIConnectionError.
             *
             * @param params A response body chunk or terminal error.
             *
             * @returns Whether the chunk was accepted.
             */
            httpResponseChunk: async (params: LlmInferenceHttpResponseChunkRequest): Promise<LlmInferenceHttpResponseChunkResult> =>
                connection.sendRequest("llmInference.httpResponseChunk", params),
        },
        /** @experimental */
        sessions: {
            /**
             * Creates or resumes a local session and returns the opened session ID.
             *
             * @param params Open a session by creating, resuming, attaching, connecting to a remote, or handing off.
             *
             * @returns Result of opening a session.
             */
            open: async (params: SessionOpenParams): Promise<SessionOpenResult> =>
                connection.sendRequest("sessions.open", params),
            /**
             * Creates a new session by forking persisted history from an existing session.
             *
             * @param params Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
             *
             * @returns Identifier and optional friendly name assigned to the newly forked session.
             */
            fork: async (params: SessionsForkRequest): Promise<SessionsForkResult> =>
                connection.sendRequest("sessions.fork", params),
            /**
             * Connects to an existing remote session and exposes it as an SDK session.
             *
             * @param params Remote session connection parameters.
             *
             * @returns Remote session connection result.
             */
            connect: async (params: ConnectRemoteSessionParams): Promise<RemoteSessionConnectionResult> =>
                connection.sendRequest("sessions.connect", params),
            /**
             * Lists sessions, optionally filtered by source and working-directory context. Returned entries are discriminated by `isRemote`: local entries carry only the lightweight `LocalSessionMetadataValue` shape; remote entries carry the full `RemoteSessionMetadataValue` shape (repository, PR number, taskType, etc.).
             *
             * @param params Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
             *
             * @returns Sessions matching the filter, ordered most-recently-modified first.
             */
            list: async (params: SessionsListRequest): Promise<SessionList> =>
                connection.sendRequest("sessions.list", params),
            /**
             * Reads a page of durable events directly from a local session's persisted journal without creating, resuming, or activating the session. The initial backward read uses a bounded tail scan for fast first paint; cursor continuations preserve the session event-log paging semantics. Persisted events may omit payloads that are reconstructed only for an active session.
             *
             * @param params Pagination options for reading an inactive or active local session's persisted event journal.
             *
             * @returns Batch of session events returned by a read, with cursor and continuation metadata.
             */
            readPersistedEvents: async (params: SessionsReadPersistedEventsRequest): Promise<EventsReadResult> =>
                connection.sendRequest("sessions.readPersistedEvents", params),
            /**
             * Finds the local session bound to a GitHub task ID, if any.
             *
             * @param params GitHub task ID to look up.
             *
             * @returns ID of the local session bound to the given GitHub task, or omitted when none.
             */
            findByTaskId: async (params: SessionsFindByTaskIDRequest): Promise<SessionsFindByTaskIDResult> =>
                connection.sendRequest("sessions.findByTaskId", params),
            /**
             * Resolves a UUID prefix to a unique session ID, if exactly one session matches.
             *
             * @param params UUID prefix to resolve to a unique session ID.
             *
             * @returns Session ID matching the prefix, omitted when no unique match exists.
             */
            findByPrefix: async (params: SessionsFindByPrefixRequest): Promise<SessionsFindByPrefixResult> =>
                connection.sendRequest("sessions.findByPrefix", params),
            /**
             * Returns the most-relevant prior session for a given working-directory context.
             *
             * @param params Optional working-directory context used to score session relevance.
             *
             * @returns Most-relevant session ID for the supplied context, or omitted when no sessions exist.
             */
            getLastForContext: async (params: SessionsGetLastForContextRequest): Promise<SessionsGetLastForContextResult> =>
                connection.sendRequest("sessions.getLastForContext", params),
            /**
             * Returns the on-disk byte size of each session's workspace directory.
             *
             * @returns Map of sessionId -> on-disk size in bytes for each session's workspace directory.
             */
            getSizes: async (): Promise<SessionSizes> =>
                connection.sendRequest("sessions.getSizes", {}),
            /**
             * Returns the subset of the supplied session IDs that are currently held by another running process.
             *
             * @param params Session IDs to test for live in-use locks.
             *
             * @returns Session IDs from the input set that are currently in use by another process.
             */
            checkInUse: async (params: SessionsCheckInUseRequest): Promise<SessionsCheckInUseResult> =>
                connection.sendRequest("sessions.checkInUse", params),
            /**
             * Closes a session: emits shutdown, flushes pending events, releases the in-use lock, and disposes the active session.
             *
             * @param params Session ID to close.
             *
             * @returns Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
             */
            close: async (params: SessionsCloseRequest): Promise<SessionsCloseResult> =>
                connection.sendRequest("sessions.close", params),
            /**
             * Closes, deactivates, and deletes a set of sessions, returning the bytes freed per session.
             *
             * @param params Session IDs to close, deactivate, and delete from disk.
             *
             * @returns Map of sessionId -> bytes freed by removing the session's workspace directory.
             */
            bulkDelete: async (params: SessionsBulkDeleteRequest): Promise<SessionBulkDeleteResult> =>
                connection.sendRequest("sessions.bulkDelete", params),
            /**
             * Deletes sessions older than the given threshold, with optional dry-run and exclusion list.
             *
             * @param params Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
             *
             * @returns Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
             */
            pruneOld: async (params: SessionsPruneOldRequest): Promise<SessionPruneResult> =>
                connection.sendRequest("sessions.pruneOld", params),
            /**
             * Flushes a session's pending events to disk.
             *
             * @param params Session ID whose pending events should be flushed to disk.
             *
             * @returns Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
             */
            save: async (params: SessionsSaveRequest): Promise<SessionsSaveResult> =>
                connection.sendRequest("sessions.save", params),
            /**
             * Releases the in-use lock held by this process for a session.
             *
             * @param params Session ID whose in-use lock should be released.
             *
             * @returns Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
             */
            releaseLock: async (params: SessionsReleaseLockRequest): Promise<SessionsReleaseLockResult> =>
                connection.sendRequest("sessions.releaseLock", params),
            /**
             * Backfills missing summary and context fields on the supplied session metadata records.
             *
             * @param params Session metadata records to enrich with summary and context information.
             *
             * @returns The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
             */
            enrichMetadata: async (params: SessionsEnrichMetadataRequest): Promise<SessionEnrichMetadataResult> =>
                connection.sendRequest("sessions.enrichMetadata", params),
            /**
             * Reloads user, plugin, and (optionally) repo hooks on the active session.
             *
             * @param params Active session ID and an optional flag for deferring repo-level hooks until folder trust.
             *
             * @returns Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
             */
            reloadPluginHooks: async (params: SessionsReloadPluginHooksRequest): Promise<SessionsReloadPluginHooksResult> =>
                connection.sendRequest("sessions.reloadPluginHooks", params),
            /**
             * Loads previously-deferred repo-level hooks on the active session, returning queued startup prompts.
             *
             * @param params Active session ID whose deferred repo-level hooks should be loaded.
             *
             * @returns Queued repo-level startup prompts and the total hook command count after loading.
             */
            loadDeferredRepoHooks: async (params: SessionsLoadDeferredRepoHooksRequest): Promise<SessionLoadDeferredRepoHooksResult> =>
                connection.sendRequest("sessions.loadDeferredRepoHooks", params),
            /**
             * Replaces the manager-wide additional plugins registered with the session manager.
             *
             * @param params Manager-wide additional plugins to register; replaces any previously-configured set.
             *
             * @returns Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
             */
            setAdditionalPlugins: async (params: SessionsSetAdditionalPluginsRequest): Promise<SessionsSetAdditionalPluginsResult> =>
                connection.sendRequest("sessions.setAdditionalPlugins", params),
            /**
             * Attaches the runtime-managed remote-control singleton to a session, awaiting initial setup. If remote control is already attached to a different session, the singleton is transferred (preserving the underlying Mission Control connection). Returns the final status.
             *
             * @param params Parameters for attaching the remote-control singleton to a session.
             *
             * @returns Wrapper for the singleton's current status.
             */
            startRemoteControl: async (params: SessionsStartRemoteControlRequest): Promise<RemoteControlStatusResult> =>
                connection.sendRequest("sessions.startRemoteControl", params),
            /**
             * Atomically rebinds the remote-control singleton to a different session, preserving the underlying Mission Control connection. When `expectedFromSessionId` is provided and does not match the singleton's current `attachedSessionId`, the transfer is rejected with `transferred: false` and the current status is returned unchanged.
             *
             * @param params Parameters for atomically rebinding the remote-control singleton.
             *
             * @returns Outcome of a transferRemoteControl call.
             */
            transferRemoteControl: async (params: SessionsTransferRemoteControlRequest): Promise<RemoteControlTransferResult> =>
                connection.sendRequest("sessions.transferRemoteControl", params),
            /**
             * Patches the steering state of the active remote-control singleton. When remote control is off, this is a no-op and the off status is returned. Today only `enabled: true` is actionable on the underlying exporter; passing `false` is reserved for future use.
             *
             * @param params Patch for the singleton's steering state.
             *
             * @returns Wrapper for the singleton's current status.
             */
            setRemoteControlSteering: async (params: SessionsSetRemoteControlSteeringRequest): Promise<RemoteControlStatusResult> =>
                connection.sendRequest("sessions.setRemoteControlSteering", params),
            /**
             * Stops the remote-control singleton. When `expectedSessionId` is provided and does not match the singleton's current `attachedSessionId`, the stop is rejected with `stopped: false` and the current status is returned unchanged (unless `force` is set, in which case the singleton is unconditionally torn down).
             *
             * @param params Parameters for stopping the remote-control singleton.
             *
             * @returns Outcome of a stopRemoteControl call.
             */
            stopRemoteControl: async (params: SessionsStopRemoteControlRequest): Promise<RemoteControlStopResult> =>
                connection.sendRequest("sessions.stopRemoteControl", params),
            /**
             * Returns the current state of the remote-control singleton, including the attached session id and frontend URL when active.
             *
             * @returns Wrapper for the singleton's current status.
             */
            getRemoteControlStatus: async (): Promise<RemoteControlStatusResult> =>
                connection.sendRequest("sessions.getRemoteControlStatus", {}),
        },
        /** @experimental */
        agentRegistry: {
            /**
             * Spawns a managed-server child with the supplied configuration and returns a discriminated-union result. The caller (typically the CLI controller) is responsible for attaching to the spawned child and sending any follow-up prompt. When the controller-local spawn gate is closed the server returns JSON-RPC MethodNotFound.
             *
             * @param params Inputs to spawn a managed-server child via the controller's spawn delegate.
             *
             * @returns Outcome of an agentRegistry.spawn call.
             */
            spawn: async (params: AgentRegistrySpawnRequest): Promise<AgentRegistrySpawnResult> =>
                connection.sendRequest("agentRegistry.spawn", params),
        },
    };
}

/**
 * Create typed server-scoped RPC methods that are part of the SDK's internal
 * surface (e.g. handshake helpers). Not exported on the public client API.
 * @internal
 */
export function createInternalServerRpc(connection: MessageConnection) {
    return {
        /**
         * Performs the SDK server connection handshake and validates the optional connection token. Marked internal because this is JSON-RPC transport plumbing invoked automatically by an SDK client's own `connect()` wrapper, not a user-facing method. Stays internal as long as the SDK client owns the handshake; would only become public if the SDK ever exposed the raw schema surface to consumers without a connection wrapper.
         *
         * @param params Connection-level opt-ins for the `server.connect` handshake. Transport authentication is consumed by the native protocol boundary before dispatch.
         *
         * @returns Handshake result reporting the server's protocol version and package version on success.
         *
         * @experimental
         */
        connect: async (params: ConnectRequest): Promise<ConnectResult> =>
            connection.sendRequest("connect", params),
        /** @experimental */
        sessions: {
            /**
             * Reads lightweight persisted metadata for one local session without opening it.
             *
             * @param params Session ID whose persisted metadata should be read.
             *
             * @returns Persisted local session metadata when the session exists.
             */
            getMetadata: async (params: SessionsGetMetadataRequest): Promise<SessionsGetMetadataResult> =>
                connection.sendRequest("sessions.getMetadata", params),
            /**
             * Lists recent local session IDs that contain user-visible history, omitting housekeeping-only sessions.
             *
             * @param params Limit for non-empty local session IDs.
             *
             * @returns Recent local session IDs that contain user-visible history.
             */
            listNonEmptySessionIds: async (params: SessionsListNonEmptySessionIdsRequest): Promise<SessionsListNonEmptySessionIdsResult> =>
                connection.sendRequest("sessions.listNonEmptySessionIds", params),
            /**
             * Computes the absolute path to a session's persisted events.jsonl file. Internal: filesystem paths are only meaningful in-process (CLI and runtime share a filesystem). Currently used by the CLI's contribution-graph feature to read historical events directly. Remote SDK consumers must not depend on this; a proper event-query API would replace it if the contribution graph ever needed to work over the wire.
             *
             * @param params Session ID whose event-log file path to compute.
             *
             * @returns Absolute path to the session's events.jsonl file on disk.
             */
            getEventFilePath: async (params: SessionsGetEventFilePathRequest): Promise<SessionsGetEventFilePathResult> =>
                connection.sendRequest("sessions.getEventFilePath", params),
            /**
             * Returns a session's persisted remote-steerable flag, if any has been recorded. Internal: this is CLI-specific book-keeping used by `--continue` / `--resume` to inherit the prior session's remote-steerable preference. SDK consumers that want similar behavior should manage their own persistence around start/stop calls rather than relying on this runtime-side flag.
             *
             * @param params Session ID to look up the persisted remote-steerable flag for.
             *
             * @returns The session's persisted remote-steerable flag, or omitted when no value has been persisted.
             */
            getPersistedRemoteSteerable: async (params: SessionsGetPersistedRemoteSteerableRequest): Promise<SessionsGetPersistedRemoteSteerableResult> =>
                connection.sendRequest("sessions.getPersistedRemoteSteerable", params),
            /**
             * Deletes one local session from disk after running the same lifecycle hooks as the session manager.
             *
             * @param params Session ID to delete from disk.
             */
            delete: async (params: SessionsDeleteRequest): Promise<void> =>
                connection.sendRequest("sessions.delete", params),
            /**
             * Gets the dynamic-context board entry count associated with a session, when available. Internal: this exists solely so CLI telemetry events (`rem_spawn_gate`, `rem_consolidation_complete`) can pair START / END board counts around the detached rem-agent spawn. "Dynamic context board" is a runtime-internal concept that is not part of the public SDK contract; the long-term plan is to relocate the telemetry emission into the runtime so this method can be deleted entirely.
             *
             * @param params Session ID whose board entry count should be returned.
             *
             * @returns Dynamic-context board entry count, when available.
             */
            getBoardEntryCount: async (params: SessionsGetBoardEntryCountRequest): Promise<SessionsGetBoardEntryCountResult> =>
                connection.sendRequest("sessions.getBoardEntryCount", params),
            /**
             * Registers extension-provided tools on the given session, gated by an optional `enabled` callback. Returns an opaque unsubscribe function the caller must invoke to deregister the tools when the extension is torn down. Marked internal because `loader`, `enabled`, and the returned `unsubscribe` are in-process handles that cannot cross the JSON-RPC boundary. Disappears once extension discovery / launch / tool registration are owned by the runtime: SDK consumers will pass pure config (search paths, disabled ids) via `SessionOptions` and the runtime will resolve, launch, register, and tear down extensions itself.
             *
             * @param params Params to attach an extension loader's tools to a session.
             *
             * @returns Handle for releasing the extension tool registration.
             */
            registerExtensionToolsOnSession: async (params: RegisterExtensionToolsParams): Promise<RegisterExtensionToolsResult> =>
                connection.sendRequest("sessions.registerExtensionToolsOnSession", params),
            /**
             * Attaches (or detaches) an in-process ExtensionController delegate for the given session, used by shared-API surfaces that need to query or modify the session's extension state. Pass `controller: undefined` to detach. Marked internal because the controller is an in-process object that cannot cross the JSON-RPC boundary. Disappears alongside `registerExtensionToolsOnSession`: once the runtime owns extension management, the public surface exposes list/enable/disable/reload as dedicated RPCs served by the runtime.
             *
             * @param params Params to attach or detach an in-process ExtensionController delegate.
             */
            configureSessionExtensions: async (params: ConfigureSessionExtensionsParams): Promise<void> =>
                connection.sendRequest("sessions.configureSessionExtensions", params),
        },
    };
}

/** Create typed session-scoped RPC methods. */
export function createSessionRpc(connection: MessageConnection, sessionId: string) {
    return {
        /**
         * Suspends the session while preserving persisted state for later resume.
         *
         * @experimental
         */
        suspend: async (): Promise<void> =>
            connection.sendRequest("session.suspend", { sessionId }),
        /**
         * Sends a user message to the session and returns its message ID.
         *
         * @param params Parameters for sending a user message to the session
         *
         * @returns Result of sending a user message
         *
         * @experimental
         */
        send: async (params: SendRequest): Promise<SendResult> =>
            connection.sendRequest("session.send", { sessionId, ...params }),
        /**
         * Sends zero or more user messages to the session in a single turn and returns their message IDs. All provided messages are appended to the conversation in order, then exactly one agent turn runs over the resulting history. When the list is empty, one turn runs over the existing history with no new user message. Remote-backed (Mission Control) sessions do not support this method and will return an error.
         *
         * @param params Parameters for sending zero or more user messages to the session in a single turn. Remote-backed (Mission Control) sessions do not support this method and will return an error.
         *
         * @returns Result of sending zero or more user messages
         *
         * @experimental
         */
        sendMessages: async (params: SendMessagesRequest): Promise<SendMessagesResult> =>
            connection.sendRequest("session.sendMessages", { sessionId, ...params }),
        /** @experimental */
        sandbox: {
            /**
             * Returns whether managed policy requires sandbox enforcement and whether an enforcement failure has permanently blocked the session.
             *
             * @returns Managed sandbox enforcement state for a session.
             */
            getEnforcementStatus: async (): Promise<SandboxEnforcementStatus> =>
                connection.sendRequest("session.sandbox.getEnforcementStatus", { sessionId }),
        },
        /**
         * Aborts the current agent turn.
         *
         * @param params Parameters for aborting the current turn
         *
         * @returns Result of aborting the current turn
         *
         * @experimental
         */
        abort: async (params: AbortRequest): Promise<AbortResult> =>
            connection.sendRequest("session.abort", { sessionId, ...params }),
        /**
         * Interrupts the current main agent turn while leaving running background work (subagents, sidekicks, and promoted attached shells) alive. No-op when the main loop is not processing.
         *
         * @param params Parameters for interrupting the main agent turn.
         *
         * @returns Result of interrupting the main agent turn.
         *
         * @experimental
         */
        interruptMainTurn: async (params: InterruptMainTurnRequest): Promise<InterruptMainTurnResult> =>
            connection.sendRequest("session.interruptMainTurn", { sessionId, ...params }),
        /**
         * Cancels every running background agent (task-registry subagents plus sidekick agents) without interrupting the main agent loop. Promoted attached shells are left running.
         *
         * @returns The number of running background agents (task-registry agents) that were cancelled.
         *
         * @experimental
         */
        cancelAllBackgroundAgents: async (): Promise<SessionCancelAllBackgroundAgentsResult> =>
            connection.sendRequest("session.cancelAllBackgroundAgents", { sessionId }),
        /**
         * Shuts down the session and persists its final state. Awaits any deferred sessionEnd hooks before resolving so user-supplied hook scripts complete before the runtime tears down.
         *
         * @param params Parameters for shutting down the session
         *
         * @experimental
         */
        shutdown: async (params: ShutdownRequest): Promise<void> =>
            connection.sendRequest("session.shutdown", { sessionId, ...params }),
        /** @experimental */
        gitHubAuth: {
            /**
             * Gets authentication status and account metadata for the session.
             *
             * @returns Authentication status and account metadata for the session.
             */
            getStatus: async (): Promise<SessionAuthStatus> =>
                connection.sendRequest("session.gitHubAuth.getStatus", { sessionId }),
            /**
             * Updates the session's auth credentials used for outbound model and API requests.
             *
             * @param params New auth credentials to install on the session. Omit to leave credentials unchanged.
             *
             * @returns Indicates whether the credential update succeeded.
             */
            setCredentials: async (params: SessionSetCredentialsParams): Promise<SessionSetCredentialsResult> =>
                connection.sendRequest("session.gitHubAuth.setCredentials", { sessionId, ...params }),
        },
        /** @experimental */
        debug: {
            /**
             * Collects a redacted session debug log bundle into a local archive or staging directory. The runtime includes session-owned logs by default and accepts caller-provided diagnostic entries so host applications can add their own files without changing this API shape.
             *
             * @param params Options for collecting a redacted session debug bundle.
             *
             * @returns Result of collecting a redacted debug bundle.
             */
            collectLogs: async (params: DebugCollectLogsRequest): Promise<DebugCollectLogsResult> =>
                connection.sendRequest("session.debug.collectLogs", { sessionId, ...params }),
        },
        /** @experimental */
        canvas: {
            /**
             * Lists canvases declared for the session.
             *
             * @returns Declared canvases available in this session.
             */
            list: async (): Promise<CanvasList> =>
                connection.sendRequest("session.canvas.list", { sessionId }),
            /**
             * Lists currently open canvas instances for the live session.
             *
             * @returns Live open-canvas snapshot.
             */
            listOpen: async (): Promise<CanvasListOpenResult> =>
                connection.sendRequest("session.canvas.listOpen", { sessionId }),
            /**
             * Opens or focuses a canvas instance.
             *
             * @param params Canvas open parameters.
             *
             * @returns Open canvas instance snapshot.
             */
            open: async (params: CanvasOpenRequest): Promise<OpenCanvasInstance> =>
                connection.sendRequest("session.canvas.open", { sessionId, ...params }),
            /**
             * Closes an open canvas instance.
             *
             * @param params Canvas close parameters.
             */
            close: async (params: CanvasCloseRequest): Promise<void> =>
                connection.sendRequest("session.canvas.close", { sessionId, ...params }),
            /** @experimental */
            action: {
                /**
                 * Invokes an action on an open canvas instance.
                 *
                 * @param params Canvas action invocation parameters.
                 *
                 * @returns Canvas action invocation result.
                 */
                invoke: async (params: CanvasActionInvokeRequest): Promise<CanvasActionInvokeResult> =>
                    connection.sendRequest("session.canvas.action.invoke", { sessionId, ...params }),
            },
        },
        /** @experimental */
        factory: {
            /**
             * Runs a registered factory by name at the top level.
             *
             * @param params Parameters for invoking a registered factory.
             *
             * @returns Complete current or terminal factory run envelope.
             */
            run: async (params: FactoryRunRequest): Promise<FactoryRunResult> =>
                connection.sendRequest("session.factory.run", { sessionId, ...params }),
            /**
             * Resumes a factory run using its persisted name, arguments, journal, and accounting.
             *
             * @param params Parameters for resuming a factory run from its persisted identity.
             *
             * @returns Resolved persisted factory identity and resumed run envelope.
             */
            resume: async (params: FactoryResumeRequest): Promise<FactoryResumeResult> =>
                connection.sendRequest("session.factory.resume", { sessionId, ...params }),
            /**
             * Gets the current or settled envelope for a factory run.
             *
             * @param params Parameters for retrieving a factory run.
             *
             * @returns Complete current or terminal factory run envelope.
             */
            getRun: async (params: FactoryGetRunRequest): Promise<FactoryRunResult> =>
                connection.sendRequest("session.factory.getRun", { sessionId, ...params }),
            /**
             * Lists durable factory runs for this session in creation order.
             *
             * @param params Parameters for paging factory runs.
             *
             * @returns A page of factory runs in durable creation order.
             */
            listRuns: async (params: FactoryListRunsRequest): Promise<FactoryListRunsResult> =>
                connection.sendRequest("session.factory.listRuns", { sessionId, ...params }),
            /**
             * Gets durable and live observability detail for one factory run.
             *
             * @param params Parameters for retrieving a factory run.
             *
             * @returns Full factory run observability detail.
             */
            getRunDetail: async (params: FactoryGetRunRequest): Promise<FactoryRunDetail> =>
                connection.sendRequest("session.factory.getRunDetail", { sessionId, ...params }),
            /**
             * Pages durable progress for one factory run.
             *
             * @param params Parameters for paging factory progress.
             *
             * @returns A bidirectional page of factory progress.
             */
            getRunProgress: async (params: FactoryGetRunProgressRequest): Promise<FactoryProgressPage> =>
                connection.sendRequest("session.factory.getRunProgress", { sessionId, ...params }),
            /**
             * Requests cancellation of a factory run and returns its run envelope.
             *
             * @param params Parameters for cancelling a factory run.
             *
             * @returns Complete current or terminal factory run envelope.
             */
            cancel: async (params: FactoryCancelRequest): Promise<FactoryRunResult> =>
                connection.sendRequest("session.factory.cancel", { sessionId, ...params }),
            /**
             * Records a batch of ordered factory progress lines.
             *
             * @param params Parameters for recording factory progress.
             *
             * @returns Acknowledgement that a factory request was accepted.
             */
            log: async (params: FactoryLogRequest): Promise<FactoryAckResult> =>
                connection.sendRequest("session.factory.log", { sessionId, ...params }),
            /**
             * Runs one factory-scoped subagent and returns its result.
             *
             * @param params Parameters for one factory-scoped subagent call.
             *
             * @returns Result of one factory-scoped subagent call.
             */
            agent: async (params: FactoryAgentRequest): Promise<FactoryAgentResult> =>
                connection.sendRequest("session.factory.agent", { sessionId, ...params }),
            /** @experimental */
            journal: {
                /**
                 * Reads a memoized factory journal entry.
                 *
                 * @param params Parameters for reading a factory journal entry.
                 *
                 * @returns Result of reading a factory journal entry.
                 */
                get: async (params: FactoryJournalGetRequest): Promise<FactoryJournalGetResult> =>
                    connection.sendRequest("session.factory.journal.get", { sessionId, ...params }),
                /**
                 * Stores a memoized factory journal entry.
                 *
                 * @param params Parameters for storing a factory journal entry.
                 *
                 * @returns Acknowledgement that a factory request was accepted.
                 */
                put: async (params: FactoryJournalPutRequest): Promise<FactoryAckResult> =>
                    connection.sendRequest("session.factory.journal.put", { sessionId, ...params }),
            },
        },
        /** @experimental */
        model: {
            /**
             * Gets the currently selected model for the session.
             *
             * @returns The currently selected model, reasoning effort, and context tier for the session. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
             */
            getCurrent: async (): Promise<CurrentModel> =>
                connection.sendRequest("session.model.getCurrent", { sessionId }),
            /**
             * Switches the session to a model and optional reasoning configuration.
             *
             * @param params Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
             *
             * @returns The model identifier active on the session after the switch.
             */
            switchTo: async (params: ModelSwitchToRequest): Promise<ModelSwitchToResult> =>
                connection.sendRequest("session.model.switchTo", { sessionId, ...params }),
            /**
             * Updates the session's reasoning effort without changing the selected model.
             *
             * @param params Reasoning effort level to apply to the currently selected model.
             *
             * @returns Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
             */
            setReasoningEffort: async (params: ModelSetReasoningEffortRequest): Promise<ModelSetReasoningEffortResult> =>
                connection.sendRequest("session.model.setReasoningEffort", { sessionId, ...params }),
            /**
             * Lists models available to this session using its own auth and integration context. Connected hosts (CLI TUI, GitHub App) should call this through the session client so remote sessions return the remote CLI's available models rather than the caller's.
             *
             * @param params Optional listing options.
             *
             * @returns The list of models available to this session.
             */
            list: async (params?: SessionModelListRequest): Promise<SessionModelList> =>
                connection.sendRequest("session.model.list", { sessionId, ...params }),
        },
        /** @experimental */
        mode: {
            /**
             * Gets the current agent interaction mode.
             *
             * @returns The session mode the agent is operating in
             */
            get: async (): Promise<SessionMode> =>
                connection.sendRequest("session.mode.get", { sessionId }),
            /**
             * Sets the current agent interaction mode.
             *
             * @param params Agent interaction mode to apply to the session.
             *
             * @returns Outcome of a session mode change, including any model switch it triggered and follow-up the host must perform.
             */
            set: async (params: ModeSetRequest): Promise<ModeSetResult> =>
                connection.sendRequest("session.mode.set", { sessionId, ...params }),
        },
        /** @experimental */
        name: {
            /**
             * Gets the session's friendly name.
             *
             * @returns The session's friendly name, or null when not yet set.
             */
            get: async (): Promise<NameGetResult> =>
                connection.sendRequest("session.name.get", { sessionId }),
            /**
             * Sets the session's friendly name.
             *
             * @param params New friendly name to apply to the session.
             */
            set: async (params: NameSetRequest): Promise<void> =>
                connection.sendRequest("session.name.set", { sessionId, ...params }),
            /**
             * Persists an auto-generated session summary as the session's name when no user-set name exists.
             *
             * @param params Auto-generated session summary to apply as the session's name when no user-set name exists.
             *
             * @returns Indicates whether the auto-generated summary was applied as the session's name.
             */
            setAuto: async (params: NameSetAutoRequest): Promise<NameSetAutoResult> =>
                connection.sendRequest("session.name.setAuto", { sessionId, ...params }),
        },
        /** @experimental */
        plan: {
            /**
             * Reads the session plan file from the workspace.
             *
             * @returns Existence, contents, and resolved path of the session plan file.
             */
            read: async (): Promise<PlanReadResult> =>
                connection.sendRequest("session.plan.read", { sessionId }),
            /**
             * Writes new content to the session plan file.
             *
             * @param params Replacement contents to write to the session plan file.
             */
            update: async (params: PlanUpdateRequest): Promise<void> =>
                connection.sendRequest("session.plan.update", { sessionId, ...params }),
            /**
             * Deletes the session plan file from the workspace.
             */
            delete: async (): Promise<void> =>
                connection.sendRequest("session.plan.delete", { sessionId }),
            /**
             * Reads todo rows from the session SQL database for plan rendering.
             *
             * @returns Todo rows read from the session SQL database. Empty when no session database is available.
             */
            readSqlTodos: async (): Promise<PlanReadSqlTodosResult> =>
                connection.sendRequest("session.plan.readSqlTodos", { sessionId }),
            /**
             * Reads todo rows AND dependency edges from the session SQL database for structured progress UI. Same defensive behavior as readSqlTodos — returns empty arrays when the database, tables, or columns aren't available. Clients should call this on session start and after every `session.todos_changed` event to refresh structured-UI rendering.
             *
             * @returns Todo rows + dependency edges read from the session SQL database.
             */
            readSqlTodosWithDependencies: async (): Promise<PlanReadSqlTodosWithDependenciesResult> =>
                connection.sendRequest("session.plan.readSqlTodosWithDependencies", { sessionId }),
        },
        /** @experimental */
        workspaces: {
            /**
             * Gets current workspace metadata for the session.
             *
             * @returns Current workspace metadata for the session, including its absolute filesystem path when available.
             */
            getWorkspace: async (): Promise<WorkspacesGetWorkspaceResult> =>
                connection.sendRequest("session.workspaces.getWorkspace", { sessionId }),
            /**
             * Updates workspace metadata for a local session and returns the refreshed workspace.
             *
             * @param params Workspace metadata fields to update.
             *
             * @returns Current workspace metadata for the session, including its absolute filesystem path when available.
             */
            updateMetadata: async (params: WorkspacesUpdateMetadataRequest): Promise<WorkspacesGetWorkspaceResult> =>
                connection.sendRequest("session.workspaces.updateMetadata", { sessionId, ...params }),
            /**
             * Ensures a local session workspace exists and returns it.
             *
             * @param params Optional session context used when creating a local workspace.
             *
             * @returns Current workspace metadata for the session, including its absolute filesystem path when available.
             */
            ensure: async (params: WorkspacesEnsureRequest): Promise<WorkspacesGetWorkspaceResult> =>
                connection.sendRequest("session.workspaces.ensure", { sessionId, ...params }),
            /**
             * Lists files stored in the session workspace files directory.
             *
             * @returns Relative paths of files stored in the session workspace files directory.
             */
            listFiles: async (): Promise<WorkspacesListFilesResult> =>
                connection.sendRequest("session.workspaces.listFiles", { sessionId }),
            /**
             * Reads a file from the session workspace files directory.
             *
             * @param params Relative path of the workspace file to read.
             *
             * @returns Contents of the requested workspace file as a UTF-8 string.
             */
            readFile: async (params: WorkspacesReadFileRequest): Promise<WorkspacesReadFileResult> =>
                connection.sendRequest("session.workspaces.readFile", { sessionId, ...params }),
            /**
             * Creates or overwrites a file in the session workspace files directory.
             *
             * @param params Relative path and UTF-8 content for the workspace file to create or overwrite.
             */
            createFile: async (params: WorkspacesCreateFileRequest): Promise<void> =>
                connection.sendRequest("session.workspaces.createFile", { sessionId, ...params }),
            /**
             * Lists workspace checkpoints in chronological order.
             *
             * @returns Workspace checkpoints in chronological order; empty when the workspace is not enabled.
             */
            listCheckpoints: async (): Promise<WorkspacesListCheckpointsResult> =>
                connection.sendRequest("session.workspaces.listCheckpoints", { sessionId }),
            /**
             * Reads the content of a workspace checkpoint by number.
             *
             * @param params Checkpoint number to read.
             *
             * @returns Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
             */
            readCheckpoint: async (params: WorkspacesReadCheckpointRequest): Promise<WorkspacesReadCheckpointResult> =>
                connection.sendRequest("session.workspaces.readCheckpoint", { sessionId, ...params }),
            /**
             * Adds a compaction summary checkpoint to the local session workspace.
             *
             * @param params Compaction summary checkpoint to persist.
             *
             * @returns Persisted summary metadata and refreshed workspace metadata.
             */
            addSummary: async (params: WorkspacesAddSummaryRequest): Promise<WorkspacesAddSummaryResult> =>
                connection.sendRequest("session.workspaces.addSummary", { sessionId, ...params }),
            /**
             * Truncates local workspace compaction summaries after a rollback.
             *
             * @param params Rollback point for local workspace summaries.
             *
             * @returns Current workspace metadata for the session, including its absolute filesystem path when available.
             */
            truncateSummaries: async (params: WorkspacesTruncateSummariesRequest): Promise<WorkspacesGetWorkspaceResult> =>
                connection.sendRequest("session.workspaces.truncateSummaries", { sessionId, ...params }),
            /**
             * Reads the autopilot objective state file from the local session workspace.
             *
             * @returns Autopilot objective file content, or null when missing.
             */
            readAutopilotObjective: async (): Promise<WorkspacesReadAutopilotObjectiveResult> =>
                connection.sendRequest("session.workspaces.readAutopilotObjective", { sessionId }),
            /**
             * Writes the autopilot objective state file in the local session workspace.
             *
             * @param params Autopilot objective file content to persist.
             *
             * @returns Result of writing the autopilot objective file.
             */
            writeAutopilotObjective: async (params: WorkspacesWriteAutopilotObjectiveRequest): Promise<WorkspacesWriteAutopilotObjectiveResult> =>
                connection.sendRequest("session.workspaces.writeAutopilotObjective", { sessionId, ...params }),
            /**
             * Deletes the autopilot objective state file from the local session workspace.
             *
             * @returns Result of deleting the autopilot objective file.
             */
            deleteAutopilotObjective: async (): Promise<WorkspacesDeleteAutopilotObjectiveResult> =>
                connection.sendRequest("session.workspaces.deleteAutopilotObjective", { sessionId }),
            /**
             * Checks whether the local session workspace has an autopilot objective state file.
             *
             * @returns Whether the autopilot objective file exists.
             */
            autopilotObjectiveExists: async (): Promise<WorkspacesAutopilotObjectiveExistsResult> =>
                connection.sendRequest("session.workspaces.autopilotObjectiveExists", { sessionId }),
            /**
             * Saves pasted content as a UTF-8 file in the session workspace.
             *
             * @param params Pasted content to save as a UTF-8 file in the session workspace.
             *
             * @returns Descriptor for the saved paste file, or null when the workspace is unavailable.
             */
            saveLargePaste: async (params: WorkspacesSaveLargePasteRequest): Promise<WorkspacesSaveLargePasteResult> =>
                connection.sendRequest("session.workspaces.saveLargePaste", { sessionId, ...params }),
            /**
             * Computes a diff for the session workspace. Never rejects for a busy session: a `session`-mode diff that cannot read the session's file-change captures falls back to an unstaged git diff with `isFallback: true` and reports why in `unavailableReason`.
             *
             * @param params Parameters for computing a workspace diff.
             *
             * @returns Workspace diff result for the requested mode.
             */
            diff: async (params: WorkspacesDiffRequest): Promise<WorkspaceDiffResult> =>
                connection.sendRequest("session.workspaces.diff", { sessionId, ...params }),
        },
        /** @experimental */
        completions: {
            /**
             * Gets the characters that should trigger host-driven completions for the session. Empty disables host-driven completions (e.g. local sessions, or a relay host that does not advertise them).
             *
             * @returns Characters that, when typed in the composer, should trigger a `completions.request`. Empty when the session has no host-driven completions (e.g. local sessions, or a relay host that does not advertise `completionTriggerCharacters`).
             */
            getTriggerCharacters: async (): Promise<CompletionsGetTriggerCharactersResult> =>
                connection.sendRequest("session.completions.getTriggerCharacters", { sessionId }),
            /**
             * Requests host-driven completion items for the current composer input. Returns an empty list when the host has no items or does not support completions.
             *
             * @param params Request host-driven completions for the current composer input.
             *
             * @returns Host-driven completion items for the current composer input. Empty when the host returns no items or does not support completions.
             */
            request: async (params: CompletionsRequestRequest): Promise<CompletionsRequestResult> =>
                connection.sendRequest("session.completions.request", { sessionId, ...params }),
        },
        /** @experimental */
        instructions: {
            /**
             * Gets instruction sources loaded for the session.
             *
             * @returns Instruction sources loaded for the session, in merge order.
             */
            getSources: async (): Promise<InstructionsGetSourcesResult> =>
                connection.sendRequest("session.instructions.getSources", { sessionId }),
        },
        /** @experimental */
        fleet: {
            /**
             * Starts fleet mode by submitting the fleet orchestration prompt to the session.
             *
             * @param params Optional user prompt to combine with the fleet orchestration instructions.
             *
             * @returns Indicates whether fleet mode was successfully activated.
             */
            start: async (params: FleetStartRequest): Promise<FleetStartResult> =>
                connection.sendRequest("session.fleet.start", { sessionId, ...params }),
        },
        /** @experimental */
        agent: {
            /**
             * Lists agents available to the session. Defaults to custom agents only; pass includeBuiltInAgents to include the effective built-in agents.
             *
             * @param params Controls whether built-in agents and authored prompt text are included.
             *
             * @returns Agents available to the session.
             */
            list: async (params?: SessionAgentListRequest): Promise<AgentList> =>
                connection.sendRequest("session.agent.list", { sessionId, ...params }),
            /**
             * Sets an in-memory authored prompt override for an available agent. For built-in agents, this replaces only the static base prompt while preserving runtime-owned dynamic prompt composition and behavior. The special `general-purpose` agent is not overrideable. Overrides are not persisted; resumed and forked sessions start without them, so the host must re-apply them.
             *
             * @param params An in-memory authored prompt override for an available agent.
             */
            setPrompt: async (params: AgentSetPromptRequest): Promise<void> =>
                connection.sendRequest("session.agent.setPrompt", { sessionId, ...params }),
            /**
             * Gets the currently selected custom agent for the session.
             *
             * @returns The currently selected custom agent, or null when using the default agent.
             */
            getCurrent: async (): Promise<AgentGetCurrentResult> =>
                connection.sendRequest("session.agent.getCurrent", { sessionId }),
            /**
             * Selects a custom agent for subsequent turns in the session.
             *
             * @param params Name of the custom agent to select for subsequent turns.
             *
             * @returns The newly selected custom agent.
             */
            select: async (params: AgentSelectRequest): Promise<AgentSelectResult> =>
                connection.sendRequest("session.agent.select", { sessionId, ...params }),
            /**
             * Clears the selected custom agent and returns the session to the default agent.
             */
            deselect: async (): Promise<void> =>
                connection.sendRequest("session.agent.deselect", { sessionId }),
            /**
             * Reloads custom agent definitions and returns the refreshed list.
             *
             * @returns Custom agents available to the session after reloading definitions from disk.
             */
            reload: async (): Promise<AgentReloadResult> =>
                connection.sendRequest("session.agent.reload", { sessionId }),
        },
        /** @experimental */
        tasks: {
            /**
             * Starts a background agent task in the session.
             *
             * @param params Agent type, prompt, name, and optional description and model override for the new task.
             *
             * @returns Identifier assigned to the newly started background agent task.
             */
            startAgent: async (params: TasksStartAgentRequest): Promise<TasksStartAgentResult> =>
                connection.sendRequest("session.tasks.startAgent", { sessionId, ...params }),
            /**
             * Lists background tasks tracked by the session.
             *
             * @returns Background tasks currently tracked by the session.
             */
            list: async (): Promise<TaskList> =>
                connection.sendRequest("session.tasks.list", { sessionId }),
            /**
             * Refreshes metadata for any detached background shells the runtime knows about.
             *
             * @returns Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
             */
            refresh: async (): Promise<TasksRefreshResult> =>
                connection.sendRequest("session.tasks.refresh", { sessionId }),
            /**
             * Waits for all in-flight background tasks and any follow-up turns to settle.
             *
             * @returns Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
             */
            waitForPending: async (): Promise<TasksWaitForPendingResult> =>
                connection.sendRequest("session.tasks.waitForPending", { sessionId }),
            /**
             * Returns progress information for a background task by ID.
             *
             * @param params Identifier of the background task to fetch progress for.
             *
             * @returns Progress information for the task, or null when no task with that ID is tracked.
             */
            getProgress: async (params: TasksGetProgressRequest): Promise<TasksGetProgressResult> =>
                connection.sendRequest("session.tasks.getProgress", { sessionId, ...params }),
            /**
             * Returns the first sync-waiting task that can currently be promoted to background mode.
             *
             * @returns The first sync-waiting task that can currently be promoted to background mode.
             */
            getCurrentPromotable: async (): Promise<TasksGetCurrentPromotableResult> =>
                connection.sendRequest("session.tasks.getCurrentPromotable", { sessionId }),
            /**
             * Promotes an eligible synchronously-waited task so it continues running in the background.
             *
             * @param params Identifier of the task to promote to background mode.
             *
             * @returns Indicates whether the task was successfully promoted to background mode.
             */
            promoteToBackground: async (params: TasksPromoteToBackgroundRequest): Promise<TasksPromoteToBackgroundResult> =>
                connection.sendRequest("session.tasks.promoteToBackground", { sessionId, ...params }),
            /**
             * Atomically promotes the first promotable sync-waiting task to background mode and returns it.
             *
             * @returns The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
             */
            promoteCurrentToBackground: async (): Promise<TasksPromoteCurrentToBackgroundResult> =>
                connection.sendRequest("session.tasks.promoteCurrentToBackground", { sessionId }),
            /**
             * Cancels a background task.
             *
             * @param params Identifier of the background task to cancel.
             *
             * @returns Indicates whether the background task was successfully cancelled.
             */
            cancel: async (params: TasksCancelRequest): Promise<TasksCancelResult> =>
                connection.sendRequest("session.tasks.cancel", { sessionId, ...params }),
            /**
             * Removes a completed or cancelled background task from tracking.
             *
             * @param params Identifier of the completed or cancelled task to remove from tracking.
             *
             * @returns Indicates whether the task was removed. False when the task does not exist or is still running/idle.
             */
            remove: async (params: TasksRemoveRequest): Promise<TasksRemoveResult> =>
                connection.sendRequest("session.tasks.remove", { sessionId, ...params }),
            /**
             * Sends a message to a background agent task.
             *
             * @param params Identifier of the target agent task, message content, and optional sender agent ID.
             *
             * @returns Indicates whether the message was delivered, with an error message when delivery failed.
             */
            sendMessage: async (params: TasksSendMessageRequest): Promise<TasksSendMessageResult> =>
                connection.sendRequest("session.tasks.sendMessage", { sessionId, ...params }),
        },
        /** @experimental */
        skills: {
            /**
             * Lists skills available to the session.
             *
             * @returns Skills available to the session, with their enabled state.
             */
            list: async (): Promise<SkillList> =>
                connection.sendRequest("session.skills.list", { sessionId }),
            /**
             * Returns the skills that have been invoked during this session.
             *
             * @returns Skills invoked during this session, ordered by invocation time (most recent last).
             */
            getInvoked: async (): Promise<SkillsGetInvokedResult> =>
                connection.sendRequest("session.skills.getInvoked", { sessionId }),
            /**
             * Enables a skill for the session.
             *
             * @param params Name of the skill to enable for the session.
             */
            enable: async (params: SkillsEnableRequest): Promise<void> =>
                connection.sendRequest("session.skills.enable", { sessionId, ...params }),
            /**
             * Disables a skill for the session.
             *
             * @param params Name of the skill to disable for the session.
             */
            disable: async (params: SkillsDisableRequest): Promise<void> =>
                connection.sendRequest("session.skills.disable", { sessionId, ...params }),
            /**
             * Reloads skill definitions for the session.
             *
             * @returns Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
             */
            reload: async (): Promise<SkillsLoadDiagnostics> =>
                connection.sendRequest("session.skills.reload", { sessionId }),
            /**
             * Ensures the session's skill definitions have been loaded from disk.
             */
            ensureLoaded: async (): Promise<void> =>
                connection.sendRequest("session.skills.ensureLoaded", { sessionId }),
        },
        /** @experimental */
        mcp: {
            /**
             * Lists MCP servers configured for the session, their connection status, and host-level state. The host-level state (disabled/filtered servers, failed/needs-auth/pending connections, mcp3p policy, full config) is empty/zero when no MCP host has been initialized for the session.
             *
             * @returns MCP servers configured for the session, with their connection status and host-level state.
             */
            list: async (): Promise<McpServerList> =>
                connection.sendRequest("session.mcp.list", { sessionId }),
            /**
             * Lists the tools exposed by a connected MCP server on this session's host. This performs a live `tools/list` request. Tool UI metadata is returned independently of whether MCP Apps rendering is enabled for the session.
             *
             * @param params Server name whose tool list should be returned.
             *
             * @returns Tools exposed by the connected MCP server. Throws when the server is not connected.
             */
            listTools: async (params: McpListToolsRequest): Promise<McpListToolsResult> =>
                connection.sendRequest("session.mcp.listTools", { sessionId, ...params }),
            /**
             * Enables an MCP server for the session.
             *
             * @param params Name of the MCP server to enable for the session.
             */
            enable: async (params: McpEnableRequest): Promise<void> =>
                connection.sendRequest("session.mcp.enable", { sessionId, ...params }),
            /**
             * Disables an MCP server for the session.
             *
             * @param params Name of the MCP server to disable for the session.
             */
            disable: async (params: McpDisableRequest): Promise<void> =>
                connection.sendRequest("session.mcp.disable", { sessionId, ...params }),
            /**
             * Reloads MCP server connections for the session.
             */
            reload: async (): Promise<void> =>
                connection.sendRequest("session.mcp.reload", { sessionId }),
            /**
             * Releases any turns waiting on an in-flight MCP load without cancelling the load, letting the agent proceed while MCP servers finish connecting in the background. No-op when no MCP load is in flight or waiting turns were already released.
             *
             * @returns Result of moving in-flight MCP loading to the background.
             */
            moveLoadingToBackground: async (): Promise<MoveMcpLoadingToBackgroundResult> =>
                connection.sendRequest("session.mcp.moveLoadingToBackground", { sessionId }),
            /**
             * Runs an MCP sampling inference on behalf of an MCP server.
             *
             * @param params Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
             *
             * @returns Outcome of an MCP sampling execution: success result, failure error, or cancellation.
             */
            executeSampling: async (params: McpExecuteSamplingParams): Promise<McpSamplingExecutionResult> =>
                connection.sendRequest("session.mcp.executeSampling", { sessionId, ...params }),
            /**
             * Cancels an in-flight MCP sampling execution by request ID.
             *
             * @param params The requestId previously passed to executeSampling that should be cancelled.
             *
             * @returns Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
             */
            cancelSamplingExecution: async (params: McpCancelSamplingExecutionParams): Promise<McpCancelSamplingExecutionResult> =>
                connection.sendRequest("session.mcp.cancelSamplingExecution", { sessionId, ...params }),
            /**
             * Sets how environment-variable values supplied to MCP servers are resolved (direct or indirect).
             *
             * @param params Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
             *
             * @returns Env-value mode recorded on the session after the update.
             */
            setEnvValueMode: async (params: McpSetEnvValueModeParams): Promise<McpSetEnvValueModeResult> =>
                connection.sendRequest("session.mcp.setEnvValueMode", { sessionId, ...params }),
            /**
             * Removes the auto-managed `github` MCP server when present.
             *
             * @returns Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
             */
            removeGitHub: async (): Promise<McpRemoveGitHubResult> =>
                connection.sendRequest("session.mcp.removeGitHub", { sessionId }),
            /**
             * Starts an individual MCP server on the live session. Omit `config` for a config-free start-by-name of an already-configured server (reuses the server's already-registered configuration); supply `config` to start from a caller-supplied configuration. Session-scoped and ephemeral: the server is added to this session's running set only and is reaped when the session ends. Does NOT modify persistent user configuration (`mcp.config.*`), so it does not affect future sessions. The server surfaces through `session.mcp.list` and the `session.mcp_servers_loaded` / `session.mcp_server_status_changed` events like any other server.
             *
             * @param params Server name and optional configuration for an individual MCP server start. Omit `config` for a config-free start-by-name of an already-configured server.
             */
            startServer: async (params: McpStartServerRequest): Promise<void> =>
                connection.sendRequest("session.mcp.startServer", { sessionId, ...params }),
            /**
             * Restarts an individual MCP server on the live session (stops then starts). Omit `config` for a config-free restart-by-name of an already-configured server; supply `config` to restart with a replacement configuration. Session-scoped and ephemeral: does NOT modify persistent user configuration (`mcp.config.*`).
             *
             * @param params Server name and optional replacement configuration for an individual MCP server restart. Omit `config` for a config-free restart-by-name of an already-configured server.
             */
            restartServer: async (params: McpRestartServerRequest): Promise<void> =>
                connection.sendRequest("session.mcp.restartServer", { sessionId, ...params }),
            /**
             * Stops an individual MCP server on the session's host.
             *
             * @param params Server name for an individual MCP server stop.
             */
            stopServer: async (params: McpStopServerRequest): Promise<void> =>
                connection.sendRequest("session.mcp.stopServer", { sessionId, ...params }),
            /**
             * Checks whether a named MCP server is currently running on the session's host.
             *
             * @param params Server name to check running status for.
             *
             * @returns Whether the named MCP server is running.
             */
            isServerRunning: async (params: McpIsServerRunningRequest): Promise<McpIsServerRunningResult> =>
                connection.sendRequest("session.mcp.isServerRunning", { sessionId, ...params }),
            /** @experimental */
            oauth: {
                /**
                 * Resolves a pending MCP OAuth request with a host-provided token or cancellation. The pending request is emitted as mcp.oauth_required with the data necessary to authorize the request.
                 *
                 * @param params Pending MCP OAuth request ID and host-provided token or cancellation response.
                 *
                 * @returns Indicates whether the pending MCP OAuth response was accepted.
                 */
                handlePendingRequest: async (params: McpOauthHandlePendingRequest): Promise<McpOauthHandlePendingResult> =>
                    connection.sendRequest("session.mcp.oauth.handlePendingRequest", { sessionId, ...params }),
                /**
                 * Notifies the session that MCP OAuth authentication succeeded and updated credentials were persisted, so cached tool definitions can be refreshed.
                 *
                 * @param params Identifies the MCP server whose persisted OAuth credentials were updated.
                 */
                authenticationStateChanged: async (params: McpOauthAuthenticationStateChangedRequest): Promise<void> =>
                    connection.sendRequest("session.mcp.oauth.authenticationStateChanged", { sessionId, ...params }),
                /**
                 * Starts OAuth authentication for a remote MCP server.
                 *
                 * @param params Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, callback success-page copy, and static OAuth client selection.
                 *
                 * @returns OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
                 */
                login: async (params: McpOauthLoginRequest): Promise<McpOauthLoginResult> =>
                    connection.sendRequest("session.mcp.oauth.login", { sessionId, ...params }),
                /**
                 * Passively probes a configured remote MCP server to classify whether OAuth is required or a cached/override token is accepted. Does not start OAuth, emit pending OAuth requests, or mutate MCP connection state.
                 *
                 * @param params Remote MCP server name for a passive OAuth status probe.
                 *
                 * @returns Passive MCP OAuth probe result. `authenticated` means the server accepted the probe request while an OAuth-origin access token was attached; it does not prove the server required or independently validated that token. The probe does not make a second unauthenticated request. Failed is an expected probe-domain outcome; JSON-RPC errors are reserved for API-call failures.
                 */
                probe: async (params: McpOauthProbeRequest): Promise<McpOauthProbeResult> =>
                    connection.sendRequest("session.mcp.oauth.probe", { sessionId, ...params }),
                /**
                 * Responds to a pending MCP OAuth authorization request by its request id.
                 *
                 * @param params Pending MCP OAuth request id to respond to.
                 *
                 * @returns Indicates whether the pending MCP OAuth response was accepted.
                 */
                respond: async (params: McpOauthRespondRequest): Promise<McpOauthRespondResult> =>
                    connection.sendRequest("session.mcp.oauth.respond", { sessionId, ...params }),
            },
            /** @experimental */
            headers: {
                /**
                 * Responds to a pending MCP dynamic headers refresh request. Hosts that subscribe to `mcp.headers_refresh_required` use this to provide short-lived per-server headers or to indicate that no dynamic headers are available for this refresh.
                 *
                 * @param params MCP headers refresh request id and the host response.
                 *
                 * @returns Indicates whether the pending MCP headers refresh response was accepted.
                 */
                handlePendingHeadersRefreshRequest: async (params: McpHeadersHandlePendingHeadersRefreshRequestRequest): Promise<McpHeadersHandlePendingHeadersRefreshRequestResult> =>
                    connection.sendRequest("session.mcp.headers.handlePendingHeadersRefreshRequest", { sessionId, ...params }),
            },
            /** @experimental */
            apps: {
                /**
                 * Fetch an MCP resource (typically a `ui://` MCP App bundle, per SEP-1865) from a connected server. Requires the `mcp-apps` session capability.
                 *
                 * @param params MCP server and resource URI to fetch.
                 *
                 * @returns Resource contents returned by the MCP server.
                 */
                readResource: async (params: McpAppsReadResourceRequest): Promise<McpAppsReadResourceResult> =>
                    connection.sendRequest("session.mcp.apps.readResource", { sessionId, ...params }),
                /**
                 * List tools that an MCP App view is allowed to call (SEP-1865 visibility filter). Returns tools whose `_meta.ui.visibility` is unset (default `["model","app"]`) or includes `"app"`.
                 *
                 * @param params MCP server to list app-callable tools for.
                 *
                 * @returns App-callable tools from the named MCP server.
                 */
                listTools: async (params: McpAppsListToolsRequest): Promise<McpAppsListToolsResult> =>
                    connection.sendRequest("session.mcp.apps.listTools", { sessionId, ...params }),
                /**
                 * Call an MCP tool from an MCP App view (SEP-1865). Enforces the visibility check that prevents an app iframe from invoking model-only tools. Returns the standard MCP `CallToolResult`.
                 *
                 * @param params MCP server, tool name, and arguments to invoke from an MCP App view.
                 *
                 * @returns Standard MCP CallToolResult
                 */
                callTool: async (params: McpAppsCallToolRequest): Promise<SessionMcpAppsCallToolResult> =>
                    connection.sendRequest("session.mcp.apps.callTool", { sessionId, ...params }),
                /**
                 * Replace the host context returned to MCP App guests on `ui/initialize`. Hosts use this to advertise theme, locale, or other metadata to the guest UI.
                 *
                 * @param params Host context to advertise to MCP App guests.
                 */
                setHostContext: async (params: McpAppsSetHostContextRequest): Promise<void> =>
                    connection.sendRequest("session.mcp.apps.setHostContext", { sessionId, ...params }),
                /**
                 * Read the current host context advertised to MCP App guests.
                 *
                 * @returns Current host context advertised to MCP App guests.
                 */
                getHostContext: async (): Promise<McpAppsHostContext> =>
                    connection.sendRequest("session.mcp.apps.getHostContext", { sessionId }),
                /**
                 * Diagnose MCP Apps wiring for a specific MCP server. Reports the session capability, feature-flag state, advertised extension, and how many tools have `_meta.ui` populated.
                 *
                 * @param params MCP server to diagnose MCP Apps wiring for.
                 *
                 * @returns Diagnostic snapshot of MCP Apps wiring for the named server.
                 */
                diagnose: async (params: McpAppsDiagnoseRequest): Promise<McpAppsDiagnoseResult> =>
                    connection.sendRequest("session.mcp.apps.diagnose", { sessionId, ...params }),
            },
            /** @experimental */
            resources: {
                /**
                 * Fetch an MCP resource from a connected server by URI (proxies MCP `resources/read`).
                 *
                 * @param params MCP server and resource URI to fetch.
                 *
                 * @returns Resource contents returned by the MCP server.
                 */
                read: async (params: McpResourcesReadRequest): Promise<McpResourcesReadResult> =>
                    connection.sendRequest("session.mcp.resources.read", { sessionId, ...params }),
                /**
                 * Enumerate one page of resources a connected MCP server exposes (proxies MCP `resources/list`). Pass `cursor` to continue from a prior result's `nextCursor`.
                 *
                 * @param params MCP server whose resources to enumerate.
                 *
                 * @returns One page of resources advertised by the named MCP server.
                 */
                list: async (params: McpResourcesListRequest): Promise<McpResourcesListResult> =>
                    connection.sendRequest("session.mcp.resources.list", { sessionId, ...params }),
                /**
                 * Enumerate one page of resource templates a connected MCP server exposes (proxies MCP `resources/templates/list`). Pass `cursor` to continue from a prior result's `nextCursor`.
                 *
                 * @param params MCP server whose resource templates to enumerate.
                 *
                 * @returns One page of resource templates advertised by the named MCP server.
                 */
                listTemplates: async (params: McpResourcesListTemplatesRequest): Promise<McpResourcesListTemplatesResult> =>
                    connection.sendRequest("session.mcp.resources.listTemplates", { sessionId, ...params }),
            },
        },
        /** @experimental */
        plugins: {
            /**
             * Lists plugins installed for the session.
             *
             * @returns Plugins installed for the session, with their enabled state and version metadata.
             */
            list: async (): Promise<PluginList> =>
                connection.sendRequest("session.plugins.list", { sessionId }),
            /**
             * Reloads the session's plugin set, refreshing MCP servers, custom agents, hooks, and skills cache so SDK-driven changes via `server.plugins.*` take effect immediately.
             *
             * @param params Optional flags controlling which side effects the reload performs.
             */
            reload: async (params?: SessionPluginsReloadRequest): Promise<void> =>
                connection.sendRequest("session.plugins.reload", { sessionId, ...params }),
        },
        /** @experimental */
        provider: {
            /**
             * Returns the provider endpoint and credentials the session is currently configured to talk to, so the caller can make inference calls directly against the same backend the session uses.
             *
             * @param params Optional model identifier to scope the endpoint snapshot to.
             *
             * @returns A snapshot of the provider endpoint the session is currently configured to talk to.
             */
            getEndpoint: async (params?: SessionProviderGetEndpointRequest): Promise<ProviderEndpoint> =>
                connection.sendRequest("session.provider.getEndpoint", { sessionId, ...params }),
            /**
             * Adds BYOK providers and/or models to the session's registry at runtime, extending the additive registry built from the session's `providers`/`models` options. Both fields are optional, so a call may add providers only, models only, or both. Within a single call providers are registered before models, so a model may reference a provider added in the same call; across calls a model may reference any provider already registered (from session creation or a prior add). A model whose referenced provider is not registered by the end of the call is rejected. Newly added models become selectable via `model.list` / `model.switchTo` and are inherited by sub-agents spawned afterwards.
             *
             * @param params BYOK providers and/or models to add to the session's registry at runtime. Both fields are optional; provide providers, models, or both.
             *
             * @returns The selectable model entries synthesized for the models added by this call.
             */
            add: async (params: ProviderAddRequest): Promise<ProviderAddResult> =>
                connection.sendRequest("session.provider.add", { sessionId, ...params }),
        },
        /** @experimental */
        options: {
            /**
             * Patches the genuinely-mutable subset of session options.
             *
             * @param params Patch of mutable session options to apply to the running session.
             *
             * @returns Indicates whether the session options patch was applied successfully.
             */
            update: async (params: SessionUpdateOptionsParams): Promise<SessionUpdateOptionsResult> =>
                connection.sendRequest("session.options.update", { sessionId, ...params }),
        },
        /** @experimental */
        lsp: {
            /**
             * Loads the merged LSP configuration set for the session's working directory.
             *
             * @param params Parameters for (re)loading the merged LSP configuration set.
             */
            initialize: async (params: LspInitializeRequest): Promise<void> =>
                connection.sendRequest("session.lsp.initialize", { sessionId, ...params }),
        },
        /** @experimental */
        extensions: {
            /**
             * Lists extensions discovered for the session and their current status.
             *
             * @returns Extensions discovered for the session, with their current status.
             */
            list: async (): Promise<ExtensionList> =>
                connection.sendRequest("session.extensions.list", { sessionId }),
            /**
             * Enables an extension for the session.
             *
             * @param params Source-qualified extension identifier to enable for the session.
             */
            enable: async (params: ExtensionsEnableRequest): Promise<void> =>
                connection.sendRequest("session.extensions.enable", { sessionId, ...params }),
            /**
             * Disables an extension for the session.
             *
             * @param params Source-qualified extension identifier to disable for the session.
             */
            disable: async (params: ExtensionsDisableRequest): Promise<void> =>
                connection.sendRequest("session.extensions.disable", { sessionId, ...params }),
            /**
             * Reloads extension definitions and processes for the session.
             */
            reload: async (): Promise<void> =>
                connection.sendRequest("session.extensions.reload", { sessionId }),
            /**
             * Push attachments into the next user-message turn from an extension. The host should surface them as composer pills and forward them via the next session.send call. Callable only by extension-owned connections.
             *
             * @param params Parameters for session.extensions.sendAttachmentsToMessage.
             */
            sendAttachmentsToMessage: async (params: SendAttachmentsToMessageParams): Promise<void> =>
                connection.sendRequest("session.extensions.sendAttachmentsToMessage", { sessionId, ...params }),
        },
        /** @experimental */
        tools: {
            /**
             * Executes one tool from the session's currently offered tool set through the native invocation pipeline.
             *
             * @param params A tool name and arguments to execute through the session's native invocation pipeline.
             *
             * @returns Canonical result returned by a session tool.
             */
            execute: async (params: ToolsExecuteRequest): Promise<ToolResult> =>
                connection.sendRequest("session.tools.execute", { sessionId, ...params }),
            /**
             * Returns the Rust-owned built-in tool descriptors used to construct the session's offered tool set.
             *
             * @param params Options controlling how Rust-owned built-in tool descriptors are materialized.
             *
             * @returns Rust-owned built-in tool descriptors for the session.
             */
            getBuiltinDescriptors: async (params: ToolsGetBuiltinDescriptorsRequest): Promise<ToolsGetBuiltinDescriptorsResult> =>
                connection.sendRequest("session.tools.getBuiltinDescriptors", { sessionId, ...params }),
            /**
             * Projects a completed task_complete tool call into its label-safe session event payload.
             *
             * @param params Task-completion tool arguments and final result used to build a label-safe session event payload.
             *
             * @returns Task completion notification with summary from the agent
             */
            taskCompleteEventData: async (params: ToolsTaskCompleteEventDataRequest): Promise<TaskCompleteData> =>
                connection.sendRequest("session.tools.taskCompleteEventData", { sessionId, ...params }),
            /**
             * Provides the result for a pending external tool call.
             *
             * @param params Pending external tool call request ID, with the tool result or an error describing why it failed.
             *
             * @returns Indicates whether the external tool call result was handled successfully.
             */
            handlePendingToolCall: async (params: HandlePendingToolCallRequest): Promise<HandlePendingToolCallResult> =>
                connection.sendRequest("session.tools.handlePendingToolCall", { sessionId, ...params }),
            /**
             * Resolves, builds, and validates the runtime tool list for the session.
             *
             * @returns Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
             */
            initializeAndValidate: async (): Promise<ToolsInitializeAndValidateResult> =>
                connection.sendRequest("session.tools.initializeAndValidate", { sessionId }),
            /**
             * Returns lightweight metadata for the session's currently initialized tools.
             *
             * @returns Current lightweight tool metadata snapshot for the session.
             */
            getCurrentMetadata: async (): Promise<ToolsGetCurrentMetadataResult> =>
                connection.sendRequest("session.tools.getCurrentMetadata", { sessionId }),
            /**
             * Atomically replaces the complete externally implemented tool list supplied by the calling connection. Built-in, MCP/plugin, extension-discovered, subagent, and tools supplied by other connections remain unchanged.
             *
             * @param params Complete externally implemented tool list for the calling connection. An empty list removes every tool previously supplied by that connection.
             *
             * @returns Empty result after replacing the calling connection's externally implemented tools.
             */
            set: async (params: ToolsSetRequest): Promise<ToolsSetResult> =>
                connection.sendRequest("session.tools.set", { sessionId, ...params }),
            /**
             * Updates the current session's live subagent settings after user settings change. The persisted user settings remain the source of truth for future sessions.
             *
             * @param params Subagent settings to apply to the current session
             *
             * @returns Empty result after applying subagent settings
             */
            updateSubagentSettings: async (params: UpdateSubagentSettingsRequest): Promise<ToolsUpdateSubagentSettingsResult> =>
                connection.sendRequest("session.tools.updateSubagentSettings", { sessionId, ...params }),
        },
        /** @experimental */
        commands: {
            /**
             * Lists slash commands available in the session.
             *
             * @param params Optional filters controlling which command sources to include in the listing.
             *
             * @returns Slash commands available in the session, after applying any include/exclude filters.
             */
            list: async (params?: SessionCommandsListRequest): Promise<CommandList> =>
                connection.sendRequest("session.commands.list", { sessionId, ...params }),
            /**
             * Invokes a slash command in the session.
             *
             * @param params Slash command name and optional raw input string to invoke.
             *
             * @returns Result of invoking the slash command (text output, prompt to send to the agent, completion, or subcommand selection).
             */
            invoke: async (params: CommandsInvokeRequest): Promise<SlashCommandInvocationResult> =>
                connection.sendRequest("session.commands.invoke", { sessionId, ...params }),
            /**
             * Reports completion of a pending client-handled slash command.
             *
             * @param params Pending command request ID and an optional error if the client handler failed.
             *
             * @returns Indicates whether the pending client-handled command was completed successfully.
             */
            handlePendingCommand: async (params: CommandsHandlePendingCommandRequest): Promise<CommandsHandlePendingCommandResult> =>
                connection.sendRequest("session.commands.handlePendingCommand", { sessionId, ...params }),
            /**
             * Executes a slash command synchronously and returns any error.
             *
             * @param params Slash command name and argument string to execute synchronously.
             *
             * @returns Error message produced while executing the command, if any.
             */
            execute: async (params: ExecuteCommandParams): Promise<ExecuteCommandResult> =>
                connection.sendRequest("session.commands.execute", { sessionId, ...params }),
            /**
             * Enqueues a slash command for FIFO processing on the local session.
             *
             * @param params Slash-prefixed command string to enqueue for FIFO processing.
             *
             * @returns Indicates whether the command was accepted into the local execution queue.
             */
            enqueue: async (params: EnqueueCommandParams): Promise<EnqueueCommandResult> =>
                connection.sendRequest("session.commands.enqueue", { sessionId, ...params }),
            /**
             * Reports whether the host actually executed a queued command and whether to continue processing.
             *
             * @param params Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
             *
             * @returns Indicates whether the queued-command response was matched to a pending request.
             */
            respondToQueuedCommand: async (params: CommandsRespondToQueuedCommandRequest): Promise<CommandsRespondToQueuedCommandResult> =>
                connection.sendRequest("session.commands.respondToQueuedCommand", { sessionId, ...params }),
        },
        /** @experimental */
        telemetry: {
            /**
             * Gets the telemetry engagement ID currently associated with the session, when available.
             *
             * @returns Telemetry engagement ID for the session, when available.
             */
            getEngagementId: async (): Promise<SessionTelemetryEngagement> =>
                connection.sendRequest("session.telemetry.getEngagementId", { sessionId }),
            /**
             * Sets feature override key/value pairs to attach to subsequent telemetry events for the session.
             *
             * @param params Feature override key/value pairs to attach to subsequent telemetry events from this session.
             */
            setFeatureOverrides: async (params: TelemetrySetFeatureOverridesRequest): Promise<void> =>
                connection.sendRequest("session.telemetry.setFeatureOverrides", { sessionId, ...params }),
        },
        /** @experimental */
        ui: {
            /**
             * Runs a transient no-tools model query against the current conversation context.
             *
             * @param params Transient question to answer without adding it to conversation history.
             *
             * @returns Completed transient query. Ordered chunks and the terminal outcome are also delivered through `ui.ephemeral_query` session events while it runs.
             */
            ephemeralQuery: async (params: UIEphemeralQueryRequest): Promise<UIEphemeralQueryResult> =>
                connection.sendRequest("session.ui.ephemeralQuery", { sessionId, ...params }),
            /**
             * Requests structured input from a UI-capable client.
             *
             * @param params Prompt message and JSON schema describing the form fields to elicit from the user.
             *
             * @returns The elicitation response (accept with form values, decline, or cancel)
             */
            elicitation: async (params: UIElicitationRequest): Promise<UIElicitationResponse> =>
                connection.sendRequest("session.ui.elicitation", { sessionId, ...params }),
            /**
             * Provides the user response for a pending elicitation request.
             *
             * @param params Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
             *
             * @returns Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
             */
            handlePendingElicitation: async (params: UIHandlePendingElicitationRequest): Promise<UIElicitationResult> =>
                connection.sendRequest("session.ui.handlePendingElicitation", { sessionId, ...params }),
            /**
             * Resolves a pending `user_input.requested` event with the user's response.
             *
             * @param params Request ID of a pending `user_input.requested` event and the user's response.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingUserInput: async (params: UIHandlePendingUserInputRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingUserInput", { sessionId, ...params }),
            /**
             * Resolves a pending `sampling.requested` event with a sampling result, or rejects it.
             *
             * @param params Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingSampling: async (params: UIHandlePendingSamplingRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingSampling", { sessionId, ...params }),
            /**
             * Resolves a pending `auto_mode_switch.requested` event with the user's accept/decline decision.
             *
             * @param params Request ID of a pending `auto_mode_switch.requested` event and the user's response.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingAutoModeSwitch: async (params: UIHandlePendingAutoModeSwitchRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingAutoModeSwitch", { sessionId, ...params }),
            /**
             * Resolves a pending `session_limits_exhausted.requested` event with the user's selected limit action.
             *
             * @param params Request ID of a pending `session_limits_exhausted.requested` event and the user's selected limit action.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingSessionLimitsExhausted: async (params: UIHandlePendingSessionLimitsExhaustedRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingSessionLimitsExhausted", { sessionId, ...params }),
            /**
             * Resolves a pending `exit_plan_mode.requested` event with the user's response.
             *
             * @param params Request ID of a pending `exit_plan_mode.requested` event and the user's response.
             *
             * @returns Indicates whether the pending UI request was resolved by this call.
             */
            handlePendingExitPlanMode: async (params: UIHandlePendingExitPlanModeRequest): Promise<UIHandlePendingResult> =>
                connection.sendRequest("session.ui.handlePendingExitPlanMode", { sessionId, ...params }),
            /**
             * Registers an in-process handler for auto-mode-switch requests so the server bridge skips dispatch.
             *
             * @returns Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
             */
            registerDirectAutoModeSwitchHandler: async (): Promise<UIRegisterDirectAutoModeSwitchHandlerResult> =>
                connection.sendRequest("session.ui.registerDirectAutoModeSwitchHandler", { sessionId }),
            /**
             * Unregisters a previously-registered in-process auto-mode-switch handler by its opaque handle.
             *
             * @param params Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
             *
             * @returns Indicates whether the handle was active and the registration count was decremented.
             */
            unregisterDirectAutoModeSwitchHandler: async (params: UIUnregisterDirectAutoModeSwitchHandlerRequest): Promise<UIUnregisterDirectAutoModeSwitchHandlerResult> =>
                connection.sendRequest("session.ui.unregisterDirectAutoModeSwitchHandler", { sessionId, ...params }),
        },
        /** @experimental */
        permissions: {
            /**
             * Replaces selected permission policy fields (rules, paths, URLs, exclusions, allow-all flags) on the session.
             *
             * @param params Patch of permission policy fields to apply (omit a field to leave it unchanged).
             *
             * @returns Indicates whether the operation succeeded.
             */
            configure: async (params: PermissionsConfigureParams): Promise<PermissionsConfigureResult> =>
                connection.sendRequest("session.permissions.configure", { sessionId, ...params }),
            /**
             * Provides a decision for a pending tool permission request.
             *
             * @param params Pending permission request ID and the decision to apply (approve/reject and scope).
             *
             * @returns Indicates whether the permission decision was applied; false when the request was already resolved.
             */
            handlePendingPermissionRequest: async (params: PermissionDecisionRequest): Promise<PermissionRequestResult> =>
                connection.sendRequest("session.permissions.handlePendingPermissionRequest", { sessionId, ...params }),
            /**
             * Reconstructs the set of pending tool permission requests from the session's event history.
             *
             * @returns List of pending permission requests reconstructed from event history.
             */
            pendingRequests: async (): Promise<PendingPermissionRequestList> =>
                connection.sendRequest("session.permissions.pendingRequests", { sessionId }),
            /**
             * Enables or disables automatic approval of tool permission requests for the session.
             *
             * @param params Allow-all toggle for tool permission requests, with an optional telemetry source.
             *
             * @returns Indicates whether the operation succeeded.
             */
            setApproveAll: async (params: PermissionsSetApproveAllRequest): Promise<PermissionsSetApproveAllResult> =>
                connection.sendRequest("session.permissions.setApproveAll", { sessionId, ...params }),
            /**
             * Sets the permission mode for the session. `manual` follows the normal approval flow, `assisted` attaches LLM safety recommendations, and `allow-all` automatically approves permission requests. The result returns the authoritative post-mutation mode so callers can update local state without racing the `session.permissions_changed` notification.
             *
             * @param params Permission mode to apply for the session.
             *
             * @returns Indicates whether the requested permission mode was applied and reports the authoritative post-mutation mode.
             */
            setMode: async (params: PermissionsSetModeRequest): Promise<PermissionsSetModeResult> =>
                connection.sendRequest("session.permissions.setMode", { sessionId, ...params }),
            /**
             * Returns the current permission mode for the session.
             *
             * @returns Current permission mode.
             */
            getMode: async (): Promise<PermissionsGetModeResult> =>
                connection.sendRequest("session.permissions.getMode", { sessionId }),
            /**
             * Adds or removes session-scoped or location-scoped permission rules.
             *
             * @param params Scope and add/remove instructions for modifying session- or location-scoped permission rules.
             *
             * @returns Indicates whether the operation succeeded.
             */
            modifyRules: async (params: PermissionsModifyRulesParams): Promise<PermissionsModifyRulesResult> =>
                connection.sendRequest("session.permissions.modifyRules", { sessionId, ...params }),
            /**
             * Sets whether the client wants permission prompts bridged into session events.
             *
             * @param params Toggles whether permission prompts should be bridged into session events for this client.
             *
             * @returns Indicates whether the operation succeeded.
             */
            setRequired: async (params: PermissionsSetRequiredRequest): Promise<PermissionsSetRequiredResult> =>
                connection.sendRequest("session.permissions.setRequired", { sessionId, ...params }),
            /**
             * Clears session-scoped tool permission approvals.
             *
             * @param params Clears session-scoped tool permission approvals, and optionally the location-scoped ones.
             *
             * @returns Indicates whether the operation succeeded.
             */
            resetSessionApprovals: async (params: PermissionsResetSessionApprovalsRequest): Promise<PermissionsResetSessionApprovalsResult> =>
                connection.sendRequest("session.permissions.resetSessionApprovals", { sessionId, ...params }),
            /**
             * Notifies the runtime that a permission prompt UI has been shown to the user.
             *
             * @param params Notification payload describing the permission prompt that the client just rendered.
             *
             * @returns Indicates whether the operation succeeded.
             */
            notifyPromptShown: async (params: PermissionPromptShownNotification): Promise<PermissionsNotifyPromptShownResult> =>
                connection.sendRequest("session.permissions.notifyPromptShown", { sessionId, ...params }),
            /** @experimental */
            paths: {
                /**
                 * Returns the session's allowed directories and primary working directory.
                 *
                 * @returns Snapshot of the session's allow-listed directories and primary working directory.
                 */
                list: async (): Promise<PermissionPathsList> =>
                    connection.sendRequest("session.permissions.paths.list", { sessionId }),
                /**
                 * Adds a directory to the session's allow-list and activates conventional skill and agent definitions under it.
                 *
                 * @param params Directory path to add to the session's allowed directories.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                add: async (params: PermissionPathsAddParams): Promise<PermissionsPathsAddResult> =>
                    connection.sendRequest("session.permissions.paths.add", { sessionId, ...params }),
                /**
                 * Updates the session's primary working directory used by the permission policy.
                 *
                 * @param params Directory path to set as the session's new primary working directory.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                updatePrimary: async (params: PermissionPathsUpdatePrimaryParams): Promise<PermissionsPathsUpdatePrimaryResult> =>
                    connection.sendRequest("session.permissions.paths.updatePrimary", { sessionId, ...params }),
                /**
                 * Reports whether a path falls within any of the session's allowed directories.
                 *
                 * @param params Path to evaluate against the session's allowed directories.
                 *
                 * @returns Indicates whether the supplied path is within the session's allowed directories.
                 */
                isPathWithinAllowedDirectories: async (params: PermissionPathsAllowedCheckParams): Promise<PermissionPathsAllowedCheckResult> =>
                    connection.sendRequest("session.permissions.paths.isPathWithinAllowedDirectories", { sessionId, ...params }),
                /**
                 * Reports whether a path falls within the session's workspace (primary) directory.
                 *
                 * @param params Path to evaluate against the session's workspace (primary) directory.
                 *
                 * @returns Indicates whether the supplied path is within the session's workspace directory.
                 */
                isPathWithinWorkspace: async (params: PermissionPathsWorkspaceCheckParams): Promise<PermissionPathsWorkspaceCheckResult> =>
                    connection.sendRequest("session.permissions.paths.isPathWithinWorkspace", { sessionId, ...params }),
            },
            /** @experimental */
            locations: {
                /**
                 * Resolves the permission location key and type for a working directory.
                 *
                 * @param params Working directory to resolve into a location-permissions key.
                 *
                 * @returns Resolved location-permissions key and type.
                 */
                resolve: async (params: PermissionLocationResolveParams): Promise<PermissionLocationResolveResult> =>
                    connection.sendRequest("session.permissions.locations.resolve", { sessionId, ...params }),
                /**
                 * Applies persisted location-scoped tool approvals and allowed directories for a working directory to this session's permission service.
                 *
                 * @param params Working directory to load persisted location permissions for.
                 *
                 * @returns Summary of persisted location permissions applied to the session.
                 */
                apply: async (params: PermissionLocationApplyParams): Promise<PermissionLocationApplyResult> =>
                    connection.sendRequest("session.permissions.locations.apply", { sessionId, ...params }),
                /**
                 * Persists a tool approval for a permission location and applies its rules to this session's live permission service.
                 *
                 * @param params Location-scoped tool approval to persist.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                addToolApproval: async (params: PermissionLocationAddToolApprovalParams): Promise<PermissionsLocationsAddToolApprovalResult> =>
                    connection.sendRequest("session.permissions.locations.addToolApproval", { sessionId, ...params }),
            },
            /** @experimental */
            folderTrust: {
                /**
                 * Reports whether a folder is trusted according to the user's folder trust state.
                 *
                 * @param params Folder path to check for trust.
                 *
                 * @returns Folder trust check result.
                 */
                isTrusted: async (params: FolderTrustCheckParams): Promise<FolderTrustCheckResult> =>
                    connection.sendRequest("session.permissions.folderTrust.isTrusted", { sessionId, ...params }),
                /**
                 * Adds a folder to the user's trusted folders list.
                 *
                 * @param params Folder path to add to trusted folders.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                addTrusted: async (params: FolderTrustAddParams): Promise<PermissionsFolderTrustAddTrustedResult> =>
                    connection.sendRequest("session.permissions.folderTrust.addTrusted", { sessionId, ...params }),
            },
            /** @experimental */
            urls: {
                /**
                 * Toggles the runtime's URL-permission policy between unrestricted and restricted modes.
                 *
                 * @param params Whether the URL-permission policy should run in unrestricted mode.
                 *
                 * @returns Indicates whether the operation succeeded.
                 */
                setUnrestrictedMode: async (params: PermissionUrlsSetUnrestrictedModeParams): Promise<PermissionsUrlsSetUnrestrictedModeResult> =>
                    connection.sendRequest("session.permissions.urls.setUnrestrictedMode", { sessionId, ...params }),
            },
        },
        /**
         * Emits a user-visible session log event.
         *
         * @param params Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
         *
         * @returns Identifier of the session event that was emitted for the log message.
         *
         * @experimental
         */
        log: async (params: LogRequest): Promise<LogResult> =>
            connection.sendRequest("session.log", { sessionId, ...params }),
        /** @experimental */
        metadata: {
            /**
             * Returns a snapshot of the session's identifying metadata, mode, agent, and remote info.
             *
             * @returns Point-in-time snapshot of slow-changing session identifier and state fields
             */
            snapshot: async (): Promise<SessionMetadataSnapshot> =>
                connection.sendRequest("session.metadata.snapshot", { sessionId }),
            /**
             * Reports whether the local session is currently processing user/agent messages.
             *
             * @returns Indicates whether the local session is currently processing a turn or background continuation.
             */
            isProcessing: async (): Promise<MetadataIsProcessingResult> =>
                connection.sendRequest("session.metadata.isProcessing", { sessionId }),
            /**
             * Returns a snapshot of activity flags for the session.
             *
             * @returns Current activity flags for the session.
             */
            activity: async (): Promise<SessionActivity> =>
                connection.sendRequest("session.metadata.activity", { sessionId }),
            /**
             * Returns the token breakdown for the session's current context window for a given model.
             *
             * @param params Model identifier and token limits used to compute the context-info breakdown.
             *
             * @returns Token breakdown for the session's current context window, or null if uninitialized.
             */
            contextInfo: async (params: MetadataContextInfoRequest): Promise<MetadataContextInfoResult> =>
                connection.sendRequest("session.metadata.contextInfo", { sessionId, ...params }),
            /**
             * Returns the experimental per-source attribution breakdown of the session's current context window as a flat list of entries (skills, subagents, MCP servers, built-in tools, plugin rollups, system/tool-definition costs, with nesting via parentId), plus the successful compaction count. The heaviest individual messages are available separately via `metadata.getContextHeaviestMessages`. Returns null until the session has initialized its system prompt and tool metadata.
             *
             * @returns Per-source attribution breakdown for the session's current context window, or null if uninitialized.
             */
            getContextAttribution: async (): Promise<MetadataContextAttributionResult> =>
                connection.sendRequest("session.metadata.getContextAttribution", { sessionId }),
            /**
             * Returns the largest individual messages currently in the session's context window, most-expensive first. Companion to `metadata.getContextAttribution`. Returns an empty list until the session has initialized.
             *
             * @param params Parameters for the heaviest-messages query.
             *
             * @returns The heaviest individual messages in the session's context window, most-expensive first.
             */
            getContextHeaviestMessages: async (params: MetadataContextHeaviestMessagesRequest): Promise<MetadataContextHeaviestMessagesResult> =>
                connection.sendRequest("session.metadata.getContextHeaviestMessages", { sessionId, ...params }),
            /**
             * Records a working-directory/git context change and emits a `session.context_changed` event. For a local session, a report whose `cwd` diverges from the session's current working directory is ignored (the call still succeeds but records nothing and emits no event): a local session's working directory is authoritative and is moved via `metadata.setWorkingDirectory` (or an SDK `session.resume` that supplies a `workingDirectory`), not by this method.
             *
             * @param params Updated working-directory/git context to record on the session.
             *
             * @returns Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode). For a local session, a report whose `cwd` diverges from the session's current working directory is ignored (the call still succeeds but records nothing and emits no event); move a local session's working directory via `metadata.setWorkingDirectory` instead.
             */
            recordContextChange: async (params: MetadataRecordContextChangeRequest): Promise<MetadataRecordContextChangeResult> =>
                connection.sendRequest("session.metadata.recordContextChange", { sessionId, ...params }),
            /**
             * Updates the session's working directory. For local sessions the target is validated first (an absolute path that exists on disk) and the permission primary directory is re-based; a rejected validation fails the call before any session state changes.
             *
             * @param params Absolute path to set as the session's new working directory. For local sessions the path must be absolute and exist on disk: it is validated before any session state changes, and a failing validation rejects the call with nothing mutated, persisted, or emitted. Remote sessions record the path as-is.
             *
             * @returns Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for any related side-effects (file index, etc.); it does NOT change the process working directory (a session's cwd is per-session, not process-global). For local sessions the runtime validates the target first (an absolute path that exists on disk) and re-bases the permission primary directory; a rejected validation fails the call before anything is mutated, persisted, or emitted. Location-scoped permission rules are then re-keyed to the new directory (best-effort). Remote sessions only record the path.
             */
            setWorkingDirectory: async (params: MetadataSetWorkingDirectoryRequest): Promise<MetadataSetWorkingDirectoryResult> =>
                connection.sendRequest("session.metadata.setWorkingDirectory", { sessionId, ...params }),
            /**
             * Re-tokenizes the session's existing messages against a model and returns aggregate token totals.
             *
             * @param params Model identifier to use when re-tokenizing the session's existing messages.
             *
             * @returns Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
             */
            recomputeContextTokens: async (params: MetadataRecomputeContextTokensRequest): Promise<MetadataRecomputeContextTokensResult> =>
                connection.sendRequest("session.metadata.recomputeContextTokens", { sessionId, ...params }),
        },
        /** @experimental */
        contentExclusion: {
            /**
             * Checks local file system absolute paths within the session working directory against its content-exclusion policy. Results preserve input order. Unsupported paths/filesystems and unavailable policy evaluation return available false, and callers must treat every requested path as excluded.
             *
             * @param params Local file system absolute paths within the session working directory to check against its content-exclusion policy.
             *
             * @returns Batch content-exclusion result. Callers must fail closed when policy evaluation is unavailable.
             */
            checkPaths: async (params: ContentExclusionCheckPathsRequest): Promise<ContentExclusionCheckPathsResult> =>
                connection.sendRequest("session.contentExclusion.checkPaths", { sessionId, ...params }),
        },
        /** @experimental */
        shell: {
            /**
             * Starts a shell command and streams output through session notifications. The command runs as the leader of its own process group (POSIX) or in a dedicated job object (Windows), so a forced termination — via "shell.kill", the request timeout, or session disposal — signals that whole group/job rather than only the direct child. Two gaps are worth planning for: a command that exits on its own does not trigger that teardown, and on POSIX a descendant that moves itself into a new session or process group (for example via "setsid") leaves the signalled group, so either can leave a background process running.
             *
             * @param params Shell command to run, with optional working directory and timeout in milliseconds.
             *
             * @returns Identifier of the spawned process, used to correlate streamed output and exit notifications.
             */
            exec: async (params: ShellExecRequest): Promise<ShellExecResult> =>
                connection.sendRequest("session.shell.exec", { sessionId, ...params }),
            /**
             * Sends a signal to a shell process previously started via "shell.exec". The signal targets the command's whole process group (POSIX) or job object (Windows), so descendants still in that group are signalled too, not just the direct child. On POSIX a descendant that moved itself into a new session or process group (for example via "setsid") is no longer in the signalled group and survives.
             *
             * @param params Identifier of a process previously returned by "shell.exec" and the signal to send.
             *
             * @returns Indicates whether the signal was delivered; false if the process was unknown or already exited.
             */
            kill: async (params: ShellKillRequest): Promise<ShellKillResult> =>
                connection.sendRequest("session.shell.kill", { sessionId, ...params }),
            /**
             * Executes a user-requested shell command through the session runtime.
             *
             * @param params User-requested shell command and cancellation handle.
             *
             * @returns Result of a user-requested shell command.
             */
            executeUserRequested: async (params: ShellExecuteUserRequestedRequest): Promise<UserRequestedShellCommandResult> =>
                connection.sendRequest("session.shell.executeUserRequested", { sessionId, ...params }),
            /**
             * Cancels a user-requested shell command by request ID.
             *
             * @param params User-requested shell execution cancellation handle.
             *
             * @returns Cancellation result for a user-requested shell command.
             */
            cancelUserRequested: async (params: ShellCancelUserRequestedRequest): Promise<CancelUserRequestedShellCommandResult> =>
                connection.sendRequest("session.shell.cancelUserRequested", { sessionId, ...params }),
        },
        /** @experimental */
        history: {
            /**
             * Compacts the session history to reduce context usage.
             *
             * @param params Optional compaction parameters.
             *
             * @returns Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
             */
            compact: async (params?: SessionHistoryCompactRequest): Promise<HistoryCompactResult> =>
                connection.sendRequest("session.history.compact", { sessionId, ...params }),
            /**
             * Truncates persisted session history to a specific event.
             *
             * @param params Identifier of the event to truncate to; this event and all later events are removed.
             *
             * @returns Number of events that were removed by the truncation.
             */
            truncate: async (params: HistoryTruncateRequest): Promise<HistoryTruncateResult> =>
                connection.sendRequest("session.history.truncate", { sessionId, ...params }),
            /**
             * Lists the user turns that the session can rewind to. Never rejects for a busy session: rewind reads need the session's file-change captures to be settled, so a session that still holds active work answers with `unavailableReason: "session-busy"` and no points, which the caller can retry.
             *
             * @returns Rewind points and file-change-tracking availability for the session.
             */
            listRewindPoints: async (): Promise<HistoryListRewindPointsResult> =>
                connection.sendRequest("session.history.listRewindPoints", { sessionId }),
            /**
             * Previews the files that a conversation-and-files rewind would restore.
             *
             * @param params Event boundary to preview for conversation-and-files rewind.
             *
             * @returns Files and aggregate changes for a prospective rewind.
             */
            previewRewind: async (params: HistoryPreviewRewindRequest): Promise<HistoryPreviewRewindResult> =>
                connection.sendRequest("session.history.previewRewind", { sessionId, ...params }),
            /**
             * Rewinds the session conversation, optionally restoring files changed by the discarded turns. Not crash-atomic: file restore and conversation truncation are separate stores, applied in that order, so a process crash between them can leave the workspace rewound while the conversation still contains the discarded turns. There is no recovery journal; re-running the same rewind is the recovery path for a crash before truncation lands, since file restore is idempotent (already-restored files are reported as skipped) and truncation is re-derived from the still-retained boundary event. After truncation lands that boundary no longer exists, so the same request is rejected; the only stage that can still be outstanding is snapshot pruning, whose failure leaves orphan snapshots the capture store tolerates. The reverse inconsistency cannot occur, because truncation is never applied before file restore succeeds.
             *
             * @param params Boundary and mode for rewinding session history.
             *
             * @returns Structured outcome of a rewind request.
             */
            rewind: async (params: HistoryRewindRequest): Promise<HistoryRewindResult> =>
                connection.sendRequest("session.history.rewind", { sessionId, ...params }),
            /**
             * Cancels any in-progress background compaction on a local session.
             *
             * @returns Indicates whether an in-progress background compaction was cancelled.
             */
            cancelBackgroundCompaction: async (): Promise<HistoryCancelBackgroundCompactionResult> =>
                connection.sendRequest("session.history.cancelBackgroundCompaction", { sessionId }),
            /**
             * Aborts any in-progress manual compaction on a local session.
             *
             * @returns Indicates whether an in-progress manual compaction was aborted.
             */
            abortManualCompaction: async (): Promise<HistoryAbortManualCompactionResult> =>
                connection.sendRequest("session.history.abortManualCompaction", { sessionId }),
            /**
             * Produces a markdown summary of the session's conversation context for hand-off scenarios.
             *
             * @returns Markdown summary of the conversation context (empty when not available).
             */
            summarizeForHandoff: async (): Promise<HistorySummarizeForHandoffResult> =>
                connection.sendRequest("session.history.summarizeForHandoff", { sessionId }),
            /**
             * Clears the session's conversation history, keeping only system and developer messages, and seeds the fresh context window with a first user message. Must be called from inside a tool handler: the clear has to drop the results of the tool calls its wipe orphans, and it rejects when no tool call is in flight.
             *
             * @param params Parameters for clearing the conversation and seeding the window that replaces it.
             *
             * @returns What a successful clear removed. A clear that could not be applied rejects instead of reporting a count.
             */
            clearContext: async (params: HistoryClearContextRequest): Promise<HistoryClearContextResult> =>
                connection.sendRequest("session.history.clearContext", { sessionId, ...params }),
        },
        /** @experimental */
        queue: {
            /**
             * Returns the local session's pending user-facing queued items and steering messages.
             *
             * @returns Snapshot of the session's pending queued items and immediate-steering messages.
             */
            pendingItems: async (): Promise<QueuePendingItemsResult> =>
                connection.sendRequest("session.queue.pendingItems", { sessionId }),
            /**
             * Moves an addressable queued item to a public visible position.
             *
             * @param params Parameters for moving a queued item by stable id.
             *
             * @returns Result of moving a queued item.
             */
            moveItem: async (params: QueueMoveItemRequest): Promise<QueueMoveItemResult> =>
                connection.sendRequest("session.queue.moveItem", { sessionId, ...params }),
            /**
             * Inserts a new queued message at a public visible position.
             *
             * @param params Parameters for inserting a queued message at a public visible position.
             *
             * @returns Result of inserting a queued message.
             */
            insertAt: async (params: QueueInsertAtRequest): Promise<QueueInsertAtResult> =>
                connection.sendRequest("session.queue.insertAt", { sessionId, ...params }),
            /**
             * Removes an addressable queued item by its stable id.
             *
             * @param params Parameters for removing a queued item by stable id.
             *
             * @returns Result of removing a queued item.
             */
            removeAt: async (params: QueueRemoveAtRequest): Promise<QueueRemoveAtResult> =>
                connection.sendRequest("session.queue.removeAt", { sessionId, ...params }),
            /**
             * Updates the text of an addressable single-message queue item.
             *
             * @param params Parameters for editing a single queued message.
             *
             * @returns Result of editing a queued message.
             */
            updateText: async (params: QueueUpdateTextRequest): Promise<QueueUpdateTextResult> =>
                connection.sendRequest("session.queue.updateText", { sessionId, ...params }),
            /**
             * Duplicates an addressable queued item immediately after its source.
             *
             * @param params Parameters for duplicating a queued item.
             *
             * @returns Result of duplicating a queued item.
             */
            duplicateAt: async (params: QueueDuplicateAtRequest): Promise<QueueDuplicateAtResult> =>
                connection.sendRequest("session.queue.duplicateAt", { sessionId, ...params }),
            /**
             * Acquires or releases the queued-lane drain pause.
             *
             * @param params Parameters for acquiring or releasing the queued-lane drain pause. Acquisition is exclusive and non-idempotent: `paused: true` against an already-paused session fails with `queue_already_paused`. The pause is never released automatically — it is not tied to the caller's lifetime, so a client that exits without sending `paused: false` leaves the lane frozen. Release is unowned: `paused: false` clears the pause for any caller, including one that never acquired it.
             */
            setDrainPaused: async (params: QueueSetDrainPausedRequest): Promise<void> =>
                connection.sendRequest("session.queue.setDrainPaused", { sessionId, ...params }),
            /**
             * Moves an addressable queued message into the live turn's steering lane.
             *
             * @param params Parameters for steering a queued message into a live turn.
             *
             * @returns Result of trying to steer a queued message into a live turn.
             */
            sendNow: async (params: QueueSendNowRequest): Promise<QueueSendNowResult> =>
                connection.sendRequest("session.queue.sendNow", { sessionId, ...params }),
            /**
             * Removes the most recently queued user-facing item (LIFO).
             *
             * @returns Indicates whether a user-facing pending item was removed.
             */
            removeMostRecent: async (): Promise<QueueRemoveMostRecentResult> =>
                connection.sendRequest("session.queue.removeMostRecent", { sessionId }),
            /**
             * Clears all pending queued items on the local session.
             */
            clear: async (): Promise<void> =>
                connection.sendRequest("session.queue.clear", { sessionId }),
        },
        /** @experimental */
        eventLog: {
            /**
             * Reads a batch of session events from a cursor, optionally waiting for new events. Supports tail-first reads via `direction: backward`.
             *
             * @param params Cursor, batch size, and optional long-poll/filter parameters for reading session events.
             *
             * @returns Batch of session events returned by a read, with cursor and continuation metadata.
             */
            read: async (params: EventLogReadRequest): Promise<EventsReadResult> =>
                connection.sendRequest("session.eventLog.read", { sessionId, ...params }),
            /**
             * Returns a snapshot of the current tail cursor without consuming events.
             *
             * @returns Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
             */
            tail: async (): Promise<EventLogTailResult> =>
                connection.sendRequest("session.eventLog.tail", { sessionId }),
            /**
             * Registers consumer interest in an event type for runtime gating purposes.
             *
             * @param params Event type to register consumer interest for, used by runtime gating logic.
             *
             * @returns Opaque handle representing an event-type interest registration.
             */
            registerInterest: async (params: RegisterEventInterestParams): Promise<RegisterEventInterestResult> =>
                connection.sendRequest("session.eventLog.registerInterest", { sessionId, ...params }),
            /**
             * Releases a consumer's previously-registered interest in an event type.
             *
             * @param params Opaque handle previously returned by `registerInterest` to release.
             *
             * @returns Indicates whether the operation succeeded.
             */
            releaseInterest: async (params: ReleaseEventInterestParams): Promise<EventLogReleaseInterestResult> =>
                connection.sendRequest("session.eventLog.releaseInterest", { sessionId, ...params }),
        },
        /** @experimental */
        usage: {
            /**
             * Gets accumulated usage metrics for the session.
             *
             * @returns Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
             */
            getMetrics: async (): Promise<UsageGetMetricsResult> =>
                connection.sendRequest("session.usage.getMetrics", { sessionId }),
        },
        /** @experimental */
        limitPrediction: {
            /**
             * Predicts an AI-credit session limit for the session's resolved model. Returns an unavailable result instead of falling back when the current model is unresolved auto.
             *
             * @param params Parameters for predicting an AI-credit session limit. Omitting `modelId` uses the session's currently selected model.
             *
             * @returns Prediction result. Available results include prediction details; unavailable results include an explicit reason.
             */
            predict: async (params?: SessionLimitPredictionPredictRequest): Promise<SessionLimitPredictionResult> =>
                connection.sendRequest("session.limitPrediction.predict", { sessionId, ...params }),
        },
        /** @experimental */
        remote: {
            /**
             * Enables remote session export or steering.
             *
             * @param params Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
             *
             * @returns GitHub URL for the session and a flag indicating whether remote steering is enabled.
             */
            enable: async (params: RemoteEnableRequest): Promise<RemoteEnableResult> =>
                connection.sendRequest("session.remote.enable", { sessionId, ...params }),
            /**
             * Disables remote session export and steering.
             */
            disable: async (): Promise<void> =>
                connection.sendRequest("session.remote.disable", { sessionId }),
            /**
             * Persists a remote-steerability change emitted by the host as a session event.
             *
             * @param params New remote-steerability state to persist as a `session.remote_steerable_changed` event.
             *
             * @returns Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
             */
            notifySteerableChanged: async (params: RemoteNotifySteerableChangedRequest): Promise<RemoteNotifySteerableChangedResult> =>
                connection.sendRequest("session.remote.notifySteerableChanged", { sessionId, ...params }),
        },
        /** @experimental */
        visibility: {
            /**
             * Returns the session's current Mission Control sharing status and shareable GitHub URL. Reflects whether the synced session is visible to repository readers ("repo") or restricted to its creator and collaborators ("unshared").
             *
             * @returns Current sharing status and shareable GitHub URL for a session.
             */
            get: async (): Promise<VisibilityGetResult> =>
                connection.sendRequest("session.visibility.get", { sessionId }),
            /**
             * Sets the session's Mission Control sharing status, controlling whether the synced session is visible to repository readers. Returns the effective status and shareable GitHub URL after the change.
             *
             * @param params Desired sharing status for the session.
             *
             * @returns Effective sharing status and shareable GitHub URL after updating session visibility.
             */
            set: async (params: VisibilitySetRequest): Promise<VisibilitySetResult> =>
                connection.sendRequest("session.visibility.set", { sessionId, ...params }),
        },
        /** @experimental */
        schedule: {
            /**
             * Lists the session's currently active scheduled prompts.
             *
             * @returns Snapshot of the currently active recurring prompts for this session.
             */
            list: async (): Promise<ScheduleList> =>
                connection.sendRequest("session.schedule.list", { sessionId }),
            /**
             * Removes a scheduled prompt by id.
             *
             * @param params Identifier of the scheduled prompt to remove.
             *
             * @returns Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
             */
            stop: async (params: ScheduleStopRequest): Promise<ScheduleStopResult> =>
                connection.sendRequest("session.schedule.stop", { sessionId, ...params }),
        },
    };
}

/**
 * Create typed session-scoped RPC methods that are part of the SDK's internal
 * surface. Not exported on the public client API.
 * @internal
 */
export function createInternalSessionRpc(connection: MessageConnection, sessionId: string) {
    return {
        /**
         * Queues or sends an internal system notification to the session according to its passive policy.
         *
         * @param params Internal request for sending a system notification.
         *
         * @experimental
         */
        sendSystemNotification: async (params: SendSystemNotificationRequest): Promise<void> =>
            connection.sendRequest("session.sendSystemNotification", { sessionId, ...params }),
        /** @experimental */
        gitHubAuth: {
            /**
             * Gets the current authentication information for internal session hosts.
             *
             * @returns Current authentication information, or null when no authentication is active.
             */
            getCurrentAuthInfo: async (): Promise<AuthIdentity | undefined> =>
                connection.sendRequest("session.gitHubAuth.getCurrentAuthInfo", { sessionId }),
            /**
             * Gets all authentication accounts available to the internal session host.
             *
             * @returns Authentication accounts available to the internal session host.
             */
            getAllAuthAvailable: async (): Promise<SessionGitHubAuthGetAllAuthAvailableResult> =>
                connection.sendRequest("session.gitHubAuth.getAllAuthAvailable", { sessionId }),
            /**
             * Refreshes Copilot account metadata for the current authentication.
             *
             * @returns Current authentication information, or null when no authentication is active.
             */
            refreshCopilotUser: async (): Promise<AuthIdentity | undefined> =>
                connection.sendRequest("session.gitHubAuth.refreshCopilotUser", { sessionId }),
            /**
             * Logs in a GitHub user through the internal session host.
             *
             * @param params Internal GitHub login parameters.
             *
             * @returns Authentication credentials accepted only at native protocol ingress. Runtime outputs use credential-free `AuthIdentity` metadata.
             */
            login: async (params: SessionAuthLoginRequest): Promise<AuthInfo> =>
                connection.sendRequest("session.gitHubAuth.login", { sessionId, ...params }),
            /**
             * Switches the session to another available authentication.
             *
             * @param params Parameters for switching the session's active authentication.
             */
            switchToAuth: async (params: SessionAuthSwitchRequest): Promise<void> =>
                connection.sendRequest("session.gitHubAuth.switchToAuth", { sessionId, ...params }),
            /**
             * Logs out the session's current GitHub authentication.
             *
             * @returns Whether the current authentication was logged out.
             */
            logout: async (): Promise<SessionGitHubAuthLogoutResult> =>
                connection.sendRequest("session.gitHubAuth.logout", { sessionId }),
            /**
             * Logs out a specific GitHub authentication.
             *
             * @param params Parameters identifying a GitHub authentication to log out.
             *
             * @returns Whether the requested authentication was logged out.
             */
            logoutUser: async (params: SessionAuthLogoutUserRequest): Promise<SessionGitHubAuthLogoutUserResult> =>
                connection.sendRequest("session.gitHubAuth.logoutUser", { sessionId, ...params }),
            /**
             * Gets validation errors from the most recent authentication attempt.
             *
             * @returns Validation errors from the most recent authentication attempt.
             */
            lastAuthErrors: async (): Promise<AuthValidationErrors> =>
                connection.sendRequest("session.gitHubAuth.lastAuthErrors", { sessionId }),
        },
        /** @experimental */
        canvas: {
            /** @experimental */
            provider: {
                /**
                 * Registers an internal canvas provider connection and its contributions.
                 *
                 * @param params Internal canvas provider registration parameters.
                 */
                register: async (params: CanvasProviderRegisterRequest): Promise<void> =>
                    connection.sendRequest("session.canvas.provider.register", { sessionId, ...params }),
                /**
                 * Unregisters an internal canvas provider connection.
                 *
                 * @param params Internal canvas provider unregistration parameters.
                 */
                unregister: async (params: CanvasProviderUnregisterRequest): Promise<void> =>
                    connection.sendRequest("session.canvas.provider.unregister", { sessionId, ...params }),
            },
        },
        /** @experimental */
        factory: {
            /**
             * Internal tool-originated factory invocation.
             *
             * @param params Internal parameters for invoking a registered factory from a tool.
             *
             * @returns Complete current or terminal factory run envelope.
             */
            runFromTool: async (params: FactoryToolRunRequest): Promise<FactoryRunResult> =>
                connection.sendRequest("session.factory.runFromTool", { sessionId, ...params }),
            /**
             * Internal tool-originated factory resume.
             *
             * @param params Internal parameters for resuming a factory run from a tool.
             *
             * @returns Resolved persisted factory identity and resumed run envelope.
             */
            resumeFromTool: async (params: FactoryToolResumeRequest): Promise<FactoryResumeResult> =>
                connection.sendRequest("session.factory.resumeFromTool", { sessionId, ...params }),
        },
        /** @experimental */
        model: {
            /**
             * Resolves and applies organization-managed and repository model overlays.
             *
             * @param params Managed, repository, and CLI model overrides to overlay onto the session at startup.
             *
             * @returns The model identifier active on the session after the switch.
             */
            applyStartupOverlay: async (params: ModelApplyStartupOverlayRequest): Promise<ModelSwitchToResult> =>
                connection.sendRequest("session.model.applyStartupOverlay", { sessionId, ...params }),
        },
        /** @experimental */
        mcp: {
            /**
             * Reloads MCP server connections for the session with an explicit host-provided configuration.
             *
             * @param params Opaque MCP reload configuration.
             *
             * @returns MCP server startup filtering result.
             */
            reloadWithConfig: async (params: McpReloadWithConfigRequest): Promise<McpStartServersResult> =>
                connection.sendRequest("session.mcp.reloadWithConfig", { sessionId, ...params }),
            /**
             * Configures the built-in GitHub MCP server for the session's current auth context.
             *
             * @param params Credential-free authentication identity used to configure GitHub MCP.
             *
             * @returns Result of configuring GitHub MCP.
             */
            configureGitHub: async (params: McpConfigureGitHubRequest): Promise<McpConfigureGitHubResult> =>
                connection.sendRequest("session.mcp.configureGitHub", { sessionId, ...params }),
            /**
             * Registers a pre-connected external MCP client (e.g. IDE) on the session's host. The caller retains lifecycle ownership of the client and transport. Marked internal because the `client` and `transport` arguments are in-process MCP SDK instances that cannot be serialized across the JSON-RPC boundary; once the CLI moves on top of the SDK, external clients will be expressed as transport configs the runtime can construct itself.
             *
             * @param params Registration parameters for an external MCP client.
             */
            registerExternalClient: async (params: McpRegisterExternalClientRequest): Promise<void> =>
                connection.sendRequest("session.mcp.registerExternalClient", { sessionId, ...params }),
            /**
             * Unregisters a previously registered external MCP client by server name. Marked internal as the paired companion of `registerExternalClient`: only in-process callers that registered a client this way can meaningfully unregister it. Disappears alongside `registerExternalClient`: once external clients are described to the runtime as config rather than handed in as instances, lifecycle (including deregistration) is owned entirely by the runtime.
             *
             * @param params Server name identifying the external client to remove.
             */
            unregisterExternalClient: async (params: McpUnregisterExternalClientRequest): Promise<void> =>
                connection.sendRequest("session.mcp.unregisterExternalClient", { sessionId, ...params }),
        },
        /** @experimental */
        commands: {
            /**
             * Finalizes persistence associated with a client-applied slash-command effect.
             *
             * @param params The pending slash-command invocation effect to finalize, plus whether the host applied or cancelled it.
             *
             * @returns Whether finalizing the invocation effect succeeded, and the failure reason when it did not.
             */
            finalizeInvocationEffect: async (params: CommandsFinalizeInvocationEffectRequest): Promise<CommandsFinalizeInvocationEffectResult> =>
                connection.sendRequest("session.commands.finalizeInvocationEffect", { sessionId, ...params }),
        },
        /** @experimental */
        settings: {
            /**
             * Returns a redacted snapshot of session runtime settings, with secrets and raw feature flags excluded. Internal: the runtime settings shape is a runtime-internal surface and is deliberately kept out of the public SDK, because consumers should not depend on the runtime's internal settings layout. It remains callable in-process and is expected to be reworked as the runtime internals are consolidated.
             *
             * @returns Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
             */
            snapshot: async (): Promise<SessionSettingsSnapshot> =>
                connection.sendRequest("session.settings.snapshot", { sessionId }),
            /**
             * Evaluates a named Rust-owned settings predicate without exposing raw feature flags. Internal: the raw feature-flag names and composition are runtime-internal, so this predicate-evaluation helper is kept out of the public SDK surface and is callable in-process only.
             *
             * @param params Named Rust-owned settings predicate to evaluate for this session.
             *
             * @returns Result of evaluating a Rust-owned settings predicate.
             */
            evaluatePredicate: async (params: SessionSettingsEvaluatePredicateRequest): Promise<SessionSettingsEvaluatePredicateResult> =>
                connection.sendRequest("session.settings.evaluatePredicate", { sessionId, ...params }),
        },
        /** @experimental */
        queue: {
            /**
             * Returns the internal native queue snapshot for in-process session orchestration.
             *
             * @returns Internal snapshot of native queue state for local session orchestration.
             */
            snapshot: async (): Promise<QueueSnapshotResult> =>
                connection.sendRequest("session.queue.snapshot", { sessionId }),
            /**
             * Reports whether the local session has native queued work pending.
             *
             * @returns Whether the native queue has pending work.
             */
            hasPending: async (): Promise<QueueHasPendingResult> =>
                connection.sendRequest("session.queue.hasPending", { sessionId }),
            /**
             * Begins a native deferred-idle drain when background work has quiesced.
             *
             * @param params Inputs for starting a deferred-idle drain.
             *
             * @returns Whether a deferred-idle drain should run.
             */
            beginDeferredIdleDrain: async (params: QueueBeginDeferredIdleDrainRequest): Promise<QueueBeginDeferredIdleDrainResult> =>
                connection.sendRequest("session.queue.beginDeferredIdleDrain", { sessionId, ...params }),
            /**
             * Finishes a native deferred-idle drain and reports whether to drain queue work or emit idle.
             *
             * @param params Inputs for completing a deferred-idle drain.
             *
             * @returns Action selected by the native deferred-idle drain.
             */
            finishDeferredIdleDrain: async (params: QueueFinishDeferredIdleDrainRequest): Promise<QueueFinishDeferredIdleDrainResult> =>
                connection.sendRequest("session.queue.finishDeferredIdleDrain", { sessionId, ...params }),
            /**
             * Marks session.idle as deferred by native background work state.
             *
             * @param params Inputs for marking session.idle deferred in native state.
             */
            deferSessionIdle: async (params: QueueDeferSessionIdleRequest): Promise<void> =>
                connection.sendRequest("session.queue.deferSessionIdle", { sessionId, ...params }),
            /**
             * Consumes queued native system notifications matching an internal filter.
             *
             * @param params Internal filter for consuming queued system notifications.
             *
             * @returns Indicates whether a user-facing pending item was removed.
             */
            consumeSystemNotifications: async (params: QueueConsumeSystemNotificationsRequest): Promise<QueueRemoveMostRecentResult> =>
                connection.sendRequest("session.queue.consumeSystemNotifications", { sessionId, ...params }),
            /**
             * Enqueues the internal resume-pending wake item when orphan handling needs a follow-up turn.
             *
             * @returns Result of enqueueing the resume-pending wake item.
             */
            enqueueResumePending: async (): Promise<QueueEnqueueResumePendingResult> =>
                connection.sendRequest("session.queue.enqueueResumePending", { sessionId }),
            /**
             * Drains the native local-session work queue for in-process session orchestration.
             */
            process: async (): Promise<void> =>
                connection.sendRequest("session.queue.process", { sessionId }),
        },
        /** @experimental */
        schedule: {
            /**
             * Hydrates the native schedule registry from persisted session events.
             */
            hydrate: async (): Promise<void> =>
                connection.sendRequest("session.schedule.hydrate", { sessionId }),
            /**
             * Reports whether the session has an active self-paced scheduled prompt.
             *
             * @returns Whether the session currently has an active self-paced schedule.
             */
            hasSelfPaced: async (): Promise<ScheduleHasSelfPacedResult> =>
                connection.sendRequest("session.schedule.hasSelfPaced", { sessionId }),
            /**
             * Registers a relative-interval scheduled prompt.
             *
             * @param params Register a relative-interval scheduled prompt.
             *
             * @returns Result of registering or re-arming a scheduled prompt.
             */
            add: async (params: ScheduleAddRequest): Promise<ScheduleAddResult> =>
                connection.sendRequest("session.schedule.add", { sessionId, ...params }),
            /**
             * Registers a recurring cron scheduled prompt.
             *
             * @param params Register a cron scheduled prompt.
             *
             * @returns Result of registering or re-arming a scheduled prompt.
             */
            addCron: async (params: ScheduleAddCronRequest): Promise<ScheduleAddResult> =>
                connection.sendRequest("session.schedule.addCron", { sessionId, ...params }),
            /**
             * Registers an absolute-time scheduled prompt.
             *
             * @param params Register an absolute-time scheduled prompt.
             *
             * @returns Result of registering or re-arming a scheduled prompt.
             */
            addAt: async (params: ScheduleAddAtRequest): Promise<ScheduleAddResult> =>
                connection.sendRequest("session.schedule.addAt", { sessionId, ...params }),
            /**
             * Registers a self-paced scheduled prompt.
             *
             * @param params Register a self-paced scheduled prompt.
             *
             * @returns Result of registering or re-arming a scheduled prompt.
             */
            addSelfPaced: async (params: ScheduleAddSelfPacedRequest): Promise<ScheduleAddResult> =>
                connection.sendRequest("session.schedule.addSelfPaced", { sessionId, ...params }),
            /**
             * Re-arms an active self-paced scheduled prompt.
             *
             * @param params Re-arm a self-paced scheduled prompt.
             *
             * @returns Result of registering or re-arming a scheduled prompt.
             */
            rearmSelfPaced: async (params: ScheduleRearmSelfPacedRequest): Promise<ScheduleAddResult> =>
                connection.sendRequest("session.schedule.rearmSelfPaced", { sessionId, ...params }),
        },
    };
}

/** Handler for `providerToken` client session API methods. */
/** @experimental */
export interface ProviderTokenHandler {
    /**
     * Asks the SDK client to get a bearer token for a BYOK provider whose config set `hasBearerTokenProvider: true`. Session-scoped: the runtime calls it back on the connection that most recently supplied that provider's config for the session (the creating connection, or a resuming connection if the session was resumed — distinct providers may be owned by different connections), passing the provider name, and uses the returned token as the Authorization header for the outbound model request. The runtime does no caching — it calls this once per outbound request; the SDK consumer owns token acquisition, caching, and refresh.
     *
     * @param params Asks the SDK client to acquire a bearer token for a BYOK provider whose config set `hasBearerTokenProvider: true`. Issued by the runtime before each outbound model request; the runtime does no caching, so this is sent once per request.
     *
     * @returns A bearer token supplied by the SDK client for a BYOK provider. The runtime sets it as `Authorization: Bearer <token>` on the outbound request and does no caching; the SDK consumer owns token caching and refresh.
     */
    getToken(params: ProviderTokenAcquireRequest): Promise<ProviderTokenAcquireResult>;
}

/** Handler for `factory` client session API methods. */
/** @experimental */
export interface FactoryHandler {
    /**
     * Asks the owning extension connection to execute a registered factory closure.
     *
     * @param params Parameters sent to the owning extension to execute a factory closure.
     *
     * @returns Result returned by an extension factory closure.
     */
    execute(params: FactoryExecuteRequest): Promise<FactoryExecuteResult>;
    /**
     * Asks the owning extension connection to abort a running factory cooperatively.
     *
     * @param params Parameters for cooperatively aborting a factory body.
     *
     * @returns Acknowledgement that a factory request was accepted.
     */
    abort(params: FactoryAbortRequest): Promise<FactoryAckResult>;
}

/** Handler for `sessionFs` client session API methods. */
/** @experimental */
export interface SessionFsHandler {
    /**
     * Reads a file from the client-provided session filesystem.
     *
     * @param params Path of the file to read from the client-provided session filesystem.
     *
     * @returns File content as a UTF-8 string, or a filesystem error if the read failed.
     */
    readFile(params: SessionFsReadFileRequest): Promise<SessionFsReadFileResult>;
    /**
     * Writes a file in the client-provided session filesystem.
     *
     * @param params File path, content to write, and optional mode for the client-provided session filesystem.
     *
     * @returns Describes a filesystem error.
     */
    writeFile(params: SessionFsWriteFileRequest): Promise<SessionFsError | undefined>;
    /**
     * Appends content to a file in the client-provided session filesystem.
     *
     * @param params File path, content to append, and optional mode for the client-provided session filesystem.
     *
     * @returns Describes a filesystem error.
     */
    appendFile(params: SessionFsAppendFileRequest): Promise<SessionFsError | undefined>;
    /**
     * Checks whether a path exists in the client-provided session filesystem.
     *
     * @param params Path to test for existence in the client-provided session filesystem.
     *
     * @returns Indicates whether the requested path exists in the client-provided session filesystem.
     */
    exists(params: SessionFsExistsRequest): Promise<SessionFsExistsResult>;
    /**
     * Gets metadata for a path in the client-provided session filesystem.
     *
     * @param params Path whose metadata should be returned from the client-provided session filesystem.
     *
     * @returns Filesystem metadata for the requested path, or a filesystem error if the stat failed.
     */
    stat(params: SessionFsStatRequest): Promise<SessionFsStatResult>;
    /**
     * Creates a directory in the client-provided session filesystem.
     *
     * @param params Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
     *
     * @returns Describes a filesystem error.
     */
    mkdir(params: SessionFsMkdirRequest): Promise<SessionFsError | undefined>;
    /**
     * Lists entry names in a directory from the client-provided session filesystem.
     *
     * @param params Directory path whose entries should be listed from the client-provided session filesystem.
     *
     * @returns Names of entries in the requested directory, or a filesystem error if the read failed.
     */
    readdir(params: SessionFsReaddirRequest): Promise<SessionFsReaddirResult>;
    /**
     * Lists directory entries with type information from the client-provided session filesystem.
     *
     * @param params Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
     *
     * @returns Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
     */
    readdirWithTypes(params: SessionFsReaddirWithTypesRequest): Promise<SessionFsReaddirWithTypesResult>;
    /**
     * Removes a file or directory from the client-provided session filesystem.
     *
     * @param params Path to remove from the client-provided session filesystem, with options for recursive removal and force.
     *
     * @returns Describes a filesystem error.
     */
    rm(params: SessionFsRmRequest): Promise<SessionFsError | undefined>;
    /**
     * Renames or moves a path in the client-provided session filesystem.
     *
     * @param params Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
     *
     * @returns Describes a filesystem error.
     */
    rename(params: SessionFsRenameRequest): Promise<SessionFsError | undefined>;
    /**
     * Executes a SQLite query against the per-session database. Providers apply busy handling for every call.
     *
     * @param params SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database. The provider applies its SQLite busy timeout for every call.
     *
     * @returns Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
     */
    sqliteQuery(params: SessionFsSqliteQueryRequest): Promise<SessionFsSqliteQueryResult>;
    /**
     * Executes SQLite statements atomically on the provider-owned connection.
     *
     * @param params Statements to execute atomically. Providers apply busy handling for every call.
     *
     * @returns Per-statement results, or a classified transaction error.
     */
    sqliteTransaction(params: SessionFsSqliteTransactionRequest): Promise<SessionFsSqliteTransactionResult>;
    /**
     * Checks whether the per-session SQLite database already exists, without creating it.
     *
     * @param params Identifies the target session.
     *
     * @returns Indicates whether the per-session SQLite database already exists.
     */
    sqliteExists(params: SessionFsSqliteExistsRequest): Promise<SessionFsSqliteExistsResult>;
}

/** Handler for `canvas` client session API methods. */
/** @experimental */
export interface CanvasHandler {
    /**
     * Opens a canvas instance on the provider.
     *
     * @param params Canvas open parameters sent to the provider.
     *
     * @returns Canvas open result returned by the provider.
     */
    open(params: CanvasProviderOpenRequest): Promise<CanvasProviderOpenResult>;
    /**
     * Closes a canvas instance on the provider.
     *
     * @param params Canvas close parameters sent to the provider.
     */
    close(params: CanvasProviderCloseRequest): Promise<void>;
    /**
     * Invokes an action on an open canvas instance via the provider.
     *
     * @param params Canvas action invocation parameters sent to the provider.
     *
     * @returns Provider-supplied action result.
     */
    invoke(params: CanvasProviderInvokeActionRequest): Promise<CanvasActionInvokeResult>;
}

/** All client session API handler groups. */
export interface ClientSessionApiHandlers {
    providerToken?: ProviderTokenHandler;
    factory?: FactoryHandler;
    sessionFs?: SessionFsHandler;
    canvas?: CanvasHandler;
}

/**
 * Register client session API handlers on a JSON-RPC connection.
 * The server calls these methods to delegate work to the client.
 * Each incoming call includes a `sessionId` in the params; the registration
 * function uses `getHandlers` to resolve the session's handlers.
 */
export function registerClientSessionApiHandlers(
    connection: MessageConnection,
    getHandlers: (sessionId: string) => ClientSessionApiHandlers,
): void {
    connection.onRequest("providerToken.getToken", async (params: ProviderTokenAcquireRequest) => {
        const handler = getHandlers(params.sessionId).providerToken;
        if (!handler) throw new Error(`No providerToken handler registered for session: ${params.sessionId}`);
        return handler.getToken(params);
    });
    connection.onRequest("factory.execute", async (params: FactoryExecuteRequest) => {
        const handler = getHandlers(params.sessionId).factory;
        if (!handler) throw new Error(`No factory handler registered for session: ${params.sessionId}`);
        return handler.execute(params);
    });
    connection.onRequest("factory.abort", async (params: FactoryAbortRequest) => {
        const handler = getHandlers(params.sessionId).factory;
        if (!handler) throw new Error(`No factory handler registered for session: ${params.sessionId}`);
        return handler.abort(params);
    });
    connection.onRequest("sessionFs.readFile", async (params: SessionFsReadFileRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readFile(params);
    });
    connection.onRequest("sessionFs.writeFile", async (params: SessionFsWriteFileRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.writeFile(params);
    });
    connection.onRequest("sessionFs.appendFile", async (params: SessionFsAppendFileRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.appendFile(params);
    });
    connection.onRequest("sessionFs.exists", async (params: SessionFsExistsRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.exists(params);
    });
    connection.onRequest("sessionFs.stat", async (params: SessionFsStatRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.stat(params);
    });
    connection.onRequest("sessionFs.mkdir", async (params: SessionFsMkdirRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.mkdir(params);
    });
    connection.onRequest("sessionFs.readdir", async (params: SessionFsReaddirRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readdir(params);
    });
    connection.onRequest("sessionFs.readdirWithTypes", async (params: SessionFsReaddirWithTypesRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.readdirWithTypes(params);
    });
    connection.onRequest("sessionFs.rm", async (params: SessionFsRmRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.rm(params);
    });
    connection.onRequest("sessionFs.rename", async (params: SessionFsRenameRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.rename(params);
    });
    connection.onRequest("sessionFs.sqliteQuery", async (params: SessionFsSqliteQueryRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.sqliteQuery(params);
    });
    connection.onRequest("sessionFs.sqliteTransaction", async (params: SessionFsSqliteTransactionRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.sqliteTransaction(params);
    });
    connection.onRequest("sessionFs.sqliteExists", async (params: SessionFsSqliteExistsRequest) => {
        const handler = getHandlers(params.sessionId).sessionFs;
        if (!handler) throw new Error(`No sessionFs handler registered for session: ${params.sessionId}`);
        return handler.sqliteExists(params);
    });
    connection.onRequest("canvas.open", async (params: CanvasProviderOpenRequest) => {
        const handler = getHandlers(params.sessionId).canvas;
        if (!handler) throw new Error(`No canvas handler registered for session: ${params.sessionId}`);
        return handler.open(params);
    });
    connection.onRequest("canvas.close", async (params: CanvasProviderCloseRequest) => {
        const handler = getHandlers(params.sessionId).canvas;
        if (!handler) throw new Error(`No canvas handler registered for session: ${params.sessionId}`);
        return handler.close(params);
    });
    connection.onRequest("canvas.action.invoke", async (params: CanvasProviderInvokeActionRequest) => {
        const handler = getHandlers(params.sessionId).canvas;
        if (!handler) throw new Error(`No canvas handler registered for session: ${params.sessionId}`);
        return handler.invoke(params);
    });
}

/** Handler for `extensionLaunchProvider` client global API methods. */
/** @experimental */
export interface ExtensionLaunchProviderHandler {
    /**
     * Asks the registered SDK client to resolve an opaque process launch profile for one discovered extension entrypoint immediately before launch or reload. The provider must respond within 15 seconds.
     *
     * @param params A discovered extension entrypoint that the registered integrator may classify and resolve to an opaque launch profile.
     *
     * @returns The launch profile for a supported entrypoint. Omit launch when the provider does not support the entrypoint.
     */
    resolve(params: ExtensionLaunchProviderResolveRequest): Promise<ExtensionLaunchProviderResolveResult>;
}

/** Handler for `llmInference` client global API methods. */
/** @experimental */
export interface LlmInferenceHandler {
    /**
     * Announces an outbound model-layer HTTP request the runtime wants the SDK client to service. Carries the request head only; the body always follows as one or more httpRequestChunk frames keyed by the same requestId, even when the body is empty (a single chunk with end=true).
     *
     * @param params The head of an outbound model-layer HTTP request.
     *
     * @returns Acknowledgement. Returning successfully simply means the SDK accepted the start frame; it does not imply the request will succeed.
     */
    httpRequestStart(params: LlmInferenceHttpRequestStartRequest): Promise<LlmInferenceHttpRequestStartResult>;
    /**
     * Delivers a body byte range (or a cancellation signal) for a request previously announced via httpRequestStart, correlated by requestId. The runtime fires at least one chunk per request — when there is no body, a single chunk with empty data and end=true. Mid-stream the runtime may send a chunk with cancel=true to abort the request; the SDK then stops issuing httpResponseChunk frames and may emit a terminal httpResponseChunk with error set.
     *
     * @param params A request body chunk or cancellation signal.
     *
     * @returns Acknowledgement. The SDK is free to ignore the ack and treat chunk delivery as fire-and-forget.
     */
    httpRequestChunk(params: LlmInferenceHttpRequestChunkRequest): Promise<LlmInferenceHttpRequestChunkResult>;
}

/** Handler for `gitHubTelemetry` client global API methods. */
/** @experimental */
export interface GitHubTelemetryHandler {
    /**
     * Forwards a single GitHub telemetry event to a host connection that opted into telemetry forwarding during the `server.connect` handshake. Opted-in connections receive every event the runtime emits after the handshake — across all sessions, plus sessionless events (for example, `server.sendTelemetry` calls with no session id).
     *
     * @param params Payload for a `gitHubTelemetry.event` notification: a single GitHub telemetry event the runtime forwards to a host connection that opted into telemetry forwarding during the `server.connect` handshake.
     */
    event(params: GitHubTelemetryNotification): Promise<void>;
}

/** Handler for `gitHubToken` client global API methods. */
/** @experimental */
export interface GitHubTokenHandler {
    /**
     * Asks the SDK client to mint a GitHub access token for a session whose configuration supplied a GitHub token provider. The runtime acquires the initial token during bootstrap and refreshes it during expiry preflight when one hour or less remains.
     *
     * @param params Asks the SDK client to acquire a GitHub access token from an opaque callback registration.
     *
     * @returns SDK host response to a GitHub credential request.
     */
    getToken(params: GitHubTokenAcquireRequest): Promise<GitHubTokenAcquireResult>;
}

/** All client global API handler groups. */
export interface ClientGlobalApiHandlers {
    extensionLaunchProvider?: ExtensionLaunchProviderHandler;
    llmInference?: LlmInferenceHandler;
    gitHubTelemetry?: GitHubTelemetryHandler;
    gitHubToken?: GitHubTokenHandler;
}

/**
 * Register client global API handlers on a JSON-RPC connection.
 * The server calls these methods to delegate work to the client.
 * Unlike session-scoped client APIs, these methods carry no implicit
 * `sessionId` dispatch key — a single set of handlers serves the entire
 * connection.
 */
export function registerClientGlobalApiHandlers(
    connection: MessageConnection,
    handlers: ClientGlobalApiHandlers,
): void {
    connection.onRequest("extensionLaunchProvider.resolve", async (params: ExtensionLaunchProviderResolveRequest) => {
        const handler = handlers.extensionLaunchProvider;
        if (!handler) throw new Error("No extensionLaunchProvider client-global handler registered");
        return handler.resolve(params);
    });
    connection.onRequest("llmInference.httpRequestStart", async (params: LlmInferenceHttpRequestStartRequest) => {
        const handler = handlers.llmInference;
        if (!handler) throw new Error("No llmInference client-global handler registered");
        return handler.httpRequestStart(params);
    });
    connection.onRequest("llmInference.httpRequestChunk", async (params: LlmInferenceHttpRequestChunkRequest) => {
        const handler = handlers.llmInference;
        if (!handler) throw new Error("No llmInference client-global handler registered");
        return handler.httpRequestChunk(params);
    });
    connection.onNotification("gitHubTelemetry.event", async (params: GitHubTelemetryNotification) => {
        const handler = handlers.gitHubTelemetry;
        if (!handler) return;
        await handler.event(params);
    });
    connection.onRequest("gitHubToken.getToken", async (params: GitHubTokenAcquireRequest) => {
        const handler = handlers.gitHubToken;
        if (!handler) throw new Error("No gitHubToken client-global handler registered");
        return handler.getToken(params);
    });
}
