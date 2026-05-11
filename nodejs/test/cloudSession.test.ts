import { afterEach, describe, expect, it, vi } from "vitest";
import {
    CloudSessionError,
    CopilotClient,
    MissionControlCommandType,
    type CloudSessionEvent,
    type MissionControlTask,
} from "../src/index.js";

const task: MissionControlTask = {
    id: "task-1",
    name: "Cloud task",
    state: "running",
    status: "ready",
    creator_id: 1,
    owner_id: 2,
    repo_id: 3,
    session_count: 1,
    created_at: "2026-05-11T10:00:00.000Z",
    updated_at: "2026-05-11T10:01:00.000Z",
    sessions: [
        {
            id: "mc-session-1",
            task_id: "task-1",
            state: "running",
            created_at: "2026-05-11T10:00:30.000Z",
            updated_at: "2026-05-11T10:00:30.000Z",
            owner_id: 2,
            repo_id: 3,
        },
    ],
};

const requestedEvent: CloudSessionEvent = {
    id: "event-1",
    parentId: null,
    timestamp: "2026-05-11T10:00:00.000Z",
    type: "session.requested",
};

const idleEvent: CloudSessionEvent = {
    id: "event-2",
    parentId: "event-1",
    timestamp: "2026-05-11T10:00:01.000Z",
    type: "session.idle",
    data: {},
};

