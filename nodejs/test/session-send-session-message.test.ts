/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFileSync } from "node:fs";
import { describe, expect, it, vi } from "vitest";
import { ResponseError } from "vscode-jsonrpc/node.js";
import {
    CopilotSession,
    SendSessionMessageError,
    type SendMode,
    type SendSessionMessageErrorCode,
    type SendSessionMessageRequest,
    type SendSessionMessageResult,
    type SessionMessageDelivery,
} from "../src/index.js";

type AssertEqual<A, B> =
    (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;

type RequestMatchesPublicContract = AssertEqual<
    SendSessionMessageRequest,
    {
        targetSessionId: string;
        content: string;
        delivery?: "immediate" | "enqueue";
    }
>;
const requestMatchesPublicContract: RequestMatchesPublicContract = true;

type ResultMatchesPublicContract = AssertEqual<
    SendSessionMessageResult,
    {
        messageId: string;
        delivery: "idle" | "steering" | "queued";
        targetDisplayName?: string;
    }
>;
const resultMatchesPublicContract: ResultMatchesPublicContract = true;

type RequestDeliveryMatchesPublicContract = AssertEqual<SendMode, "immediate" | "enqueue">;
const requestDeliveryMatchesPublicContract: RequestDeliveryMatchesPublicContract = true;

type ResultDeliveryMatchesPublicContract = AssertEqual<
    SessionMessageDelivery,
    "idle" | "steering" | "queued"
>;
const resultDeliveryMatchesPublicContract: ResultDeliveryMatchesPublicContract = true;

type ErrorCodeMatchesPublicContract = AssertEqual<
    SendSessionMessageErrorCode,
    "refused" | "not-delivered" | "ambiguous"
>;
const errorCodeMatchesPublicContract: ErrorCodeMatchesPublicContract = true;

if (false) {
    const session = null as unknown as CopilotSession;
    const base = { targetSessionId: "target-session", content: "Please inspect this." };

    // @ts-expect-error Source identity is derived from the bound session.
    void session.sendSessionMessage({ ...base, source: "source-session" });
    // @ts-expect-error Reply identity is derived by the recipient runtime.
    void session.sendSessionMessage({ ...base, reply: "source-session" });
    // @ts-expect-error Provenance is host-derived and cannot be caller supplied.
    void session.sendSessionMessage({ ...base, provenance: "authenticated" });
    // @ts-expect-error Presentation metadata is not part of the public request.
    void session.sendSessionMessage({ ...base, presentation: { label: "sender" } });
    // @ts-expect-error Continuation authority is derived by the recipient runtime.
    void session.sendSessionMessage({ ...base, continuation: ["source-session"] });
    // @ts-expect-error Message IDs are assigned by the host.
    void session.sendSessionMessage({ ...base, messageId: "caller-selected" });
}

describe("CopilotSession.sendSessionMessage", () => {
    it("omits delivery when the caller does not provide it and returns the admission result", async () => {
        const result = { messageId: "message-1", delivery: "idle" as const };
        const sendRequest = vi.fn(async () => result);
        const session = new CopilotSession("source-session", { sendRequest } as never);

        await expect(
            session.sendSessionMessage({
                targetSessionId: "target-session",
                content: "Please inspect this.",
            })
        ).resolves.toEqual(result);

        expect(sendRequest).toHaveBeenCalledExactlyOnceWith("session.sendSessionMessage", {
            sessionId: "source-session",
            targetSessionId: "target-session",
            content: "Please inspect this.",
        });
    });

    it.each(["immediate", "enqueue"] as const)(
        "forwards explicit %s delivery without rewriting it",
        async (delivery) => {
            const sendRequest = vi.fn(async () => ({
                messageId: `message-${delivery}`,
                delivery: delivery === "immediate" ? ("steering" as const) : ("queued" as const),
            }));
            const session = new CopilotSession("source-session", { sendRequest } as never);

            await session.sendSessionMessage({
                targetSessionId: "target-session",
                content: "Please inspect this.",
                delivery,
            });

            expect(sendRequest).toHaveBeenCalledExactlyOnceWith("session.sendSessionMessage", {
                sessionId: "source-session",
                targetSessionId: "target-session",
                content: "Please inspect this.",
                delivery,
            });
        }
    );

    it("does not allow untyped input to override the bound source session", async () => {
        const result = { messageId: "message-1", delivery: "idle" as const };
        const sendRequest = vi.fn(async () => result);
        const session = new CopilotSession("source-session", { sendRequest } as never);
        const params = JSON.parse(
            '{"sessionId":"forged-session","targetSessionId":"target-session","content":"Please inspect this."}'
        ) as SendSessionMessageRequest;

        await expect(session.sendSessionMessage(params)).resolves.toEqual(result);
        expect(sendRequest).toHaveBeenCalledExactlyOnceWith("session.sendSessionMessage", {
            sessionId: "source-session",
            targetSessionId: "target-session",
            content: "Please inspect this.",
        });
    });

    it.each([
        {
            kind: "session_message_refused",
            publicCode: "refused",
            runtimeCode: "target-not-active",
            messageId: "message-refused",
        },
        {
            kind: "session_message_refused",
            publicCode: "refused",
            runtimeCode: "transport-unavailable",
            messageId: "message-unsupported-platform",
        },
        {
            kind: "session_message_not_delivered",
            publicCode: "not-delivered",
            runtimeCode: "not-delivered",
            messageId: "message-not-delivered",
        },
        {
            kind: "session_message_ambiguous",
            publicCode: "ambiguous",
            runtimeCode: "ambiguous",
            messageId: "message-ambiguous",
        },
    ] as const)(
        "translates $kind to the stable $publicCode outcome",
        async ({ kind, publicCode, runtimeCode, messageId }) => {
            const responseError = new ResponseError(-32603, `send failed: ${publicCode}`, {
                kind,
                code: runtimeCode,
                messageId,
            });
            const sendRequest = vi.fn(async () => {
                throw responseError;
            });
            const session = new CopilotSession("source-session", { sendRequest } as never);

            const error = await session
                .sendSessionMessage({
                    targetSessionId: "target-session",
                    content: "Please inspect this.",
                })
                .catch((caught: unknown) => caught);

            expect(error).toBeInstanceOf(SendSessionMessageError);
            expect(error).toMatchObject({
                name: "SendSessionMessageError",
                code: publicCode,
                message: responseError.message,
                messageId,
            });
            expect(sendRequest).toHaveBeenCalledTimes(1);
        }
    );

    it.each([
        ["non-object data", "not-an-envelope"],
        ["missing kind", { code: "target-not-active" }],
        ["missing runtime code", { kind: "session_message_refused" }],
        ["unknown kind", { kind: "session_message_unknown", code: "target-not-active" }],
        ["unknown refusal code", { kind: "session_message_refused", code: "unknown-refusal" }],
        [
            "mismatched outcome and code",
            { kind: "session_message_ambiguous", code: "not-delivered" },
        ],
        [
            "non-string message ID",
            {
                kind: "session_message_ambiguous",
                code: "ambiguous",
                messageId: 42,
            },
        ],
    ])("leaves %s as the original ResponseError", async (_label, data) => {
        const responseError = new ResponseError(-32603, "raw runtime failure", data);
        const sendRequest = vi.fn(async () => {
            throw responseError;
        });
        const session = new CopilotSession("source-session", { sendRequest } as never);

        const error = await session
            .sendSessionMessage({
                targetSessionId: "target-session",
                content: "Please inspect this.",
            })
            .catch((caught: unknown) => caught);

        expect(error).toBe(responseError);
        expect(error).toBeInstanceOf(ResponseError);
        expect(error).not.toBeInstanceOf(SendSessionMessageError);
    });

    it("keeps the generated session wrapper source-bound", () => {
        const generatedRpc = readFileSync(
            new URL("../src/generated/rpc.ts", import.meta.url),
            "utf8"
        );

        expect(generatedRpc).toContain(
            "sendSessionMessage: async (params: SendSessionMessageRequest): Promise<SendSessionMessageResult> =>"
        );
        expect(generatedRpc).toContain(
            'connection.sendRequest("session.sendSessionMessage", { ...params, sessionId })'
        );
    });
});

void requestMatchesPublicContract;
void resultMatchesPublicContract;
void requestDeliveryMatchesPublicContract;
void resultDeliveryMatchesPublicContract;
void errorCodeMatchesPublicContract;
