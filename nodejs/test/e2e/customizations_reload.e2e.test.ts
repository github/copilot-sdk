/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session customization reload", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("discovers repository instructions created after session startup without changing its cwd", async () => {
        execFileSync("git", ["init", "--quiet"], { cwd: workDir });
        const session = await client.createSession({ onPermissionRequest: approveAll });

        try {
            const before = await session.rpc.metadata.snapshot();
            const marker = "SDK_RELOAD_NEW_REPOSITORY_INSTRUCTIONS";
            expect(
                (await session.rpc.instructions.getSources()).sources.some((source) =>
                    source.content.includes(marker)
                )
            ).toBe(false);

            const instructionsDir = join(workDir, ".github");
            mkdirSync(instructionsDir);
            writeFileSync(
                join(instructionsDir, "copilot-instructions.md"),
                `Repository instructions: ${marker}\n`
            );

            const reload = await session.rpc.customizations.reload();
            expect(reload.errors).toEqual([]);
            expect(reload.outcomes).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({ subsystem: "repositoryContext", status: "reloaded" }),
                    expect.objectContaining({ subsystem: "instructions", status: "reloaded" }),
                ])
            );
            expect(
                (await session.rpc.instructions.getSources()).sources.some(
                    (source) =>
                        source.type === "repo" &&
                        source.location === "repository" &&
                        source.content.includes(marker)
                )
            ).toBe(true);
            expect((await session.rpc.metadata.snapshot()).workingDirectory).toBe(
                before.workingDirectory
            );
        } finally {
            await session.disconnect();
        }
    });
});
