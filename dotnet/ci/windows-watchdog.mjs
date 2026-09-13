import { spawn } from "node:child_process";
import {
  closeSync,
  mkdirSync,
  openSync,
  readSync,
  writeFileSync,
} from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

export const windowsSupervisorPath = fileURLToPath(
  new URL("./bin/Release/net10.0/WindowsWatchdog.dll", import.meta.url),
);

export function startWindowsJob(command, args, directory, cwd) {
  const statusPath = join(directory, "windows-job.jsonl");
  writeFileSync(statusPath, "");
  const child = spawn("dotnet", [windowsSupervisorPath, statusPath], {
    stdio: ["pipe", "pipe", "pipe"],
    cwd,
    windowsHide: true,
  });
  // Arguments are transmitted in memory, not persisted or shell-interpolated.
  child.stdin.on("error", (error) => {
    if (error.code !== "EPIPE" && error.code !== "EOF")
      child.emit("error", error);
  });
  child.stdin.end(`${JSON.stringify({ command, args })}\n`);
  let offset = 0;
  let pending = "";
  let last = { processes: [] };
  return {
    child,
    status() {
      let fd;
      try {
        fd = openSync(statusPath, "r");
      } catch (error) {
        if (error.code === "ENOENT") return last;
        throw error;
      }
      try {
        const buffer = Buffer.alloc(1024 * 1024);
        const count = readSync(fd, buffer, 0, buffer.length, offset);
        offset += count;
        pending += buffer.toString("utf8", 0, count);
        const end = pending.lastIndexOf("\n");
        if (end !== -1) {
          const start = pending.lastIndexOf("\n", end - 1) + 1;
          last = JSON.parse(pending.slice(start, end));
          pending = pending.slice(end + 1);
        }
        if (pending.length > buffer.length)
          throw new Error("Windows status exceeded its size limit");
        return last;
      } finally {
        closeSync(fd);
      }
    },
  };
}

async function stackReport(pid, directory) {
  const started = performance.now();
  const statusDirectory = join(directory, `stack-collector-${pid}`);
  mkdirSync(statusDirectory, { recursive: true });
  const { child, status } = startWindowsJob(
    "dotnet",
    [
      "tool",
      "run",
      "dotnet-stack",
      "--",
      "report",
      "--process-id",
      String(pid),
    ],
    statusDirectory,
    import.meta.dirname,
  );
  // The tool launcher can also retain a child's pipes. Use the same owned job
  // as the test command instead of execFile's parent-only Windows timeout kill.
  return await new Promise((resolve, reject) => {
    let output = "";
    let size = 0;
    let failure;
    let forceClose;
    let settled = false;
    const finish = (error) => {
      if (settled) return;
      settled = true;
      clearTimeout(deadline);
      clearTimeout(forceClose);
      if (error) reject(error);
      else resolve(output);
    };
    const stop = (code) => {
      failure ??= Object.assign(new Error(code), { code });
      child.kill("SIGKILL");
      forceClose ??= setTimeout(() => {
        child.stdout.destroy();
        child.stderr.destroy();
        child.unref();
        finish(failure);
      }, 1_000);
    };
    const deadline = setTimeout(
      () => stop("STACK_TIMEOUT"),
      Math.max(0, 10_000 - (performance.now() - started)),
    );
    for (const stream of [child.stdout, child.stderr]) {
      stream.setEncoding("utf8");
      stream.on("data", (chunk) => {
        size += Buffer.byteLength(chunk);
        if (size > 4 * 1024 * 1024) stop("STACK_OUTPUT_LIMIT");
        else if (stream === child.stdout) output += chunk;
      });
    }
    child.on("error", (error) => {
      failure ??= error;
    });
    child.on("close", (code) => {
      try {
        if (failure) finish(failure);
        else if (code !== 0 || status().exitCode !== 0)
          finish(Object.assign(new Error("Stack collector failed"), { code }));
        else finish();
      } catch (error) {
        finish(error);
      }
    });
  });
}

export function managedStacks(output) {
  // dotnet-stack prints type/method signatures, never values. Retain only
  // thread IDs, native boundaries and module!method names, excluding headers,
  // paths and even parameter signatures. Do not persist the raw EventPipe data.
  const lines = output.split(/\r?\n/).flatMap((line) => {
    if (/^Thread \(0x[0-9a-f]+\):$/i.test(line)) return [line];
    if (/^\s+\[Native Frames\]$/.test(line)) return [line.trim()];
    const frame =
      /^\s+([A-Za-z0-9_.$+`<>,[\]:-]+![A-Za-z0-9_.$+`<>,[\]:-]+)(?:\(|$)/.exec(
        line,
      );
    return frame ? [`  ${frame[1]}`] : [];
  });
  let result = lines.slice(0, 4096).join("\n").slice(0, 65_536);
  if (lines.length > 4096 || lines.join("\n").length > 65_536)
    result = `${result.slice(0, 65_500)}\n[Stack output truncated]`;
  for (const name of [
    "COPILOT_HMAC_KEY",
    "GH_TOKEN",
    "GITHUB_TOKEN",
    "COPILOT_GITHUB_TOKEN",
  ]) {
    if (process.env[name])
      result = result.replaceAll(process.env[name], "[REDACTED]");
  }
  return result;
}

export async function collectWindowsStacks(processes, directory, record) {
  const managed = processes.filter(({ runtime }) => runtime === "core");
  for (const { pid, runtime } of processes) {
    if (runtime === "framework")
      record({ event: "managed-stack-unsupported", pid, runtime });
  }
  // Testhosts first, then the build/test orchestrators. Four ten-second caps
  // leave most of the four-minute job reserve for cleanup and artifact upload.
  managed.sort(
    (a, b) =>
      Number(b.role.startsWith("testhost")) -
      Number(a.role.startsWith("testhost")),
  );
  if (managed.length > 4)
    record({
      event: "managed-stack-target-limit",
      available: managed.length,
      limit: 4,
    });
  for (const { pid } of managed.slice(0, 4)) {
    try {
      const stdout = await stackReport(pid, directory);
      const stacks = managedStacks(stdout);
      if (!stacks) {
        record({ event: "managed-stack-empty", pid });
        continue;
      }
      writeFileSync(join(directory, `managed-stack-${pid}.txt`), stacks);
      record({ event: "managed-stack", pid });
    } catch (error) {
      record({
        event: "managed-stack-unavailable",
        pid,
        code: error.code,
        signal: error.signal,
        killed: error.killed,
      });
    }
  }
}
