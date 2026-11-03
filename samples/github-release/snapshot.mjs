// Copyright (c) Microsoft Corporation. All rights reserved.

import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { createReadStream } from "node:fs";
import { rm, stat } from "node:fs/promises";
import { dirname } from "node:path";
import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";

const [assetUrl, buildVersion = "1.2.3"] = process.argv.slice(2);
if (!assetUrl) {
    throw new Error("Usage: node samples/github-release/snapshot.mjs <release asset URL> [build version]");
}
const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
    gitHubToken: process.env.GITHUB_TOKEN || process.env.GH_TOKEN || undefined,
    useLoggedInUser: false,
});
const failures = [];
let snapshotId;
let download;

async function digest(path) {
    const hash = createHash("sha256");
    for await (const chunk of createReadStream(path)) hash.update(chunk);
    return hash.digest("hex");
}

try {
    await client.start();
    const api = client.internalRpc.gitHubRelease;
    snapshotId = await api.allocateRequestId();
    const params = {
        ...snapshotId,
        updateChannel: "stable",
        buildVersion,
        packageIdentity: "release",
    };
    const [snapshot, repeated] = await Promise.all([api.getSnapshot(params), api.getSnapshot(params)]);
    assert.deepEqual(repeated, snapshot);
    if (snapshot.outcome !== "success") throw new Error(snapshot.error);
    download = await api.downloadAsset({
        ...(await api.allocateRequestId()),
        snapshotRequestId: snapshotId.requestId,
        assetUrl,
    });
    if (download.kind !== "downloaded" || !download.path) {
        throw new Error(download.message ?? `Release download: ${download.kind}`);
    }
    assert.equal(download.sizeBytes, (await stat(download.path)).size);
    assert.equal(download.sha256, await digest(download.path));
    await api.cancel(snapshotId);
    snapshotId = undefined;
    assert.equal(download.sha256, await digest(download.path));
    process.stdout.write(`${JSON.stringify({
        kind: "release-snapshot",
        outcome: snapshot.outcome,
        runningVersion: snapshot.runningVersion,
        lookupDurationMs: snapshot.lookupDurationMs,
        candidate: snapshot.automaticUpdateCandidate?.tag_name,
        sizeBytes: download.sizeBytes,
        sha256: download.sha256,
        completedFileSurvivedSnapshotDisposal: true,
    })}\n`);
} catch (error) {
    failures.push(error);
} finally {
    for (const cleanup of [
        () => snapshotId && client.internalRpc.gitHubRelease.cancel(snapshotId),
        () => download?.path && rm(dirname(download.path), { recursive: true, force: true }),
        () => client.stop(),
    ]) {
        try {
            await cleanup();
        } catch (error) {
            failures.push(error);
        }
    }
}
if (failures.length) throw new AggregateError(failures, "Release snapshot example failed");
