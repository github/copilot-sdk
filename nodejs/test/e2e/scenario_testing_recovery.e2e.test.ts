/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { rimraf } from "rimraf";
import { describe, expect, it } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";

const FAKE_RECOVERY_CLI = `const fs = require("fs");

const captureIndex = process.argv.indexOf("--capture-file");
const captureFile = process.argv[captureIndex + 1];
const modeIndex = process.argv.indexOf("--scenario");
const scenario = process.argv[modeIndex + 1];
const requests = [];
let resumeAttempts = 0;
let buffer = Buffer.alloc(0);

function saveCapture() {
  fs.writeFileSync(captureFile, JSON.stringify(requests));
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write(\`Content-Length: \${Buffer.byteLength(body, "utf8")}\\r\\n\\r\\n\${body}\`);
}

function writeError(id, code, message, data) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, error: { code, message, data } });
  process.stdout.write(\`Content-Length: \${Buffer.byteLength(body, "utf8")}\\r\\n\\r\\n\${body}\`);
}

function handle(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) {
    return;
  }
  requests.push({ method: message.method, params: message.params ?? null });
  saveCapture();

  if (message.method === "connect") {
    writeResponse(message.id, { ok: true, protocolVersion: 4, version: "fake-recovery" });
    return;
  }

  if (message.method === "session.create") {
    writeResponse(message.id, {
      sessionId: message.params.sessionId,
      workspacePath: null,
      capabilities: null
    });
    return;
  }

  if (message.method === "session.resume") {
    resumeAttempts += 1;
    if (scenario === "resume-retry" && resumeAttempts === 1) {
      writeError(message.id, -32001, "Session not found before acceptance", {
        kind: "not_found",
        phase: "preacceptance",
        sessionId: message.params.sessionId
      });
      return;
    }
    writeResponse(message.id, {
      sessionId: message.params.sessionId,
      workspacePath: null,
      capabilities: null
    });
    return;
  }

  if (message.method === "session.delete" && scenario === "delete-not-found") {
    writeError(message.id, -32001, "Session not found during cleanup", {
      kind: "not_found",
      operation: "delete",
      sessionId: message.params.sessionId
    });
    return;
  }

  if (message.method === "session.send" && scenario === "ambiguous-send") {
    writeError(message.id, -32098, "Transport lost after request acceptance", {
      kind: "ambiguous_transport_loss",
      accepted: true
    });
    return;
  }

  if (message.method === "session.detach") {
    writeResponse(message.id, { success: true });
    return;
  }

  writeResponse(message.id, null);
}

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\\r\\n\\r\\n");
    if (headerEnd < 0) {
      return;
    }
    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\\s*(\\d+)/i.exec(header);
    if (!match) {
      throw new Error("Missing Content-Length header");
    }
    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) {
      return;
    }
    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handle(JSON.parse(body));
  }
}

process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();
`;

type FakeClient = {
    client: CopilotClient;
    capturePath: string;
    directory: string;
};

function createFakeClient(scenario: string): FakeClient {
    const directory = mkdtempSync(join(tmpdir(), "copilot-node-scenario-recovery-"));
    const cliPath = join(directory, "fake-recovery-cli.js");
    const capturePath = join(directory, "capture.json");
    writeFileSync(cliPath, FAKE_RECOVERY_CLI);
    return {
        client: new CopilotClient({
            workingDirectory: directory,
            baseDirectory: directory,
            env: { ...process.env, COPILOT_HOME: directory },
            connection: RuntimeConnection.forStdio({
                path: cliPath,
                args: ["--capture-file", capturePath, "--scenario", scenario],
            }),
            useLoggedInUser: false,
        }),
        capturePath,
        directory,
    };
}

async function disposeFakeClient(fake: FakeClient): Promise<void> {
    await fake.client.forceStop();
    await rimraf(fake.directory, { maxRetries: 10, retryDelay: 100 });
}

function capturedRequests(fake: FakeClient): Array<{
    method: string;
    params: Record<string, unknown> | null;
}> {
    return JSON.parse(readFileSync(fake.capturePath, "utf8")) as Array<{
        method: string;
        params: Record<string, unknown> | null;
    }>;
}

describe("Scenario testing lifecycle recovery", () => {
    it("should allow caller retry after preacceptance session not found", async () => {
        const fake = createFakeClient("resume-retry");
        try {
            await fake.client.start();
            await expect(
                fake.client.resumeSession("retry-session", {
                    onPermissionRequest: approveAll,
                })
            ).rejects.toMatchObject({
                code: -32001,
                data: {
                    kind: "not_found",
                    phase: "preacceptance",
                    sessionId: "retry-session",
                },
            });

            const resumed = await fake.client.resumeSession("retry-session", {
                onPermissionRequest: approveAll,
            });
            expect(resumed.sessionId).toBe("retry-session");

            const resumeRequests = capturedRequests(fake).filter(
                (request) => request.method === "session.resume"
            );
            expect(resumeRequests).toHaveLength(2);
            await resumed.disconnect();
        } finally {
            await disposeFakeClient(fake);
        }
    });

    it("should classify delete not found for scenario cleanup", async () => {
        const fake = createFakeClient("delete-not-found");
        try {
            await fake.client.start();
            await expect(
                fake.client.deleteSession("missing-cleanup-session")
            ).rejects.toMatchObject({
                code: -32001,
                data: {
                    kind: "not_found",
                    operation: "delete",
                    sessionId: "missing-cleanup-session",
                },
            });
            expect(
                capturedRequests(fake).filter((request) => request.method === "session.delete")
            ).toHaveLength(1);
        } finally {
            await disposeFakeClient(fake);
        }
    });

    it.each([undefined, "enqueue", "immediate"] as const)(
        "should not replay scenario send after ambiguous transport loss (%s)",
        async (mode) => {
            const fake = createFakeClient("ambiguous-send");
            try {
                await fake.client.start();
                const session = await fake.client.createSession({
                    onPermissionRequest: approveAll,
                });
                await expect(
                    session.send({
                        prompt: "AMBIGUOUS_SEND_MUST_NOT_REPLAY",
                        ...(mode === undefined ? {} : { mode }),
                    })
                ).rejects.toMatchObject({
                    code: -32098,
                    data: {
                        kind: "ambiguous_transport_loss",
                        accepted: true,
                    },
                });

                const sends = capturedRequests(fake).filter(
                    (request) => request.method === "session.send"
                );
                expect(sends).toHaveLength(1);
                expect(sends[0].params).toMatchObject({
                    prompt: "AMBIGUOUS_SEND_MUST_NOT_REPLAY",
                    ...(mode === undefined ? {} : { mode }),
                });
            } finally {
                await disposeFakeClient(fake);
            }
        }
    );
});
