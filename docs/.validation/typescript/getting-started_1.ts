// Source: getting-started.md:244
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    streaming: true,
});

// Listen for response chunks
session.on("assistant.message_delta", (event) => {
    process.stdout.write(event.data.deltaContent);
});
session.on("session.idle", () => {
    console.log(); // New line when done
});

await session.sendAndWait({ prompt: "Tell me a short joke" });

await client.stop();
process.exit(0);