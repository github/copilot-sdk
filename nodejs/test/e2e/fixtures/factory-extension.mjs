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

await joinSession({ factories: [argumentEcho] });
writeFileSync(new URL("./ready", import.meta.url), "ready");
