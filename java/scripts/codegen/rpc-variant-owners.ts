/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Historical Java RPC variant superclasses at copilot-sdk
 * ad69ee168680728872434817191ef8cc5e449431 (61 public variants).
 *
 * This is ABI input, not generated output: a shared schema title cannot express
 * which Java superclass previously owned its public class name. Preserve these
 * entries when adding union memberships. For a newly shared variant, explicitly
 * record its previously published owner here; do not infer it from generation
 * order or mutable generated files. Removal/reassignment requires an intentional
 * API compatibility decision, not an automatic schema-update rewrite.
 */
export const RPC_VARIANT_OWNERS: Readonly<Record<string, string>> = {
    AgentRegistrySpawnError: "AgentRegistrySpawnResult",
    AgentRegistrySpawnRegistryTimeout: "AgentRegistrySpawnResult",
    AgentRegistrySpawnSpawned: "AgentRegistrySpawnResult",
    AgentRegistrySpawnValidationError: "AgentRegistrySpawnResult",
    ApiKeyAuthInfo: "AuthInfo",
    CopilotApiTokenAuthInfo: "AuthInfo",
    EnvAuthInfo: "AuthInfo",
    GhCliAuthInfo: "AuthInfo",
    HMACAuthInfo: "AuthInfo",
    TokenAuthInfo: "AuthInfo",
    TokenProviderAuthInfo: "AuthInfo",
    UserAuthInfo: "AuthInfo",
    CatalogAiSkillCandidate: "CatalogCandidate",
    CatalogMcpServerCandidate: "CatalogCandidate",
    CatalogCandidateSourceEmbedded: "CatalogCandidateSource",
    CatalogCandidateSourceUrl: "CatalogCandidateSource",
    CatalogAuthenticationRequiredError: "CatalogSearchResult",
    CatalogContractViolationError: "CatalogSearchResult",
    CatalogInvalidRequestError: "CatalogSearchResult",
    CatalogMalformedCardError: "CatalogSearchResult",
    CatalogNegotiationRefusedError: "CatalogSearchResult",
    CatalogNetworkFailureError: "CatalogSearchResult",
    CatalogPolicyRejectedError: "CatalogSearchResult",
    CatalogSearchSucceeded: "CatalogSearchResult",
    CatalogUnavailableError: "CatalogSearchResult",
    CatalogUnsafeRetrievalError: "CatalogSearchResult",
    CatalogUnsupportedKindError: "CatalogSearchResult",
    CatalogTrustSnapshotAbsent: "CatalogTrustSnapshot",
    CatalogTrustSnapshotCurrent: "CatalogTrustSnapshot",
    CatalogTrustSnapshotDowngraded: "CatalogTrustSnapshot",
    CatalogTrustSnapshotMalformed: "CatalogTrustSnapshot",
    CatalogTrustSnapshotRevoked: "CatalogTrustSnapshot",
    CatalogTrustSnapshotStale: "CatalogTrustSnapshot",
    CatalogTrustSnapshotUnsupported: "CatalogTrustSnapshot",
    GitHubTokenAcquireResultCancelled: "GitHubTokenAcquireResult",
    GitHubTokenAcquireResultToken: "GitHubTokenAcquireResult",
    McpOauthProbeResultAuthenticated: "McpOauthProbeResult",
    McpOauthProbeResultFailed: "McpOauthProbeResult",
    McpOauthProbeResultNeedsAuth: "McpOauthProbeResult",
    McpOauthProbeResultNoAuthRequired: "McpOauthProbeResult",
    CatalogHandleRejectedError: "McpPlanInstallResult",
    CatalogNotInstallableError: "McpPlanInstallResult",
    CatalogUnavailableTransportError: "McpPlanInstallResult",
    McpPlanInstallPlanned: "McpPlanInstallResult",
    SessionLimitPredictionResultAvailable: "SessionLimitPredictionResult",
    SessionLimitPredictionResultUnavailable: "SessionLimitPredictionResult",
    SessionsOpenAttach: "SessionsOpenParams",
    SessionsOpenCloud: "SessionsOpenParams",
    SessionsOpenCreate: "SessionsOpenParams",
    SessionsOpenHandoff: "SessionsOpenParams",
    SessionsOpenRemote: "SessionsOpenParams",
    SessionsOpenResume: "SessionsOpenParams",
    SessionsOpenResumeLast: "SessionsOpenParams",
    SlashCommandAgentPromptResult: "SlashCommandInvocationResult",
    SlashCommandCompletedResult: "SlashCommandInvocationResult",
    SlashCommandInvocationResultAddTimelineEntry: "SlashCommandInvocationResult",
    SlashCommandInvocationResultSetModel: "SlashCommandInvocationResult",
    SlashCommandInvocationResultSetPlanModel: "SlashCommandInvocationResult",
    SlashCommandInvocationResultShowDialog: "SlashCommandInvocationResult",
    SlashCommandSelectSubcommandResult: "SlashCommandInvocationResult",
    SlashCommandTextResult: "SlashCommandInvocationResult",
};
