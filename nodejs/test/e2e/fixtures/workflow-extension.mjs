import { closeSync, existsSync, fstatSync, openSync, writeFileSync, writeSync } from "node:fs";
import { defineWorkflow, joinSession } from "@github/copilot-sdk/extension";

const marker = (name) => new URL(`./${name}`, import.meta.url);

async function waitForMarker(name, timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    while (!existsSync(marker(name))) {
        if (Date.now() >= deadline) {
            throw new Error(`Timed out waiting for ${name}`);
        }
        await new Promise((resolve) => setTimeout(resolve, 50));
    }
}

function incrementMarker(name) {
    const descriptor = openSync(marker(name), "a+");
    try {
        writeSync(descriptor, "1");
        return fstatSync(descriptor).size;
    } finally {
        closeSync(descriptor);
    }
}

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

const arrayResult = defineWorkflow({
    meta: {
        name: "array-result",
        description: "Return an array result.",
        phases: [],
    },
    run: async () => [1, "two", false],
});

const phased = defineWorkflow({
    meta: {
        name: "phased",
        description: "Record named phases and ordinary progress.",
        phases: [{ title: "Collect" }, { title: "Summarize" }],
    },
    run: async ({ phase, log }) => {
        phase("Collect");
        log("Collected");
        phase("Summarize");
        log("Summarized");
        return "finished";
    },
});

const forwardsSubagentOptions = defineWorkflow({
    meta: {
        name: "forwards-subagent-options",
        description: "Send every declared subagent option to the runtime.",
        phases: [],
    },
    run: async ({ agent }) => {
        const call = agent("Confirm that this request is accepted.", {
            agent: "reviewer",
            reasoningEffort: "high",
            contextTier: "long_context",
        });
        call.catch(() => {});
        let settleTimer;
        const stillPending = new Promise((resolve) => {
            settleTimer = setTimeout(() => resolve(undefined), 3000);
            settleTimer.unref?.();
        });
        try {
            await Promise.race([call, stillPending]);
            return { didThrow: false };
        } catch {
            return { didThrow: true };
        } finally {
            clearTimeout(settleTimer);
        }
    },
});

const startsFromContextSession = defineWorkflow({
    meta: {
        name: "starts-from-context-session",
        description: "Try to start a workflow through the context session.",
        phases: [],
    },
    run: async ({ session }) => {
        try {
            await session.workflow.run("argument-echo");
            return "unexpectedly started a workflow";
        } catch (error) {
            return error instanceof Error ? error.message : String(error);
        }
    },
});

let session;

const startsFromModuleSession = defineWorkflow({
    meta: {
        name: "starts-from-module-session",
        description: "Try to start a workflow through the module session.",
        phases: [],
    },
    run: async () => {
        try {
            await session.workflow.run("argument-echo");
            return "unexpectedly started a workflow";
        } catch (error) {
            return error instanceof Error ? error.message : String(error);
        }
    },
});

const parked = defineWorkflow({
    meta: {
        name: "parked",
        description: "Wait for a test-controlled release marker.",
        phases: [],
    },
    run: async () => {
        writeFileSync(marker("entered"), "entered");
        await waitForMarker("release", 30_000);
        return "released";
    },
});

const failsOnce = defineWorkflow({
    meta: {
        name: "fails-once",
        description: "Fails its first attempt and succeeds when resumed.",
        phases: [],
    },
    run: async () => {
        if (!existsSync(marker("fails-once-attempted"))) {
            writeFileSync(marker("fails-once-attempted"), "attempted");
            throw new Error("first attempt failed");
        }
        return "resumed";
    },
});

const externallyPaused = defineWorkflow({
    meta: {
        name: "externally-paused",
        description: "Wait until the calling session pauses this run.",
        phases: [],
    },
    run: async ({ signal }) => {
        writeFileSync(marker("external-pause-entered"), "entered");
        await new Promise((_, reject) => {
            const abort = () => reject(signal.reason ?? new Error("Workflow aborted"));
            if (signal.aborted) {
                abort();
                return;
            }
            signal.addEventListener("abort", abort, { once: true });
        });
        return "unexpectedly completed";
    },
});

const durablePauseCheckpoint = defineWorkflow({
    meta: {
        name: "durable-pause-checkpoint",
        description: "Pause once after journaled preparation, then complete after resume.",
        phases: [],
    },
    run: async ({ pause, step }) => {
        const attempt = incrementMarker("checkpoint-attempts");
        const prepared = await step("prepare", () => incrementMarker("checkpoint-preparations"));
        await pause("review-ready");
        return { attempt, prepared };
    },
});

session = await joinSession({
    workflows: [
        argumentEcho,
        arrayResult,
        phased,
        forwardsSubagentOptions,
        startsFromContextSession,
        startsFromModuleSession,
        parked,
        failsOnce,
        externallyPaused,
        durablePauseCheckpoint,
    ],
});

if (!session.workflow) {
    throw new Error("Workflow API was not registered");
}

void waitForMarker("start-b", 30_000)
    .then(async () => {
        const result = await session.workflow.run("argument-echo", {
            args: { source: "module-watcher" },
            notifyOnComplete: false,
        });
        writeFileSync(marker("b-result"), JSON.stringify({ status: "success", result }));
    })
    .catch((error) => {
        if (existsSync(marker("start-b"))) {
            writeFileSync(
                marker("b-result"),
                JSON.stringify({
                    status: "error",
                    error: error instanceof Error ? error.message : String(error),
                })
            );
        }
    });

writeFileSync(marker("ready"), "ready");
