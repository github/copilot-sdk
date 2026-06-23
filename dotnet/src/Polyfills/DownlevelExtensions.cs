/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Buffers;
using System.ComponentModel;
using System.Globalization;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text;

namespace System
{
    internal static class DownlevelArgumentNullExceptionExtensions
    {
        extension(ArgumentNullException)
        {
            public static void ThrowIfNull(object? argument, [CallerArgumentExpression(nameof(argument))] string? paramName = null)
            {
                if (argument is null)
                {
                    throw new ArgumentNullException(paramName);
                }
            }
        }
    }

    internal static class DownlevelObjectDisposedExceptionExtensions
    {
        extension(ObjectDisposedException)
        {
            public static void ThrowIf(bool condition, object instance)
            {
                if (condition)
                {
                    throw new ObjectDisposedException(instance?.GetType().FullName);
                }
            }
        }
    }

    internal static class DownlevelArgumentExceptionExtensions
    {
        extension(ArgumentException)
        {
            public static void ThrowIfNullOrWhiteSpace(string? argument, [CallerArgumentExpression(nameof(argument))] string? paramName = null)
            {
                if (argument is null)
                {
                    throw new ArgumentNullException(paramName);
                }

                if (string.IsNullOrWhiteSpace(argument))
                {
                    throw new ArgumentException("The value cannot be an empty string or composed entirely of whitespace.", paramName);
                }
            }
        }
    }

    internal static class DownlevelDateTimeExtensions
    {
        extension(DateTime)
        {
            public static DateTime UnixEpoch => new(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        }
    }

    internal static class DownlevelDateTimeOffsetExtensions
    {
        extension(DateTimeOffset)
        {
            public static DateTimeOffset UnixEpoch => new(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
        }
    }

    internal static class DownlevelIntExtensions
    {
        extension(int)
        {
            public static bool TryParse(ReadOnlySpan<byte> utf8Text, NumberStyles style, IFormatProvider? provider, out int result)
            {
                if (style == NumberStyles.None)
                {
                    return TryParseNonNegativeInt32(utf8Text, out result);
                }

                return int.TryParse(Encoding.UTF8.GetString(utf8Text.ToArray()), style, provider, out result);
            }
        }

        private static bool TryParseNonNegativeInt32(ReadOnlySpan<byte> utf8Text, out int result)
        {
            if (utf8Text.IsEmpty)
            {
                result = 0;
                return false;
            }

            var value = 0;
            foreach (var c in utf8Text)
            {
                var digit = c - (byte)'0';
                if ((uint)digit > 9)
                {
                    result = 0;
                    return false;
                }

                if (value > (int.MaxValue - digit) / 10)
                {
                    result = 0;
                    return false;
                }

                value = (value * 10) + digit;
            }

            result = value;
            return true;
        }
    }

    internal static class DownlevelOperatingSystemExtensions
    {
        extension(OperatingSystem)
        {
            public static bool IsWindows() => RuntimeInformation.IsOSPlatform(OSPlatform.Windows);

            public static bool IsLinux() => RuntimeInformation.IsOSPlatform(OSPlatform.Linux);

            public static bool IsMacOS() => RuntimeInformation.IsOSPlatform(OSPlatform.OSX);
        }
    }

    internal static class DownlevelDisposableExtensions
    {
        extension(IDisposable disposable)
        {
            public ValueTask DisposeAsync()
            {
                disposable.Dispose();
                return default;
            }
        }
    }
}

namespace System.Collections.Generic
{
    internal static class DownlevelKeyValuePairExtensions
    {
        extension<TKey, TValue>(KeyValuePair<TKey, TValue> pair)
        {
            public void Deconstruct(out TKey key, out TValue value)
            {
                key = pair.Key;
                value = pair.Value;
            }
        }
    }
}

namespace System.Diagnostics
{
    internal static class DownlevelStopwatchExtensions
    {
        extension(Stopwatch)
        {
            public static TimeSpan GetElapsedTime(long startingTimestamp) =>
                GetElapsedTime(startingTimestamp, Stopwatch.GetTimestamp());

            public static TimeSpan GetElapsedTime(long startingTimestamp, long endingTimestamp)
            {
                var elapsedTicks = endingTimestamp - startingTimestamp;
                return TimeSpan.FromTicks((long)(elapsedTicks * ((double)TimeSpan.TicksPerSecond / Stopwatch.Frequency)));
            }
        }
    }

