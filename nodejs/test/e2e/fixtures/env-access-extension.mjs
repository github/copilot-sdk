/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Extension that asks for named sensitive environment variables at join time.
//
// Every input is an environment variable so one fixture serves both the stub-host
// case (spawned directly, importing the built SDK through EXTENSION_SDK_MODULE)
// and the real-CLI case (forked by the CLI, which injects the SDK module).
//
// - EXTENSION_SDK_MODULE: import specifier for the SDK. Defaults to the module
//   name the CLI resolves for a forked extension.
// - EXTENSION_ENV_REQUEST: comma-separated names to pass to joinSession({ env }).
// - EXTENSION_PREJOIN_FILE: `NAME=<value>` per requested name, sampled BEFORE the
//   join. A test compares it with the post-join sample to tell a value the
//   process already inherited from one the host granted.
// - EXTENSION_POSTJOIN_FILE: the same sample, written once the join settles.
// - EXTENSION_RESULT_FILE: `joined` or `rejected:<message>`. A denied extension
//   has no session to report through, so it reports here.

import { writeFileSync } from "node:fs";

const sdkModule = process.env.EXTENSION_SDK_MODULE ?? "@github/copilot-sdk/extension";
const { joinSession } = await import(sdkModule);

const requested = (process.env.EXTENSION_ENV_REQUEST ?? "")
    .split(",")
    .map((name) => name.trim())
    .filter((name) => name.length > 0);

const sample = () => requested.map((name) => `${name}=${process.env[name] ?? ""}`).join("\n");

const record = (file, contents) => {
    if (file) {
        writeFileSync(file, contents);
    }
};

record(process.env.EXTENSION_PREJOIN_FILE, sample());

const config = {
    tools: [
        {
            name: "env_access_greeter",
            description: "Greets someone. Always call this tool when asked to greet.",
            parameters: { type: "object", properties: { name: { type: "string" } } },
            handler: async (args) => `Hello from env-access, ${args.name || "World"}!`,
        },
    ],
};
// An extension that wants nothing omits the option entirely, as an ordinary
// extension does.
if (requested.length > 0) {
    config.env = requested;
}

try {
    await joinSession(config);
    record(process.env.EXTENSION_POSTJOIN_FILE, sample());
    record(process.env.EXTENSION_RESULT_FILE, "joined");
} catch (error) {
    // Sampled after the rejection too, so a test can prove a denied extension
    // never saw the value rather than only that the join failed.
    record(process.env.EXTENSION_POSTJOIN_FILE, sample());
    record(
        process.env.EXTENSION_RESULT_FILE,
        `rejected:${error instanceof Error ? error.message : String(error)}`
    );
}
