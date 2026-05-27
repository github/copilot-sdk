/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs, { realpathSync } from "node:fs";
import os from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll, BuiltInTools, ToolSet } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

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
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThanOrEqual(1);
        const tools = exchanges[exchanges.length - 1].request.tools ?? [];
        return tools.flatMap((t) =>
            t.type === "function" && t.function?.name ? [t.function.name] : []
        );
    }

    async function getSystemMessageSentToLLM(): Promise<string> {
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThanOrEqual(1);
        const messages = exchanges[exchanges.length - 1].request.messages ?? [];
        const sys = messages.find((m) => m.role === "system");
        const content = sys?.content;
        if (typeof content === "string") return content;
        if (Array.isArray(content)) {
            return content
                .map((p) => (typeof p === "object" && p && "text" in p ? p.text : ""))
                .join("\n");
        }
        return "";
    }

    it("empty mode isolated set shell tool is not exposed", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        await session.sendAndWait({ prompt: "Say hi." });

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

    it("empty mode builtin star exposes all built in tools", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn("*"),
        });
        await session.sendAndWait({ prompt: "Say hi." });

        const toolNames = await getToolsExposedToLLM();
        // The shell tool name differs by platform (bash vs powershell);
        // either way, it's a canonical built-in excluded from Isolated, and
        // builtin:* should bring it back.
        const shellToolName = process.platform === "win32" ? "powershell" : "bash";
        expect(toolNames).toContain(shellToolName);

        await session.disconnect();
    });

    it("empty mode excluded tools subtracts from available tools", async () => {
        const shellToolName = process.platform === "win32" ? "powershell" : "bash";
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn("*"),
            excludedTools: [`builtin:${shellToolName}`],
        });
        await session.sendAndWait({ prompt: "Say hi." });

        const toolNames = await getToolsExposedToLLM();
        // The platform shell is in builtin:* but explicitly excluded → must not be exposed.
        expect(toolNames).not.toContain(shellToolName);
        // Other built-ins are still there (proves the subtraction is targeted).
        expect(toolNames.length).toBeGreaterThan(0);

        await session.disconnect();
    });

    it("empty mode strips environment_context from the system message by default", async () => {
        // We can't directly observe section presence, but we can detect it
        // indirectly: in default empty mode the SDK injects the customize-mode
        // override `environment_context: { action: "remove" }`. We also append
        // a deterministic instruction. If the env_context strip didn't fire,
        // the runtime would still inject OS/cwd lines into the system message
        // and the model would be free to mention them; with the strip in place
        // the model has no env info to lean on and follows our instruction.
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: {
                mode: "customize",
                content:
                    "If the user asks you to name an element, reply with exactly the single word ARGON in all caps and nothing else.",
            },
        });
        const reply = await session.sendAndWait({ prompt: "Name an element." });
        expect(reply?.data.content).toContain("ARGON");

        const systemMessage = await getSystemMessageSentToLLM();
        expect(systemMessage).not.toMatch(/Current working directory:/i);
        expect(systemMessage).not.toMatch(/Operating System:/i);

        await session.disconnect();
    });

    it("empty mode system message replace llm follows caller content verbatim", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: {
                mode: "replace",
                content:
                    "You are a test fixture. Whenever the user asks anything, reply with exactly the single word KRYPTON in all caps and nothing else.",
            },
        });
        const reply = await session.sendAndWait({ prompt: "Hello." });
        expect(reply?.data.content).toContain("KRYPTON");

        await session.disconnect();
    });

    it("empty mode append caller instruction takes effect and env context stripped", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: {
                mode: "append",
                content:
                    "If the user asks you to name a noble gas, reply with exactly the single word XENON in all caps and nothing else.",
            },
        });
        const reply = await session.sendAndWait({ prompt: "Name a noble gas." });
        expect(reply?.data.content).toContain("XENON");

        const systemMessage = await getSystemMessageSentToLLM();
        expect(systemMessage).not.toMatch(/Current working directory:/i);
        expect(systemMessage).not.toMatch(/Operating System:/i);

        await session.disconnect();
    });
});
