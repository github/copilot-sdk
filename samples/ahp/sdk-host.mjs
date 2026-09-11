import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";

const path = process.argv[2] ?? process.env.COPILOT_CLI_PATH;
if (!path) {
    throw new Error("Pass the local copilot-runtime binary path or set COPILOT_CLI_PATH.");
}

const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path }),
    gitHubToken: process.env.GITHUB_TOKEN ?? process.env.GH_TOKEN,
});
let ahpStarted = false;

try {
    await client.start();
    const { url } = await client.startAhpHost();
    ahpStarted = true;
    const session = await client.createSession({
        model: process.env.COPILOT_MODEL ?? "gpt-4.1",
        systemMessage: {
            mode: "replace",
            content: "You are Bert. When asked who you are, reply exactly: I am Bert.",
        },
        onPermissionRequest: async () => ({ kind: "denied-interactively-by-user" }),
    });
    session.on("assistant.message", (event) => {
        console.log(`[SDK observed ${session.sessionId}] ${event.data.content}`);
    });
    session.on("session.error", (event) => {
        console.error(`[SDK session error] ${event.data.message}`);
    });

    console.log(`AHP URL: ${url}`);
    console.log(`SDK session ID: ${session.sessionId}`);
    console.log(`In another terminal: npm run client -- '${url}' '${session.sessionId}'`);
    console.log("Waiting for AHP prompts. Press Ctrl+C to stop.");
    await new Promise((resolve) => {
        process.once("SIGINT", resolve);
        process.once("SIGTERM", resolve);
    });
} finally {
    try {
        if (ahpStarted) await client.stopAhpHost();
    } finally {
        await client.stop();
    }
}
