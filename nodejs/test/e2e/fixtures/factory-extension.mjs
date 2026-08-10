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

session = await joinSession({
    factories: [
        argumentEcho,
        arrayResult,
        startsFromContextSession,
        startsFromModuleSession,
        parked,
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
