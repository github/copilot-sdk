/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { createHash, randomBytes } from "node:crypto";
import { once } from "node:events";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";
import { AeadId, CipherSuite, KdfId, KemId } from "hpke-js";
import { expect, it } from "vitest";
import { WebSocketServer } from "ws";
import { withDeadline } from "./e2e/harness/ahpClient.js";
import { AUTH_ALGORITHM, sealAhpAuthToken } from "./e2e/harness/ahpSealedAuth.js";

it("seals connect, create reauthentication and reconnect in the shared cross-SDK AHP subprocess", async () => {
    const token = "fake-token-for-e2e-tests";
    const resource = "https://api.github.com";
    const suite = new CipherSuite({
        kem: KemId.DhkemX25519HkdfSha256,
        kdf: KdfId.HkdfSha256,
        aead: AeadId.Aes256Gcm,
    });

    const recipient = await suite.kem.generateKeyPair();
    const publicKey = Buffer.from(await suite.kem.serializePublicKey(recipient.publicKey));
    const keyId = createHash("sha256")
        .update(publicKey)
        .digest()
        .subarray(0, 8)
        .toString("base64url");
    const accepted: { challenge: string; nonce: string }[] = [];
    const rejected: unknown[] = [];
    const server = new WebSocketServer({ host: "127.0.0.1", port: 0 });
    server.on("connection", (socket) => {
        const challenge = randomBytes(16).toString("hex");
        socket.on("message", async (data) => {
            const request = JSON.parse(data.toString());
            try {
                let result: unknown = {};
                switch (request.method) {
                    case "initialize":
                        expect(request.params.protocolVersions).toEqual(["0.9.0"]);
                        result = {
                            protocolVersion: "0.9.0",
                            _meta: {
                                "copilot.encryptionRequired": ["auth-token", "mcp-auth-token"],
                                "copilot.authChallenge": {
                                    challenge,
                                    responseMaxAgeSeconds: 300,
                                    required: true,
                                },
                            },
                        };
                        break;
                    case "subscribe": {
                        const channel = request.params.channel;
                        const state =
                            channel === "ahp-root://"
                                ? {
                                      agents: [
                                          {
                                              provider: "copilot",
                                              protectedResources: [
                                                  { resource, resource_name: "GitHub API" },
                                              ],
                                          },
                                      ],
                                      _meta: {
                                          "copilot.encryptionKeys": [
                                              {
                                                  keyId,
                                                  use: "auth-token",
                                                  algorithm: AUTH_ALGORITHM,
                                                  publicKey: publicKey.toString("base64"),
                                              },
                                          ],
                                      },
                                  }
                                : channel.startsWith("ahp-session:")
                                  ? { defaultChat: "ahp-chat:/fixture" }
                                  : { turns: [] };
                        result = { snapshot: { resource: channel, fromSeq: 0, state } };
                        break;
                    }
                    case "authenticate": {
                        expect(request.params.resource).toBe(resource);
                        const prefix = `copilot-sealed.v1.${keyId}.`;
                        expect(request.params.token.startsWith(prefix)).toBe(true);
                        expect(request.params.token).not.toContain(token);
                        const box = Buffer.from(
                            request.params.token.slice(prefix.length),
                            "base64url"
                        );
                        const receiver = await suite.createRecipientContext({
                            recipientKey: recipient.privateKey,
                            enc: new Uint8Array(box.subarray(0, 32)),
                        });
                        const plaintext = JSON.parse(
                            Buffer.from(
                                await receiver.open(new Uint8Array(box.subarray(32)))
                            ).toString()
                        );
                        expect(plaintext).toMatchObject({
                            cty: "text",
                            value: token,
                            ctx: { purpose: "auth-token", resource, connection: { challenge } },
                        });
                        const binding = plaintext.ctx.connection;
                        expect(binding.nonce).toMatch(/^[0-9a-f]{32}$/);
                        expect(
                            Math.abs(Math.floor(Date.now() / 1000) - binding.issuedAt)
                        ).toBeLessThan(300);
                        expect(accepted.some((previous) => previous.nonce === binding.nonce)).toBe(
                            false
                        );
                        accepted.push({ challenge, nonce: binding.nonce });
                        break;
                    }
                    case "createSession":
                    case "unsubscribe":
                        break;
                    default:
                        throw new Error(`Unexpected AHP method ${request.method}`);
                }
                if (request.id !== undefined)
                    socket.send(
                        JSON.stringify({
                            jsonrpc: "2.0",
                            id: request.id,
                            result,
                        })
                    );
            } catch (error) {
                rejected.push(error);
                socket.send(
                    JSON.stringify({
                        jsonrpc: "2.0",
                        id: request.id,
                        error: {
                            code: -32602,
                            message: "fixture rejected unbound authentication",
                        },
                    })
                );
            }
        });
    });
    await once(server, "listening");
    const address = server.address();
    assert(address && typeof address !== "string");
    // Exactly the entry point launched by .NET, Java, Go and Python harnesses.
    const driver = spawn(
        process.execPath,
        ["--import", "tsx", "test/e2e/harness/ahpTestDriver.ts"],
        { cwd: fileURLToPath(new URL("../", import.meta.url)), stdio: "pipe" }
    );
    const exited = once(driver, "exit");
    let diagnostics = "";
    driver.stderr.on("data", (data) => {
        diagnostics += data.toString();
    });
    const lines = createInterface({ input: driver.stdout })[Symbol.asyncIterator]();
    async function read() {
        const line = await withDeadline(lines.next(), "shared AHP subprocess response");
        assert(!line.done, "shared AHP subprocess closed output");
        return JSON.parse(line.value);
    }
    async function request(command: Record<string, unknown>) {
        driver.stdin.write(JSON.stringify(command) + "\n");
        const response = await read();
        expect(rejected).toEqual([]);
        expect(response).not.toHaveProperty("error");
        return response.result;
    }
    try {
        expect(await read()).toEqual({ ready: true });
        const command = {
            op: "connect",
            url: `ws://127.0.0.1:${address.port}`,
            githubToken: token,
            clientId: "cross-sdk-sealed-auth",
        };
        const connected = await request(command);
        const created = await request({
            op: "create",
            clientId: connected.clientId,
            workDir: fileURLToPath(new URL(".", import.meta.url)),
        });
        expect(created.sessionUri).toMatch(/^ahp-session:/);
        await request({ op: "close", clientId: connected.clientId });
        const reconnected = await request(command);
        await request({ op: "close", clientId: reconnected.clientId });
        expect(accepted).toHaveLength(3);
        expect(accepted[0].challenge).toBe(accepted[1].challenge);
        expect(accepted[2].challenge).not.toBe(accepted[0].challenge);
    } finally {
        driver.stdin.end();
        try {
            const [code] = await withDeadline(exited, "shared AHP subprocess exit", 10_000);
            expect(code, diagnostics).toBe(0);
        } finally {
            if (driver.exitCode === null && driver.signalCode === null) {
                driver.kill();
                await withDeadline(exited, "reap shared AHP subprocess", 10_000);
            }
            for (const socket of server.clients) socket.terminate();
            await new Promise<void>((resolve, reject) => {
                server.close((error) => (error ? reject(error) : resolve()));
            });
        }
    }
});

