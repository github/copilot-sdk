/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as path from "path";
import { fileURLToPath } from "url";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import type { CopilotSession, MCPServerConfig, MCPStdioServerConfig } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const __filename = fileURLToPath(import.meta.url);
const TEST_MCP_SERVER = path.resolve(
    path.dirname(__filename),
    "../../../test/harness/test-mcp-server.mjs"
);
const TEST_HARNESS_DIR = path.dirname(TEST_MCP_SERVER);

describe("Session MCP and skills RPC", async () => {
    // --yolo auto-approves extension permission gates at the CLI level,
    // preventing breakage from new gates (e.g., extension-permission-access).
    const {
        copilotClient: client,
        workDir,
        env,
    } = await createSdkTestContext({
        copilotClientOptions: { connection: RuntimeConnection.forStdio({ args: ["--yolo"] }) },
    });

    function createSkill(skillsDir: string, skillName: string, description: string): void {
        const skillSubdir = path.join(skillsDir, skillName);
        fs.mkdirSync(skillSubdir, { recursive: true });
        const skillContent = `---\nname: ${skillName}\ndescription: ${description}\n---\n\n# ${skillName}\n\nThis skill is used by RPC E2E tests.\n`;
        fs.writeFileSync(path.join(skillSubdir, "SKILL.md"), skillContent);
    }

    function createSkillDirectory(skillName: string, description: string): string {
        const skillsDir = path.join(
            workDir,
            "session-rpc-skills",
            `dir-${Date.now()}-${Math.random().toString(36).slice(2)}`
        );
        fs.mkdirSync(skillsDir, { recursive: true });
        createSkill(skillsDir, skillName, description);
        return skillsDir;
    }

    function createTestMcpServers(...serverNames: string[]): Record<string, MCPServerConfig> {
        return Object.fromEntries(
            serverNames.map((name) => [
                name,
                {
                    type: "stdio",
                    command: "node",
                    args: [TEST_MCP_SERVER],
                    workingDirectory: TEST_HARNESS_DIR,
                    tools: ["*"],
                } as MCPStdioServerConfig,
            ])
        );
    }

    function createMcpAppsClient(): CopilotClient {
        const mcpAppsClient = new CopilotClient({
            workingDirectory: workDir,
            env: {
                ...env,
                COPILOT_MCP_APPS: "true",
                MCP_APPS: "true",
            },
            logLevel: "error",
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
        });
        onTestFinished(async () => {
            try {
                await mcpAppsClient.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });
        return mcpAppsClient;
    }

    async function waitForMcpServerStatus(
        session: CopilotSession,
        serverName: string,
        expectedStatus = "connected"
    ): Promise<void> {
        const deadline = Date.now() + 60_000;
        let lastStatus = "<not listed>";

        while (Date.now() < deadline) {
            const result = await session.rpc.mcp.list();
            const server = result.servers.find((s) => s.name === serverName);
            if (server?.status === expectedStatus) {
                return;
            }
            lastStatus = server?.status ?? "<not listed>";
            await new Promise((resolve) => setTimeout(resolve, 200));
        }

        throw new Error(
            `${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}`
        );
    }

    async function expectFailure(
        action: () => Promise<unknown>,
        expectedMessage: string
    ): Promise<void> {
        await expect(action()).rejects.toSatisfy((err: unknown) => {
            const text = err instanceof Error ? err.message : String(err);
            expect(text.toLowerCase()).toContain(expectedMessage.toLowerCase());
            return true;
        });
    }

    it("should list and toggle session skills", async () => {
        const skillName = `session-rpc-skill-${Date.now()}-${Math.random().toString(36).slice(2)}`;
        const skillsDir = createSkillDirectory(skillName, "Session skill controlled by RPC.");
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillDirectories: [skillsDir],
            disabledSkills: [skillName],
        });

        const disabled = await session.rpc.skills.list();
        const disabledSkill = disabled.skills.find((s) => s.name === skillName);
        expect(disabledSkill).toBeDefined();
        expect(disabledSkill!.enabled).toBe(false);
        expect(disabledSkill!.path.endsWith(path.join(skillName, "SKILL.md"))).toBe(true);

        await session.rpc.skills.enable({ name: skillName });
        const enabled = await session.rpc.skills.list();
        const enabledSkill = enabled.skills.find((s) => s.name === skillName);
        expect(enabledSkill).toBeDefined();
        expect(enabledSkill!.enabled).toBe(true);

        await session.rpc.skills.disable({ name: skillName });
        const disabledAgain = await session.rpc.skills.list();
        const disabledSkillAgain = disabledAgain.skills.find((s) => s.name === skillName);
        expect(disabledSkillAgain).toBeDefined();
        expect(disabledSkillAgain!.enabled).toBe(false);

        await session.disconnect();
    });

    it("should reload session skills", async () => {
        const skillsDir = path.join(
            workDir,
            "reloadable-rpc-skills",
            `dir-${Date.now()}-${Math.random().toString(36).slice(2)}`
        );
        fs.mkdirSync(skillsDir, { recursive: true });
        const skillName = `reload-rpc-skill-${Date.now()}-${Math.random().toString(36).slice(2)}`;

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillDirectories: [skillsDir],
        });

        const before = await session.rpc.skills.list();
        expect(before.skills.find((s) => s.name === skillName)).toBeUndefined();

        createSkill(skillsDir, skillName, "Skill added after session creation.");
        await session.rpc.skills.reload();

        const after = await session.rpc.skills.list();
        const reloadedSkill = after.skills.find((s) => s.name === skillName);
        expect(reloadedSkill).toBeDefined();
        expect(reloadedSkill!.enabled).toBe(true);
        expect(reloadedSkill!.description).toBe("Skill added after session creation.");

        await session.disconnect();
    });

    it("should ensure skills are loaded and list invoked skills", async () => {
        const skillName = `ensure-rpc-skill-${Date.now()}-${Math.random().toString(36).slice(2)}`;
        const skillsDir = createSkillDirectory(skillName, "Skill loaded explicitly by RPC.");
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillDirectories: [skillsDir],
        });

        await session.rpc.skills.ensureLoaded();

        const loaded = await session.rpc.skills.list();
        const skill = loaded.skills.find((s) => s.name === skillName);
        expect(skill).toBeDefined();
        expect(skill!.enabled).toBe(true);
        expect(skill!.description).toBe("Skill loaded explicitly by RPC.");

        const invoked = await session.rpc.skills.getInvoked();
        expect(invoked.skills).toEqual([]);

        await session.disconnect();
    });

    it("should list mcp servers with configured server", async () => {
        const serverName = "rpc-list-mcp-server";
        const mcpServers = createTestMcpServers(serverName);

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers,
        });

        await waitForMcpServerStatus(session, serverName);
        const result = await session.rpc.mcp.list();
        const server = result.servers.find((s) => s.name === serverName);
        expect(server).toBeDefined();
        expect(typeof server!.status).toBe("string");

        await session.disconnect();
    });

    it("should set mcp env value mode and remove github server", async () => {
        const serverName = "github";
        const mcpServers = createTestMcpServers(serverName);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers,
        });

        await waitForMcpServerStatus(session, serverName);

        const direct = await session.rpc.mcp.setEnvValueMode({ mode: "direct" });
        expect(direct.mode).toBe("direct");

        const indirect = await session.rpc.mcp.setEnvValueMode({ mode: "indirect" });
        expect(indirect.mode).toBe("indirect");

        const removeGitHub = await session.rpc.mcp.removeGitHub();
        expect(removeGitHub.removed).toBe(false);

        const servers = await session.rpc.mcp.list();
        expect(
            servers.servers.some(
                (server) => server.name === serverName && server.status === "connected"
            )
        ).toBe(true);

        await session.disconnect();
    });

    it("should report mcp sampling failure and cancel missing sampling", async () => {
        const serverName = "rpc-sampling-server";
        const mcpServers = createTestMcpServers(serverName);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers,
        });

        await waitForMcpServerStatus(session, serverName);

        const cancelMissing = await session.rpc.mcp.cancelSamplingExecution({
            requestId: `missing-${Date.now()}`,
        });
        expect(cancelMissing.cancelled).toBe(false);

        try {
            const result = await session.rpc.mcp.executeSampling({
                requestId: `sampling-${Date.now()}`,
                serverName,
                mcpRequestId: `mcp-request-${Date.now()}`,
                request: {},
            });

            expect(result.action).toBe("failure");
            expect(result.result).toBeUndefined();
            expect(result.error?.trim()).toBeTruthy();
            expect(result.error?.toLowerCase()).not.toContain("unhandled method");
            expect(result.error?.toLowerCase()).toMatch(/sampling|message|request/);
        } catch (err: unknown) {
            const text = err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
            expect(text.toLowerCase()).not.toContain("unhandled method");
            expect(text.toLowerCase()).toMatch(/sampling|message|request/);
        } finally {
            await session.disconnect();
        }
    });

    it("should list plugins", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const result = await session.rpc.plugins.list();
        expect(Array.isArray(result.plugins)).toBe(true);
        for (const plugin of result.plugins) {
            expect(plugin.name).toBeTruthy();
        }

        await session.disconnect();
    });

    it("should list extensions", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const result = await session.rpc.extensions.list();
        expect(Array.isArray(result.extensions)).toBe(true);
        for (const extension of result.extensions) {
            expect(extension.id).toBeTruthy();
            expect(extension.name).toBeTruthy();
        }

        await session.disconnect();
    });

    it("should round trip mcp app host context", async () => {
        const mcpAppsClient = createMcpAppsClient();
        const session = await mcpAppsClient.createSession({ onPermissionRequest: approveAll });
        try {
            await session.rpc.mcp.apps.setHostContext({
                context: {
                    availableDisplayModes: ["inline", "fullscreen"],
                    displayMode: "inline",
                    locale: "en-GB",
                    platform: "desktop",
                    theme: "dark",
                    timeZone: "Etc/UTC",
                    userAgent: "node-sdk-e2e",
                },
            });

            const result = await session.rpc.mcp.apps.getHostContext();
            expect(result.context.displayMode).toBe("inline");
            expect(result.context.locale).toBe("en-GB");
            expect(result.context.platform).toBe("desktop");
            expect(result.context.theme).toBe("dark");
            expect(result.context.timeZone).toBe("Etc/UTC");
            expect(result.context.userAgent).toBe("node-sdk-e2e");
            expect(result.context.availableDisplayModes).toEqual(["inline", "fullscreen"]);
        } finally {
            await session.disconnect();
            await mcpAppsClient.stop();
        }
    });

    it("should diagnose and report mcp app capability errors", async () => {
        const serverName = "rpc-apps-server";
        const otherServerName = "rpc-apps-other-server";
        const mcpServers = createTestMcpServers(serverName, otherServerName);
        (mcpServers[serverName] as MCPStdioServerConfig).env = {
            MCP_APP_RPC_VALUE: "from-app-rpc",
        };
        const mcpAppsClient = createMcpAppsClient();
        const session = await mcpAppsClient.createSession({
            onPermissionRequest: approveAll,
            mcpServers,
        });
        try {
            await waitForMcpServerStatus(session, serverName);
            await waitForMcpServerStatus(session, otherServerName);

            const diagnose = await session.rpc.mcp.apps.diagnose({ serverName });
            expect(diagnose.capability).toBeDefined();
            expect(diagnose.server.connected).toBe(true);
            expect(diagnose.server.toolCount).toBeGreaterThanOrEqual(1);
            expect(diagnose.server.toolsWithUiMeta).toBe(0);
            expect(diagnose.server.sampleToolNames).toEqual([]);

            await expectFailure(
                () =>
                    session.rpc.mcp.apps.listTools({
                        serverName,
                        originServerName: serverName,
                    }),
                "mcp-apps"
            );
            await expectFailure(
                () =>
                    session.rpc.mcp.apps.listTools({
                        serverName,
                        originServerName: otherServerName,
                    }),
                "mcp-apps"
            );
            await expectFailure(
                () =>
                    session.rpc.mcp.apps.callTool({
                        serverName,
                        toolName: "get_env",
                        originServerName: serverName,
                        arguments: { name: "MCP_APP_RPC_VALUE" },
                    }),
                "mcp-apps"
            );
        } finally {
            await session.disconnect();
            await mcpAppsClient.stop();
        }
    });

    it("should report error when mcp app resource is not available", async () => {
        const serverName = "rpc-apps-resource-server";
        const mcpAppsClient = createMcpAppsClient();
        const session = await mcpAppsClient.createSession({
            onPermissionRequest: approveAll,
            mcpServers: createTestMcpServers(serverName),
        });
        try {
            await waitForMcpServerStatus(session, serverName);

            await expect(
                session.rpc.mcp.apps.readResource({
                    serverName,
                    uri: "ui://missing-resource",
                })
            ).rejects.toSatisfy((err: unknown) => {
                const text =
                    err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
                expect(text.toLowerCase()).not.toContain("unhandled method");
                expect(text.toLowerCase()).toMatch(/resource|not found|method not found/);
                return true;
            });
        } finally {
            await session.disconnect();
            await mcpAppsClient.stop();
        }
    });

    it("should report error when mcp host is not initialized", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await expectFailure(
            () => session.rpc.mcp.enable({ serverName: "missing-server" }),
            "No MCP host initialized"
        );
        await expectFailure(
            () => session.rpc.mcp.disable({ serverName: "missing-server" }),
            "No MCP host initialized"
        );
        await expectFailure(() => session.rpc.mcp.reload(), "MCP config reload not available");
        await expectFailure(
            () => session.rpc.mcp.oauth.login({ serverName: "missing-server" }),
            "MCP host is not available"
        );

        await session.disconnect();
    });

    it("should report error when mcp oauth server is not configured", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: createTestMcpServers("configured-stdio-server"),
        });
        await waitForMcpServerStatus(session, "configured-stdio-server");

        await expectFailure(
            () => session.rpc.mcp.oauth.login({ serverName: "missing-server" }),
            "is not configured"
        );

        await session.disconnect();
    });

    it("should report error when mcp oauth server is not remote", async () => {
        const serverName = "configured-stdio-server";
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: createTestMcpServers(serverName),
        });
        await waitForMcpServerStatus(session, serverName);

        await expectFailure(
            () =>
                session.rpc.mcp.oauth.login({
                    serverName,
                    forceReauth: true,
                    clientName: "SDK E2E",
                    callbackSuccessMessage: "Done",
                }),
            "not a remote server"
        );

        await session.disconnect();
    });

    it("should report error when extensions are not available", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await expectFailure(
            () => session.rpc.extensions.enable({ id: "missing-extension" }),
            "Extensions not available"
        );
        await expectFailure(
            () => session.rpc.extensions.disable({ id: "missing-extension" }),
            "Extensions not available"
        );
        await expectFailure(() => session.rpc.extensions.reload(), "Extensions not available");

        await session.disconnect();
    });
});
