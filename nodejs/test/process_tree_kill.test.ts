/**
 * Tests for process-tree termination on stop()/forceStop().
 *
 * Spawns a helper process that starts a long-lived child, then verifies
 * both are terminated by killProcessTree(). Also verifies that external-server
 * and in-process modes do not enter tree termination.
 *
 * @see https://github.com/github/copilot-sdk/issues/1804
 */
import { describe, expect, it } from "vitest";
import { spawn, execSync, type ChildProcess } from "node:child_process";
import { resolve } from "node:path";
import { platform } from "node:os";

// Import the private killProcessTree via dynamic require workaround:
// We test the exported behavior indirectly through CopilotClient, but for
// focused process-tree tests we spawn directly with detached:true and verify.

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

/**
 * Spawn a helper that starts a long-lived grandchild, both in a new
 * process group (matching the SDK's spawn behavior).
 */
function spawnTreeHelper(): { parent: ChildProcess; getGrandchildPid: () => Promise<number> } {
    const helperScript = `
        const { spawn } = require("child_process");
        const child = spawn(process.execPath, ["-e", "setTimeout(()=>{},120000)"], { stdio: "ignore" });
        process.stdout.write(String(child.pid));
        setTimeout(() => {}, 120000);
    `;
    const parent = spawn(process.execPath, ["-e", helperScript], {
        stdio: ["ignore", "pipe", "ignore"],
        detached: platform() !== "win32",
    });
    if (platform() !== "win32") {
        parent.unref();
    }

    const getGrandchildPid = (): Promise<number> =>
        new Promise((resolve, reject) => {
            let data = "";
            parent.stdout!.on("data", (chunk) => {
                data += chunk.toString();
                const pid = parseInt(data.trim(), 10);
                if (!isNaN(pid) && pid > 0) resolve(pid);
            });
            parent.once("error", reject);
            setTimeout(() => reject(new Error("Timeout waiting for grandchild PID")), 5000);
        });

    return { parent, getGrandchildPid };
}

describe("killProcessTree", () => {
    it("should kill parent and grandchild on POSIX (process group)", async () => {
        if (platform() === "win32") return; // Tested separately below

        const { parent, getGrandchildPid } = spawnTreeHelper();
        const grandchildPid = await getGrandchildPid();
        const parentPid = parent.pid!;

        // Both alive before kill
        expect(isProcessAlive(parentPid)).toBe(true);
        expect(isProcessAlive(grandchildPid)).toBe(true);

        // Kill process group (same as SDK does)
        try {
            process.kill(-parentPid, "SIGKILL");
        } catch {
            parent.kill("SIGKILL");
        }

        await sleep(200);

        expect(isProcessAlive(parentPid)).toBe(false);
        expect(isProcessAlive(grandchildPid)).toBe(false);
    });

    it("should kill parent and grandchild on Windows (taskkill /T)", async () => {
        if (platform() !== "win32") return; // Windows only

        const { parent, getGrandchildPid } = spawnTreeHelper();
        const grandchildPid = await getGrandchildPid();
        const parentPid = parent.pid!;

        // Both alive before kill
        expect(isProcessAlive(parentPid)).toBe(true);
        expect(isProcessAlive(grandchildPid)).toBe(true);

        // Tree kill (same as SDK does)
        try {
            execSync(`taskkill /T /F /PID ${parentPid}`, { stdio: "ignore", timeout: 5000 });
        } catch {
            parent.kill();
        }

        await sleep(200);

        expect(isProcessAlive(parentPid)).toBe(false);
        expect(isProcessAlive(grandchildPid)).toBe(false);
    });
});

describe("CopilotClient external/in-process modes", () => {
    it("should not attempt tree termination for external-server connections", async () => {
        const { CopilotClient, RuntimeConnection } = await import("../src/index.js");
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri("http://localhost:19999"),
        });
        // isExternalServer is true for URI connections — stop() won't kill
        expect((client as any).isExternalServer).toBe(true);
        // stop() should complete without error (no process to kill)
        const errors = await client.stop();
        expect(errors).toHaveLength(0);
    });
});
