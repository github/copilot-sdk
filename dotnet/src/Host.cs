/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using System.Globalization;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization.Metadata;
using System.Text.RegularExpressions;
using GitHub.Copilot.Rpc;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot;

/// <summary>Reports termination of a connection-owned AHP listener.</summary>
/// <param name="HostId">The listener identity.</param>
/// <param name="Reason">The reason participation ended.</param>
/// <param name="ExitCode">Legacy companion-process exit code, absent for in-process hosting.</param>
/// <param name="Error">An explicit startup or cleanup failure, if any.</param>
[Experimental(Diagnostics.Experimental)]
public sealed record AhpHostExit(string HostId, HostExitReason Reason, long? ExitCode, string? Error);

/// <summary>Explicit host admission policy, independent of inference credentials.</summary>
[Experimental(Diagnostics.Experimental)]
public abstract record AhpHostAuthentication
{
    private AhpHostAuthentication() { }

    /// <summary>Admit the authenticated host's GitHub account.</summary>
    public sealed record GitHub : AhpHostAuthentication;

    /// <summary>Allow anonymous admission without disabling the listener connection token.</summary>
    public sealed record None : AhpHostAuthentication;

    /// <summary>Validate decrypted admission tokens against a stable application owner.</summary>
    /// <param name="RequireIdentity">Every accepted identity must match this nonempty value exactly.</param>
    /// <param name="Authenticate">Receives only the admission token and its cancellable lifetime; return null to reject.</param>
    public sealed record Custom(
        string RequireIdentity,
        Func<string, CancellationToken, Task<AhpAuthenticationResult?>> Authenticate) : AhpHostAuthentication;
}

/// <summary>Application admission identity and required future RFC3339 expiration.</summary>
/// <param name="Identity">Must exactly match the configured owner.</param>
/// <param name="ExpiresAt">Required future RFC3339 timestamp, including timezone.</param>
[Experimental(Diagnostics.Experimental)]
public sealed record AhpAuthenticationResult(string Identity, string ExpiresAt);

/// <summary>Settings selected by the host; preserve Config and add application callbacks.</summary>
/// <param name="Config">Configuration for CreateSessionAsync.</param>
/// <param name="CancellationToken">Cancelled when participation ends, including before the factory returns.</param>
/// <param name="Authentication">Trusted host admission context, never session configuration or inference authentication.</param>
[Experimental(Diagnostics.Experimental)]
public sealed record AhpSessionCreateRequest(SessionConfig Config, HostSessionAuthentication Authentication, CancellationToken CancellationToken);

/// <summary>Resume the requested identity with Config, or return its retained original.</summary>
/// <param name="SessionId">The durable application session identity.</param>
/// <param name="Config">Configuration for ResumeSessionAsync.</param>
/// <param name="CancellationToken">Cancelled when participation ends.</param>
/// <param name="Authentication">Trusted admission context under the current host policy.</param>
[Experimental(Diagnostics.Experimental)]
public sealed record AhpSessionResumeRequest(string SessionId, ResumeSessionConfig Config, HostSessionAuthentication Authentication, CancellationToken CancellationToken);

/// <summary>
/// Transport options and local application callbacks for in-process AHP hosting.
/// Select at least one transport explicitly; no local listener is started by default.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AhpHostOptions
{
    /// <summary>Required in every client mode. Select anonymous admission explicitly with None.</summary>
    public AhpHostAuthentication? Authentication { get; init; }
    /// <summary>Stable durable catalog identity; must agree with GitHubEnvironment.ComputeId when both are supplied.</summary>
    public string? ComputeId { get; init; }
    /// <summary>Local WebSocket transport settings; absent disables the local listener.</summary>
    public HostLocalServerOptions? LocalServer { get; init; }
    /// <summary>GitHub Mission Control transport settings, with required Name and ComputeId; absent disables it.</summary>
    public HostGitHubEnvironmentOptions? GitHubEnvironment { get; init; }
    /// <summary>Remote creation admission, independent of factories. Null selects the denied default.</summary>
    public bool? AllowCreatingNewSessions { get; init; }
    /// <summary>Creates the exact requested session on the owning client.</summary>
    public Func<AhpSessionCreateRequest, Task<CopilotSession>>? CreateSession { get; init; }
    /// <summary>Resumes the exact requested session on the owning client.</summary>
    public Func<AhpSessionResumeRequest, Task<CopilotSession>>? ResumeSession { get; init; }
    /// <summary>
    /// Receives the original object once per handoff, including late factory results.
    /// The SDK never destroys or disconnects that session on the application's behalf.
    /// Callback failures are reported through the client's logger.
    /// </summary>
    public Func<CopilotSession, Task>? OnSessionReleased { get; init; }
    /// <summary>Reports listener exit once. Callback failures are logged.</summary>
    public Func<AhpHostExit, Task>? OnExit { get; init; }
}

