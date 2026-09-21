/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ChildProcess } from "node:child_process";
import { mkdir } from "node:fs/promises";
import type { Socket } from "node:net";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import {
    ResponsePartKind,
    TurnState,
    type ChatState,
    type SessionState,
} from "@microsoft/agent-host-protocol";
import { describe, expect, it } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import {
    assertHostStopped,
    assertProcessStopped,
    assertRuntimeChild,
    authenticateAhp,
    connectAhp,
    createAhpSession,
    localHostArtifacts,
    streamedTurn,
    withDeadline,
} from "./harness/runtimeHost.js";

const enabled = process.env.COPILOT_RUNTIME_HOST_E2E === "1";

// Linux /proc lets this opt-in source-build suite verify executable identity as
// well as process ancestry. No released CLI or runtime package may substitute.
describe.skipIf(!enabled)("Runtime-supervised AHP host", async () => {
    if (!enabled) return;
    if (process.platform !== "linux") throw new Error("Runtime host topology E2Es require Linux");
    const artifacts = localHostArtifacts();
    const connectionToken = "runtime-host-e2e-runtime-connection";
    const ctx = await createSdkTestContext({
        copilotClientOptions: {
            connection: RuntimeConnection.forTcp({
                path: artifacts.runtimePath,
                connectionToken,
            }),
            env: artifacts.env,
        },
    });
    const owner = ctx.copilotClient;
    const catalogPath = join(ctx.env.COPILOT_HOME, "ahp", "sessions");

    async function configureReplay() {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true for read-only canonical inference replay");
        }
        await ctx.openAiEndpoint.updateConfig({
            filePath: fileURLToPath(
                new URL(
                    "../../../test/snapshots/session/sendandwait_blocks_until_session_idle_and_returns_final_assistant_message.yaml",
                    import.meta.url
                )
            ),
            workDir: ctx.workDir,
        });
    }

    async function resumeAhp(
        ahp: Awaited<ReturnType<typeof connectAhp>>,
        sessionUri: string,
        excludedSdkSessionId?: string
    ) {
        await authenticateAhp(ahp, ctx.env.GITHUB_TOKEN);
        const listed = await ahp.client.request("listSessions", { channel: "ahp-root://" });
        const resources = listed.items.map((item) => item.resource);
        expect(resources).toContain(sessionUri);
        if (excludedSdkSessionId) {
            expect(resources).not.toContain(`ahp-session:/${excludedSdkSessionId}`);
        }
        // Subscribing a dormant catalog URI is the standard AHP resume path.
        const { result } = await ahp.client.subscribe(sessionUri);
        const session = result.snapshot?.state as SessionState | undefined;
        expect(session?.lifecycle).toBe("ready");
        expect(session?.defaultChat).toBeTruthy();
        const chat = await ahp.client.subscribe(session!.defaultChat!);
        expect(chat.result.snapshot).toBeDefined();
        const state = chat.result.snapshot?.state as ChatState;
        const turn = state.turns.find((entry) => entry.message.text === "What is 2+2?");
        expect(turn?.state).toBe(TurnState.Complete);
        expect(
            turn?.responseParts
                .filter((part) => part.kind === ResponsePartKind.Markdown)
                .map((part) => part.content)
                .join("")
        ).toContain("4");
        return state;
    }

    function runtimeDetails() {
        const internals = owner as unknown as {
            runtimePort: number;
            cliProcess: ChildProcess;
        };
        const pid = internals.cliProcess.pid;
        expect(pid).toBeGreaterThan(0);
        return { pid: pid!, port: internals.runtimePort };
    }

    it("disposes listener, client and runtime child without closing the owner session", async () => {
        await using session = await owner.createSession({ onPermissionRequest: approveAll });
        await using host = await owner.startHost();
        const ahp = await connectAhp(host);
        try {
            await ahp.client.ping();
            await assertRuntimeChild(host, runtimeDetails().pid, artifacts, catalogPath);
            await Promise.all([host.dispose(), host.dispose()]);
            const exit = await withDeadline(host.closed, "disposed host notification");
            expect(exit.hostId).toBe(host.hostId);
            expect(exit.reason).toBe("disposed");
            await assertHostStopped(host, ahp);
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
            // Empty sessions are not persisted/listed until their first turn.
            await using additionalSession = await owner.createSession({
                onPermissionRequest: approveAll,
            });
            expect(additionalSession.sessionId).not.toBe(session.sessionId);
            await host.dispose();
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("reports unexpected child exit while the owner session remains usable", async () => {
        await using session = await owner.createSession({ onPermissionRequest: approveAll });
        await using host = await owner.startHost();
        const ahp = await connectAhp(host);
        try {
            await assertRuntimeChild(host, runtimeDetails().pid, artifacts);
            process.kill(host.pid, "SIGKILL");
            const exit = await withDeadline(host.closed, "unexpected host exit notification");
            expect(exit.hostId).toBe(host.hostId);
            expect(exit.reason).toBe("exited");
            await assertHostStopped(host, ahp);
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("rejects a second same-home host and releases ownership on disconnect without stopping SDK sessions", async () => {
        await configureReplay();
        await using survivingSession = await owner.createSession({
            onPermissionRequest: approveAll,
        });
        const otherOwner = new CopilotClient({
            connection: RuntimeConnection.forUri(`localhost:${runtimeDetails().port}`, {
                connectionToken,
            }),
        });
        try {
            await using abandonedHost = await otherOwner.startHost();
            const abandonedAhp = await connectAhp(abandonedHost);
            try {
                const session = await createAhpSession(
                    abandonedAhp,
                    ctx.workDir,
                    ctx.env.GITHUB_TOKEN
                );
                const response = await streamedTurn(
                    abandonedAhp.client,
                    session.chatUri,
                    session.subscription,
                    "What is 2+2?"
                );
                expect(response.text).toContain("4");
                await assertRuntimeChild(
                    abandonedHost,
                    runtimeDetails().pid,
                    artifacts,
                    catalogPath
                );
                await expect(owner.startHost()).rejects.toThrow("catalog already in use");
                await abandonedAhp.client.ping();
                await expect(survivingSession.getEvents()).resolves.toEqual(expect.any(Array));
                const socket = (otherOwner as unknown as { socket: Socket }).socket;
                expect(socket.destroyed).toBe(false);
                // Lose only this owner transport, without calling host.dispose or
                // client.stop, and without killing the shared runtime process.
                socket.destroy();
                const exit = await withDeadline(abandonedHost.closed, "owner connection loss");
                expect(exit.reason).toBe("ownerDisconnected");
                await assertHostStopped(abandonedHost, abandonedAhp);
                await expect(survivingSession.getEvents()).resolves.toEqual(expect.any(Array));
                await using replacement = await owner.startHost();
                const replacementAhp = await connectAhp(replacement);
                try {
                    await resumeAhp(replacementAhp, session.sessionUri, survivingSession.sessionId);
                } finally {
                    await replacementAhp.client.shutdown();
                }
                await using additionalSession = await owner.createSession({
                    onPermissionRequest: approveAll,
                });
                expect(additionalSession.sessionId).not.toBe(survivingSession.sessionId);
            } finally {
                await abandonedAhp.client.shutdown();
            }
        } finally {
            await otherOwner.stop();
        }
    });

    it("streams beside an SDK session and recovers its catalog after disposal and forced child death", async () => {
        // Reuse the existing canonical 2+2 conversation through the existing
        // matcher. AHP and SDK both send this exact prompt/model; incompatible
        // requests still fail in replay-only mode rather than inventing replies.
        await configureReplay();
        await using sdkSession = await owner.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            streaming: true,
        });
        await using host = await owner.startHost();
        const ahp = await connectAhp(host);
        try {
            const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            await assertRuntimeChild(host, runtimeDetails().pid, artifacts);
            const [response, sdkResponse] = await Promise.all([
                streamedTurn(ahp.client, session.chatUri, session.subscription, "What is 2+2?"),
                sdkSession.sendAndWait({ prompt: "What is 2+2?" }),
            ]);
            expect(response.text).toContain("4");
            expect(response.deltas).toBeGreaterThan(0);
            expect(sdkResponse?.data.content).toContain("4");

            const runtimeSessions = (await owner.listSessions()).map((item) => item.sessionId);
            expect(runtimeSessions).toContain(sdkSession.sessionId);
            expect(runtimeSessions).toContain(session.sessionId);
            await using observer = await owner.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
            });
            expect(
                (await observer.getEvents()).some(
                    (event) =>
                        event.type === "assistant.message" && event.data.content.includes("4")
                )
            ).toBe(true);
            await host.dispose();
            await assertHostStopped(host, ahp);
            expect(
                (await sdkSession.getEvents()).some((event) => event.type === "assistant.message")
            ).toBe(true);

            await using replacement = await owner.startHost();
            const replacementAhp = await connectAhp(replacement);
            try {
                const resumed = await resumeAhp(
                    replacementAhp,
                    session.sessionUri,
                    sdkSession.sessionId
                );
                expect(resumed.turns.length).toBeGreaterThan(0);
                process.kill(replacement.pid, "SIGKILL");
                expect(
                    (await withDeadline(replacement.closed, "forced catalog owner exit")).reason
                ).toBe("exited");
                await assertHostStopped(replacement, replacementAhp);
            } finally {
                await replacementAhp.client.shutdown();
            }
            await using recovered = await owner.startHost();
            const recoveredAhp = await connectAhp(recovered);
            try {
                const resumed = await resumeAhp(
                    recoveredAhp,
                    session.sessionUri,
                    sdkSession.sessionId
                );
                expect(resumed.turns.length).toBeGreaterThan(0);
                await expect(sdkSession.getEvents()).resolves.toEqual(expect.any(Array));
            } finally {
                await recoveredAhp.client.shutdown();
            }
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("uses SDK baseDirectory for a durable catalog across runtime restart and rejects another runtime's writer", async () => {
        await configureReplay();
        const baseDirectory = join(ctx.env.COPILOT_HOME, "explicit-base");
        await mkdir(baseDirectory, { recursive: true });
        const first = ctx.createClient({ baseDirectory });
        const second = ctx.createClient({ baseDirectory });
        let sessionUri: string;
        try {
            await using host = await first.startHost();
            const ahp = await connectAhp(host);
            try {
                const runtime = (first as unknown as { cliProcess: ChildProcess }).cliProcess;
                await assertRuntimeChild(
                    host,
                    runtime.pid!,
                    artifacts,
                    join(baseDirectory, "ahp", "sessions")
                );
                const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
                sessionUri = session.sessionUri;
                const response = await streamedTurn(
                    ahp.client,
                    session.chatUri,
                    session.subscription,
                    "What is 2+2?"
                );
                expect(response.text).toContain("4");
                // Independent ordinary SDK runtimes may use this home; only a
                // second AHP server is excluded from the catalog.
                await using ordinary = await second.createSession({
                    onPermissionRequest: approveAll,
                });
                await expect(second.startHost()).rejects.toThrow("catalog already in use");
                await expect(ordinary.getEvents()).resolves.toEqual(expect.any(Array));
                await ahp.client.ping();
                await host.dispose();
                await assertHostStopped(host, ahp);
            } finally {
                await ahp.client.shutdown();
            }
        } finally {
            await first.stop();
            await second.stop();
        }

        const restarted = ctx.createClient({ baseDirectory });
        try {
            await using host = await restarted.startHost();
            const ahp = await connectAhp(host);
            try {
                const resumed = await resumeAhp(ahp, sessionUri!);
                expect(resumed.turns.length).toBeGreaterThan(0);
            } finally {
                await ahp.client.shutdown();
            }
        } finally {
            await restarted.stop();
        }
    });

    it("gracefully shuts down the runtime with an attached AHP session", async () => {
        await using host = await owner.startHost();
        const ahp = await connectAhp(host);
        const runtimePid = runtimeDetails().pid;
        try {
            await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            await assertRuntimeChild(host, runtimePid, artifacts);

            // Prove the actual shutdown RPC succeeds before allowing SDK stop
            // to reap its process. Eventual forced cleanup is not success.
            await withDeadline(owner.rpc.runtime.shutdown(), "runtime shutdown response");
            const exit = await withDeadline(host.closed, "runtime shutdown host notification");
            expect(exit.hostId).toBe(host.hostId);
            expect(exit.reason).toBe("runtimeShutdown");
            expect(exit.error).toBeUndefined();
            expect(exit.exitCode).toBe(0);
            await assertHostStopped(host, ahp);

            await owner.stop();
            await assertProcessStopped(runtimePid, "SDK-owned runtime");
        } finally {
            await ahp.client.shutdown();
        }
    });
});