describe("Cloud sessions", () => {
    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it("creates a Mission Control cloud task and attaches to task events", async () => {
        const fetchMock = mockFetch([
            jsonResponse(task),
            jsonResponse({ events: [requestedEvent] }),
        ]);
        const progress: string[] = [];
        const client = new CopilotClient({
            autoStart: false,
            gitHubToken: "token-1",
            env: {
                COPILOT_MC_BASE_URL: "https://mc.test/agents",
                COPILOT_MC_FRONTEND_URL: "https://github.test",
            },
        });

        const session = await client.createCloudSession({
            repository: { owner: "github", name: "copilot-sdk", branch: "main" },
            initialEventTimeoutMs: 0,
            onProgress: (event) => progress.push(event.phase),
        });

        expect(session.metadata).toMatchObject({
            taskId: "task-1",
            missionControlSessionId: "mc-session-1",
            frontendUrl: "https://github.test/copilot/tasks/task-1",
            repository: { owner: "github", name: "copilot-sdk", branch: "main" },
            state: "running",
            status: "ready",
        });
        expect(session.getMessages()).toEqual([requestedEvent]);
        expect(progress).toEqual([
            "creating_task",
            "provisioning_sandbox",
            "waiting_for_session",
            "connected",
        ]);

        expect(fetchMock).toHaveBeenNthCalledWith(
            1,
            "https://mc.test/agents/tasks",
            expect.objectContaining({
                method: "POST",
                headers: expect.objectContaining({
                    Authorization: "Bearer token-1",
                    "X-Copilot-Agent-Slug": "copilot-developer-sandbox",
                }),
                body: JSON.stringify({ repositories: [{ owner: "github", name: "copilot-sdk" }] }),
            })
        );
        expect(fetchMock).toHaveBeenNthCalledWith(
            2,
            "https://mc.test/agents/tasks/task-1/events",
            expect.objectContaining({ method: "GET" })
        );

        await session.disconnect();
    });

    it("creates a repo-less cloud task when owner is provided", async () => {
        const fetchMock = mockFetch([jsonResponse(task), jsonResponse({ events: [] })]);
        const client = new CopilotClient({
            autoStart: false,
            env: { COPILOT_MC_BASE_URL: "https://mc.test/agents" },
        });

        const session = await client.createCloudSession({
            owner: "github",
            initialEventTimeoutMs: 0,
        });

        expect(session.metadata.owner).toBe("github");
        expect(fetchMock).toHaveBeenNthCalledWith(
            1,
            "https://mc.test/agents/tasks",
            expect.objectContaining({
                method: "POST",
                body: JSON.stringify({ owner: "github" }),
            })
        );

        await session.disconnect();
    });

    it("requires an owner when creating a repo-less cloud task", async () => {
        const fetchMock = mockFetch([]);
        const client = new CopilotClient({
            autoStart: false,
            env: { COPILOT_MC_BASE_URL: "https://mc.test/agents" },
        });

        await expect(client.createCloudSession({ initialEventTimeoutMs: 0 })).rejects.toThrow(
            "CloudSessionOptions.owner is required when repository is omitted"
        );
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it("sends cloud session user messages through the Mission Control steer API", async () => {
        const fetchMock = mockFetch([
            textResponse("", { status: 404 }),
            jsonResponse({ events: [] }),
            textResponse("", { status: 202 }),
        ]);
        const client = new CopilotClient({
            autoStart: false,
            env: { COPILOT_MC_BASE_URL: "https://mc.test/agents" },
        });

        const session = await client.connectCloudSession("task-1", {
            initialEventTimeoutMs: 0,
        });
        await session.send({ prompt: "hello cloud" });

        expect(fetchMock).toHaveBeenNthCalledWith(
            3,
            "https://mc.test/agents/tasks/task-1/steer",
            expect.objectContaining({
                method: "POST",
                body: JSON.stringify({
                    type: MissionControlCommandType.UserMessage,
                    content: "hello cloud",
                }),
            })
        );

        await session.disconnect();
    });

    it("sorts replayed events and deduplicates events observed during polling", async () => {
        vi.useFakeTimers();
        const polledEvent: CloudSessionEvent = {
            id: "event-3",
            parentId: "event-2",
            timestamp: "2026-05-11T10:00:02.000Z",
            type: "session.idle",
            data: {},
        };
        mockFetch([
            jsonResponse(task),
            jsonResponse({ events: [idleEvent, requestedEvent] }),
            jsonResponse({ events: [idleEvent, requestedEvent, polledEvent] }),
        ]);
        const client = new CopilotClient({
            autoStart: false,
            env: { COPILOT_MC_BASE_URL: "https://mc.test/agents" },
        });

        const session = await client.connectCloudSession("task-1", {
            initialEventTimeoutMs: 0,
            pollIntervalMs: 10,
        });
        const seen: string[] = [];
        session.on((event) => seen.push(event.id));

        expect(session.getMessages().map((event) => event.id)).toEqual(["event-1", "event-2"]);

        await vi.advanceTimersByTimeAsync(10);

        expect(seen).toEqual(["event-3"]);
        expect(session.getMessages().map((event) => event.id)).toEqual([
            "event-1",
            "event-2",
            "event-3",
        ]);

        await session.disconnect();
    });

    it("surfaces Mission Control error responses as typed cloud session errors", async () => {
        mockFetch([textResponse(JSON.stringify({ message: "blocked" }), { status: 403 })]);
        const client = new CopilotClient({
            autoStart: false,
            env: { COPILOT_MC_BASE_URL: "https://mc.test/agents" },
        });

        await expect(
            client.createCloudSession({
                repository: { owner: "github", name: "copilot-sdk" },
                initialEventTimeoutMs: 0,
            })
        ).rejects.toMatchObject({
            name: "CloudSessionError",
            message: "blocked",
            reason: "policy_blocked",
            status: 403,
        } satisfies Partial<CloudSessionError>);
    });
});

function mockFetch(responses: Response[]): ReturnType<typeof vi.fn> {
    const fetchMock = vi.fn(async () => {
        const response = responses.shift();
        if (!response) {
            throw new Error("Unexpected fetch call");
        }
        return response;
    });
    vi.stubGlobal("fetch", fetchMock);
    return fetchMock;
}

function jsonResponse(value: unknown, init?: ResponseInit): Response {
    return new Response(JSON.stringify(value), {
        status: 200,
        headers: { "Content-Type": "application/json" },
        ...init,
    });
}

function textResponse(value: string, init?: ResponseInit): Response {
    return new Response(value, {
        status: 200,
        ...init,
    });
}