/// <summary>An AHP listener bound to its original SDK connection, not a later reconnect.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AhpHost : IAsyncDisposable
{
    private readonly ServerHostApi _rpc;

    internal AhpHost(HostStartResult info, ServerHostApi rpc)
    {
        HostId = info.HostId;
        Url = info.Url;
        Token = info.Token;
        Pid = info.Pid;
        EnvironmentId = info.EnvironmentId;
        _rpc = rpc;
    }

    /// <summary>The listener identity.</summary>
    public string HostId { get; }
    /// <summary>The bound AHP WebSocket address, absent without a local server.</summary>
    public string? Url { get; }
    /// <summary>The connection token, absent without an authenticated local server.</summary>
    public string? Token { get; }
    /// <summary>Legacy companion PID; absent for in-process listeners, not the runtime PID.</summary>
    public long? Pid { get; }
    /// <summary>The GitHub Mission Control environment ID, absent without a GitHub environment.</summary>
    public string? EnvironmentId { get; }

    /// <summary>Durably advertises an attached session without invoking factories or transferring ownership.</summary>
    public Task<HostPublishSessionResult> PublishSessionAsync(string sessionId, CancellationToken cancellationToken = default)
        => _rpc.PublishSessionAsync(HostId, sessionId, cancellationToken);

    /// <summary>
    /// Withdraws a live or dormant application publication and revokes AHP attachments.
    /// Preserves local history, worktrees and ongoing work; returns false when absent.
    /// Rejects nonapplication publications without stopping the host.
    /// </summary>
    public Task<HostUnpublishSessionResult> UnpublishSessionAsync(string sessionId, CancellationToken cancellationToken = default)
        => _rpc.UnpublishSessionAsync(HostId, sessionId, cancellationToken);

    /// <summary>Lists all live and dormant catalog sessions advertised by this host.</summary>
    public Task<HostListSessionsResult> ListSessionsAsync(CancellationToken cancellationToken = default)
        => _rpc.ListSessionsAsync(HostId, cancellationToken);

    /// <summary>Changes subsequent remote admissions without disconnecting clients. Already-admitted creation may finish.</summary>
    public Task SetAllowCreatingNewSessionsAsync(bool allowed, CancellationToken cancellationToken = default)
        => _rpc.SetSessionCreationPolicyAsync(HostId, allowed, cancellationToken);

    /// <summary>Stops and joins listener cleanup without deleting application sessions.</summary>
    public ValueTask DisposeAsync() => new(DisposeAsync(CancellationToken.None));

    /// <summary>Every disposal reaches the runtime, including repeated and concurrent calls.</summary>
    public Task DisposeAsync(CancellationToken cancellationToken) => _rpc.DisposeAsync(HostId, cancellationToken);
}

public sealed partial class CopilotClient
{
    private readonly object _ahpGate = new();
    private readonly Dictionary<string, AhpHostOptions> _ahpHosts = [];
    private readonly Dictionary<string, AhpHandoff> _ahpHandoffs = [];
    private readonly Dictionary<string, AhpAuthenticationAttempt> _ahpAuthentications = [];

