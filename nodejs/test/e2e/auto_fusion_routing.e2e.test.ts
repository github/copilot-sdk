/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { beforeEach, describe, expect, it, onTestFailed } from "vitest";
import { approveAll, CopilotRequestHandler, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { isByokBackend } from "./harness/testBackend.js";

const MODEL = "gpt-5.6-sol";
const REPLY = "AUTO_FUSION_EXECUTION_DONE";

class AutoFusionRequestHandler extends CopilotRequestHandler {
    readonly plans: Record<string, unknown>[] = [];
    readonly auto: Record<string, unknown>[] = [];
    readonly inference: Record<string, unknown>[] = [];
    failRoute = false;
    disabled = false;
    constituentsEnabled = true;
    inferenceGate?: {
        entered: ReturnType<typeof Promise.withResolvers<void>>;
        release: ReturnType<typeof Promise.withResolvers<void>>;
    };

    constructor(
        private readonly planVersion: "1" | "2",
        private readonly executionModel = MODEL
    ) {
        super();
    }

    protected override async sendRequest(request: Request): Promise<Response> {
        const path = new URL(request.url).pathname;
        const models = {
            data: [...new Set([MODEL, this.executionModel])].map((id) => ({
                id,
                name: id,
                model_picker_enabled: this.constituentsEnabled,
                policy: { state: this.constituentsEnabled ? "enabled" : "disabled" },
                supported_endpoints: ["/chat/completions"],
                capabilities: {
                    supports: { streaming: true, tool_calls: true, vision: true },
                    limits: {
                        max_prompt_tokens: 128000,
                        max_output_tokens: 16384,
                        max_context_window_tokens: 144384,
                    },
                },
            })),
        };
        if (path === "/models") return Response.json(models);
        if (path === "/meta") {
            return Response.json({
                models,
                auto: {
                    default_tier: "Balance-V2",
                    tiers: [
                        {
                            id: "Balance-V2",
                            display_name: "Balance",
                            description: "",
                            type: "auto",
                            status: { enabled: true },
                        },
                        {
                            id: "Other-V2",
                            display_name: "Ordinary Auto",
                            description: "",
                            type: "auto",
                            status: { enabled: !this.disabled, message: "Contact admin" },
                        },
                        ...["hydrafusion", "hydrafusion-max"].map((id) => ({
                            id,
                            display_name: id,
                            description: "",
                            type: "fusion",
                            status: { enabled: !this.disabled, message: "Contact admin" },
                        })),
                        {
                            id: "future-fusion",
                            display_name: "Future",
                            description: "",
                            type: "fusion",
                            status: { enabled: true },
                        },
                        {
                            id: "future-type",
                            display_name: "Unknown",
                            description: "",
                            type: "ensemble",
                            status: { enabled: true },
                        },
                    ],
                },
            });
        }
        if (path === "/model/fusion") {
            const body = (await request.json()) as Record<string, unknown>;
            this.plans.push(body);
            if (this.failRoute)
                return Response.json({ message: "Route unavailable" }, { status: 500 });
            return Response.json({
                fusion_mode: body.fusion_mode,
                fusion_pattern: "solo",
                plan_version: this.planVersion,
                steps: [{ role: "generation", model_id: this.executionModel }],
                session: { token: "jwt.fake.auto.fusion.plan", expires_at: 0 },
            });
        }
        if (path === "/auto") {
            this.auto.push((await request.json()) as Record<string, unknown>);
            return Response.json({
                selected_model: models.data[0],
                session_token: "jwt.fake.auto.fusion.reset",
                expires_at: Math.floor(Date.now() / 1000) + 3600,
            });
        }
        if (path === "/chat/completions") {
            const body = (await request.json()) as Record<string, unknown>;
            this.inference.push(body);
            const gate = this.inferenceGate;
            if (gate) {
                gate.entered.resolve();
                await gate.release.promise;
            }
            if (body.model !== MODEL && body.model !== this.executionModel)
                throw new Error(`Synthetic model escaped to inference: ${body.model}`);
            const completion = {
                id: "chatcmpl-auto-fusion",
                object: "chat.completion",
                created: 0,
                model: body.model,
                choices: [
                    {
                        index: 0,
                        message: { role: "assistant", content: REPLY },
                        finish_reason: "stop",
                    },
                ],
                usage: { prompt_tokens: 11, completion_tokens: 7, total_tokens: 18 },
            };
            if (!body.stream) return Response.json(completion);
            return new Response(
                `data: ${JSON.stringify({
                    ...completion,
                    object: "chat.completion.chunk",
                    choices: [
                        {
                            index: 0,
                            delta: { role: "assistant", content: REPLY },
                            finish_reason: "stop",
                        },
                    ],
                })}\n\ndata: [DONE]\n\n`,
                { headers: { "content-type": "text/event-stream" } }
            );
        }
        throw new Error(`Unexpected routing request: ${request.method} ${path}`);
    }
}

describe.skipIf(isByokBackend).each([
    { mode: "hydrafusion", version: "1" as const },
    { mode: "hydrafusion", version: "2" as const },
    { mode: "hydrafusion-max", version: "2" as const },
])("Metadata Auto execution: $mode v$version", async ({ mode, version }) => {
    const executionModel = mode === "hydrafusion" ? "claude-sonnet-5" : MODEL;
    const handler = new AutoFusionRequestHandler(version, executionModel);
    const {
        copilotClient: client,
        createClient,
        workDir,
    } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            requestHandler: handler,
            env: {
                DYNAMIC_AUTO_TIERS: "false",
                COPILOT_EXP_COPILOT_CLI_DYNAMIC_AUTO_TIERS: "false",
                HYDRAFUSION: "false",
                HYDRAFUSION_ROLLOUT: "false",
                COPILOT_EXP_COPILOT_CLI_HYDRAFUSION_ROLLOUT: "false",
                HYDRAFUSION_PLAN_V2: String(version === "2"),
                COPILOT_EXP_COPILOT_CLI_HYDRAFUSION_PLAN_V2: String(version === "2"),
            },
        },
    });
    const config = {
        model: "auto",
        workingDirectory: workDir,
        expAssignments: {
            Features: [],
            Flights: {},
            Configs: [{ Id: "default", Parameters: { copilot_cli_dynamic_auto_tiers: true } }],
            AssignmentContext: "metadata-fusion-treatment",
        },
        capi: { enableWebSocketResponses: false },
        onPermissionRequest: approveAll,
    };
    beforeEach(() => {
        handler.plans.length = 0;
        handler.auto.length = 0;
        handler.inference.length = 0;
        handler.failRoute = false;
        handler.disabled = false;
        handler.constituentsEnabled = true;
        handler.inferenceGate = undefined;
    });

    it("routes only the selected preference, commits it, resumes cold and resets to Auto", async () => {
        const diagnosticStart = performance.now();
        const phases: { phase: string; elapsedMs: number }[] = [];
        const markPhase = (phase: string) =>
            phases.push({ phase, elapsedMs: Math.round(performance.now() - diagnosticStart) });
        onTestFailed(() => {
            process.stderr.write(
                `${JSON.stringify({
                    test: "metadata Auto create/resume/reset",
                    mode,
                    version,
                    phases,
                    planCount: handler.plans.length,
                    autoCount: handler.auto.length,
                    inferenceCount: handler.inference.length,
                })}\n`
            );
        });
        markPhase("creating session");
        const session = await client.createSession(config);
        markPhase("listing models");
        const catalog = await session.rpc.model.list();
        expect(catalog.auto?.tiers.some((tier) => tier.id === mode)).toBe(true);
        expect(
            catalog.list.some(
                (model) =>
                    typeof model === "object" &&
                    model !== null &&
                    "id" in model &&
                    typeof model.id === "string" &&
                    model.id.startsWith("hydrafusion")
            )
        ).toBe(false);
        expect(handler.plans).toEqual([]);
        await expect(session.setAutoTier("future-fusion")).rejects.toThrow("unsupported execution");
        await expect(session.setAutoTier("future-type")).rejects.toThrow("unsupported execution");
        await expect(session.setAutoTier("HydraFusion")).rejects.toThrow("unavailable");
        const pending = await session.setAutoTier(mode);
        markPhase("first turn");
        expect(pending.pendingAutoTier).toBe(mode);
        expect((await session.rpc.model.getCurrent()).autoTier).toBeUndefined();
        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));
        expect(
            (
                await session.sendAndWait({
                    prompt: "What is the chemical symbol for gold? Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(handler.plans).toEqual([
            expect.objectContaining({
                fusion_mode: mode,
                ...(version === "2" ? { plan_version: version } : {}),
            }),
        ]);
        if (version === "1") expect(handler.plans[0]).not.toHaveProperty("plan_version");
        expect(handler.auto).toEqual([]);
        expect(events.some((event) => event.type === "session.fusion_route_failed")).toBe(false);
        expect(events).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    type: "session.fusion_completed",
                    data: expect.objectContaining({ syntheticModel: mode }),
                }),
            ])
        );
        expect(events).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    type: "session.fusion_resolved",
                    data: expect.objectContaining({
                        syntheticModel: mode,
                        policy: mode === "hydrafusion" ? "standard" : "max",
                        primaryModel: executionModel,
                    }),
                }),
            ])
        );
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: mode,
        });
        const sessionId = session.sessionId;
        markPhase("cold client shutdown");
        await session.disconnect();
        await client.stop();
        const coldClient = createClient({ requestHandler: handler });
        try {
            markPhase("cold resume");
            const resumed = await coldClient.resumeSession(sessionId, config);
            markPhase("cold structured-output admission");
            expect(await resumed.rpc.model.getCurrent()).toMatchObject({
                modelId: "auto",
                autoTier: mode,
            });
            await expect(
                resumed.sendAndWait({
                    prompt: "Return a structured chemical symbol before the resumed turn.",
                    responseSchema: { type: "object", properties: { symbol: { type: "string" } } },
                })
            ).rejects.toThrow("responseFormat is not supported");
            expect(handler.plans).toHaveLength(1);

            markPhase("resumed turn");
            expect(
                (
                    await resumed.sendAndWait({
                        prompt: "What is the chemical symbol for silver? Do not use tools.",
                    })
                )?.data.content
            ).toBe(REPLY);
            expect(handler.plans).toHaveLength(2);
            markPhase("switch concrete then auto");
            await resumed.setModel(MODEL);
            expect((await resumed.rpc.model.getCurrent()).autoTier).toBe(mode);
            await resumed.setModel("auto");
            expect(
                (
                    await resumed.sendAndWait({
                        prompt: "Name the chemical symbol for iron. Do not use tools.",
                    })
                )?.data.content
            ).toBe(REPLY);
            expect(handler.plans).toHaveLength(3);
            markPhase("switch ordinary auto");
            await resumed.setModel("auto", { autoTier: "Balance-V2" });
            expect((await resumed.rpc.model.getCurrent()).autoTier).toBe(mode);
            expect(
                (
                    await resumed.sendAndWait({
                        prompt: "What is the chemical symbol for copper? Do not use tools.",
                    })
                )?.data.content
            ).toBe(REPLY);
            expect(handler.plans).toHaveLength(3);
            expect(handler.auto.at(-1)).toMatchObject({ tier: "Balance-V2" });
            expect(await resumed.rpc.model.getCurrent()).toMatchObject({
                modelId: "auto",
                autoTier: "Balance-V2",
            });
            await resumed.setAutoTier(null);
            markPhase("reset turn");
            expect(
                (
                    await resumed.sendAndWait({
                        prompt: "Name the chemical symbol for lead. Do not use tools.",
                    })
                )?.data.content
            ).toBe(REPLY);
            expect((await resumed.rpc.model.getCurrent()).autoTier).toBeUndefined();
            expect(handler.plans).toHaveLength(3);
            await resumed.disconnect();
        } finally {
            markPhase("cold client cleanup");
            await coldClient.stop();
        }
        markPhase("complete");
    });

    it("executes explicit create and atomic Auto selection without rewriting the logical model", async () => {
        const created = await client.createSession({
            ...config,
            capi: { ...config.capi, autoTier: mode },
        });
        expect(
            (
                await created.sendAndWait({
                    prompt: "Name the chemical symbol for gold. Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(await created.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: mode,
        });
        await created.disconnect();
        const switched = await client.createSession({ ...config, model: MODEL });
        await switched.setModel("auto", { autoTier: mode });
        expect((await switched.rpc.model.getCurrent()).pendingAutoTier).toBe(mode);
        expect(
            (
                await switched.sendAndWait({
                    prompt: "Name the chemical symbol for silver. Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(await switched.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: mode,
        });
        expect(handler.plans).toHaveLength(2);
        expect(
            handler.plans.every(
                (plan) =>
                    plan.fusion_mode === mode &&
                    plan.plan_version === (version === "2" ? version : undefined)
            )
        ).toBe(true);
        expect(handler.auto).toEqual([]);
        await switched.disconnect();
    });

    it("retains typed Fusion route-failure fallback and commits only the concrete response", async () => {
        handler.failRoute = true;
        const session = await client.createSession(config);
        await session.setAutoTier(mode);
        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));
        expect(
            (
                await session.sendAndWait({
                    prompt: "What is the chemical symbol for gold? Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(events.some((event) => event.type === "session.fusion_route_failed")).toBe(true);
        expect(events.filter((event) => event.type === "assistant.message")).toHaveLength(1);
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: mode,
        });
        expect(handler.plans).toHaveLength(1);
        expect(handler.auto).toEqual([]);
        await session.disconnect();
    });

    it("applies reasoning-effort constraints consistently to dedicated Fusion setters", async () => {
        const session = await client.createSession(config);
        await session.rpc.model.setReasoningEffort({ reasoningEffort: "high" });
        await expect(session.setAutoTier(mode)).rejects.toThrow("do not support reasoning effort");
        expect((await session.rpc.model.getCurrent()).reasoningEffort).toBe("high");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();
        await session.rpc.model.setReasoningEffort({ reasoningEffort: "none" });
        await session.setAutoTier(mode);
        await expect(
            session.rpc.model.setReasoningEffort({ reasoningEffort: "high" })
        ).rejects.toThrow("do not support reasoning effort");
        await session.rpc.model.setReasoningEffort({ reasoningEffort: "none" });
        await session.setAutoTier(null);
        await session.rpc.model.setReasoningEffort({ reasoningEffort: "high" });
        expect((await session.rpc.model.getCurrent()).reasoningEffort).toBe("high");
        await session.disconnect();
    });

    it("retains the staged Fusion preference when the first inference is cancelled", async () => {
        const session = await client.createSession({
            ...config,
            capi: { ...config.capi, autoTier: "Balance-V2" },
        });
        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));
        await session.setAutoTier(mode);
        const gate = {
            entered: Promise.withResolvers<void>(),
            release: Promise.withResolvers<void>(),
        };
        handler.inferenceGate = gate;
        const turn = session
            .sendAndWait({
                prompt: "What is the chemical symbol for gold? Do not use tools.",
            })
            .then(
                (reply) => ({ reply, error: undefined }),
                (error: Error) => ({ reply: undefined, error })
            );
        try {
            await gate.entered.promise;
            expect((await session.rpc.model.getCurrent()).activatingAutoTier).toBe(mode);
            await session.abort();
        } finally {
            gate.release.resolve();
            handler.inferenceGate = undefined;
        }
        await turn;
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: "Balance-V2",
            pendingAutoTier: mode,
        });
        expect((await session.rpc.model.getCurrent()).activatingAutoTier).toBeUndefined();
        expect(events.some((event) => event.type === "session.auto_tier_switch_failed")).toBe(
            false
        );
        expect(
            (
                await session.sendAndWait({
                    prompt: "What is the chemical symbol for silver? Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(handler.plans).toHaveLength(2);
        expect(handler.auto).toEqual([]);
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: mode,
        });
        await session.disconnect();
    });

    it.each(["fusion", "auto"] as const)(
        "settles an invalidated pending %s preference once and preserves the incumbent",
        async (execution) => {
            await client.stop();
            const session = await client.createSession({
                ...config,
                capi: { ...config.capi, autoTier: "Balance-V2" },
            });
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            const requested = execution === "fusion" ? mode : "Other-V2";
            await session.setAutoTier(requested);
            expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe(requested);
            handler.disabled = true;
            await session.rpc.model.list({ skipCache: true });
            await session
                .sendAndWait({
                    prompt: "What is the chemical symbol for gold? Do not use tools.",
                })
                .then(
                    (reply) => expect(reply?.data.content).toBe(REPLY),
                    (error: unknown) => {
                        expect(error).toBeInstanceOf(Error);
                        expect((error as Error).message).toContain("Contact admin");
                    }
                );
            const current = await session.rpc.model.getCurrent();
            expect(current.autoTier).toBe("Balance-V2");
            expect(current.pendingAutoTier).toBeUndefined();
            expect(current.activatingAutoTier).toBeUndefined();
            expect(
                events.filter((event) => event.type === "session.auto_tier_switch_failed")
            ).toEqual([
                expect.objectContaining({
                    data: expect.objectContaining({
                        requestedAutoTier: requested,
                        effectiveAutoTier: "Balance-V2",
                        reason: "unsupported",
                    }),
                }),
            ]);
            expect(
                (
                    await session.sendAndWait({
                        prompt: "What is the chemical symbol for silver? Do not use tools.",
                    })
                )?.data.content
            ).toBe(REPLY);
            expect(handler.plans).toEqual([]);
            expect(handler.auto.every((body) => body.tier === "Balance-V2")).toBe(true);
            expect(
                events.filter((event) => event.type === "session.auto_tier_switch_failed")
            ).toHaveLength(1);
            await session.disconnect();
        }
    );

    it("reports setup failure after claiming Fusion and clears only the failed request", async () => {
        await client.stop();
        const session = await client.createSession({
            ...config,
            capi: { ...config.capi, autoTier: "Balance-V2" },
        });
        const events: SessionEvent[] = [];
        const failedTurnIdle = Promise.withResolvers<void>();
        let failureObserved = false;
        session.on((event) => {
            events.push(event);
            if (event.type === "session.error") failureObserved = true;
            if (event.type === "session.idle" && failureObserved) failedTurnIdle.resolve();
        });
        await session.setAutoTier(mode);
        handler.constituentsEnabled = false;
        await session.rpc.model.list({ skipCache: true });
        await expect(
            session.sendAndWait({
                prompt: "What is the chemical symbol for gold? Do not use tools.",
            })
        ).rejects.toThrow("no executable constituent");
        // sendAndWait rejects on session.error before that failed turn's idle tail settles.
        await failedTurnIdle.promise;
        // Host-forwarded error/idle can overtake this event's recorded-stream delivery.
        await expect
            .poll(() => events.filter((event) => event.type === "session.auto_tier_switch_failed"))
            .toEqual([
                expect.objectContaining({
                    data: expect.objectContaining({
                        requestedAutoTier: mode,
                        effectiveAutoTier: "Balance-V2",
                        reason: "setup_failed",
                    }),
                }),
            ]);
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: "Balance-V2",
        });
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();
        expect((await session.rpc.model.getCurrent()).activatingAutoTier).toBeUndefined();
        expect(handler.plans).toEqual([]);
        expect(handler.inference).toEqual([]);
        handler.constituentsEnabled = true;
        await session.rpc.model.list({ skipCache: true });
        expect(
            (
                await session.sendAndWait({
                    prompt: "What is the chemical symbol for silver? Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(handler.plans).toEqual([]);
        expect(
            events.filter((event) => event.type === "session.auto_tier_switch_failed")
        ).toHaveLength(1);
        await session.disconnect();
    });

    it("rejects unsupported context and structured output before Fusion activation", async () => {
        const session = await client.createSession(config);
        await expect(
            session.rpc.model.switchTo({
                modelId: "auto",
                autoTier: mode,
                contextTier: "long",
            })
        ).rejects.toThrow("do not support context tiers");
        expect((await session.rpc.model.getCurrent()).autoTier).toBeUndefined();
        await session.setAutoTier(mode);
        await expect(
            session.sendAndWait({
                prompt: "Return the chemical symbol for gold as structured JSON.",
                responseSchema: {
                    type: "object",
                    properties: { symbol: { type: "string" } },
                    required: ["symbol"],
                    additionalProperties: false,
                },
            })
        ).rejects.toThrow("responseFormat is not supported");
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            pendingAutoTier: mode,
        });
        expect((await session.rpc.model.getCurrent()).autoTier).toBeUndefined();
        expect(handler.plans).toEqual([]);
        expect(handler.auto).toEqual([]);
        expect(handler.inference).toEqual([]);
        expect((await session.rpc.queue.pendingItems()).items).toEqual([]);
        expect(
            (await session.getEvents()).filter(
                (event) => event.type === "user.message" || event.type === "session.error"
            )
        ).toEqual([]);
        await session.disconnect();
    });
});

