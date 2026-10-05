/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync } from "node:fs";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { rimraf } from "rimraf";
import { afterAll, describe, expect, it, onTestFinished, vi } from "vitest";
import { createSdkTestContext } from "./sdkTestContext";

vi.mock("./CapiProxy", () => ({
    CapiProxy: class {
        start = vi.fn().mockResolvedValue("http://127.0.0.1:1");
        getProxyEnv = vi.fn().mockReturnValue({});
        setCopilotUserByToken = vi.fn().mockResolvedValue(undefined);
        updateConfig = vi.fn().mockResolvedValue(undefined);
        stop = vi.fn().mockResolvedValue(undefined);
    },
}));

vi.mock("rimraf", async (importOriginal) => {
    const actual = await importOriginal<typeof import("rimraf")>();
    return { ...actual, rimraf: vi.fn(actual.rimraf) };
});

describe("SDK test context cleanup", async () => {
    let workDir: string;
    afterAll(() => {
        expect(rimraf).toHaveBeenCalled();
        expect(existsSync(workDir)).toBe(false);
    });
    const context = await createSdkTestContext();
    workDir = context.workDir;

    it("keeps working files available until test-owned client cleanup finishes", async () => {
        const client = context.createClient();
        const logsDir = join(workDir, "logs");
        const logPath = join(logsDir, "client.log");
        onTestFinished(async () => {
            try {
                expect(rimraf).not.toHaveBeenCalled();
                expect(await readFile(logPath, "utf8")).toBe("client is running");
                await writeFile(logPath, "client is stopped");
            } finally {
                await client.stop();
            }
        });
        await mkdir(logsDir);
        await writeFile(logPath, "client is running");
    });
});
