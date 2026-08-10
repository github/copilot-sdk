import { writeFileSync } from "node:fs";
import { defineFactory, joinSession } from "@github/copilot-sdk/extension";

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

await joinSession({ factories: [argumentEcho, arrayResult] });
writeFileSync(new URL("./ready", import.meta.url), "ready");
