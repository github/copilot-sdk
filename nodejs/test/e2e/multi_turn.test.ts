/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Multi-turn Tool Usage", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should use tool results from previous turns", async () => {
        // Write a file, then ask the model to read it and reason about its content
        await writeFile(join(workDir, "secret.txt"), "The magic number is 42.");
        const session = await client.createSession();

        const msg1 = await session.sendAndWait({
            prompt: "Read the file 'secret.txt' and tell me what the magic number is.",
        });
        expect(msg1?.data.content).toContain("42");

        // Follow-up that requires context from the previous turn
        const msg2 = await session.sendAndWait({
            prompt: "What is that magic number multiplied by 2?",
        });
        expect(msg2?.data.content).toContain("84");
    });

    it("should handle file creation then reading across turns", async () => {
        const session = await client.createSession();

        // First turn: create a file
        await session.sendAndWait({
            prompt: "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'.",
        });

        // Second turn: read the file
        const msg = await session.sendAndWait({
            prompt: "Read the file 'greeting.txt' and tell me its exact contents.",
        });
        expect(msg?.data.content).toContain("Hello from multi-turn test");
    });
});
