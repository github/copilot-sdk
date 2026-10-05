/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { SCHEMA_FILES } from "../build-prerequisites.mjs";
import {
    assertGeneratedClean,
    checkGenerated,
    generationBaseline,
    PROTOCOL_FILES,
    requiresSdkGeneration,
} from "./check-generated.mjs";

const sdkRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const clients = [
    "nodejs/src/generated/rpc.ts",
    "python/copilot/generated/rpc.py",
    "go/rpc/zrpc.go",
    "dotnet/src/Generated/Rpc.cs",
    "java/sdk/src/generated/java/com/github/copilot/generated/Rpc.java",
    "rust/src/generated/rpc.rs",
];

function fixture(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-freshness-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const git = (...args) => execFileSync("git", args, { cwd: root, encoding: "utf8" }).trim();
    const write = (file, text = "generated\n") => {
        fs.mkdirSync(path.dirname(path.join(root, file)), { recursive: true });
        fs.writeFileSync(path.join(root, file), text);
    };
    const commit = () => {
        git("add", ".");
        git("commit", "--quiet", "-m", "fixture");
        return git("rev-parse", "HEAD");
    };
    git("init", "--quiet");
    git("config", "user.name", "SDK Test");
    git("config", "user.email", "sdk-test@example.com");
    const schemas = Object.fromEntries(SCHEMA_FILES.map((name) => [`generated/${name}`, "{}\n"]));
    for (const [file, contents] of Object.entries(schemas)) write(file, contents);
    for (const file of [...clients, ...PROTOCOL_FILES]) write(`src/sdk/${file}`);
    const baseline = commit();
    const options = {
        root,
        baseline,
        generateSchemas: () => {
            for (const [file, contents] of Object.entries(schemas)) write(file, contents);
        },
        generateSdk: () => {
            for (const file of clients) write(`src/sdk/${file}`);
        },
        generateProtocol: () => {
            for (const file of PROTOCOL_FILES) write(`src/sdk/${file}`);
        },
    };
    return { root, git, write, commit, schemas, options };
}

for (const name of SCHEMA_FILES) {
    test(`rejects stale ${name} before SDK generation`, (t) => {
        const f = fixture(t);
        f.schemas[`generated/${name}`] = '{"new":true}\n';
        let sdkRan = false;
        f.options.generateSdk = () => {
            sdkRan = true;
        };
        assert.throws(
            () => checkGenerated(f.options),
            (error) => {
                assert(error instanceof Error);
                assert.match(error.message, /From the runtime repository root, run:\n  pnpm run generate:sdk/);
                assert.match(error.message, /Commit all resulting generated changes, including added\/deleted files/);
                assert.match(error.message, /Do not hand-edit generated files/);
                assert.match(error.message, new RegExp(name.replaceAll(".", "\\.")));
                return true;
            },
        );
        assert.equal(sdkRan, false);
    });
}

test("rejects a missing committed schema restored as an untracked output", (t) => {
    const f = fixture(t);
    f.git("rm", "generated/api.schema.json");
    f.commit();
    assert.throws(() => checkGenerated(f.options), /\?\? generated\/api.schema.json/);
});

for (const stale of [...clients.map((file) => [file]), clients]) {
    test(`rejects committed new schemas with stale clients: ${stale.join(", ")}`, (t) => {
        const f = fixture(t);
        f.schemas["generated/api.schema.json"] = '{"new":true}\n';
        f.write("generated/api.schema.json", f.schemas["generated/api.schema.json"]);
        f.commit();
        f.options.generateSdk = () => {
            for (const file of stale) f.write(`src/sdk/${file}`, "updated projection\n");
        };
        assert.throws(() => checkGenerated(f.options), /pnpm run generate:sdk/);
    });
}

test("accepts correctly committed schemas and all six clients", (t) => {
    const f = fixture(t);
    f.schemas["generated/api.schema.json"] = '{"new":true}\n';
    f.write("generated/api.schema.json", f.schemas["generated/api.schema.json"]);
    f.options.generateSdk = () => {
        for (const file of clients) f.write(`src/sdk/${file}`, "updated projection\n");
    };
    f.options.generateSdk();
    f.commit();
    assert.equal(checkGenerated(f.options), true);
    assert.equal(f.git("status", "--porcelain"), "");
});

test("rejects manual client edits even when the schemas did not change", (t) => {
    const f = fixture(t);
    f.write(`src/sdk/${clients[0]}`, "manual edit\n");
    f.commit();
    assert.throws(() => checkGenerated(f.options), /pnpm run generate:sdk/);
});

test("does not classify handwritten package initializers or build artifacts as generation inputs", () => {
    assert.equal(
        requiresSdkGeneration([
            "src/sdk/python/copilot/generated/__init__.py",
            "src/sdk/dotnet/src/obj/Generated.cs",
            "src/sdk/dotnet/src/nested/bin/Generated.cs",
        ]),
        false,
    );
});

test("skips projections for internal-only and SDK documentation changes", (t) => {
    const f = fixture(t);
    f.write("src/runtime/src/internal.rs", "internal");
    f.write("src/sdk/docs/example.md", "documentation");
    f.commit();
    f.options.generateSdk = () => assert.fail("SDK generation was unnecessary");
    assert.equal(checkGenerated(f.options), false);
});

