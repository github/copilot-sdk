/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotSession } from "./session.js";
import { type PermissionHandler, type ResumeSessionConfig } from "./types.js";
import type { FactoryHandle } from "./factory.js";
import type { AppSessionBadgesExtension } from "./appSessionBadges.js";
import { joinExtensionSession } from "./extensionSession.js";

export {
    AppSessionBadgesExtension,
    type AppSessionBadge,
    type AppSessionBadgeState,
    type AppSessionBadgeTarget,
    type AppSessionBadgeTargetIdentity,
    type AppSessionBadgeUpdate,
    type AppSessionBadgesSnapshot,
    type AppSessionBadgesSnapshotHandler,
} from "./appSessionBadges.js";

export {
    Canvas,
    CanvasError,
    createCanvas,
    type CanvasAction,
    type CanvasDeclaration,
    type CanvasHostContext,
    type CanvasJsonSchema,
    type CanvasOptions,
} from "./canvas.js";

export type JoinSessionConfig = Omit<
    ResumeSessionConfig,
    "onPermissionRequest" | "extensionSdkPath"
> & {
    onPermissionRequest?: PermissionHandler;
    /**
     * Names of sensitive environment variables this extension needs, such as
     * `"GITHUB_TOKEN"`.
     *
     * The Copilot CLI strips sensitive variables from every extension process
     * before it starts, so an extension that needs one must ask for it by name.
     * The CLI prompts the user with the extension's name and the exact list of
     * variables requested. On approval the granted values are written into this
     * process's `process.env` before {@link joinSession} resolves, so they are
     * readable afterwards. On denial the join rejects and the extension does not
     * load, so its tools never reach the model.
     *
     * An approval is remembered against the exact set of names the user saw, so
     * asking for an additional variable later prompts again. Names that are unset
     * or that the CLI does not filter from extensions are not prompted for. An
     * empty list means the same as omitting the option: nothing is requested.
     *
     * Requires a Copilot CLI that supports extension environment access; older
     * CLIs ignore the request and grant nothing.
     *
     * @example
     * ```typescript
     * const session = await joinSession({
     *     requestedEnvironmentVariables: ["GITHUB_TOKEN"],
     * });
     * const token = process.env.GITHUB_TOKEN;
     * ```
     */
    requestedEnvironmentVariables?: string[];
    /**
     * Factory handles to register when the extension joins the session.
     *
     * @experimental Part of the experimental Agent Factories surface and may
     * change or be removed in future SDK or CLI releases.
     */
    factories?: FactoryHandle[];
};

export type { ExtensionInfo, FactoryLimits, FactoryMeta } from "./types.js";
export {
    defineFactory,
    FactoryResumeError,
    isFactoryRunTerminal,
    type RunOptions,
    type ResumeOptions,
    type FactoryResumeErrorCode,
    type SessionFactoryApi,
    type FactoryAgentOptions,
    type FactoryContext,
    type FactoryDefinition,
    type FactoryHandle,
    type FactoryJsonSchema,
    type JsonValue,
    type FactoryPipelineStage,
    type FactoryStepOptions,
    type FactoryRunResult,
    type FactoryRunStatus,
    type FactoryRunSummary,
    type FactoryListRunsOptions,
    type FactoryRunsPage,
    type FactoryRunDetail,
    type FactoryProgressPage,
    type FactoryProgressLine,
    type FactoryPhaseObservation,
    type FactoryPhaseStatus,
    type FactoryAgentSummary,
} from "./factory.js";

/**
 * Joins the current foreground session.
 *
 * @param config - Configuration to add to the session
 * @returns A promise that resolves with the joined session
 *
 * @example
 * ```typescript
 * import { joinSession } from "@github/copilot-sdk/extension";
 *
 * const session = await joinSession({ tools: [myTool] });
 * ```
 */
export async function joinSession(config: JoinSessionConfig = {}): Promise<CopilotSession> {
    const { session } = await joinExtensionSession(config);
    return session;
}

/**
 * Joins the retained app session and explicitly registers a session-badge contribution.
 *
 * The app owns the hidden session lifecycle and supplies full replacement
 * snapshots containing only sessions eligible for extension-provided badges.
 */
export async function joinAppSessionBadges(
    config: JoinSessionConfig = {}
): Promise<AppSessionBadgesExtension> {
    const { client, session } = await joinExtensionSession(config);
    try {
        return await client.registerAppSessionBadges(session);
    } catch (error) {
        try {
            await session.disconnect();
        } catch (cleanupError) {
            throw new AggregateError(
                [error, cleanupError],
                "Failed to register app session badges and disconnect the extension session"
            );
        }
        throw error;
    }
}
