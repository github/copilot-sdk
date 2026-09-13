import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdirSync, readFileSync, rmSync } from "node:fs";
import { join } from "node:path";
import { test } from "node:test";
import {
  collectProcesses,
  collectSamples,
  ownedProcesses,
  progressMarker,
  runWithWatchdog,
  sampleStacks,
} from "./test-watchdog.mjs";

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

function run(t, source, options = {}) {
  const directory = outputDirectory(t);
  return {
    directory,
    result: runWithWatchdog({
      command: process.execPath,
      args: ["-e", source, "--", ...(options.extraArgs ?? [])],
      directory,
      timeoutMs: 5_000,
      graceMs: 25,
      inspect: async () => [],
      sample: async () => {},
      forwardOutput: false,
      ...options,
    }),
  };
}

test("recognizes build, provisioning, test and shutdown without recording raw output", () => {
  for (const [line, phase] of [
    ["       _DownloadCopilotCli:", "runtime-provisioning"],
    ["       CoreCompile:", "compilation"],
    ["       _CopyCopilotCliToOutput:", "runtime-copy"],
    ["Test run for /private/path.dll (net8.0)", "testhost-startup"],
    ["Starting test execution, please wait...", "test-discovery"],
    [
      "[xUnit.net 00:00:00.10]   Starting: GitHub.Copilot.SDK.Test",
      "tests-and-fixture-cleanup",
    ],
    [
      "[xUnit.net 00:01:50.44]   Finished: GitHub.Copilot.SDK.Test",
      "testhost-shutdown",
    ],
    ["Test Run Successful.", "test-command-shutdown"],
    ["Test Run Aborted.", "test-command-shutdown"],
  ])
    assert.deepEqual(progressMarker(line), { phase });
  assert.deepEqual(
    progressMarker(
      '  Passed GitHub.Copilot.Test.E2E.Example.Test(token: "secret") [1 s]',
    ),
    {
      outcome: "Passed",
      test: "GitHub.Copilot.Test.E2E.Example.Test",
    },
  );
  assert.equal(progressMarker("secret output"), null);
});

test("process snapshots contain only owned numeric metadata and known executable roles", () => {
  assert.deepEqual(
    ownedProcesses(
      `
 123 1 123 S 0.1 4096 01:02 /private/dotnet
 124 123 123 R+ 10.0 2048 00:01 /private/unrecognized-secret
 125 1 125 S 0.0 1024 01:00 /private/node
`,
      123,
    ),
    [
      {
        pid: 123,
        ppid: 1,
        group: 123,
        state: "S",
        cpu: 0.1,
        rssKiB: 4096,
        elapsed: "01:02",
        role: "dotnet",
      },
      {
        pid: 124,
        ppid: 123,
        group: 123,
        state: "R+",
        cpu: 10,
        rssKiB: 2048,
        elapsed: "00:01",
        role: "other",
      },
    ],
  );
});

test("samples omit headers and image paths and redact credentials", (t) => {
  const previous = process.env.COPILOT_HMAC_KEY;
  process.env.COPILOT_HMAC_KEY = "watchdog-test-secret";
  t.after(() => {
    if (previous === undefined) delete process.env.COPILOT_HMAC_KEY;
    else process.env.COPILOT_HMAC_KEY = previous;
  });
  assert.equal(
    sampleStacks(
      "Path: private\nCall graph:\n    wait watchdog-test-secret\nBinary Images:\nprivate",
    ),
    "    wait [REDACTED]",
  );
});

test("forwards arguments, preserves success and failure, and records split output markers", async (t) => {
  for (const code of [0, 23]) {
    const { directory, result } = run(
      t,
      `
      const assert = require("node:assert/strict");
      assert.deepEqual(process.argv.slice(1), ["--filter", "(A|B)&C", "--blame-hang"]);
      process.stdout.write("       _DownloadCopilot");
      setTimeout(() => {
        console.log("Cli:");
        console.log("secret output");
        console.log("Test Run Successful.");
        process.exitCode = ${code};
      }, 20);
    `,
      { extraArgs: ["--filter", "(A|B)&C", "--blame-hang"] },
    );
    assert.equal(await result, code);
    assert.ok(
      events(directory).some((event) => event.phase === "runtime-provisioning"),
    );
    assert.ok(
      !readFileSync(join(directory, "watchdog.jsonl"), "utf8").includes(
        "secret output",
      ),
    );
  }
});

test("expired job budget never starts a command", async (t) => {
  const { directory, result } = run(t, "process.exit(99)", { timeoutMs: 0 });
  assert.equal(await result, 124);
  assert.ok(!events(directory).some((event) => event.event === "spawn"));
});

