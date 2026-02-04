// Source: auth/byok.md:67
import { CopilotClient } from "@github/copilot-sdk";

const FOUNDRY_MODEL_URL = "https://your-resource.openai.azure.com/openai/v1/";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-5.2-codex",  // Your deployment name
    provider: {
        type: "openai",
        baseUrl: FOUNDRY_MODEL_URL,
        wireApi: "responses",  // Use "completions" for older models
        apiKey: process.env.FOUNDRY_API_KEY,
    },
});

session.on("assistant.message", (event) => {
    console.log(event.data.content);
});

await session.sendAndWait({ prompt: "What is 2+2?" });
await client.stop();