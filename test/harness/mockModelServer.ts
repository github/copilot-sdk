/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { MockModelEndpoint } from "./mockModelEndpoint";

// Standalone entrypoint for the mock BYOK model (OpenAI-compatible) endpoint.
// Intended to be spawned as a child process by E2E tests in any SDK language:
// read the `Listening:` line below for the base + control URLs, point a BYOK
// provider's baseUrl at it, then GET the recordedUrl to inspect the credential
// the runtime injected and POST the stopUrl (or kill the process) when done.
// Kept separate from the CAPI record/replay proxy so it can be used on its own.

const endpoint = new MockModelEndpoint();
const info = await endpoint.start();

// Single-line, machine-parseable startup banner mirroring server.ts.
console.log(`Listening: ${JSON.stringify(info)}`);

const shutdown = async () => {
  await endpoint.stop();
  process.exit(0);
};
process.on("SIGTERM", () => void shutdown());
process.on("SIGINT", () => void shutdown());
