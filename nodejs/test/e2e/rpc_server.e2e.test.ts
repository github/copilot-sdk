/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as path from "path";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Server-scoped RPC", async () => {
    const { copilotClient: client, openAiEndpoint, env, workDir } = await createSdkTestContext();

    function createAuthenticatedClient(token: string): CopilotClient {
        const childEnv = {
            ...env,
            COPILOT_DEBUG_GITHUB_API_URL: env.COPILOT_API_URL,
        };
        const authClient = new CopilotClient({
            cwd: workDir,
            env: childEnv,
            logLevel: "error",
            cliPath: process.env.COPILOT_CLI_PATH,
            gitHubToken: token,
        });
        onTestFinished(async () => {
            try {
                await authClient.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });
        return authClient;
    }

    async function configureAuthenticatedUser(
        token: string,
        quotaSnapshots?: Record<
            string,
            {
                entitlement?: number;
                overage_count?: number;
                overage_permitted?: boolean;
                percent_remaining?: number;
                timestamp_utc?: string;
                unlimited?: boolean;
            }
        >
    ): Promise<void> {
        await openAiEndpoint.setCopilotUserByToken(token, {
            login: "rpc-user",
            copilot_plan: "individual_pro",
            endpoints: {
                api: env.COPILOT_API_URL,
                telemetry: "https://localhost:1/telemetry",
            },
            analytics_tracking_id: "rpc-user-tracking-id",
            quota_snapshots: quotaSnapshots,
        });
    }

    function createSkillDirectory(skillName: string, description: string): string {
        const skillsDir = path.join(
            workDir,
            "server-rpc-skills",
            `dir-${Date.now()}-${Math.random().toString(36).slice(2)}`
        );
        const skillSubdir = path.join(skillsDir, skillName);
        fs.mkdirSync(skillSubdir, { recursive: true });
        const skillContent = `---\nname: ${skillName}\ndescription: ${description}\n---\n\n# ${skillName}\n\nThis skill is used by RPC E2E tests.\n`;
        fs.writeFileSync(path.join(skillSubdir, "SKILL.md"), skillContent);
        return skillsDir;
    }

    it("should call rpc ping with typed params and result", async () => {
        await client.start();
        const result = await client.ping("typed rpc test");
        expect(result.message).toBe("pong: typed rpc test");
        expect(result.timestamp).toBeGreaterThanOrEqual(0);
    });

    it("should call rpc models list with typed result", async () => {
        const token = "rpc-models-token";
        await configureAuthenticatedUser(token);
        const authClient = createAuthenticatedClient(token);
        await authClient.start();

        const result = await authClient.listModels();
        expect(Array.isArray(result)).toBe(true);
        expect(result.some((m) => m.id === "claude-sonnet-4.5")).toBe(true);
        for (const model of result) {
            expect(model.name).toBeTruthy();
        }
    });

    it("should call rpc account getquota when authenticated", async () => {
        const token = "rpc-quota-token";
        await configureAuthenticatedUser(token, {
            chat: {
                entitlement: 100,
                overage_count: 2,
                overage_permitted: true,
                percent_remaining: 75,
                timestamp_utc: "2026-04-30T00:00:00Z",
            },
        });
        const authClient = createAuthenticatedClient(token);
        await authClient.start();

        const result = await authClient.rpc.account.getQuota({ gitHubToken: token });

        expect(result.quotaSnapshots).toHaveProperty("chat");
        const chatQuota = result.quotaSnapshots.chat;
        expect(chatQuota.entitlementRequests).toBe(100);
        expect(chatQuota.usedRequests).toBe(25);
        expect(chatQuota.remainingPercentage).toBe(75);
        expect(chatQuota.overage).toBe(2);
        expect(chatQuota.usageAllowedWithExhaustedQuota).toBe(true);
        expect(chatQuota.overageAllowedWithExhaustedQuota).toBe(true);
        expect(chatQuota.resetDate).toBe("2026-04-30T00:00:00Z");
    });

    it("should call rpc tools list with typed result", async () => {
        await client.start();
        const result = await client.rpc.tools.list();
        expect(result.tools).toBeDefined();
        expect(result.tools.length).toBeGreaterThan(0);
        for (const tool of result.tools) {
            expect(tool.name).toBeTruthy();
        }
    });

    it("should discover server mcp and skills", async () => {
        await client.start();

        const skillName = `server-rpc-skill-${Date.now()}-${Math.random().toString(36).slice(2)}`;
        const skillDirectory = createSkillDirectory(
            skillName,
            "Skill discovered by server-scoped RPC tests."
        );

        const mcp = await client.rpc.mcp.discover({ workingDirectory: workDir });
        expect(mcp.servers).toBeDefined();

        const skills = await client.rpc.skills.discover({ skillDirectories: [skillDirectory] });
        const discovered = skills.skills.filter((s) => s.name === skillName);
        expect(discovered).toHaveLength(1);
        expect(discovered[0].description).toBe("Skill discovered by server-scoped RPC tests.");
        expect(discovered[0].enabled).toBe(true);
        expect(discovered[0].path.endsWith(path.join(skillName, "SKILL.md"))).toBe(true);

        try {
            await client.rpc.skills.config.setDisabledSkills({ disabledSkills: [skillName] });
            const disabled = await client.rpc.skills.discover({
                skillDirectories: [skillDirectory],
            });
            const disabledMatches = disabled.skills.filter((s) => s.name === skillName);
            expect(disabledMatches).toHaveLength(1);
            expect(disabledMatches[0].enabled).toBe(false);
        } finally {
            await client.rpc.skills.config.setDisabledSkills({ disabledSkills: [] });
        }
    });
});
