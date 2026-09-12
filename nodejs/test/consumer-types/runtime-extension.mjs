import assert from "node:assert/strict";
import { approveAll, CopilotSession } from "@github/copilot-sdk";
import { joinSession } from "@github/copilot-sdk/extension";

const session = await joinSession({
    onPermissionRequest: approveAll,
    tools: [
        {
            name: "consumer_probe",
            description: "Return a deterministic result.",
            parameters: { type: "object", properties: {} },
            handler: async () => "consumer-result",
        },
    ],
    hooks: {
        onSessionStart: async (input, context) => ({
            additionalContext: `${input.source}:${context.sessionId}`,
        }),
        onSessionEnd: async (input) => ({ sessionSummary: input.reason }),
    },
});
assert.ok(session instanceof CopilotSession);
assert.equal(session.sessionId, "consumer-session");
process.stderr.write("consumer-ready\n");
