/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { MockIdentityEndpoint } from "./mockIdentityEndpoint";

// Standalone entrypoint for the mock Azure managed identity token endpoint.
// Intended to be spawned as a child process by E2E tests in any SDK language:
// read the `Listening:` line below for the endpoint + control URLs, set
// IDENTITY_ENDPOINT / IDENTITY_HEADER from it, then POST the stopUrl (or kill
// the process) when done. Kept separate from the CAPI record/replay proxy so it
// can be used on its own.

const endpoint = new MockIdentityEndpoint();
const info = await endpoint.start();

// Single-line, machine-parseable startup banner mirroring server.ts.
console.log(`Listening: ${JSON.stringify(info)}`);

const shutdown = async () => {
  await endpoint.stop();
  process.exit(0);
};
process.on("SIGTERM", () => void shutdown());
process.on("SIGINT", () => void shutdown());