for (const file of [
    "src/sdk/scripts/codegen/go.ts",
    "src/sdk/scripts/codegen/package-lock.json",
    "src/sdk/java/scripts/codegen/java.ts",
    "src/sdk/rust/.rustfmt.nightly.toml",
    "src/sdk/go/types.go",
    "src/sdk/dotnet/src/Models.cs",
    ...clients.map((file) => `src/sdk/${file}`),
]) {
    test(`validates projections for changed or deleted input/output ${file}`, () => {
        assert.equal(requiresSdkGeneration([file]), true);
    });
}

test("detects new Go output outside a generated directory", (t) => {
    const f = fixture(t);
    f.options.baseline = undefined;
    f.options.generateSdk = () => f.write("src/sdk/go/rpc/znew.go");
    assert.throws(() => checkGenerated(f.options), /\?\? src\/sdk\/go\/rpc\/znew.go/);
});

test("detects a deleted generated output", (t) => {
    const f = fixture(t);
    f.options.baseline = undefined;
    f.options.generateSdk = () => fs.unlinkSync(path.join(f.root, "src/sdk", clients[0]));
    assert.throws(() => checkGenerated(f.options), /D src\/sdk\/nodejs\/src\/generated\/rpc.ts/);
});

for (const phase of ["generateSchemas", "generateSdk", "generateProtocol"]) {
    test(`propagates ${phase} failure`, (t) => {
        const f = fixture(t);
        f.options.baseline = undefined;
        f.options[phase] = () => {
            throw new Error(`${phase} failed`);
        };
        assert.throws(() => checkGenerated(f.options), new RegExp(`${phase} failed`));
    });
}

test("validates protocol constants even when schema projection generation is skipped", (t) => {
    const f = fixture(t);
    f.write("src/sdk/sdk-protocol-version.json", '{"version":4}\n');
    f.write(`src/sdk/${PROTOCOL_FILES[4]}`, "stale Java\n");
    f.commit();
    f.options.generateSdk = () => assert.fail("Protocol-only changes do not require projections");
    assert.throws(() => checkGenerated(f.options), /SdkProtocolVersion.java/);
});

test("uses the tested PR merge's actual base, not an earlier event base", (t) => {
    const f = fixture(t);
    const oldBase = f.options.baseline;
    f.git("checkout", "--quiet", "-b", "topic");
    f.write("internal", "feature");
    f.commit();
    f.git("checkout", "--quiet", "-b", "advanced-base", oldBase);
    f.write("generated/api.schema.json", '{"baseAdvanced":true}\n');
    const actualBase = f.commit();
    f.git("merge", "--quiet", "--no-ff", "topic", "-m", "test merge");
    assert.equal(generationBaseline(f.root, "pull_request", { pull_request: { base: { sha: oldBase } } }), actualBase);
    const paths = f.git("diff", "--name-only", actualBase, "HEAD").split("\n");
    assert.equal(requiresSdkGeneration(paths), false);
});

test("uses the whole merge-group/push range, not just the final commit", (t) => {
    const f = fixture(t);
    f.write("generated/api.schema.json", '{"earlierCommit":true}\n');
    f.commit();
    f.write("internal", "last commit");
    f.commit();
    for (const [eventName, event] of [
        ["merge_group", { merge_group: { base_sha: f.options.baseline } }],
        ["push", { before: f.options.baseline }],
    ]) {
        const baseline = generationBaseline(f.root, eventName, event);
        assert.equal(baseline, f.options.baseline);
        assert.equal(requiresSdkGeneration(f.git("diff", "--name-only", baseline, "HEAD").split("\n")), true);
    }
});

test("requires a valid baseline unless full validation was explicitly selected", (t) => {
    const f = fixture(t);
    assert.throws(() => generationBaseline(f.root, "merge_group"), /Missing.*baseline/);
    assert.throws(() => generationBaseline(f.root, "push", { before: "absent" }));
    assert.equal(generationBaseline(f.root, "workflow_dispatch"), undefined);
    assert.equal(generationBaseline(f.root, "push", { before: "0".repeat(40) }), undefined);
});

test("protocol generation owns all six outputs and preserves their current APIs and bytes", (t) => {
    const f = fixture(t);
    const script = "src/sdk/nodejs/scripts/update-protocol-version.ts";
    const currentVersion = JSON.parse(fs.readFileSync(path.join(sdkRoot, "sdk-protocol-version.json"), "utf8")).version;
    f.write(script, fs.readFileSync(path.join(sdkRoot, "nodejs/scripts/update-protocol-version.ts"), "utf8"));
    f.write("src/sdk/sdk-protocol-version.json", JSON.stringify({ version: currentVersion }));
    execFileSync(process.execPath, [script], { cwd: f.root });
    for (const file of PROTOCOL_FILES) {
        assert.equal(
            fs.readFileSync(path.join(f.root, "src/sdk", file), "utf8"),
            fs.readFileSync(path.join(sdkRoot, file), "utf8"),
            file,
        );
    }
    const nextVersion = currentVersion + 1;
    f.write("src/sdk/sdk-protocol-version.json", JSON.stringify({ version: nextVersion }));
    execFileSync(process.execPath, [script], { cwd: f.root });
    for (const file of PROTOCOL_FILES) {
        assert.match(fs.readFileSync(path.join(f.root, "src/sdk", file), "utf8"), new RegExp(`\\b${nextVersion}\\b`));
    }
    f.commit();
    execFileSync(process.execPath, [script], { cwd: f.root });
    assertGeneratedClean(
        f.root,
        PROTOCOL_FILES.map((file) => `src/sdk/${file}`),
        "protocol generator",
    );
});
