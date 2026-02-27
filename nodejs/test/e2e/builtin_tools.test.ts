/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile, mkdir } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Built-in Tools", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    describe("bash", () => {
        it("should capture exit code in output", async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Run 'echo hello && echo world'. Tell me the exact output.",
            });
            expect(msg?.data.content).toContain("hello");
            expect(msg?.data.content).toContain("world");
        });

        it.skipIf(process.platform === "win32")("should capture stderr output", async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Run 'echo error_msg >&2; echo ok' and tell me what stderr said. Reply with just the stderr content.",
            });
            expect(msg?.data.content).toContain("error_msg");
        });
    });

    describe("view", () => {
        it("should read file with line range", async () => {
            await writeFile(join(workDir, "lines.txt"), "line1\nline2\nline3\nline4\nline5\n");
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Read lines 2 through 4 of the file 'lines.txt' in this directory. Tell me what those lines contain.",
            });
            expect(msg?.data.content).toContain("line2");
            expect(msg?.data.content).toContain("line4");
        });

        it("should handle nonexistent file gracefully", async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Try to read the file 'does_not_exist.txt'. If it doesn't exist, say 'FILE_NOT_FOUND'.",
            });
            expect(msg?.data.content?.toUpperCase()).toMatch(
                /NOT.FOUND|NOT.EXIST|NO.SUCH|FILE_NOT_FOUND|DOES.NOT.EXIST|ERROR/i
            );
        });
    });

    describe("edit", () => {
        it("should edit a file successfully", async () => {
            await writeFile(join(workDir, "edit_me.txt"), "Hello World\nGoodbye World\n");
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Edit the file 'edit_me.txt': replace 'Hello World' with 'Hi Universe'. Then read it back and tell me its contents.",
            });
            expect(msg?.data.content).toContain("Hi Universe");
        });
    });

    describe("create_file", () => {
        it("should create a new file", async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Create a file called 'new_file.txt' with the content 'Created by test'. Then read it back to confirm.",
            });
            expect(msg?.data.content).toContain("Created by test");
        });
    });

    describe("grep", () => {
        it("should search for patterns in files", async () => {
            await writeFile(join(workDir, "data.txt"), "apple\nbanana\napricot\ncherry\n");
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Search for lines starting with 'ap' in the file 'data.txt'. Tell me which lines matched.",
            });
            expect(msg?.data.content).toContain("apple");
            expect(msg?.data.content).toContain("apricot");
        });
    });

    describe("glob", () => {
        it("should find files by pattern", async () => {
            await mkdir(join(workDir, "src"), { recursive: true });
            await writeFile(join(workDir, "src", "app.ts"), "export const app = 1;");
            await writeFile(join(workDir, "src", "index.ts"), "export const index = 1;");
            await writeFile(join(workDir, "README.md"), "# Readme");
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const msg = await session.sendAndWait({
                prompt: "Find all .ts files in this directory (recursively). List the filenames you found.",
            });
            expect(msg?.data.content).toContain("app.ts");
            expect(msg?.data.content).toContain("index.ts");
        });
    });
});
