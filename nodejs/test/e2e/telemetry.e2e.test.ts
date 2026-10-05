/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile, writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

interface TelemetryEntry {
    type?: string;
    traceId?: string;
    spanId?: string;
    parentSpanId?: string;
    instrumentationScope?: { name?: string };
    attributes?: Record<string, unknown>;
    status?: { code?: number };
}

function getStringAttribute(entry: TelemetryEntry, name: string): string | undefined {
    const value = entry.attributes?.[name];
    if (value === undefined || value === null) {
        return undefined;
    }
    return typeof value === "string" ? value : JSON.stringify(value);
}

function isRootSpan(entry: TelemetryEntry): boolean {
    const parent = entry.parentSpanId ?? "";
    return parent === "" || parent === "0000000000000000";
}

async function readTelemetryEntries(path: string): Promise<TelemetryEntry[]> {
    const content = await readFile(path, "utf8");
    const entries: TelemetryEntry[] = [];
    for (const line of content.split("\n")) {
        const trimmed = line.trim();
        if (!trimmed) {
            continue;
        }

        entries.push(JSON.parse(trimmed));
    }

    return entries;
}

describe("Telemetry export", async () => {
    const marker = "copilot-sdk-telemetry-e2e";
    const sourceName = "ts-sdk-telemetry-e2e";
    const toolName = "echo_telemetry_marker";
    const prompt = `Use the ${toolName} tool with value '${marker}', then respond with TELEMETRY_E2E_DONE.`;

    const telemetryFileName = `telemetry-${Date.now()}-${Math.random().toString(36).slice(2)}.jsonl`;

    const {
        copilotClient: client,
        createClient,
        workDir,
    } = await createSdkTestContext({
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            // Telemetry is lowered to environment variables the native runtime reads, which
            // the in-process transport cannot carry per-client (the runtime runs in the shared
            // host process); see https://github.com/github/copilot-sdk/issues/1934. Pin the
            // child-process (stdio) transport so this scenario is exercised even in the
            // in-process CI cell, matching the .NET suite.
            connection: RuntimeConnection.forStdio(),
            telemetry: {
                filePath: telemetryFileName,
                exporterType: "file",
                sourceName,
                captureContent: true,
            },
        },
    });

    it("should export file telemetry for sdk interactions", { timeout: 90_000 }, async () => {
        await client.start();
        expect((await client.getAuthStatus()).isAuthenticated).toBe(true);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool(toolName, {
                    description: "Echoes a marker string for telemetry validation.",
                    parameters: z.object({ value: z.string() }),
                    handler: ({ value }) => value,
                }),
            ],
        });

        // Resolve account metadata before the first invocation snapshots its identity.
        expect(await session.rpc.gitHubAuth.getStatus()).toMatchObject({
            isAuthenticated: true,
            login: "e2e-test-user",
        });
        const assistantMessage = await session.sendAndWait({ prompt }, 90_000);
        expect(assistantMessage).toBeDefined();
        expect(assistantMessage?.data.content ?? "").toContain("TELEMETRY_E2E_DONE");

        await session.disconnect();
        await client.stop();

        // Telemetry exporter writes to telemetryFileName resolved relative to the CLI cwd (workDir).
        const telemetryPath = join(workDir, telemetryFileName);
        const entries = await readTelemetryEntries(telemetryPath);
        const spans = entries.filter((entry) => entry.type === "span");

        expect(spans.length).toBeGreaterThan(0);
        for (const span of spans) {
            expect(span.instrumentationScope?.name).toBe(sourceName);
        }

        for (const span of spans) {
            expect(span.status?.code).not.toBe(2);
        }

        const invokeAgentSpan = spans.find(
            (span) => getStringAttribute(span, "gen_ai.operation.name") === "invoke_agent"
        );
        expect(invokeAgentSpan).toBeDefined();
        expect(getStringAttribute(invokeAgentSpan!, "gen_ai.conversation.id")).toBe(
            session.sessionId
        );
        expect(isRootSpan(invokeAgentSpan!)).toBe(true);
        expect(getStringAttribute(invokeAgentSpan!, "enduser.pseudo.id")).toBe(
            "e2e-test-tracking-id"
        );
        expect(invokeAgentSpan!.attributes).not.toHaveProperty("user.name");
        const invokeAgentSpanId = invokeAgentSpan!.spanId;
        expect(invokeAgentSpanId).toBeTruthy();
        const invokeAgentTraceId = invokeAgentSpan!.traceId;
        expect(invokeAgentTraceId).toBeTruthy();

        const chatSpans = spans.filter(
            (span) => getStringAttribute(span, "gen_ai.operation.name") === "chat"
        );
        expect(chatSpans.length).toBeGreaterThan(0);
        for (const chat of chatSpans) {
            expect(chat.parentSpanId).toBe(invokeAgentSpanId);
            expect(chat.traceId).toBe(invokeAgentTraceId);
        }
        expect(
            chatSpans.some((span) =>
                (getStringAttribute(span, "gen_ai.input.messages") ?? "").includes(prompt)
            )
        ).toBe(true);

        const toolDefinitions = chatSpans.flatMap((span) => {
            const definitions = getStringAttribute(span, "gen_ai.tool.definitions");
            return definitions ? (JSON.parse(definitions) as unknown[]) : [];
        });
        expect(toolDefinitions).toContainEqual(
            expect.objectContaining({
                name: toolName,
                description: "Echoes a marker string for telemetry validation.",
                parameters: expect.objectContaining({
                    type: "object",
                    properties: expect.objectContaining({
                        value: expect.objectContaining({ type: "string" }),
                    }),
                }),
            })
        );
        expect(
            chatSpans.some((span) =>
                (getStringAttribute(span, "gen_ai.output.messages") ?? "").includes(
                    "TELEMETRY_E2E_DONE"
                )
            )
        ).toBe(true);

        const toolSpan = spans.find(
            (span) => getStringAttribute(span, "gen_ai.operation.name") === "execute_tool"
        );
        expect(toolSpan).toBeDefined();
        expect(toolSpan!.parentSpanId).toBe(invokeAgentSpanId);
        expect(toolSpan!.traceId).toBe(invokeAgentTraceId);
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.name")).toBe(toolName);
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.call.id")).toBeTruthy();
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.call.arguments")).toBe(
            `{"value":"${marker}"}`
        );
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.call.result")).toBe(marker);
    });

    it("should export per request subagent chat spans", { timeout: 90_000 }, async () => {
        const sourceName = "ts-sdk-subagent-telemetry-e2e";
        const telemetryFileName = "subagent-telemetry.jsonl";
        const prompt =
            "Use the task tool in sync mode to ask a task agent to read subagent-otel.txt with the view tool. Then reply with SUBAGENT_OTEL_DONE.";
        await writeFile(join(workDir, "subagent-otel.txt"), "SUBAGENT_OTEL_FILE_CONTENT");

        const subagentClient = createClient({
            connection: RuntimeConnection.forStdio(),
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            telemetry: {
                filePath: telemetryFileName,
                exporterType: "file",
                sourceName,
                captureContent: true,
            },
        });
        let sessionId: string;
        try {
            await subagentClient.start();
            expect((await subagentClient.getAuthStatus()).isAuthenticated).toBe(true);
            const session = await subagentClient.createSession({ onPermissionRequest: approveAll });
            sessionId = session.sessionId;
            try {
                expect((await session.rpc.gitHubAuth.getStatus()).isAuthenticated).toBe(true);
                const response = await session.sendAndWait({ prompt }, 90_000);
                expect(response?.data.content ?? "").toContain("SUBAGENT_OTEL_DONE");
            } finally {
                await session.disconnect();
            }
        } finally {
            await subagentClient.stop();
        }

        const spans = (await readTelemetryEntries(join(workDir, telemetryFileName))).filter(
            (entry) => entry.type === "span"
        );
        expect(spans.every((span) => span.instrumentationScope?.name === sourceName)).toBe(true);
        expect(spans.every((span) => span.status?.code !== 2)).toBe(true);
        const invocationSpans = spans.filter(
            (span) => getStringAttribute(span, "gen_ai.operation.name") === "invoke_agent"
        );
        expect(invocationSpans).toHaveLength(2);
        for (const span of invocationSpans) {
            expect(getStringAttribute(span, "enduser.pseudo.id")).toBe("e2e-test-tracking-id");
            expect(span.attributes).not.toHaveProperty("user.name");
        }
        const roots = invocationSpans.filter(isRootSpan);
        expect(roots).toHaveLength(1);
        const root = roots[0]!;
        expect(getStringAttribute(root, "gen_ai.conversation.id")).toBe(sessionId!);
        expect(root.spanId).toBeTruthy();
        expect(root.traceId).toBeTruthy();

        const taskTools = spans.filter(
            (span) =>
                getStringAttribute(span, "gen_ai.operation.name") === "execute_tool" &&
                getStringAttribute(span, "gen_ai.tool.name") === "task"
        );
        expect(taskTools).toHaveLength(1);
        const taskTool = taskTools[0]!;
        expect(taskTool.parentSpanId).toBe(root.spanId);
        const subagents = invocationSpans.filter((span) => span.parentSpanId === taskTool.spanId);
        expect(subagents).toHaveLength(1);
        const subagent = subagents[0]!;
        expect(taskTool.traceId).toBe(root.traceId);
        expect(subagent.traceId).toBe(root.traceId);

        const chats = spans.filter(
            (span) => getStringAttribute(span, "gen_ai.operation.name") === "chat"
        );
        expect(chats).toHaveLength(4);
        const parentChats = chats.filter((span) => span.parentSpanId === root.spanId);
        expect(parentChats).toHaveLength(2);
        for (const chat of parentChats) {
            expect(chat.traceId).toBe(root.traceId);
        }
        const childChats = chats.filter((span) => span.parentSpanId === subagent.spanId);
        expect(childChats).toHaveLength(2);
        for (const chat of childChats) {
            expect(chat.traceId).toBe(root.traceId);
            expect(getStringAttribute(chat, "github.copilot.initiator")).toBe("sub-agent");
        }
        const requestingChats = childChats.filter((span) =>
            (getStringAttribute(span, "gen_ai.output.messages") ?? "").includes('"view"')
        );
        expect(requestingChats).toHaveLength(1);
        expect(
            getStringAttribute(requestingChats[0]!, "gen_ai.input.messages") ?? ""
        ).not.toContain("SUBAGENT_OTEL_FILE_CONTENT");
        const finalChats = childChats.filter((span) =>
            (getStringAttribute(span, "gen_ai.output.messages") ?? "").includes(
                "SUBAGENT_OTEL_CHILD_DONE"
            )
        );
        expect(finalChats).toHaveLength(1);
        expect(getStringAttribute(finalChats[0]!, "gen_ai.input.messages")).toContain(
            "SUBAGENT_OTEL_FILE_CONTENT"
        );
    });
});
