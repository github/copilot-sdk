/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { AssistantMessageEvent, CopilotSession, SessionEvent } from "../../../src";

export async function getFinalAssistantMessage(
    session: CopilotSession,
    { alreadyIdle = false }: { alreadyIdle?: boolean } = {}
): Promise<AssistantMessageEvent> {
    // Install the live subscription (via getFutureFinalResponse) before issuing the
    // existing-messages RPC so we don't miss events that arrive while that RPC is in flight.
    const futurePromise = getFutureFinalResponse(session);
    // We may end up returning from the existing-messages path; attach a noop handler so
    // the unawaited future-response rejection doesn't surface as an unhandled rejection.
    futurePromise.catch(() => {});

    const existing = await getExistingFinalResponse(session, alreadyIdle);
    if (existing) {
        return existing;
    }
    return futurePromise;
}

async function getExistingFinalResponse(
    session: CopilotSession,
    alreadyIdle: boolean = false
): Promise<AssistantMessageEvent | undefined> {
    const messages = await session.getEvents();
    const finalUserMessageIndex = messages.findLastIndex((m) => m.type === "user.message");
    const currentTurnMessages =
        finalUserMessageIndex < 0 ? messages : messages.slice(finalUserMessageIndex);

    const currentTurnError = currentTurnMessages.find((m) => m.type === "session.error");
    if (currentTurnError) {
        const error = new Error(currentTurnError.data.message);
        error.stack = currentTurnError.data.stack;
        throw error;
    }

    const sessionIdleMessageIndex = alreadyIdle
        ? currentTurnMessages.length
        : currentTurnMessages.findIndex((m) => m.type === "session.idle");
    if (sessionIdleMessageIndex !== -1) {
        return currentTurnMessages
            .slice(0, sessionIdleMessageIndex)
            .findLast((m) => m.type === "assistant.message") as AssistantMessageEvent | undefined;
    }

    return undefined;
}

function getFutureFinalResponse(session: CopilotSession): Promise<AssistantMessageEvent> {
    return new Promise<AssistantMessageEvent>((resolve, reject) => {
        let finalAssistantMessage: AssistantMessageEvent | undefined;
        session.on((event) => {
            if (event.type === "assistant.message") {
                finalAssistantMessage = event;
            } else if (event.type === "session.idle") {
                if (!finalAssistantMessage) {
                    reject(
                        new Error("Received session.idle without a preceding assistant.message")
                    );
                } else {
                    resolve(finalAssistantMessage);
                }
            } else if (event.type === "session.error") {
                const error = new Error(event.data.message);
                error.stack = event.data.stack;
                reject(error);
            }
        });
    });
}

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
