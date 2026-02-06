/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotSession, SessionEvent } from "../../../src";

export async function retry(
    message: string,
    fn: () => Promise<void>,
    maxTries: number = 100,
    delay: number = 100
) {
    let failedAttempts = 0;
    while (true) {
        try {
            await fn();
            return;
        } catch (error: unknown) {
            failedAttempts++;
            if (failedAttempts >= maxTries) {
                throw new Error(
                    `Failed to ${message} after ${maxTries} attempts\n${formatError(error)}`
                );
            }
            await new Promise((resolve) => setTimeout(resolve, delay));
        }
    }
}

export function formatError(error: unknown): string {
    if (error instanceof Error) {
        return String(error);
    } else if (typeof error === "object" && error !== null) {
        try {
            return JSON.stringify(error);
        } catch {
            return "[object with circular reference]";
        }
    } else {
        return String(error);
    }
}

export function getNextEventOfType(
    session: CopilotSession,
    eventType: SessionEvent["type"]
): Promise<SessionEvent> {
    return new Promise<SessionEvent>((resolve, reject) => {
        const unsubscribe = session.on((event) => {
            if (event.type === eventType) {
                unsubscribe();
                resolve(event);
            } else if (event.type === "session.error") {
                unsubscribe();
                reject(new Error(`${event.data.message}\n${event.data.stack}`));
            }
        });
    });
}