    /// <summary>
    /// Starts AHP hosting in the connected runtime, without a companion process.
    /// At least one of LocalServer or GitHubEnvironment must be selected explicitly.
    /// Its lifetime belongs to this connection, not the global runtime.
    /// Cancellation abandons the wait; a listener that subsequently starts is disposed.
    /// </summary>
    [Experimental(Diagnostics.Experimental)]
    public async Task<AhpHost> StartAhpHostAsync(AhpHostOptions options, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(options);
        var authentication = options.Authentication;
        if (authentication is null)
            throw new ArgumentException("An explicit authentication mode is required", nameof(options));
        if (authentication is AhpHostAuthentication.Custom custom &&
            (string.IsNullOrWhiteSpace(custom.RequireIdentity) || custom.Authenticate is null))
            throw new ArgumentException("Custom authentication requires a nonempty RequireIdentity and Authenticate callback", nameof(options));
        if (authentication is not AhpHostAuthentication.GitHub && options.GitHubEnvironment is not null)
            throw new ArgumentException("GitHubEnvironment requires GitHub authentication", nameof(options));
        HostAuthentication wireAuthentication = authentication switch
        {
            AhpHostAuthentication.GitHub => new HostAuthenticationGitHub(),
            AhpHostAuthentication.None => new HostAuthenticationNone(),
            AhpHostAuthentication.Custom policy => new HostAuthenticationCustom { RequireIdentity = policy.RequireIdentity },
            _ => throw new ArgumentException("Invalid authentication mode", nameof(options))
        };
        var localServer = options.LocalServer is { } local ? new HostLocalServerOptions
        {
            Hostname = local.Hostname,
            Port = local.Port,
            Token = local.Token,
            RequireConnectionToken = local.RequireConnectionToken
        } : null;
        var githubEnvironment = options.GitHubEnvironment is { } github ? new HostGitHubEnvironmentOptions
        {
            Name = github.Name,
            ComputeId = github.ComputeId,
            RequireConnectionBinding = github.RequireConnectionBinding
        } : null;
        if (localServer is null && githubEnvironment is null)
            throw new ArgumentException("At least one AHP transport must be configured", nameof(options));
        var connection = await EnsureConnectedAsync(cancellationToken).ConfigureAwait(false);
        cancellationToken.ThrowIfCancellationRequested();
        var hostId = Guid.NewGuid().ToString();
        lock (_ahpGate) _ahpHosts.Add(hostId, options);
        var rpc = connection.Server.Host;
        var startup = rpc.StartAsync(new HostStartRequest
        {
            HostId = hostId,
            Authentication = wireAuthentication,
            ComputeId = options.ComputeId,
            AllowCreatingNewSessions = options.AllowCreatingNewSessions,
            LocalServer = localServer,
            GitHubEnvironment = githubEnvironment,
            SessionFactory = options.CreateSession is not null ? true : null,
            ResumeFactory = options.ResumeSession is not null ? true : null
        }, CancellationToken.None);
        try
        {
            var info = await startup.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken).ConfigureAwait(false);
            return new AhpHost(info, rpc);
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            _ = DisposeAbandonedAhpStartupAsync(startup, rpc, hostId);
            throw;
        }
        catch
        {
            ReleaseAhpHost(hostId);
            throw;
        }
    }

    private async Task DisposeAbandonedAhpStartupAsync(Task<HostStartResult> startup, ServerHostApi rpc, string hostId)
    {
        try
        {
            HostStartResult info;
            try { info = await startup.ConfigureAwait(false); }
            catch { return; }
            await rpc.DisposeAsync(info.HostId, CancellationToken.None).ConfigureAwait(false);
        }
        catch (Exception error) { _logger.LogError(error, "AHP cancelled startup cleanup failed"); }
        finally { ReleaseAhpHost(hostId); }
    }

    private void RegisterAhpHandlers(JsonRpc rpc)
    {
        rpc.SetLocalRpcMethod("host.materializeSession",
            (Func<AhpMaterializeRequest, ValueTask<AhpMaterializeResult>>)(request => new(MaterializeAhpSessionAsync(request))),
            singleObjectParam: true);
        rpc.SetLocalRpcMethod("host.sessionReleased",
            (Action<AhpReleaseRequest>)(request => ReleaseAhpSession(request.HostId, request.HandoffId)), singleObjectParam: true);
        rpc.SetLocalRpcMethod("host.exited", (Action<HostExitedRequest>)(request =>
            HandleAhpExit(new(request.HostId, request.Reason, request.ExitCode, request.Error))), singleObjectParam: true);
        rpc.SetLocalRpcMethod("host.validateAuthentication",
            (Func<AhpValidateAuthenticationRequest, CancellationToken, ValueTask<AhpAuthenticationResult?>>)((request, token) =>
                new(ValidateAhpAuthenticationAsync(request, token, rpc.ConnectionClosedToken))), singleObjectParam: true);
        rpc.SetLocalRpcMethod("host.authenticationCancelled",
            (Action<AhpAuthenticationCancelled>)(request => CancelAhpAuthentication(request.HostId, request.RequestId)),
            singleObjectParam: true);
    }

    private AhpHostOptions? ReleaseAhpHost(string hostId)
    {
        AhpHostOptions? options;
        string[] handoffs;
        string[] authentications;
        lock (_ahpGate)
        {
            _ahpHosts.TryGetValue(hostId, out options);
            _ahpHosts.Remove(hostId);
            handoffs = _ahpHandoffs.Where(pair => pair.Value.HostId == hostId).Select(pair => pair.Key).ToArray();
            authentications = _ahpAuthentications.Where(pair => pair.Value.HostId == hostId).Select(pair => pair.Key).ToArray();
        }
        foreach (var id in authentications) CancelAhpAuthentication(hostId, id);
        foreach (var id in handoffs) ReleaseAhpSession(hostId, id);
        return options;
    }

    private void CancelAhpAuthentication(string hostId, string requestId, AhpAuthenticationAttempt? expected = null)
    {
        AhpAuthenticationAttempt? entry;
        lock (_ahpGate)
        {
            if (!_ahpAuthentications.TryGetValue(requestId, out entry) || entry.HostId != hostId ||
                (expected is not null && !ReferenceEquals(entry, expected))) return;
            _ahpAuthentications.Remove(requestId);
        }
        entry.Cancelled.TrySetResult(true);
        _ = CancelAhpAuthenticationAsync(entry);
    }

    private async Task CancelAhpAuthenticationAsync(AhpAuthenticationAttempt entry)
    {
        try
        {
#if NET8_0_OR_GREATER
            await entry.Cancellation.CancelAsync().ConfigureAwait(false);
#else
            await Task.Run(() => entry.Cancellation.Cancel()).ConfigureAwait(false);
#endif
        }
        catch (AggregateException)
        {
            // Callback exceptions may contain the application credential.
            _logger.LogError("AHP authentication cancellation callback failed");
        }
        finally { entry.CancellationCompleted.TrySetResult(true); }
    }

    private async Task<AhpAuthenticationResult?> ValidateAhpAuthenticationAsync(
        AhpValidateAuthenticationRequest request, CancellationToken requestToken, CancellationToken connectionToken)
    {
        AhpHostAuthentication.Custom policy;
        AhpAuthenticationAttempt entry;
        lock (_ahpGate)
        {
            if (!_ahpHosts.TryGetValue(request.HostId, out var options) ||
                options.Authentication is not AhpHostAuthentication.Custom configured ||
                _ahpAuthentications.ContainsKey(request.RequestId))
                throw new InvalidOperationException("AHP authenticator is unavailable or request already exists");
            policy = configured;
            entry = new(request.HostId);
            _ahpAuthentications.Add(request.RequestId, entry);
        }
        using var requestRegistration = requestToken.Register(() => CancelAhpAuthentication(request.HostId, request.RequestId, entry));
        using var connectionRegistration = connectionToken.Register(() => CancelAhpAuthentication(request.HostId, request.RequestId, entry));
        var validation = Task.Run(() => policy.Authenticate(request.Token, entry.Cancellation.Token));
        try
        {
            await Task.WhenAny(validation, entry.Cancelled.Task).ConfigureAwait(false);
            lock (_ahpGate)
            {
                if (!_ahpAuthentications.TryGetValue(request.RequestId, out var current) || !ReferenceEquals(current, entry) ||
                    !_ahpHosts.TryGetValue(request.HostId, out var host) || !ReferenceEquals(host.Authentication, policy))
                    throw new InvalidOperationException("AHP authentication ended");
            }
            var result = await validation.ConfigureAwait(false);
            if (result is not null &&
                (result.Identity != policy.RequireIdentity ||
                 result.ExpiresAt is null ||
                 !Regex.IsMatch(result.ExpiresAt, @"^\d{4}-\d{2}-\d{2}[Tt]\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:[Zz]|[+-]\d{2}:\d{2})\z", RegexOptions.CultureInvariant, TimeSpan.FromSeconds(1)) ||
                 !DateTimeOffset.TryParse(result.ExpiresAt, CultureInfo.InvariantCulture, DateTimeStyles.None, out var expiresAt) ||
                 expiresAt <= DateTimeOffset.UtcNow))
                throw new InvalidOperationException("Invalid AHP authentication result");
            lock (_ahpGate)
            {
                if (!_ahpAuthentications.TryGetValue(request.RequestId, out var current) || !ReferenceEquals(current, entry) ||
                    !_ahpHosts.ContainsKey(request.HostId) || requestToken.IsCancellationRequested || connectionToken.IsCancellationRequested)
                    throw new InvalidOperationException("AHP authentication ended");
                return result;
            }
        }
        catch
        {
            throw new InvalidOperationException("AHP authentication rejected or ended");
        }
        finally
        {
            CancelAhpAuthentication(request.HostId, request.RequestId, entry);
            _ = DisposeAhpAuthenticationAsync(entry, validation);
        }
    }

    private async Task DisposeAhpAuthenticationAsync(AhpAuthenticationAttempt entry, Task validation)
    {
        try { await validation.ConfigureAwait(false); }
        catch (OperationCanceledException)
        {
            // Cancellation is expected during authentication teardown.
        }
        catch (Exception)
        {
            // Observe late callback failures without exposing application credentials.
            _logger.LogDebug("AHP authentication validator failed during cleanup");
        }
        await entry.CancellationCompleted.Task.ConfigureAwait(false);
        entry.Cancellation.Dispose();
    }

    private void HandleAhpExit(AhpHostExit report)
    {
        var options = ReleaseAhpHost(report.HostId);
        if (options?.OnExit is { } callback)
            _ = NotifyAhpAsync(() => callback(report), "exit");
    }

    private void DisconnectAhpHosts()
    {
        string[] ids;
        lock (_ahpGate) ids = _ahpHosts.Keys.ToArray();
        foreach (var id in ids)
            HandleAhpExit(new(id, HostExitReason.OwnerDisconnected, null,
                "Owner connection closed; runtime cleanup cannot be acknowledged on this connection."));
    }

    private void ReleaseAhpSession(string hostId, string handoffId)
    {
        AhpHandoff entry;
        CopilotSession? session;
        lock (_ahpGate)
        {
            if (!_ahpHandoffs.TryGetValue(handoffId, out entry!) || entry.HostId != hostId) return;
            _ahpHandoffs.Remove(handoffId);
            entry.Released = true;
            session = entry.Session;
            entry.Session = null;
            entry.Configs = null;
            entry.Cancelled.TrySetResult(true);
        }
        _ = CancelAhpSessionAsync(entry);
        NotifyAhpReleased(entry, session);
        _ = DisposeAhpCancellationAsync(entry);
    }

    private async Task CancelAhpSessionAsync(AhpHandoff entry)
    {
        try
        {
#if NET8_0_OR_GREATER
            await entry.Cancellation.CancelAsync().ConfigureAwait(false);
#else
            await Task.Run(() => entry.Cancellation.Cancel()).ConfigureAwait(false);
#endif
        }
        catch (AggregateException error) { _logger.LogError(error, "AHP cancellation callback failed"); }
        finally { entry.CancellationCompleted.TrySetResult(true); }
    }

    private static async Task DisposeAhpCancellationAsync(AhpHandoff entry)
    {
        await entry.FactoryCompleted.Task.ConfigureAwait(false);
        await entry.CancellationCompleted.Task.ConfigureAwait(false);
        entry.Cancellation.Dispose();
    }

    private void NotifyAhpReleased(AhpHandoff entry, CopilotSession? session)
    {
        if (session is not null && entry.Options.OnSessionReleased is { } callback)
            _ = NotifyAhpAsync(async () =>
            {
                await entry.CancellationCompleted.Task.ConfigureAwait(false);
                await callback(session).ConfigureAwait(false);
            }, "session release");
    }

    private async Task NotifyAhpAsync(Func<Task> callback, string label)
    {
        try { await Task.Run(callback).ConfigureAwait(false); }
        catch (Exception error)
        {
            if (_logger.IsEnabled(LogLevel.Error))
                _logger.LogError(error, "AHP {Callback} callback failed", label);
        }
    }

    private void CaptureAhpSession<T>(CopilotSession session, T request, JsonTypeInfo<T> typeInfo)
    {
        AhpHandoff[] entries;
        lock (_ahpGate)
            entries = _ahpHandoffs.Values.Where(entry => entry.SessionId == session.SessionId && entry.Configs is not null).ToArray();
        if (entries.Length == 0) return;
        var snapshot = JsonSerializer.SerializeToElement(request, typeInfo);
        lock (_ahpGate)
            foreach (var entry in entries)
                if (!entry.Released && entry.Configs is { } configs) configs[session] = snapshot;
    }

    private static bool AhpTrustedAuthenticationMatches(AhpHostAuthentication? policy, HostSessionAuthentication trusted) =>
        (policy, trusted) switch
        {
            (AhpHostAuthentication.GitHub, HostSessionAuthenticationGitHub github) => !string.IsNullOrWhiteSpace(github.Identity),
            (AhpHostAuthentication.Custom custom, HostSessionAuthenticationCustom owner) => owner.Identity == custom.RequireIdentity,
            (AhpHostAuthentication.None, HostSessionAuthenticationNone) => true,
            _ => false
        };

    private async Task<AhpMaterializeResult> MaterializeAhpSessionAsync(AhpMaterializeRequest request)
    {
        ArgumentNullException.ThrowIfNull(request.Authentication);
        var sessionId = request.Config.GetProperty("sessionId").GetString();
        if (sessionId is null || sessionId.Length == 0) throw new ArgumentException("AHP handoff requires sessionId");
        AhpHandoff entry;
        lock (_ahpGate)
        {
            if (!_ahpHosts.TryGetValue(request.HostId, out var options) || _ahpHandoffs.ContainsKey(request.HandoffId) ||
                (request.Resume ? options.ResumeSession is null : options.CreateSession is null))
                throw new InvalidOperationException("AHP session factory is unavailable or handoff already exists");
            if (!AhpTrustedAuthenticationMatches(options.Authentication, request.Authentication))
                throw new InvalidOperationException("AHP trusted authentication does not match host policy");
            entry = new(request.HostId, sessionId, options);
            _ahpHandoffs.Add(request.HandoffId, entry);
        }
        var cancellationToken = entry.Token;
        var materialized = Task.Run(async () =>
        {
            try
            {
                var config = JsonNode.Parse(request.Config.GetRawText())!.AsObject();
                if (config.TryGetPropertyValue("configDir", out var directory))
                {
                    config.Remove("configDir");
                    config["configDirectory"] = directory;
                }
                CopilotSession session;
                if (request.Resume)
                {
                    config.Remove("sessionId");
                    var options = config.Deserialize(ClientJsonContext.Default.ResumeSessionConfig)
                        ?? throw new InvalidOperationException("Invalid AHP resume configuration");
                    session = await entry.Options.ResumeSession!(new(sessionId, options, request.Authentication, entry.Token)).ConfigureAwait(false);
                }
                else
                {
                    var options = config.Deserialize(ClientJsonContext.Default.SessionConfig)
                        ?? throw new InvalidOperationException("Invalid AHP create configuration");
                    session = await entry.Options.CreateSession!(new(options, request.Authentication, entry.Token)).ConfigureAwait(false);
                }
                bool released;
                lock (_ahpGate)
                {
                    released = entry.Released;
                    entry.Session = released ? null : session;
                }
                if (released)
                {
                    NotifyAhpReleased(entry, session);
                    throw new AhpHandoffEndedException();
                }
                if (session is null || session.SessionId != sessionId || !ReferenceEquals(GetSession(sessionId), session))
                    throw new InvalidOperationException("AHP callback must return the requested session from this client");
                lock (_ahpGate)
                {
                    var actual = default(JsonElement);
                    var captured = entry.Configs?.TryGetValue(session, out actual) == true;
                    entry.Configs = null;
                    if ((!request.Resume || captured) && !ContainsAhpConfig(actual, request.Config, root: true))
                        throw new InvalidOperationException("AHP callback must preserve the supplied session configuration");
                }
                return new AhpMaterializeResult(sessionId);
            }
            finally { entry.FactoryCompleted.TrySetResult(true); }
        });
        _ = ObserveAhpFactoryAsync(materialized, cancellationToken);
        await Task.WhenAny(materialized, entry.Cancelled.Task).ConfigureAwait(false);
        if (materialized.IsCompleted)
        {
            try { return await materialized.ConfigureAwait(false); }
            catch
            {
                ReleaseAhpSession(request.HostId, request.HandoffId);
                throw;
            }
        }
        throw new AhpHandoffEndedException();
    }

    private async Task ObserveAhpFactoryAsync(Task task, CancellationToken cancellationToken)
    {
        try { await task.ConfigureAwait(false); }
        catch (AhpHandoffEndedException) { }
        catch (OperationCanceledException error) when (cancellationToken.IsCancellationRequested &&
            error.CancellationToken == cancellationToken)
        { }
        catch (Exception error) { _logger.LogError(error, "AHP session factory failed"); }
    }

    private static bool ContainsAhpConfig(JsonElement actual, JsonElement expected, bool root = false)
    {
        if (expected.ValueKind != JsonValueKind.Object) return JsonElement.DeepEquals(actual, expected);
        if (actual.ValueKind != JsonValueKind.Object) return false;
        foreach (var property in expected.EnumerateObject())
        {
            var name = root ? property.Name switch
            {
                "suppressResumeEvent" => "disableResume",
                "enableExperimentalMode" => "isExperimentalMode",
                "enableMcpApps" => "requestMcpApps",
                _ => property.Name
            } : property.Name;
            if (!actual.TryGetProperty(name, out var value))
            {
                // These default-false flags are omitted by the session request builder.
                if (root && property.Value.ValueKind == JsonValueKind.False &&
                    name is "streaming" or "requestMcpApps" or "disableResume") continue;
                return false;
            }
            if (!ContainsAhpConfig(value, property.Value)) return false;
        }
        return true;
    }

    internal sealed record AhpMaterializeRequest(string HostId, string HandoffId, JsonElement Config, bool Resume, HostSessionAuthentication Authentication);
    internal sealed record AhpValidateAuthenticationRequest(string HostId, string RequestId, string Token);
    internal sealed record AhpAuthenticationCancelled(string HostId, string RequestId);
    internal sealed record AhpMaterializeResult(string SessionId);
    internal sealed record AhpReleaseRequest(string HostId, string HandoffId);
    private sealed class AhpHandoffEndedException() : InvalidOperationException("AHP session handoff ended");

    private sealed class AhpAuthenticationAttempt(string hostId)
    {
        public string HostId { get; } = hostId;
        public CancellationTokenSource Cancellation { get; } = new();
        public TaskCompletionSource<bool> Cancelled { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public TaskCompletionSource<bool> CancellationCompleted { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
    }

    private sealed class AhpHandoff(string hostId, string sessionId, AhpHostOptions options)
    {
        public string HostId { get; } = hostId;
        public string SessionId { get; } = sessionId;
        public AhpHostOptions Options { get; } = options;
        public CancellationTokenSource Cancellation { get; } = new();
        public CancellationToken Token => Cancellation.Token;
        public TaskCompletionSource<bool> Cancelled { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public TaskCompletionSource<bool> CancellationCompleted { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public TaskCompletionSource<bool> FactoryCompleted { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public CopilotSession? Session { get; set; }
        public Dictionary<CopilotSession, JsonElement>? Configs { get; set; } = [];
        public bool Released { get; set; }
    }
}
