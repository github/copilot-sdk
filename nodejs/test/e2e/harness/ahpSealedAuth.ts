/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { createHash, randomBytes } from "node:crypto";
import { AeadId, CipherSuite, KdfId, KemId } from "hpke-js";

export const AUTH_ALGORITHM = "hpke-x25519-hkdf-sha256-aes256gcm";

function object(value: unknown): Record<string, unknown> {
    assert(value !== null && typeof value === "object" && !Array.isArray(value));
    return value as Record<string, unknown>;
}

// These tests own the direct listener endpoint. Its actual handshake supplies
// the connection binding; the root advertises the recipient, not initialize.
// Matches protocol_base/ahp/sealing.rs and copilot-host spec/encryption.
export async function sealAhpAuthToken(
    initializeMeta: unknown,
    rootMeta: unknown,
    resource: string,
    token: string
): Promise<string> {
    const initialized = object(initializeMeta);
    const required = initialized["copilot.encryptionRequired"];
    assert(Array.isArray(required) && required.includes("auth-token"));
    const binding = object(initialized["copilot.authChallenge"]);
    assert.equal(binding.required, true);
    assert.equal(typeof binding.challenge, "string");
    assert.match(binding.challenge as string, /^[0-9a-f]{32}$/);
    assert(typeof binding.responseMaxAgeSeconds === "number" && binding.responseMaxAgeSeconds > 0);
    const keys = object(rootMeta)["copilot.encryptionKeys"];
    assert(Array.isArray(keys), "Root must advertise encryption keys");
    const key = keys
        .map(object)
        .find((entry) => entry.use === "auth-token" && entry.algorithm === AUTH_ALGORITHM);
    assert(key, "Host must advertise an auth-token HPKE key");
    assert.equal(typeof key.publicKey, "string");
    const publicKey = Buffer.from(key.publicKey as string, "base64");
    assert.equal(publicKey.length, 32);
    assert.equal(
        key.keyId,
        createHash("sha256").update(publicKey).digest().subarray(0, 8).toString("base64url")
    );
    const suite = new CipherSuite({
        kem: KemId.DhkemX25519HkdfSha256,
        kdf: KdfId.HkdfSha256,
        aead: AeadId.Aes256Gcm,
    });
    const recipient = await suite.kem.deserializePublicKey(new Uint8Array(publicKey));
    const sender = await suite.createSenderContext({ recipientPublicKey: recipient });
    const ciphertext = await sender.seal(
        new TextEncoder().encode(
            JSON.stringify({
                cty: "text",
                ctx: {
                    purpose: "auth-token",
                    resource,
                    connection: {
                        challenge: binding.challenge,
                        nonce: randomBytes(16).toString("hex"),
                        issuedAt: Math.floor(Date.now() / 1000),
                    },
                },
                value: token,
            })
        )
    );
    const box = Buffer.concat([Buffer.from(sender.enc), Buffer.from(ciphertext)]);
    return `copilot-sealed.v1.${key.keyId}.${box.toString("base64url")}`;
}