test("a hung command is sampled before termination and fails within the inner budget", async (t) => {
  let sampled = false;
  const { directory, result } = run(
    t,
    `
    console.log("Test run for test.dll");
    setInterval(() => {}, 1000);
  `,
    {
      timeoutMs: 1_000,
      sample: async () => {
        sampled = true;
        throw new Error("unavailable");
      },
      inspect: async () => {
        throw new Error("unavailable");
      },
    },
  );
  assert.equal(await result, 124);
  assert.ok(sampled);
  assert.equal(
    events(directory).find((event) => event.event === "deadline-exceeded")
      .phase,
    "testhost-startup",
  );
  assert.ok(
    events(directory).some(
      (event) => event.event === "process-snapshot-unavailable",
    ),
  );
  assert.ok(
    events(directory).some((event) => event.event === "samples-unavailable"),
  );
});

test("missing executables preserve spawn failure", async (t) => {
  const { result } = run(t, "", {
    command: "nonexistent-dotnet-watchdog-test-command",
  });
  assert.equal(await result, 127);
});

test(
  "signal exits preserve the shell exit status",
  {
    skip: process.platform === "win32",
  },
  async (t) => {
    const { result } = run(t, "process.kill(process.pid, 'SIGTERM')");
    assert.equal(await result, 143);
  },
);

test(
  "owned descendants retaining pipes cannot hide the first command failure",
  {
    skip: process.platform === "win32",
  },
  async (t) => {
    for (const code of [0, 37]) {
      let descendants = [];
      const { directory, result } = run(
        t,
        `
      const { spawn } = require("node:child_process");
      spawn(process.execPath, ["-e", "process.on('SIGTERM', () => {}); setInterval(() => {}, 1000)"],
        { stdio: ["ignore", 1, 2] }).unref();
      process.exit(${code});
    `,
        {
          timeoutMs: 1_000,
          inspect: collectProcesses,
          sample: async (processes) => {
            descendants = processes;
          },
        },
      );
      assert.equal(await result, code || 124);
      assert.equal(
        events(directory).find((event) => event.event === "deadline-exceeded")
          .phase,
        "output-drain",
      );
      assert.ok(descendants.length > 0);
      // kill(0) can still see a zombie briefly; ps must not see a live descendant.
      const remaining = await collectProcesses(descendants[0].group);
      assert.ok(remaining.every((process) => process.state.startsWith("Z")));
    }
  },
);

test(
  "termination requests retain diagnostics and a failing status",
  {
    skip: process.platform === "win32",
  },
  async (t) => {
    const directory = outputDirectory(t);
    const script = `
    import { runWithWatchdog } from ${JSON.stringify(new URL("./test-watchdog.mjs", import.meta.url).href)};
    process.exitCode = await runWithWatchdog({
      command: process.execPath,
      args: ["-e", "process.stdout.write('ready\\\\n'); setInterval(() => {}, 1000)"],
      directory: ${JSON.stringify(directory)}, timeoutMs: 5000, graceMs: 25,
      inspect: async () => [], sample: async () => {},
    });
  `;
    const child = spawn(
      process.execPath,
      ["--input-type=module", "-e", script],
      { stdio: ["ignore", "pipe", "inherit"] },
    );
    t.after(() => child.kill("SIGKILL"));
    const exited = new Promise((resolve) => child.on("exit", resolve));
    await new Promise((resolve) => child.stdout.once("data", resolve));
    child.kill("SIGTERM");
    assert.equal(await exited, 143);
    assert.ok(events(directory).some((event) => event.event === "terminated"));
  },
);

test(
  "macOS sample captures an owned process call graph without raw files",
  {
    skip: process.platform !== "darwin",
  },
  async (t) => {
    const directory = outputDirectory(t);
    const child = spawn(
      process.execPath,
      ["-e", "process.stdout.write('ready'); setInterval(() => {}, 1000)"],
      {
        stdio: ["ignore", "pipe", "inherit"],
      },
    );
    t.after(() => child.kill("SIGKILL"));
    await new Promise((resolve) => child.stdout.once("data", resolve));
    const records = [];
    await collectSamples([{ pid: child.pid }], directory, (record) =>
      records.push(record),
    );
    assert.equal(records[0].event, "sample");
    const stacks = readFileSync(
      join(directory, `sample-${child.pid}.txt`),
      "utf8",
    );
    assert.notEqual(stacks, "No call graph available");
    assert.ok(!stacks.includes("Binary Images:"));
  },
);
