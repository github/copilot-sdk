import { approveAll } from "@github/copilot-sdk";
import { joinSession } from "@github/copilot-sdk/extension";

// Compile only. Runtime extension coverage uses runtime-extension.mjs.
const options = {
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
        onSessionEnd: async (input, context) => {
            const reason: string = input.reason;
            const sessionId: string = context.sessionId;
            void reason;
            void sessionId;
        },
    },
} satisfies Parameters<typeof joinSession>[0];

export const session = await joinSession(options);
const pending: Promise<Awaited<ReturnType<typeof joinSession>>> = joinSession(options);
void pending;
