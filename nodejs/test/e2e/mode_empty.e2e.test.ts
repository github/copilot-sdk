/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs, { realpathSync } from "node:fs";
import os from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll, BuiltInTools, ToolSet } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

/**
 * E2E coverage for the Mode = "empty" SDK surface and source-qualified tool
 * filter patterns. The runtime is mode-agnostic — these tests verify that the
 * SDK's translation reaches the runtime correctly by inspecting:
 *   - the resulting CapiProxy chat-completion request (the LLM only sees tools
 *     that the runtime exposed for the session), and
 *   - end-to-end behavior (asking the agent to use a tool that should or
 *     shouldn't be enabled).
 */
describe("Mode = empty + ToolSet patterns", async () => {
    // Empty mode requires baseDirectory at construction time; the harness
    // already creates a per-test home dir but doesn't surface it directly,
    // so spin up our own and feed it to the client constructor.
    const emptyModeBaseDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-empty-mode-")));
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext({
        copilotClientOptions: { mode: "empty", baseDirectory: emptyModeBaseDir },
    });

    async function getToolsExposedToLLM(): Promise<string[]> {
        await retry(
            "capture chat completion request",
            async () => {
                const exchanges = await openAiEndpoint.getExchanges();
                expect(exchanges.length).toBeGreaterThanOrEqual(1);
            },
            1_200
        );
        const exchanges = await openAiEndpoint.getExchanges();
        const tools = exchanges[exchanges.length - 1].request.tools ?? [];
        return tools.flatMap((t) =>
            t.type === "function" && t.function?.name ? [t.function.name] : []
        );
    }

    it("empty mode + Isolated set: shell tool is NOT exposed", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        await session.send({ prompt: "Say hi." }).catch(() => {});

        const toolNames = await getToolsExposedToLLM();
        // Isolated should not contain shell / fs editing / web fetch / grep.
        expect(toolNames).not.toContain("bash");
        expect(toolNames).not.toContain("edit");
        expect(toolNames).not.toContain("grep");
        expect(toolNames).not.toContain("web_fetch");
        // Sanity: at least one of the isolated tools is registered.
        const anyIsolated = BuiltInTools.Isolated.some((name) => toolNames.includes(name));
        expect(anyIsolated).toBe(true);

        await session.disconnect();
    });

    it("empty mode + builtin:* exposes all built-in tools", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn("*"),
        });
        await session.send({ prompt: "Say hi." }).catch(() => {});

        const toolNames = await getToolsExposedToLLM();
        // The shell tool name differs by platform (bash vs powershell);
        // either way, it's a canonical built-in excluded from Isolated, and
        // builtin:* should bring it back.
        const shellToolName = process.platform === "win32" ? "powershell" : "bash";
        expect(toolNames).toContain(shellToolName);

        await session.disconnect();
    });

    it("empty mode + excluded default: excludedTools subtracts from availableTools", async () => {
        const shellToolName = process.platform === "win32" ? "powershell" : "bash";
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn("*"),
            excludedTools: [`builtin:${shellToolName}`],
        });
        await session.send({ prompt: "Say hi." }).catch(() => {});

        const toolNames = await getToolsExposedToLLM();
        // The platform shell is in builtin:* but explicitly excluded → must not be exposed.
        expect(toolNames).not.toContain(shellToolName);
        // Other built-ins are still there (proves the subtraction is targeted).
        expect(toolNames.length).toBeGreaterThan(0);

        await session.disconnect();
    });
});
