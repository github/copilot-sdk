import { readFile, unlink, writeFile } from "node:fs/promises";
import { z } from "zod";
import { CopilotClient, defineTool, type SessionEvent } from "@github/copilot-sdk";
import {
    GitHubActionsClient,
    type DelegateHandle,
    type DelegateRequest,
    type GitHubActionsTarget,
} from "./github-actions-client.js";

const stateFile =
    process.env.DELEGATE_STATE_FILE ?? "/tmp/copilot-sdk-github-actions-delegate.json";
const repository = process.env.GITHUB_ACTIONS_REPOSITORY;
const workflow = process.env.GITHUB_ACTIONS_WORKFLOW ?? "sdk-delegated-agent.lock.yml";
const ref = process.env.GITHUB_ACTIONS_REF ?? "main";

if (!repository || !process.env.GITHUB_TOKEN) {
    throw new Error("Set GITHUB_ACTIONS_REPOSITORY and GITHUB_TOKEN before running this sample.");
}

const target: GitHubActionsTarget = {
    repository,
    workflow,
    allowedRefs: [ref],
    allowedAgentTypes: ["researcher", "editor"],
};

const actions = new GitHubActionsClient({
    token: () => process.env.GITHUB_TOKEN ?? "",
    targets: [target],
    onLifecycle: (event) => {
        console.log(`[delegate:${event.correlationId}] ${event.state}`, event.runUrl ?? "");
    },
});

const requestSchema = z.object({
    task: z.string().min(1).max(12_000),
    repository: z.string(),
    ref: z.string(),
    agentType: z.enum(["researcher", "editor"]),
    expectedOutput: z.string().min(1).max(1_000),
});

const delegate = defineTool("github_actions_delegate", {
    description:
        "Delegate an isolated research or Node.js editing task to an allowlisted GitHub Actions worker.",
    parameters: requestSchema,
});

interface PendingDelegate {
    sessionId: string;
    requestId: string;
    handle: DelegateHandle;
}

function waitForExternalToolRequest(
    session: Awaited<ReturnType<CopilotClient["createSession"]>>
): Promise<Extract<SessionEvent, { type: "external_tool.requested" }>> {
    return new Promise((resolve) => {
        const unsubscribe = session.on("external_tool.requested", (event) => {
            if (event.data.toolName === "github_actions_delegate") {
                unsubscribe();
                resolve(event);
            }
        });
    });
}

async function dispatch(prompt: string): Promise<void> {
    if (!prompt) {
        throw new Error("Pass the orchestrator task after the dispatch command.");
    }

    const client = new CopilotClient();
    const session = await client.createSession({
        tools: [delegate],
        availableTools: ["custom:github_actions_delegate"],
        systemMessage: {
            content: [
                "Act as an orchestrator.",
                "Delegate the requested work through github_actions_delegate.",
                "Do not use local sub-agents.",
                `Only use repository ${repository} at ref ${ref}.`,
            ].join("\n"),
        },
        onPermissionRequest: async () => ({ kind: "approve-once" }),
    });

    const requested = waitForExternalToolRequest(session);
    await session.send({ prompt });
    const event = await requested;
    const request = requestSchema.parse(event.data.arguments) as DelegateRequest;
    const handle = await actions.dispatch(request, {
        sessionId: event.data.sessionId,
        toolCallId: event.data.toolCallId,
    });
    const state: PendingDelegate = {
        sessionId: session.sessionId,
        requestId: event.data.requestId,
        handle,
    };

    await writeFile(stateFile, JSON.stringify(state, null, 2), { mode: 0o600 });
    console.log(`Pending delegation saved to ${stateFile}.`);
    await client.forceStop();
}

async function complete(): Promise<void> {
    const state = JSON.parse(await readFile(stateFile, "utf8")) as PendingDelegate;
    const result = await actions.wait(state.handle);

    const client = new CopilotClient();
    const session = await client.resumeSession(state.sessionId, {
        tools: [delegate],
        availableTools: ["custom:github_actions_delegate"],
        continuePendingWork: true,
        onPermissionRequest: async () => ({ kind: "approve-once" }),
    });
    const assistantMessage = new Promise<Extract<SessionEvent, { type: "assistant.message" }>>(
        (resolve) => {
            const unsubscribe = session.on("assistant.message", (event) => {
                unsubscribe();
                resolve(event);
            });
        }
    );

    await session.rpc.tools.handlePendingToolCall({
        requestId: state.requestId,
        result: {
            resultType: result.conclusion === "success" ? "success" : "failure",
            textResultForLlm: JSON.stringify(result),
        },
    });
    console.log((await assistantMessage).data.content);
    await unlink(stateFile);
    await client.stop();
}

const [command, ...promptParts] = process.argv.slice(2);
if (command === "dispatch") {
    await dispatch(promptParts.join(" ").trim());
} else if (command === "complete") {
    await complete();
} else {
    throw new Error('Use "dispatch <task>" or "complete".');
}
