import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  collectSamples,
  runWithWatchdog,
} from "../../dotnet/ci/test-watchdog.mjs";

export function goProgressMarker(line) {
  if (line === "=== Running Go SDK E2E Tests ===")
    return { phase: "go-build-and-test" };
  // Go buffers a package's verbose output until it exits. In-process TestMain
  // writes its own phase and goroutine artifacts without relying on this pipe.
  const result =
    /^(ok|FAIL|\?)\s+(github\.com\/github\/copilot-sdk\/go(?:\/[A-Za-z0-9_.-]+)*)(?=\s|$)/.exec(
      line,
    );
  return result ? { outcome: result[1], package: result[2] } : null;
}

export function diagnosticBudget(deadline, now = Date.now()) {
  if (!Number.isFinite(deadline) || deadline <= 0)
    throw new Error("The Go CI watchdog requires GO_TEST_DEADLINE");
  const timeoutMs = Math.min(15 * 60_000, deadline - now);
  return { timeoutMs, captureAt: now + timeoutMs - 60_000 };
}

export function prioritizeGoProcesses(processes) {
  const rank = ({ role }) => (role === "e2e.test" ? 0 : role === "go" ? 1 : 2);
  return [...processes].sort((a, b) => rank(a) - rank(b));
}

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  if (process.platform !== "darwin")
    throw new Error("The Go CI watchdog requires macOS");
  const directory = resolve("TestResults");
  const { timeoutMs, captureAt } = diagnosticBudget(
    Number(process.env.GO_TEST_DEADLINE),
  );
  mkdirSync(directory, { recursive: true });
  const { copilotCliVersion } = JSON.parse(
    readFileSync(new URL("../../nodejs/package.json", import.meta.url)),
  );
  writeFileSync(
    join(directory, "watchdog-runtime.json"),
    JSON.stringify({
      copilotCliVersion,
      platform: process.platform,
      arch: process.arch,
      node: process.version,
      captureAt,
    }),
  );
  process.env.GO_TEST_DIAGNOSTIC_DIRECTORY = directory;
  process.env.GO_TEST_DIAGNOSTIC_CAPTURE_AT = String(captureAt);
  process.exitCode = await runWithWatchdog({
    command: "/bin/bash",
    args: ["test.sh"],
    directory,
    timeoutMs,
    marker: goProgressMarker,
    label: "Go",
    sample: (processes, directory, record) =>
      collectSamples(prioritizeGoProcesses(processes), directory, record),
  });
}
