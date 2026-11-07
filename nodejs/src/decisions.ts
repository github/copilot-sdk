/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { ResponseError } from "vscode-jsonrpc/node";
import type {
    DecisionAnswer,
    DecisionContext,
    DecisionErrorData,
    DecisionErrorKind,
    DecisionModelInfo,
    DecisionsAskRequest,
    DecisionsAskResult,
    ModelsListRequest,
    createServerRpc,
} from "./generated/rpc.js";

type ServerRpc = ReturnType<typeof createServerRpc>;

/** Options shared by the decision helpers. */
export interface DecisionCallOptions {
    /**
     * Rejects the call with an `AbortError` as soon as the signal aborts. The
     * runtime does not observe the abort: the request still finishes, or hits
     * its `timeoutMs` deadline, on the runtime side.
     */
    signal?: AbortSignal;
}

/** Options for {@link CopilotDecisions.yesNo}. */
export interface YesNoOptions extends DecisionCallOptions {
    /** Account to ask as; see `DecisionsAskRequest.selectionId`. */
    selectionId?: string;
    /** GitHub token to ask as; see `DecisionsAskRequest.gitHubToken`. */
    gitHubToken?: string;
    /** Decision model id; the runtime default when omitted. */
    model?: string;
    /** Probability at or above which the answer counts as yes. Defaults to 0.5. */
    threshold?: number;
    /** Deadline in milliseconds; see `DecisionsAskRequest.timeoutMs`. */
    timeoutMs?: number;
    /** Static telemetry identifier of the calling feature. */
    purpose?: string;
}

/** Result of {@link CopilotDecisions.yesNo}. A refusal is never reported as "no". */
export type YesNoResult = { refused: false; yes: boolean; probability: number } | { refused: true };

/** A decisions result with its answers also indexed by question name. */
export type DecisionsAskResultWithNames = DecisionsAskResult & {
    byName: Readonly<Record<string, DecisionAnswer>>;
};

/**
 * Convenience wrapper over the `decisions.*` RPCs: one-off yes/no, pick-one and
 * rating questions answered by Copilot decision models.
 *
 * @experimental
 */
export class CopilotDecisions {
    constructor(private readonly rpc: () => ServerRpc) {}

    /**
     * The decision models the account can call: `models.list`'s `decisionModels`,
     * or an empty list when the runtime does not report them (the feature is off).
     */
    async listModels(
        request?: ModelsListRequest,
        options?: DecisionCallOptions
    ): Promise<DecisionModelInfo[]> {
        const list = await withAbort(() => this.rpc().models.list(request ?? {}), options?.signal);
        return list.decisionModels ?? [];
    }

    /** Asks every question in `request` and returns the answers, also indexed by name. */
    async ask(
        request: DecisionsAskRequest,
        options?: DecisionCallOptions
    ): Promise<DecisionsAskResultWithNames> {
        const result = await withAbort(() => this.rpc().decisions.ask(request), options?.signal);
        return {
            ...result,
            byName: Object.fromEntries(result.answers.map((answer) => [answer.name, answer])),
        };
    }

    /** Asks one yes/no question about `context`, which may be plain text or a structured context. */
    async yesNo(
        question: string,
        context: string | DecisionContext,
        options?: YesNoOptions
    ): Promise<YesNoResult> {
        const threshold = options?.threshold ?? 0.5;
        const result = await this.ask(
            {
                context: typeof context === "string" ? { kind: "text", text: context } : context,
                questions: [{ kind: "boolean", name: "answer", instructions: question }],
                ...(options?.selectionId !== undefined ? { selectionId: options.selectionId } : {}),
                ...(options?.gitHubToken !== undefined ? { gitHubToken: options.gitHubToken } : {}),
                ...(options?.model !== undefined ? { model: options.model } : {}),
                ...(options?.timeoutMs !== undefined ? { timeoutMs: options.timeoutMs } : {}),
                ...(options?.purpose !== undefined ? { purpose: options.purpose } : {}),
            },
            options
        );
        const answer = result.answers[0];
        if (answer?.kind !== "boolean") {
            return { refused: true };
        }
        return {
            refused: false,
            yes: answer.probability >= threshold,
            probability: answer.probability,
        };
    }
}

/**
 * The typed failure behind a rejected `decisions.*` call, or `undefined` when
 * the error did not come from one (for example an `AbortError`).
 */
export function decisionErrorData(error: unknown): DecisionErrorData | undefined {
    if (!(error instanceof ResponseError)) {
        return undefined;
    }
    const data: unknown = error.data;
    if (typeof data !== "object" || data === null) {
        return undefined;
    }
    const kind = (data as { kind?: unknown }).kind;
    // Other RPCs also put a string `kind` in error data, so only the decision kinds count.
    if (typeof kind !== "string" || !DECISION_ERROR_KINDS.has(kind)) {
        return undefined;
    }
    return data as DecisionErrorData;
}

const DECISION_ERROR_KINDS: ReadonlySet<string> = new Set<DecisionErrorKind>([
    "notEnabled",
    "noModel",
    "unauthenticated",
    "invalidRequest",
    "tooLarge",
    "upstreamProtocol",
    "rateLimited",
    "timeout",
    "unavailable",
    "overloaded",
]);

/** Shorthand for `decisionErrorData(error)?.kind`. */
export function decisionErrorKind(error: unknown): DecisionErrorKind | undefined {
    return decisionErrorData(error)?.kind;
}

function abortError(signal: AbortSignal): Error {
    const reason: unknown = signal.reason;
    if (reason instanceof Error && reason.name === "AbortError") {
        return reason;
    }
    const error = new Error("The decisions call was aborted");
    error.name = "AbortError";
    return error;
}

function withAbort<T>(call: () => Promise<T>, signal: AbortSignal | undefined): Promise<T> {
    if (!signal) {
        return call();
    }
    if (signal.aborted) {
        return Promise.reject(abortError(signal));
    }
    return new Promise<T>((resolve, reject) => {
        const onAbort = () => reject(abortError(signal));
        signal.addEventListener("abort", onAbort, { once: true });
        call().then(
            (value) => {
                signal.removeEventListener("abort", onAbort);
                resolve(value);
            },
            (error: unknown) => {
                signal.removeEventListener("abort", onAbort);
                reject(error);
            }
        );
    });
}
