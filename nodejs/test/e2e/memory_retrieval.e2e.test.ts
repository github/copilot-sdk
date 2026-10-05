/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtempSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN, isCI } from "./harness/sdkTestContext.js";

const memoryFeatureFlags = {
    "copilot-feature-agentic-memory": true,
    "copilot-feature-agentic-memory-disabled": false,
    copilot_feature_agentic_memory_user_scoped: true,
};
const responseToken = "SDK_MEMORY_RETRIEVAL_COMPLETE";
const prompt = `Reply with exactly ${responseToken}.`;
const structuredMemory = {
    subject: "SDK_MEMORY_SUBJECT",
    fact: "SDK_MEMORY_FACT_MARKER",
    citations: ["SDK_MEMORY_CITATION"],
};

describe("Memory retrieval through the TypeScript SDK", async () => {
    const { copilotClient, openAiEndpoint, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            env: {
                "copilot-feature-agentic-memory": "true",
                "copilot-feature-agentic-memory-disabled": "false",
                copilot_feature_agentic_memory_user_scoped: "true",
            },
        },
    });
    const authTokenToUse = isCI
        ? DEFAULT_GITHUB_TOKEN
        : (process.env.GITHUB_TOKEN ?? DEFAULT_GITHUB_TOKEN);

    async function runMemoryTurn(memoriesStatusCode = 200) {
        const repositoryDirectory = mkdtempSync(join(workDir, `memory-${memoriesStatusCode}-`));
        execFileSync("git", ["init", "--initial-branch=main"], { cwd: repositoryDirectory });
        execFileSync(
            "git",
            [
                "remote",
                "add",
                "origin",
                `https://github.com/octo/sdk-memory-${memoriesStatusCode}.git`,
            ],
            { cwd: repositoryDirectory }
        );
        await openAiEndpoint.setCopilotUserByToken(authTokenToUse, {
            login: "e2e-test-user",
            id: 12345,
            copilot_plan: "individual_pro",
            is_mcp_enabled: true,
            endpoints: {
                api: openAiEndpoint.url,
                telemetry: "https://localhost:1/telemetry",
            },
            analytics_tracking_id: "e2e-test-tracking-id",
        });
        await openAiEndpoint.setMemoryApiStub({
            enabled: { enabled: true },
            memories: { userMemories: [structuredMemory] },
            memoriesStatusCode,
        });
        const session = await copilotClient.createSession({
            featureFlags: memoryFeatureFlags,
            gitHubToken: authTokenToUse,
            memory: { enabled: true },
            onPermissionRequest: approveAll,
            workingDirectory: repositoryDirectory,
        });
        try {
            const response = await session.sendAndWait({ prompt });
            expect(response?.data.content).toBe(responseToken);
        } finally {
            await session.disconnect();
        }

        const requests = await openAiEndpoint.getRequests();
        const requestUrls = requests.map((request) => request.url);
        const memoryRequests = requests.filter((request) =>
            request.url.includes("/internal/memory/v0/")
        );
        const requestPaths = memoryRequests.map(
            (request) => new URL(request.url, "https://memory.test").pathname
        );
        const exchanges = await openAiEndpoint.getExchanges();
        const systemContent = exchanges
            .flatMap((exchange) => exchange.request.messages)
            .filter((message) => message.role === "system")
            .map((message) =>
                typeof message.content === "string"
                    ? message.content
                    : JSON.stringify(message.content)
            )
            .join("\n");
        const toolNames = exchanges
            .flatMap((exchange) => exchange.request.tools ?? [])
            .map((tool) => (tool.type === "function" ? tool.function.name : tool.type));

        return { requestPaths, requestUrls, systemContent, toolNames };
    }

    it("injects structured memories into the model request", async () => {
        const { requestPaths, requestUrls, systemContent, toolNames } = await runMemoryTurn();

        expect(
            requestPaths,
            `Captured URLs: ${JSON.stringify(requestUrls)}; tools: ${JSON.stringify(toolNames)}; structured fact present: ${systemContent.includes(structuredMemory.fact)}`
        ).toContain("/agents/swe/internal/memory/v0/enabled");
        expect(requestPaths).toContain("/agents/swe/internal/memory/v0/memories");
        expect(requestPaths).not.toContain("/agents/swe/internal/memory/v0/prompt");
        expect(systemContent).toContain(structuredMemory.subject);
        expect(systemContent).toContain(structuredMemory.fact);
        expect(systemContent).toContain(structuredMemory.citations);
    });

    it("does not inject memories when structured retrieval fails", async () => {
        const { requestPaths, requestUrls, systemContent } = await runMemoryTurn(401);

        expect(requestPaths, `Captured URLs: ${JSON.stringify(requestUrls)}`).toContain(
            "/agents/swe/internal/memory/v0/enabled"
        );
        expect(requestPaths).toContain("/agents/swe/internal/memory/v0/memories");
        expect(requestPaths).not.toContain("/agents/swe/internal/memory/v0/prompt");
        expect(systemContent).not.toContain(structuredMemory.fact);
    });
});