    internal static class DownlevelProcessExtensions
    {
        extension(Process process)
        {
            public void Kill(bool entireProcessTree)
            {
                if (entireProcessTree)
                {
                    if (OperatingSystem.IsWindows())
                    {
                        using var taskKill = Process.Start(new ProcessStartInfo
                        {
                            FileName = "taskkill.exe",
                            Arguments = string.Format(CultureInfo.InvariantCulture, "/PID {0} /T /F", process.Id),
                            CreateNoWindow = true,
                            RedirectStandardError = true,
                            RedirectStandardOutput = true,
                            UseShellExecute = false,
                        });

                        if (taskKill is not null &&
                            taskKill.WaitForExit(milliseconds: 30_000) &&
                            (taskKill.ExitCode == 0 || process.HasExited))
                        {
                            return;
                        }
                    }
                    else
                    {
                        KillDescendantProcesses(process.Id);
                    }
                }

                if (!process.HasExited)
                {
                    process.Kill();
                }
            }

            public Task WaitForExitAsync(Threading.CancellationToken cancellationToken = default)
            {
                if (process.HasExited)
                {
                    return Task.CompletedTask;
                }

                if (cancellationToken.IsCancellationRequested)
                {
                    return Task.FromCanceled(cancellationToken);
                }

                var completion = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
                EventHandler handler = (_, _) => completion.TrySetResult(null);
                process.EnableRaisingEvents = true;
                process.Exited += handler;

                if (process.HasExited)
                {
                    completion.TrySetResult(null);
                }

                var cancellationRegistration = cancellationToken.CanBeCanceled
                    ? cancellationToken.Register(static state => ((TaskCompletionSource<object?>)state!).TrySetCanceled(), completion)
                    : default;

                return WaitForExitAsyncCore(process, completion.Task, handler, cancellationRegistration);
            }
        }

        private static async Task WaitForExitAsyncCore(
            Process process,
            Task waitTask,
            EventHandler handler,
            Threading.CancellationTokenRegistration cancellationRegistration)
        {
            using var _ = cancellationRegistration;
            try
            {
                await waitTask.ConfigureAwait(false);
            }
            finally
            {
                process.Exited -= handler;
            }
        }

        private static void KillDescendantProcesses(int parentProcessId)
        {
            foreach (var childProcessId in GetChildProcessIds(parentProcessId))
            {
                KillDescendantProcesses(childProcessId);

                try
                {
                    using var childProcess = Process.GetProcessById(childProcessId);
                    if (!childProcess.HasExited)
                    {
                        childProcess.Kill();
                    }
                }
                catch (Exception ex) when (ex is ArgumentException or InvalidOperationException or Win32Exception or PlatformNotSupportedException)
                {
                    IgnoreBestEffortProcessException(ex);
                }
            }
        }

        private static List<int> GetChildProcessIds(int parentProcessId)
        {
            var childProcessIds = new List<int>();

            try
            {
                using var pgrep = Process.Start(new ProcessStartInfo
                {
                    FileName = "pgrep",
                    Arguments = string.Format(CultureInfo.InvariantCulture, "-P {0}", parentProcessId),
                    CreateNoWindow = true,
                    RedirectStandardError = true,
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                });

                if (pgrep is null)
                {
                    return childProcessIds;
                }

                var output = pgrep.StandardOutput.ReadToEnd();
                if (!pgrep.WaitForExit(milliseconds: 5_000))
                {
                    pgrep.Kill();
                    return childProcessIds;
                }

                childProcessIds.AddRange(
                    output.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries)
                        .Select(static line =>
                        {
                            var success = int.TryParse(line, NumberStyles.None, CultureInfo.InvariantCulture, out var childProcessId);
                            return (success, childProcessId);
                        })
                        .Where(static result => result.success)
                        .Select(static result => result.childProcessId));
            }
            catch (Exception ex) when (ex is ObjectDisposedException or InvalidOperationException or Win32Exception or PlatformNotSupportedException)
            {
                IgnoreBestEffortProcessException(ex);
            }

            return childProcessIds;
        }

        private static void IgnoreBestEffortProcessException(Exception exception) =>
            Debug.WriteLine(exception.ToString());
    }
}

namespace System.IO
{
    internal static class DownlevelStreamExtensions
    {
        extension(Stream stream)
        {
            public ValueTask<int> ReadAsync(Memory<byte> buffer, Threading.CancellationToken cancellationToken = default)
            {
                if (MemoryMarshal.TryGetArray(buffer, out ArraySegment<byte> segment))
                {
                    return new ValueTask<int>(stream.ReadAsync(segment.Array!, segment.Offset, segment.Count, cancellationToken));
                }

                return ReadAsyncSlow(stream, buffer, cancellationToken);
            }

