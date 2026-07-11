/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.Logging;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Threading.Channels;

namespace GitHub.Copilot;

/// <summary>
/// Hosts the Copilot runtime in-process by loading the Rust cdylib (<c>runtime.node</c>)
/// and speaking JSON-RPC over its C ABI (FFI) instead of spawning a CLI child process
/// and communicating over stdio/TCP.
/// </summary>
/// <remarks>
/// The Rust <c>host_start</c> export spawns the residual TypeScript worker itself —
/// typically the packaged single-file CLI (<c>copilot --embedded-host</c>, which embeds
/// its own Node) or, for dev, <c>node dist-cli/index.js --embedded-host</c> — so the .NET
/// host never launches Node directly. JSON-RPC frames are pumped across the ABI: writes go
/// to <c>connection_write</c>; inbound frames arrive on a native callback that feeds
/// <see cref="ReceiveStream"/>.
/// <para>
/// The native interop layer has two implementations selected by target framework. On
/// modern .NET it uses source-generated <c>LibraryImport</c> P/Invoke with an
/// <c>UnmanagedCallersOnly</c> function-pointer callback, which is trim- and
/// NativeAOT-compatible. On <c>netstandard2.0</c> (which has neither <c>LibraryImport</c>
/// nor <c>NativeLibrary</c>) it falls back to classic delegate-based P/Invoke over a
/// hand-rolled <c>dlopen</c>/<c>LoadLibrary</c> loader. Because the library lives at a
/// runtime-resolved absolute path, the modern path maps the logical
/// <see cref="LibraryName"/> via a resolver and the legacy path loads the absolute path
/// directly.
/// </para>
/// </remarks>
internal sealed partial class FfiRuntimeHost : IDisposable
{
    /// <summary>Logical name the native interop layer binds the cdylib to.</summary>
    private const string LibraryName = "copilot_runtime";

    private readonly ILogger _logger;
    private readonly string _cliEntrypoint;
    private readonly string _libraryPath;
    private readonly IReadOnlyDictionary<string, string>? _environment;

    private readonly CallbackReceiveStream _receiveStream = new();
    private CallbackSendStream? _sendStream;

    private uint _serverId;
    private uint _connectionId;
    private bool _disposed;

    private FfiRuntimeHost(string libraryPath, string cliEntrypoint, IReadOnlyDictionary<string, string>? environment, ILogger logger)
    {
        _libraryPath = libraryPath;
        _cliEntrypoint = cliEntrypoint;
        _environment = environment;
        _logger = logger;
    }

    /// <summary>The stream JSON-RPC reads server→client frames from.</summary>
    public Stream ReceiveStream => _receiveStream;

    /// <summary>The stream JSON-RPC writes client→server frames to.</summary>
    public Stream SendStream => _sendStream
        ?? throw new InvalidOperationException("FfiRuntimeHost has not been started.");

