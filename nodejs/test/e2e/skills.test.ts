/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as path from "path";
import { beforeEach, describe, expect, it } from "vitest";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Skills Configuration", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext({ logLevel: "debug" });
    const SKILL_MARKER = "PINEAPPLE_COCONUT_42";
    const skillsDir = path.join(workDir, ".test_skills");

    beforeEach(() => {
        // Ensure we start fresh each time
        if (fs.existsSync(skillsDir)) {
            fs.rmSync(skillsDir, { recursive: true, force: true });
        }
    });

    function createSkillDir(): string {
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

        // Skipped because the underlying feature doesn't work correctly yet.
        // - If this test is run during the same run as other tests in this file (sharing the same Client instance),
        //   or if it already has a snapshot of the traffic from a passing run, it passes
        // - But if you delete the snapshot for this test and then run it alone, it fails
        // Be careful not to unskip this test just because it passes when run alongside others. It needs to pass when
        // run alone and without any prior snapshot.
        // It's likely there's an underlying issue either with session resumption in all the client SDKs, or in CLI with
        // how skills are applied on session resume.
        // Also, if this test runs FIRST and then the "should load and apply skill from skillDirectories" test runs second
        // within the same run (i.e., sharing the same Client instance), then the second test fails too. There's definitely
        // some state being shared or cached incorrectly.
        it.skip("should apply skill on session resume with skillDirectories", async () => {
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
