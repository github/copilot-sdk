/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFileSync } from "node:fs";
import { afterEach, describe, expect, it, onTestFinished, vi } from "vitest";
import { ResponseError } from "vscode-jsonrpc/node.js";
import { CopilotClient } from "../src/client.js";
import { joinSession } from "../src/extension.js";
import { CopilotSession } from "../src/session.js";
import {
    defineFactory,
    FactoryResumeError,
    isFactoryRunTerminal,
    type FactoryAgentOptions,
    type FactoryContext,
    type FactoryDefinition,
    type FactoryJsonSchema,
    type JsonValue,
} from "../src/factory.js";

/** Builds a `factory.run_updated` invalidation event for a run. */
function runUpdatedEvent(runId: string, revision: number): Record<string, unknown> {
    return {
        type: "factory.run_updated",
        id: `event-${runId}-${revision}`,
        parentId: null,
        timestamp: new Date().toISOString(),
        ephemeral: true,
        data: { runId, revision },
    };
}

async function stopClient(client: CopilotClient): Promise<void> {
    await client.stop();
}

describe("factories", () => {
    const originalSessionId = process.env.SESSION_ID;

    afterEach(() => {
        if (originalSessionId === undefined) {
            delete process.env.SESSION_ID;
        } else {
            process.env.SESSION_ID = originalSessionId;
        }
        vi.restoreAllMocks();
    });

    it("defines a stable handle and accepts omitted limits", async () => {
        const meta = {
            name: "no-limits",
            description: "A factory without resource limits",
            phases: [],
        };
        const run = vi.fn(async ({ args }: { args: unknown }) => args);
        const handle = defineFactory({ meta, run });

        expect(handle.meta).toEqual(meta);
        expect(handle.meta).not.toBe(meta);
        expect(Object.isFrozen(handle)).toBe(true);
        expect(Object.isFrozen(handle.meta)).toBe(true);

        // The handle holds a snapshot, so mutating the caller's object after
        // registration cannot desynchronize the advertised metadata.
        meta.name = "mutated";
        (meta.phases as string[]).push("late");
        expect(handle.meta.name).toBe("no-limits");
        expect(handle.meta.phases).toEqual([]);
        meta.name = "no-limits";
        meta.phases.length = 0;

        // The stored metadata is deep-frozen, so the handle's view of it must be
        // readonly all the way down. Assert both halves: the mutation is a type
        // error, and it also throws at runtime.
        expect(() => {
            // @ts-expect-error handle.meta is deeply readonly.
            handle.meta.name = "mutated";
        }).toThrow(TypeError);
        expect(() => {
            // @ts-expect-error handle.meta.phases is a readonly array.
            handle.meta.phases.push({ title: "late" });
        }).toThrow(TypeError);

        const session = new CopilotSession("session-1", {} as never);
        session.registerFactories([handle]);
        const result = await session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: meta.name,
            runId: "run-1",
            executionToken: "execution-token",
            args: { value: 42 },
        });

        expect(run).toHaveBeenCalledOnce();
        expect(result).toEqual({ result: { value: 42 } });
    });

    it.each([
        [[{ title: "" }], "must not be empty"],
        [[{ title: "Inspect" }, { title: "Inspect" }], "declared more than once"],
    ])("rejects invalid declared phase titles", (phases, message) => {
        expect(() =>
            defineFactory({
                meta: {
                    name: "invalid-phases",
                    description: "Invalid phase metadata",
                    phases,
                },
                run: async () => {},
            })
        ).toThrow(message);
    });

    it("returns an absent execute result for a void factory", async () => {
        const factory = defineFactory({
            meta: {
                name: "void-result",
                description: "Returns no result",
                phases: [],
            },
            run: async () => {},
        });
        const session = new CopilotSession("session-void-result", {} as never);
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "void-result",
                runId: "run-void-result",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({});
    });

    it.each<JsonValue>([42, "factory-result", [1, "two", false]])(
        "returns non-object JSON factory result %j",
        async (factoryResult) => {
            const factory = defineFactory({
                meta: {
                    name: "json-result",
                    description: "Returns any JSON value",
                    phases: [],
                },
                run: async () => factoryResult,
            });
            const session = new CopilotSession("session-json-result", {} as never);
            session.registerFactories([factory]);

            await expect(
                session.clientSessionApis.factory!.execute({
                    sessionId: session.sessionId,
                    name: "json-result",
                    runId: "run-json-result",
                    executionToken: "execution-token",
                    args: {},
                })
            ).resolves.toEqual({ result: factoryResult });
        }
    );

    it.each([
        ["function", { nested: () => undefined }, "$.nested"],
        ["symbol", [Symbol("invalid")], "$[0]"],
        ["BigInt", { nested: 1n }, "$.nested"],
    ])("rejects a %s anywhere in a factory result", async (_label, factoryResult, expectedPath) => {
        const factory = defineFactory({
            meta: {
                name: "unsupported-result",
                description: "Returns an unsupported value",
                phases: [],
            },
            run: async () => factoryResult as never,
        });
        const session = new CopilotSession("session-unsupported-result", {} as never);
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "unsupported-result",
                runId: "run-unsupported-result",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toMatchObject({
            message: `Factory result contains a function, symbol, or BigInt at ${expectedPath}`,
            data: {
                code: "factory_result_not_json",
                category: "unsupported_type",
            },
        });
    });

    it.each([
        ["NaN", Number.NaN],
        ["Infinity", Number.POSITIVE_INFINITY],
    ])("rejects the non-finite number %s in a factory result", async (_label, value) => {
        const factory = defineFactory({
            meta: {
                name: "non-finite-result",
                description: "Returns a non-finite number",
                phases: [],
            },
            run: async () => ({ value }) as never,
        });
        const session = new CopilotSession("session-non-finite-result", {} as never);
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "non-finite-result",
                runId: "run-non-finite-result",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toMatchObject({
            message: "Factory result contains a non-finite number at $.value",
            data: {
                code: "factory_result_not_json",
                category: "non_finite_number",
            },
        });
    });

    it("rejects a cyclic factory result", async () => {
        const factoryResult: Record<string, unknown> = {};
        factoryResult.self = factoryResult;
        const factory = defineFactory({
            meta: {
                name: "cyclic-result",
                description: "Returns a cycle",
                phases: [],
            },
            run: async () => factoryResult as never,
        });
        const session = new CopilotSession("session-cyclic-result", {} as never);
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "cyclic-result",
                runId: "run-cyclic-result",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toMatchObject({
            message: "Factory result contains a cyclic reference at $.self",
            data: {
                code: "factory_result_not_json",
                category: "cyclic_value",
            },
        });
    });

    it.each([
        ["object", { nested: undefined }, "$.nested"],
        ["array", [undefined], "$[0]"],
    ])(
        "rejects nested undefined in a factory result %s",
        async (_label, factoryResult, expectedPath) => {
            const factory = defineFactory({
                meta: {
                    name: "nested-undefined-result",
                    description: "Returns nested undefined",
                    phases: [],
                },
                run: async () => factoryResult as never,
            });
            const session = new CopilotSession("session-nested-undefined-result", {} as never);
            session.registerFactories([factory]);

            await expect(
                session.clientSessionApis.factory!.execute({
                    sessionId: session.sessionId,
                    name: "nested-undefined-result",
                    runId: "run-nested-undefined-result",
                    executionToken: "execution-token",
                    args: {},
                })
            ).rejects.toMatchObject({
                message: `Factory result contains nested undefined at ${expectedPath}`,
                data: {
                    code: "factory_result_not_json",
                    category: "nested_undefined",
                },
            });
        }
    );

    it("rejects duplicate factory names within a single registration", () => {
        const run = async () => null;
        const first = defineFactory({
            meta: { name: "dup", description: "first", phases: [] },
            run,
        });
        const second = defineFactory({
            meta: { name: "dup", description: "second", phases: [] },
            run,
        });

        const session = new CopilotSession("session-dup", {} as never);
        expect(() => session.registerFactories([first, second])).toThrow(
            /Duplicate factory name "dup"/
        );
    });

    it.each([
        ["maxConcurrentSubagents", 0],
        ["maxConcurrentSubagents", 1.5],
        ["maxTotalSubagents", -1],
        ["maxTotalSubagents", Number.POSITIVE_INFINITY],
        ["timeoutSeconds", 0],
        ["timeoutSeconds", Number.NaN],
        ["timeoutSeconds", Number.POSITIVE_INFINITY],
        ["maxAiCredits", 0],
        ["maxAiCredits", Number.NaN],
        ["maxAiCredits", Number.POSITIVE_INFINITY],
        ["maxAiCredits", 0.000_000_000_4],
        ["maxAiCredits", (Number.MAX_SAFE_INTEGER + 2) / 1_000_000_000],
    ] as const)("rejects invalid %s limit %s", (field, value) => {
        const definition = {
            meta: {
                name: `invalid-${field}-${String(value)}`,
                description: "Invalid factory",
                phases: [],
                limits: { [field]: value },
            },
            run: async () => null,
        } as FactoryDefinition;

        expect(() => defineFactory(definition)).toThrow(/must be a positive/);
    });

    it("accepts positive fractional timeoutSeconds through the Node timer ceiling", () => {
        for (const timeoutSeconds of [0.001, 1.5, 2_147_483.647]) {
            expect(() =>
                defineFactory({
                    meta: {
                        name: `accepted-timeout-${timeoutSeconds}`,
                        description: "Factory with an accepted active-execution timeout",
                        phases: [],
                        limits: { timeoutSeconds },
                    },
                    run: async () => null,
                })
            ).not.toThrow();
        }
    });

    it("accepts AI-credit ceilings that round to a positive safe nano-AIU integer", () => {
        for (const maxAiCredits of [
            0.000_000_000_5,
            1.25,
            Number.MAX_SAFE_INTEGER / 1_000_000_000,
        ]) {
            expect(() =>
                defineFactory({
                    meta: {
                        name: `accepted-credits-${maxAiCredits}`,
                        description: "Factory with an accepted AI-credit ceiling",
                        phases: [],
                        limits: { maxAiCredits },
                    },
                    run: async () => null,
                })
            ).not.toThrow();
        }
    });

    it("rejects timeoutSeconds above the Node setTimeout ceiling", () => {
        const definition = {
            meta: {
                name: "oversized-timeout",
                description: "Factory with an out-of-range timeout",
                phases: [],
                limits: { timeoutSeconds: 2_147_483.648 },
            },
            run: async () => null,
        } as FactoryDefinition;

        expect(() => defineFactory(definition)).toThrow(
            'Factory limit "timeoutSeconds" must not exceed 2147483.647 seconds'
        );
    });

    it("documents timeoutSeconds as accumulated active-execution time in public and generated types", () => {
        const publicTypes = readFileSync(new URL("../src/types.ts", import.meta.url), "utf8");
        const generatedRpc = readFileSync(
            new URL("../src/generated/rpc.ts", import.meta.url),
            "utf8"
        );

        expect(publicTypes).toContain("Maximum accumulated active-execution time, in seconds.");
        expect(publicTypes).toContain("subprocess waits, queued-agent waits, and sleeps");
        expect(publicTypes).toContain("timeoutSeconds?: number;");
        expect(generatedRpc).toContain("Maximum accumulated active-execution time in seconds.");
        expect(generatedRpc).toContain("subprocess waits, queued-agent waits, and sleeps");
        expect(generatedRpc).toContain("timeoutSeconds?: number;");
    });

    // A guessed ceiling does not make a run safer: it stops a healthy run partway
    // with `factory_limit_reached`, after that run has already taken the user's
    // approval and spent credits. Both documents are handed to the model verbatim
    // by the `factories_manage` guide, so neither may read as an invitation to
    // invent one.
    it("documents limits as opt-in rather than inviting an invented ceiling", () => {
        const guide = readFileSync(new URL("../docs/factories.md", import.meta.url), "utf8");
        const patterns = readFileSync(
            new URL("../docs/factory-patterns.md", import.meta.url),
            "utf8"
        );

        expect(guide).toContain(
            "Set a ceiling only from real knowledge of what the factory costs, or because the user named one"
        );
        expect(guide).toContain("no basis for estimating a number");

        // The opening `defineFactory` sample is the shape an author copies. Filling
        // all four ceilings in there taught the numbers as much as the syntax.
        const openingSample = guide.slice(0, guide.indexOf("## Declaring an argument shape"));
        expect(openingSample).not.toContain("limits: {");

        // The Scaling section used to answer "there is no built-in concurrency cap"
        // with "so declare one before fanning out widely".
        expect(patterns).not.toContain("declare one before fanning out widely");
        expect(patterns).toContain("bound a wide fan-out with the factory's own counters");
    });

    it("documents factory invocation and list paging behavior accurately", () => {
        const guide = readFileSync(new URL("../docs/factories.md", import.meta.url), "utf8");
        const publicApi = readFileSync(new URL("../src/factory.ts", import.meta.url), "utf8");
        const listRunsPagingWording = "newest default page of this session's durable factory runs";
        const resumeCodes = [
            "not_found",
            "non_resumable",
            "already_active",
            "factory_already_running",
            "factory_limits_invalid",
            "factory_session_disposed",
            "factory_storage_unavailable",
            "factory_storage_corrupt",
        ];
        const normalizeJSDoc = (document: string) =>
            document.replace(/\r?\n\s*\* ?/g, " ").replace(/\s+/g, " ");
        const normalizedGuide = normalizeJSDoc(guide);
        const normalizedPublicApi = normalizeJSDoc(publicApi);

        for (const document of [guide, publicApi]) {
            expect(document).not.toContain("reapproval_declined");
            expect(document).not.toContain("no_approval_provider");
            expect(document).not.toMatch(/declined fresh run[\s\S]*terminal `cancelled` envelope/i);
        }

        for (const document of [normalizedGuide, normalizedPublicApi]) {
            expect(document).toContain(listRunsPagingWording);
        }

        expect(normalizedGuide).toContain(
            "SDK-initiated `run` and `resume` do not request permission"
        );
        expect(normalizedGuide).toContain(
            "`run_factory` tool requests permission before the durable row exists"
        );
        expect(normalizedGuide).toContain("declining it creates no run row");
        expect(normalizedGuide).toContain("its maximum number of active top-level runs");
        for (const code of resumeCodes) {
            expect(guide).toContain(`\`${code}\``);
        }
        expect(guide).toContain(
            "Options are exactly `label`, `schema`, `model`, `agent`, `reasoningEffort`, and `contextTier`"
        );
        expect(normalizedGuide).toContain(
            "session returned by `joinSession`. It refuses calls that start or resume a factory run"
        );

        expect(normalizedPublicApi).toContain("SDK-initiated runs do not request permission");
        expect(normalizedPublicApi).toContain("declining it creates no run row");
        expect(normalizedPublicApi).toContain(
            "while the session is at its active top-level run limit"
        );
        expect(normalizedPublicApi).toContain("SDK-initiated resumes do not request permission");
        expect(normalizedPublicApi).toContain("with a documented resume code rejects with");
        expect(normalizedPublicApi).toContain(
            "session instance returned by `joinSession`. It refuses calls that start or resume a factory run"
        );
    });

    it("carries a declared argsSchema through defineFactory into the registration payload", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const argsSchema = {
            type: "object",
            required: ["repoPath"],
            properties: {
                repoPath: { type: "string" },
                depth: { type: ["integer", "null"] },
                mode: { enum: ["fast", "thorough"] },
            },
        } satisfies FactoryJsonSchema;
        const meta = {
            name: "declares-args",
            description: "Declares the argument shape it expects",
            phases: [],
            argsSchema,
        };
        const factory = defineFactory({ meta, run: async () => ({ ok: true }) });

        // The declaration is snapshotted and deep-frozen like the rest of the
        // metadata, so it cannot be mutated after registration.
        expect(factory.meta.argsSchema).toEqual(argsSchema);
        expect(factory.meta.argsSchema).not.toBe(argsSchema);
        expect(Object.isFrozen(factory.meta.argsSchema)).toBe(true);
        expect(() => {
            // @ts-expect-error handle.meta.argsSchema is deeply readonly.
            factory.meta.argsSchema!.type = "array";
        }).toThrow(TypeError);

        const omitted = defineFactory({
            meta: { name: "omits-args", description: "Declares nothing", phases: [] },
            run: async () => ({ ok: true }),
        });
        expect(omitted.meta.argsSchema).toBeUndefined();
        expect("argsSchema" in omitted.meta).toBe(false);

        const sendRequest = vi
            .spyOn(
                (client as never as { connection: { sendRequest: Function } }).connection,
                "sendRequest"
            )
            .mockImplementation(async (method: string, params: Record<string, unknown>) => {
                if (method === "session.resume") {
                    return { sessionId: params.sessionId };
                }
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSessionForExtension(
            "session-args-schema",
            { onPermissionRequest: () => ({ kind: "approved" }) },
            [factory, omitted]
        );

        const payload = sendRequest.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as { factories: Array<Record<string, unknown>> };
        // The schema has to survive JSON serialization to reach the runtime, which
        // validates `args` against it before a run row exists.
        expect(JSON.parse(JSON.stringify(payload.factories))[0].argsSchema).toEqual(argsSchema);
        expect(payload.factories[1]).not.toHaveProperty("argsSchema");
    });

    it("documents argsSchema consistently with the runtime's enforced subset", () => {
        const publicTypes = readFileSync(new URL("../src/types.ts", import.meta.url), "utf8");
        const publicApi = readFileSync(new URL("../src/factory.ts", import.meta.url), "utf8");
        const guide = readFileSync(new URL("../docs/factories.md", import.meta.url), "utf8");
        const normalizeJSDoc = (document: string) =>
            document.replace(/\r?\n\s*\* ?/g, " ").replace(/\s+/g, " ");

        expect(publicTypes).toContain("argsSchema?: FactoryJsonSchema;");

        // The `run_factory` tool tells the model exactly this. The two surfaces
        // have to agree about what a declaration does and does not enforce.
        for (const document of [normalizeJSDoc(publicTypes), guide]) {
            expect(document).toContain("types, required properties, and enum");
            expect(document).toMatch(
                /`minLength`, `pattern`,? (?:and|or) `additionalProperties` are recorded/
            );
        }
        expect(normalizeJSDoc(publicTypes)).toContain("before** the run starts");
        // Enforcement is tool-path only: `toolRunFactoryValidateArgs` is called from
        // the runtime's runFactoryTool, and never from `session.factory.run`. Both
        // surfaces must keep saying so, or authors will assume their own SDK-initiated
        // runs are checked.
        expect(normalizeJSDoc(publicTypes)).toContain(
            "`session.factory.run(...)` is not validated against the declaration"
        );
        expect(guide).toContain("Validation covers the model's `run_factory` path only");
        expect(normalizeJSDoc(publicApi)).toContain(
            "`null`, `boolean`, `integer`, `number`, `string`, `array`, or `object`"
        );
        expect(guide).toContain("no run row, permission prompt, or credit spend happens");
    });

    it("serializes only factory metadata in the extension resume payload", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const run = vi.fn(async () => ({ ok: true }));
        const factory = defineFactory({
            meta: {
                name: "registered",
                description: "Registration test",
                phases: [{ title: "Run" }],
                limits: { maxTotalSubagents: 2 },
            },
            run,
        });
        const sendRequest = vi
            .spyOn(
                (client as never as { connection: { sendRequest: Function } }).connection,
                "sendRequest"
            )
            .mockImplementation(async (method: string, params: Record<string, unknown>) => {
                if (method === "session.resume") {
                    const sessions = (client as never as { sessions: Map<string, CopilotSession> })
                        .sessions;
                    expect(
                        sessions.get(params.sessionId as string)?.clientSessionApis.factory
                    ).toBeDefined();
                    return { sessionId: params.sessionId };
                }
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSessionForExtension(
            "session-registration",
            { onPermissionRequest: () => ({ kind: "approved" }) },
            [factory]
        );

        const payload = sendRequest.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as {
            factories: unknown[];
        };
        expect(payload.factories).toEqual([factory.meta]);
        expect(payload.factories[0]).not.toHaveProperty("run");
        expect(JSON.stringify(payload.factories)).not.toContain("async");
    });

    it("passes factories only through the extension join path", async () => {
        process.env.SESSION_ID = "session-extension";
        const factory = defineFactory({
            meta: {
                name: "extension-only",
                description: "Extension-only registration",
                phases: [],
            },
            run: async () => ({ ok: true }),
        });
        const resumeSessionForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as CopilotSession);

        await joinSession({ factories: [factory] });

        expect(resumeSessionForExtension).toHaveBeenCalledWith(
            "session-extension",
            expect.objectContaining({ suppressResumeEvent: true }),
            [factory],
            undefined
        );
    });

    it("builds the factory context with the unrestricted joined session identity", async () => {
        process.env.SESSION_ID = "session-context";
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.log") {
                return {};
            }
            if (method === "session.tasks.list") {
                return { tasks: [] };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const joinedSession = new CopilotSession("session-context", { sendRequest } as never);
        const contextSeen = Promise.withResolvers<{
            runId: string;
            args: unknown;
            session: CopilotSession;
            signal: AbortSignal;
        }>();
        const factory = defineFactory({
            meta: {
                name: "context",
                description: "Context test",
                phases: [],
            },
            run: async (context) => {
                contextSeen.resolve(context);
                context.phase("A");
                context.log("hi");
                const tasks = await context.session.rpc.tasks.list();
                return { ok: true, taskCount: tasks.tasks.length };
            },
        });
        vi.spyOn(CopilotClient.prototype, "resumeSessionForExtension").mockImplementation(
            async (_sessionId, _config, factories) => {
                joinedSession.registerFactories(factories);
                return joinedSession;
            }
        );

        const joinSessionResult = await joinSession({ factories: [factory] });
        const executeResult = await joinSessionResult.clientSessionApis.factory!.execute({
            sessionId: joinSessionResult.sessionId,
            name: "context",
            runId: "run-context",
            executionToken: "execution-token",
            args: { value: 42 },
        });
        const context = await contextSeen.promise;

        expect(context.runId).toBe("run-context");
        expect(context.args).toEqual({ value: 42 });
        expect(context.session).toBe(joinSessionResult);
        expect(context.session.rpc).toBe(joinSessionResult.rpc);
        expect(context.signal).toBeInstanceOf(AbortSignal);
        expect(executeResult).toEqual({ result: { ok: true, taskCount: 0 } });
        expect(sendRequest).toHaveBeenCalledWith("session.tasks.list", {
            sessionId: joinSessionResult.sessionId,
        });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.log", {
            sessionId: joinSessionResult.sessionId,
            runId: "run-context",
            executionToken: "execution-token",
            lines: [
                { seq: 0, kind: "phase", text: "A" },
                { seq: 1, kind: "log", text: "hi" },
            ],
        });
    });

    it("rejects nested factories without forwarding a runNested request", async () => {
        const sendRequest = vi.fn(async () => {
            throw new Error("Unexpected forward request");
        });
        const session = new CopilotSession("session-no-nesting", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "no-nesting",
                description: "Nested factory rejection test",
                phases: [],
            },
            run: async (context) => context.factory("nested", { value: 42 }),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "no-nesting",
                runId: "run-no-nesting",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toThrow("nested factories are not supported");
        expect(sendRequest).not.toHaveBeenCalled();
    });

    it("keeps factory reads and cancellation available inside a factory body", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            switch (method) {
                case "session.factory.getRun":
                    return { runId: "other-run", status: "completed" };
                case "session.factory.listRuns":
                    return { runs: [] };
                case "session.factory.cancel":
                    return {};
                default:
                    throw new Error(`Unexpected method: ${method}`);
            }
        });
        const session = new CopilotSession("session-factory-reads", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "factory-reads",
                description: "Read factory state from a factory body",
                phases: [],
            },
            run: async ({ session: contextSession }) => {
                const [run, runs] = await Promise.all([
                    contextSession.factory.getRun("other-run"),
                    contextSession.factory.listRuns(),
                    contextSession.factory.cancel("other-run"),
                ]);
                return { runId: run.runId, runCount: runs.length };
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "factory-reads",
                runId: "run-factory-reads",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: { runId: "other-run", runCount: 0 } });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.getRun", {
            sessionId: session.sessionId,
            runId: "other-run",
        });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.listRuns", {
            sessionId: session.sessionId,
        });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.cancel", {
            sessionId: session.sessionId,
            runId: "other-run",
        });
    });

    it("allows factory.run after a factory body returns", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.run") {
                return { runId: "run-after-body", status: "completed", result: "started" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-after-body", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "returns",
                description: "Return before a separate factory run",
                phases: [],
            },
            run: async () => "finished",
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "returns",
                runId: "run-returns",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "finished" });
        await expect(session.factory.run("after-body")).resolves.toMatchObject({
            status: "completed",
            result: "started",
        });
    });

    it("allows a factory-body timer to start a factory after the body settles", async () => {
        const delayedRun = Promise.withResolvers<unknown>();
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.run") {
                return { runId: "run-from-timer", status: "completed", result: "started" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-timer", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "timer",
                description: "Start a factory from an unawaited timer",
                phases: [],
            },
            run: async () => {
                setTimeout(() => {
                    void session.factory
                        .run("from-timer")
                        .then(delayedRun.resolve, delayedRun.reject);
                }, 0);
                return "finished";
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "timer",
                runId: "run-timer",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "finished" });
        await expect(delayedRun.promise).resolves.toMatchObject({
            status: "completed",
            result: "started",
        });
    });

    it("flushes progress incrementally while a factory body is awaiting", async () => {
        const sendRequest = vi.fn(async () => ({}));
        const session = new CopilotSession("session-live-progress", { sendRequest } as never);
        const body = Promise.withResolvers<void>();
        const factory = defineFactory({
            meta: {
                name: "live-progress",
                description: "Incremental progress test",
                phases: [],
            },
            run: async ({ log }) => {
                log("before await");
                await body.promise;
                return "done";
            },
        });
        session.registerFactories([factory]);

        const execution = session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "live-progress",
            runId: "run-live-progress",
            executionToken: "execution-token",
            args: {},
        });
        await vi.waitFor(() => {
            expect(sendRequest).toHaveBeenCalledWith("session.factory.log", {
                sessionId: session.sessionId,
                runId: "run-live-progress",
                executionToken: "execution-token",
                lines: [{ seq: 0, kind: "log", text: "before await" }],
            });
        });

        body.resolve();
        await expect(execution).resolves.toEqual({ result: "done" });
    });

    it("calls factory.agent with the current run id and returns its text", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.agent") {
                return { result: "pong" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-agent", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "agent",
                description: "Agent context test",
                phases: [],
            },
            run: async ({ agent }) =>
                agent("Reply with pong", {
                    label: "Pong helper",
                    model: "gpt-test",
                    schema: { type: "string" },
                    effort: "high",
                } as FactoryAgentOptions),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "agent",
                runId: "run-agent",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "pong" });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.agent", {
            sessionId: session.sessionId,
            factoryRunId: "run-agent",
            executionToken: "execution-token",
            prompt: "Reply with pong",
            opts: {
                label: "Pong helper",
                model: "gpt-test",
                schema: { type: "string" },
            },
        });
    });

    it("forwards every declared factory.agent option", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.agent") {
                return { result: "pong" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-agent-options", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "agent-options",
                description: "Agent option forwarding test",
                phases: [],
            },
            run: async ({ agent }) =>
                agent("Reply with pong", {
                    label: "Pong helper",
                    model: "gpt-test",
                    schema: { type: "string" },
                    agent: "reviewer",
                    reasoningEffort: "high",
                    contextTier: "long_context",
                }),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "agent-options",
                runId: "run-agent-options",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "pong" });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.agent", {
            sessionId: session.sessionId,
            factoryRunId: "run-agent-options",
            executionToken: "execution-token",
            prompt: "Reply with pong",
            opts: {
                label: "Pong helper",
                model: "gpt-test",
                schema: { type: "string" },
                agent: "reviewer",
                reasoningEffort: "high",
                contextTier: "long_context",
            },
        });
    });

    it("sends empty factory.agent options when none are supplied", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.agent") {
                return { result: "pong" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-empty-agent-options", {
            sendRequest,
        } as never);
        const factory = defineFactory({
            meta: {
                name: "empty-agent-options",
                description: "Empty agent option forwarding test",
                phases: [],
            },
            run: async ({ agent }) => agent("Reply with pong"),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "empty-agent-options",
                runId: "run-empty-agent-options",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "pong" });
        expect(sendRequest).toHaveBeenCalledWith("session.factory.agent", {
            sessionId: session.sessionId,
            factoryRunId: "run-empty-agent-options",
            executionToken: "execution-token",
            prompt: "Reply with pong",
            opts: {},
        });
    });

    it("keeps each execution token on callbacks from overlapping contexts with the same run id", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.agent") {
                return { result: "agent result" };
            }
            if (method === "session.factory.journal.get") {
                return { hit: false };
            }
            return {};
        });
        const session = new CopilotSession("session-overlapping-attempts", {
            sendRequest,
        } as never);
        const contexts: FactoryContext[] = [];
        const bodies = [Promise.withResolvers<void>(), Promise.withResolvers<void>()];
        const contextsReady = Promise.withResolvers<void>();
        const factory = defineFactory({
            meta: {
                name: "overlapping-attempts",
                description: "Execution token capture test",
                phases: [],
            },
            run: async (context) => {
                const invocation = contexts.length;
                contexts.push(context);
                if (contexts.length === 2) {
                    contextsReady.resolve();
                }
                await bodies[invocation].promise;
                return `attempt ${invocation + 1}`;
            },
        });
        session.registerFactories([factory]);
        const first = session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "overlapping-attempts",
            runId: "shared-run",
            executionToken: "old-token",
            args: {},
        });
        const second = session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "overlapping-attempts",
            runId: "shared-run",
            executionToken: "current-token",
            args: {},
        });
        await contextsReady.promise;

        contexts[0].log("stale log");
        await contexts[0].agent("stale agent");
        await contexts[0].step("stale journal", () => "stale result");
        await contexts[1].agent("current agent");

        expect(sendRequest).toHaveBeenCalledWith(
            "session.factory.log",
            expect.objectContaining({ executionToken: "old-token" })
        );
        expect(sendRequest).toHaveBeenCalledWith(
            "session.factory.agent",
            expect.objectContaining({ executionToken: "old-token", prompt: "stale agent" })
        );
        expect(sendRequest).toHaveBeenCalledWith(
            "session.factory.journal.get",
            expect.objectContaining({ executionToken: "old-token", key: "stale journal" })
        );
        expect(sendRequest).toHaveBeenCalledWith(
            "session.factory.journal.put",
            expect.objectContaining({ executionToken: "old-token", key: "stale journal" })
        );
        expect(sendRequest).toHaveBeenCalledWith(
            "session.factory.agent",
            expect.objectContaining({ executionToken: "current-token", prompt: "current agent" })
        );

        bodies[0].resolve();
        bodies[1].resolve();
        await expect(first).resolves.toEqual({ result: "attempt 1" });
        await expect(second).resolves.toEqual({ result: "attempt 2" });
    });

    it("runs a durable step once, serves cached null, and does not cache failures", async () => {
        const journal = new Map<string, unknown>();
        const sendRequest = vi.fn(
            async (method: string, params: { key?: string; resultJson?: unknown }) => {
                if (method === "session.factory.journal.get") {
                    return journal.has(params.key!)
                        ? { hit: true, resultJson: journal.get(params.key!) }
                        : { hit: false };
                }
                if (method === "session.factory.journal.put") {
                    journal.set(params.key!, params.resultJson);
                    return {};
                }
                throw new Error(`Unexpected method: ${method}`);
            }
        );
        const session = new CopilotSession("session-step", { sendRequest } as never);
        let cachedProducerCalls = 0;
        let failingProducerCalls = 0;
        const factory = defineFactory({
            meta: {
                name: "step",
                description: "Durable step context test",
                phases: [],
            },
            run: async ({ step }) => {
                const first = await step("cached-null", async () => {
                    cachedProducerCalls++;
                    return null;
                });
                const second = await step("cached-null", async () => {
                    cachedProducerCalls++;
                    return "wrong";
                });
                const failed = await step("retry", async () => {
                    failingProducerCalls++;
                    throw new Error("transient");
                }).catch(() => "failed");
                const retried = await step("retry", async () => {
                    failingProducerCalls++;
                    return "recovered";
                });
                return { first, second, failed, retried };
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "step",
                runId: "run-step",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({
            result: { first: null, second: null, failed: "failed", retried: "recovered" },
        });
        expect(cachedProducerCalls).toBe(1);
        expect(failingProducerCalls).toBe(2);
        expect(
            sendRequest.mock.calls.filter(([method]) => method === "session.factory.journal.put")
        ).toHaveLength(2);
    });

    it.each([
        ["undefined", () => undefined],
        ["NaN", () => Number.NaN],
        ["Infinity", () => Number.POSITIVE_INFINITY],
        ["function", () => () => undefined],
        ["symbol", () => Symbol("invalid")],
        ["BigInt", () => 1n],
        [
            "cycle",
            () => {
                const value: Record<string, unknown> = {};
                value.self = value;
                return value;
            },
        ],
        ["non-plain object", () => new Date()],
        [
            "accessor property",
            () => Object.defineProperty({}, "value", { enumerable: true, get: () => "hidden" }),
        ],
        [
            "non-enumerable property",
            () => Object.defineProperty({}, "value", { enumerable: false, value: "hidden" }),
        ],
        ["array hole", () => new Array(1)],
        [
            "array accessor",
            () => Object.defineProperty([], "0", { enumerable: true, get: () => "hidden" }),
        ],
        ["array extra key", () => Object.assign([1], { extra: "dropped" })],
    ])("rejects a journaled step %s result", async (_label, makeValue) => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.journal.get") {
                return { hit: false };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-invalid-step", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "invalid-step",
                description: "Rejects lossy step values",
                phases: [],
            },
            run: async ({ step }) => {
                await step("invalid", async () => makeValue() as never);
                return "must-not-complete";
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "invalid-step",
                runId: "run-invalid-step",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toMatchObject({
            data: {
                code: "factory_step_not_json",
            },
        });
        expect(
            sendRequest.mock.calls.filter(([method]) => method === "session.factory.journal.put")
        ).toHaveLength(0);
    });

    it("validates a journaled step cache hit before replay", async () => {
        const cached = Object.assign([1], { extra: "dropped" });
        const producer = vi.fn(async () => "must-not-run");
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.journal.get") {
                return { hit: true, resultJson: cached };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-invalid-step-cache", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "invalid-step-cache",
                description: "Rejects invalid cached values",
                phases: [],
            },
            run: async ({ step }) => step("cached", producer),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "invalid-step-cache",
                runId: "run-invalid-step-cache",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toMatchObject({
            data: {
                code: "factory_step_not_json",
                category: "unsupported_object",
            },
        });
        expect(producer).not.toHaveBeenCalled();
    });

    it("replays a journaled step value identically on resume", async () => {
        const journal = new Map<string, unknown>();
        const sendRequest = vi.fn(
            async (method: string, params: { key?: string; resultJson?: unknown }) => {
                if (method === "session.factory.journal.get") {
                    return journal.has(params.key!)
                        ? { hit: true, resultJson: journal.get(params.key!) }
                        : { hit: false };
                }
                if (method === "session.factory.journal.put") {
                    journal.set(params.key!, params.resultJson);
                    return {};
                }
                throw new Error(`Unexpected method: ${method}`);
            }
        );
        const producer = vi.fn(async () => ({ nested: [1, null, "same"] }));
        const session = new CopilotSession("session-step-replay", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "step-replay",
                description: "Replays strict JSON",
                phases: [],
            },
            run: async ({ step }) => step("same", producer),
        });
        session.registerFactories([factory]);

        const first = await session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "step-replay",
            runId: "run-step-replay",
            executionToken: "execution-token",
            args: {},
        });
        const replay = await session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "step-replay",
            runId: "run-step-replay",
            executionToken: "execution-token",
            args: {},
        });

        expect(replay).toEqual(first);
        expect(producer).toHaveBeenCalledOnce();
    });

    it("bypasses validation and journaling for a volatile step", async () => {
        const sendRequest = vi.fn();
        const session = new CopilotSession("session-volatile-step", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "volatile-step",
                description: "Allows author-opted-out volatile values",
                phases: [],
            },
            run: async ({ step }) => {
                const value = await step("volatile", async () => (() => "not JSON") as never, {
                    volatile: true,
                });
                expect(typeof value).toBe("function");
                return "completed";
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "volatile-step",
                runId: "run-volatile-step",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "completed" });
        expect(sendRequest).not.toHaveBeenCalled();
    });

    it("does not start a volatile step producer after the run is aborted", async () => {
        const sendRequest = vi.fn();
        const session = new CopilotSession("session-volatile-abort", { sendRequest } as never);
        let producerRan = false;
        const factory = defineFactory({
            meta: {
                name: "volatile-abort",
                description: "Volatile steps honour cancellation",
                phases: [],
            },
            run: async ({ step, runId }) => {
                // Abort mid-run, then attempt a volatile step. The producer must
                // not run: cancellation has to stop new extension work starting,
                // exactly as it does on the journaled path.
                await session.clientSessionApis.factory!.abort({
                    sessionId: session.sessionId,
                    runId,
                });
                await step(
                    "volatile",
                    () => {
                        producerRan = true;
                        return "should not happen";
                    },
                    { volatile: true }
                );
                return "completed";
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "volatile-abort",
                runId: "run-volatile-abort",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toThrow();
        expect(producerRan).toBe(false);
    });

    it("rejects a factory result array with an extra own key", async () => {
        const factory = defineFactory({
            meta: {
                name: "array-extra-result",
                description: "Rejects lossy array keys",
                phases: [],
            },
            run: async () => Object.assign([1], { extra: 1n }) as never,
        });
        const session = new CopilotSession("session-array-extra-result", {} as never);
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "array-extra-result",
                runId: "run-array-extra-result",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toMatchObject({
            data: {
                code: "factory_result_not_json",
                category: "unsupported_object",
            },
        });
    });

    it("exposes factory getRun and forwards the run id", async () => {
        const envelope = { runId: "run-read", status: "error", error: "failed" };
        const sendRequest = vi.fn(async () => envelope);
        const session = new CopilotSession("session-read", { sendRequest } as never);

        await expect(session.factory.getRun("run-read")).resolves.toEqual(envelope);
        expect(sendRequest).toHaveBeenCalledWith("session.factory.getRun", {
            sessionId: session.sessionId,
            runId: "run-read",
        });
    });

    it("exposes factory observability methods and forwards paging options", async () => {
        const summary = {
            runId: "run-observe",
            factoryName: "observe",
            description: "Observe",
            status: "running" as const,
            revision: 4,
            createdAt: 1,
            startedAt: 2,
            updatedAt: 3,
            completedAt: null,
            currentPhase: { id: "p0", ordinal: 0 },
            declaredPhaseCount: 1,
            liveAgentCount: 1,
            totalSpawnedAgentCount: 1,
            consumed: { activeMs: 10, subagents: 1, nanoAiu: 5 },
            declaredLimits: {},
            approved: {},
            observedAt: 4,
            activeSegmentStartedAt: 2,
            terminal: null,
        };
        const progress = {
            records: [],
            oldestSeq: null,
            newestSeq: null,
            hasMoreOlder: false,
            hasMoreNewer: false,
            revision: 4,
        };
        const detail = { ...summary, phases: [], agents: [], progress };
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.listRuns") return { runs: [summary] };
            if (method === "session.factory.getRunDetail") return detail;
            return progress;
        });
        const session = new CopilotSession("session-observe", { sendRequest } as never);

        await expect(session.factory.listRuns()).resolves.toEqual([summary]);
        await expect(session.factory.getRunDetail("run-observe")).resolves.toEqual(detail);
        await expect(
            session.factory.getRunProgress("run-observe", {
                phaseId: "p0",
                afterSeq: 10,
                limit: 50,
            })
        ).resolves.toEqual(progress);
        expect(sendRequest).toHaveBeenNthCalledWith(1, "session.factory.listRuns", {
            sessionId: session.sessionId,
        });
        expect(sendRequest).toHaveBeenNthCalledWith(2, "session.factory.getRunDetail", {
            sessionId: session.sessionId,
            runId: "run-observe",
        });
        expect(sendRequest).toHaveBeenNthCalledWith(3, "session.factory.getRunProgress", {
            sessionId: session.sessionId,
            runId: "run-observe",
            phaseId: "p0",
            afterSeq: 10,
            limit: 50,
        });
    });

    it("exposes factory cancel and forwards the run id", async () => {
        const envelope = { runId: "run-cancel", status: "cancelled", reason: "cancelled" };
        const sendRequest = vi.fn(async () => envelope);
        const session = new CopilotSession("session-cancel", { sendRequest } as never);

        await expect(session.factory.cancel("run-cancel")).resolves.toEqual(envelope);
        expect(sendRequest).toHaveBeenCalledWith("session.factory.cancel", {
            sessionId: session.sessionId,
            runId: "run-cancel",
        });
    });

    it("runs parallel as a barrier and maps a throwing thunk to null", async () => {
        const first = Promise.withResolvers<string>();
        const second = Promise.withResolvers<string>();
        const started: string[] = [];
        const session = new CopilotSession("session-parallel", {} as never);
        const factory = defineFactory({
            meta: {
                name: "parallel",
                description: "Parallel combinator test",
                phases: [],
            },
            run: async ({ parallel }) =>
                parallel([
                    async () => {
                        started.push("first");
                        return first.promise;
                    },
                    async () => {
                        started.push("second");
                        return second.promise;
                    },
                    async () => {
                        started.push("throwing");
                        throw new Error("expected");
                    },
                ]),
        });
        session.registerFactories([factory]);

        let settled = false;
        const execution = session.clientSessionApis
            .factory!.execute({
                sessionId: session.sessionId,
                name: "parallel",
                runId: "run-parallel",
                args: {},
            })
            .finally(() => {
                settled = true;
            });
        await vi.waitFor(() => expect(started).toEqual(["first", "second", "throwing"]));

        second.resolve("second");
        await Promise.resolve();
        expect(settled).toBe(false);

        first.resolve("first");
        await expect(execution).resolves.toEqual({ result: ["first", "second", null] });
    });

    it("rejects already-invoked promises passed to parallel with a clear diagnostic", async () => {
        const session = new CopilotSession("session-parallel-promises", {} as never);
        const factory = defineFactory({
            meta: {
                name: "parallel-promises",
                description: "Parallel misuse diagnostic",
                phases: [],
            },
            run: async ({ parallel }) =>
                parallel([Promise.resolve("already running")] as unknown as Array<
                    () => Promise<string>
                >),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "parallel-promises",
                runId: "run-parallel-promises",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toThrow(
            "parallel() expects an array of functions, not promises. Wrap each call: () => agent(...)"
        );
    });

    it("flows pipeline items independently and drops only the item whose stage throws", async () => {
        const releaseFirstItem = Promise.withResolvers<void>();
        const secondStageStarted = Promise.withResolvers<void>();
        const finalStageItems: string[] = [];
        const session = new CopilotSession("session-pipeline", {} as never);
        const factory = defineFactory({
            meta: {
                name: "pipeline",
                description: "Pipeline combinator test",
                phases: [],
            },
            run: async ({ pipeline }) =>
                pipeline(
                    ["slow", "fast", "throw"],
                    async (_previous, item) => {
                        if (item === "slow") {
                            await releaseFirstItem.promise;
                        }
                        if (item === "throw") {
                            throw new Error("expected");
                        }
                        return `${item}-stage-1`;
                    },
                    async (previous, item) => {
                        if (item === "fast") {
                            secondStageStarted.resolve();
                        }
                        finalStageItems.push(item as string);
                        return `${previous}-stage-2`;
                    }
                ),
        });
        session.registerFactories([factory]);

        const execution = session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "pipeline",
            runId: "run-pipeline",
            executionToken: "execution-token",
            args: {},
        });
        await secondStageStarted.promise;
        expect(finalStageItems).toEqual(["fast"]);

        releaseFirstItem.resolve();
        await expect(execution).resolves.toEqual({
            result: ["slow-stage-1-stage-2", "fast-stage-1-stage-2", null],
        });
        expect(finalStageItems).toEqual(["fast", "slow"]);
    });

    it("enforces the 4096-item cap for parallel and pipeline", async () => {
        const session = new CopilotSession("session-fanout-cap", {} as never);
        const factory = defineFactory({
            meta: {
                name: "fanout-cap",
                description: "Fan-out cap test",
                phases: [],
            },
            run: async ({ parallel, pipeline }) => {
                const tooManyItems = Array.from({ length: 4097 }, () => null);
                const parallelError = await parallel(
                    tooManyItems.map(() => async () => null)
                ).catch((error: unknown) => error);
                const pipelineError = await pipeline(tooManyItems).catch((error: unknown) => error);
                return {
                    parallel: (parallelError as Error).message,
                    pipeline: (pipelineError as Error).message,
                };
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "fanout-cap",
                runId: "run-fanout-cap",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({
            result: {
                parallel: "parallel() accepts at most 4096 items; got 4097.",
                pipeline: "pipeline() accepts at most 4096 items; got 4097.",
            },
        });
    });

    it("does not deadlock nested combinators when only leaf agents use a one-slot limiter", async () => {
        let active = 0;
        let maxActive = 0;
        let tail = Promise.resolve();
        const sendRequest = vi.fn(
            async (method: string, params: { prompt: string }): Promise<{ result: string }> => {
                if (method !== "session.factory.agent") {
                    throw new Error(`Unexpected method: ${method}`);
                }
                const previous = tail;
                const done = Promise.withResolvers<void>();
                tail = done.promise;
                await previous;
                active++;
                maxActive = Math.max(maxActive, active);
                await Promise.resolve();
                active--;
                done.resolve();
                return { result: params.prompt };
            }
        );
        const session = new CopilotSession("session-nested-combinators", {
            sendRequest,
        } as never);
        const factory = defineFactory({
            meta: {
                name: "nested-combinators",
                description: "Nested combinator deadlock regression",
                phases: [],
            },
            run: async ({ agent, parallel, pipeline }) =>
                parallel([
                    () => parallel([() => agent("a"), () => agent("b")]),
                    () => pipeline(["c"], (_previous, item) => agent(item as string)),
                ]),
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "nested-combinators",
                runId: "run-nested-combinators",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: [["a", "b"], ["c"]] });
        expect(maxActive).toBe(1);
        expect(sendRequest).toHaveBeenCalledTimes(3);
    });

    it("flushes buffered progress in finally when the factory body throws", async () => {
        const sendRequest = vi.fn(async () => ({}));
        const session = new CopilotSession("session-throw-progress", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "throw-progress",
                description: "Throwing progress test",
                phases: [],
            },
            run: async ({ log }) => {
                log("before throw");
                throw new Error("body failed");
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "throw-progress",
                runId: "run-throw-progress",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toThrow("body failed");
        expect(sendRequest).toHaveBeenCalledWith("session.factory.log", {
            sessionId: session.sessionId,
            runId: "run-throw-progress",
            executionToken: "execution-token",
            lines: [{ seq: 0, kind: "log", text: "before throw" }],
        });
    });

    it("keeps a completed execution successful when only the final progress flush fails", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.log") {
                throw new Error("final transport failure");
            }
            return {};
        });
        const warning = vi.spyOn(console, "warn").mockImplementation(() => {});
        const session = new CopilotSession("session-final-flush-failure", {
            sendRequest,
        } as never);
        const factory = defineFactory({
            meta: {
                name: "final-flush-failure",
                description: "Final flush failure regression test",
                phases: [],
            },
            run: async ({ log }) => {
                log("final line");
                return "done";
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "final-flush-failure",
                runId: "run-final-flush-failure",
                executionToken: "execution-token",
                args: {},
            })
        ).resolves.toEqual({ result: "done" });
        expect(warning).toHaveBeenCalledWith(
            "Failed to flush final factory progress after the factory body settled",
            expect.objectContaining({ message: "final transport failure" })
        );
    });

    it("keeps a completed execution successful when a background progress flush fails", async () => {
        vi.useFakeTimers();
        const release = Promise.withResolvers<void>();
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.log") {
                throw new Error("background transport failure");
            }
            return {};
        });
        const warning = vi.spyOn(console, "warn").mockImplementation(() => {});
        const session = new CopilotSession("session-background-flush-failure", {
            sendRequest,
        } as never);
        const factory = defineFactory({
            meta: {
                name: "background-flush-failure",
                description: "Background flush failure regression test",
                phases: [],
            },
            run: async ({ log }) => {
                log("background line");
                await release.promise;
                return "done";
            },
        });
        session.registerFactories([factory]);

        try {
            const execution = session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "background-flush-failure",
                runId: "run-background-flush-failure",
                executionToken: "execution-token",
                args: {},
            });
            await vi.advanceTimersByTimeAsync(10_000);
            await Promise.resolve();

            release.resolve();

            await expect(execution).resolves.toEqual({ result: "done" });
            expect(warning).toHaveBeenCalledWith(
                "Ignoring a background factory progress flush failure after the factory body settled",
                expect.objectContaining({ message: "background transport failure" })
            );
        } finally {
            vi.useRealTimers();
        }
    });

    it("keeps a mid-run progress flush failure fatal", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.log") {
                throw new Error("mid-run transport failure");
            }
            if (method === "session.factory.agent") {
                return { result: "must not complete" };
            }
            return {};
        });
        const session = new CopilotSession("session-mid-run-flush-failure", {
            sendRequest,
        } as never);
        const factory = defineFactory({
            meta: {
                name: "mid-run-flush-failure",
                description: "Mid-run flush failure regression test",
                phases: [],
            },
            run: async ({ agent, log }) => {
                log("before agent");
                return agent("trigger a flush");
            },
        });
        session.registerFactories([factory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "mid-run-flush-failure",
                runId: "run-mid-run-flush-failure",
                executionToken: "execution-token",
                args: {},
            })
        ).rejects.toThrow("mid-run transport failure");
        expect(sendRequest).not.toHaveBeenCalledWith("session.factory.agent", expect.anything());
    });

    it("surfaces the per-run abort signal on the factory context", async () => {
        const session = new CopilotSession("session-abort-signal", {} as never);
        const signalSeen = Promise.withResolvers<AbortSignal>();
        const factory = defineFactory({
            meta: {
                name: "abort-signal",
                description: "Abort signal test",
                phases: [],
            },
            run: async ({ signal }) => {
                signalSeen.resolve(signal);
                await new Promise<void>((resolve) =>
                    signal.addEventListener("abort", () => resolve(), { once: true })
                );
                return signal.aborted;
            },
        });
        session.registerFactories([factory]);

        const execution = session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "abort-signal",
            runId: "run-abort-signal",
            executionToken: "execution-token",
            args: {},
        });
        const signal = await signalSeen.promise;
        expect(signal.aborted).toBe(false);

        await session.clientSessionApis.factory!.abort({
            sessionId: session.sessionId,
            runId: "run-abort-signal",
        });

        expect(signal.aborted).toBe(true);
        await expect(execution).resolves.toEqual({ result: true });
    });

    it("rejects an in-flight runtime-backed await when factory.abort trips the signal", async () => {
        const agentResponse = Promise.withResolvers<{ result: string }>();
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.factory.agent") {
                return agentResponse.promise;
            }
            return {};
        });
        const session = new CopilotSession("session-abort-await", { sendRequest } as never);
        const factory = defineFactory({
            meta: {
                name: "abort-await",
                description: "Abort an in-flight factory await",
                phases: [],
            },
            run: async ({ agent }) => agent("wait forever"),
        });
        session.registerFactories([factory]);

        const execution = session.clientSessionApis.factory!.execute({
            sessionId: session.sessionId,
            name: "abort-await",
            runId: "run-abort-await",
            executionToken: "execution-token",
            args: {},
        });
        await vi.waitFor(() =>
            expect(sendRequest).toHaveBeenCalledWith("session.factory.agent", expect.anything())
        );

        await session.clientSessionApis.factory!.abort({
            sessionId: session.sessionId,
            runId: "run-abort-await",
        });

        await expect(execution).rejects.toMatchObject({ name: "AbortError" });
        agentResponse.resolve({ result: "late" });
    });

    it.each(["parallel", "pipeline"] as const)(
        "propagates cancellation out of %s instead of mapping it to null",
        async (combinator) => {
            const agentResponse = Promise.withResolvers<{ result: string }>();
            const sendRequest = vi.fn(async (method: string) => {
                if (method === "session.factory.agent") {
                    return agentResponse.promise;
                }
                return {};
            });
            const session = new CopilotSession("session-abort-parallel", { sendRequest } as never);
            const factory = defineFactory({
                meta: {
                    name: `abort-${combinator}`,
                    description: "Cancellation must bubble out of a combinator",
                    phases: [],
                },
                // If the combinator swallowed the AbortError to null, this run would
                // resolve successfully with [null] despite the run being cancelled.
                run: async ({ agent, parallel, pipeline }) =>
                    combinator === "parallel"
                        ? parallel([() => agent("wait forever")])
                        : pipeline(["wait forever"], (_previous, item) => agent(item as string)),
            });
            session.registerFactories([factory]);

            const execution = session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: `abort-${combinator}`,
                runId: `run-abort-${combinator}`,
                executionToken: "execution-token",
                args: {},
            });
            await vi.waitFor(() =>
                expect(sendRequest).toHaveBeenCalledWith("session.factory.agent", expect.anything())
            );

            await session.clientSessionApis.factory!.abort({
                sessionId: session.sessionId,
                runId: `run-abort-${combinator}`,
            });

            await expect(execution).rejects.toMatchObject({ name: "AbortError" });
            agentResponse.resolve({ result: "late" });
        }
    );

    it("dispatches factory.execute to the registered factory selected by name", async () => {
        const firstRun = vi.fn(async () => ({ selected: "first" }));
        const secondRun = vi.fn(async ({ args, log }) => {
            log("executing");
            return { selected: "second", echoed: args };
        });
        const firstFactory = defineFactory({
            meta: {
                name: "first",
                description: "First factory",
                phases: [],
            },
            run: firstRun,
        });
        const secondFactory = defineFactory({
            meta: {
                name: "second",
                description: "Second factory",
                phases: [],
            },
            run: secondRun,
        });
        const session = new CopilotSession("session-execute", {
            sendRequest: vi.fn(async () => ({})),
        } as never);
        session.registerFactories([firstFactory, secondFactory]);

        await expect(
            session.clientSessionApis.factory!.execute({
                sessionId: session.sessionId,
                name: "second",
                runId: "run-echo",
                executionToken: "execution-token",
                args: { message: "hello" },
            })
        ).resolves.toEqual({
            result: { selected: "second", echoed: { message: "hello" } },
        });
        expect(firstRun).not.toHaveBeenCalled();
        expect(secondRun).toHaveBeenCalledOnce();

        const error = await session.clientSessionApis
            .factory!.execute({
                sessionId: session.sessionId,
                name: "missing",
                runId: "run-missing",
                args: {},
            })
            .catch((caught: unknown) => caught);
        expect(error).toBeInstanceOf(ResponseError);
        expect((error as ResponseError<{ code: string; name: string }>).data).toEqual({
            code: "factory_not_found",
            name: "missing",
        });
    });

    it("runs fresh factories and routes direct and legacy resumes by ID without args", async () => {
        const factory = defineFactory({
            meta: {
                name: "friendly-run",
                description: "Friendly run wrapper",
                phases: [],
            },
            run: async () => ({ unused: true }),
        });
        const sendRequest = vi.fn(async (method: string, params: { name?: string }) =>
            method === "session.factory.resume"
                ? {
                      factoryName: "stored-name",
                      run: {
                          runId: "run-prior",
                          status: "completed",
                          result: { name: "stored-name", persistedArgs: true },
                      },
                  }
                : {
                      runId: "run-foreground",
                      status: "completed",
                      result: { name: params.name },
                  }
        );
        const session = new CopilotSession("session-run", { sendRequest } as never);

        await expect(
            session.factory.resume("run-prior", {
                limits: { maxTotalSubagents: 7 },
            })
        ).resolves.toMatchObject({
            status: "completed",
            result: { name: "stored-name", persistedArgs: true },
        });
        await expect(
            session.factory.run("by-name", {
                args: { value: 1 },
                limits: { maxTotalSubagents: 7 },
                resumeFromRunId: "run-prior",
            })
        ).resolves.toMatchObject({
            status: "completed",
            result: { name: "stored-name", persistedArgs: true },
        });
        await expect(session.factory.run(factory)).resolves.toMatchObject({
            status: "completed",
            result: { name: "friendly-run" },
        });
        expect(sendRequest).toHaveBeenNthCalledWith(1, "session.factory.resume", {
            sessionId: session.sessionId,
            runId: "run-prior",
            limits: { maxTotalSubagents: 7 },
        });
        expect(sendRequest).toHaveBeenNthCalledWith(2, "session.factory.resume", {
            sessionId: session.sessionId,
            runId: "run-prior",
            limits: { maxTotalSubagents: 7 },
        });
        expect(sendRequest).toHaveBeenNthCalledWith(3, "session.factory.run", {
            sessionId: session.sessionId,
            name: "friendly-run",
            args: {},
            options: { limits: undefined },
        });
    });

    it("returns the full envelope for a failed foreground run", async () => {
        const envelope = {
            runId: "run-error",
            status: "error" as const,
            error: "factory failed",
            snapshot: { completed: 1 },
        };
        const session = new CopilotSession("session-error", {
            sendRequest: vi.fn(async () => envelope),
        } as never);

        // A run that exists resolves with its envelope; only pre-execution
        // failures (no run id) reject.
        await expect(session.factory.run("failing")).resolves.toEqual(envelope);
    });

    it.each([
        "not_found",
        "non_resumable",
        "already_active",
        "factory_already_running",
        "factory_limits_invalid",
        "factory_session_disposed",
        "factory_storage_unavailable",
        "factory_storage_corrupt",
    ] as const)(
        "throws FactoryResumeError with code %s for pre-execution failures",
        async (code) => {
            const session = new CopilotSession("session-resume-error", {
                sendRequest: vi.fn(async () => {
                    throw new ResponseError(-32602, `resume failed: ${code}`, { code });
                }),
            } as never);

            const error = await session.factory
                .resume("run-error")
                .catch((caught: unknown) => caught);
            expect(error).toBeInstanceOf(FactoryResumeError);
            expect((error as FactoryResumeError).code).toBe(code);
        }
    );

    it("leaves an unreachable permission_denied response as a raw ResponseError", async () => {
        const session = new CopilotSession("session-resume-permission-denied", {
            sendRequest: vi.fn(async () => {
                throw new ResponseError(-32602, "resume failed: permission_denied", {
                    code: "permission_denied",
                });
            }),
        } as never);

        const error = await session.factory.resume("run-error").catch((caught: unknown) => caught);
        expect(error).toBeInstanceOf(ResponseError);
        expect(error).not.toBeInstanceOf(FactoryResumeError);
        expect((error as ResponseError<{ code: string }>).data.code).toBe("permission_denied");
    });

    it("returns resumed execution failures as envelopes", async () => {
        const envelope = {
            runId: "run-execution-error",
            status: "error" as const,
            error: "resumed body failed",
        };
        const session = new CopilotSession("session-resumed-run-error", {
            sendRequest: vi.fn(async () => ({ factoryName: "stored-name", run: envelope })),
        } as never);

        await expect(session.factory.resume("run-execution-error")).resolves.toEqual(envelope);
    });
});

