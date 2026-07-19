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
import type { FactoryHandle } from "./factory.js";

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
    FactoryRunError,
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
        factories,
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
        factories
    );
}
