# .NET CI hang diagnostics

The macOS and Windows default/CAPI shard 1 jobs run the **unchanged** `dotnet test`
command through `test-watchdog.mjs`. Frameworks, filters, environment, and the
10-minute per-test blame timeout are unchanged. The watchdog deadline is the
earlier of 15 minutes after command launch or 16 minutes after checkout. The
20-minute job limit is unchanged; the remaining time is for diagnostics, cleanup,
and the two-minute artifact upload.

On Windows, the workflow builds `WindowsWatchdog.csproj` with the already
installed .NET 10 SDK and restores the pinned Microsoft `dotnet-stack` tool from
`dotnet-tools.json`. The helper joins a kill-on-close Windows Job Object **before**
starting the command. Nested jobs contain grandchildren even when ancestors exit
between snapshots. Cleanup never uses executable-name matching or an unrelated
process search. Held process handles prevent sampled PIDs from being reused.

The root command exiting is not sufficient: the helper also waits for stdout and
stderr EOF. A descendant retaining either pipe therefore still reaches the
watchdog deadline, preserving an earlier command failure (otherwise exit 124).
After a normal root exit and EOF, any remaining background servers are cleaned
up without changing the command's result. Killing the supervisor also closes its
job and terminates its owned descendants. Supervisor/launch failures stay failures.

## Reading the next CI artifact

Download `dotnet-test-diagnostics-windows-latest-default-capi-1-<attempt>`:

- `watchdog.jsonl`: allowlisted build/provisioning/test/shutdown phase markers,
  recognized target framework, completed test method names (no argument values),
  exit-versus-output-drain timing, deadline, inspection errors, and final status.
- `windows-job.jsonl`: append-only snapshots on a five-second cadence, plus
  lifecycle changes. Only owned PIDs, known executable roles, runtime kind,
  CPU/RSS, process start times, and numeric thread states/wait reasons are stored.
  Snapshots cover at most 128 processes and 64 threads per process; total
  `processCount`/`threadCount` values expose truncation.
- `managed-stack-<pid>.txt`: at the watchdog deadline, readable .NET managed
  thread stacks for up to four owned CoreCLR processes, prioritizing testhosts.
  Each collector has a ten-second deadline (including startup), a one-second
  forced-close bound, and its own Job Object. Collector stdout/stderr are captured
  in memory only, capped at 4 MiB; artifacts retain only thread IDs, native boundaries, and
  module/method names, capped at 4,096 lines / 64 KiB. Truncation and unavailable
  captures are explicit. `stack-collector-<pid>/windows-job.jsonl` diagnoses the
  collector itself.
- Existing TRX and blame sequence files: correlate the framework and last
  completed test with the test host's existing failure/active-test evidence.
- `watchdog-runtime.json`: pinned CLI version, platform, architecture, and Node.

`dotnet-stack` supports CoreCLR, not .NET Framework. Framework processes get an
explicit `managed-stack-unsupported` event, numeric thread information, and the
existing blame/TRX diagnostics. Native CLI stacks are not collected on Windows.
Thread stacks are not a dump of suspended async state machines; an off-thread
await may still need follow-up investigation. No heap/process dumps, environment,
command lines, arbitrary console output, or locals are added to diagnostic
artifacts. The existing TRX/blame artifact behavior is unchanged.

Instrumentation does not establish the cause of the original Windows timeout.
Use the next failure's phase, framework, exit/EOF timing, test sequence and stacks
to distinguish provisioning, test execution, fixture disposal and pipe retention
before attributing it or changing SDK behavior.

## Focused local validation (Windows)

From `dotnet`:

```powershell
dotnet tool restore --tool-manifest ci\dotnet-tools.json
dotnet build ci\WindowsWatchdog.csproj -c Release -p:UseSharedCompilation=false
dotnet format ci\WindowsWatchdog.csproj --no-restore --verify-no-changes
node --test --test-timeout=30000 ci\test-watchdog.test.mjs
node --test --test-timeout=60000 ci\test-watchdog-windows.test.mjs
```

The Windows-only controls cover orphaned pipe holders, original failure
preservation, unrelated-process survival, supervisor termination, normal EOF with
background servers, and real managed waiting-stack capture. `--stack-probe` on the
helper is their small managed fixture, not part of SDK test selection. The shared
suite retains its existing POSIX-only controls; run it on macOS to exercise native
sampling and process-group behavior.

Other CI entry points may import `runWithWatchdog` and supply `command`, `args`,
an absolute artifact `directory`, and `timeoutMs`. Optional `marker` and `label`
parameters default to `progressMarker` and `".NET"`; a custom marker must return
only allowlisted metadata or `null`, never raw console output or argument values.
This lets the Go macOS entry point reuse process-group cleanup and native sampling
without duplicating the watchdog. Importing the module does not execute its .NET
CLI entry point.