describe("factory run settlement", () => {
    it.each([
        ["completed", true],
        ["error", true],
        ["halted", true],
        ["cancelled", true],
        ["pending", false],
        ["running", false],
    ] as const)("classifies %s as terminal=%s", (status, expected) => {
        expect(isFactoryRunTerminal(status)).toBe(expected);
    });

    it("resolves immediately when the run has already settled", async () => {
        const envelope = { runId: "run-settled", status: "completed" as const, result: 42 };
        const sendRequest = vi.fn(async () => envelope);
        const session = new CopilotSession("session-wait-settled", { sendRequest } as never);

        await expect(session.factory.waitForRun("run-settled")).resolves.toEqual(envelope);
        expect(sendRequest).toHaveBeenCalledTimes(1);
        expect(sendRequest).toHaveBeenCalledWith("session.factory.getRun", {
            sessionId: session.sessionId,
            runId: "run-settled",
        });
    });

    it("waits for a running run to reach a terminal status", async () => {
        const running = { runId: "run-wait", status: "running" as const };
        const terminal = { runId: "run-wait", status: "completed" as const, result: "done" };
        let current: unknown = running;
        const sendRequest = vi.fn(async () => current);
        const session = new CopilotSession("session-wait-running", { sendRequest } as never);

        const settled = session.factory.waitForRun("run-wait");
        // The first read observed a running envelope, so the wait is still pending.
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));

        // An invalidation event for an unrelated run must not trigger a re-read.
        (session as never as { _dispatchEvent(event: unknown): void })._dispatchEvent(
            runUpdatedEvent("some-other-run", 2)
        );
        expect(sendRequest).toHaveBeenCalledTimes(1);

        current = terminal;
        (session as never as { _dispatchEvent(event: unknown): void })._dispatchEvent(
            runUpdatedEvent("run-wait", 3)
        );

        await expect(settled).resolves.toEqual(terminal);
    });

    it("periodically re-reads when a terminal invalidation is missed", async () => {
        vi.useFakeTimers();
        const running = { runId: "run-poll", status: "running" as const };
        const terminal = { runId: "run-poll", status: "completed" as const, result: "polled" };
        let current: unknown = running;
        const sendRequest = vi.fn(async () => current);
        const session = new CopilotSession("session-wait-poll", { sendRequest } as never);

        try {
            const settled = session.factory.waitForRun("run-poll");
            await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));

            current = terminal;
            await vi.advanceTimersByTimeAsync(5_000);

            await expect(settled).resolves.toEqual(terminal);
            expect(sendRequest).toHaveBeenCalledTimes(2);
        } finally {
            vi.useRealTimers();
        }
    });

    it("stops watching once the run settles", async () => {
        const running = { runId: "run-unsub", status: "running" as const };
        const terminal = { runId: "run-unsub", status: "error" as const, error: "body failed" };
        let current: unknown = running;
        const sendRequest = vi.fn(async () => current);
        const session = new CopilotSession("session-wait-unsub", { sendRequest } as never);
        const handlersFor = (): Set<unknown> | undefined =>
            (
                session as never as {
                    typedEventHandlers: Map<string, Set<unknown>>;
                }
            ).typedEventHandlers.get("factory.run_updated");

        const settled = session.factory.waitForRun("run-unsub");
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));
        expect(handlersFor()?.size ?? 0).toBe(1);

        current = terminal;
        (session as never as { _dispatchEvent(event: unknown): void })._dispatchEvent(
            runUpdatedEvent("run-unsub", 2)
        );
        await expect(settled).resolves.toEqual(terminal);

        // The subscription must be released, or every completed wait leaks a
        // listener for the lifetime of the session.
        expect(handlersFor()?.size ?? 0).toBe(0);

        const callsAtSettlement = sendRequest.mock.calls.length;
        // A late event for a settled run must not provoke another read.
        (session as never as { _dispatchEvent(event: unknown): void })._dispatchEvent(
            runUpdatedEvent("run-unsub", 3)
        );
        expect(sendRequest).toHaveBeenCalledTimes(callsAtSettlement);
    });

    it("rejects when the signal is already aborted and never reads", async () => {
        const sendRequest = vi.fn(async () => ({ runId: "run-pre", status: "running" }));
        const session = new CopilotSession("session-wait-pre-abort", { sendRequest } as never);

        await expect(
            session.factory.waitForRun("run-pre", { signal: AbortSignal.abort() })
        ).rejects.toThrow();
        expect(sendRequest).not.toHaveBeenCalled();
    });

    it("rejects when aborted while waiting, leaving the run untouched", async () => {
        const sendRequest = vi.fn(async () => ({ runId: "run-abort", status: "running" }));
        const session = new CopilotSession("session-wait-abort", { sendRequest } as never);
        const controller = new AbortController();

        const settled = session.factory.waitForRun("run-abort", { signal: controller.signal });
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));

        controller.abort();
        await expect(settled).rejects.toThrow();
        // Aborting the wait must not cancel the run.
        expect(sendRequest).not.toHaveBeenCalledWith("session.factory.cancel", expect.anything());
    });

    it("propagates a read failure", async () => {
        const sendRequest = vi.fn(async () => {
            throw new Error("factory_storage_unavailable");
        });
        const session = new CopilotSession("session-wait-error", { sendRequest } as never);

        await expect(session.factory.waitForRun("run-broken")).rejects.toThrow(
            "factory_storage_unavailable"
        );
    });

    it("collapses a burst of invalidation events into one in-flight read", async () => {
        const running = { runId: "run-burst", status: "running" as const };
        const terminal = { runId: "run-burst", status: "completed" as const };
        let release: (() => void) | undefined;
        const gate = new Promise<void>((resolve) => (release = resolve));
        let readCount = 0;
        const sendRequest = vi.fn(async () => {
            readCount += 1;
            if (readCount === 2) {
                await gate;
            }
            // Reads 1 and 2 observe a running run; only the coalesced third
            // read observes the terminal one.
            return readCount >= 3 ? terminal : running;
        });
        const session = new CopilotSession("session-wait-burst", { sendRequest } as never);

        const settled = session.factory.waitForRun("run-burst");
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));

        const dispatch = (revision: number): void =>
            (session as never as { _dispatchEvent(event: unknown): void })._dispatchEvent(
                runUpdatedEvent("run-burst", revision)
            );

        // Second read is held open while three more events arrive; they must
        // collapse into a single follow-up read rather than three.
        dispatch(2);
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(2));
        dispatch(3);
        dispatch(4);
        dispatch(5);
        expect(sendRequest).toHaveBeenCalledTimes(2);

        release?.();
        await expect(settled).resolves.toEqual(terminal);
        // One initial read, the held read, and exactly one coalesced re-read
        // standing in for all three queued events.
        expect(sendRequest).toHaveBeenCalledTimes(3);
    });
});
