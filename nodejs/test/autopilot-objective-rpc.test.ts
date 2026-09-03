import { describe, expect, it } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";

import { createSessionRpc, type AutopilotObjectiveGetStateResult } from "../src/generated/rpc.js";

describe("autopilot objective RPC", () => {
    it("dispatches getState and preserves canonical objective state", async () => {
        const responses: AutopilotObjectiveGetStateResult[] = [
            { state: null },
            {
                state: {
                    id: 1,
                    objective: "Ship the release",
                    status: "active",
                    turnCount: 2,
                    creditCountNanoAiu: "0",
                },
            },
            {
                state: {
                    id: 2,
                    objective: "Wait for approval",
                    status: "paused",
                    turnCount: 3,
                    pauseReason: "Approval required",
                    creditCountNanoAiu: "9007199254740993",
                    creditLimit: {
                        creditsUsed: 9.007199254740993,
                        creditsUsedNanoAiu: "9007199254740993",
                    },
                },
            },
            {
                state: {
                    id: 3,
                    objective: "Publish the SDK",
                    status: "completed",
                    turnCount: 4,
                    completionSummary: "Published",
                    creditCountNanoAiu: "9007199254740994",
                    creditLimit: {
                        credits: 2.5,
                        creditsUsed: 1.25,
                        creditsUsedNanoAiu: "1250000000",
                    },
                },
            },
        ];
        const calls: Array<{ method: string; params: unknown }> = [];
        const connection = {
            sendRequest: async (method: string, params: unknown) => {
                calls.push({ method, params });
                return responses[calls.length - 1];
            },
        } as unknown as MessageConnection;
        const rpc = createSessionRpc(connection, "session-1");

        const results = [];
        for (let index = 0; index < responses.length; index++) {
            results.push(await rpc.autopilotObjective.getState());
        }

        expect(calls).toEqual(
            responses.map(() => ({
                method: "session.autopilotObjective.getState",
                params: { sessionId: "session-1" },
            }))
        );
        expect(results[0].state).toBeNull();
        expect(results[1].state).not.toHaveProperty("pauseReason");
        expect(results[1].state).not.toHaveProperty("completionSummary");
        expect(results[1].state).not.toHaveProperty("creditLimit");
        expect(results[2].state?.status).toBe("paused");
        expect(results[2].state?.creditLimit?.credits).toBeUndefined();
        expect(results[2].state?.creditCountNanoAiu).toBe("9007199254740993");
        expect(results[3].state?.status).toBe("completed");
        expect(results[3].state?.completionSummary).toBe("Published");
        expect(results[3].state?.creditLimit?.creditsUsedNanoAiu).toBe("1250000000");
    });
});
