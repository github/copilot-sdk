/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { mkdir, realpath, rm, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { join } from "node:path";
import { text } from "node:stream/consumers";
import { describe, expect, it, onTestFailed, onTestFinished } from "vitest";
import {
    createAttributedPermissionResult,
    type CopilotSession,
    type NamedProviderConfig,
    type PermissionRequest,
    type ProviderModelConfig,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getNextEventOfType } from "./harness/sdkTestHelper.js";

const JUDGE_OUTPUT = "ALLOW: Explicitly authorized bounded filename search.";
const HUMAN_REVIEW_OUTPUT = "DENY: Ask the registered human permission handler.";
const SHELL_TOOL = process.platform === "win32" ? "powershell" : "bash";
const EARLIER_RESTRICTION_MARKER = "ASSISTED_AUTHORIZATION_RESTRICT";
const LATER_AUTHORIZATION_MARKER = "ASSISTED_AUTHORIZATION_ALLOW";
const AUTHORIZATION_FIXTURE = "assisted-authorization-updated";

type ContractRecommendation = "approve" | "requireApproval" | "excluded";
type ContractScope = "root" | "subagent";

function contractMarker(recommendation: ContractRecommendation): string {
    return recommendation === "requireApproval" ? "REQUIRE" : recommendation.toUpperCase();
}

function contractShellCallId(scope: ContractScope, recommendation: ContractRecommendation): string {
    const owner = scope === "subagent" ? "child" : "root";
    return `assisted-contract-${owner}-${recommendation}`;
}

function contractFixtureName(scope: ContractScope, recommendation: ContractRecommendation): string {
    return `assisted-contract-${recommendation}-${scope}`;
}

describe("Assisted permission handling in Autopilot", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext({ useStdio: true });

    it.each(["lifecycle", "authorization", "handler"] as const)(
        "resolves Assisted recommendations before Autopilot permission recovery (%s contract)",
        async (contract) => {
            let agentCalls = 0;
            const judgeOutputs: string[] = [];
            const authorizationJudgeRequests: string[] = [];
            const providerFailures: Error[] = [];
            const started = Date.now();
            let phase = "setup";
            let phaseStarted = started;
            const completedPhases: Array<{ phase: string; elapsedMs: number }> = [];
            const enterPhase = (next: string) => {
                const now = Date.now();
                completedPhases.push({ phase, elapsedMs: now - phaseStarted });
                phase = next;
                phaseStarted = now;
            };
            onTestFailed(() => {
                console.error(
                    "Assisted contract failure boundary:",
                    JSON.stringify({
                        phase,
                        phaseElapsedMs: Date.now() - phaseStarted,
                        elapsedMs: Date.now() - started,
                        completedPhases,
                        agentCalls,
                        judgeCalls: judgeOutputs.length,
                        providerFailures: providerFailures.length,
                    })
                );
            });
            const outsideDir = `${workDir}-outside`;
            await mkdir(outsideDir, { recursive: true });
            await writeFile(join(outsideDir, "approval-probe.txt"), "ASSISTED_READ_PROBE\n");
            onTestFinished(() => rm(outsideDir, { recursive: true, force: true }));
            const modelServer = createServer((request, response) => {
                void (async () => {
                    const body = JSON.parse(await text(request)) as {
                        model: string;
                        stream?: boolean;
                        messages: Array<{ role?: string; content?: unknown }>;
                    };
                    const isJudge = body.model === "gpt-6-luna";
                    const messages = JSON.stringify(body.messages);
                    const latestUserMessage = [...body.messages]
                        .reverse()
                        .find((entry) => entry.role === "user");
                    const latestUserText = JSON.stringify(latestUserMessage?.content);
                    let message:
                        | { role: "assistant"; content: string }
                        | {
                              role: "assistant";
                              content: string;
                              tool_calls: Array<{
                                  id: string;
                                  type: "function";
                                  function: { name: string; arguments: string };
                              }>;
                          };
                    if (isJudge) {
                        const earlierRestrictionIndex = messages.lastIndexOf(
                            EARLIER_RESTRICTION_MARKER
                        );
                        const laterAuthorizationIndex = messages.lastIndexOf(
                            LATER_AUTHORIZATION_MARKER
                        );
                        const authorizationRoute = laterAuthorizationIndex !== -1;
                        if (authorizationRoute) {
                            authorizationJudgeRequests.push(messages);
                        }
                        const contractRequiresApproval =
                            messages.includes(contractFixtureName("root", "requireApproval")) ||
                            messages.includes(contractFixtureName("subagent", "requireApproval"));
                        const output = authorizationRoute
                            ? earlierRestrictionIndex !== -1 &&
                              earlierRestrictionIndex < laterAuthorizationIndex
                                ? JUDGE_OUTPUT
                                : HUMAN_REVIEW_OUTPUT
                            : messages.includes("assisted-human-ask") || contractRequiresApproval
                              ? HUMAN_REVIEW_OUTPUT
                              : JUDGE_OUTPUT;
                        judgeOutputs.push(output);
                        message = { role: "assistant", content: output };
                    } else {
                        agentCalls++;
                        const toolResultCount = body.messages.filter(
                            (entry) => entry.role === "tool"
                        ).length;
                        const contractChildRoute = latestUserText.includes(
                            "ASSISTED_CONTRACT_CHILD_"
                        );
                        const contractSubagentRoute = latestUserText.includes(
                            "ASSISTED_CONTRACT_SUBAGENT_"
                        );
                        const contractRootRoute =
                            latestUserText.includes("ASSISTED_CONTRACT_ROOT_");
                        const authorizationRestrictionRoute = latestUserText.includes(
                            EARLIER_RESTRICTION_MARKER
                        );
                        const authorizationAllowRoute = latestUserText.includes(
                            LATER_AUTHORIZATION_MARKER
                        );
                        if (authorizationRestrictionRoute) {
                            message = { role: "assistant", content: "restriction-recorded" };
                        } else if (authorizationAllowRoute) {
                            message =
                                toolResultCount >= 2
                                    ? { role: "assistant", content: "authorization-updated" }
                                    : toolResultCount === 1
                                      ? {
                                            role: "assistant",
                                            content: "",
                                            tool_calls: [
                                                {
                                                    id: "assisted-authorization-task-complete",
                                                    type: "function",
                                                    function: {
                                                        name: "task_complete",
                                                        arguments: JSON.stringify({
                                                            summary: "authorization-updated",
                                                        }),
                                                    },
                                                },
                                            ],
                                        }
                                      : {
                                            role: "assistant",
                                            content: "",
                                            tool_calls: [
                                                {
                                                    id: "assisted-authorization-shell",
                                                    type: "function",
                                                    function: {
                                                        name: SHELL_TOOL,
                                                        arguments: JSON.stringify({
                                                            command:
                                                                process.platform === "win32"
                                                                    ? `New-Item -ItemType Directory -Path ${AUTHORIZATION_FIXTURE}`
                                                                    : `mkdir ${AUTHORIZATION_FIXTURE}`,
                                                            description:
                                                                "Create the fixture authorized by the latest instruction",
                                                        }),
                                                    },
                                                },
                                            ],
                                        };
                        } else if (
                            contractChildRoute ||
                            contractSubagentRoute ||
                            contractRootRoute
                        ) {
                            const recommendation: ContractRecommendation = latestUserText.includes(
                                "_REQUIRE"
                            )
                                ? "requireApproval"
                                : latestUserText.includes("_EXCLUDED")
                                  ? "excluded"
                                  : "approve";
                            const scope: ContractScope =
                                contractChildRoute || contractSubagentRoute ? "subagent" : "root";
                            const finalText = `contract-${scope}-${
                                recommendation === "approve" ? "approved" : "blocked"
                            }`;
                            if (contractSubagentRoute) {
                                message =
                                    toolResultCount === 0
                                        ? {
                                              role: "assistant",
                                              content: "",
                                              tool_calls: [
                                                  {
                                                      id: `assisted-contract-task-${recommendation}`,
                                                      type: "function",
                                                      function: {
                                                          name: "task",
                                                          arguments: JSON.stringify({
                                                              name: "assisted-contract",
                                                              description:
                                                                  "Exercise an Assisted permission request",
                                                              prompt: `ASSISTED_CONTRACT_CHILD_${contractMarker(
                                                                  recommendation
                                                              )}: Run the requested shell command once and report whether it completed.`,
                                                              agent_type: "task",
                                                              mode: "sync",
                                                          }),
                                                      },
                                                  },
                                              ],
                                          }
                                        : {
                                              role: "assistant",
                                              content: "",
                                              tool_calls: [
                                                  {
                                                      id: `assisted-contract-task-complete-${recommendation}`,
                                                      type: "function",
                                                      function: {
                                                          name: "task_complete",
                                                          arguments: JSON.stringify({
                                                              summary: finalText,
                                                          }),
                                                      },
                                                  },
                                              ],
                                          };
                            } else if (contractChildRoute && toolResultCount > 0) {
                                message = { role: "assistant", content: finalText };
                            } else if (
                                (contractChildRoute || contractRootRoute) &&
                                (toolResultCount === 0 ||
                                    (recommendation === "excluded" && toolResultCount === 1))
                            ) {
                                const command =
                                    recommendation === "excluded"
                                        ? process.platform === "win32"
                                            ? "Get-Content approval-probe.txt | & $runner"
                                            : "printf payload | $runner"
                                        : process.platform === "win32"
                                          ? `New-Item -ItemType Directory -Path ${contractFixtureName(
                                                scope,
                                                recommendation
                                            )}`
                                          : `mkdir ${contractFixtureName(scope, recommendation)}`;
                                message = {
                                    role: "assistant",
                                    content: "",
                                    tool_calls: [
                                        {
                                            id:
                                                toolResultCount === 0
                                                    ? contractShellCallId(scope, recommendation)
                                                    : `${contractShellCallId(
                                                          scope,
                                                          recommendation
                                                      )}-retry`,
                                            type: "function",
                                            function: {
                                                name: SHELL_TOOL,
                                                arguments: JSON.stringify({
                                                    command,
                                                    description:
                                                        "Exercise Assisted permission routing",
                                                }),
                                            },
                                        },
                                    ],
                                };
                            } else {
                                message = {
                                    role: "assistant",
                                    content: "",
                                    tool_calls: [
                                        {
                                            id: `assisted-contract-task-complete-${recommendation}`,
                                            type: "function",
                                            function: {
                                                name: "task_complete",
                                                arguments: JSON.stringify({ summary: finalText }),
                                            },
                                        },
                                    ],
                                };
                            }
                        } else {
                            const primeIndex = messages.lastIndexOf("ASSISTED_SESSION_PRIME");
                            const shellIndex = messages.lastIndexOf("ASSISTED_SHELL_");
                            const pathIndex = messages.lastIndexOf("ASSISTED_PATH_ROUTE");
                            const humanIndex = messages.lastIndexOf("ASSISTED_HUMAN_");
                            const primeRoute =
                                primeIndex > Math.max(shellIndex, pathIndex, humanIndex);
                            const humanRoute =
                                humanIndex > Math.max(primeIndex, shellIndex, pathIndex);
                            const shellRoute =
                                shellIndex > Math.max(primeIndex, pathIndex, humanIndex);
                            const resumedShellRoute =
                                messages.lastIndexOf("ASSISTED_SHELL_RESUME_ROUTE") === shellIndex;
                            const humanApproved = messages.includes("ASSISTED_HUMAN_APPROVE_");
                            const humanLifecycle =
                                messages.includes("ASSISTED_HUMAN_APPROVE_RESUME_ROUTE") ||
                                messages.includes("ASSISTED_HUMAN_DENY_RESUME_ROUTE")
                                    ? "resume"
                                    : "create";
                            const finalText = humanRoute
                                ? humanApproved
                                    ? "human-approved"
                                    : "human-denied"
                                : shellRoute
                                  ? "shell-approved"
                                  : "approval-probe.txt";
                            message = primeRoute
                                ? { role: "assistant", content: "prime-ready" }
                                : toolResultCount >= 2
                                  ? {
                                        role: "assistant",
                                        content: finalText,
                                    }
                                  : toolResultCount === 1
                                    ? {
                                          role: "assistant",
                                          content: "",
                                          tool_calls: [
                                              {
                                                  id: "assisted-autopilot-task-complete",
                                                  type: "function",
                                                  function: {
                                                      name: "task_complete",
                                                      arguments: JSON.stringify({
                                                          summary: finalText,
                                                      }),
                                                  },
                                              },
                                          ],
                                      }
                                    : {
                                          role: "assistant",
                                          content: "",
                                          tool_calls: [
                                              {
                                                  id: shellRoute
                                                      ? "assisted-autopilot-shell"
                                                      : humanRoute
                                                        ? "assisted-autopilot-human"
                                                        : "assisted-autopilot-glob",
                                                  type: "function",
                                                  function:
                                                      shellRoute || humanRoute
                                                          ? {
                                                                name: SHELL_TOOL,
                                                                arguments: JSON.stringify({
                                                                    command: humanRoute
                                                                        ? process.platform ===
                                                                          "win32"
                                                                            ? `New-Item -ItemType Directory -Path assisted-human-ask-${humanApproved ? "approve" : "deny"}-${humanLifecycle}`
                                                                            : `mkdir assisted-human-ask-${humanApproved ? "approve" : "deny"}-${humanLifecycle}`
                                                                        : process.platform ===
                                                                            "win32"
                                                                          ? `New-Item -ItemType Directory -Path assisted-shell-${resumedShellRoute ? "resume" : "create"}`
                                                                          : `mkdir assisted-shell-${resumedShellRoute ? "resume" : "create"}`,
                                                                    description: humanRoute
                                                                        ? "Create the human-reviewed SDK permission fixture"
                                                                        : "Create the authorized SDK permission fixture",
                                                                }),
                                                            }
                                                          : {
                                                                name: "glob",
                                                                arguments: JSON.stringify({
                                                                    pattern: "*.txt",
                                                                    path: outsideDir,
                                                                }),
                                                            },
                                              },
                                          ],
                                      };
                        }
                    }

                    const choice = {
                        index: 0,
                        message,
                        finish_reason: "tool_calls" in message ? "tool_calls" : "stop",
                    };
                    const completion = {
                        id: `assisted-autopilot-${judgeOutputs.length + agentCalls}`,
                        object: "chat.completion",
                        created: 1,
                        model: body.model,
                        choices: [choice],
                        usage: { prompt_tokens: 100, completion_tokens: 20, total_tokens: 120 },
                    };
                    if (body.stream) {
                        response.writeHead(200, { "content-type": "text/event-stream" });
                        response.end(
                            `data: ${JSON.stringify({
                                ...completion,
                                object: "chat.completion.chunk",
                                choices: [
                                    {
                                        index: 0,
                                        delta: {
                                            ...message,
                                            ...("tool_calls" in message
                                                ? {
                                                      tool_calls: message.tool_calls.map(
                                                          (call, index) => ({ index, ...call })
                                                      ),
                                                  }
                                                : {}),
                                        },
                                        finish_reason: choice.finish_reason,
                                    },
                                ],
                            })}\n\ndata: [DONE]\n\n`
                        );
                    } else {
                        response.writeHead(200, { "content-type": "application/json" });
                        response.end(JSON.stringify(completion));
                    }
                })().catch((error: unknown) => {
                    providerFailures.push(
                        error instanceof Error ? error : new Error(String(error))
                    );
                    response.writeHead(500).end();
                });
            });
            await new Promise<void>((resolve, reject) => {
                modelServer.once("error", reject);
                modelServer.listen(0, "127.0.0.1", resolve);
            });
            onTestFinished(async () => {
                modelServer.closeAllConnections();
                if (modelServer.listening) {
                    await new Promise<void>((resolve, reject) => {
                        modelServer.close((error) => (error ? reject(error) : resolve()));
                    });
                }
            });
            const address = modelServer.address();
            if (!address || typeof address === "string") {
                throw new Error("Missing local model server address");
            }

            execFileSync("git", ["init", "--quiet"], { cwd: workDir });
            await writeFile(join(workDir, "approval-probe.txt"), "ASSISTED_READ_PROBE\n");

            const providers: NamedProviderConfig[] = [
                {
                    name: "local",
                    type: "openai",
                    baseUrl: `http://127.0.0.1:${address.port}/v1`,
                    apiKey: "synthetic-test-token",
                    wireApi: "completions",
                },
            ];
            const models: ProviderModelConfig[] = ["gpt-5.6-sol", "gpt-6-luna"].map((id) => ({
                id,
                provider: "local",
                modelId: "gpt-4o",
                wireModel: id,
            }));
            const scenarios = [
                { route: "shell", lifecycle: "create", decision: "judge" },
                { route: "path", lifecycle: "create", decision: "judge" },
                { route: "shell", lifecycle: "resume", decision: "judge" },
                { route: "path", lifecycle: "resume", decision: "judge" },
                { route: "human", lifecycle: "create", decision: "approve" },
                { route: "human", lifecycle: "create", decision: "deny" },
                { route: "human", lifecycle: "resume", decision: "approve" },
                { route: "human", lifecycle: "resume", decision: "deny" },
            ] as const;
            for (const scenario of contract === "lifecycle" ? scenarios : []) {
                const label = `initial:${scenario.route}:${scenario.lifecycle}:${scenario.decision}`;
                let permissionCallbacks = 0;
                const expectedRecommendation =
                    scenario.decision === "judge" ? "approve" : "requireApproval";
                const recommendationByToolCallId = new Map<string, string | undefined>();
                const recommendationWaiters = new Map<
                    string,
                    (recommendation: string | undefined) => void
                >();
                const recommendations: string[] = [];
                const recoveryStatuses: string[] = [];
                const toolResults: Array<{ success?: boolean; result?: { content?: string } }> = [];
                const decisionSources: string[] = [];
                const taskOutcomes: Array<{
                    success?: boolean;
                    summary?: string;
                    outcome?: string;
                    reason?: string;
                }> = [];
                const sessionConfig = {
                    model: "local/gpt-5.6-sol",
                    providers,
                    models,
                    availableTools: [SHELL_TOOL, "glob", "task_complete"],
                    streaming: false,
                    skipCustomInstructions: true,
                    featureFlags: {
                        AUTO_APPROVAL: true,
                        ASSISTED_PERMISSIONS_V2: true,
                    },
                    onPermissionRequest: async (request: PermissionRequest) => {
                        permissionCallbacks++;
                        if (!request.toolCallId) {
                            throw new Error(
                                "Expected the permission request to identify its tool call"
                            );
                        }
                        const recommendation = recommendationByToolCallId.has(request.toolCallId)
                            ? recommendationByToolCallId.get(request.toolCallId)
                            : await new Promise<string | undefined>((resolve) => {
                                  recommendationWaiters.set(request.toolCallId!, resolve);
                              });
                        recommendationByToolCallId.delete(request.toolCallId);
                        if (recommendation !== expectedRecommendation) {
                            throw new Error(
                                `Expected Assisted recommendation ${expectedRecommendation}, got ${String(
                                    recommendation
                                )}`
                            );
                        }
                        if (scenario.decision === "judge") {
                            return createAttributedPermissionResult(
                                { kind: "approve-once" },
                                {
                                    outcome: "auto_approved",
                                    source: "assisted_approval",
                                    surface: "sdk",
                                    responseCapability: "headless",
                                }
                            );
                        }
                        return createAttributedPermissionResult(
                            { kind: scenario.decision === "approve" ? "approve-once" : "reject" },
                            {
                                outcome: "prompted_user",
                                source: "human_response",
                                surface: "sdk",
                                responseCapability: "interactive",
                            }
                        );
                    },
                } as const;
                let session: CopilotSession | undefined;
                try {
                    enterPhase(`${label}:create`);
                    session = await client.createSession(sessionConfig);
                    if (scenario.lifecycle === "resume") {
                        const sessionId = session.sessionId;
                        enterPhase(`${label}:prime`);
                        await session.sendAndWait({
                            prompt: "ASSISTED_SESSION_PRIME: Reply with prime-ready without tools.",
                        });
                        enterPhase(`${label}:configure-before-resume`);
                        await configureAssistedAutopilot(session, workDir);
                        await expectAssistedAutopilotConfigured(session, workDir);
                        enterPhase(`${label}:disconnect-before-resume`);
                        await session.disconnect();
                        session = undefined;
                        enterPhase(`${label}:resume`);
                        session = await client.resumeSession(sessionId, sessionConfig);
                        enterPhase(`${label}:verify-resume`);
                        await expectAssistedAutopilotConfigured(session, workDir);
                    } else {
                        enterPhase(`${label}:configure`);
                        await configureAssistedAutopilot(session, workDir);
                    }
                    session.on((event) => {
                        if (event.type === "permission.requested") {
                            const promptRequest = (
                                event.data as {
                                    promptRequest?: {
                                        assistedApproval?: { recommendation?: string };
                                        autoApproval?: { recommendation?: string };
                                    };
                                }
                            ).promptRequest;
                            const recommendation =
                                promptRequest?.assistedApproval?.recommendation ??
                                promptRequest?.autoApproval?.recommendation;
                            const toolCallId = (
                                event.data as {
                                    permissionRequest?: { toolCallId?: string };
                                }
                            ).permissionRequest?.toolCallId;
                            if (toolCallId) {
                                const waiter = recommendationWaiters.get(toolCallId);
                                if (waiter) {
                                    recommendationWaiters.delete(toolCallId);
                                    waiter(recommendation);
                                } else {
                                    recommendationByToolCallId.set(toolCallId, recommendation);
                                }
                            }
                            if (recommendation) recommendations.push(recommendation);
                        } else if (event.type === "permission.completed") {
                            const source = (event.data as { decisionSource?: string })
                                .decisionSource;
                            if (source) decisionSources.push(source);
                        } else if (event.type === "session.permission_recovery") {
                            recoveryStatuses.push((event.data as { status: string }).status);
                        } else if (event.type === "tool.execution_complete") {
                            toolResults.push(event.data);
                        } else if (event.type === "session.task_complete") {
                            taskOutcomes.push(event.data);
                        }
                    });

                    try {
                        const taskComplete = getNextEventOfType(session, "session.task_complete");
                        enterPhase(`${label}:send`);
                        await session.send({
                            prompt:
                                scenario.route === "shell"
                                    ? `ASSISTED_SHELL_${scenario.lifecycle.toUpperCase()}_ROUTE: Create the authorized fixture directory once and report shell-approved.`
                                    : scenario.route === "path"
                                      ? `ASSISTED_PATH_ROUTE: Use only glob to find *.txt in ${outsideDir} and report the matching filename.`
                                      : `ASSISTED_HUMAN_${scenario.decision.toUpperCase()}_${scenario.lifecycle.toUpperCase()}_ROUTE: Try to create the human-reviewed fixture directory once and report human-${scenario.decision === "approve" ? "approved" : "denied"}.`,
                        });
                        enterPhase(`${label}:task-complete`);
                        await taskComplete;
                    } catch (error) {
                        throw new Error(`${JSON.stringify(scenario)} failed`, { cause: error });
                    }

                    enterPhase(`${label}:assert`);
                    const expectedCompletions =
                        scenario.route === "path" && scenario.lifecycle === "create" ? 2 : 1;
                    expect(permissionCallbacks, JSON.stringify(scenario)).toBe(expectedCompletions);
                    expect(recommendations, JSON.stringify(scenario)).toEqual(
                        Array(expectedCompletions).fill(expectedRecommendation)
                    );
                    expect(decisionSources, JSON.stringify(scenario)).toEqual(
                        Array(expectedCompletions).fill(
                            scenario.decision === "judge" ? "assisted_approval" : "human_response"
                        )
                    );
                    expect(recoveryStatuses, JSON.stringify(scenario)).toEqual([]);
                    expect(toolResults, JSON.stringify(scenario)).toHaveLength(2);
                    if (scenario.decision === "deny") {
                        expect(
                            toolResults.some((result) => result.success === false),
                            JSON.stringify(scenario)
                        ).toBe(true);
                        expect(toolResults.at(-1)?.success, JSON.stringify(scenario)).toBe(true);
                    } else {
                        expect(
                            toolResults.every((result) => result.success === true),
                            JSON.stringify(scenario)
                        ).toBe(true);
                    }
                    expect(taskOutcomes, JSON.stringify(scenario)).toHaveLength(1);
                    expect(taskOutcomes[0], JSON.stringify(scenario)).toMatchObject({
                        success: true,
                    });
                    expect(taskOutcomes[0]?.summary, JSON.stringify(scenario)).toContain(
                        scenario.route === "shell"
                            ? "shell-approved"
                            : scenario.route === "path"
                              ? "approval-probe.txt"
                              : `human-${scenario.decision === "approve" ? "approved" : "denied"}`
                    );
                    if (scenario.route === "human") {
                        expect(
                            existsSync(
                                join(
                                    workDir,
                                    `assisted-human-ask-${scenario.decision}-${scenario.lifecycle}`
                                )
                            ),
                            JSON.stringify(scenario)
                        ).toBe(scenario.decision === "approve");
                    }
                } finally {
                    if (session) {
                        enterPhase(`${label}:cleanup`);
                        await session.abort();
                        await session.disconnect();
                    }
                }
            }

            if (contract === "lifecycle") {
                expect(providerFailures).toEqual([]);
                expect(judgeOutputs).toEqual([
                    ...Array(4).fill(JUDGE_OUTPUT),
                    ...Array(4).fill(HUMAN_REVIEW_OUTPUT),
                ]);
                expect(agentCalls).toBe(scenarios.length * 2 + 4);
            }

            if (contract === "authorization") {
                const authorizationJudgeStart = judgeOutputs.length;
                let authorizationPermissionCallbacks = 0;
                const authorizationRecommendations: string[] = [];
                const authorizationDecisionSources: string[] = [];
                const authorizationRecoveryStatuses: string[] = [];
                const authorizationToolResults: Array<{ toolCallId?: string; success?: boolean }> =
                    [];
                const authorizationTaskOutcomes: Array<{ success?: boolean; summary?: string }> =
                    [];
                let resolveAuthorizationRecommendation: (
                    recommendation: string | undefined
                ) => void;
                const authorizationRecommendation = new Promise<string | undefined>((resolve) => {
                    resolveAuthorizationRecommendation = resolve;
                });
                let authorizationSession: CopilotSession | undefined;
                try {
                    enterPhase("authorization:create");
                    authorizationSession = await client.createSession({
                        model: "local/gpt-5.6-sol",
                        providers,
                        models,
                        availableTools: [SHELL_TOOL, "task_complete"],
                        streaming: false,
                        skipCustomInstructions: true,
                        featureFlags: {
                            AUTO_APPROVAL: true,
                            ASSISTED_PERMISSIONS_V2: true,
                        },
                        onPermissionRequest: async (request) => {
                            authorizationPermissionCallbacks++;
                            if (request.toolCallId !== "assisted-authorization-shell") {
                                throw new Error(
                                    `Expected authorization shell permission, got ${String(
                                        request.toolCallId
                                    )}`
                                );
                            }
                            const recommendation = await authorizationRecommendation;
                            if (recommendation !== "approve") {
                                throw new Error(
                                    `Expected later authorization to reach the judge as approve, got ${String(
                                        recommendation
                                    )}`
                                );
                            }
                            return createAttributedPermissionResult(
                                { kind: "approve-once" },
                                {
                                    outcome: "auto_approved",
                                    source: "assisted_approval",
                                    surface: "sdk",
                                    responseCapability: "headless",
                                }
                            );
                        },
                    });
                    enterPhase("authorization:prime");
                    await authorizationSession.sendAndWait({
                        prompt: `${EARLIER_RESTRICTION_MARKER}: Do not create the authorization fixture. Reply restriction-recorded without tools.`,
                    });
                    enterPhase("authorization:configure");
                    await configureAssistedAutopilot(authorizationSession, workDir);
                    authorizationSession.on((event) => {
                        if (event.type === "permission.requested") {
                            const recommendation = (
                                event.data as {
                                    promptRequest?: {
                                        assistedApproval?: { recommendation?: string };
                                    };
                                }
                            ).promptRequest?.assistedApproval?.recommendation;
                            if (recommendation) {
                                authorizationRecommendations.push(recommendation);
                            }
                            resolveAuthorizationRecommendation(recommendation);
                        } else if (event.type === "permission.completed") {
                            const source = (event.data as { decisionSource?: string })
                                .decisionSource;
                            if (source) authorizationDecisionSources.push(source);
                        } else if (event.type === "session.permission_recovery") {
                            authorizationRecoveryStatuses.push(
                                (event.data as { status: string }).status
                            );
                        } else if (event.type === "tool.execution_complete") {
                            authorizationToolResults.push({
                                toolCallId: event.data.toolCallId,
                                success: event.data.success,
                            });
                        } else if (event.type === "session.task_complete") {
                            authorizationTaskOutcomes.push(event.data);
                        }
                    });

                    const taskComplete = getNextEventOfType(
                        authorizationSession,
                        "session.task_complete"
                    );
                    enterPhase("authorization:send");
                    await authorizationSession.send({
                        prompt: `${LATER_AUTHORIZATION_MARKER}: The earlier restriction is superseded. Create the authorization fixture now and report authorization-updated.`,
                    });
                    enterPhase("authorization:task-complete");
                    await taskComplete;

                    enterPhase("authorization:assert");
                    expect(judgeOutputs.slice(authorizationJudgeStart)).toEqual([JUDGE_OUTPUT]);
                    expect(authorizationJudgeRequests).toHaveLength(1);
                    const authorizationRequest = authorizationJudgeRequests[0] ?? "";
                    expect(
                        authorizationRequest.indexOf(EARLIER_RESTRICTION_MARKER)
                    ).toBeGreaterThanOrEqual(0);
                    expect(
                        authorizationRequest.indexOf(LATER_AUTHORIZATION_MARKER)
                    ).toBeGreaterThan(authorizationRequest.indexOf(EARLIER_RESTRICTION_MARKER));
                    expect(authorizationPermissionCallbacks).toBe(1);
                    expect(authorizationRecommendations).toEqual(["approve"]);
                    expect(authorizationDecisionSources).toEqual(["assisted_approval"]);
                    expect(authorizationRecoveryStatuses).toEqual([]);
                    expect(authorizationToolResults).toContainEqual({
                        toolCallId: "assisted-authorization-shell",
                        success: true,
                    });
                    expect(authorizationTaskOutcomes).toEqual([
                        expect.objectContaining({
                            success: true,
                            summary: "authorization-updated",
                        }),
                    ]);
                    expect(existsSync(join(workDir, AUTHORIZATION_FIXTURE))).toBe(true);
                } finally {
                    if (authorizationSession) {
                        enterPhase("authorization:cleanup");
                        await authorizationSession.abort();
                        await authorizationSession.disconnect();
                    }
                }
            }

            const contractScenarios = [
                { scope: "root", recommendation: "approve", handler: "registered" },
                { scope: "root", recommendation: "requireApproval", handler: "registered" },
                { scope: "root", recommendation: "excluded", handler: "registered" },
                { scope: "subagent", recommendation: "approve", handler: "registered" },
                { scope: "subagent", recommendation: "requireApproval", handler: "registered" },
                { scope: "root", recommendation: "requireApproval", handler: "none" },
            ] as const satisfies readonly {
                scope: ContractScope;
                recommendation: ContractRecommendation;
                handler: "registered" | "none";
            }[];
            const contractJudgeStart = judgeOutputs.length;
            for (const scenario of contract === "handler" ? contractScenarios : []) {
                const label = `contract:${scenario.scope}:${scenario.recommendation}:${scenario.handler}`;
                let permissionCallbacks = 0;
                const recommendations: string[] = [];
                const recoveryStatuses: string[] = [];
                const decisionSources: string[] = [];
                const sequence: string[] = [];
                const subagentIds = new Set<string>();
                const permissionAgentIds: Array<string | undefined> = [];
                const toolResults: Array<{
                    agentId?: string;
                    toolCallId?: string;
                    success?: boolean;
                    error?: { message?: string };
                    result?: unknown;
                }> = [];
                const taskOutcomes: Array<{ success?: boolean; summary?: string }> = [];
                const onPermissionRequest = () => {
                    permissionCallbacks++;
                    sequence.push("host");
                    if (scenario.recommendation === "approve") {
                        return createAttributedPermissionResult(
                            { kind: "approve-once" },
                            {
                                outcome: "auto_approved",
                                source: "assisted_approval",
                                surface: "sdk",
                                responseCapability: "headless",
                            }
                        );
                    }
                    if (scenario.recommendation === "requireApproval") {
                        return createAttributedPermissionResult(
                            {
                                kind: "reject",
                                feedback: HUMAN_REVIEW_OUTPUT.replace(/^DENY:\s*/, ""),
                            },
                            {
                                outcome: "autopilot_denied",
                                source: "assisted_approval",
                                surface: "sdk",
                                responseCapability: "headless",
                            }
                        );
                    }
                    if (scenario.recommendation === "excluded") {
                        return createAttributedPermissionResult(
                            { kind: "user-not-available" },
                            {
                                outcome: "autopilot_denied",
                                source: "unattended_fallback",
                                surface: "sdk",
                                responseCapability: "headless",
                            }
                        );
                    }
                    throw new Error("Unexpected Assisted permission recommendation");
                };
                const sessionConfig = {
                    model: "local/gpt-5.6-sol",
                    providers,
                    models,
                    availableTools: [SHELL_TOOL, "task", "task_complete"],
                    streaming: false,
                    skipCustomInstructions: true,
                    featureFlags: {
                        AUTO_APPROVAL: true,
                        ASSISTED_PERMISSIONS_V2: true,
                    },
                    ...(scenario.handler === "registered" ? { onPermissionRequest } : {}),
                } as const;
                let session: CopilotSession | undefined;
                try {
                    enterPhase(`${label}:create`);
                    session = await client.createSession(sessionConfig);
                    enterPhase(`${label}:configure`);
                    await configureAssistedAutopilot(session, workDir);
                    session.on((event) => {
                        if (event.type === "permission.requested") {
                            const promptRequest = (
                                event.data as {
                                    promptRequest?: {
                                        assistedApproval?: { recommendation?: string };
                                        autoApproval?: { recommendation?: string };
                                    };
                                }
                            ).promptRequest;
                            const recommendation =
                                promptRequest?.assistedApproval?.recommendation ??
                                promptRequest?.autoApproval?.recommendation;
                            if (recommendation) {
                                recommendations.push(recommendation);
                                sequence.push(`permission:${recommendation}`);
                            }
                            permissionAgentIds.push(event.agentId);
                        } else if (event.type === "permission.completed") {
                            const source = (event.data as { decisionSource?: string })
                                .decisionSource;
                            if (source) decisionSources.push(source);
                        } else if (event.type === "session.permission_recovery") {
                            const status = (event.data as { status: string }).status;
                            recoveryStatuses.push(status);
                            sequence.push(`recovery:${status}`);
                        } else if (event.type === "tool.execution_complete") {
                            toolResults.push({
                                agentId: event.agentId,
                                toolCallId: event.data.toolCallId,
                                success: event.data.success,
                                error: event.data.error,
                                result: event.data.result,
                            });
                        } else if (event.type === "session.task_complete") {
                            taskOutcomes.push(event.data);
                        } else if (event.type === "subagent.started") {
                            subagentIds.add(event.agentId);
                        }
                    });

                    const marker = contractMarker(scenario.recommendation);
                    const prompt =
                        scenario.scope === "root"
                            ? `ASSISTED_CONTRACT_ROOT_${marker}: Run the requested shell command once and report the outcome.`
                            : `ASSISTED_CONTRACT_SUBAGENT_${marker}: Use the task tool once so a task subagent runs the requested shell command.`;
                    try {
                        const taskComplete = getNextEventOfType(session, "session.task_complete");
                        enterPhase(`${label}:send`);
                        await session.send({ prompt });
                        enterPhase(`${label}:task-complete`);
                        await taskComplete;
                    } catch (error) {
                        throw new Error(`${JSON.stringify(scenario)} failed`, { cause: error });
                    }

                    enterPhase(`${label}:assert`);
                    if (scenario.handler === "none") {
                        expect(permissionCallbacks, JSON.stringify(scenario)).toBe(0);
                        expect(recommendations, JSON.stringify(scenario)).toEqual([]);
                    } else if (scenario.recommendation === "approve") {
                        expect(permissionCallbacks, JSON.stringify(scenario)).toBe(1);
                        expect(recommendations, JSON.stringify(scenario)).toEqual(["approve"]);
                    } else if (scenario.recommendation === "requireApproval") {
                        expect(permissionCallbacks, JSON.stringify(scenario)).toBe(1);
                        expect(recommendations, JSON.stringify(scenario)).toEqual([
                            "requireApproval",
                        ]);
                    } else {
                        expect(
                            permissionCallbacks,
                            JSON.stringify(scenario)
                        ).toBeGreaterThanOrEqual(1);
                        expect(recommendations, JSON.stringify(scenario)).toEqual(
                            Array(permissionCallbacks).fill("excluded")
                        );
                    }
                    if (scenario.handler === "none") {
                        expect(decisionSources, JSON.stringify(scenario)).toEqual([]);
                        expect(recoveryStatuses, JSON.stringify(scenario)).toEqual(["recovering"]);
                    } else {
                        expect(decisionSources, JSON.stringify(scenario)).toContain(
                            scenario.recommendation === "excluded"
                                ? "unattended_fallback"
                                : "assisted_approval"
                        );
                    }
                    if (scenario.recommendation === "excluded") {
                        expect(recoveryStatuses.at(-1), JSON.stringify(scenario)).toBe("blocked");
                    } else if (scenario.handler === "registered") {
                        expect(recoveryStatuses, JSON.stringify(scenario)).toEqual([]);
                    }
                    const hostIndex = sequence.indexOf("host");
                    const firstRecoveryIndex = sequence.findIndex((entry) =>
                        entry.startsWith("recovery:")
                    );
                    if (firstRecoveryIndex !== -1 && scenario.handler === "registered") {
                        expect(
                            hostIndex,
                            JSON.stringify({ scenario, sequence })
                        ).toBeGreaterThanOrEqual(0);
                        expect(hostIndex, JSON.stringify({ scenario, sequence })).toBeLessThan(
                            firstRecoveryIndex
                        );
                    }

                    const shellResult = toolResults.find(
                        (result) =>
                            result.toolCallId ===
                            contractShellCallId(scenario.scope, scenario.recommendation)
                    );
                    expect(shellResult, JSON.stringify({ scenario, toolResults })).toBeDefined();
                    expect(shellResult?.success, JSON.stringify(scenario)).toBe(
                        scenario.recommendation === "approve" && scenario.handler === "registered"
                    );
                    if (
                        scenario.recommendation === "requireApproval" &&
                        scenario.handler === "registered"
                    ) {
                        expect(shellResult?.error?.message, JSON.stringify(scenario)).toContain(
                            "Ask the registered human permission handler."
                        );
                    } else if (scenario.handler === "none") {
                        expect(shellResult?.error?.message, JSON.stringify(scenario)).toContain(
                            "Permission could not be granted automatically."
                        );
                    }
                    if (scenario.scope === "subagent") {
                        expect(subagentIds.size, JSON.stringify(scenario)).toBe(1);
                        expect(
                            subagentIds.has(shellResult?.agentId ?? ""),
                            JSON.stringify(scenario)
                        ).toBe(true);
                        for (const agentId of permissionAgentIds) {
                            expect(subagentIds.has(agentId ?? ""), JSON.stringify(scenario)).toBe(
                                true
                            );
                        }
                    }

                    expect(
                        existsSync(
                            join(
                                workDir,
                                contractFixtureName(scenario.scope, scenario.recommendation)
                            )
                        ),
                        JSON.stringify(scenario)
                    ).toBe(
                        scenario.recommendation === "approve" && scenario.handler === "registered"
                    );
                    expect(
                        (await session.rpc.permissions.pendingRequests()).items,
                        JSON.stringify(scenario)
                    ).toEqual([]);
                    expect(taskOutcomes, JSON.stringify(scenario)).toHaveLength(1);
                    expect(taskOutcomes[0]?.success, JSON.stringify(scenario)).toBe(
                        scenario.recommendation !== "excluded" && scenario.handler === "registered"
                    );
                    if (scenario.handler === "none") {
                        expect(taskOutcomes[0], JSON.stringify(scenario)).toMatchObject({
                            outcome: "continue",
                            reason: "Autopilot is still recovering from a required permission.",
                        });
                    }
                } finally {
                    if (session) {
                        enterPhase(`${label}:cleanup`);
                        await session.abort();
                        await session.disconnect();
                    }
                }
            }

            enterPhase("final-assertions");
            expect(providerFailures).toEqual([]);
            if (contract === "handler") {
                expect(judgeOutputs.slice(contractJudgeStart)).toEqual([
                    JUDGE_OUTPUT,
                    HUMAN_REVIEW_OUTPUT,
                    JUDGE_OUTPUT,
                    HUMAN_REVIEW_OUTPUT,
                    HUMAN_REVIEW_OUTPUT,
                ]);
            }
        }
    );
});

async function configureAssistedAutopilot(session: CopilotSession, workDir: string): Promise<void> {
    await session.rpc.permissions.folderTrust.addTrusted({ path: workDir });
    await session.rpc.permissions.configure({
        approveAllToolPermissionRequests: false,
        approveAllReadPermissionRequests: false,
        rules: { approved: [], denied: [] },
        paths: {
            workspacePath: workDir,
            additionalDirectories: [],
            unrestricted: false,
            includeTempDirectory: false,
        },
    });
    await session.rpc.permissions.setMode({
        mode: "assisted",
        assistedApprovalModel: "local/gpt-6-luna",
        source: "rpc",
    });
    await session.rpc.mode.set({ mode: "autopilot" });
}

async function expectAssistedAutopilotConfigured(
    session: CopilotSession,
    workDir: string
): Promise<void> {
    expect(await session.rpc.mode.get()).toBe("autopilot");
    expect(await session.rpc.permissions.getMode()).toMatchObject({ mode: "assisted" });
    expect((await session.rpc.permissions.folderTrust.isTrusted({ path: workDir })).trusted).toBe(
        true
    );
    expect((await session.rpc.permissions.paths.list()).primary).toBe(await realpath(workDir));
}
