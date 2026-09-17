/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ChildProcess } from "node:child_process";
import { AssistantMessageEvent, CopilotSession, SessionEvent } from "../../../src";

const CHILD_SHUTDOWN_TIMEOUT_MS = 1_000;

export async function stopChildProcess(child: ChildProcess): Promise<void> {
    if (hasChildExited(child)) {
        return;
    }

    child.kill("SIGTERM");
    if (await waitForChildExit(child, CHILD_SHUTDOWN_TIMEOUT_MS)) {
        return;
    }

    child.kill("SIGKILL");
    if (!(await waitForChildExit(child, CHILD_SHUTDOWN_TIMEOUT_MS))) {
        throw new Error("Child process did not exit after SIGKILL");
    }
}

function hasChildExited(child: ChildProcess): boolean {
    return child.exitCode !== null || child.signalCode !== null;
}

function waitForChildExit(child: ChildProcess, timeoutMs: number): Promise<boolean> {
    if (hasChildExited(child)) {
        return Promise.resolve(true);
    }

    return new Promise<boolean>((resolvePromise) => {
        let settled = false;
        const finish = (exited: boolean) => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timeout);
            child.off("exit", onExit);
            resolvePromise(exited);
        };
        const onExit = () => finish(true);
        const timeout = setTimeout(() => finish(false), timeoutMs);

        child.once("exit", onExit);
        if (hasChildExited(child)) {
            onExit();
        }
    });
}

export async function withFinalAssistantMessage(
    session: CopilotSession,
    trigger: () => Promise<unknown>
): Promise<AssistantMessageEvent> {
    type Outcome = { message: AssistantMessageEvent } | { error: Error };
    let resolveOutcome!: (outcome: Outcome) => void;
    const outcomePromise = new Promise<Outcome>((resolve) => {
        resolveOutcome = resolve;
    });
    let finalAssistantMessage: AssistantMessageEvent | undefined;

    // session.idle is ephemeral: subscribe before triggering work, not after an RPC
    // reply or a history lookup. Keep errors as values while the trigger is in flight.
    const unsubscribe = session.on((event) => {
        if (event.type === "assistant.message") {
            finalAssistantMessage = event;
        } else if (event.type === "session.idle" && event.data.mode !== "autopilot") {
            unsubscribe();
            resolveOutcome(
                finalAssistantMessage
                    ? { message: finalAssistantMessage }
                    : {
                          error: new Error(
                              "Received session.idle without a preceding assistant.message"
                          ),
                      }
            );
        } else if (event.type === "session.error") {
            unsubscribe();
            const error = new Error(event.data.message);
            error.stack = event.data.stack;
            resolveOutcome({ error });
        }
    });

    try {
        await trigger();
        const outcome = await outcomePromise;
        if ("error" in outcome) {
            throw outcome.error;
        }
        return outcome.message;
    } finally {
        unsubscribe();
    }
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

export async function waitForCondition(
    predicate: () => boolean | Promise<boolean>,
    {
        timeoutMs = 30_000,
        intervalMs = 100,
        timeoutMessage = "Timed out waiting for condition.",
    }: { timeoutMs?: number; intervalMs?: number; timeoutMessage?: string } = {}
): Promise<void> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (await predicate()) {
            return;
        }
        await new Promise((resolve) => setTimeout(resolve, intervalMs));
    }
    throw new Error(timeoutMessage);
}
