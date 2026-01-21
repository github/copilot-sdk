/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as path from "path";
import { describe, expect, it } from "vitest";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe.skip("Skills Configuration", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext({ logLevel: "debug" });
    const SKILL_MARKER = "PINEAPPLE_COCONUT_42";
    let skillDirCounter = 0;

    function createSkillDir(): string {
        const skillsDir = path.join(
            workDir,
            ".test_skills",
            `copilot-skills-test-${++skillDirCounter}`
        );
        fs.mkdirSync(skillsDir, { recursive: true });

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
        return skillsDir;
    }

    describe("Skill Behavior", () => {
        it("should load and apply skill from skillDirectories", async () => {
            const skillsDir = createSkillDir();
            const session = await client.createSession({
                skillDirectories: [skillsDir],
            });

            expect(session.sessionId).toBeDefined();

            // The skill instructs the model to include a marker - verify it appears
            const message = await session.sendAndWait({
                prompt: "Say hello briefly using the test skill.",
            });

            expect(message?.data.content).toContain(SKILL_MARKER);

            await session.destroy();
        });

        it("should not apply skill when disabled via disabledSkills", async () => {
            const skillsDir = createSkillDir();
            const session = await client.createSession({
                skillDirectories: [skillsDir],
                disabledSkills: ["test-skill"],
            });

            expect(session.sessionId).toBeDefined();

            // The skill is disabled, so the marker should NOT appear
            const message = await session.sendAndWait({
                prompt: "Say hello briefly using the test skill.",
            });

            expect(message?.data.content).not.toContain(SKILL_MARKER);

            await session.destroy();
        });

        it("should apply skill on session resume with skillDirectories", async () => {
            const skillsDir = createSkillDir();

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
                prompt: "Say hello again using the test skill.",
            });

            expect(message2?.data.content).toContain(SKILL_MARKER);

            await session2.destroy();
        });
    });
});