describe.skipIf(isByokBackend).each(["1", "2"] as const)(
    "Manual HydraFusion plan-version control: %s",
    async (version) => {
        const handler = new AutoFusionRequestHandler(version);
        const { copilotClient: client, workDir } = await createSdkTestContext({
            useStdio: true,
            copilotClientOptions: {
                gitHubToken: DEFAULT_GITHUB_TOKEN,
                requestHandler: handler,
                env: {
                    DYNAMIC_AUTO_TIERS: "false",
                    COPILOT_EXP_COPILOT_CLI_DYNAMIC_AUTO_TIERS: "false",
                    HYDRAFUSION: "true",
                    HYDRAFUSION_ROLLOUT: "true",
                    HYDRAFUSION_PLAN_V2: String(version === "2"),
                    COPILOT_EXP_COPILOT_CLI_HYDRAFUSION_PLAN_V2: String(version === "2"),
                },
            },
        });
        it("retains the manual v1 Max and v2 Standard execution mapping", async () => {
            const session = await client.createSession({
                model: "hydrafusion",
                enableExperimentalMode: true,
                workingDirectory: workDir,
                capi: { enableWebSocketResponses: false },
                onPermissionRequest: approveAll,
            });
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            expect(
                (
                    await session.sendAndWait({
                        prompt: "What is the chemical symbol for gold? Do not use tools.",
                    })
                )?.data.content
            ).toBe(REPLY);
            expect(handler.plans).toEqual([
                expect.objectContaining({
                    fusion_mode: version === "1" ? "hydrafusion-max" : "hydrafusion",
                    ...(version === "2" ? { plan_version: "2" } : {}),
                }),
            ]);
            expect(events).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({
                        type: "session.fusion_resolved",
                        data: expect.objectContaining({
                            policy: version === "1" ? "max" : "standard",
                        }),
                    }),
                ])
            );
            expect(events.some((event) => event.type === "session.fusion_route_failed")).toBe(
                false
            );
            expect((await session.rpc.model.getCurrent()).modelId).toBe("hydrafusion");
            await session.disconnect();
        });

        if (version === "1") {
            it("routes metadata Standard independently of manual Max history and resumes same-policy reuse", async () => {
                const planStart = handler.plans.length;
                const config = {
                    model: "hydrafusion",
                    enableExperimentalMode: true,
                    workingDirectory: workDir,
                    capi: { enableWebSocketResponses: false },
                    onPermissionRequest: approveAll,
                    expAssignments: {
                        Features: [],
                        Flights: {},
                        Configs: [
                            {
                                Id: "default",
                                Parameters: {
                                    copilot_cli_dynamic_auto_tiers: true,
                                    copilot_cli_hydrafusion_turn_policy: "initial_only",
                                },
                            },
                        ],
                        AssignmentContext: "policy-scoped-fusion-history",
                    },
                };
                const session = await client.createSession(config);
                const events: SessionEvent[] = [];
                session.on((event) => events.push(event));
                expect(
                    (
                        await session.sendAndWait({
                            prompt: "Name the chemical symbol for gold. Do not use tools.",
                        })
                    )?.data.content
                ).toBe(REPLY);
                expect(handler.plans.slice(planStart)).toEqual([
                    expect.objectContaining({ fusion_mode: "hydrafusion-max" }),
                ]);
                await session.setModel("auto", { autoTier: "hydrafusion" });
                expect(
                    (
                        await session.sendAndWait({
                            prompt: "Name the chemical symbol for silver. Do not use tools.",
                        })
                    )?.data.content
                ).toBe(REPLY);
                expect(handler.plans.slice(planStart)).toEqual([
                    expect.objectContaining({ fusion_mode: "hydrafusion-max" }),
                    expect.objectContaining({ fusion_mode: "hydrafusion" }),
                ]);
                expect(
                    events
                        .filter((event) => event.type === "session.fusion_resolved")
                        .map((event) => event.data.policy)
                ).toEqual(["max", "standard"]);
                expect(await session.rpc.model.getCurrent()).toMatchObject({
                    modelId: "auto",
                    autoTier: "hydrafusion",
                });
                const sessionId = session.sessionId;
                await session.disconnect();
                await client.stop();
                const resumed = await client.resumeSession(sessionId, {
                    ...config,
                    model: "auto",
                });
                expect(
                    (
                        await resumed.sendAndWait({
                            prompt: "Name the chemical symbol for copper. Do not use tools.",
                        })
                    )?.data.content
                ).toBe(REPLY);
                expect(handler.plans.slice(planStart)).toHaveLength(2);
                expect(await resumed.rpc.model.getCurrent()).toMatchObject({
                    modelId: "auto",
                    autoTier: "hydrafusion",
                });
                await resumed.disconnect();
            });
        }
    }
);

