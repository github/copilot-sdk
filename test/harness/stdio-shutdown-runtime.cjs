/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Shared SDK shutdown fixture: node <script> <cleanup-marker> <mode> <pid-file>.
const fs = require("node:fs");
const { rename, writeFile } = require("node:fs/promises");

const [marker, mode, pidFile] = process.argv.slice(2);
if (!marker || !pidFile || !["stop", "dispose", "force", "fallback", "start-failure"].includes(mode)) {
    throw new Error("Expected cleanup marker, shutdown mode, and PID file arguments");
}
fs.writeFileSync(pidFile, String(process.pid));
if (mode === "fallback" || mode === "start-failure") {
    setInterval(() => {}, 1000);
}

let buffered = Buffer.alloc(0);
let shutdownRequested = false;
process.stdin.on("data", (chunk) => {
    buffered = Buffer.concat([buffered, chunk]);
    while (true) {
        const headerEnd = buffered.indexOf("\r\n\r\n");
        if (headerEnd < 0) return;
        const header = /Content-Length:\s*(\d+)/i.exec(buffered.subarray(0, headerEnd).toString());
        if (!header) throw new Error("Missing Content-Length header");
        const length = Number(header[1]);
        if (buffered.length < headerEnd + 4 + length) return;
        const message = JSON.parse(buffered.subarray(headerEnd + 4, headerEnd + 4 + length));
        buffered = buffered.subarray(headerEnd + 4 + length);
        if (message.id === undefined) continue;
        let result;
        switch (message.method) {
            case "connect":
                result = {
                    ok: true,
                    protocolVersion: mode === "start-failure" ? -1 : 3,
                    version: "test",
                };
                break;
            case "ping":
                result = { message: message.params?.message, timestamp: Date.now(), protocolVersion: 3 };
                break;
            case "runtime.shutdown":
                shutdownRequested = true;
                result = {};
                break;
            default:
                throw new Error(`Unexpected method: ${message.method}`);
        }
        const body = JSON.stringify({ jsonrpc: "2.0", id: message.id, result });
        process.stdout.write(`Content-Length: ${Buffer.byteLength(body)}\r\n\r\n${body}`);
    }
});
process.stdin.on("end", async () => {
    if (!shutdownRequested) throw new Error("Missing runtime.shutdown");
    // Like the native wrapper, finalize host output only after transport EOF.
    // Marker existence signals a completed write to tests that force-stop the child.
    await writeFile(`${marker}.tmp`, '{"type":"span"}\n');
    await rename(`${marker}.tmp`, marker);
});
