/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient } from "./client.js";
import type { FactoryHandle } from "./factory.js";
import type { CopilotSession } from "./session.js";
import {
    defaultJoinSessionPermissionHandler,
    type ExtensionJoinOptions,
    type PermissionHandler,
    type ResumeSessionConfig,
} from "./types.js";

export interface ExtensionSessionConfig extends Omit<
    ResumeSessionConfig,
    "onPermissionRequest" | "extensionSdkPath"
> {
    onPermissionRequest?: PermissionHandler;
    requestedEnvironmentVariables?: string[];
    factories?: FactoryHandle[];
}

/** @internal */
export async function joinExtensionSession(
    config: ExtensionSessionConfig
): Promise<{ client: CopilotClient; session: CopilotSession }> {
    const sessionId = process.env.SESSION_ID;
    if (!sessionId) {
        throw new Error(
            "Extension entry points are intended for child processes launched by the Copilot runtime."
        );
    }

    const client = new CopilotClient({ _internalConnection: { kind: "parent-process" } });
    const {
        extensionSdkPath: _stripped,
        factories,
        requestedEnvironmentVariables,
        ...rest
    } = config as ExtensionSessionConfig & {
        extensionSdkPath?: string;
    };
    void _stripped;

    const extensionOptions: ExtensionJoinOptions | undefined = requestedEnvironmentVariables?.length
        ? { requestedEnvironmentVariables }
        : undefined;
    try {
        const session = await client.resumeSessionForExtension(
            sessionId,
            {
                ...rest,
                onPermissionRequest:
                    config.onPermissionRequest ?? defaultJoinSessionPermissionHandler,
                suppressResumeEvent: config.suppressResumeEvent ?? true,
            },
            factories,
            extensionOptions
        );
        return { client, session };
    } catch (error) {
        const cleanupErrors: unknown[] = [];
        try {
            cleanupErrors.push(...(await client.stop()));
        } catch (cleanupError) {
            cleanupErrors.push(cleanupError);
        }
        if (cleanupErrors.length > 0) {
            throw new AggregateError(
                [error, ...cleanupErrors],
                "Failed to join and stop extension session"
            );
        }
        throw error;
    }
}
