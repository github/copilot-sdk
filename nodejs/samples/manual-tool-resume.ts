import {
    CopilotClient,
    defineTool,
    type CopilotSession,
    type SessionEvent,
} from "@github/copilot-sdk";
import { z } from "zod";

type EventOfType<T extends SessionEvent["type"]> = Extract<SessionEvent, { type: T }>;

function waitForEvent<T extends SessionEvent["type"]>(
    session: CopilotSession,
    type: T,
    predicate?: (event: EventOfType<T>) => boolean
): Promise<EventOfType<T>> {
    return new Promise((resolve) => {
        const unsubscribe = session.on(type, (event) => {
            const typed = event as EventOfType<T>;
            if (!predicate || predicate(typed)) {
                unsubscribe();
                resolve(typed);
            }
        });
    });
}

async function pause() {
    console.log("Simulating time passing...\n");
    await new Promise((resolve) => setTimeout(resolve, 1000));
}

const tool = defineTool("manual_resume_status", {
    description: "Looks up a status value. The SDK consumer supplies the result manually.",
    parameters: z.object({
        id: z.string().describe("Identifier to look up"),
    }),
    // No handler: the SDK exposes the declaration and leaves execution pending.
});

// 1. Create a session with a declaration-only tool, then stop after the permission prompt.
const client1 = new CopilotClient();
const session1 = await client1.createSession({ tools: [tool] });

// Subscribe before sending so the permission event cannot be missed.
const permissionRequested = waitForEvent(session1, "permission.requested");
await session1.send({
    prompt: "Use the manual_resume_status tool with id 'alpha', then tell me the status.",
});

const permissionEvent = await permissionRequested;
await client1.forceStop();
await pause();

// 2. Resume pending work and grant permission to invoke the tool.
const client2 = new CopilotClient();
const session2 = await client2.resumeSession(session1.sessionId, {
    tools: [tool],
    continuePendingWork: true,
});

// Subscribe before approving so the external tool request cannot be missed.
const toolRequested = waitForEvent(
    session2,
    "external_tool.requested",
    (event) => event.data.toolName === "manual_resume_status"
);

await session2.rpc.permissions.handlePendingPermissionRequest({
    requestId: permissionEvent.data.requestId,
    result: { kind: "approve-once" },
});

const toolEvent = await toolRequested;
await client2.forceStop();
await pause();

// 3. Resume again and manually provide the pending tool result.
const client3 = new CopilotClient();
const session3 = await client3.resumeSession(session1.sessionId, {
    tools: [tool],
    continuePendingWork: true,
});

const assistantMessage = waitForEvent(session3, "assistant.message");
await session3.rpc.tools.handlePendingToolCall({
    requestId: toolEvent.data.requestId,
    result: "MANUAL_STATUS_READY",
});

const answer = await assistantMessage;
console.log(answer.data.content);
await client3.forceStop();
