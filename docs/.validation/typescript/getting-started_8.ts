// Source: getting-started.md:1193
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
    cliUrl: "localhost:4321"
});

// Use the client normally
const session = await client.createSession();
// ...