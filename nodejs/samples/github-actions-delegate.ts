import { CopilotClient, defineTool } from "@github/copilot-sdk";
import { z } from "zod";
import {
    GitHubActionsClient,
    type DelegateRequest,
    type GitHubActionsTarget,
} from "./github-actions-client.js";

const repository = process.env.GITHUB_ACTIONS_REPOSITORY;
const workflow = process.env.GITHUB_ACTIONS_WORKFLOW ?? "sdk-delegated-agent.lock.yml";
const ref = process.env.GITHUB_ACTIONS_REF ?? "main";
const token = process.env.GITHUB_TOKEN;

if (!repository || !token) {
    throw new Error("Set GITHUB_ACTIONS_REPOSITORY and GITHUB_TOKEN before running this sample.");
}

const target: GitHubActionsTarget = {
    repository,
    workflow,
    allowedRefs: [ref],
    allowedAgentTypes: ["researcher", "editor"],
};

const actions = new GitHubActionsClient({
    token: () => token,
    targets: [target],
    onLifecycle: (event) => {
        console.log(`[delegate:${event.correlationId}] ${event.state}`, event.runUrl ?? "");
    },
});

const delegate = defineTool("github_actions_delegate", {
    description:
        "Delegate an isolated research or Node.js editing task to an allowlisted GitHub Actions worker.",
    parameters: z.object({
        task: z.string().min(1).max(12_000),
        repository: z.string(),
        ref: z.string(),
        agentType: z.enum(["researcher", "editor"]),
        expectedOutput: z.string().min(1).max(1_000),
    }),
    handler: async (request: DelegateRequest, invocation) => {
        const result = await actions.execute(request, invocation);
        return {
            resultType: result.conclusion === "success" ? "success" : "failure",
            textResultForLlm: JSON.stringify(result),
        };
    },
});

const client = new CopilotClient();
const session = await client.createSession({
    tools: [delegate],
    defaultAgent: {
        excludedTools: ["task"],
    },
    systemMessage: {
        content: [
            "Act as an orchestrator.",
            "Delegate research and Node.js editing work through github_actions_delegate.",
            "Do not use local sub-agents.",
            `Only use repository ${repository} at ref ${ref}.`,
            "Use a separate delegation for each independent task and summarize their returned run, artifact, and pull request links.",
        ].join("\n"),
    },
    onPermissionRequest: async () => ({ kind: "approve-once" }),
});

const task = process.argv.slice(2).join(" ").trim();
if (!task) {
    throw new Error('Pass a task, for example: npm run delegate -- "Review Node.js retries".');
}

try {
    const response = await session.sendAndWait({ prompt: task });
    console.log(response?.data.content ?? "The orchestrator returned no message.");
} finally {
    await client.stop();
}
