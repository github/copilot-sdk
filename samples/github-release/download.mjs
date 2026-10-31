// Copyright (c) Microsoft Corporation. All rights reserved.

import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";

const [assetUrl, expectedSha256] = process.argv.slice(2);
if (!assetUrl || !expectedSha256) {
    throw new Error("Usage: node samples/github-release/download.mjs <release asset URL> <SHA-256>");
}
const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
    useLoggedInUser: false,
});
const errors = [];
try {
    await client.start();
    const api = client.internalRpc.gitHubRelease;
    const result = await api.downloadAsset({
        ...(await api.allocateRequestId()),
        assetUrl,
        expectedSha256,
    });
    if (result.kind !== "downloaded") {
        throw new Error(result.message ?? `Release download: ${result.kind}`);
    }
    process.stdout.write(`${JSON.stringify(result)}\n`);
} catch (error) {
    errors.push(error);
} finally {
    await client.stop().catch((error) => errors.push(error));
}
if (errors.length) throw new AggregateError(errors, "Release download example failed");
