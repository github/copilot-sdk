import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { runWithWatchdog } from "../../dotnet/ci/test-watchdog.mjs";
import {
  diagnosticBudget,
  goProgressMarker,
  prioritizeGoProcesses,
} from "./test-watchdog.mjs";

test("only package progress is retained, without arbitrary output or arguments", () => {
  assert.deepEqual(goProgressMarker("=== Running Go SDK E2E Tests ==="), {
    phase: "go-build-and-test",
  });
  for (const outcome of ["ok", "FAIL", "?"]) {
    assert.deepEqual(
      goProgressMarker(
        `${outcome}\tgithub.com/github/copilot-sdk/go/internal/e2e\tsecret-value`,
      ),
      { outcome, package: "github.com/github/copilot-sdk/go/internal/e2e" },
    );
  }
  for (const line of [
    "secret-value",
    "=== RUN TestWithSecret/secret-value",
    "ok github.com/github/copilot-sdk/go/internal/e2e?secret-value",
  ]) {
    assert.equal(goProgressMarker(line), null);
  }
});

test("capture precedes the earlier of the command and absolute job deadlines", () => {
  const now = 2_000_000;
  assert.deepEqual(diagnosticBudget(now + 18 * 60_000, now), {
    timeoutMs: 15 * 60_000,
    captureAt: now + 14 * 60_000,
  });

  assert.deepEqual(diagnosticBudget(now + 5 * 60_000, now), {
    timeoutMs: 5 * 60_000,
    captureAt: now + 4 * 60_000,
  });
  assert.equal(diagnosticBudget(now - 1, now).timeoutMs, -1);
  for (const deadline of [NaN, Infinity, 0, -1])
    assert.throws(() => diagnosticBudget(deadline, now), /GO_TEST_DEADLINE/);
});

test("the shared watchdog uses Go markers and preserves the command failure", async () => {
  const directory = mkdtempSync(join(tmpdir(), "go-watchdog-"));
  try {
    const code = await runWithWatchdog({
      command: process.execPath,
      args: [
        "-e",
        "console.log('=== Running Go SDK E2E Tests ==='); console.log('secret-value'); process.exitCode = 37;",
      ],
      directory,
      timeoutMs: 10_000,
      forwardOutput: false,
      inspect: async () => [],
      marker: goProgressMarker,
      label: "Go",
    });

    assert.equal(code, 37);
    const timeline = readFileSync(join(directory, "watchdog.jsonl"), "utf8");
    assert.match(timeline, /"phase":"go-build-and-test"/);
    assert.doesNotMatch(timeline, /secret-value/);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});

test("native sampling prioritizes the in-process E2E host over launcher processes", () => {
  const processes = [
    ...Array.from({ length: 8 }, (_, pid) => ({ pid, role: "other" })),
    { pid: 100, role: "go" },
    { pid: 101, role: "e2e.test" },
  ];
  assert.deepEqual(prioritizeGoProcesses(processes).slice(0, 2), [
    { pid: 101, role: "e2e.test" },
    { pid: 100, role: "go" },
  ]);
  assert.equal(processes[0].pid, 0);
});
