/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import type { ChildProcess } from "node:child_process";
import { resolve } from "node:path";
import {
  approveAll,
  CopilotClient,
  RuntimeConnection,
} from "../../nodejs/src/index.js";
import {
  assertHostStopped,
  assertRuntimeChild,
  connectAhp,
  createAhpSession,
  localHostArtifacts,
  streamedTurn,
} from "../../nodejs/test/e2e/harness/runtimeHost.js";

if (process.platform !== "linux")
  throw new Error("This source-build topology smoke requires Linux");
const artifacts = localHostArtifacts();
const workDir = resolve(process.argv[2] ?? ".");
const githubToken = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
assert(githubToken, "Set GITHUB_TOKEN or GH_TOKEN for the live smoke");
const owner = new CopilotClient({
  connection: RuntimeConnection.forTcp({ path: artifacts.runtimePath }),
  workingDirectory: workDir,
  gitHubToken: githubToken,
  env: {
    ...process.env,
    ...artifacts.env,
    GITHUB_TOKEN: githubToken,
    GH_TOKEN: githubToken,
  },
});
try {
  await using sdkSession = await owner.createSession({
    onPermissionRequest: approveAll,
    model: "claude-sonnet-5",
  });
  await using host = await owner.startHost();
  const ahp = await connectAhp(host);
  try {
    const session = await createAhpSession(ahp, workDir, githubToken);
    const runtime = (owner as unknown as { cliProcess: ChildProcess })
      .cliProcess;
    assert(runtime.pid);
    await assertRuntimeChild(host, runtime.pid, artifacts);
    console.log(
      `SDK app ${process.pid} -> runtime ${runtime.pid} -> copilotd-lite ${host.pid}`,
    );
    const listener = new URL(host.url);
    listener.search = "";
    console.log(
      `AHP listener: ${listener} (connection token intentionally not printed)`,
    );
    const [response, sdkResponse] = await Promise.all([
      streamedTurn(
        ahp.client,
        session.chatUri,
        session.subscription,
        "What is 2+2?",
      ),
      sdkSession.sendAndWait({ prompt: "What is 2+2?" }),
    ]);
    assert(response.deltas > 0, "AHP turn must include streaming deltas");
    assert(response.text.includes("4"));
    assert(sdkResponse?.data.content.includes("4"));
    const ids = (await owner.listSessions()).map((item) => item.sessionId);
    assert(
      ids.includes(session.sessionId),
      "AHP session must live in the owner's runtime",
    );
    assert(ids.includes(sdkSession.sessionId));
    console.log(
      `Standard AHP response: ${response.text} (${response.deltas} streamed deltas)`,
    );
    console.log(
      `SDK response on the same runtime: ${sdkResponse.data.content}`,
    );
    await host.dispose();
    await assertHostStopped(host, ahp);
    await sdkSession.getEvents();
    console.log(
      "Listener closed, AHP client disconnected, child reaped; SDK session survives.",
    );
  } finally {
    await ahp.client.shutdown();
  }
} finally {
  await owner.stop();
}
