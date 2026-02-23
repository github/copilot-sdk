import express from "express";
import { CopilotClient } from "@github/copilot-sdk";

const PORT = parseInt(process.env.PORT || "8080", 10);
const CLI_URL = process.env.CLI_URL || process.env.COPILOT_CLI_URL || "localhost:3000";

const app = express();
app.use(express.json());

app.post("/chat", async (req, res) => {
  const { prompt } = req.body;
  if (!prompt || typeof prompt !== "string") {
    res.status(400).json({ error: "Missing 'prompt' in request body" });
    return;
  }

  const client = new CopilotClient({ cliUrl: CLI_URL });

  try {
    const session = await client.createSession({ model: "claude-haiku-4.5" });

    const response = await session.sendAndWait({ prompt });

    await session.destroy();

    if (response?.data.content) {
      res.json({ response: response.data.content });
    } else {
      res.status(502).json({ error: "No response content from Copilot CLI" });
    }
  } catch (err) {
    res.status(500).json({ error: String(err) });
  } finally {
    await client.stop();
  }
});

// When run directly, start server and optionally self-test
const server = app.listen(PORT, async () => {
  console.log(`Listening on port ${PORT}`);

  // Self-test mode: send a request and exit
  if (process.env.SELF_TEST === "1") {
    try {
      const resp = await fetch(`http://localhost:${PORT}/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: "What is the capital of France?" }),
      });
      const data = await resp.json();
      if (data.response) {
        console.log(data.response);
      } else {
        console.error("Self-test failed:", data);
        process.exit(1);
      }
    } catch (err) {
      console.error("Self-test error:", err);
      process.exit(1);
    } finally {
      server.close();
    }
  }
});
