// Source: debugging.md:23
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
  logLevel: "debug",  // Options: "none", "error", "warning", "info", "debug", "all"
});