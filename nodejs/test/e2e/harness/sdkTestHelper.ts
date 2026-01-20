/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { AssistantMessageEvent, CopilotSession, SessionEvent } from "../../../src";

export async function getFinalAssistantMessage(
    session: CopilotSession
): Promise<AssistantMessageEvent> {
    // We don't know whether the answer has already arrived or not, so race both possibilities
    return new Promise<AssistantMessageEvent>(async (resolve, reject) => {
        getFutureFinalResponse(session).then(resolve).catch(reject);
        getExistingFinalResponse(session)
            .then((msg) => {
                if (msg) {
                    resolve(msg);
                }
            })
            .catch(reject);
    });
}

function getExistingFinalResponse(
    session: CopilotSession
): Promise<AssistantMessageEvent | undefined> {
    return new Promise<AssistantMessageEvent | undefined>(async (resolve, reject) => {
        const messages = await session.getMessages();
        const finalUserMessageIndex = messages.findLastIndex((m) => m.type === "user.message");
        const currentTurnMessages =
            finalUserMessageIndex < 0 ? messages : messages.slice(finalUserMessageIndex);

        const currentTurnError = currentTurnMessages.find((m) => m.type === "session.error");
        if (currentTurnError) {
            const error = new Error(currentTurnError.data.message);
            error.stack = currentTurnError.data.stack;
            reject(error);
            return;
        }

        const sessionIdleMessageIndex = currentTurnMessages.findIndex(
            (m) => m.type === "session.idle"
        );
        if (sessionIdleMessageIndex !== -1) {
            const lastAssistantMessage = currentTurnMessages
                .slice(0, sessionIdleMessageIndex)
                .findLast((m) => m.type === "assistant.message");
            resolve(lastAssistantMessage as AssistantMessageEvent | undefined);
            return;
        }

        resolve(undefined);
    });
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
