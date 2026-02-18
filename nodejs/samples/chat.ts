import * as readline from "node:readline";
import { CopilotClient, approveAll, type SessionEvent } from "@github/copilot-sdk";

async function main() {
    const client = new CopilotClient();
    const session = await client.createSession({
        onPermissionRequest: approveAll,
    });

    session.on((event: SessionEvent) => {
        let output: string | null = null;
        if (event.type === "assistant.reasoning") {
            output = `[reasoning: ${event.data.content}]`;
        } else if (event.type === "tool.execution_start") {
            output = `[tool: ${event.data.toolName}]`;
        }
        if (output) console.log(`\x1b[34m${output}\x1b[0m`);
    });

    const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
    const prompt = (q: string) => new Promise<string>((r) => rl.question(q, r));

    console.log("Chat with Copilot (Ctrl+C to exit)\n");

    while (true) {
        const input = await prompt("You: ");
        if (!input.trim()) continue;
        console.log();

        const reply = await session.sendAndWait({ prompt: input });
        console.log(`\nAssistant: ${reply?.data.content}\n`);
    }
}

main().catch(console.error);
