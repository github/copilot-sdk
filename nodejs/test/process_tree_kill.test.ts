/**
 * Tests for process-tree termination on stop()/forceStop().
 *
 * Each case spawns a real process tree (a runtime stand-in that forks a
 * long-lived grandchild), hands it to a CopilotClient as its owned runtime,
 * and drives the public teardown methods. The assertions are on the SDK's
 * behaviour, so removing the tree termination fails these tests.
 *
 * @see https://github.com/github/copilot-sdk/issues/1804
 */
import { describe, expect, it } from "vitest";
import { spawn, type ChildProcess } from "node:child_process";
import { platform } from "node:os";
import { CopilotClient, RuntimeConnection } from "../src/index.js";

const isWindows = platform() === "win32";

function isProcessAlive(pid: number): boolean {
    try {
        process.kill(pid, 0);
        return true;
    } catch {
        return false;
    }
}

function sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Poll until the pid is gone, so the assertion does not race the OS. */
async function waitForExit(pid: number, timeoutMs = 10000): Promise<boolean> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (!isProcessAlive(pid)) {
            return true;
        }
        await sleep(50);
    }
    return !isProcessAlive(pid);
}

/**
 * Spawn a runtime stand-in that forks a long-lived grandchild, matching the
 * SDK's own spawn flags so the group/tree shape is the real one.
 *
 * The grandchild is detached on Windows and left in the group on POSIX. Either
 * way it outlives a kill aimed only at the root, which is what makes these
 * tests fail if tree termination regresses to terminating the root.
 *
 * @param ignoreSigterm - make the grandchild survive SIGTERM, reproducing the
 * descendant that outlived teardown in the linked issue.
 */
function spawnRuntimeStandIn(ignoreSigterm = false): {
    parent: ChildProcess;
    grandchildPid: Promise<number>;
} {
    const grandchildBody = ignoreSigterm
        ? `process.on("SIGTERM", () => {}); process.stdout.write("ready"); setTimeout(() => {}, 120000);`
        : `process.stdout.write("ready"); setTimeout(() => {}, 120000);`;
    // The grandchild only reports its pid once it is running, otherwise a signal
    // can land before its SIGTERM handler is installed and it dies by default.
    const helperScript = `
        const { spawn } = require("child_process");
        const child = spawn(process.execPath, ["-e", ${JSON.stringify(grandchildBody)}], {
            stdio: ["ignore", "pipe", "ignore"],
            detached: ${String(isWindows)},
        });
        child.stdout.once("data", () => process.stdout.write(String(child.pid)));
        child.unref();
        setTimeout(() => {}, 120000);
    `;
    const parent = spawn(process.execPath, ["-e", helperScript], {
        stdio: ["ignore", "pipe", "ignore"],
        detached: !isWindows,
    });

    const grandchildPid = new Promise<number>((resolve, reject) => {
        let data = "";
        parent.stdout!.on("data", (chunk) => {
            data += chunk.toString();
            const pid = parseInt(data.trim(), 10);
            if (!isNaN(pid) && pid > 0) resolve(pid);
        });
        parent.once("error", reject);
        setTimeout(() => reject(new Error("Timeout waiting for grandchild PID")), 5000);
    });

    return { parent, grandchildPid };
}

/** A client that owns `child` as its runtime, without starting a real CLI. */
function clientOwning(child: ChildProcess): CopilotClient {
    const client = new CopilotClient({
        connection: RuntimeConnection.forStdio({ path: "copilot" }),
    });
    (client as unknown as { cliProcess: ChildProcess }).cliProcess = child;
    (client as unknown as { isExternalServer: boolean }).isExternalServer = false;
    return client;
}

describe("process tree termination", () => {
    it("stop() terminates descendants of the owned runtime", async () => {
        const { parent, grandchildPid } = spawnRuntimeStandIn();
        const grandchild = await grandchildPid;
        const parentPid = parent.pid!;
        expect(isProcessAlive(parentPid)).toBe(true);
        expect(isProcessAlive(grandchild)).toBe(true);

        const errors = await clientOwning(parent).stop();

        expect(errors).toHaveLength(0);
        expect(await waitForExit(parentPid)).toBe(true);
        expect(await waitForExit(grandchild)).toBe(true);
    });

    it("forceStop() terminates descendants of the owned runtime", async () => {
        const { parent, grandchildPid } = spawnRuntimeStandIn();
        const grandchild = await grandchildPid;
        const parentPid = parent.pid!;

        await clientOwning(parent).forceStop();

        expect(await waitForExit(parentPid)).toBe(true);
        expect(await waitForExit(grandchild)).toBe(true);
    });

    // Windows has no SIGTERM, so the escalation this covers is POSIX-only.
    it.skipIf(isWindows)(
        "stop() reaps a descendant that ignores SIGTERM",
        async () => {
            const { parent, grandchildPid } = spawnRuntimeStandIn(true);
            const grandchild = await grandchildPid;
            const parentPid = parent.pid!;

            await clientOwning(parent).stop();

            expect(await waitForExit(parentPid)).toBe(true);
            expect(await waitForExit(grandchild)).toBe(true);
        },
        30000
    );

    it("leaves processes alone for external-server connections", async () => {
        const { parent, grandchildPid } = spawnRuntimeStandIn();
        const grandchild = await grandchildPid;
        const parentPid = parent.pid!;

        const client = new CopilotClient({
            connection: RuntimeConnection.forUri("http://localhost:19999"),
        });
        (client as unknown as { cliProcess: ChildProcess }).cliProcess = parent;
        expect((client as unknown as { isExternalServer: boolean }).isExternalServer).toBe(true);

        const errors = await client.stop();

        expect(errors).toHaveLength(0);
        expect(isProcessAlive(parentPid)).toBe(true);
        expect(isProcessAlive(grandchild)).toBe(true);

        parent.kill("SIGKILL");
        try {
            process.kill(grandchild, "SIGKILL");
        } catch {
            // Already gone.
        }
    });
});
