// Copyright (c) Microsoft Corporation. All rights reserved.

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { copyFileSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const scriptsDir = dirname(fileURLToPath(import.meta.url));
const targets = [
    "darwin-arm64",
    "darwin-x64",
    "linux-arm64",
    "linux-x64",
    "linuxmusl-arm64",
    "linuxmusl-x64",
    "win32-arm64",
    "win32-x64",
];
const scripts = [
    ["snapshot-bundled-cli-version.sh", "cli-version.txt", false],
    ["snapshot-bundled-in-process-version.sh", "cli-version-in-process.txt", true],
];

function fixture(t, version) {
    const root = mkdtempSync(join(scriptsDir, ".snapshot-test-"));
    t.after(() => rmSync(root, { recursive: true, force: true }));
    const rust = join(root, "rust");
    mkdirSync(join(rust, "scripts"), { recursive: true });
    mkdirSync(join(root, "nodejs"));
    mkdirSync(join(root, "bin"));
    writeFileSync(join(root, "nodejs", "package.json"), JSON.stringify({ copilotCliVersion: version }));
    for (const script of [...scripts.map(([script]) => script), "snapshot-release.sh"]) {
        copyFileSync(join(scriptsDir, script), join(rust, "scripts", script));
    }
    const hashes = new Map();
    for (const target of targets) {
        for (const asset of [
            `copilot-${target}.${target.startsWith("win32") ? "zip" : "tar.gz"}`,
            `github-copilot-${version}-${target}.tgz`,
        ]) {
            hashes.set(asset, createHash("sha256").update(asset).digest("hex"));
        }
    }
    const sums = [...hashes].map(([asset, hash]) => `${hash.toUpperCase()} *${asset}`).join("\r\n");
    writeFileSync(join(rust, "SHA256SUMS.txt"), `${sums}\r\n`);
    // Capture the download boundary without contacting GitHub.
    writeFileSync(join(root, "bin", "curl"), `#!/usr/bin/env bash
printf '%s\\n' "$@" > curl-args.txt
cat SHA256SUMS.txt
`, { mode: 0o755 });
    return {
        rust,
        hashes,
        run(script, args = []) {
            const result = spawnSync("bash", [
                "-c",
                'export PATH="$PWD/../bin:$PATH"; exec bash "$@"',
                "snapshot-test",
                join("scripts", script),
                ...args,
            ], {
                cwd: rust,
                encoding: "utf8",
                windowsHide: true,
                timeout: 30_000,
            });
            assert.ifError(result.error);
            return result;
        },
    };
}

function assertSnapshot(fixture, filename, version, runtime, releaseUrl) {
    const contents = readFileSync(join(fixture.rust, filename), "utf8");
    const entries = new Map(contents.split(/\r?\n/).filter((line) => line && !line.startsWith("#"))
        .map((line) => line.split("=")));
    assert.equal(entries.get("version"), version);
    assert.equal(entries.get("release-url"), releaseUrl);
    assert.equal(entries.size, targets.length + 1 + Number(Boolean(releaseUrl)));
    for (const target of targets) {
        const key = runtime ? `copilot-${target}` : `copilot-${target}.${target.startsWith("win32") ? "zip" : "tar.gz"}`;
        const asset = runtime ? `github-copilot-${version}-${target}.tgz` : key;
        assert.equal(entries.get(key), fixture.hashes.get(asset));
    }
}

test("no-option snapshots keep the legacy exact version, URL, and hash format", (t) => {
    const version = "1.2.3-4";
    const f = fixture(t, version);
    for (const [script, output, runtime] of scripts) {
        const result = f.run(script);
        assert.equal(result.status, 0, result.stderr);
        assertSnapshot(f, output, version, runtime, undefined);
        assert.match(
            readFileSync(join(f.rust, "curl-args.txt"), "utf8"),
            /https:\/\/github\.com\/github\/copilot-cli\/releases\/download\/v1\.2\.3-4\/SHA256SUMS\.txt/,
        );
    }
});

test("local checksum snapshots pin the final unstable URL without publication or sibling metadata", (t) => {
    const version = "1.2.3-unstable.20260923";
    const url = `https://github.com/github/copilot-sdk/releases/download/runtime-${version}`;
    const f = fixture(t, version);
    rmSync(join(f.rust, "..", "nodejs"), { recursive: true });
    for (const [script, output, runtime] of scripts) {
        const result = f.run(script, ["--version", version, "--release-url", url, "--checksums", "SHA256SUMS.txt"]);
        assert.equal(result.status, 0, result.stderr);
        assertSnapshot(f, output, version, runtime, url);
    }
    assert.throws(() => readFileSync(join(f.rust, "curl-args.txt")), { code: "ENOENT" });
});

test("URL-only override fetches checksums from the exact selected release", (t) => {
    const version = "1.2.3-unstable.20260923";
    const url = `https://github.com/github/copilot-sdk/releases/download/runtime-${version}`;
    const f = fixture(t, version);
    for (const [script, output, runtime] of scripts) {
        const result = f.run(script, ["--release-url", url]);
        assert.equal(result.status, 0, result.stderr);
        assertSnapshot(f, output, version, runtime, url);
        assert.equal(readFileSync(join(f.rust, "curl-args.txt"), "utf8").trim().split("\n").at(-1), `${url}/SHA256SUMS.txt`);
    }
});

test("reviewed producers can stamp normal promotions without changing legacy product code", (t) => {
    for (const version of ["1.2.3", "1.2.3-4"]) {
        const f = fixture(t, version);
        const product = join(f.rust, "..", "old-product", "rust");
        mkdirSync(product, { recursive: true });
        writeFileSync(join(product, "build.rs"), "// unchanged legacy build script\n");
        rmSync(join(f.rust, "..", "nodejs"), { recursive: true });
        for (const [script, output, runtime] of scripts) {
            writeFileSync(join(f.rust, output), "reviewed snapshot must remain unchanged");
            const result = f.run(script, [
                "--version", version,
                "--release-url", `https://github.com/github/copilot-cli/releases/download/v${version}`,
                "--checksums", "SHA256SUMS.txt",
                "--output", join(product, output),
            ]);
            assert.equal(result.status, 0, result.stderr);
            assertSnapshot({ ...f, rust: product }, output, version, runtime, undefined);
            assert.equal(readFileSync(join(f.rust, output), "utf8"), "reviewed snapshot must remain unchanged");
        }
        assert.equal(readFileSync(join(product, "build.rs"), "utf8"), "// unchanged legacy build script\n");
        assert.throws(() => readFileSync(join(product, "scripts", "snapshot-release.sh")), { code: "ENOENT" });
        assert.throws(() => readFileSync(join(f.rust, "curl-args.txt")), { code: "ENOENT" });
    }
});

test("invalid or mismatched release locations fail before acquisition", (t) => {
    const f = fixture(t, "1.2.3");
    for (const [script] of scripts) {
        for (const url of [
            "https://github.com/github/copilot-sdk/releases/download/runtime-9.9.9",
            "https://github.com/github/copilot-cli/releases/latest",
            "https://user:token@github.com/github/copilot-sdk/releases/download/runtime-1.2.3",
        ]) {
            const result = f.run(script, ["--release-url", url]);
            assert.notEqual(result.status, 0);
            assert.match(result.stderr, /exact public/);
        }
    }
    assert.throws(() => readFileSync(join(f.rust, "curl-args.txt")), { code: "ENOENT" });
});

test("missing, duplicate, and invalid hashes preserve an existing snapshot", (t) => {
    const f = fixture(t, "1.2.3");
    for (const [script, output, runtime] of scripts) {
        const asset = runtime ? "github-copilot-1.2.3-darwin-arm64.tgz" : "copilot-darwin-arm64.tar.gz";
        const hash = f.hashes.get(asset);
        for (const contents of ["", `bad ${asset}\n`, `${hash} ${asset}\n${hash} ${asset}\n`]) {
            writeFileSync(join(f.rust, output), "original snapshot");
            writeFileSync(join(f.rust, "invalid-sums.txt"), contents);
            const result = f.run(script, ["--checksums", "invalid-sums.txt"]);
            assert.notEqual(result.status, 0);
            assert.match(result.stderr, /one valid SHA-256/);
            assert.equal(readFileSync(join(f.rust, output), "utf8"), "original snapshot");
        }
    }
});
