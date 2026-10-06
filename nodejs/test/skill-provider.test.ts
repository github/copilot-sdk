/* eslint-disable @typescript-eslint/no-explicit-any */
import { PassThrough } from "node:stream";
import {
    CancellationTokenSource,
    createMessageConnection,
    type MessageConnection,
    ResponseError,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    approveAll,
    CopilotClient,
    RuntimeConnection,
    type CopilotClientOptions,
    type SkillProvider,
    type SkillProviderCallOptions,
    type SkillProviderDescriptor,
} from "../src/index.js";

interface RecordedRequest {
    method: string;
    params: any;
}

type RuntimeRequestHandler = (
    method: string,
    params: any,
    runtime: MessageConnection
) => Promise<unknown> | unknown;

interface Harness {
    client: CopilotClient;
    runtime: MessageConnection;
    requests: RecordedRequest[];
}

/**
 * Starts a client over an in-memory JSON-RPC pipe. The `runtime` end records
 * every request and can call the client's reverse-RPC handlers.
 */
async function startClient(
    options: Partial<CopilotClientOptions> = {},
    onRuntimeRequest?: RuntimeRequestHandler
): Promise<Harness> {
    const clientToRuntime = new PassThrough();
    const runtimeToClient = new PassThrough();
    const clientConnection = createMessageConnection(
        new StreamMessageReader(runtimeToClient),
        new StreamMessageWriter(clientToRuntime)
    );
    const runtime = createMessageConnection(
        new StreamMessageReader(clientToRuntime),
        new StreamMessageWriter(runtimeToClient)
    );
    const requests: RecordedRequest[] = [];
    runtime.onRequest(async (method: string, params: any) => {
        requests.push({ method, params });
        const custom = await onRuntimeRequest?.(method, params, runtime);
        if (custom !== undefined) {
            return custom;
        }
        if (method === "session.create" || method === "session.resume") {
            return { sessionId: params.sessionId };
        }
        if (method === "session.detach") {
            return { success: true };
        }
        return {};
    });
    runtime.listen();

    const client = new CopilotClient({
        connection: RuntimeConnection.forUri("localhost:1234"),
        ...options,
    });
    vi.spyOn(client as any, "connectToServer").mockImplementation(async () => {
        (client as any).connection = clientConnection;
        (client as any).attachConnectionHandlers();
        clientConnection.listen();
    });
    vi.spyOn(client as any, "verifyProtocolVersion").mockResolvedValue(undefined);
    onTestFinished(async () => {
        await client.forceStop();
        clientConnection.dispose();
        runtime.dispose();
    });

    await client.start();
    return { client, runtime, requests };
}

function payload(requests: RecordedRequest[], method: string): any {
    const request = requests.find((r) => r.method === method);
    expect(request, `${method} was not sent`).toBeDefined();
    return request!.params;
}

async function rejection(promise: Promise<unknown>): Promise<ResponseError<any>> {
    const error = await promise.then(
        () => undefined,
        (e: unknown) => e
    );
    expect(error).toBeInstanceOf(ResponseError);
    return error as ResponseError<any>;
}

const reviewSkill: SkillProviderDescriptor = {
    name: "review",
    description: "Reviews code",
};

function recordingProvider(
    skills: SkillProviderDescriptor[] | null = [reviewSkill],
    markdown: Record<string, string> = { review: "Review carefully." }
): SkillProvider & { calls: string[] } {
    const calls: string[] = [];
    return {
        calls,
        listSkills: () => {
            calls.push("list");
            return skills as SkillProviderDescriptor[];
        },
        readSkill: (name) => {
            calls.push(`read:${name}`);
            return markdown[name] ?? null;
        },
    };
}

