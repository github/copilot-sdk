/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi } from "vitest";
import {
    CopilotSession,
    type ListMessageableSessionsRequest,
    type ListMessageableSessionsResult,
    type MessageableSession,
} from "../src/index.js";

type AssertEqual<A, B> =
    (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;

type RequestMatchesPublicContract = AssertEqual<
    ListMessageableSessionsRequest,
    {
        name?: string;
    }
>;
const requestMatchesPublicContract: RequestMatchesPublicContract = true;

type CandidateMatchesPublicContract = AssertEqual<
    MessageableSession,
    {
        sessionId: string;
        name?: string;
        summary?: string;
    }
>;
const candidateMatchesPublicContract: CandidateMatchesPublicContract = true;

type ResultMatchesPublicContract = AssertEqual<
    ListMessageableSessionsResult,
    {
        sessions: MessageableSession[];
    }
>;
const resultMatchesPublicContract: ResultMatchesPublicContract = true;

const assertRejectedListInputs = (session: CopilotSession): void => {
    // @ts-expect-error Source identity is derived from the bound session.
    void session.listMessageableSessions({ sourceSessionId: "forged" });
    // @ts-expect-error Discovery never accepts a delivery target.
    void session.listMessageableSessions({ targetSessionId: "target-session" });
};
void assertRejectedListInputs;

describe("CopilotSession.listMessageableSessions", () => {
    it("lists all candidates when no name is supplied", async () => {
        const result = {
            sessions: [
                { sessionId: "session-a", name: "Research" },
                { sessionId: "session-b", summary: "Research" },
            ],
        };
        const sendRequest = vi.fn(async () => result);
        const session = new CopilotSession("source-session", { sendRequest } as never);

        await expect(session.listMessageableSessions()).resolves.toEqual(result);
        expect(sendRequest).toHaveBeenCalledExactlyOnceWith("session.listMessageableSessions", {
            sessionId: "source-session",
        });
    });

    it("forwards the exact-name query without rewriting it", async () => {
        const result = { sessions: [{ sessionId: "session-a", name: "Research" }] };
        const sendRequest = vi.fn(async () => result);
        const session = new CopilotSession("source-session", { sendRequest } as never);

        await expect(session.listMessageableSessions({ name: "  ReSeArCh  " })).resolves.toEqual(
            result
        );
        expect(sendRequest).toHaveBeenCalledExactlyOnceWith("session.listMessageableSessions", {
            sessionId: "source-session",
            name: "  ReSeArCh  ",
        });
    });

    it("does not allow untyped input to override the bound source session", async () => {
        const result = { sessions: [] };
        const sendRequest = vi.fn(async () => result);
        const session = new CopilotSession("source-session", { sendRequest } as never);
        const params = JSON.parse(
            '{"sessionId":"forged-session","name":"Research"}'
        ) as ListMessageableSessionsRequest;

        await expect(session.listMessageableSessions(params)).resolves.toEqual(result);
        expect(sendRequest).toHaveBeenCalledExactlyOnceWith("session.listMessageableSessions", {
            sessionId: "source-session",
            name: "Research",
        });
    });
});

void requestMatchesPublicContract;
void candidateMatchesPublicContract;
void resultMatchesPublicContract;
