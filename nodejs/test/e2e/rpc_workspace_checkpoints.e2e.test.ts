/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync, readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session workspace checkpoint RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should list no checkpoints for fresh session", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const result = await session.rpc.workspaces.listCheckpoints();
            expect(result.checkpoints).toEqual([]);
        } finally {
            await session.disconnect();
        }
    });

    it("should return null or empty content for unknown checkpoint", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            // A high but 32-bit-safe checkpoint number that will never exist in a fresh
            // session, so the read reports the checkpoint as missing.
            const result = await session.rpc.workspaces.readCheckpoint({ number: 4294967294 });
            expect(result.content ?? "").toBe("");
        } finally {
            await session.disconnect();
        }
    });

    it("should return typed workspace diff result", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const result = await session.rpc.workspaces.diff({ mode: "unstaged" });
            expect(result.requestedMode).toBe("unstaged");
            expect(["unstaged", "branch"]).toContain(result.mode);
            expect(Array.isArray(result.changes)).toBe(true);
            for (const change of result.changes) {
                expect(change.path.trim()).toBeTruthy();
                expect(["added", "modified", "deleted", "renamed"]).toContain(change.changeType);
                expect(typeof change.diff).toBe("string");
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should save large paste and expose readable content", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const content = "Large paste payload 🚀\n".repeat(512);
            const result = await session.rpc.workspaces.saveLargePaste({ content });
            const saved = result.saved;

            expect(saved).not.toBeNull();
            expect(saved!.filename.trim()).toBeTruthy();
            expect(saved!.filePath.trim()).toBeTruthy();
            expect(saved!.sizeBytes).toBe(Buffer.byteLength(content, "utf8"));

            try {
                const read = await session.rpc.workspaces.readFile({ path: saved!.filename });
                expect(read.content).toBe(content);
            } catch (err: unknown) {
                expect(existsSync(saved!.filePath)).toBe(true);
                expect(readFileSync(saved!.filePath, "utf8")).toBe(content);
                expect(err).toBeDefined();
            }
        } finally {
            await session.disconnect();
        }
    });
});