            public ValueTask WriteAsync(ReadOnlyMemory<byte> buffer, Threading.CancellationToken cancellationToken = default)
            {
                if (MemoryMarshal.TryGetArray(buffer, out ArraySegment<byte> segment))
                {
                    return new ValueTask(stream.WriteAsync(segment.Array!, segment.Offset, segment.Count, cancellationToken));
                }

                return WriteAsyncSlow(stream, buffer, cancellationToken);
            }

            public async ValueTask ReadExactlyAsync(Memory<byte> buffer, Threading.CancellationToken cancellationToken = default)
            {
                var totalRead = 0;
                while (totalRead < buffer.Length)
                {
                    var bytesRead = await stream.ReadAsync(buffer.Slice(totalRead), cancellationToken).ConfigureAwait(false);
                    if (bytesRead <= 0)
                    {
                        throw new EndOfStreamException();
                    }

                    totalRead += bytesRead;
                }
            }

            public void Write(ReadOnlySpan<byte> buffer)
            {
                if (buffer.IsEmpty)
                {
                    return;
                }

                var rented = ArrayPool<byte>.Shared.Rent(buffer.Length);
                try
                {
                    buffer.CopyTo(rented);
                    stream.Write(rented, 0, buffer.Length);
                }
                finally
                {
                    ArrayPool<byte>.Shared.Return(rented);
                }
            }
        }

        private static async ValueTask<int> ReadAsyncSlow(Stream stream, Memory<byte> buffer, Threading.CancellationToken cancellationToken)
        {
            var rented = ArrayPool<byte>.Shared.Rent(buffer.Length);
            try
            {
                var bytesRead = await stream.ReadAsync(rented, 0, buffer.Length, cancellationToken).ConfigureAwait(false);
                rented.AsMemory(0, bytesRead).CopyTo(buffer);
                return bytesRead;
            }
            finally
            {
                ArrayPool<byte>.Shared.Return(rented);
            }
        }

        private static async ValueTask WriteAsyncSlow(Stream stream, ReadOnlyMemory<byte> buffer, Threading.CancellationToken cancellationToken)
        {
            var rented = ArrayPool<byte>.Shared.Rent(buffer.Length);
            try
            {
                buffer.CopyTo(rented);
                await stream.WriteAsync(rented, 0, buffer.Length, cancellationToken).ConfigureAwait(false);
            }
            finally
            {
                ArrayPool<byte>.Shared.Return(rented);
            }
        }
    }

    internal static class DownlevelTextReaderExtensions
    {
        extension(TextReader reader)
        {
            public Task<string?> ReadLineAsync(Threading.CancellationToken cancellationToken)
            {
                var task = reader.ReadLineAsync();
                return cancellationToken.CanBeCanceled
                    ? WaitAsync(task, cancellationToken)
                    : task;
            }
        }

        private static async Task<T> WaitAsync<T>(Task<T> task, Threading.CancellationToken cancellationToken)
        {
            if (task.IsCompleted || !cancellationToken.CanBeCanceled)
            {
                return await task.ConfigureAwait(false);
            }

            var cancellationTask = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
            using var registration = cancellationToken.Register(static state => ((TaskCompletionSource<object?>)state!).TrySetCanceled(), cancellationTask);
            if (await Task.WhenAny(task, cancellationTask.Task).ConfigureAwait(false) != task)
            {
                throw new OperationCanceledException(cancellationToken);
            }

            return await task.ConfigureAwait(false);
        }
    }
}

namespace System.Net.Sockets
{
    internal static class DownlevelSocketExtensions
    {
        extension(Socket socket)
        {
            public Task ConnectAsync(string host, int port, Threading.CancellationToken cancellationToken)
            {
                if (cancellationToken.IsCancellationRequested)
                {
                    return Task.FromCanceled(cancellationToken);
                }

                var completion = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
                var connectState = new SocketConnectState(socket, completion);
                try
                {
                    socket.BeginConnect(
                        host,
                        port,
                        static asyncResult =>
                        {
                            var connectState = (SocketConnectState)asyncResult.AsyncState!;
                            try
                            {
                                connectState.Socket.EndConnect(asyncResult);
                                connectState.Completion.TrySetResult(null);
                            }
                            catch (SocketException ex)
                            {
                                connectState.Completion.TrySetException(ex);
                            }
                            catch (ObjectDisposedException ex)
                            {
                                connectState.Completion.TrySetException(ex);
                            }
                            catch (InvalidOperationException ex)
                            {
                                connectState.Completion.TrySetException(ex);
                            }
                            catch (Exception ex) when (!IsFatal(ex))
                            {
                                connectState.Completion.TrySetException(ex);
                            }
                        },
                        connectState);
                }
                catch (SocketException ex)
                {
                    completion.TrySetException(ex);
                }
                catch (ObjectDisposedException ex)
                {
                    completion.TrySetException(ex);
                }
                catch (InvalidOperationException ex)
                {
                    completion.TrySetException(ex);
                }
                catch (Exception ex) when (!IsFatal(ex))
                {
                    completion.TrySetException(ex);
                }

                return cancellationToken.CanBeCanceled
                    ? WaitAsync(completion.Task, socket.Dispose, cancellationToken)
                    : completion.Task;
            }
        }

