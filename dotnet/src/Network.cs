/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using GitHub.Copilot.Rpc;

namespace GitHub.Copilot;

/// <summary>
/// Network callbacks through which the SDK host takes over parts of the runtime's networking, for example when the
/// runtime runs in WSL or a container and should use VS Code's proxy settings and the host's Kerberos tickets.
/// </summary>
/// <remarks>
/// <para>
/// Each configured callback replaces the runtime's own handling for the traffic in <see cref="Scope"/>; without it,
/// the runtime handles that part itself. The SDK registers the configured callbacks during
/// <see cref="CopilotClient.StartAsync"/>, before sessions can be created, and only when at least one is configured.
/// </para>
/// <para>
/// Exceptions thrown by a callback, including cancellation not requested through the supplied
/// <see cref="CancellationToken"/>, fail the request. Callbacks are started on a thread-pool thread and may block,
/// for example while prompting for credentials. The supplied <see cref="CancellationToken"/> is cancelled when the
/// runtime stops waiting (it cancels the request, for example when its wait times out) or the connection closes, and
/// a callback not yet started by then is not started. The SDK then stops awaiting the callback and ignores any later
/// result.
/// </para>
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public sealed class NetworkConfig
{
    /// <summary>
    /// Gets which traffic the callbacks serve. <see langword="null"/> (the default) or
    /// <see cref="NetworkScope.Connection"/> serves only requests made for sessions this client creates or resumes;
    /// <see cref="NetworkScope.Runtime"/> also serves the runtime's shared background traffic.
    /// </summary>
    /// <remarks>
    /// Only one connection at a time can hold <see cref="NetworkScope.Runtime"/>, so startup fails if another client
    /// holds it. The scope applies to the whole registration, which ends when the connection closes.
    /// </remarks>
    public NetworkScope? Scope { get; init; }

    /// <summary>
    /// Gets the callback that returns the proxies for <see cref="NetworkResolveProxyRequest.Url"/> as a PAC-style list
    /// such as <c>PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT</c> (the format of Electron's
    /// <c>session.resolveProxy</c>); return <c>DIRECT</c> for a direct connection, or <see langword="null"/> to let the
    /// runtime resolve the proxy itself, as if no callback were registered.
    /// </summary>
    /// <remarks>
    /// <see cref="NetworkResolveProxyRequest.Url"/> is the request target as a PAC script sees it: without credentials
    /// or fragment, and for <c>https</c> and <c>wss</c> also without path and query. Entry types the runtime doesn't
    /// support, such as SOCKS and QUIC, are skipped. The
    /// callback decides every request that would otherwise follow ambient proxy settings: proxy environment variables,
    /// <c>NO_PROXY</c> and operating-system proxy settings are not consulted unless it returns <see langword="null"/>,
    /// and a failing callback fails the request rather than connecting directly. Requests to loopback targets, and
    /// requests with an explicitly configured proxy, never consult it. The runtime caches answers, including
    /// <see langword="null"/>, for 5 seconds and waits at most 10 seconds for the callback.
    /// </remarks>
    public Func<NetworkResolveProxyRequest, CancellationToken, Task<string?>>? ResolveProxy { get; init; }

    /// <summary>
    /// Gets the callback that returns a fresh, single-use base64 SPNEGO token without the <c>Negotiate </c> prefix for
    /// a proxy's Negotiate challenge, or <see langword="null"/> when credentials are unavailable.
    /// </summary>
    /// <remarks>
    /// The runtime then does not look up Kerberos credentials itself, so a missing token or a failing callback fails
    /// the request, unless the proxy also offers Basic and <see cref="AuthenticateBasic"/> supplies credentials.
    /// <see cref="NetworkAuthenticateNegotiateRequest.Spn"/> uses the platform-independent
    /// <c>service/host[@REALM]</c> form; validate it before issuing credentials. Tokens must not be logged or persisted.
    /// The runtime waits at most 30 seconds for the callback (less when the request's own deadline is sooner). The
    /// callback supplies the initial SPNEGO token only; exchanges requiring continuation tokens are not supported.
    /// Proxies chosen by <see cref="ResolveProxy"/> are authenticated the same way.
    /// </remarks>
    public Func<NetworkAuthenticateNegotiateRequest, CancellationToken, Task<string?>>? AuthenticateNegotiate { get; init; }

    /// <summary>
    /// Gets the callback that returns credentials for a proxy's Basic challenge, or <see langword="null"/> when
    /// credentials are unavailable, which fails the request with the proxy's challenge.
    /// </summary>
    /// <remarks>
    /// Applies to requests the runtime tunnels through the proxy (<c>https</c> and <c>wss</c>). The runtime remembers
    /// the answer for that proxy and uses it for later connections, until the proxy rejects it; then it calls again
    /// with <see cref="NetworkAuthenticateBasicRequest.PreviousRejected"/> set, so answer with different credentials,
    /// for example by asking the user, or with <see langword="null"/>. A later connection sends the answer without
    /// waiting for a challenge, unless the runtime may try Negotiate with that proxy first. A connection submits at
    /// most three sets of credentials. While a call is pending, other requests through that proxy wait for its
    /// answer. When a proxy offers both Negotiate and Basic, the runtime prefers Negotiate and answers with Basic when
    /// Negotiate is unavailable or the proxy rejects it.
    /// <see cref="NetworkAuthenticateBasicRequest.Realm"/> comes from the proxy, so treat it as untrusted text. An empty
    /// <see cref="NetworkAuthenticateBasicResult.Username"/> also counts as unavailable. The user name must not contain
    /// a colon, and neither the user name nor the password may contain control characters, otherwise the request
    /// fails. An omitted
    /// <see cref="NetworkAuthenticateBasicResult.Password"/> means an empty password. The runtime does not time the
    /// callback out: a request whose own deadline passes stops waiting, but the answer is still remembered.
    /// Credentials must not be logged or persisted.
    /// </remarks>
    public Func<NetworkAuthenticateBasicRequest, CancellationToken, Task<NetworkAuthenticateBasicResult?>>? AuthenticateBasic { get; init; }
}

