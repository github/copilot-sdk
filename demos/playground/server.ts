/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Playground Server
 *
 * Express server that communicates with the Copilot SDK.
 * The SDK manages the CLI process and sessions; this server exposes
 * that functionality via HTTP endpoints for the React frontend.
 */

import { CopilotClient, SessionConfig } from "@github/copilot-sdk";
import express from "express";

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());

// SDK Client - singleton that manages the CLI process
let client: CopilotClient | null = null;

// In-memory session storage
const sessions = new Map();
const sessionEvents = new Map();
const sessionModels = new Map();

/**
 * Initialize the SDK client
 *
 * Environment variables:
 * - COPILOT_CLI_PATH: Path to CLI binary (default: 'copilot' in PATH)
 * - COPILOT_CWD: Working directory for the CLI process (default: current directory)
 */
async function initializeClient(): Promise<boolean> {
    const cliPath = process.env.COPILOT_CLI_PATH || "copilot";
    const cwd = process.env.COPILOT_CWD || process.cwd();

    console.log("🚀 Initializing Copilot SDK client...");
    console.log("   CLI Path:", cliPath);
    console.log("   Working Directory:", cwd);

    client = new CopilotClient({
        cliPath,
        cwd,
        logLevel: "debug",
    });

    try {
        await client.start();
        console.log("✅ SDK client started successfully\n");
        return true;
    } catch (error) {
        const message = (error as Error).message;

        // Provide helpful error message if CLI is not found
        if (message.includes("ENOENT") || message.includes("not found")) {
            console.error("❌ Copilot CLI not found.\n");
            console.error("   To fix this, either:");
            console.error(
                "   1. Install the CLI: https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli"
            );
            console.error("   2. Set COPILOT_CLI_PATH to your CLI location\n");
        } else {
            console.error("❌ Failed to start SDK client:", message);
        }

        return false;
    }
}

// API Routes

/**
 * GET /api/status
 * Check if SDK is connected
 */
app.get("/api/status", (_req, res) => {
    // Include model info for each session
    const sessionsWithModels = Array.from(sessions.keys()).map((id) => ({
        id,
        model: sessionModels.get(id) || "unknown",
    }));

    res.json({
        connected: client !== null,
        sessions: sessionsWithModels,
    });
});

/**
 * POST /api/sessions
 * Create a new session
 * Body: { sessionId?: string, model?: string }
 */
app.post("/api/sessions", async (req, res) => {
    try {
        if (!client) {
            return res.status(503).json({ error: "SDK client not initialized" });
        }

        const { sessionId, model, systemMessage, provider } = req.body;
        const selectedModel = model || "claude-sonnet-4.5";

        // Build redacted provider config for logging
        const redactedProvider = provider
            ? {
                  type: provider.type,
                  baseUrl: provider.baseUrl,
                  apiKey: provider.apiKey ? "[REDACTED]" : undefined,
                  bearerToken: provider.bearerToken ? "[REDACTED]" : undefined,
                  wireApi: provider.wireApi,
                  azure: provider.azure,
              }
            : undefined;

        console.log("Creating session:", {
            sessionId,
            model: selectedModel,
            systemMessage: systemMessage ? systemMessage.mode : "default",
            provider: redactedProvider,
        });

        const sessionConfig: SessionConfig = {
            sessionId,
            model: selectedModel,
        };

        // Add system message config if provided
        if (systemMessage && systemMessage.mode && systemMessage.content) {
            sessionConfig.systemMessage = {
                mode: systemMessage.mode,
                content: systemMessage.content,
            };
        }

        // Add provider config if provided
        if (provider && provider.baseUrl) {
            sessionConfig.provider = {
                type: provider.type,
                baseUrl: provider.baseUrl,
            };
            if (provider.apiKey) {
                sessionConfig.provider.apiKey = provider.apiKey;
            }
            if (provider.bearerToken) {
                sessionConfig.provider.bearerToken = provider.bearerToken;
            }
            if (provider.wireApi) {
                sessionConfig.provider.wireApi = provider.wireApi;
            }
            if (provider.azure) {
                sessionConfig.provider.azure = provider.azure;
            }
        }

        const session = await client.createSession(sessionConfig);

        // Store session and model
        sessions.set(session.sessionId, session);
        sessionEvents.set(session.sessionId, []);
        sessionModels.set(session.sessionId, selectedModel);

        // Listen to session events
        session.on((event) => {
            const events = sessionEvents.get(session.sessionId);
            if (events) {
                events.push(event);

                // Keep only last 1000 events per session
                if (events.length > 1000) {
                    events.shift();
                }
            }

            console.log(`[${session.sessionId}] Event:`, event.type);
        });

        res.json({
            sessionId: session.sessionId,
        });
    } catch (error) {
        console.error("Failed to create session:", (error as Error).message);
        res.status(500).json({
            error: (error as Error).message || "Failed to create session",
        });
    }
});

