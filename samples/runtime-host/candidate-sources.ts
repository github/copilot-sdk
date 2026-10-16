/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { readFileSync, realpathSync } from "node:fs";
import { isAbsolute, join } from "node:path";

interface CandidateSource {
    repository: string;
    checkout: string;
    commit: string;
}

export interface CandidateSources {
    runtime: CandidateSource;
    hosting: { repository: "github/copilot-host"; commit: string };
    sdk: CandidateSource & { path: "src/sdk" };
}

/** The linked library's immutable source is recorded by Cargo, not a companion build. */
export function hostingSource(runtime: string): CandidateSources["hosting"] {
    const lock = readFileSync(join(runtime, "Cargo.lock"), "utf8");
    const packages = lock.split(/^\[\[package\]\]\s*$/m).filter((entry) => /^name = "copilotd-hosting"$/m.test(entry));
    assert.equal(packages.length, 1, "Cargo.lock must identify one copilotd-hosting library");
    const source = /^source = "(git\+[^"]+)"$/m.exec(packages[0])?.[1];
    assert(source, "The hosting library must use an immutable Git source");
    const url = new URL(source.slice(4));
    assert.equal(url.origin + url.pathname.replace(/\.git$/, ""), "https://github.com/github/copilot-host");
    const commit = url.hash.slice(1);
    assert.match(commit, /^[a-f0-9]{40}$/, "Hosting source needs a full resolved commit");
    assert.equal(url.searchParams.get("rev"), commit, "Hosting dependency must pin its resolved commit");
    return { repository: "github/copilot-host", commit };
}

function git(checkout: string, ...args: string[]): string {
    return execFileSync("git", ["-C", checkout, ...args], {
        encoding: "utf8",
    }).trim();
}

export function validateCandidateSources(sources: CandidateSources, sdk: string): { runtime: string; sdk: string } {
    const roots = {} as { runtime: string; sdk: string };
    for (const [name, repository] of [["runtime", "github/copilot-agent-runtime"]] as const) {
        const source = sources[name];
        assert.equal(source.repository, repository);
        assert(isAbsolute(source.checkout), `${name} checkout must be absolute`);
        assert.match(source.commit, /^[a-f0-9]{40}$/, `${name} needs a full source commit`);
        roots[name] = realpathSync(source.checkout);
        assert.equal(
            realpathSync(git(roots[name], "rev-parse", "--show-toplevel")),
            roots[name],
            `${name} checkout must be a repository root`,
        );
        assert.equal(
            git(roots[name], "rev-parse", "HEAD"),
            source.commit,
            `${name} candidate source must match the local checkout`,
        );
    }
    assert.deepEqual(
        sources.hosting,
        hostingSource(roots.runtime),
        "Candidate hosting source must match the runtime's locked library",
    );
    assert.equal(sources.sdk.repository, sources.runtime.repository);
    assert(isAbsolute(sources.sdk.checkout), "sdk checkout must be absolute");
    assert.equal(realpathSync(sources.sdk.checkout), roots.runtime, "SDK must belong to the runtime checkout");
    assert.equal(sources.sdk.commit, sources.runtime.commit, "SDK must use the runtime commit");
    assert.equal(sources.sdk.path, "src/sdk", "SDK must identify the src/sdk subtree");
    roots.sdk = realpathSync(join(roots.runtime, sources.sdk.path));
    assert.equal(roots.sdk, realpathSync(sdk), "Candidate must use this local SDK checkout");
    assert.equal(
        realpathSync(git(roots.sdk, "rev-parse", "--show-toplevel")),
        roots.runtime,
        "SDK must be part of the runtime repository",
    );
    return roots;
}

export function localCandidateSources(runtime: string, sdk: string): CandidateSources {
    const sources: CandidateSources = {
        runtime: {
            repository: "github/copilot-agent-runtime",
            checkout: realpathSync(runtime),
            commit: git(runtime, "rev-parse", "HEAD"),
        },
        hosting: hostingSource(runtime),
        sdk: {
            repository: "github/copilot-agent-runtime",
            checkout: realpathSync(runtime),
            commit: git(runtime, "rev-parse", "HEAD"),
            path: "src/sdk",
        },
    };
    validateCandidateSources(sources, sdk);
    for (const name of ["runtime"] as const) {
        assert.equal(
            git(sources[name].checkout, "status", "--porcelain", "--untracked-files=no"),
            "",
            `Commit tracked changes in ${name} before attesting a source revision`,
        );
    }
    return sources;
}
