/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "crypto";
import { mkdirSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { CopilotSession, SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("Session-scoped RPC", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    async function assertImplementedFailure(
        action: () => Promise<unknown>,
        method: string
    ): Promise<void> {
        await expect(action()).rejects.toSatisfy((err: unknown) => {
            const text = err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
            expect(text.toLowerCase()).not.toContain(`unhandled method ${method.toLowerCase()}`);
            return true;
        });
    }

    function getConversationMessages(events: SessionEvent[]): { role: string; content: string }[] {
        const messages: { role: string; content: string }[] = [];
        for (const evt of events) {
            if (evt.type === "user.message") {
                messages.push({ role: "user", content: evt.data.content });
            } else if (evt.type === "assistant.message") {
                messages.push({ role: "assistant", content: evt.data.content });
            }
        }
        return messages;
    }

    it("should call session rpc model getcurrent", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });

        const result = await session.rpc.model.getCurrent();
        expect(result.modelId).toBeTruthy();

        await session.disconnect();
    });

    it("should call session rpc model switchto", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });

        const before = await session.rpc.model.getCurrent();
        expect(before.modelId).toBeTruthy();

        const result = await session.rpc.model.switchTo({
            modelId: "gpt-4.1",
            reasoningEffort: "high",
        });
        await waitForCondition(
            async () => (await session.rpc.model.getCurrent()).modelId === "gpt-4.1",
            { timeoutMessage: "session.model.getCurrent did not reflect switchTo" }
        );
        const after = await session.rpc.model.getCurrent();

        expect(result.modelId).toBe("gpt-4.1");
        expect(after.modelId).toBe("gpt-4.1");

        await session.disconnect();
    });

    it("should shutdown session with routine type", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const shutdownEvent = waitForEvent(
            session,
            (event): event is Extract<SessionEvent, { type: "session.shutdown" }> =>
                event.type === "session.shutdown" && event.data.shutdownType === "routine",
            "session.shutdown routine event"
        );

        await session.rpc.shutdown({
            type: "routine",
            reason: "SDK E2E shutdown coverage",
        });

        expect((await shutdownEvent).data.shutdownType).toBe("routine");
    });

    it("should set and get each session mode value", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            for (const mode of ["interactive", "plan", "autopilot"] as const) {
                await session.rpc.mode.set({ mode });
                expect(await session.rpc.mode.get()).toBe(mode);
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should get and set session mode", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initial = await session.rpc.mode.get();
        expect(initial).toBe("interactive");

        await session.rpc.mode.set({ mode: "plan" });
        expect(await session.rpc.mode.get()).toBe("plan");

        await session.rpc.mode.set({ mode: "interactive" });
        expect(await session.rpc.mode.get()).toBe("interactive");

        await session.disconnect();
    });

    it("should read update and delete plan", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initial = await session.rpc.plan.read();
        expect(initial.exists).toBe(false);
        expect(initial.content).toBeFalsy();

        const planContent = "# Test Plan\n\n- Step 1\n- Step 2";
        await session.rpc.plan.update({ content: planContent });

        const afterUpdate = await session.rpc.plan.read();
        expect(afterUpdate.exists).toBe(true);
        expect(afterUpdate.content).toBe(planContent);

        await session.rpc.plan.delete();

        const afterDelete = await session.rpc.plan.read();
        expect(afterDelete.exists).toBe(false);
        expect(afterDelete.content).toBeFalsy();

        await session.disconnect();
    });

    it("should call workspace file rpc methods", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initial = await session.rpc.workspaces.listFiles();
        expect(initial.files).toBeDefined();

        await session.rpc.workspaces.createFile({
            path: "test.txt",
            content: "Hello, workspace!",
        });

        const afterCreate = await session.rpc.workspaces.listFiles();
        expect(afterCreate.files).toContain("test.txt");

        const file = await session.rpc.workspaces.readFile({ path: "test.txt" });
        expect(file.content).toBe("Hello, workspace!");

        const workspace = await session.rpc.workspaces.getWorkspace();
        expect(workspace.workspace).toBeDefined();
        expect(workspace.workspace.id).toBeTruthy();

        await session.disconnect();
    });

    it.each(["../escaped.txt", "../../escaped.txt", "nested/../../../escaped.txt"])(
        "should reject workspace file path traversal: %s",
        async (filePath) => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                await expect(
                    session.rpc.workspaces.createFile({
                        path: filePath,
                        content: "should not land outside workspace",
                    })
                ).rejects.toThrow(/workspace files directory/i);

                await expect(session.rpc.workspaces.readFile({ path: filePath })).rejects.toThrow(
                    /workspace files directory/i
                );
            } finally {
                await session.disconnect();
            }
        }
    );

    it("should create workspace file with nested path auto-creating dirs", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const nestedPath = `nested-${randomUUID()}/subdir/file.txt`;
            await session.rpc.workspaces.createFile({
                path: nestedPath,
                content: "nested content",
            });

            expect((await session.rpc.workspaces.readFile({ path: nestedPath })).content).toBe(
                "nested content"
            );
            expect(
                (await session.rpc.workspaces.listFiles()).files.some((f) => f.endsWith("file.txt"))
            ).toBe(true);
        } finally {
            await session.disconnect();
        }
    });

    it("should report error reading nonexistent workspace file", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await expect(
                session.rpc.workspaces.readFile({
                    path: `never-exists-${randomUUID()}.txt`,
                })
            ).rejects.toThrow();
        } finally {
            await session.disconnect();
        }
    });

    it("should update existing workspace file with update operation", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const filePath = `reused-${randomUUID()}.txt`;
            await session.rpc.workspaces.createFile({ path: filePath, content: "v1" });

            const updated = waitForEvent(
                session,
                (
                    event
                ): event is Extract<SessionEvent, { type: "session.workspace_file_changed" }> =>
                    event.type === "session.workspace_file_changed" &&
                    event.data.path === filePath &&
                    event.data.operation === "update",
                `workspace_file_changed update event for ${filePath}`
            );
            await session.rpc.workspaces.createFile({ path: filePath, content: "v2" });

            expect((await updated).data.operation).toBe("update");
            expect((await session.rpc.workspaces.readFile({ path: filePath })).content).toBe("v2");
        } finally {
            await session.disconnect();
        }
    });

    it.each(["", "   ", "\t\n  \r"])(
        "should reject empty or whitespace session name",
        async (emptyOrWhitespace) => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                await expect(session.rpc.name.set({ name: emptyOrWhitespace })).rejects.toThrow(
                    /empty/i
                );
            } finally {
                await session.disconnect();
            }
        }
    );

    it("should emit title changed event each time name set is called", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const titleA = `Title-A-${randomUUID()}`;
            const titleB = `Title-B-${randomUUID()}`;

            const first = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.title_changed" }> =>
                    event.type === "session.title_changed" && event.data.title === titleA,
                "first title_changed event"
            );
            await session.rpc.name.set({ name: titleA });
            expect((await first).data.title).toBe(titleA);

            const second = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.title_changed" }> =>
                    event.type === "session.title_changed" && event.data.title === titleB,
                "second title_changed event"
            );
            await session.rpc.name.set({ name: titleB });
            expect((await second).data.title).toBe(titleB);
        } finally {
            await session.disconnect();
        }
    });

    it("should get and set session metadata", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.rpc.name.set({ name: "SDK test session" });
        const name = await session.rpc.name.get();
        expect(name.name).toBe("SDK test session");

        const sources = await session.rpc.instructions.getSources();
        expect(sources.sources).toBeDefined();

        await session.disconnect();
    });

    it("should call metadata snapshot, setWorkingDirectory, and recordContextChange", async () => {
        const firstDirectory = createUniqueDirectory(workDir, "rpc-session-state-first");
        const secondDirectory = createUniqueDirectory(workDir, "rpc-session-state-second");
        const contextDirectory = createUniqueDirectory(workDir, "rpc-session-state-context");
        const branch = `rpc-context-${randomUUID()}`;
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
            workingDirectory: firstDirectory,
        });
        try {
            const initialSnapshot = await session.rpc.metadata.snapshot();
            expect(initialSnapshot.sessionId).toBe(session.sessionId);
            expect(initialSnapshot.currentMode).toBe("interactive");
            expect(initialSnapshot.selectedModel).toBe("claude-sonnet-4.5");
            expect(initialSnapshot.isRemote).toBe(false);
            expect(initialSnapshot.alreadyInUse).toBe(false);
            expect(Date.parse(initialSnapshot.startTime)).not.toBeNaN();
            expect(Date.parse(initialSnapshot.modifiedTime)).not.toBeNaN();
            expect(pathsEqual(initialSnapshot.workingDirectory, firstDirectory)).toBe(true);
            expect(initialSnapshot.workspace?.id).toBe(session.sessionId);
            expect(initialSnapshot.workspacePath?.trim()).toBeTruthy();

            const setWorkingDirectory = await session.rpc.metadata.setWorkingDirectory({
                workingDirectory: secondDirectory,
            });
            expect(pathsEqual(setWorkingDirectory.workingDirectory, secondDirectory)).toBe(true);

            await waitForCondition(
                async () =>
                    pathsEqual(
                        (await session.rpc.metadata.snapshot()).workingDirectory,
                        secondDirectory
                    ),
                { timeoutMessage: "Timed out waiting for metadata snapshot to reflect cwd." }
            );

            const contextChanged = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.context_changed" }> =>
                    event.type === "session.context_changed" && event.data.branch === branch,
                "session.context_changed event"
            );

            const context = {
                cwd: contextDirectory,
                gitRoot: firstDirectory,
                branch,
                repository: "github/copilot-sdk-e2e",
                repositoryHost: "github.com",
                hostType: "github" as const,
                baseCommit: "0000000000000000000000000000000000000000",
                headCommit: "1111111111111111111111111111111111111111",
            };
            await session.rpc.metadata.recordContextChange({ context });

            const event = await contextChanged;
            expect(pathsEqual(event.data.cwd, contextDirectory)).toBe(true);
            expect(pathsEqual(event.data.gitRoot ?? "", firstDirectory)).toBe(true);
            expect(event.data.branch).toBe(branch);
            expect(event.data.repository).toBe("github/copilot-sdk-e2e");
            expect(event.data.repositoryHost).toBe("github.com");
            expect(event.data.hostType).toBe("github");
            expect(event.data.baseCommit).toBe(context.baseCommit);
            expect(event.data.headCommit).toBe(context.headCommit);
        } finally {
            await session.disconnect();
        }
    });

    it("should update options and initialize session services", async () => {
        const initialDirectory = createUniqueDirectory(workDir, "rpc-options-initial");
        const optionsDirectory = createUniqueDirectory(workDir, "rpc-options-updated");
        const featureName = `rpc-session-state-${randomUUID()}`;
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            workingDirectory: initialDirectory,
        });
        try {
            const update = await session.rpc.options.update({
                clientName: "node-sdk-rpc-session-state-e2e",
                lspClientName: "node-sdk-rpc-session-state-lsp",
                integrationId: `node-sdk-${randomUUID()}`,
                featureFlags: { [featureName]: true },
                workingDirectory: optionsDirectory,
                coauthorEnabled: false,
                enableStreaming: false,
                askUserDisabled: true,
            });
            expect(update.success).toBe(true);

            await waitForCondition(
                async () =>
                    pathsEqual(
                        (await session.rpc.metadata.snapshot()).workingDirectory,
                        optionsDirectory
                    ),
                {
                    timeoutMessage:
                        "Timed out waiting for options.update workingDirectory to reach metadata snapshot.",
                }
            );

            await expect(
                session.rpc.lsp.initialize({
                    workingDirectory: optionsDirectory,
                    gitRoot: initialDirectory,
                    force: true,
                })
            ).resolves.toBeNull();

            await expect(
                session.rpc.telemetry.setFeatureOverrides({
                    features: {
                        rpc_session_state_feature: featureName,
                        rpc_session_state_value: "enabled",
                    },
                })
            ).resolves.toBeNull();

            await expect(session.rpc.tools.initializeAndValidate()).resolves.toBeDefined();
            expect(
                pathsEqual(
                    (await session.rpc.metadata.snapshot()).workingDirectory,
                    optionsDirectory
                )
            ).toBe(true);
        } finally {
            await session.disconnect();
        }
    });

    it("should set reasoning effort and auto name", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });
        try {
            const reasoning = await session.rpc.model.setReasoningEffort({
                reasoningEffort: "high",
            });
            expect(reasoning.reasoningEffort).toBe("high");

            const currentModel = await session.rpc.model.getCurrent();
            expect(currentModel.modelId).toBe("claude-sonnet-4.5");
            expect(currentModel.reasoningEffort).toBe("high");

            const autoName = `Auto Session ${randomUUID()}`;
            const autoChanged = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.title_changed" }> =>
                    event.type === "session.title_changed" && event.data.title === autoName,
                "session.title_changed event after name.setAuto"
            );
            const autoResult = await session.rpc.name.setAuto({ summary: `  ${autoName}  ` });
            expect(autoResult.applied).toBe(true);
            expect((await autoChanged).data.title).toBe(autoName);
            expect((await session.rpc.name.get()).name).toBe(autoName);

            const explicitName = `Explicit Session ${randomUUID()}`;
            const explicitChanged = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.title_changed" }> =>
                    event.type === "session.title_changed" && event.data.title === explicitName,
                "session.title_changed event after explicit name.set"
            );
            await session.rpc.name.set({ name: explicitName });
            expect((await explicitChanged).data.title).toBe(explicitName);

            const ignoredAutoResult = await session.rpc.name.setAuto({
                summary: `Ignored ${randomUUID()}`,
            });
            expect(ignoredAutoResult.applied).toBe(false);
            expect((await session.rpc.name.get()).name).toBe(explicitName);
        } finally {
            await session.disconnect();
        }
    });

    it("should set auth credentials", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const login = `sdk-rpc-${randomUUID()}`;
            const setCredentials = await session.rpc.auth.setCredentials({
                credentials: {
                    type: "user",
                    host: "https://github.com",
                    login,
                    copilotUser: {
                        analytics_tracking_id: "rpc-session-state-tracking-id",
                        chat_enabled: true,
                        copilot_plan: "individual_pro",
                        endpoints: {
                            api: "https://api.githubcopilot.test",
                            telemetry: "https://localhost:1/telemetry",
                        },
                        login,
                    },
                },
            });
            expect(setCredentials.success).toBe(true);

            const status = await session.rpc.auth.getStatus();
            expect(status.isAuthenticated).toBe(true);
            expect(status.authType).toBe("user");
            expect(status.host).toBe("https://github.com");
            expect(status.login).toBe(login);
        } finally {
            await session.disconnect();
        }
    });

    it("should fork session with persisted messages", async () => {
        const sourcePrompt = "Say FORK_SOURCE_ALPHA exactly.";
        const forkPrompt = "Now say FORK_CHILD_BETA exactly.";

        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initialAnswer = await session.sendAndWait({ prompt: sourcePrompt });
        expect(initialAnswer?.data.content ?? "").toContain("FORK_SOURCE_ALPHA");

        const sourceConversation = getConversationMessages(await session.getEvents());
        expect(
            sourceConversation.some((m) => m.role === "user" && m.content === sourcePrompt)
        ).toBe(true);
        expect(
            sourceConversation.some(
                (m) => m.role === "assistant" && m.content.includes("FORK_SOURCE_ALPHA")
            )
        ).toBe(true);

        const fork = await client.rpc.sessions.fork({ sessionId: session.sessionId });
        expect(fork.sessionId).toBeTruthy();
        expect(fork.sessionId).not.toBe(session.sessionId);

        const forkedSession = await client.resumeSession(fork.sessionId, {
            onPermissionRequest: approveAll,
        });
        const forkedConversation = getConversationMessages(await forkedSession.getEvents());
        expect(forkedConversation.slice(0, sourceConversation.length)).toEqual(sourceConversation);

        const forkAnswer = await forkedSession.sendAndWait({ prompt: forkPrompt });
        expect(forkAnswer?.data.content ?? "").toContain("FORK_CHILD_BETA");

        const sourceAfterFork = getConversationMessages(await session.getEvents());
        expect(sourceAfterFork.some((m) => m.content === forkPrompt)).toBe(false);

        const forkAfterPrompt = getConversationMessages(await forkedSession.getEvents());
        expect(forkAfterPrompt.some((m) => m.role === "user" && m.content === forkPrompt)).toBe(
            true
        );
        expect(
            forkAfterPrompt.some(
                (m) => m.role === "assistant" && m.content.includes("FORK_CHILD_BETA")
            )
        ).toBe(true);

        await forkedSession.disconnect();
        await session.disconnect();
    });

    it("should handle forking session without persisted events", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            let fork: Awaited<ReturnType<typeof client.rpc.sessions.fork>>;
            try {
                fork = await client.rpc.sessions.fork({ sessionId: session.sessionId });
            } catch (err: unknown) {
                const text =
                    err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
                expect(text.toLowerCase()).toContain("not found or has no persisted events");
                expect(text.toLowerCase()).not.toContain("unhandled method sessions.fork");
                return;
            }

            expect(fork.sessionId.trim()).toBeTruthy();
            expect(fork.sessionId).not.toBe(session.sessionId);

            const forkedSession = await client.resumeSession(fork.sessionId, {
                onPermissionRequest: approveAll,
            });
            try {
                expect(getConversationMessages(await forkedSession.getEvents())).toEqual([]);
            } finally {
                await forkedSession.disconnect();
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should fork session to event id excluding boundary event", async () => {
        const firstPrompt = "Say FORK_BOUNDARY_FIRST exactly.";
        const secondPrompt = "Say FORK_BOUNDARY_SECOND exactly.";

        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.sendAndWait({ prompt: firstPrompt });
            await session.sendAndWait({ prompt: secondPrompt });

            const sourceEvents = await session.getEvents();
            const secondUserEvent = sourceEvents.find(
                (event) => event.type === "user.message" && event.data.content === secondPrompt
            );
            expect(secondUserEvent).toBeDefined();
            const boundaryEventId = secondUserEvent!.id;

            const fork = await client.rpc.sessions.fork({
                sessionId: session.sessionId,
                toEventId: boundaryEventId,
            });
            expect(fork.sessionId.trim()).toBeTruthy();
            expect(fork.sessionId).not.toBe(session.sessionId);

            const forkedSession = await client.resumeSession(fork.sessionId, {
                onPermissionRequest: approveAll,
            });
            try {
                const forkedEvents = await forkedSession.getEvents();
                expect(forkedEvents.some((event) => event.id === boundaryEventId)).toBe(false);

                const forkedConversation = getConversationMessages(forkedEvents);
                expect(
                    forkedConversation.some((m) => m.role === "user" && m.content === firstPrompt)
                ).toBe(true);
                expect(
                    forkedConversation.some((m) => m.role === "user" && m.content === secondPrompt)
                ).toBe(false);
            } finally {
                await forkedSession.disconnect();
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should report error when forking session to unknown event id", async () => {
        const sourcePrompt = "Say FORK_UNKNOWN_EVENT_OK exactly.";
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.sendAndWait({ prompt: sourcePrompt });

            const bogusEventId = randomUUID();
            await expect(
                client.rpc.sessions.fork({
                    sessionId: session.sessionId,
                    toEventId: bogusEventId,
                })
            ).rejects.toSatisfy((err: unknown) => {
                const text =
                    err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
                expect(text.toLowerCase()).toContain(`event ${bogusEventId} not found`);
                expect(text.toLowerCase()).not.toContain("unhandled method sessions.fork");
                return true;
            });
        } finally {
            await session.disconnect();
        }
    });

    it("should call session usage and permission rpcs", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const metrics = await session.rpc.usage.getMetrics();
        expect(Date.parse(metrics.sessionStartTime)).not.toBeNaN();
        if (metrics.totalNanoAiu !== undefined && metrics.totalNanoAiu !== null) {
            expect(metrics.totalNanoAiu).toBeGreaterThanOrEqual(0);
        }
        if (metrics.tokenDetails) {
            for (const detail of Object.values(metrics.tokenDetails)) {
                expect(detail.tokenCount).toBeGreaterThanOrEqual(0);
            }
        }
        for (const modelMetric of Object.values(metrics.modelMetrics)) {
            if (modelMetric.totalNanoAiu !== undefined && modelMetric.totalNanoAiu !== null) {
                expect(modelMetric.totalNanoAiu).toBeGreaterThanOrEqual(0);
            }
            if (modelMetric.tokenDetails) {
                for (const detail of Object.values(modelMetric.tokenDetails)) {
                    expect(detail.tokenCount).toBeGreaterThanOrEqual(0);
                }
            }
        }

        try {
            const approve = await session.rpc.permissions.setApproveAll({ enabled: true });
            expect(approve.success).toBe(true);

            const reset = await session.rpc.permissions.resetSessionApprovals();
            expect(reset.success).toBe(true);
        } finally {
            await session.rpc.permissions.setApproveAll({ enabled: false });
        }

        await session.disconnect();
    });

    it("should report implemented errors for unsupported session rpc paths", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await assertImplementedFailure(
            () => session.rpc.history.truncate({ eventId: "missing-event" }),
            "session.history.truncate"
        );

        await assertImplementedFailure(
            () => session.rpc.mcp.oauth.login({ serverName: "missing-server" }),
            "session.mcp.oauth.login"
        );

        await session.disconnect();
    });

    it("should compact session history after messages", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        expect((await session.rpc.metadata.isProcessing()).processing).toBe(false);
        await session.sendAndWait({ prompt: "What is 2+2?" });
        expect((await session.rpc.metadata.isProcessing()).processing).toBe(false);

        const contextInfo = await session.rpc.metadata.contextInfo({
            promptTokenLimit: 128_000,
            outputTokenLimit: 4_096,
            selectedModel: "claude-sonnet-4.5",
        });
        expect(contextInfo.contextInfo).not.toBeNull();
        if (contextInfo.contextInfo) {
            expect(contextInfo.contextInfo.modelName).toBe("claude-sonnet-4.5");
            expect(contextInfo.contextInfo.promptTokenLimit).toBe(128_000);
            expect(contextInfo.contextInfo.limit).toBeGreaterThanOrEqual(
                contextInfo.contextInfo.promptTokenLimit
            );
            expect(contextInfo.contextInfo.totalTokens).toBeGreaterThan(0);
            expect(contextInfo.contextInfo.systemTokens).toBeGreaterThan(0);
            expect(contextInfo.contextInfo.conversationTokens).toBeGreaterThan(0);
            expect(contextInfo.contextInfo.toolDefinitionsTokens).toBeGreaterThanOrEqual(0);
            expect(contextInfo.contextInfo.totalTokens).toBe(
                contextInfo.contextInfo.systemTokens +
                    contextInfo.contextInfo.conversationTokens +
                    contextInfo.contextInfo.toolDefinitionsTokens
            );
        }

        const recomputed = await session.rpc.metadata.recomputeContextTokens({
            modelId: "claude-sonnet-4.5",
        });
        expect(recomputed.systemTokenCount).toBeGreaterThan(0);
        expect(recomputed.messagesTokenCount).toBeGreaterThan(0);
        expect(recomputed.totalTokens).toBe(
            recomputed.systemTokenCount + recomputed.messagesTokenCount
        );

        const result = await session.rpc.history.compact();
        expect(result.success).toBe(true);
        expect(result.messagesRemoved).toBeGreaterThanOrEqual(0);
        if (result.contextWindow) {
            expect(result.contextWindow.messagesLength).toBeGreaterThanOrEqual(0);
            expect(result.contextWindow.currentTokens).toBeGreaterThanOrEqual(0);
            if (result.contextWindow.conversationTokens != null) {
                expect(result.contextWindow.conversationTokens).toBeGreaterThanOrEqual(0);
                expect(result.contextWindow.conversationTokens).toBeLessThanOrEqual(
                    result.contextWindow.currentTokens
                );
            }
        }
        expect(await session.rpc.name.get()).toBeDefined();

        await session.disconnect();
    });
});

function createUniqueDirectory(baseDir: string, prefix: string): string {
    const directory = join(baseDir, `${prefix}-${randomUUID()}`);
    mkdirSync(directory, { recursive: true });
    return directory;
}

function pathsEqual(left: string, right: string): boolean {
    return normalizePath(left) === normalizePath(right);
}

function normalizePath(value: string): string {
    return value.replace(/[\\/]+$/g, "").toLowerCase();
}

function waitForEvent<T extends SessionEvent>(
    session: CopilotSession,
    predicate: (event: SessionEvent) => event is T,
    description: string,
    timeoutMs = 15_000
): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        let unsubscribe: () => void = () => {};
        const timeout = setTimeout(() => {
            unsubscribe();
            reject(new Error(`Timed out waiting for ${description}`));
        }, timeoutMs);

        unsubscribe = session.on((event) => {
            if (predicate(event)) {
                clearTimeout(timeout);
                unsubscribe();
                resolve(event);
            } else if (event.type === "session.error") {
                clearTimeout(timeout);
                unsubscribe();
                reject(new Error(`${event.data.message}\n${event.data.stack ?? ""}`));
            }
        });
    });
}