    /// <summary>
    /// Loads the cdylib next to the given CLI entrypoint and prepares the FFI host.
    /// The entrypoint is either the packaged single-file CLI binary (e.g.
    /// <c>runtimes/&lt;rid&gt;/native/copilot</c>) or, for dev, a <c>.js</c> file (e.g.
    /// <c>dist-cli/index.js</c>) launched via <c>node</c>. The cdylib is resolved
    /// relative to the entrypoint directory, preferring the flat, natural
    /// shared-library name the .NET build emits (e.g. <c>libcopilot_runtime.so</c>)
    /// and falling back to the dev tarball layout
    /// <c>prebuilds/&lt;prebuildsFolder&gt;/runtime.node</c>, where
    /// <paramref name="prebuildsFolder"/> is the napi-rs
    /// <c>&lt;node-platform&gt;-&lt;arch&gt;</c> folder name (e.g. <c>win32-x64</c>).
    /// </summary>
    public static FfiRuntimeHost Create(string cliEntrypoint, string prebuildsFolder, IReadOnlyDictionary<string, string>? environment, ILogger logger)
    {
        var fullEntrypoint = Path.GetFullPath(cliEntrypoint);
        var distDir = Path.GetDirectoryName(fullEntrypoint)
            ?? throw new InvalidOperationException($"Could not determine directory for '{cliEntrypoint}'.");

        // Bundled .NET layout: flat, natural shared-library name next to the CLI.
        var flatLibraryPath = Path.Combine(distDir, GetRuntimeLibraryFileName());
        // Dev/tarball layout: dist-cli/prebuilds/<node-platform>-<arch>/runtime.node.
        var prebuildsLibraryPath = Path.Combine(distDir, "prebuilds", prebuildsFolder, "runtime.node");

        var libraryPath = File.Exists(flatLibraryPath) ? flatLibraryPath
            : File.Exists(prebuildsLibraryPath) ? prebuildsLibraryPath
            : throw new InvalidOperationException(
                $"FFI runtime library not found. Looked for '{flatLibraryPath}' and '{prebuildsLibraryPath}'.");

        PrepareNativeLibrary(libraryPath);
        return new FfiRuntimeHost(libraryPath, fullEntrypoint, environment, logger);
    }

    /// <summary>
    /// The natural platform shared-library file name for the runtime cdylib, as
    /// emitted by the .NET build (the .node file renamed to what the Rust cdylib
    /// would be called on this OS).
    /// </summary>
    private static string GetRuntimeLibraryFileName()
    {
        if (OperatingSystem.IsWindows()) return "copilot_runtime.dll";
        if (OperatingSystem.IsMacOS()) return "libcopilot_runtime.dylib";
        return "libcopilot_runtime.so";
    }

    /// <summary>
    /// Starts the in-process runtime: spawns the CLI worker via the Rust host,
    /// waits for readiness, and opens the FFI JSON-RPC connection.
    /// </summary>
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        // host_start blocks until the worker connects back and signals readiness
        // (up to ~30s), and connection_open must run outside any async runtime, so
        // perform the blocking FFI handshake on a background thread.
        await Task.Run(() =>
        {
            var argvJson = BuildArgvJson(_cliEntrypoint);
            var envJson = BuildEnvJson(_environment);

            _serverId = NativeHostStart(argvJson, envJson);
            if (_serverId == 0)
            {
                throw new InvalidOperationException(
                    $"copilot_runtime_host_start failed (library '{_libraryPath}', entrypoint '{_cliEntrypoint}').");
            }

            _connectionId = NativeOpenConnection(_serverId);
            if (_connectionId == 0)
            {
                DisposeNativeCallback();
                NativeHostShutdown(_serverId);
                _serverId = 0;
                throw new InvalidOperationException("copilot_runtime_connection_open failed.");
            }

            _sendStream = new CallbackSendStream(SendFrame);
        }, cancellationToken).ConfigureAwait(false);

