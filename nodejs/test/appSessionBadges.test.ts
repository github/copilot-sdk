import { describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { AppSessionBadgesExtension } from "../src/appSessionBadges.js";
import type { CopilotSession } from "../src/session.js";

interface RecordedRequest {
    method: string;
    params: unknown;
}

function createConnection() {
    const requests: RecordedRequest[] = [];
    const notifications = new Map<string, (payload: unknown) => void>();
    const disposed = vi.fn();
    const connection = {
        onNotification(method: string, handler: (payload: unknown) => void) {
            notifications.set(method, handler);
            return { dispose: disposed };
        },
        async sendRequest(method: string, params?: unknown) {
            requests.push({ method, params });
            if (method === "extensions.appSessionBadges.register") {
                notifications.get("appSessionBadges.snapshot")?.({
                    protocolVersion: 1,
                    revision: 7,
                    sessions: [
                        {
                            workspaceId: "workspace-1",
                            sessionId: "session-1",
                            repositoryPath: "C:\\src\\repo",
                            worktreePath: "C:\\src\\worktree",
                            branch: "feature",
                        },
                    ],
                });
            }
            return null;
        },
    } as unknown as MessageConnection;

    return { connection, requests, notifications, disposed };
}

describe("AppSessionBadgesExtension", () => {
    it("registers after installing the snapshot listener and retains the immediate snapshot", async () => {
        const { connection, requests } = createConnection();

        const contribution = await AppSessionBadgesExtension.register(
            {} as CopilotSession,
            connection
        );

        expect(requests).toEqual([
            {
                method: "extensions.appSessionBadges.register",
                params: undefined,
            },
        ]);
        expect(contribution.snapshot).toEqual({
            protocolVersion: 1,
            revision: 7,
            sessions: [
                {
                    workspaceId: "workspace-1",
                    sessionId: "session-1",
                    repositoryPath: "C:\\src\\repo",
                    worktreePath: "C:\\src\\worktree",
                    branch: "feature",
                },
            ],
        });
    });

    it("replays the latest snapshot and delivers later full replacements", async () => {
        const { connection, notifications } = createConnection();
        const contribution = await AppSessionBadgesExtension.register(
            {} as CopilotSession,
            connection
        );
        const handler = vi.fn();

        const unsubscribe = contribution.onSnapshot(handler);
        notifications.get("appSessionBadges.snapshot")?.({
            protocolVersion: 1,
            revision: 8,
            sessions: [],
        });
        unsubscribe();
        notifications.get("appSessionBadges.snapshot")?.({
            protocolVersion: 1,
            revision: 9,
            sessions: [],
        });

        expect(handler).toHaveBeenCalledTimes(2);
        expect(handler.mock.calls[0]![0].revision).toBe(7);
        expect(handler.mock.calls[1]![0]).toEqual({
            protocolVersion: 1,
            revision: 8,
            sessions: [],
        });
        expect(contribution.snapshot?.revision).toBe(9);
    });

    it("continues snapshot delivery when one handler throws", async () => {
        const { connection, notifications } = createConnection();
        const contribution = await AppSessionBadgesExtension.register(
            {} as CopilotSession,
            connection
        );
        const error = new Error("handler failed");
        const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
        const secondHandler = vi.fn();
        let shouldThrow = false;
        contribution.onSnapshot(() => {
            if (shouldThrow) {
                throw error;
            }
        });
        contribution.onSnapshot(secondHandler);
        secondHandler.mockClear();
        shouldThrow = true;

        notifications.get("appSessionBadges.snapshot")?.({
            protocolVersion: 1,
            revision: 8,
            sessions: [],
        });

        expect(secondHandler).toHaveBeenCalledOnce();
        expect(consoleError).toHaveBeenCalledWith(
            "App session badges snapshot handler failed",
            error
        );
        consoleError.mockRestore();
    });

    it("publishes and clears exact constrained v1 badge payloads", async () => {
        const { connection, requests } = createConnection();
        const contribution = await AppSessionBadgesExtension.register(
            {} as CopilotSession,
            connection
        );
        const target = { workspaceId: "workspace-1", sessionId: "session-1" };

        await contribution.setBadge(target, { state: "open", label: "PR available" });
        await contribution.clearBadge(target);

        expect(requests.slice(1)).toEqual([
            {
                method: "extensions.appSessionBadges.setBadge",
                params: {
                    protocolVersion: 1,
                    workspaceId: "workspace-1",
                    sessionId: "session-1",
                    badge: { state: "open", label: "PR available" },
                },
            },
            {
                method: "extensions.appSessionBadges.setBadge",
                params: {
                    protocolVersion: 1,
                    workspaceId: "workspace-1",
                    sessionId: "session-1",
                    badge: null,
                },
            },
        ]);
    });

    it("rejects unsupported states and malformed snapshots", async () => {
        const { connection, notifications } = createConnection();
        const contribution = await AppSessionBadgesExtension.register(
            {} as CopilotSession,
            connection
        );

        await expect(
            contribution.setBadge({ workspaceId: "workspace-1", sessionId: "session-1" }, {
                state: "queued",
            } as never)
        ).rejects.toThrow("Unsupported app session badge state");
        expect(() =>
            notifications.get("appSessionBadges.snapshot")?.({
                protocolVersion: 2,
                revision: 8,
                sessions: [],
            })
        ).toThrow("Unsupported app session badges protocol version");
    });

    it("disposes its notification registration", async () => {
        const { connection, disposed } = createConnection();
        const contribution = await AppSessionBadgesExtension.register(
            {} as CopilotSession,
            connection
        );

        contribution.dispose();

        expect(disposed).toHaveBeenCalledOnce();
    });
});
