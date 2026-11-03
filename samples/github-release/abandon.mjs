/* Copyright (c) Microsoft Corporation. All rights reserved. */

import assert from "node:assert/strict";
import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";

const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
    gitHubToken: process.env.GITHUB_TOKEN,
    useLoggedInUser: false,
});
let failure;
try {
    await client.start();
    // This source-checkout example uses the internal release contract.
    const api = client["internalRpc"].gitHubRelease;
    const allocation = await api.allocateRequestId();
    const result = await api.abandonRequest(allocation);
    assert.deepEqual(result, { abandoned: true });
    assert.deepEqual(await api.cancel(allocation), { canceled: false });
    assert.deepEqual(await api.abandonRequest(allocation), { abandoned: false });
    await client.ping();
    process.stdout.write(JSON.stringify({ abandoned: result.abandoned, connectionUsable: true }) + "\n");
} catch (error) {
    failure = error;
    throw error;
} finally {
    const errors = await client.stop();
    if (errors.length) {
        const cleanup = new AggregateError(errors, "SDK cleanup failed");
        if (!failure) throw cleanup;
        process.stderr.write(`${cleanup}\n`);
    }
}
