import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdirSync, readFileSync, readdirSync, rmSync } from "node:fs";
import { join } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { test } from "node:test";
import { runWithWatchdog } from "./test-watchdog.mjs";
import {
  collectWindowsStacks,
  managedStacks,
  startWindowsJob,
  windowsSupervisorPath,
} from "./windows-watchdog.mjs";

assert.equal(process.platform, "win32", "Run these controls on Windows.");

function outputDirectory(t) {
  const directory = join(
    import.meta.dirname,
    `.watchdog-test-${process.pid}-${crypto.randomUUID()}`,
  );
  mkdirSync(directory, { recursive: true });
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  return directory;
}

function events(directory) {
  return readFileSync(join(directory, "watchdog.jsonl"), "utf8")
    .trim()
    .split("\n")
    .map(JSON.parse);
}

function alive(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch (error) {
    if (error.code === "ESRCH") return false;
    throw error;
  }
}

async function until(predicate, timeoutMs = 10_000) {
  const deadline = Date.now() + timeoutMs;
  while (!predicate()) {
    assert.ok(Date.now() < deadline, "Condition did not become true in time");
    await delay(50);
  }
}

test("managed-stack artifacts allowlist frames and thread IDs, not paths or values", (t) => {
  const previous = process.env.COPILOT_HMAC_KEY;
  process.env.COPILOT_HMAC_KEY = "SecretType";
  t.after(() => {
    if (previous === undefined) delete process.env.COPILOT_HMAC_KEY;
    else process.env.COPILOT_HMAC_KEY = previous;
  });
  assert.equal(
    managedStacks(`
private-path secret output
Thread (0x1234):
  [Native Frames]
  System.Private.CoreLib!System.Threading.Thread.Sleep(int32)
  Test!SecretType.Wait(class System.String[])
  Unrecognized secret content
  C:\\private\\path!Method(value)
`),
    "Thread (0x1234):\n[Native Frames]\n  System.Private.CoreLib!System.Threading.Thread.Sleep\n  Test![REDACTED].Wait",
  );
  assert.equal(managedStacks("raw diagnostic error with secret"), "");
  assert.ok(managedStacks("  A!B()\n".repeat(50_000)).length <= 65_536);
});

test("orphaned descendants retaining pipes preserve the first failure and die with their owned job", async (t) => {
  const unrelated = spawn(
    process.execPath,
    ["-e", "setInterval(() => {}, 1000)"],
    {
      stdio: "ignore",
    },
  );
  t.after(() => unrelated.kill("SIGKILL"));
  for (const code of [0, 37]) {
    const directory = outputDirectory(t);
    let descendants = [];
    // Both ancestors exit before a heartbeat. PID enumeration by surviving
    // parent IDs or taskkill /T on the exited root would miss the grandchild.
    const grandchild = "setInterval(() => {}, 1000)";
    const intermediate = `
      const { spawn } = require("node:child_process");
      spawn(process.execPath, ["-e", ${JSON.stringify(grandchild)}],
        { detached: true, stdio: ["ignore", 1, 2] }).unref();
    `;
    const source = `
      const { spawn } = require("node:child_process");
      console.log("Test run for private.dll (.NETCoreApp,Version=v8.0)");
      spawn(process.execPath, ["-e", ${JSON.stringify(intermediate)}],
        { detached: true, stdio: ["ignore", 1, 2] }).unref();
      process.exit(${code});
    `;
    const started = performance.now();
    const result = await runWithWatchdog({
      command: process.execPath,
      args: ["-e", source],
      directory,
      timeoutMs: 15_000,
      intervalMs: 500,
      graceMs: 50,
      sample: async (processes) => {
        descendants = processes;
      },
      forwardOutput: false,
    });
    assert.equal(result, code || 124, JSON.stringify(events(directory)));
    assert.ok(performance.now() - started < 19_000);
    const deadline = events(directory).find(
      (event) => event.event === "deadline-exceeded",
    );
    assert.equal(deadline.phase, "output-drain");
    assert.equal(deadline.framework, "net8.0");
    assert.ok(descendants.some(({ role }) => role === "node"));
    assert.ok(descendants.every(({ pid }) => pid !== unrelated.pid));
    await until(() => descendants.every(({ pid }) => !alive(pid)), 3_000);
    assert.ok(
      alive(unrelated.pid),
      "An unrelated process must not be terminated",
    );
    const exits = events(directory).filter(
      ({ event }) => event === "command-exit",
    );
    assert.equal(exits.length, 1);
    assert.equal(exits[0].exitCode, code);
  }
});

test("terminating the supervisor closes its job and kills live descendants", async (t) => {
  const directory = outputDirectory(t);
  const { child, status } = startWindowsJob(
    process.execPath,
    [
      "-e",
      `
      require("node:child_process").spawn(process.execPath,
        ["-e", "setInterval(() => {}, 1000)"], { stdio: "inherit" });
      setInterval(() => {}, 1000);
    `,
    ],
    directory,
  );
  t.after(() => child.kill("SIGKILL"));
  const closed = new Promise((resolve) => child.once("close", resolve));
  await until(
    () => status().processes.filter(({ role }) => role === "node").length === 2,
  );
  const owned = status().processes;
  assert.ok(owned.every(({ threads }) => threads.length > 0));
  assert.ok(owned.every(({ runtime }) => runtime === "native"));
  child.kill("SIGKILL");
  await closed;
  await until(() => owned.every(({ pid }) => !alive(pid)), 3_000);
});