it("seals fixture authentication to the advertised key, resource and live challenge", async () => {
    const suite = new CipherSuite({
        kem: KemId.DhkemX25519HkdfSha256,
        kdf: KdfId.HkdfSha256,
        aead: AeadId.Aes256Gcm,
    });
    const recipient = await suite.kem.generateKeyPair();
    const publicKey = Buffer.from(await suite.kem.serializePublicKey(recipient.publicKey));
    const keyId = createHash("sha256")
        .update(publicKey)
        .digest()
        .subarray(0, 8)
        .toString("base64url");
    const challenge = "00112233445566778899aabbccddeeff";
    const initializeMeta = {
        "copilot.encryptionRequired": ["auth-token", "mcp-auth-token"],
        "copilot.authChallenge": { challenge, responseMaxAgeSeconds: 300, required: true },
    };
    const rootMeta = {
        "copilot.encryptionKeys": [
            {
                keyId,
                use: "auth-token",
                algorithm: AUTH_ALGORITHM,
                publicKey: publicKey.toString("base64"),
            },
        ],
    };
    const nonces = new Set<string>();
    for (let attempt = 0; attempt < 2; attempt++) {
        const before = Math.floor(Date.now() / 1000);
        const sealed = await sealAhpAuthToken(
            initializeMeta,
            rootMeta,
            "https://api.github.com",
            "fixture-only-token"
        );
        const prefix = `copilot-sealed.v1.${keyId}.`;
        expect(sealed.startsWith(prefix)).toBe(true);
        expect(sealed).not.toContain("fixture-only-token");
        const box = Buffer.from(sealed.slice(prefix.length), "base64url");
        const receiver = await suite.createRecipientContext({
            recipientKey: recipient.privateKey,
            enc: new Uint8Array(box.subarray(0, 32)),
        });
        const value = JSON.parse(
            Buffer.from(await receiver.open(new Uint8Array(box.subarray(32)))).toString()
        );
        expect(value).toMatchObject({
            cty: "text",
            value: "fixture-only-token",
            ctx: {
                purpose: "auth-token",
                resource: "https://api.github.com",
                connection: { challenge },
            },
        });
        expect(value.ctx.connection.nonce).toMatch(/^[0-9a-f]{32}$/);
        expect(value.ctx.connection.issuedAt).toBeGreaterThanOrEqual(before);
        expect(value.ctx.connection.issuedAt).toBeLessThanOrEqual(Math.floor(Date.now() / 1000));
        nonces.add(value.ctx.connection.nonce);
    }
    expect(nonces.size).toBe(2);
    await expect(
        sealAhpAuthToken({}, rootMeta, "https://api.github.com", "fixture-only-token")
    ).rejects.toThrow();
    await expect(
        sealAhpAuthToken(
            initializeMeta,
            {
                "copilot.encryptionKeys": [
                    {
                        keyId,
                        use: "mcp-auth-token",
                        algorithm: AUTH_ALGORITHM,
                        publicKey: publicKey.toString("base64"),
                    },
                ],
            },
            "https://api.github.com",
            "fixture-only-token"
        )
    ).rejects.toThrow();
});
