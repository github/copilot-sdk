import { writeFileSync } from "node:fs";
import { defineWorkflow, joinSession } from "@github/copilot-sdk/extension";

const argumentEcho = defineWorkflow({
    meta: {
        name: "argument-echo",
        description: "Return the invocation arguments verbatim.",
        phases: [],
        argsSchema: {
            type: ["object", "array", "string", "number", "integer", "boolean", "null"],
        },
    },
    run: async ({ args }) => args,
});

const session = await joinSession({
    workflows: [argumentEcho],
});

if (!session.workflow) {
    throw new Error("Workflow API was not registered");
}

writeFileSync(new URL("./ready", import.meta.url), "ready");