        if (_logger.IsEnabled(LogLevel.Debug))
        {
            _logger.LogDebug(
                "FfiRuntimeHost started. Library={Library}, ServerId={ServerId}, ConnectionId={ConnectionId}",
                _libraryPath, _serverId, _connectionId);
        }
    }

    private static byte[] BuildArgvJson(string cliEntrypoint)
    {
        // A .js entrypoint (dev / dist-cli) is launched via node; the packaged
        // single-file CLI binary embeds its own Node and is invoked directly.
        var isJsFile = cliEntrypoint.EndsWith(".js", StringComparison.OrdinalIgnoreCase);
        using var stream = new MemoryStream();
        using (var writer = new Utf8JsonWriter(stream))
        {
            writer.WriteStartArray();
            if (isJsFile)
            {
                writer.WriteStringValue("node");
            }
            writer.WriteStringValue(cliEntrypoint);
            writer.WriteStringValue("--embedded-host");
            // Pin the worker to the bundled pkg matching the loaded cdylib, instead of
            // drifting to a newer version under the user's ~/.copilot/pkg (ABI skew).
            writer.WriteStringValue("--no-auto-update");
            writer.WriteEndArray();
        }
        return stream.ToArray();
    }

    private static byte[]? BuildEnvJson(IReadOnlyDictionary<string, string>? environment)
    {
        if (environment is null || environment.Count == 0)
        {
            return null;
        }
        using var stream = new MemoryStream();
        using (var writer = new Utf8JsonWriter(stream))
        {
            writer.WriteStartObject();
            foreach (var kvp in environment)
            {
                writer.WriteString(kvp.Key, kvp.Value);
            }
            writer.WriteEndObject();
        }
        return stream.ToArray();
    }

    /// <summary>
    /// Writes one framed message to the native connection. The bytes are read
    /// synchronously by the native side (it copies before returning), so the
    /// span does not need to outlive the call — no allocation or copy on our side.
    /// </summary>
    private delegate bool FrameWriter(ReadOnlySpan<byte> frame);

    private bool SendFrame(ReadOnlySpan<byte> frame)
    {
        if (_disposed || _connectionId == 0)
        {
            return false;
        }
        return NativeConnectionWrite(_connectionId, frame);
    }

    private void FeedInbound(IntPtr bytesPtr, UIntPtr bytesLen)
    {
        var length = checked((int)bytesLen.ToUInt64());
        var buffer = new byte[length];
        Marshal.Copy(bytesPtr, buffer, 0, length);
        _receiveStream.Feed(buffer);
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }
        _disposed = true;

        try
        {
            if (_connectionId != 0)
            {
                NativeConnectionClose(_connectionId);
                _connectionId = 0;
            }
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "FfiRuntimeHost: connection_close failed");
        }

        try
        {
            if (_serverId != 0)
            {
                NativeHostShutdown(_serverId);
                _serverId = 0;
            }
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "FfiRuntimeHost: host_shutdown failed");
        }

        _receiveStream.Complete();
        DisposeNativeCallback();
    }

    /// <summary>Length as the native pointer-sized unsigned integer the ABI expects.</summary>
    private static UIntPtr Len(int value) => new((uint)value);

