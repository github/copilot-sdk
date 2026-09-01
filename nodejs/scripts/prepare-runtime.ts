import { join } from "node:path";
import { ensureCopilotPackage, ensureRuntimeBundle } from "../src/runtimeArtifacts.js";
import { COPILOT_CLI_VERSION } from "../src/cliVersion.js";

const [option] = process.argv.slice(2);
if (option === "--print-legacy-path") {
    const packageRoot = await ensureCopilotPackage(COPILOT_CLI_VERSION);
    process.stdout.write(`${join(packageRoot, "app.js")}\n`);
} else if (option === "--print-path" || option === undefined) {
    const runtimePath = await ensureRuntimeBundle(COPILOT_CLI_VERSION);
    process.stdout.write(`${runtimePath}\n`);
} else {
    throw new Error(`Unknown option: ${option}`);
}
