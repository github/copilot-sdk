/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Verifies published runtime staging is target-specific and fails on incomplete packages. */

import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

import { preparePublishedRuntime } from "./prepare-published-runtime.js";

test("normalizes an acquired target without substituting another libc", async (t) => {
    const root = mkdtempSync(join(tmpdir(), "published-runtime-"));
    t.after(() => rmSync(root, { recursive: true, force: true }));
    const packageRoot = join(root, "package");
    const wrapper = join(packageRoot, "prebuilds/linuxmusl-arm64/copilot-runtime");
    const executable = join(root, "downloaded-copilot");
    mkdirSync(join(packageRoot, "prebuilds/linuxmusl-arm64"), { recursive: true });
    writeFileSync(wrapper, "musl-wrapper");
    writeFileSync(join(packageRoot, "prebuilds/linuxmusl-arm64/runtime.node"), "musl-native");
    writeFileSync(executable, "musl-executable");

    await preparePublishedRuntime({
        target: "linuxmusl-arm64",
        runtimeRoot: root,
        acquirePackage: async (target) => {
            assert.equal(target, "linuxmusl-arm64");
            return packageRoot;
        },
        acquireExecutable: async (target) => {
            assert.equal(target, "linuxmusl-arm64");
            return { path: executable };
        },
    });

    assert.equal(readFileSync(join(root, "dist-bin/linuxmusl-arm64/copilot"), "utf8"), "musl-executable");
    assert.equal(readFileSync(join(root, "dist-cli/prebuilds/linuxmusl-arm64/runtime.node"), "utf8"), "musl-native");
    assert.equal(statSync(join(root, "dist-bin/linuxmusl-arm64/copilot")).mode & 0o111, 0o111);
    assert.equal(statSync(join(root, "dist-cli/prebuilds/linux-arm64"), { throwIfNoEntry: false }), undefined);
});

test("rejects an incomplete package before acquiring a standalone executable", async (t) => {
    const root = mkdtempSync(join(tmpdir(), "published-runtime-incomplete-"));
    t.after(() => rmSync(root, { recursive: true, force: true }));
    let executableRequested = false;

    await assert.rejects(
        preparePublishedRuntime({
            target: "linuxmusl-arm64",
            runtimeRoot: root,
            acquirePackage: async () => root,
            acquireExecutable: async () => {
                executableRequested = true;
                return { path: join(root, "missing") };
            },
        }),
        /Published runtime wrapper not found/,
    );
    assert.equal(executableRequested, false);
});