#if NET
    // ---- Modern interop: source-generated LibraryImport P/Invoke (trim/AOT-safe) ----

    private static readonly object ResolverLock = new();
    private static bool s_resolverRegistered;
    private static string? s_resolvedLibraryPath;

    // A normal (non-pinned) handle to this instance, passed to the native side as
    // the callback's user_data so the static outbound callback can route back here.
    private GCHandle _selfHandle;

    /// <summary>
    /// Registers (once) a process-wide <see cref="NativeLibrary.SetDllImportResolver"/>
    /// that maps <see cref="LibraryName"/> to the absolute <c>runtime.node</c> path so the
    /// <see cref="LibraryImportAttribute"/> stubs resolve. The resolved handle is cached by
    /// the runtime after first use, so all in-process hosts share a single loaded library.
    /// </summary>
    private static void PrepareNativeLibrary(string libraryPath)
    {
        lock (ResolverLock)
        {
            if (s_resolvedLibraryPath is not null && s_resolvedLibraryPath != libraryPath)
            {
                throw new InvalidOperationException(
                    $"An in-process FFI runtime library is already loaded from '{s_resolvedLibraryPath}'; "
                    + $"loading a different library from '{libraryPath}' in the same process is not supported.");
            }
            s_resolvedLibraryPath = libraryPath;
            if (!s_resolverRegistered)
            {
                NativeLibrary.SetDllImportResolver(typeof(FfiRuntimeHost).Assembly, Resolve);
                s_resolverRegistered = true;
            }
        }
    }

    private static IntPtr Resolve(string libraryName, Assembly assembly, DllImportSearchPath? searchPath)
    {
        if (libraryName == LibraryName && s_resolvedLibraryPath is not null)
        {
            return NativeLibrary.Load(s_resolvedLibraryPath);
        }
        return IntPtr.Zero;
    }

    private static uint NativeHostStart(byte[] argvJson, byte[]? env) =>
        HostStart(argvJson, Len(argvJson.Length), env, env is null ? UIntPtr.Zero : Len(env.Length));

    private uint NativeOpenConnection(uint serverId)
    {
        _selfHandle = GCHandle.Alloc(this);
        unsafe
        {
            return ConnectionOpen(
                serverId,
                &OnOutboundStatic,
                GCHandle.ToIntPtr(_selfHandle),
                null, UIntPtr.Zero,
                null, UIntPtr.Zero,
                null, UIntPtr.Zero);
        }
    }

    private static bool NativeHostShutdown(uint serverId) => HostShutdown(serverId);

    private static bool NativeConnectionWrite(uint connectionId, ReadOnlySpan<byte> frame) => ConnectionWrite(connectionId, frame, Len(frame.Length));

    private static bool NativeConnectionClose(uint connectionId) => ConnectionClose(connectionId);

    private void DisposeNativeCallback()
    {
        if (_selfHandle.IsAllocated)
        {
            _selfHandle.Free();
        }
    }

    [UnmanagedCallersOnly(CallConvs = new[] { typeof(CallConvCdecl) })]
    private static void OnOutboundStatic(IntPtr userData, IntPtr bytesPtr, nuint bytesLen)
    {
        if (userData == IntPtr.Zero || bytesPtr == IntPtr.Zero || bytesLen == 0)
        {
            return;
        }
        if (GCHandle.FromIntPtr(userData).Target is FfiRuntimeHost self)
        {
            self.FeedInbound(bytesPtr, bytesLen);
        }
    }

    [LibraryImport(LibraryName, EntryPoint = "copilot_runtime_host_start")]
    [UnmanagedCallConv(CallConvs = new[] { typeof(CallConvCdecl) })]
    private static partial uint HostStart(
        byte[] argvJson, nuint argvJsonLen,
        byte[]? env, nuint envLen);

    [LibraryImport(LibraryName, EntryPoint = "copilot_runtime_host_shutdown")]
    [UnmanagedCallConv(CallConvs = new[] { typeof(CallConvCdecl) })]
    [return: MarshalAs(UnmanagedType.U1)]
    private static partial bool HostShutdown(uint serverId);

    [LibraryImport(LibraryName, EntryPoint = "copilot_runtime_connection_open")]
    [UnmanagedCallConv(CallConvs = new[] { typeof(CallConvCdecl) })]
    private static unsafe partial uint ConnectionOpen(
        uint serverId,
        delegate* unmanaged[Cdecl]<IntPtr, IntPtr, nuint, void> onOutbound,
        IntPtr userData,
        byte[]? extSource, nuint extSourceLen,
        byte[]? extName, nuint extNameLen,
        byte[]? connToken, nuint connTokenLen);

    [LibraryImport(LibraryName, EntryPoint = "copilot_runtime_connection_write")]
    [UnmanagedCallConv(CallConvs = new[] { typeof(CallConvCdecl) })]
    [return: MarshalAs(UnmanagedType.U1)]
    private static partial bool ConnectionWrite(uint connectionId, ReadOnlySpan<byte> bytes, nuint bytesLen);

    [LibraryImport(LibraryName, EntryPoint = "copilot_runtime_connection_close")]
    [UnmanagedCallConv(CallConvs = new[] { typeof(CallConvCdecl) })]
    [return: MarshalAs(UnmanagedType.U1)]
    private static partial bool ConnectionClose(uint connectionId);
