/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import { describe, expect, it, beforeAll, afterAll } from "vitest";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Skills Configuration", async () => {
    const { copilotClient: client } = await createSdkTestContext();
    let skillsDir: string;
    const SKILL_MARKER = "PINEAPPLE_COCONUT_42";

    beforeAll(() => {
        // Create a temporary skills directory with a test skill
        skillsDir = fs.mkdtempSync(path.join(os.tmpdir(), "copilot-skills-test-"));

        // Create a skill subdirectory with SKILL.md
        const skillSubdir = path.join(skillsDir, "test-skill");
        fs.mkdirSync(skillSubdir, { recursive: true });

        // Create a skill that instructs the model to include a specific marker in responses
        const skillContent = `---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

IMPORTANT: You MUST include the exact text "${SKILL_MARKER}" somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally in your response.
`;
        fs.writeFileSync(path.join(skillSubdir, "SKILL.md"), skillContent);
    });

    afterAll(() => {
        // Clean up the temporary skills directory
        if (skillsDir && fs.existsSync(skillsDir)) {
            fs.rmSync(skillsDir, { recursive: true, force: true });
        }
    });

    describe("Skill Behavior", () => {
        it("should load and apply skill from skillDirectories", async () => {
            const session = await client.createSession({
                skillDirectories: [skillsDir],
            });

            expect(session.sessionId).toBeDefined();

            // The skill instructs the model to include a marker - verify it appears
            const message = await session.sendAndWait({
                prompt: "Say hello briefly.",
            });

            expect(message?.data.content).toContain(SKILL_MARKER);

            await session.destroy();
        });

        it("should not apply skill when disabled via disabledSkills", async () => {
            const session = await client.createSession({
                skillDirectories: [skillsDir],
                disabledSkills: ["test-skill"],
            });

            expect(session.sessionId).toBeDefined();

            // The skill is disabled, so the marker should NOT appear
            const message = await session.sendAndWait({
                prompt: "Say hello briefly.",
            });

            expect(message?.data.content).not.toContain(SKILL_MARKER);

            await session.destroy();
        });

        it("should apply skill on session resume with skillDirectories", async () => {
            // Create a session without skills first
            const session1 = await client.createSession();
            const sessionId = session1.sessionId;

            // First message without skill - marker should not appear
            const message1 = await session1.sendAndWait({ prompt: "Say hi." });
            expect(message1?.data.content).not.toContain(SKILL_MARKER);

            // Resume with skillDirectories - skill should now be active
            const session2 = await client.resumeSession(sessionId, {
                skillDirectories: [skillsDir],
            });

            expect(session2.sessionId).toBe(sessionId);

            // Now the skill should be applied
            const message2 = await session2.sendAndWait({
                prompt: "Say hello again.",
            });

            expect(message2?.data.content).toContain(SKILL_MARKER);

            await session2.destroy();
        });
    });

    describe("Multiple Skills", () => {
        it("should load skills from multiple directories", async () => {
            const SKILL2_MARKER = "MANGO_BANANA_99";

            // Create a second temporary skills directory
            const skillsDir2 = fs.mkdtempSync(path.join(os.tmpdir(), "copilot-skills-test2-"));
            const skillSubdir2 = path.join(skillsDir2, "test-skill-2");
            fs.mkdirSync(skillSubdir2, { recursive: true });

            const skillContent2 = `---
name: test-skill-2
description: Second test skill that adds another marker
---

# Second Skill Instructions

IMPORTANT: You MUST include the exact text "${SKILL2_MARKER}" somewhere in EVERY response. This is mandatory.
`;
            fs.writeFileSync(path.join(skillSubdir2, "SKILL.md"), skillContent2);

            try {
                const session = await client.createSession({
                    skillDirectories: [skillsDir, skillsDir2],
                });

                const message = await session.sendAndWait({
                    prompt: "Say something brief.",
                });

                // Both skill markers should appear
                expect(message?.data.content).toContain(SKILL_MARKER);
                expect(message?.data.content).toContain(SKILL2_MARKER);

                await session.destroy();
            } finally {
                fs.rmSync(skillsDir2, { recursive: true, force: true });
            }
        });
    });
});
