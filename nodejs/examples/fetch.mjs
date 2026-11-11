/* Copyright (c) Microsoft Corporation. All rights reserved. */

// Run with the matching checkout's runtime: node src/sdk/nodejs/examples/fetch.mjs /path/to/copilot-runtime
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { once } from "node:events";
import { mkdtemp, readFile, rm, stat } from "node:fs/promises";
import { createServer } from "node:http";
import { join, resolve } from "node:path";
import { CopilotClient, RuntimeConnection } from "../dist/index.js";

const runtimePath = process.argv[2];
if (!runtimePath) throw new Error("Usage: node fetch.mjs <same-checkout copilot-runtime path>");
const home = await mkdtemp(resolve(".copilot-fetch-example-"));
execFileSync("git", ["init", "--quiet"], { cwd: home, windowsHide: true });
const bytes = Buffer.alloc(3 * 1024 * 1024 + 17);
const streamBytes = Buffer.from([0, 255, 128, 13, 10, 7]);
for (let i = 0; i < bytes.length; i++) bytes[i] = i % 256;
const server = createServer((request, response) => {
    if (request.url === "/file") {
        response.writeHead(503, { "content-type": "application/octet-stream" });
        response.end(bytes);
    } else if (request.url === "/stream") {
        response.writeHead(206, { "content-type": "application/octet-stream" });
        response.end(streamBytes);
    } else {
        response.writeHead(200, { "content-type": "text/plain; charset=utf-8" });
        response.end("Generic SDK fetch: €");
    }
});
let client;
let file;
try {
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
    const origin = `http://127.0.0.1:${server.address().port}`;
    client = new CopilotClient({
        connection: RuntimeConnection.forStdio({
            path: resolve(runtimePath),
            env: { ...process.env, COPILOT_HOME: home },
        }),
        workingDirectory: home,
        gitHubToken: "ghs_fetch_example_local_only",
        useLoggedInUser: false,
    });
    await client.start();
    const request = await client.rpc.fetch.create();
    const text = await client.rpc.fetch.send({
        responseMode: "text",
        ...request,
        url: `${origin}/text`,
        proxyEnv: {},
    });
    assert.equal(text.status, 200);
    assert.equal(text.bodyText, "Generic SDK fetch: €");
    const stream = await client.rpc.fetch.create();
    let streamStatus;
    let streamReads = 0;
    try {
        const headers = await client.rpc.fetch.send({
            responseMode: "stream",
            ...stream,
            url: `${origin}/stream`,
            proxyEnv: {},
        });
        streamStatus = headers.status;
        const chunks = [];
        for (;;) {
            // Streaming bytes cross JSON-RPC. Use file mode for large bodies.
            const chunk = await client.rpc.fetch.read(stream);
            streamReads++;
            if (chunk.done) break;
            chunks.push(Buffer.from(chunk.body, "base64"));
        }
        assert.equal(streamStatus, 206);
        assert.deepEqual(Buffer.concat(chunks), streamBytes);
    } finally {
        await client.rpc.fetch.cancel(stream);
    }
    const download = await client.rpc.fetch.create();
    const destinationPath = join(home, "response.bin");
    file = await client.rpc.fetch.send({
        ...download,
        url: `${origin}/file`,
        proxyEnv: {},
        responseMode: "file",
        destinationPath,
    });
    assert.equal(file.status, 503);
    assert.equal(file.sizeBytes, bytes.length);
    assert.deepEqual(Object.keys(file).sort(), [
        "endedAtUnverifiedEof",
        "headers",
        "path",
        "responseMode",
        "sizeBytes",
        "status",
        "statusText",
        "url",
    ]);
    assert(Buffer.byteLength(JSON.stringify(file)) < 2048);
    // This example runs the runtime locally. Remote clients cannot read its path directly.
    assert.deepEqual(await readFile(file.path), bytes);
    assert.deepEqual(await client.rpc.fetch.cancel(download), { canceled: false });
    await client.stop();
    assert.deepEqual(await readFile(file.path), bytes);
    // Completed files survive cancellation and disconnect. The caller removes them.
    await rm(file.path);
    await assert.rejects(stat(file.path), { code: "ENOENT" });
    console.log(
        JSON.stringify({
            textStatus: text.status,
            bodyText: text.bodyText,
            streamStatus,
            streamedBytes: streamBytes.length,
            streamReads,
            streamResponseBytesInRpc: true,
            downloadStatus: file.status,
            downloadedBytes: file.sizeBytes,
            rpcResultBytes: Buffer.byteLength(JSON.stringify(file)),
            downloadResponseBytesInRpc: false,
            completedFileSurvivedDisconnect: true,
            callerDeletedFile: true,
        })
    );
    file = undefined;
} finally {
    try {
        if (file) await rm(file.path, { force: true });
    } finally {
        try {
            await client?.stop();
        } finally {
            server.closeAllConnections();
            if (server.listening) await new Promise((resolve) => server.close(resolve));
            await rm(home, { recursive: true, force: true });
        }
    }
}
