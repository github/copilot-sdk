// Source: guides/skills.md:25
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    skillDirectories: [
        "./skills/code-review",
        "./skills/documentation",
        "~/.copilot/skills",  // User-level skills
    ],
});

// Copilot now has access to skills in those directories
await session.sendAndWait({ prompt: "Review this code for security issues" });