#else
    // ---- Legacy interop: delegate-based P/Invoke for netstandard2.0 ----
    // netstandard2.0 has neither LibraryImport, NativeLibrary, nor UnmanagedCallersOnly,
    // so the cdylib is loaded through a hand-rolled dlopen/LoadLibrary shim and each
    // export is bound to a [UnmanagedFunctionPointer] delegate. The outbound callback is
    // an instance delegate kept alive in a field for the connection's lifetime.

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate uint HostStartDelegate(
        byte[] argvJson, UIntPtr argvJsonLen,
        byte[]? env, UIntPtr envLen);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    [return: MarshalAs(UnmanagedType.U1)]
    private delegate bool HostShutdownDelegate(uint serverId);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate uint ConnectionOpenDelegate(
        uint serverId,
        OutboundCallbackDelegate onOutbound,
        IntPtr userData,
        byte[]? extSource, UIntPtr extSourceLen,
        byte[]? extName, UIntPtr extNameLen,
        byte[]? connToken, UIntPtr connTokenLen);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    [return: MarshalAs(UnmanagedType.U1)]
    private delegate bool ConnectionWriteDelegate(uint connectionId, IntPtr bytes, UIntPtr bytesLen);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    [return: MarshalAs(UnmanagedType.U1)]
    private delegate bool ConnectionCloseDelegate(uint connectionId);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void OutboundCallbackDelegate(IntPtr userData, IntPtr bytesPtr, UIntPtr bytesLen);

    private static readonly object NativeLock = new();
    private static bool s_loaded;
    private static string? s_loadedPath;
    private static HostStartDelegate? s_hostStart;
    private static HostShutdownDelegate? s_hostShutdown;
    private static ConnectionOpenDelegate? s_connectionOpen;
    private static ConnectionWriteDelegate? s_connectionWrite;
    private static ConnectionCloseDelegate? s_connectionClose;

    // Held for the connection's lifetime so the marshaled function pointer handed to the
    // native side is not collected while Rust may still invoke it.
    private OutboundCallbackDelegate? _outboundDelegate;

    private static void PrepareNativeLibrary(string libraryPath)
    {
        lock (NativeLock)
        {
            if (s_loaded)
            {
                if (s_loadedPath != libraryPath)
                {
                    throw new InvalidOperationException(
                        $"An in-process FFI runtime library is already loaded from '{s_loadedPath}'; "
                        + $"loading a different library from '{libraryPath}' in the same process is not supported.");
                }
                return;
            }

            var handle = NativeLoader.Load(libraryPath);
            if (handle == IntPtr.Zero)
            {
                throw new InvalidOperationException($"Failed to load FFI runtime library '{libraryPath}'.");
            }

            s_hostStart = Bind<HostStartDelegate>(handle, "copilot_runtime_host_start");
            s_hostShutdown = Bind<HostShutdownDelegate>(handle, "copilot_runtime_host_shutdown");
            s_connectionOpen = Bind<ConnectionOpenDelegate>(handle, "copilot_runtime_connection_open");
            s_connectionWrite = Bind<ConnectionWriteDelegate>(handle, "copilot_runtime_connection_write");
            s_connectionClose = Bind<ConnectionCloseDelegate>(handle, "copilot_runtime_connection_close");
            s_loaded = true;
            s_loadedPath = libraryPath;
        }
    }

    private static T Bind<T>(IntPtr handle, string export) where T : Delegate
    {
        var symbol = NativeLoader.GetSymbol(handle, export);
        if (symbol == IntPtr.Zero)
        {
            throw new InvalidOperationException($"FFI runtime library is missing the '{export}' export.");
        }
        return Marshal.GetDelegateForFunctionPointer<T>(symbol);
    }

    private static uint NativeHostStart(byte[] argvJson, byte[]? env) =>
        s_hostStart!(argvJson, Len(argvJson.Length), env, env is null ? UIntPtr.Zero : Len(env.Length));

    private uint NativeOpenConnection(uint serverId)
    {
        _outboundDelegate = OnOutbound;
        return s_connectionOpen!(
            serverId,
            _outboundDelegate,
            IntPtr.Zero,
            null, UIntPtr.Zero,
            null, UIntPtr.Zero,
            null, UIntPtr.Zero);
    }

    private static bool NativeHostShutdown(uint serverId) => s_hostShutdown!(serverId);

    private static unsafe bool NativeConnectionWrite(uint connectionId, ReadOnlySpan<byte> frame)
    {
        fixed (byte* ptr = frame)
        {
            return s_connectionWrite!(connectionId, (IntPtr)ptr, Len(frame.Length));
        }
    }

    private static bool NativeConnectionClose(uint connectionId) => s_connectionClose!(connectionId);

    private void DisposeNativeCallback() => _outboundDelegate = null;

    private void OnOutbound(IntPtr userData, IntPtr bytesPtr, UIntPtr bytesLen)
    {
        if (bytesPtr == IntPtr.Zero || bytesLen == UIntPtr.Zero)
        {
            return;
        }
        FeedInbound(bytesPtr, bytesLen);
    }

    /// <summary>
    /// Minimal cross-platform native library loader for <c>netstandard2.0</c>, which lacks
    /// <c>NativeLibrary</c>. Uses <c>LoadLibrary</c>/<c>GetProcAddress</c> on Windows
    /// and <c>dlopen</c>/<c>dlsym</c> elsewhere (trying <c>libdl.so.2</c> first, then
    /// <c>libdl</c> for older Linux and macOS).
    /// </summary>
    private static class NativeLoader
    {
        public static IntPtr Load(string path) =>
            RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? Windows.LoadLibrary(path) : Unix.Open(path);

        public static IntPtr GetSymbol(IntPtr handle, string name) =>
            RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? Windows.GetProcAddress(handle, name) : Unix.Sym(handle, name);

        private static class Windows
        {
            [DllImport("kernel32", SetLastError = true, CharSet = CharSet.Unicode, BestFitMapping = false, ThrowOnUnmappableChar = true)]
            public static extern IntPtr LoadLibrary([MarshalAs(UnmanagedType.LPWStr)] string path);

            [DllImport("kernel32", SetLastError = true, BestFitMapping = false, ThrowOnUnmappableChar = true)]
            public static extern IntPtr GetProcAddress(IntPtr module, [MarshalAs(UnmanagedType.LPStr)] string name);
        }

        private static class Unix
        {
            private const int RtldNow = 2;

            public static IntPtr Open(string path)
            {
                try { return Libdl2.dlopen(path, RtldNow); }
                catch (DllNotFoundException) { return Libdl1.dlopen(path, RtldNow); }
            }

            public static IntPtr Sym(IntPtr handle, string name)
            {
                try { return Libdl2.dlsym(handle, name); }
                catch (DllNotFoundException) { return Libdl1.dlsym(handle, name); }
            }

            private static class Libdl2
            {
                [DllImport("libdl.so.2", EntryPoint = "dlopen", CharSet = CharSet.Ansi, BestFitMapping = false, ThrowOnUnmappableChar = true)]
                public static extern IntPtr dlopen([MarshalAs(UnmanagedType.LPStr)] string fileName, int flags);

                [DllImport("libdl.so.2", EntryPoint = "dlsym", CharSet = CharSet.Ansi, BestFitMapping = false, ThrowOnUnmappableChar = true)]
                public static extern IntPtr dlsym(IntPtr handle, [MarshalAs(UnmanagedType.LPStr)] string symbol);
            }

            private static class Libdl1
            {
                [DllImport("libdl", EntryPoint = "dlopen", CharSet = CharSet.Ansi, BestFitMapping = false, ThrowOnUnmappableChar = true)]
                public static extern IntPtr dlopen([MarshalAs(UnmanagedType.LPStr)] string fileName, int flags);

                [DllImport("libdl", EntryPoint = "dlsym", CharSet = CharSet.Ansi, BestFitMapping = false, ThrowOnUnmappableChar = true)]
                public static extern IntPtr dlsym(IntPtr handle, [MarshalAs(UnmanagedType.LPStr)] string symbol);
            }
        }
    }
