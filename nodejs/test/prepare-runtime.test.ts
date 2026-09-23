/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { resolvePreparedRuntimePath } from "../scripts/prepare-runtime";

describe("prepare-runtime", () => {
    it("selects explicit same-checkout runtime artifacts without published acquisition", async () => {
        const root = await mkdtemp(join(tmpdir(), "copilot-prepare-runtime-"));
        try {
            const cliPath = join(root, "copilot-runtime");
            const legacyCliPath = join(root, "app.js");
            await Promise.all([writeFile(cliPath, ""), writeFile(legacyCliPath, "")]);
            const acquirePackage = vi.fn(async () => {
                throw new Error("published acquisition must not run");
            });

            await expect(
                resolvePreparedRuntimePath({
                    acquirePackage,
                    environment: {
                        COPILOT_CLI_PATH: cliPath,
                        COPILOT_LEGACY_CLI_PATH: legacyCliPath,
                        COPILOT_RUNTIME_SOURCE: "checkout",
                    },
                })
            ).resolves.toBe(cliPath);
            await expect(
                resolvePreparedRuntimePath({
                    acquirePackage,
                    environment: {
                        COPILOT_CLI_PATH: cliPath,
                        COPILOT_LEGACY_CLI_PATH: legacyCliPath,
                        COPILOT_RUNTIME_SOURCE: "checkout",
                    },
                    option: "--print-legacy-path",
                })
            ).resolves.toBe(legacyCliPath);
            expect(acquirePackage).not.toHaveBeenCalled();
        } finally {
            await rm(root, { force: true, recursive: true });
        }
    });

    it("fails missing checkout artifacts before published acquisition", async () => {
        const acquirePackage = vi.fn(async () => {
            throw new Error("published acquisition must not run");
        });

        await expect(
            resolvePreparedRuntimePath({
                acquirePackage,
                environment: { COPILOT_RUNTIME_SOURCE: "checkout" },
                root: join(import.meta.dirname, "copied-sdk"),
            })
        ).rejects.toThrow(
            /COPILOT_CLI_PATH must select an existing same-checkout runtime artifact/
        );
        expect(acquirePackage).not.toHaveBeenCalled();
    });

    it("retains pinned package acquisition outside the runtime checkout", async () => {
        const packageRoot = join(import.meta.dirname, "published-package");
        const acquirePackage = vi.fn(async () => packageRoot);

        await expect(
            resolvePreparedRuntimePath({
                acquirePackage,
                environment: {},
                option: "--print-legacy-path",
                root: join(import.meta.dirname, "copied-sdk"),
            })
        ).resolves.toBe(join(packageRoot, "app.js"));
        expect(acquirePackage).toHaveBeenCalledOnce();
    });
});
