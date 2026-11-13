/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot;

internal sealed class ShellAuthenticationCancellation : IDisposable
{
    private readonly CancellationTokenSource _source = new();
    private readonly CancellationTokenRegistration _registration;
    private Task? _cancellation;
    private bool _disposed;

    public ShellAuthenticationCancellation(CancellationToken cancellationToken)
    {
        _registration = cancellationToken.Register(static state => ((ShellAuthenticationCancellation)state!).Cancel(), this);
    }

    public CancellationToken Token => _source.Token;

    public bool IsCancellationRequested => _source.IsCancellationRequested;

    public void Cancel()
    {
        lock (_source)
        {
            if (!_disposed)
            {
                // Repeated CancelAsync calls do not join the first call's notifications.
                _cancellation ??= CancelAsync();
            }
        }
    }

    private async Task CancelAsync()
    {
        try
        {
            // Consumer callbacks must not run on the reader or the thread retiring the session.
#if NET8_0_OR_GREATER
            await _source.CancelAsync().ConfigureAwait(false);
#else
            await Task.Run(() => _source.Cancel()).ConfigureAwait(false);
#endif
        }
        catch (AggregateException)
        {
            // Cancellation callbacks are consumer code and must not disrupt the cancel or teardown.
        }
    }

    public void Dispose()
    {
        _registration.Dispose();
        lock (_source)
        {
            if (_disposed)
            {
                return;
            }
            _disposed = true;
            if (_cancellation is null)
            {
                _source.Dispose();
            }
            else
            {
                // Completing the request must neither discard queued callbacks nor wait for them.
                _ = _cancellation.ContinueWith(
                    static (_, state) => ((CancellationTokenSource)state!).Dispose(),
                    _source,
                    CancellationToken.None,
                    TaskContinuationOptions.ExecuteSynchronously,
                    TaskScheduler.Default);
            }
        }
    }
}