#endif

    /// <summary>
    /// A read-only stream fed by the native outbound callback. Chunks are queued on
    /// an unbounded channel and drained in order by the JSON-RPC read loop.
    /// </summary>
    private sealed class CallbackReceiveStream : Stream
    {
        private readonly Channel<byte[]> _channel = Channel.CreateUnbounded<byte[]>(
            new UnboundedChannelOptions { SingleReader = true, SingleWriter = false });
        private ReadOnlyMemory<byte> _leftover;

        public void Feed(byte[] data) => _channel.Writer.TryWrite(data);

        public void Complete() => _channel.Writer.TryComplete();

#if !NETSTANDARD2_0
        public override async ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken = default)
        {
            return await ReadCoreAsync(buffer, cancellationToken).ConfigureAwait(false);
        }
#endif

        private async ValueTask<int> ReadCoreAsync(Memory<byte> buffer, CancellationToken cancellationToken)
        {
            if (_leftover.IsEmpty)
            {
                while (true)
                {
                    if (!await _channel.Reader.WaitToReadAsync(cancellationToken).ConfigureAwait(false))
                    {
                        return 0; // EOF: channel completed.
                    }
                    if (_channel.Reader.TryRead(out var chunk))
                    {
                        _leftover = chunk;
                        break;
                    }
                    // Data was signalled but lost a race for it; wait again rather
                    // than reporting a spurious EOF.
                }
            }

            var n = Math.Min(buffer.Length, _leftover.Length);
            _leftover.Span.Slice(0, n).CopyTo(buffer.Span);
            _leftover = _leftover.Slice(n);
            return n;
        }

        public override int Read(byte[] buffer, int offset, int count) =>
            ReadCoreAsync(buffer.AsMemory(offset, count), CancellationToken.None).AsTask().GetAwaiter().GetResult();

        public override Task<int> ReadAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            ReadCoreAsync(buffer.AsMemory(offset, count), cancellationToken).AsTask();

        public override bool CanRead => true;
        public override bool CanSeek => false;
        public override bool CanWrite => false;
        public override long Length => throw new NotSupportedException();
        public override long Position { get => throw new NotSupportedException(); set => throw new NotSupportedException(); }
        public override void Flush() { }
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();
        public override void SetLength(long value) => throw new NotSupportedException();
        public override void Write(byte[] buffer, int offset, int count) => throw new NotSupportedException();
    }

    /// <summary>
    /// A write-only stream that forwards each frame to the native
    /// <c>connection_write</c> export.
    /// </summary>
    private sealed class CallbackSendStream(FrameWriter write) : Stream
    {
        private void WriteFrame(ReadOnlySpan<byte> frame)
        {
            if (!write(frame))
            {
                throw new IOException("Failed to write a frame to the in-process runtime connection.");
            }
        }

        public override void Write(byte[] buffer, int offset, int count) => WriteFrame(buffer.AsSpan(offset, count));

#if !NETSTANDARD2_0
        public override void Write(ReadOnlySpan<byte> buffer) => WriteFrame(buffer);

        public override ValueTask WriteAsync(ReadOnlyMemory<byte> buffer, CancellationToken cancellationToken = default)
        {
            WriteFrame(buffer.Span);
            return ValueTask.CompletedTask;
        }
#endif

        public override Task WriteAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken)
        {
            WriteFrame(buffer.AsSpan(offset, count));
            return Task.CompletedTask;
        }

        public override bool CanRead => false;
        public override bool CanSeek => false;
        public override bool CanWrite => true;
        public override long Length => throw new NotSupportedException();
        public override long Position { get => throw new NotSupportedException(); set => throw new NotSupportedException(); }
        public override void Flush() { }
        public override Task FlushAsync(CancellationToken cancellationToken) => Task.CompletedTask;
        public override int Read(byte[] buffer, int offset, int count) => throw new NotSupportedException();
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();
        public override void SetLength(long value) => throw new NotSupportedException();
    }
}
