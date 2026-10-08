/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync, readFileSync } from "node:fs";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { waitForFileText } from "./sdkTestHelper.js";

vi.mock("node:fs", () => ({
    existsSync: vi.fn(),
    readFileSync: vi.fn(),
}));

describe("waitForFileText", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.mocked(existsSync).mockReturnValue(true);
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.resetAllMocks();
    });

    it("waits for a Windows writer to release the marker", async () => {
        vi.mocked(readFileSync)
            .mockImplementationOnce(() => {
                throw Object.assign(new Error("resource busy"), { code: "EBUSY" });
            })
            .mockReturnValue("done");

        const result = expect(waitForFileText("marker", "done")).resolves.toBeUndefined();
        await Promise.all([result, vi.advanceTimersByTimeAsync(100)]);
        expect(readFileSync).toHaveBeenCalledTimes(2);
    });

    it("preserves the locking error when the observation deadline expires", async () => {
        const busy = Object.assign(new Error("resource busy"), { code: "EBUSY" });
        vi.mocked(readFileSync).mockImplementation(() => {
            throw busy;
        });

        const result = expect(waitForFileText("marker", "done", 200)).rejects.toMatchObject({
            message: expect.stringContaining("Timed out waiting for shell command"),
            cause: busy,
        });
        await Promise.all([result, vi.advanceTimersByTimeAsync(200)]);
    });

    it("does not hide other filesystem errors", async () => {
        const denied = Object.assign(new Error("permission denied"), { code: "EACCES" });
        vi.mocked(readFileSync).mockImplementation(() => {
            throw denied;
        });

        await expect(waitForFileText("marker", "done")).rejects.toBe(denied);
    });

    it("preserves timeout diagnostics without replacing the marker error", async () => {
        const busy = Object.assign(new Error("resource busy"), { code: "EBUSY" });
        vi.mocked(existsSync).mockImplementation((filePath) => filePath !== "absent");
        vi.mocked(readFileSync).mockImplementation((filePath) => {
            if (filePath === "marker.phase") {
                return "marker-written";
            }
            if (filePath === "unreadable") {
                throw new Error("permission denied");
            }
            throw busy;
        });

        const result = expect(
            waitForFileText("marker", "done", 200, ["marker.phase", "absent", "unreadable"])
        ).rejects.toMatchObject({
            message:
                "Timed out waiting for shell command to write 'done' to 'marker'.\n" +
                "marker.phase: marker-written\nabsent: absent\n" +
                "unreadable: could not read: Error: permission denied",
            cause: busy,
        });
        await Promise.all([result, vi.advanceTimersByTimeAsync(200)]);
    });

    it("continues observing an absent or incomplete marker", async () => {
        vi.mocked(existsSync).mockReturnValueOnce(false);
        vi.mocked(readFileSync).mockReturnValueOnce("").mockReturnValue("done");

        const result = expect(waitForFileText("marker", "done")).resolves.toBeUndefined();
        await Promise.all([result, vi.advanceTimersByTimeAsync(200)]);
        expect(readFileSync).toHaveBeenCalledTimes(2);
    });
});
