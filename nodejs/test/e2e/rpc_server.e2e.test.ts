/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as path from "path";
import { randomUUID } from "node:crypto";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("Server-scoped RPC", async () => {
    const { copilotClient: client, openAiEndpoint, env, workDir } = await createSdkTestContext();

    function createAuthenticatedClient(token: string): CopilotClient {
        return createClientWithEnv(
            {
                COPILOT_DEBUG_GITHUB_API_URL: env.COPILOT_API_URL,
            },
            token
        );
    }

    function createClientWithEnv(
        extraEnv: Record<string, string | undefined>,
        token?: string
    ): CopilotClient {
        const childEnv = {
            ...env,
            ...extraEnv,
        };
        const extraClient = new CopilotClient({
            workingDirectory: workDir,
            env: childEnv,
            logLevel: "error",
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken: token,
        });
        onTestFinished(async () => {
            try {
                await extraClient.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });
        return extraClient;
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

    function createUniqueWorkDirectory(prefix: string): string {
        const directory = path.join(workDir, `${prefix}-${randomUUID()}`);
        fs.mkdirSync(directory, { recursive: true });
        return directory;
    }

    async function saveAndGetEventFilePath(
        targetClient: CopilotClient,
        sessionId: string
    ): Promise<string> {
        await expect(targetClient.rpc.sessions.save({ sessionId })).resolves.toBeDefined();
        const pathResult = await targetClient.rpc.sessions.getEventFilePath({ sessionId });
        expect(pathResult.filePath.trim()).toBeTruthy();
        expect(path.isAbsolute(pathResult.filePath)).toBe(true);
        expect(path.basename(pathResult.filePath)).toBe("events.jsonl");
        return pathResult.filePath;
    }

    it("should call rpc ping with typed params and result", async () => {
        await client.start();
        const result = await client.ping("typed rpc test");
        expect(result.message).toBe("pong: typed rpc test");
        expect(Date.parse(result.timestamp)).not.toBeNaN();
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

    it("should call rpc sessionFs setProvider with typed result", async () => {
        const fsClient = createClientWithEnv({});
        await fsClient.start();

        const result = await fsClient.rpc.sessionFs.setProvider({
            initialCwd: "/",
            sessionStatePath: "/session-state",
            conventions: "posix",
            capabilities: { sqlite: true },
        });

        expect(result.success).toBe(true);
    });

    it("should add secret filter values", async () => {
        const secretClient = createClientWithEnv({ COPILOT_ENABLE_SECRET_FILTERING: "true" });
        await secretClient.start();

        const result = await secretClient.rpc.secrets.addFilterValues({
            values: [`rpc-secret-${randomUUID()}`],
        });

        expect(result.ok).toBe(true);
    });

    it("should list, find, and inspect persisted session state", async () => {
        const sessionId = randomUUID();
        const missingTaskId = `missing-task-${randomUUID()}`;
        const missingSessionId = randomUUID();
        const workingDirectory = createUniqueWorkDirectory("server-rpc-list");
        let closed = false;
        const session = await client.createSession({
            sessionId,
            workingDirectory,
        });
        try {
            await session.log("SERVER_RPC_LIST_READY");
            const eventFilePath = await saveAndGetEventFilePath(client, sessionId);
            expect(eventFilePath.toLowerCase()).toContain(sessionId.toLowerCase());

            await client.rpc.sessions.close({ sessionId });
            closed = true;

            const listed = await client.rpc.sessions.list({
                metadataLimit: 0,
                filter: { cwd: workingDirectory },
            });
            expect(Array.isArray(listed.sessions)).toBe(true);
            expect(
                listed.sessions.every(
                    (session) =>
                        session.context?.cwd === undefined ||
                        pathsEqual(session.context.cwd, workingDirectory)
                )
            ).toBe(true);

            const byPrefix = await client.rpc.sessions.findByPrefix({
                prefix: missingSessionId.slice(0, 8),
            });
            expect(byPrefix.sessionId).toBeUndefined();

            const byTaskId = await client.rpc.sessions.findByTaskId({ taskId: missingTaskId });
            expect(byTaskId.sessionId).toBeUndefined();

            const lastForContext = await client.rpc.sessions.getLastForContext({
                context: { cwd: workingDirectory },
            });
            expect(
                lastForContext.sessionId === undefined || lastForContext.sessionId === sessionId
            ).toBe(true);

            const sizes = await client.rpc.sessions.getSizes();
            if (sizes.sizes[sessionId] !== undefined) {
                expect(sizes.sizes[sessionId]).toBeGreaterThanOrEqual(0);
            }

            const inUse = await client.rpc.sessions.checkInUse({
                sessionIds: [sessionId, missingSessionId],
            });
            expect(inUse.inUse).not.toContain(missingSessionId);

            const remoteSteerable = await client.rpc.sessions.getPersistedRemoteSteerable({
                sessionId,
            });
            expect(remoteSteerable.remoteSteerable).toBeUndefined();
        } finally {
            if (closed) {
                await client.rpc.sessions.bulkDelete({ sessionIds: [sessionId] });
            } else {
                await session.disconnect();
            }
        }
    }, 60_000);

    it("should enrich basic session metadata", async () => {
        const sessionId = randomUUID();
        const workingDirectory = createUniqueWorkDirectory("server-rpc-enrich");
        const session = await client.createSession({
            sessionId,
            workingDirectory,
            onPermissionRequest: () => ({ kind: "approve-once" }),
        });
        try {
            await saveAndGetEventFilePath(client, sessionId);

            const now = new Date().toISOString();
            const result = await client.rpc.sessions.enrichMetadata({
                sessions: [
                    {
                        sessionId,
                        startTime: now,
                        modifiedTime: now,
                        isRemote: false,
                        name: "Basic metadata",
                        context: { cwd: workingDirectory },
                    },
                ],
            });

            const enriched = result.sessions[0];
            expect(enriched.sessionId).toBe(sessionId);
            expect(pathsEqual(enriched.context?.cwd ?? "", workingDirectory)).toBe(true);
            expect(enriched.isRemote).toBe(false);
        } finally {
            await session.disconnect();
        }
    });

    it("should close active session and release lock", async () => {
        const sessionId = randomUUID();
        const workingDirectory = createUniqueWorkDirectory("server-rpc-close");
        const session = await client.createSession({
            sessionId,
            workingDirectory,
            onPermissionRequest: () => ({ kind: "approve-once" }),
        });

        await session.log("SERVER_RPC_CLOSE_READY");
        await saveAndGetEventFilePath(client, sessionId);

        await expect(client.rpc.sessions.close({ sessionId })).resolves.toBeDefined();
        await expect(client.rpc.sessions.releaseLock({ sessionId })).resolves.toBeDefined();
        const inUse = await client.rpc.sessions.checkInUse({ sessionIds: [sessionId] });
        expect(inUse.inUse).not.toContain(sessionId);

        // The server-side close disposes the session; do not call session.disconnect().
    });

    it("should prune dry-run and bulkDelete persisted session", async () => {
        const sessionId = randomUUID();
        const missingSessionId = randomUUID();
        const workingDirectory = createUniqueWorkDirectory("server-rpc-delete");
        const session = await client.createSession({
            sessionId,
            workingDirectory,
            onPermissionRequest: () => ({ kind: "approve-once" }),
        });

        await saveAndGetEventFilePath(client, sessionId);
        await client.rpc.sessions.close({ sessionId });

        const prune = await client.rpc.sessions.pruneOld({
            olderThanDays: 0,
            dryRun: true,
            includeNamed: true,
            excludeSessionIds: [],
        });
        expect(prune.dryRun).toBe(true);
        expect(prune.candidates).not.toContain(missingSessionId);
        expect(prune.deleted).not.toContain(sessionId);
        expect(prune.freedBytes).toBeGreaterThanOrEqual(0);

        const deleted = await client.rpc.sessions.bulkDelete({
            sessionIds: [sessionId, missingSessionId],
        });
        expect(deleted.freedBytes[sessionId]).toBeGreaterThanOrEqual(0);
        if (deleted.freedBytes[missingSessionId] !== undefined) {
            expect(deleted.freedBytes[missingSessionId]).toBe(0);
        }

        await waitForCondition(
            async () =>
                !(await client.rpc.sessions.list({})).sessions.some(
                    (session) => session.sessionId === sessionId
                ),
            { timeoutMessage: `Timed out waiting for sessions.bulkDelete to remove ${sessionId}.` }
        );

        // The server-side close/deletion disposes the session; do not call session.disconnect().
        expect(session.sessionId).toBe(sessionId);
    });

    it("should set additional plugins and reload deferred hooks", async () => {
        await client.start();
        await expect(
            client.rpc.sessions.setAdditionalPlugins({ plugins: [] })
        ).resolves.toBeDefined();

        const sessionId = randomUUID();
        const workingDirectory = createUniqueWorkDirectory("server-rpc-hooks");
        const session = await client.createSession({
            sessionId,
            workingDirectory,
            enableConfigDiscovery: false,
        });
        try {
            await expect(
                client.rpc.sessions.reloadPluginHooks({ sessionId, deferRepoHooks: true })
            ).resolves.toBeDefined();

            const loaded = await client.rpc.sessions.loadDeferredRepoHooks({ sessionId });
            expect(loaded.startupPrompts).toEqual([]);
            expect(loaded.hookCount).toBe(0);
        } finally {
            await client.rpc.sessions.setAdditionalPlugins({ plugins: [] });
            await session.disconnect();
        }
    });

    it("should report implemented error when connecting unknown remote session", async () => {
        await client.start();
        const remoteSessionId = `remote-${randomUUID()}`;

        await expect(client.rpc.sessions.connect({ sessionId: remoteSessionId })).rejects.toSatisfy(
            (err: unknown) => {
                const text =
                    err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
                expect(text.toLowerCase()).not.toContain("unhandled method sessions.connect");
                expect(text.toLowerCase()).toContain("session");
                return true;
            }
        );
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

function pathsEqual(left: string, right: string): boolean {
    return normalizePath(left) === normalizePath(right);
}

function normalizePath(value: string): string {
    return path
        .resolve(value)
        .replace(/[\\/]+$/g, "")
        .toLowerCase();
}
