/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("System message transform", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should invoke transform callbacks with section content", async () => {
        const transformedSections: Record<string, string> = {};

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    identity: {
                        action: (content: string) => {
                            transformedSections["identity"] = content;
                            // Pass through unchanged
                            return content;
                        },
                    },
                    tone: {
                        action: (content: string) => {
                            transformedSections["tone"] = content;
                            return content;
                        },
                    },
                },
            },
        });

        await writeFile(join(workDir, "test.txt"), "Hello transform!");

        await session.sendAndWait({
            prompt: "Read the contents of test.txt and tell me what it says",
        });

        // Transform callbacks should have been invoked with real section content
        expect(Object.keys(transformedSections).length).toBe(2);
        expect(transformedSections["identity"]).toBeDefined();
        expect(transformedSections["identity"]!.length).toBeGreaterThan(0);
        expect(transformedSections["tone"]).toBeDefined();
        expect(transformedSections["tone"]!.length).toBeGreaterThan(0);

        await session.disconnect();
    });

    it("should apply transform modifications to section content", async () => {
        let originalContent = "";
        let transformedContent = "";

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    identity: {
                        action: (content: string) => {
                            originalContent = content;
                            // Append a custom instruction via transform
                            transformedContent = content + "\nTRANSFORM_MARKER";
                            return transformedContent;
                        },
                    },
                },
            },
        });

        await writeFile(join(workDir, "hello.txt"), "Hello!");

        await session.sendAndWait({
            prompt: "Read the contents of hello.txt",
        });

        // Verify the transform callback was invoked and modified the content
        expect(originalContent.length).toBeGreaterThan(0);
        expect(transformedContent).toContain("TRANSFORM_MARKER");
        expect(transformedContent).toContain(originalContent);

        await session.disconnect();
    });

    it("should work with static overrides and transforms together", async () => {
        const transformedSections: Record<string, string> = {};

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    // Static override
                    safety: { action: "remove" },
                    // Transform
                    identity: {
                        action: (content: string) => {
                            transformedSections["identity"] = content;
                            return content;
                        },
                    },
                },
            },
        });

        await writeFile(join(workDir, "combo.txt"), "Combo test!");

        await session.sendAndWait({
            prompt: "Read the contents of combo.txt and tell me what it says",
        });

        // Transform should have been invoked
        expect(transformedSections["identity"]).toBeDefined();
        expect(transformedSections["identity"]!.length).toBeGreaterThan(0);

        await session.disconnect();
    });
});
