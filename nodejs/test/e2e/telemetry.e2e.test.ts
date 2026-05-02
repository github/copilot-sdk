/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync, statSync } from "fs";
import { readFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getFinalAssistantMessage } from "./harness/sdkTestHelper.js";

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

async function readTelemetryEntries(
    path: string,
    isComplete: (entries: TelemetryEntry[]) => boolean,
    timeoutMs = 30_000
): Promise<TelemetryEntry[]> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (existsSync(path) && statSync(path).size > 0) {
            const content = await readFile(path, "utf8");
            const entries: TelemetryEntry[] = [];
            for (const line of content.split("\n")) {
                const trimmed = line.trim();
                if (!trimmed) continue;
                try {
                    entries.push(JSON.parse(trimmed));
                } catch {
                    // Skip malformed lines (file may still be writing)
                }
            }
            if (entries.length > 0 && isComplete(entries)) {
                return entries;
            }
        }
        await new Promise((resolve) => setTimeout(resolve, 100));
    }
    throw new Error(`Timed out waiting for telemetry records in '${path}'.`);
}

describe("Telemetry export", async () => {
    const marker = "copilot-sdk-telemetry-e2e";
    const sourceName = "ts-sdk-telemetry-e2e";
    const toolName = "echo_telemetry_marker";
    const prompt = `Use the ${toolName} tool with value '${marker}', then respond with TELEMETRY_E2E_DONE.`;

    const telemetryFileName = `telemetry-${Date.now()}-${Math.random().toString(36).slice(2)}.jsonl`;

    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            telemetry: {
                filePath: telemetryFileName,
                exporterType: "file",
                sourceName,
                captureContent: true,
            },
        },
    });

    it("should export file telemetry for sdk interactions", { timeout: 90_000 }, async () => {
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

        await session.send({ prompt });
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage).toBeDefined();
        expect(assistantMessage.data.content ?? "").toContain("TELEMETRY_E2E_DONE");

        await session.disconnect();
        await client.stop();

        // Telemetry exporter writes to telemetryFileName resolved relative to the CLI cwd (workDir).
        const telemetryPath = join(workDir, telemetryFileName);
        const entries = await readTelemetryEntries(telemetryPath, (entries) =>
            entries.some(
                (entry) =>
                    entry.type === "span" &&
                    getStringAttribute(entry, "gen_ai.operation.name") === "invoke_agent"
            )
        );
        const spans = entries.filter((entry) => entry.type === "span");

        expect(spans.length).toBeGreaterThan(0);
        for (const span of spans) {
            expect(span.instrumentationScope?.name).toBe(sourceName);
        }

        // All spans for one SDK turn must share the same trace id and must not be in error state.
        const traceIds = Array.from(
            new Set(spans.map((span) => span.traceId).filter((id): id is string => Boolean(id)))
        );
        expect(traceIds).toHaveLength(1);
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
        const invokeAgentSpanId = invokeAgentSpan!.spanId;
        expect(invokeAgentSpanId).toBeTruthy();

        const chatSpans = spans.filter(
            (span) => getStringAttribute(span, "gen_ai.operation.name") === "chat"
        );
        expect(chatSpans.length).toBeGreaterThan(0);
        for (const chat of chatSpans) {
            expect(chat.parentSpanId).toBe(invokeAgentSpanId);
        }
        expect(
            chatSpans.some((span) =>
                (getStringAttribute(span, "gen_ai.input.messages") ?? "").includes(prompt)
            )
        ).toBe(true);
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
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.name")).toBe(toolName);
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.call.id")).toBeTruthy();
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.call.arguments")).toBe(
            `{"value":"${marker}"}`
        );
        expect(getStringAttribute(toolSpan!, "gen_ai.tool.call.result")).toBe(marker);
    });
});