/**
 * POST /api/sessions/:sessionId/send
 * Send a message to a session
 * Body: { prompt: string }
 */
app.post("/api/sessions/:sessionId/send", async (req, res) => {
    try {
        const { sessionId } = req.params;
        const { prompt } = req.body;

        const session = sessions.get(sessionId);
        if (!session) {
            return res.status(404).json({ error: "Session not found" });
        }

        if (!prompt) {
            return res.status(400).json({ error: "Prompt is required" });
        }

        console.log("[%s] Sending message:", sessionId, prompt.substring(0, 50) + "...");

        const messageId = await session.send({ prompt });

        res.json({ messageId });
    } catch (error) {
        console.error("Failed to send message:", (error as Error).message);
        res.status(500).json({
            error: (error as Error).message || "Failed to send message",
        });
    }
});

/**
 * GET /api/sessions/:sessionId/events
 * Get events for a session
 */
app.get("/api/sessions/:sessionId/events", async (req, res) => {
    try {
        const { sessionId } = req.params;

        const session = sessions.get(sessionId);
        if (!session) {
            return res.status(404).json({ error: "Session not found" });
        }

        const events = sessionEvents.get(sessionId) || [];

        res.json({
            sessionId,
            events: events.slice(), // Return a copy
        });
    } catch (error) {
        console.error("Failed to get events:", (error as Error).message);
        res.status(500).json({
            error: (error as Error).message || "Failed to get events",
        });
    }
});

/**
 * DELETE /api/sessions/:sessionId
 * Destroy a session
 */
app.delete("/api/sessions/:sessionId", async (req, res) => {
    try {
        const { sessionId } = req.params;

        const session = sessions.get(sessionId);
        if (!session) {
            return res.status(404).json({ error: "Session not found" });
        }

        console.log(`[${sessionId}] Destroying session`);

        await session.destroy();

        sessions.delete(sessionId);
        sessionEvents.delete(sessionId);
        sessionModels.delete(sessionId);

        res.json({ success: true });
    } catch (error) {
        console.error("Failed to destroy session:", (error as Error).message);
        res.status(500).json({
            error: (error as Error).message || "Failed to destroy session",
        });
    }
});

/**
 * GET /api/sessions/:sessionId/messages
 * Get all messages/events from a session
 */
app.get("/api/sessions/:sessionId/messages", async (req, res) => {
    try {
        const { sessionId } = req.params;

        const session = sessions.get(sessionId);
        if (!session) {
            return res.status(404).json({ error: "Session not found" });
        }

        const messages = await session.getMessages();

        res.json({
            sessionId,
            messages,
        });
    } catch (error) {
        console.error("Failed to get messages:", (error as Error).message);
        res.status(500).json({
            error: (error as Error).message || "Failed to get messages",
        });
    }
});

// Start server
async function start() {
    const initialized = await initializeClient();

    if (!initialized) {
        console.error("⚠️  Server will start but SDK is not connected");
        console.error("⚠️  Make sure COPILOT_CLI_PATH is set correctly");
    }

    app.listen(PORT, () => {
        console.log(`\n🚀 SDK API Server running at http://localhost:${PORT}`);
        console.log(`📝 Open http://localhost:5173 in your browser\n`);
    });
}

// Graceful shutdown
process.on("SIGINT", async () => {
    console.log("\n\nShutting down...");

    // Destroy all sessions
    for (const [sessionId, session] of sessions) {
        try {
            console.log(`Destroying session: ${sessionId}`);
            await session.destroy();
        } catch (error) {
            console.error(`Failed to destroy session ${sessionId}:`, (error as Error).message);
        }
    }

    // Stop SDK client
    if (client) {
        try {
            await client.stop();
            console.log("SDK client stopped");
        } catch (error) {
            console.error("Failed to stop SDK client:", (error as Error).message);
        }
    }

    process.exit(0);
});

start();
