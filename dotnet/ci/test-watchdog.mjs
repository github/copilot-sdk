import { execFile, spawn } from "node:child_process";
import {
  appendFileSync,
  mkdirSync,
  readFileSync,
  writeFileSync,
} from "node:fs";
import { basename, join, resolve } from "node:path";
import { constants } from "node:os";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const exec = promisify(execFile);

// Persist only recognized markers, never arbitrary console output (Actions'
// secret masking does not apply to artifact files).
export function progressMarker(line) {
  if (/\b_DownloadCopilotCli:/.test(line))
    return { phase: "runtime-provisioning" };
  if (/\bCoreCompile:/.test(line)) return { phase: "compilation" };
  if (/\b_CopyCopilotCliToOutput:/.test(line)) return { phase: "runtime-copy" };
  if (/^Test run for /.test(line)) return { phase: "testhost-startup" };
  if (/^Starting test execution,/.test(line))
    return { phase: "test-discovery" };
  if (/^\[xUnit\.net [\d:.]+\]\s+Starting:/.test(line)) {
    return { phase: "tests-and-fixture-cleanup" };
  }
  if (/^\[xUnit\.net [\d:.]+\]\s+Finished:/.test(line)) {
    return { phase: "testhost-shutdown" };
  }
  if (/^Test Run (Successful|Failed|Aborted)\./.test(line)) {
    return { phase: "test-command-shutdown" };
  }
  const test =
    /^\s*(Passed|Failed|Skipped) (GitHub\.Copilot\.Test\.[A-Za-z0-9_.]+)(?=[(\s]|$)/.exec(
      line,
    );
  if (test) return { outcome: test[1], test: test[2] };
  return null;
}

export function ownedProcesses(output, group) {
  return output.split("\n").flatMap((line) => {
    const match =
      /^\s*(\d+)\s+(\d+)\s+(\d+)\s+(\S+)\s+([\d.]+)\s+(\d+)\s+([\d:-]+)\s+(.+?)\s*$/.exec(
        line,
      );
    if (!match || Number(match[3]) !== group) return [];
    const name = basename(match[8]);
    return [
      {
        pid: Number(match[1]),
        ppid: Number(match[2]),
        group,
        state: match[4],
        cpu: Number(match[5]),
        rssKiB: Number(match[6]),
        elapsed: match[7],
        role: /^(dotnet|testhost|copilot|copilot-runtime|node|tar)$/.test(name)
          ? name
          : "other",
      },
    ];
  });
}

export function sampleStacks(output) {
  // sample reports native stacks, not memory or local variables. Omit its
  // process/path headers and binary-image paths as well.
  const graph =
    /Call graph:\r?\n([\s\S]*?)(?:\r?\nTotal number in stack|\r?\nBinary Images:|$)/.exec(
      output,
    );
  let stacks = graph?.[1] ?? "No call graph available";
  for (const name of [
    "COPILOT_HMAC_KEY",
    "GH_TOKEN",
    "GITHUB_TOKEN",
    "COPILOT_GITHUB_TOKEN",
  ]) {
    if (process.env[name])
      stacks = stacks.replaceAll(process.env[name], "[REDACTED]");
  }
  return stacks;
}

export async function collectProcesses(group) {
  const { stdout } = await exec(
    "ps",
    ["-axo", "pid=,ppid=,pgid=,stat=,%cpu=,rss=,etime=,comm="],
    {
      timeout: 3_000,
      killSignal: "SIGKILL",
      maxBuffer: 4 * 1024 * 1024,
      env: { ...process.env, LC_ALL: "C" },
    },
  );
  return ownedProcesses(stdout, group);
}

export async function collectSamples(processes, directory, record) {
  if (process.platform !== "darwin") return;
  // Bound diagnostics too: eight one-second samples, each capped at five seconds.
  for (const { pid } of processes.slice(0, 8)) {
    try {
      // Explicit stdout avoids sample's default on-disk report. Only the
      // filtered call graph below is written to the artifact directory.
      const { stdout } = await exec(
        "/usr/bin/sample",
        [String(pid), "1", "1", "-file", "/dev/stdout"],
        {
          timeout: 5_000,
          killSignal: "SIGKILL",
          maxBuffer: 4 * 1024 * 1024,
        },
      );
      writeFileSync(join(directory, `sample-${pid}.txt`), sampleStacks(stdout));
      record({ event: "sample", pid });
    } catch {
      record({ event: "sample-unavailable", pid });
    }
  }
}

export async function runWithWatchdog({
  command = "dotnet",
  args,
  directory,
  timeoutMs,
  intervalMs = 60_000,
  graceMs = 5_000,
  inspect = collectProcesses,
  sample = collectSamples,
  forwardOutput = true,
}) {
  mkdirSync(directory, { recursive: true });
  const started = performance.now();
  let phase = "build-startup";
  let finalized = false;
  const record = (data) =>
    !finalized &&
    appendFileSync(
      join(directory, "watchdog.jsonl"),
      `${JSON.stringify({ at: new Date().toISOString(), elapsedMs: Math.round(performance.now() - started), phase, ...data })}\n`,
    );
  record({ event: "start", timeoutMs });
  if (timeoutMs <= 0) {
    record({ event: "deadline-expired-before-start" });
    return 124;
  }

  // On macOS the child leads a process group. Kill only that owned group, even
  // if dotnet exits while a descendant still holds its output pipe open.
  const child = spawn(command, args, {
    detached: process.platform !== "win32",
    stdio: ["ignore", "pipe", "pipe"],
  });
  let result;
  let stopping = false;
  let finish;
  const completed = new Promise((resolve) => {
    finish = resolve;
  });
  const signal = (name) => {
    try {
      if (process.platform === "win32") child.kill(name);
      else process.kill(-child.pid, name);
    } catch (error) {
      if (error.code !== "ESRCH")
        record({ event: "signal-failed", signal: name });
    }
  };
  const snapshot = async () => {
    try {
      const processes = await inspect(child.pid);
      record({ event: "processes", processes });
      return processes;
    } catch {
      record({ event: "process-snapshot-unavailable" });
      return [];
    }
  };
  const stop = async (reason, code) => {
    if (stopping) return;
    stopping = true;
    result = result || code;
    record({ event: reason });
    if (forwardOutput)
      console.error(
        `[.NET watchdog] ${reason} during ${phase}; preserving diagnostics.`,
      );
    clearInterval(heartbeat);
    clearTimeout(deadline);
    const processes = await snapshot();
    // Actions allows only a short signal grace period on cancellation. Keep
    // the already-written timeline and snapshot; sample only our own deadline.
    if (reason === "deadline-exceeded") {
      try {
        await sample(processes, directory, record);
      } catch {
        record({ event: "samples-unavailable" });
      }
    }
    signal("SIGTERM");
    await new Promise((resolve) =>
      setTimeout(
        resolve,
        reason === "deadline-exceeded" ? graceMs : Math.min(graceMs, 1_000),
      ),
    );
    signal("SIGKILL");
    child.stdout.destroy();
    child.stderr.destroy();
    child.unref();
    record({ event: "stopped", exitCode: result });
    finish();
  };
  const onInterrupt = () => {
    void stop("interrupted", 130);
  };
  const onTerminate = () => {
    void stop("terminated", 143);
  };
  process.on("SIGINT", onInterrupt);
  process.on("SIGTERM", onTerminate);

  for (const [stream, destination] of [
    [child.stdout, process.stdout],
    [child.stderr, process.stderr],
  ]) {
    if (forwardOutput) stream.pipe(destination);
    let pending = "";
    stream.setEncoding("utf8");
    stream.on("data", (chunk) => {
      pending += chunk;
      let newline;
      while ((newline = pending.indexOf("\n")) !== -1) {
        const marker = progressMarker(pending.slice(0, newline));
        if (marker) {
          if (marker.phase) phase = marker.phase;
          record({ event: "progress", ...marker });
        }
        pending = pending.slice(newline + 1);
      }
      // Compiler invocations can be very long; none are diagnostic markers.
      if (pending.length > 16_384) pending = "";
    });
  }
  child.on("spawn", () => record({ event: "spawn", pid: child.pid }));
  child.on("error", () => {
    result ??= 127;
    record({ event: "spawn-error" });
  });
  child.on("exit", (code, exitSignal) => {
    result ??= code ?? (exitSignal ? 128 + constants.signals[exitSignal] : 1);
    record({ event: "command-exit", exitCode: code, signal: exitSignal });
    phase = "output-drain";
  });
  child.on("close", () => {
    record({ event: "output-closed" });
    if (!stopping) finish();
  });
  const heartbeat = setInterval(() => {
    void snapshot();
  }, intervalMs);
  const deadline = setTimeout(() => {
    void stop("deadline-exceeded", 124);
  }, timeoutMs);
  await completed;
  clearInterval(heartbeat);
  clearTimeout(deadline);
  process.off("SIGINT", onInterrupt);
  process.off("SIGTERM", onTerminate);
  record({ event: "finish", exitCode: result });
  finalized = true;
  return result ?? 1;
}

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const directory = resolve("TestResults");
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
    }),
  );
  const deadline = Number(process.env.DOTNET_TEST_DEADLINE);
  if (
    !Number.isFinite(deadline) ||
    deadline <= 0 ||
    process.platform !== "darwin"
  ) {
    throw new Error(
      "The .NET CI watchdog requires macOS and DOTNET_TEST_DEADLINE",
    );
  }
  process.exitCode = await runWithWatchdog({
    args: process.argv.slice(2),
    directory,
    timeoutMs: Math.min(15 * 60_000, deadline - Date.now()),
  });
}
