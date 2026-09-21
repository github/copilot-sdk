/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ChildProcess } from "node:child_process";
import type { Socket } from "node:net";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import {
    assertHostStopped,
    assertRuntimeChild,
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
            await assertRuntimeChild(host, runtimeDetails().pid, artifacts);
            await Promise.all([host.dispose(), host.dispose()]);
            const exit = await withDeadline(host.closed, "disposed host notification");
            expect(exit.hostId).toBe(host.hostId);
            expect(exit.reason).toBe("disposed");
            await assertHostStopped(host, ahp);
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
            expect((await owner.listSessions()).map((item) => item.sessionId)).toContain(
                session.sessionId
            );
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

    it("cleans up a lost owner connection without stopping another owner's host or session", async () => {
        await using survivingSession = await owner.createSession({
            onPermissionRequest: approveAll,
        });
        await using survivingHost = await owner.startHost();
        const survivingAhp = await connectAhp(survivingHost);
        const otherOwner = new CopilotClient({
            connection: RuntimeConnection.forUri(`localhost:${runtimeDetails().port}`, {
                connectionToken,
            }),
        });
        try {
            await using abandonedHost = await otherOwner.startHost();
            const abandonedAhp = await connectAhp(abandonedHost);
            try {
                await assertRuntimeChild(abandonedHost, runtimeDetails().pid, artifacts);
                const socket = (otherOwner as unknown as { socket: Socket }).socket;
                expect(socket.destroyed).toBe(false);
                // Lose only this owner transport, without calling host.dispose or
                // client.stop, and without killing the shared runtime process.
                socket.destroy();
                const exit = await withDeadline(abandonedHost.closed, "owner connection loss");
                expect(exit.reason).toBe("ownerDisconnected");
                await assertHostStopped(abandonedHost, abandonedAhp);
                await survivingAhp.client.ping();
                await assertRuntimeChild(survivingHost, runtimeDetails().pid, artifacts);
                await expect(survivingSession.getEvents()).resolves.toEqual(expect.any(Array));
                await using additionalSession = await owner.createSession({
                    onPermissionRequest: approveAll,
                });
                expect(additionalSession.sessionId).not.toBe(survivingSession.sessionId);
            } finally {
                await abandonedAhp.client.shutdown();
            }
        } finally {
            await otherOwner.stop();
            await survivingAhp.client.shutdown();
        }
    });

    it("streams a standard AHP turn alongside an SDK session on the same runtime", async () => {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true for read-only canonical inference replay");
        }
        // Reuse the existing canonical 2+2 conversation through the existing
        // matcher. AHP and SDK both send this exact prompt/model; incompatible
        // requests still fail in replay-only mode rather than inventing replies.
        await ctx.openAiEndpoint.updateConfig({
            filePath: fileURLToPath(
                new URL(
                    "../../../test/snapshots/session/sendandwait_blocks_until_session_idle_and_returns_final_assistant_message.yaml",
                    import.meta.url
                )
            ),
            workDir: ctx.workDir,
        });
        await using sdkSession = await owner.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            streaming: true,
        });
        await using host = await owner.startHost();
        const ahp = await connectAhp(host);
        try {
            const session = await createAhpSession(ahp, ctx.workDir);
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
        } finally {
            await ahp.client.shutdown();
        }
    });
});
