using System.ComponentModel;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Runtime.Versioning;
using System.Text.Json;
using System.Text.Json.Serialization;

[assembly: SupportedOSPlatform("windows")]

namespace GitHub.Copilot.Ci;

internal static class WindowsWatchdog
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        PropertyNameCaseInsensitive = true,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
    };

    private static int Main(string[] args)
    {
        if (args is ["--stack-probe"])
        {
            Console.WriteLine("secret test payload");
            WaitForStackProbe();
            return 0;
        }

        var state = new Status();
        var tracked = new Dictionary<int, Process>();
        WindowsJob? job = null;
        bool completed = false;
        // Append-only IPC avoids Windows rename/delete sharing races with readers.
        using var output = new StreamWriter(new FileStream(args[0], FileMode.Append, FileAccess.Write, FileShare.Read));
        output.AutoFlush = true;
        try
        {
            job = new WindowsJob();
            var request = JsonSerializer.Deserialize<Request>(Console.ReadLine()!, JsonOptions)!;
            var start = new ProcessStartInfo(request.Command)
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardInput = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
            };
            foreach (string argument in request.Args)
                start.ArgumentList.Add(argument);
            using var root = Process.Start(start)!;
            root.StandardInput.Close();
            state.RootPid = root.Id;
            Task drained = Task.WhenAll(
                root.StandardOutput.BaseStream.CopyToAsync(Console.OpenStandardOutput()),
                root.StandardError.BaseStream.CopyToAsync(Console.OpenStandardError()));
            long nextSnapshot = 0;
            do
            {
                int? previousExitCode = state.ExitCode;
                if (root.HasExited)
                    state.ExitCode = root.ExitCode;
                int[] ids = job.ProcessIds().Where(id => id != Environment.ProcessId).ToArray();
                bool closing = state.ExitCode.HasValue && drained.IsCompleted;
                if (closing)
                    drained.GetAwaiter().GetResult();
                if (closing || state.ExitCode != previousExitCode || Environment.TickCount64 >= nextSnapshot)
                {
                    state.ProcessCount = ids.Length;
                    state.Processes = ids.Take(128).Select(id => Snapshot(id, job, tracked)).ToArray();
                    state.OutputClosed = closing;
                    output.WriteLine(JsonSerializer.Serialize(state, JsonOptions));
                    nextSnapshot = Environment.TickCount64 + 5_000;
                }
                if (closing)
                {
                    // Compiler servers may outlive a successful command without
                    // holding its pipes. Clean them up, but only after BOTH the
                    // actual root exit and output EOF, never merely root exit.
                    if (ids.Length != 0)
                        job.Abort(state.ExitCode!.Value);
                    break;
                }
                Thread.Sleep(250);
            } while (true);
            job.Complete();
            completed = true;
            return state.ExitCode!.Value;
        }
        catch (Exception error) when (error is Win32Exception or InvalidOperationException or IOException or JsonException or ArgumentException)
        {
            state.Error = error.GetType().Name;
            state.ErrorCode = error.HResult;
            state.ExitCode = state.ExitCode is null or 0 ? 127 : state.ExitCode;
            Console.Error.WriteLine($"[.NET watchdog] Windows supervisor failed: {state.Error}");
            output.WriteLine(JsonSerializer.Serialize(state, JsonOptions));
            return state.ExitCode.Value;
        }
        finally
        {
            foreach (Process process in tracked.Values)
                process.Dispose();
            if (job is not null)
            {
                // Never turn an inspector/supervisor failure into success. Abort
                // gives the supervisor AND its descendants a nonzero exit code.
                if (!completed)
                    job.Abort(state.ExitCode is null or 0 ? 127 : state.ExitCode.Value);
                job.Dispose();
            }
        }
    }

    private static object Snapshot(int pid, WindowsJob job, Dictionary<int, Process> tracked)
    {
        try
        {
            if (!tracked.TryGetValue(pid, out Process? process))
            {
                process = Process.GetProcessById(pid);
                try
                {
                    if (!job.Owns(process.Handle))
                        throw new InvalidOperationException();
                    // Retain the handle while sampling so an exited PID cannot
                    // be recycled into an unrelated diagnostic target.
                    tracked.Add(pid, process);
                }
                catch
                {
                    process.Dispose();
                    throw;
                }
            }
            process.Refresh();
            string role = process.ProcessName.ToLowerInvariant();
            if (role is not ("dotnet" or "testhost" or "testhost.x86" or "msbuild" or "copilot" or "copilot-runtime" or "node" or "tar" or "pwsh"))
                role = "other";
            string runtime = "native";
            foreach (ProcessModule module in process.Modules)
            {
                if (module.ModuleName.Equals("coreclr.dll", StringComparison.OrdinalIgnoreCase))
                {
                    runtime = "core";
                    break;
                }
                if (module.ModuleName.Equals("clr.dll", StringComparison.OrdinalIgnoreCase))
                {
                    runtime = "framework";
                    break;
                }
            }
            ProcessThreadCollection threads = process.Threads;
            return new
            {
                pid,
                role,
                runtime,
                cpuMs = Math.Round(process.TotalProcessorTime.TotalMilliseconds),
                rssKiB = Math.Round(process.WorkingSet64 / 1024d),
                started = process.StartTime.ToUniversalTime().ToString("O"),
                threadCount = threads.Count,
                threads = threads.Cast<ProcessThread>().Take(64).Select(ThreadSnapshot).ToArray(),
            };
        }
        catch (Exception error) when (error is ArgumentException or InvalidOperationException or Win32Exception)
        {
            return new { pid, unavailable = error.GetType().Name, code = error.HResult };
        }
    }

    private static object ThreadSnapshot(ProcessThread thread)
    {
        try
        {
            System.Diagnostics.ThreadState state = thread.ThreadState;
            return new
            {
                id = thread.Id,
                state = (int)state,
                wait = state == System.Diagnostics.ThreadState.Wait ? (int?)thread.WaitReason : null,
            };
        }
        catch (InvalidOperationException error)
        {
            return new { id = thread.Id, unavailable = error.GetType().Name };
        }
    }

    [MethodImpl(MethodImplOptions.NoInlining)]
    private static void WaitForStackProbe() => Thread.Sleep(Timeout.Infinite);

    private sealed record Request(string Command, string[] Args);

    private sealed class Status
    {
        public int? RootPid { get; set; }
        public int? ExitCode { get; set; }
        public int ProcessCount { get; set; }
        public bool OutputClosed { get; set; }
        public object[] Processes { get; set; } = [];
        public string? Error { get; set; }
        public int? ErrorCode { get; set; }
    }
}
