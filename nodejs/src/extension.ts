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
     * Workflow handles to register when the extension joins the session.
     *
     * @experimental Part of the experimental Dynamic Workflows surface and may
     * change or be removed in future SDK or CLI releases.
     */
    workflows?: WorkflowHandle[];
};

export type { ExtensionInfo, WorkflowLimits, WorkflowMeta } from "./types.js";
export {
    defineWorkflow,
    WorkflowRunError,
    type RunOptions,
    type SessionWorkflowApi,
    type WorkflowAgentOptions,
    type WorkflowContext,
    type WorkflowDefinition,
    type WorkflowHandle,
    type WorkflowJsonSchema,
    type WorkflowPipelineStage,
    type WorkflowStepOptions,
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
        workflows
    );
}