        private static async Task WaitAsync(Task task, Action cancellationAction, Threading.CancellationToken cancellationToken)
        {
            if (task.IsCompleted)
            {
                await task.ConfigureAwait(false);
                return;
            }

            var cancellationTask = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
            using var registration = cancellationToken.Register(
                static state =>
                {
                    var cancellationState = (CancellationState)state!;
                    cancellationState.CancellationAction();
                    cancellationState.Completion.TrySetCanceled();
                },
                new CancellationState(cancellationTask, cancellationAction));

            if (await Task.WhenAny(task, cancellationTask.Task).ConfigureAwait(false) != task)
            {
                throw new OperationCanceledException(cancellationToken);
            }

            await task.ConfigureAwait(false);
        }

        private static bool IsFatal(Exception exception) =>
            exception is OutOfMemoryException or StackOverflowException or AccessViolationException or AppDomainUnloadedException;

        private sealed record CancellationState(TaskCompletionSource<object?> Completion, Action CancellationAction);

        private sealed record SocketConnectState(Socket Socket, TaskCompletionSource<object?> Completion);
    }
}

namespace System.Runtime.ExceptionServices
{
    internal static class DownlevelExceptionDispatchInfoExtensions
    {
        extension(ExceptionDispatchInfo)
        {
            public static void Throw(Exception exception)
            {
                ExceptionDispatchInfo.Capture(exception).Throw();
            }
        }
    }
}

namespace System.Runtime.InteropServices
{
    internal static class DownlevelRuntimeInformationExtensions
    {
        extension(RuntimeInformation)
        {
            public static string RuntimeIdentifier
            {
                get
                {
                    var os = OperatingSystem.IsWindows() ? "win" :
                        OperatingSystem.IsLinux() ? "linux" :
                        OperatingSystem.IsMacOS() ? "osx" :
                        RuntimeInformation.OSDescription.ToLowerInvariant().Replace(' ', '-');

                    var arch = RuntimeInformation.OSArchitecture switch
                    {
                        Architecture.X64 => "x64",
                        Architecture.X86 => "x86",
                        Architecture.Arm => "arm",
                        Architecture.Arm64 => "arm64",
                        _ => RuntimeInformation.OSArchitecture.ToString().ToLowerInvariant(),
                    };

                    return $"{os}-{arch}";
                }
            }
        }
    }
}

namespace System.Threading
{
    internal static class DownlevelCancellationTokenRegistrationExtensions
    {
        extension(CancellationTokenRegistration registration)
        {
            public ValueTask DisposeAsync()
            {
                registration.Dispose();
                return default;
            }
        }
    }
}

namespace System.Threading.Tasks
{
    internal static class DownlevelValueTaskExtensions
    {
        extension(ValueTask)
        {
            public static ValueTask<T> FromResult<T>(T result) => new(result);
        }
    }

    internal static class DownlevelTaskExtensions
    {
        extension(Task task)
        {
            public async Task WaitAsync(TimeSpan timeout, CancellationToken cancellationToken = default)
            {
                cancellationToken.ThrowIfCancellationRequested();

                using var delayCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
                var completed = await Task.WhenAny(task, Task.Delay(timeout, delayCts.Token)).ConfigureAwait(false);
                if (!ReferenceEquals(completed, task))
                {
                    cancellationToken.ThrowIfCancellationRequested();
                    throw new TimeoutException();
                }

                delayCts.Cancel();
                await task.ConfigureAwait(false);
            }
        }

