/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";

const url = process.argv[2];
if (!url) throw new Error("Usage: node samples/github-fetch/request.mjs <GitHub API URL>");
const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
    gitHubToken: process.env.GITHUB_TOKEN,
    useLoggedInUser: false,
});
let failure;
try {
    await client.start();
    const api = client.internalRpc.gitHubFetch;
    const result = await api.request({
        ...(await api.allocateRequestId()),
        method: "GET",
        url,
        headers: [],
        apiName: "GitHub source example",
        retryOptions: { maxRetries: 0, enableCircuitBreaker: false },
    });
    if (result.kind !== "response") throw new Error(result.message);
    process.stdout.write(
        JSON.stringify({
            status: result.status,
            body: Buffer.from(result.bodyBase64, "base64").toString("utf8"),
        }) + "\n",
    );
    if (result.status < 200 || result.status >= 300) process.exitCode = 1;
} catch (error) {
    failure = error;
    throw error;
} finally {
    try {
        await client.stop();
    } catch (error) {
        if (!failure) throw error;
        process.stderr.write(`SDK cleanup failed: ${String(error)}\n`);
    }
}