[Experimental(Diagnostics.Experimental)]
internal sealed class NetworkAdapter : INetworkHandler
{
    private readonly NetworkScope? _scope;
    private readonly Func<NetworkResolveProxyRequest, CancellationToken, Task<string?>>? _resolveProxy;
    private readonly Func<NetworkAuthenticateNegotiateRequest, CancellationToken, Task<string?>>? _authenticateNegotiate;
    private readonly Func<NetworkAuthenticateBasicRequest, CancellationToken, Task<NetworkAuthenticateBasicResult?>>? _authenticateBasic;

    internal NetworkAdapter(NetworkConfig config)
    {
        _scope = config.Scope;
        _resolveProxy = config.ResolveProxy;
        _authenticateNegotiate = config.AuthenticateNegotiate;
        _authenticateBasic = config.AuthenticateBasic;
    }

    /// <summary>Registers the configured callbacks, and the scope when set, with the runtime.</summary>
    internal Task RegisterAsync(ServerNetworkApi network, CancellationToken cancellationToken) =>
        network.SetProviderWithOptionsAsync(
            new NetworkSetProviderRequest
            {
                Scope = _scope,
                ResolveProxy = _resolveProxy is null ? null : new NetworkResolveProxyOptions(),
                AuthenticateNegotiate = _authenticateNegotiate is null ? null : new NetworkAuthenticateNegotiateOptions(),
                AuthenticateBasic = _authenticateBasic is null ? null : new NetworkAuthenticateBasicOptions(),
            },
            cancellationToken);

    public async Task<NetworkResolveProxyResult> ResolveProxyAsync(
        NetworkResolveProxyRequest request,
        CancellationToken cancellationToken = default)
    {
        var callback = _resolveProxy
            ?? throw new InvalidOperationException("No network.resolveProxy callback registered");
        // Leave the JSON-RPC reader for the thread pool, regardless of the current SynchronizationContext or
        // TaskScheduler, before invoking the callback, so the reader can still deliver the runtime's cancellation of
        // this request if the callback blocks.
        await Task.CompletedTask.ConfigureAwait(ConfigureAwaitOptions.ForceYielding);
        var proxy = await HostCallbacks.RunCancellableAsync(
            callback, request, "Proxy resolution callback", cancellationToken).ConfigureAwait(false);
        return new NetworkResolveProxyResult { Proxy = proxy };
    }

    public async Task<NetworkAuthenticateNegotiateResult> AuthenticateNegotiateAsync(
        NetworkAuthenticateNegotiateRequest request,
        CancellationToken cancellationToken = default)
    {
        var callback = _authenticateNegotiate
            ?? throw new InvalidOperationException("No network.authenticateNegotiate callback registered");
        // Leave the JSON-RPC reader for the thread pool, regardless of the current SynchronizationContext or
        // TaskScheduler, before invoking the callback, so the reader can still deliver the runtime's cancellation of
        // this request if the callback blocks.
        await Task.CompletedTask.ConfigureAwait(ConfigureAwaitOptions.ForceYielding);
        var token = await HostCallbacks.RunCancellableAsync(
            callback, request, "Negotiate authentication callback", cancellationToken).ConfigureAwait(false);
        return new NetworkAuthenticateNegotiateResult { Token = token };
    }

    public async Task<NetworkAuthenticateBasicResult> AuthenticateBasicAsync(
        NetworkAuthenticateBasicRequest request,
        CancellationToken cancellationToken = default)
    {
        var callback = _authenticateBasic
            ?? throw new InvalidOperationException("No network.authenticateBasic callback registered");
        // Leave the JSON-RPC reader for the thread pool, regardless of the current SynchronizationContext or
        // TaskScheduler, before invoking the callback, so the reader can still deliver the runtime's cancellation of
        // this request if the callback blocks.
        await Task.CompletedTask.ConfigureAwait(ConfigureAwaitOptions.ForceYielding);
        var credentials = await HostCallbacks.RunCancellableAsync(
            callback, request, "Basic authentication callback", cancellationToken).ConfigureAwait(false);
        return credentials ?? new NetworkAuthenticateBasicResult();
    }
}
