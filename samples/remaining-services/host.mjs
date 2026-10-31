// Copyright (c) Microsoft Corporation. All rights reserved.

import { mkdtemp, rm } from "node:fs/promises";
import assert from "node:assert/strict";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";

// Internal host RPCs are not part of the public SDK compatibility guarantee.
const home = await mkdtemp(join(tmpdir(), "copilot-services-"));
Object.assign(process.env, {
    COPILOT_HOME: home,
    COPILOT_OFFLINE: "true",
    COPILOT_GITHUB_TOKEN: "",
    GITHUB_TOKEN: "",
    GH_TOKEN: "",
    COPILOT_SDK_AUTH_TOKEN: "",
});
const client = new CopilotClient({
    connection: RuntimeConnection.forInProcess(),
    useLoggedInUser: false,
});
const errors = [];
try {
    await client.start();
    const api = client.internalRpc;
    const location = { homeDir: home, copilotHome: join(home, "selected-profile") };
    const registry = await api.agentRegistry.ensureDir(location);
    assert.equal(registry.error, undefined);
    const agents = await api.agentRegistry.listEntries(location);
    assert.equal(agents.error, undefined);
    assert.deepEqual(agents.entries, []);
    const removal = await api.agentRegistry.removeEntry({ ...location, pid: process.pid });
    assert.equal(removal.error, undefined);
    await api.tools.largeOutput.cleanup();
    process.stdout.write(
        `${JSON.stringify({
            registryDirectory: registry.path,
            agents: agents.entries.length,
        })}\n`,
    );
} catch (error) {
    errors.push(error);
} finally {
    await client.stop().catch((error) => errors.push(error));
    await rm(home, { recursive: true, force: true }).catch((error) => errors.push(error));
}
if (errors.length) throw new AggregateError(errors, "Remaining service example failed");
