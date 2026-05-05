/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as path from "path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { MCPServerConfig } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session MCP and skills RPC", async () => {
    // --yolo auto-approves extension permission gates at the CLI level,
    // preventing breakage from new gates (e.g., extension-permission-access).
    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: { cliArgs: ["--yolo"] },
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

    it("should list mcp servers with configured server", async () => {
        const serverName = "rpc-list-mcp-server";
        const mcpServers: Record<string, MCPServerConfig> = {
            [serverName]: {
                type: "stdio",
                command: "echo",
                args: ["rpc-list-mcp-server"],
                tools: ["*"],
            },
        };

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers,
        });

        const result = await session.rpc.mcp.list();
        const server = result.servers.find((s) => s.name === serverName);
        expect(server).toBeDefined();
        expect(typeof server!.status).toBe("string");

        await session.disconnect();
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
