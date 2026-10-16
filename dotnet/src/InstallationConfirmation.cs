/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using RpcInstallationDecision = GitHub.Copilot.Rpc.InstallationDecision;
using RpcInstallationsConfirmRequest = GitHub.Copilot.Rpc.InstallationsConfirmRequest;
using RpcInstallationsConfirmResult = GitHub.Copilot.Rpc.InstallationsConfirmResult;
using RpcInstallationsHandler = GitHub.Copilot.Rpc.IInstallationsHandler;

namespace GitHub.Copilot;

/// <summary>
/// The cancellable lifetime of one installation confirmation.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class InstallationConfirmationContext
{
    internal InstallationConfirmationContext(CancellationToken cancellationToken)
    {
        CancellationToken = cancellationToken;
    }

    /// <summary>
    /// Gets a token cancelled when the runtime retires this request or the original connection closes.
    /// </summary>
    public CancellationToken CancellationToken { get; }
}

/// <summary>
/// Collects a fresh human decision for the complete installation review on its original connection.
/// </summary>
/// <param name="request">The generated installation confirmation request.</param>
/// <param name="context">The confirmation context, including a token cancelled by request cancellation or connection closure.</param>
/// <returns>An explicit confirm, decline or cancel decision.</returns>
[Experimental(Diagnostics.Experimental)]
public delegate ValueTask<RpcInstallationDecision> InstallationConfirmationHandler(
    RpcInstallationsConfirmRequest request,
    InstallationConfirmationContext context);

[Experimental(Diagnostics.Experimental)]
internal sealed class InstallationConfirmationAdapter(InstallationConfirmationHandler handler) : RpcInstallationsHandler
{
    private readonly InstallationConfirmationHandler _handler =
        handler ?? throw new ArgumentNullException(nameof(handler));

    private CancellationToken _connectionClosed;

    internal void Attach(JsonRpc rpc)
    {
        ArgumentNullException.ThrowIfNull(rpc);
        _connectionClosed = rpc.ConnectionClosedToken;
    }

    public async Task<RpcInstallationsConfirmResult> ConfirmAsync(
        RpcInstallationsConfirmRequest request,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);

        ThrowIfRequestCancelled(cancellationToken);
        ThrowIfConnectionClosed(_connectionClosed);

        var confirmationId = request.ConfirmationId;
        var reviewFingerprint = request.ReviewFingerprint;
        var combinedCancellation = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, _connectionClosed);
        Task<RpcInstallationDecision>? handlerTask = null;

        try
        {
            var context = new InstallationConfirmationContext(combinedCancellation.Token);
            var cancellationSignal = CreateCancellationSignal(combinedCancellation.Token);

            using var cancellationRegistration = RegisterCancellationSignal(cancellationSignal, combinedCancellation.Token);

            handlerTask = InvokeHandlerAsync(request, context);
            var completed = await Task.WhenAny(
                handlerTask,
                cancellationSignal.Task).ConfigureAwait(false);

            ThrowIfRequestCancelled(cancellationToken);
            ThrowIfConnectionClosed(_connectionClosed);

            if (ReferenceEquals(completed, cancellationSignal.Task))
            {
                throw new InvalidOperationException("Installation confirmation cancelled.");
            }

            var decision = await handlerTask.ConfigureAwait(false);
            ThrowIfRequestCancelled(cancellationToken);
            ThrowIfConnectionClosed(_connectionClosed);

            return new RpcInstallationsConfirmResult
            {
                ConfirmationId = confirmationId,
                ReviewFingerprint = reviewFingerprint,
                Decision = IsValidDecision(decision)
                    ? decision
                    : throw new InvalidOperationException("Invalid installation confirmation decision."),
            };
        }
        finally
        {
            DisposeCancellationWhenHandlerCompletes(combinedCancellation, handlerTask);
        }
    }

    private async Task<RpcInstallationDecision> InvokeHandlerAsync(
        RpcInstallationsConfirmRequest request,
        InstallationConfirmationContext context)
    {
        return await _handler(request, context).ConfigureAwait(false);
    }

    private static bool IsValidDecision(RpcInstallationDecision decision)
    {
        return decision == RpcInstallationDecision.Confirm ||
            decision == RpcInstallationDecision.Decline ||
            decision == RpcInstallationDecision.Cancel;
    }

    private static TaskCompletionSource CreateCancellationSignal(CancellationToken cancellationToken)
    {
        var signal = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        if (cancellationToken.IsCancellationRequested)
        {
            signal.TrySetResult();
        }

        return signal;
    }

    private static CancellationTokenRegistration RegisterCancellationSignal(
        TaskCompletionSource signal,
        CancellationToken cancellationToken)
    {
        return cancellationToken.CanBeCanceled
            ? cancellationToken.Register(static state => ((TaskCompletionSource)state!).TrySetResult(), signal)
            : default;
    }

    private static void ThrowIfRequestCancelled(CancellationToken cancellationToken)
    {
        if (cancellationToken.IsCancellationRequested)
        {
            throw new LocalRpcInvocationException(
                -32800,
                "Installation confirmation request cancelled");
        }
    }

    private static void ThrowIfConnectionClosed(CancellationToken connectionClosed)
    {
        if (connectionClosed.IsCancellationRequested)
        {
            throw new InvalidOperationException("Installation confirmation connection closed.");
        }
    }

    private static void DisposeCancellationWhenHandlerCompletes(
        CancellationTokenSource cancellation,
        Task<RpcInstallationDecision>? handlerTask)
    {
        if (handlerTask is not null && !handlerTask.IsCompleted)
        {
            _ = handlerTask.ContinueWith(
                static (task, state) =>
                {
                    _ = task.Exception;
                    ((CancellationTokenSource)state!).Dispose();
                },
                cancellation,
                CancellationToken.None,
                TaskContinuationOptions.ExecuteSynchronously,
                TaskScheduler.Default);
            return;
        }

        cancellation.Dispose();
    }
}
