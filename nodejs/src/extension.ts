/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient } from "./client.js";
import type { CopilotSession } from "./session.js";
import {
    defaultJoinSessionPermissionHandler,
    type PermissionHandler,
    type ResumeSessionConfig,
} from "./types.js";
import type { WorkflowHandle } from "./workflow.js";

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
     * Workflow handles to register when the extension joins the session.
     *
     * @experimental Part of the experimental Dynamic Workflows surface and may
     * change or be removed in future SDK or CLI releases.
     */
    workflows?: WorkflowHandle[];
};

export type { ExtensionInfo } from "./types.js";
export {
    defineWorkflow,
    WorkflowResumeError,
    isWorkflowRunTerminal,
    type WorkflowRunOptions,
    type WorkflowResumeOptions,
    type WorkflowLimitOverrides,
    type WorkflowResumeErrorCode,
    type SessionWorkflowApi,
    type WorkflowAgentOptions,
    type WorkflowContext,
    type WorkflowDefinition,
    type WorkflowHandle,
    type JsonValue,
    type WorkflowJsonSchema,
    type WorkflowLimits,
    type WorkflowMeta,
    type WorkflowPipelineStage,
    type WorkflowStepOptions,
    type WorkflowRunResult,
    type WorkflowRunStatus,
    type WorkflowRunSummary,
    type WorkflowListRunsOptions,
    type WorkflowRunsPage,
    type WorkflowRunDetail,
    type WorkflowProgressPage,
    type WorkflowProgressLine,
    type WorkflowPhaseObservation,
    type WorkflowPhaseStatus,
    type WorkflowAgentSummary,
} from "./workflow.js";

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
    const sessionId = process.env.SESSION_ID;
    if (!sessionId) {
        throw new Error(
            "joinSession() is intended for extensions running as child processes of the Copilot CLI."
        );
    }

    const client = new CopilotClient({ _internalConnection: { kind: "parent-process" } });

    // Strip `extensionSdkPath` at runtime even though `JoinSessionConfig` omits it
    // at the type level — untyped (JS) callers can still slip it through, and
    // honoring it here would be misleading since the extension subprocess has
    // already been forked by the host with the SDK the host chose.
    const {
        extensionSdkPath: _stripped,
        workflows,
        requestedEnvironmentVariables,
        ...rest
    } = config as JoinSessionConfig & {
        extensionSdkPath?: string;
    };
    void _stripped;

    return client.resumeSessionForExtension(
        sessionId,
        {
            ...rest,
            onPermissionRequest: config.onPermissionRequest ?? defaultJoinSessionPermissionHandler,
            suppressResumeEvent: config.suppressResumeEvent ?? true,
        },
        { workflows },
        requestedEnvironmentVariables?.length ? { requestedEnvironmentVariables } : undefined
    );
}