        extension<T>(Task<T> task)
        {
            public async Task<T> WaitAsync(TimeSpan timeout, CancellationToken cancellationToken = default)
            {
                await ((Task)task).WaitAsync(timeout, cancellationToken).ConfigureAwait(false);
                return await task.ConfigureAwait(false);
            }
        }
    }
}

namespace System.Text
{
    internal static class DownlevelEncodingExtensions
    {
        extension(Encoding encoding)
        {
            public string GetString(ReadOnlySpan<byte> bytes)
            {
                if (bytes.IsEmpty)
                {
                    return string.Empty;
                }

                var rented = ArrayPool<byte>.Shared.Rent(bytes.Length);
                try
                {
                    bytes.CopyTo(rented);
                    return encoding.GetString(rented, 0, bytes.Length);
                }
                finally
                {
                    ArrayPool<byte>.Shared.Return(rented);
                }
            }
        }
    }
}

namespace System.Net.Http
{
    internal static class DownlevelHttpContentExtensions
    {
        extension(HttpContent content)
        {
            public Task<IO.Stream> ReadAsStreamAsync(Threading.CancellationToken cancellationToken) =>
                content.ReadAsStreamAsync();
        }
    }
}

namespace System.Net.WebSockets
{
    /// <summary>
    /// Polyfill for the <c>System.Net.WebSockets.ValueWebSocketReceiveResult</c>
    /// struct, which is unavailable on .NET Standard 2.0.
    /// </summary>
    internal readonly struct ValueWebSocketReceiveResult
    {
        public ValueWebSocketReceiveResult(int count, WebSocketMessageType messageType, bool endOfMessage)
        {
            Count = count;
            MessageType = messageType;
            EndOfMessage = endOfMessage;
        }

        public int Count { get; }

        public WebSocketMessageType MessageType { get; }

        public bool EndOfMessage { get; }
    }

    internal static class DownlevelWebSocketExtensions
    {
        extension(WebSocket socket)
        {
            public ValueTask SendAsync(ReadOnlyMemory<byte> buffer, WebSocketMessageType messageType, bool endOfMessage, Threading.CancellationToken cancellationToken)
            {
                if (Runtime.InteropServices.MemoryMarshal.TryGetArray(buffer, out ArraySegment<byte> segment))
                {
                    return new ValueTask(socket.SendAsync(segment, messageType, endOfMessage, cancellationToken));
                }

                return SendAsyncSlow(socket, buffer, messageType, endOfMessage, cancellationToken);
            }

            public ValueTask<ValueWebSocketReceiveResult> ReceiveAsync(Memory<byte> buffer, Threading.CancellationToken cancellationToken) =>
                ReceiveAsyncCore(socket, buffer, cancellationToken);
        }

        private static async ValueTask SendAsyncSlow(WebSocket socket, ReadOnlyMemory<byte> buffer, WebSocketMessageType messageType, bool endOfMessage, Threading.CancellationToken cancellationToken)
        {
            var rented = ArrayPool<byte>.Shared.Rent(buffer.Length);
            try
            {
                buffer.CopyTo(rented);
                await socket.SendAsync(new ArraySegment<byte>(rented, 0, buffer.Length), messageType, endOfMessage, cancellationToken).ConfigureAwait(false);
            }
            finally
            {
                ArrayPool<byte>.Shared.Return(rented);
            }
        }

        private static async ValueTask<ValueWebSocketReceiveResult> ReceiveAsyncCore(WebSocket socket, Memory<byte> buffer, Threading.CancellationToken cancellationToken)
        {
            if (Runtime.InteropServices.MemoryMarshal.TryGetArray(buffer, out ArraySegment<byte> segment))
            {
                var result = await socket.ReceiveAsync(segment, cancellationToken).ConfigureAwait(false);
                return new ValueWebSocketReceiveResult(result.Count, result.MessageType, result.EndOfMessage);
            }

            var rented = ArrayPool<byte>.Shared.Rent(buffer.Length);
            try
            {
                var result = await socket.ReceiveAsync(new ArraySegment<byte>(rented, 0, buffer.Length), cancellationToken).ConfigureAwait(false);
                new ReadOnlyMemory<byte>(rented, 0, result.Count).CopyTo(buffer);
                return new ValueWebSocketReceiveResult(result.Count, result.MessageType, result.EndOfMessage);
            }
            finally
            {
                ArrayPool<byte>.Shared.Return(rented);
            }
        }
    }
}
