import { join } from "node:path";
import { getRuntimePlatform, materializeRuntimeBundle } from "../src/runtimeArtifacts.js";
import { COPILOT_CLI_VERSION } from "../src/cliVersion.js";
import { ensureCopilotPackage } from "./releaseArtifacts.js";

const [option] = process.argv.slice(2);
const platform = getRuntimePlatform();
const packageRoot = await ensureCopilotPackage(COPILOT_CLI_VERSION, { platform });
if (option === "--print-legacy-path") {
    process.stdout.write(`${join(packageRoot, "app.js")}\n`);
} else if (option === "--print-path" || option === undefined) {
    const runtimePath = materializeRuntimeBundle({ packageRoot, platform });
    process.stdout.write(`${runtimePath}\n`);
} else {
    throw new Error(`Unknown option: ${option}`);
}