describe("skill providers", () => {
    describe("session payloads", () => {
        it("leaves create and resume payloads unchanged without a provider", async () => {
            const { client, requests } = await startClient();

            const session = await client.createSession({ onPermissionRequest: approveAll });
            await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

            expect(payload(requests, "session.create")).not.toHaveProperty("hasSkillProvider");
            expect(payload(requests, "session.resume")).not.toHaveProperty("hasSkillProvider");
        });

        it("flags create and resume payloads when a provider is supplied", async () => {
            const { client, requests } = await startClient();
            const provider = recordingProvider();

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: provider,
            });
            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                skillProvider: provider,
            });

            const create = payload(requests, "session.create");
            expect(create.hasSkillProvider).toBe(true);
            expect(create).not.toHaveProperty("skillProvider");
            expect(create.enableSkills).toBeUndefined();
            const resume = payload(requests, "session.resume");
            expect(resume.hasSkillProvider).toBe(true);
            expect(resume).not.toHaveProperty("skillProvider");
            expect(provider.calls).toEqual([]);
        });

        it("keeps empty mode's enableSkills default when a provider is supplied", async () => {
            const { client, requests } = await startClient({
                mode: "empty",
                baseDirectory: "/tmp/copilot-test",
            });

            await client.createSession({
                onPermissionRequest: approveAll,
                availableTools: [],
                skillProvider: recordingProvider(),
            });

            const create = payload(requests, "session.create");
            expect(create.enableSkills).toBe(false);
            expect(create.hasSkillProvider).toBe(true);
        });

        it.each(["session.create", "session.resume"] as const)(
            "serves callbacks the runtime makes while handling %s",
            async (openMethod) => {
                let earlyList: unknown;
                const { client } = await startClient({}, async (method, params, runtime) => {
                    if (method === openMethod) {
                        earlyList = await runtime.sendRequest("skillProvider.list", {
                            sessionId: params.sessionId,
                        });
                        return { sessionId: params.sessionId };
                    }
                    return undefined;
                });
                const provider = recordingProvider();
                const config = { onPermissionRequest: approveAll, skillProvider: provider };

                if (openMethod === "session.create") {
                    await client.createSession(config);
                } else {
                    await client.resumeSession("existing-session", config);
                }

                expect(earlyList).toEqual({ skills: [reviewSkill] });
            }
        );
    });

    describe("cloud sessions", () => {
        it("rejects a provider before starting the client or sending anything", async () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("localhost:1234"),
            });
            const start = vi.spyOn(client, "start");
            const provider = recordingProvider();

            await expect(
                client.createSession({
                    onPermissionRequest: approveAll,
                    cloud: {},
                    skillProvider: provider,
                })
            ).rejects.toThrow("Skill providers are not supported for cloud sessions.");

            expect(start).not.toHaveBeenCalled();
            expect(provider.calls).toEqual([]);
        });
    });

    describe("dispatch", () => {
        it.each([
            ["synchronous", (value: any) => value],
            ["asynchronous", (value: any) => Promise.resolve(value)],
        ])("serves list and read from a %s provider", async (_kind, wrap) => {
            const { client, runtime } = await startClient();
            const descriptors: SkillProviderDescriptor[] = [
                reviewSkill,
                {
                    name: "deploy",
                    description: "Deploys the service",
                    userInvocable: false,
                    disableModelInvocation: true,
                    argumentHint: "[environment]",
                },
            ];
            const reads: string[] = [];
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: {
                    listSkills: () => wrap(descriptors),
                    readSkill: (name) => {
                        reads.push(name);
                        return wrap(`# ${name}`);
                    },
                },
            });

            await expect(
                runtime.sendRequest("skillProvider.list", { sessionId: session.sessionId })
            ).resolves.toEqual({ skills: descriptors });
            await expect(
                runtime.sendRequest("skillProvider.read", {
                    sessionId: session.sessionId,
                    name: "deploy",
                })
            ).resolves.toEqual({ markdown: "# deploy" });
            expect(reads).toEqual(["deploy"]);
        });

        it("omits unset optional descriptor fields", async () => {
            const { client, runtime } = await startClient();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: recordingProvider([
                    { name: "review", description: "Reviews code", argumentHint: undefined },
                ]),
            });

            const result: any = await runtime.sendRequest("skillProvider.list", {
                sessionId: session.sessionId,
            });

            expect(Object.keys(result.skills[0])).toEqual(["name", "description"]);
        });

        it("answers a null catalog with an empty list", async () => {
            const { client, runtime } = await startClient();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: recordingProvider(null),
            });

            await expect(
                runtime.sendRequest("skillProvider.list", { sessionId: session.sessionId })
            ).resolves.toEqual({ skills: [] });
        });

        it.each([null, undefined])("answers a %s read with null markdown", async (missing) => {
            const { client, runtime } = await startClient();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: { listSkills: () => [reviewSkill], readSkill: () => missing },
            });

            await expect(
                runtime.sendRequest("skillProvider.read", {
                    sessionId: session.sessionId,
                    name: "review",
                })
            ).resolves.toEqual({ markdown: null });
        });

        it.each(["listSkills", "readSkill"] as const)(
            "reports a %s failure generically without forwarding its message",
            async (failing) => {
                const { client, runtime } = await startClient();
                const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
                onTestFinished(() => consoleError.mockRestore());
                const secret = "db-password-in-error";
                const providerError = new Error(secret);
                const session = await client.createSession({
                    onPermissionRequest: approveAll,
                    skillProvider: {
                        listSkills: () => {
                            if (failing === "listSkills") throw providerError;
                            return [reviewSkill];
                        },
                        readSkill: async () => {
                            throw providerError;
                        },
                    },
                });

                const error = await rejection(
                    failing === "listSkills"
                        ? runtime.sendRequest("skillProvider.list", {
                              sessionId: session.sessionId,
                          })
                        : runtime.sendRequest("skillProvider.read", {
                              sessionId: session.sessionId,
                              name: "review",
                          })
                );

                expect(error.code).toBe(-32603);
                expect(error.data).toBeUndefined();
                expect(error.message).not.toContain(secret);
                expect(consoleError).toHaveBeenCalledWith(`Skill provider ${failing} failed`, {
                    sessionId: session.sessionId,
                    error: providerError,
                });
            }
        );

        it.each(["listSkills", "readSkill"] as const)(
            "aborts a %s call when the runtime cancels it",
            async (operation) => {
                const { client, runtime } = await startClient();
                const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
                onTestFinished(() => consoleError.mockRestore());
                let entered!: () => void;
                const started = new Promise<void>((resolve) => (entered = resolve));
                const block = ({ signal }: SkillProviderCallOptions) =>
                    new Promise<never>((_resolve, reject) => {
                        signal.addEventListener("abort", () => reject(signal.reason), {
                            once: true,
                        });
                        entered();
                    });
                const session = await client.createSession({
                    onPermissionRequest: approveAll,
                    skillProvider: {
                        listSkills: (options) => block(options),
                        readSkill: (_name, options) => block(options),
                    },
                });
                const cancellation = new CancellationTokenSource();

                const pending = rejection(
                    operation === "listSkills"
                        ? runtime.sendRequest(
                              "skillProvider.list",
                              { sessionId: session.sessionId },
                              cancellation.token
                          )
                        : runtime.sendRequest(
                              "skillProvider.read",
                              { sessionId: session.sessionId, name: "review" },
                              cancellation.token
                          )
                );
                await started;
                cancellation.cancel();

                expect((await pending).code).toBe(-32800);
                expect(consoleError).not.toHaveBeenCalled();
            }
        );

        it("rejects a request for an unknown session", async () => {
            const { runtime } = await startClient();

            await rejection(runtime.sendRequest("skillProvider.list", { sessionId: "missing" }));
        });

        it("rejects a request for a session without a provider", async () => {
            const { client, runtime } = await startClient();
            const session = await client.createSession({ onPermissionRequest: approveAll });

            await rejection(
                runtime.sendRequest("skillProvider.read", {
                    sessionId: session.sessionId,
                    name: "review",
                })
            );
        });
    });

    describe("teardown", () => {
        it("stops calling the provider after the session disconnects", async () => {
            const { client, runtime } = await startClient();
            const provider = recordingProvider();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: provider,
            });
            const sessionId = session.sessionId;

            await session.disconnect();
            await rejection(runtime.sendRequest("skillProvider.list", { sessionId }));

            expect(provider.calls).toEqual([]);
        });

        it("keeps a session that failed to open from serving its provider", async () => {
            const provider = recordingProvider();
            let sessionId: string | undefined;
            const { client, runtime } = await startClient({}, (method, params) => {
                if (method === "session.create") {
                    sessionId = params.sessionId;
                    throw new Error("create failed");
                }
                return undefined;
            });

            await expect(
                client.createSession({ onPermissionRequest: approveAll, skillProvider: provider })
            ).rejects.toThrow("create failed");
            await rejection(runtime.sendRequest("skillProvider.list", { sessionId: sessionId! }));

            expect(provider.calls).toEqual([]);
        });

        it("keeps the resident provider when a resume fails validation", async () => {
            const { client, runtime, requests } = await startClient({
                mode: "empty",
                baseDirectory: "/tmp/copilot-test",
            });
            const resident = recordingProvider();
            const replacement = recordingProvider();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                availableTools: [],
                skillProvider: resident,
            });

            await expect(
                client.resumeSession(session.sessionId, {
                    onPermissionRequest: approveAll,
                    skillProvider: replacement,
                })
            ).rejects.toThrow("availableTools");
            await runtime.sendRequest("skillProvider.list", { sessionId: session.sessionId });

            expect(requests.map((r) => r.method)).not.toContain("session.resume");
            expect(resident.calls).toEqual(["list"]);
            expect(replacement.calls).toEqual([]);
        });

        it("restores the resident provider when the runtime rejects a resume", async () => {
            const { client, runtime } = await startClient({}, (method) => {
                if (method === "session.resume") {
                    throw new Error("resume failed");
                }
                return undefined;
            });
            const resident = recordingProvider();
            const replacement = recordingProvider();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: resident,
            });

            await expect(
                client.resumeSession(session.sessionId, {
                    onPermissionRequest: approveAll,
                    skillProvider: replacement,
                })
            ).rejects.toThrow("resume failed");
            await runtime.sendRequest("skillProvider.list", { sessionId: session.sessionId });

            expect(resident.calls).toEqual(["list"]);
            expect(replacement.calls).toEqual([]);
        });

        it.each(["create", "resume"] as const)(
            "does not register a provider when session-FS setup fails on %s",
            async (open) => {
                const { client, runtime, requests } = await startClient({
                    sessionFs: {
                        initialCwd: "/workspace",
                        sessionStatePath: "/state",
                        conventions: "posix",
                    },
                });
                const provider = recordingProvider();
                const config = {
                    onPermissionRequest: approveAll,
                    sessionId: "fs-failure",
                    skillProvider: provider,
                };

                await expect(
                    open === "create"
                        ? client.createSession(config)
                        : client.resumeSession("fs-failure", config)
                ).rejects.toThrow("createSessionFsProvider is required");
                await rejection(
                    runtime.sendRequest("skillProvider.list", { sessionId: "fs-failure" })
                );

                expect(requests.map((r) => r.method)).not.toContain(`session.${open}`);
                expect(provider.calls).toEqual([]);
            }
        );

        it("serves the resumed provider instead of the original one", async () => {
            const { client, runtime } = await startClient();
            const original = recordingProvider();
            const replacement = recordingProvider();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                skillProvider: original,
            });
            await session.disconnect();

            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                skillProvider: replacement,
            });
            await runtime.sendRequest("skillProvider.list", { sessionId: session.sessionId });

            expect(original.calls).toEqual([]);
            expect(replacement.calls).toEqual(["list"]);
        });
    });
});
