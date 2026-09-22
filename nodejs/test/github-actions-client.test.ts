import { describe, expect, it, vi } from "vitest";
import {
    GitHubActionsClient,
    type DelegateLifecycleEvent,
    type DelegateRequest,
} from "../samples/github-actions-client.js";

const request: DelegateRequest = {
    task: "Review retry handling",
    repository: "octo/example",
    ref: "main",
    agentType: "researcher",
    expectedOutput: "A concise report",
};

const invocation = {
    sessionId: "session-1",
    toolCallId: "tool-call-1",
};

function jsonResponse(value: unknown, status = 200): Response {
    return new Response(JSON.stringify(value), {
        status,
        headers: { "Content-Type": "application/json" },
    });
}

function createClient(fetchImplementation: typeof fetch, events: DelegateLifecycleEvent[] = []) {
    return new GitHubActionsClient({
        token: () => "test-token",
        targets: [
            {
                repository: "octo/example",
                workflow: "worker.yml",
                allowedRefs: ["main"],
                allowedAgentTypes: ["researcher", "editor"],
            },
        ],
        fetch: fetchImplementation,
        pollIntervalMs: 1,
        sleep: async () => {},
        onLifecycle: (event) => events.push(event),
    });
}

describe("GitHubActionsClient", () => {
    it("dispatches once, correlates the run, and returns outputs", async () => {
        const events: DelegateLifecycleEvent[] = [];
        let runLookup = 0;
        const fetchImplementation = vi.fn<typeof fetch>(async (input, init) => {
            const url = String(input);
            if (url.includes("/actions/workflows/worker.yml/runs")) {
                runLookup += 1;
                if (runLookup === 1) {
                    return jsonResponse({ workflow_runs: [] });
                }
                return jsonResponse({
                    workflow_runs: [
                        {
                            id: 42,
                            status: runLookup < 4 ? "in_progress" : "completed",
                            conclusion: runLookup < 4 ? null : "success",
                            html_url: "https://github.com/octo/example/actions/runs/42",
                            display_title: "SDK delegate 76364575d6bbe9ed561b09ed",
                            head_sha: "abc123",
                        },
                    ],
                });
            }
            if (url.endsWith("/actions/workflows/worker.yml/dispatches")) {
                expect(init?.method).toBe("POST");
                expect(init?.headers).toMatchObject({
                    Authorization: ["Bearer", "test-token"].join(" "),
                });
                const body = JSON.parse(String(init?.body)) as {
                    inputs: Record<string, string>;
                };
                expect(body.inputs.output_branch).toBe("agent/76364575d6bbe9ed561b09ed");
                return new Response(null, { status: 204 });
            }
            if (url.endsWith("/actions/runs/42/artifacts")) {
                return jsonResponse({
                    artifacts: [
                        {
                            id: 7,
                            name: "agent-result",
                            archive_download_url:
                                "https://api.github.com/repos/octo/example/actions/artifacts/7/zip",
                            expired: false,
                        },
                    ],
                });
            }
            if (url.includes("/pulls?")) {
                return jsonResponse([
                    {
                        number: 9,
                        html_url: "https://github.com/octo/example/pull/9",
                        state: "open",
                    },
                ]);
            }
            throw new Error(`Unexpected request: ${init?.method ?? "GET"} ${url}`);
        });

        const result = await createClient(fetchImplementation, events).execute(request, invocation);

        expect(result).toMatchObject({
            runId: 42,
            conclusion: "success",
            outputBranch: "agent/76364575d6bbe9ed561b09ed",
            artifacts: [{ id: 7, name: "agent-result" }],
            pullRequests: [{ number: 9, state: "open" }],
        });
        expect(events.map((event) => event.state)).toEqual(["queued", "running", "completed"]);
        expect(
            fetchImplementation.mock.calls.filter(([url]) => String(url).endsWith("/dispatches"))
        ).toHaveLength(1);
    });

    it("reuses a correlated run instead of dispatching it again", async () => {
        const fetchImplementation = vi.fn<typeof fetch>(async (input) => {
            const url = String(input);
            if (url.includes("/actions/workflows/worker.yml/runs")) {
                return jsonResponse({
                    workflow_runs: [
                        {
                            id: 42,
                            status: "completed",
                            conclusion: "failure",
                            html_url: "https://github.com/octo/example/actions/runs/42",
                            display_title: "SDK delegate 76364575d6bbe9ed561b09ed",
                            head_sha: "abc123",
                        },
                    ],
                });
            }
            if (url.endsWith("/actions/runs/42/artifacts")) {
                return jsonResponse({ artifacts: [] });
            }
            if (url.includes("/pulls?")) {
                return jsonResponse([]);
            }
            throw new Error(`Unexpected request: ${url}`);
        });

        const result = await createClient(fetchImplementation).execute(request, invocation);

        expect(result.conclusion).toBe("failure");
        expect(
            fetchImplementation.mock.calls.some(([url]) => String(url).endsWith("/dispatches"))
        ).toBe(false);
    });

    it("cancels the correlated run when the invocation is aborted", async () => {
        const controller = new AbortController();
        let runLookup = 0;
        let cancellationRequested = false;
        const fetchImplementation = vi.fn<typeof fetch>(async (input, init) => {
            const url = String(input);
            if (url.includes("/actions/workflows/worker.yml/runs")) {
                runLookup += 1;
                if (runLookup === 1) {
                    return jsonResponse({ workflow_runs: [] });
                }
                return jsonResponse({
                    workflow_runs: [
                        {
                            id: 42,
                            status: "in_progress",
                            conclusion: null,
                            html_url: "https://github.com/octo/example/actions/runs/42",
                            display_title: "SDK delegate 76364575d6bbe9ed561b09ed",
                            head_sha: "abc123",
                        },
                    ],
                });
            }
            if (url.endsWith("/actions/workflows/worker.yml/dispatches")) {
                return new Response(null, { status: 204 });
            }
            if (url.endsWith("/actions/runs/42/cancel")) {
                cancellationRequested = init?.method === "POST";
                return new Response(null, { status: 202 });
            }
            throw new Error(`Unexpected request: ${url}`);
        });
        const client = new GitHubActionsClient({
            token: () => "test-token",
            targets: [
                {
                    repository: "octo/example",
                    workflow: "worker.yml",
                    allowedRefs: ["main"],
                    allowedAgentTypes: ["researcher"],
                },
            ],
            fetch: fetchImplementation,
            pollIntervalMs: 1,
            sleep: async () => {
                controller.abort();
                throw Object.assign(new Error("aborted"), { name: "AbortError" });
            },
        });

        await expect(
            client.execute(request, { ...invocation, signal: controller.signal })
        ).rejects.toMatchObject({ name: "AbortError" });
        expect(cancellationRequested).toBe(true);
    });

    it("rejects repositories, refs, and agent types outside the allowlist", async () => {
        const fetchImplementation = vi.fn<typeof fetch>();
        const client = createClient(fetchImplementation);

        await expect(
            client.dispatch({ ...request, repository: "other/example" }, invocation)
        ).rejects.toThrow("not allowlisted");
        await expect(
            client.dispatch({ ...request, ref: "feature/untrusted" }, invocation)
        ).rejects.toThrow("not allowlisted");
        await expect(
            client.dispatch({ ...request, agentType: "admin" }, invocation)
        ).rejects.toThrow("not allowlisted");
        expect(fetchImplementation).not.toHaveBeenCalled();
    });
});
