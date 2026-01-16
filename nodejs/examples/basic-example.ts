/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Example: Basic usage of the Copilot SDK
 */

import { existsSync } from "node:fs";
import { CopilotClient, type Tool } from "../src/index.js";

async function main() {
    console.log("🚀 Starting Copilot SDK Example\n");

    // Create client - will auto-start CLI server
    const cliCommand = process.env.COPILOT_CLI_PATH?.trim();
    let cliPath: string | undefined;
    let cliArgs: string[] | undefined;

    if (cliCommand) {
        if (!cliCommand.includes(" ") || existsSync(cliCommand)) {
            cliPath = cliCommand;
        } else {
            const tokens = cliCommand
                .match(/(?:[^\s"]+|"[^"]*")+/g)
                ?.map((token) => token.replace(/^"(.*)"$/, "$1"));
            if (tokens && tokens.length > 0) {
                cliPath = tokens[0];
                if (tokens.length > 1) {
                    cliArgs = tokens.slice(1);
                }
            }
        }
    }

    const client = new CopilotClient({
        logLevel: "info",
        ...(cliPath ? { cliPath } : {}),
        ...(cliArgs && cliArgs.length > 0 ? { cliArgs } : {}),
    });

    try {
        const facts: Record<string, string> = {
            javascript: "JavaScript was created in 10 days by Brendan Eich in 1995.",
            node: "Node.js lets you run JavaScript outside the browser using the V8 engine.",
        };

        const tools: Tool[] = [
            {
                name: "lookup_fact",
                description: "Returns a fun fact about a given topic.",
                parameters: {
                    type: "object",
                    properties: {
                        topic: {
                            type: "string",
                            description: "Topic to look up (e.g. 'javascript', 'node')",
                        },
                    },
                    required: ["topic"],
                },
                handler: async ({ arguments: args }) => {
                    const topic = String((args as { topic: string }).topic || "").toLowerCase();
                    const fact = facts[topic];
                    if (!fact) {
                        return {
                            textResultForLlm: `No fact stored for ${topic}.`,
                            resultType: "failure",
                            sessionLog: `lookup_fact: missing topic ${topic}`,
                            toolTelemetry: {},
                        };
                    }

                    return {
                        textResultForLlm: fact,
                        resultType: "success",
                        sessionLog: `lookup_fact: served ${topic}`,
                        toolTelemetry: {},
                    };
                },
            },
        ];

        // Create a session
        console.log("📝 Creating session...");
        const session = await client.createSession({
            model: "gpt-5",
            tools,
        });
        console.log(`✅ Session created: ${session.sessionId}\n`);

        // Listen to events
        session.on((event) => {
            console.log(`📢 Event [${event.type}]:`, JSON.stringify(event.data, null, 2));
        });

        // Send a simple message
        console.log("💬 Sending message...");
        await session.sendAndWait({
            prompt: "You can call the lookup_fact tool. First, please tell me 2+2.",
        });
        console.log("✅ Message completed\n");

        // Send another message
        console.log("\n💬 Sending follow-up message...");
        await session.sendAndWait({
            prompt: "Great. Now use lookup_fact to tell me something about Node.js.",
        });
        console.log("✅ Follow-up completed\n");

        // Clean up
        console.log("\n🧹 Cleaning up...");
        await session.destroy();
        await client.stop();

        console.log("✅ Done!");
    } catch (error) {
        console.error("❌ Error:", error);
        await client.stop();
        process.exit(1);
    }
}

main();
