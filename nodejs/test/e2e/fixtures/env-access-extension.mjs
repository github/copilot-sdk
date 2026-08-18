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
// - EXTENSION_ENV_REQUEST_EMPTY: set to `1` to pass an explicit empty list when
//   EXTENSION_ENV_REQUEST names nothing, instead of omitting the option.
// - EXTENSION_OBSERVE_ENV_NAMES: comma-separated names that are sampled but
//   deliberately NOT requested, so a test can prove a name outside the approved
//   set never reached this process.

import { writeFileSync } from "node:fs";

const sdkModule = process.env.EXTENSION_SDK_MODULE ?? "@github/copilot-sdk/extension";
const { joinSession } = await import(sdkModule);

const requested = (process.env.EXTENSION_ENV_REQUEST ?? "")
    .split(",")
    .map((name) => name.trim())
    .filter((name) => name.length > 0);
const observed = (process.env.EXTENSION_OBSERVE_ENV_NAMES ?? "")
    .split(",")
    .map((name) => name.trim())
    .filter((name) => name.length > 0);
const sampledNames = [...requested, ...observed];
const sample = () => sampledNames.map((name) => `${name}=${process.env[name] ?? ""}`).join("\n");

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
// An extension that wants nothing normally omits the option entirely.
// EXTENSION_ENV_REQUEST_EMPTY covers the other public way to ask for nothing:
// passing an empty list, which must reach the wire the same way.
if (requested.length > 0) {
    config.env = requested;
} else if (process.env.EXTENSION_ENV_REQUEST_EMPTY === "1") {
    config.env = [];
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
