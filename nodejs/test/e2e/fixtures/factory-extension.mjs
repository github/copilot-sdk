import { existsSync, writeFileSync } from "node:fs";
import { defineFactory, joinSession } from "@github/copilot-sdk/extension";

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

const argumentEcho = defineFactory({
    meta: {
        name: "argument-echo",
        description: "Return the invocation arguments verbatim.",
        phases: [],
        // Proves a declared shape survives the SDK boundary and registers against a
        // real runtime. It does not exercise enforcement: `argsSchema` is checked by
        // the model's `run_factory` tool, and these tests invoke `session.factory.run`,
        // which does not validate. The declaration stays as wide as this factory's
        // actual contract — it echoes any JsonValue, and is called with an array, an
        // object, and nothing — so it cannot constrain the runs below.
        argsSchema: {
            type: ["object", "array", "string", "number", "integer", "boolean", "null"],
        },
    },
    run: async ({ args }) => args,
});

const arrayResult = defineFactory({
    meta: {
        name: "array-result",
        description: "Return an array result.",
        phases: [],
    },
    run: async () => [1, "two", false],
});

const phased = defineFactory({
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

const forwardsSubagentOptions = defineFactory({
    meta: {
        name: "forwards-subagent-options",
        description: "Send every declared subagent option to the runtime.",
        phases: [],
    },
    run: async ({ agent }) => {
        // Only the runtime's acceptance of the payload is under test. A refused
        // request rejects quickly, because the runtime parses the options before
        // it starts a subagent. A subagent that is merely slow to reach a model
        // proves the payload was accepted, so waiting for it adds nothing and
        // hangs wherever no model is reachable.
        const call = agent("Confirm that this request is accepted.", {
            agent: "reviewer",
            reasoningEffort: "high",
            contextTier: "long_context",
        });
        // A rejection that lands after the race still needs a handler.
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

const startsFromContextSession = defineFactory({
    meta: {
        name: "starts-from-context-session",
        description: "Try to start a factory through the context session.",
        phases: [],
    },
    run: async ({ session }) => {
        try {
            await session.factory.run("argument-echo");
            return "unexpectedly started a factory";
        } catch (error) {
            return error instanceof Error ? error.message : String(error);
        }
    },
});

let session;

const startsFromModuleSession = defineFactory({
    meta: {
        name: "starts-from-module-session",
        description: "Try to start a factory through the module session.",
        phases: [],
    },
    run: async () => {
        try {
            await session.factory.run("argument-echo");
            return "unexpectedly started a factory";
        } catch (error) {
            return error instanceof Error ? error.message : String(error);
        }
    },
});

const parked = defineFactory({
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

const failsOnce = defineFactory({
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

session = await joinSession({
    factories: [
        argumentEcho,
        arrayResult,
        phased,
        forwardsSubagentOptions,
        startsFromContextSession,
        startsFromModuleSession,
        parked,
        failsOnce,
    ],
});

void waitForMarker("start-b", 30_000)
    .then(async () => {
        const result = await session.factory.run("argument-echo", {
            args: { source: "module-watcher" },
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