test("normal output EOF cleans up background servers without hiding the command result", async (t) => {
  for (const code of [0, 43]) {
    const directory = outputDirectory(t);
    const result = await runWithWatchdog({
      command: process.execPath,
      args: [
        "-e",
        `
        const child = require("node:child_process").spawn(process.execPath,
          ["-e", "setInterval(() => {}, 1000)"], { detached: true, stdio: "ignore" });
        require("node:fs").writeFileSync(${JSON.stringify(join(directory, "background-pid"))}, String(child.pid));
        child.unref();
        process.exit(${code});
      `,
      ],
      directory,
      timeoutMs: 15_000,
      forwardOutput: false,
    });
    assert.equal(result, code);
    assert.ok(
      !events(directory).some(({ event }) => event === "deadline-exceeded"),
    );
    assert.ok(
      events(directory).some(
        ({ event }) => event === "owned-background-cleanup",
      ),
    );
    const pid = Number(readFileSync(join(directory, "background-pid"), "utf8"));
    await until(() => !alive(pid), 3_000);
  }
});

test("termination requests retain a failing status and clean the owned Windows job", async (t) => {
  const directory = outputDirectory(t);
  const result = runWithWatchdog({
    command: process.execPath,
    args: ["-e", "setInterval(() => {}, 1000)"],
    directory,
    timeoutMs: 20_000,
    intervalMs: 100,
    graceMs: 50,
    forwardOutput: false,
  });
  let pid;
  await until(() => {
    pid = events(directory)
      .flatMap((event) => event.processes ?? [])
      .at(-1)?.pid;
    return pid !== undefined;
  });
  // Windows TerminateProcess cannot deliver POSIX signals. Exercise the SDK's
  // termination handler separately from the real supervisor-kill control above.
  process.emit("SIGTERM");
  assert.equal(await result, 143);
  assert.ok(events(directory).some(({ event }) => event === "terminated"));
  await until(() => !alive(pid), 3_000);
});

test("the command-line entry point accepts Windows and records its runtime", async (t) => {
  const directory = outputDirectory(t);
  const child = spawn(
    process.execPath,
    [join(import.meta.dirname, "test-watchdog.mjs"), "--version"],
    {
      cwd: directory,
      env: {
        ...process.env,
        DOTNET_TEST_DEADLINE: String(Date.now() + 15_000),
      },
      stdio: "ignore",
    },
  );
  t.after(() => child.kill("SIGKILL"));
  assert.equal(await new Promise((resolve) => child.once("exit", resolve)), 0);
  const artifacts = join(directory, "TestResults");
  assert.equal(events(artifacts).at(-1).exitCode, 0);
  assert.equal(
    JSON.parse(readFileSync(join(artifacts, "watchdog-runtime.json"), "utf8"))
      .platform,
    "win32",
  );
});

test("Windows captures real managed waiting stacks without a memory dump or raw log", async (t) => {
  const directory = outputDirectory(t);
  const result = await runWithWatchdog({
    command: "dotnet",
    args: [windowsSupervisorPath, "--stack-probe"],
    directory,
    timeoutMs: 10_000,
    intervalMs: 500,
    graceMs: 50,
    forwardOutput: false,
  });
  assert.equal(result, 124);
  const captured = events(directory).filter(
    ({ event }) => event === "managed-stack",
  );
  assert.equal(
    captured.length,
    1,
    JSON.stringify(
      events(directory).filter(({ event }) => event !== "processes"),
    ),
  );
  const stacks = readFileSync(
    join(directory, `managed-stack-${captured[0].pid}.txt`),
    "utf8",
  );
  assert.match(stacks, /WindowsWatchdog\.WaitForStackProbe/);
  assert.match(stacks, /^Thread \(0x[0-9a-f]+\):/im);
  assert.ok(!stacks.includes("secret test payload"));
  assert.ok(
    !readFileSync(join(directory, "watchdog.jsonl"), "utf8").includes(
      "secret test payload",
    ),
  );
  assert.ok(
    !readdirSync(directory, { recursive: true }).some((file) =>
      /\.(dmp|nettrace|log)$/i.test(file),
    ),
  );
  await until(() => !alive(captured[0].pid), 3_000);
});

test("unsupported runtimes and failed stack collection are explicitly recorded", async (t) => {
  const directory = outputDirectory(t);
  const records = [];
  await collectWindowsStacks(
    [
      { pid: 2147483647, runtime: "core", role: "dotnet" },
      { pid: 2147483646, runtime: "framework", role: "testhost" },
    ],
    directory,
    (event) => records.push(event),
  );
  assert.deepEqual(records[0], {
    event: "managed-stack-unsupported",
    pid: 2147483646,
    runtime: "framework",
  });
  assert.equal(records[1].event, "managed-stack-unavailable");
  assert.equal(records[1].pid, 2147483647);
  assert.equal(records[1].code, 4294967295);
  assert.ok(
    !readdirSync(directory).some((file) => file.startsWith("managed-stack-")),
  );
});