describe.skipIf(isByokBackend)("Metadata Fusion rejection before activation", async () => {
    const handler = new AutoFusionRequestHandler("1");
    const { copilotClient: client, workDir } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            requestHandler: handler,
            env: {
                DYNAMIC_AUTO_TIERS: "true",
                HYDRAFUSION_PLAN_V2: "false",
                COPILOT_EXP_COPILOT_CLI_HYDRAFUSION_PLAN_V2: "false",
            },
        },
    });

    it("rejects Max with actionable plan-v2 guidance without changing incumbent or calling endpoints", async () => {
        const config = {
            model: "auto",
            workingDirectory: workDir,
            capi: { autoTier: "Balance-V2" },
            onPermissionRequest: approveAll,
        };
        await expect(
            client.createSession({ ...config, capi: { autoTier: "hydrafusion-max" } })
        ).rejects.toThrow("HYDRAFUSION_PLAN_V2");
        const session = await client.createSession(config);
        await expect(session.setAutoTier("hydrafusion-max")).rejects.toThrow("HYDRAFUSION_PLAN_V2");
        await expect(session.setModel("auto", { autoTier: "hydrafusion-max" })).rejects.toThrow(
            "HYDRAFUSION_PLAN_V2"
        );
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: "Balance-V2",
        });
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();
        expect(handler.plans).toEqual([]);
        expect(handler.auto).toEqual([]);
        expect(handler.inference).toEqual([]);
        handler.disabled = true;
        await session.rpc.model.list({ skipCache: true });
        await expect(session.setAutoTier("hydrafusion")).rejects.toThrow("Contact admin");
        await expect(session.setAutoTier("Other-V2")).rejects.toThrow("Contact admin");
        expect((await session.rpc.model.getCurrent()).autoTier).toBe("Balance-V2");
        await session.disconnect();
    });

    it("does not settle pending Auto intent when structured-output admission rejects metadata", async () => {
        await client.stop();
        handler.disabled = false;
        const session = await client.createSession({
            model: "auto",
            workingDirectory: workDir,
            capi: { autoTier: "Balance-V2", enableWebSocketResponses: false },
            onPermissionRequest: approveAll,
        });
        await session.setAutoTier("Other-V2");
        handler.disabled = true;
        await session.rpc.model.list({ skipCache: true });
        const before = await session.getEvents();
        await expect(
            session.sendAndWait({
                prompt: "Return the chemical symbol for gold as structured JSON.",
                responseSchema: {
                    type: "object",
                    properties: { symbol: { type: "string" } },
                },
            })
        ).rejects.toThrow("Contact admin");
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: "Balance-V2",
            pendingAutoTier: "Other-V2",
        });
        expect((await session.rpc.model.getCurrent()).activatingAutoTier).toBeUndefined();
        expect((await session.rpc.queue.pendingItems()).items).toEqual([]);
        expect(await session.getEvents()).toEqual(before);
        expect(handler.plans).toEqual([]);
        expect(handler.inference).toEqual([]);
        handler.disabled = false;
        await session.rpc.model.list({ skipCache: true });
        expect(
            (
                await session.sendAndWait({
                    prompt: "What is the chemical symbol for silver? Do not use tools.",
                })
            )?.data.content
        ).toBe(REPLY);
        expect(handler.auto.at(-1)).toMatchObject({ tier: "Other-V2" });
        expect(await session.rpc.model.getCurrent()).toMatchObject({
            modelId: "auto",
            autoTier: "Other-V2",
        });
        await session.disconnect();
    });
});
