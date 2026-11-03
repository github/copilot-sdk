/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class TestHelperTests
{
    [Fact]
    public async Task ExchangeObservationChecksTheFinalSnapshotAfterJoiningItsCanceledProbe()
    {
        List<ParsedHttpExchange> expected =
        [
            new(new ChatCompletionRequest("first-model", [], null), null, null),
            new(new ChatCompletionRequest("second-model", [], null), null, null),
        ];
        var calls = 0;
        var active = 0;
        var canceled = false;
        var result = await TestHelper.WaitForExchangesAsync(async token =>
        {
            if (++calls == 1)
            {
                Assert.True(token.CanBeCanceled);
                active++;
                try
                {
                    await Task.Delay(Timeout.Infinite, token);
                    throw new InvalidOperationException("held probe must be canceled");
                }
                catch (OperationCanceledException) when (token.IsCancellationRequested)
                {
                    canceled = true;
                    throw;
                }
                finally
                {
                    active--;
                }
            }
            Assert.True(canceled);
            Assert.Equal(0, active);
            Assert.False(token.CanBeCanceled);
            return expected;
        }, minimumCount: 2, timeout: TimeSpan.Zero);

        Assert.Same(expected, result);
        Assert.Equal(2, calls);
        Assert.Equal(0, active);
    }

    [Fact]
    public async Task ExchangeObservationRetainsTheExactMinimumCountOnItsFinalSnapshot()
    {
        var calls = 0;
        var error = await Assert.ThrowsAsync<TimeoutException>(() => TestHelper.WaitForExchangesAsync(async token =>
        {
            if (++calls == 1)
            {
                await Task.Delay(Timeout.Infinite, token);
            }
            return [new(new ChatCompletionRequest("only-one-model", [], null), null, null)];
        }, minimumCount: 2, timeout: TimeSpan.Zero));

        Assert.Equal("Timed out waiting for 2 chat completion request(s)", error.Message);
        Assert.IsType<TimeoutException>(error.InnerException);
        Assert.Equal(2, calls);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task ExchangeObservationPropagatesAnImmediateProbeFailureWithoutAnotherRequest(bool timeoutFailure)
    {
        Exception original = timeoutFailure ? new TimeoutException("probe failed") : new IOException("probe failed");
        var calls = 0;
        var error = await Assert.ThrowsAnyAsync<Exception>(() => TestHelper.WaitForExchangesAsync(_ =>
        {
            calls++;
            return Task.FromException<List<ParsedHttpExchange>>(original);
        }, timeout: TimeSpan.Zero));

        Assert.Same(original, error);
        Assert.Equal(1, calls);
    }

    [Fact]
    public async Task ExchangeObservationPropagatesAFailedFinalSnapshot()
    {
        var original = new IOException("final snapshot failed");
        var calls = 0;
        var error = await Assert.ThrowsAsync<IOException>(() => TestHelper.WaitForExchangesAsync(async token =>
        {
            if (++calls == 1)
            {
                await Task.Delay(Timeout.Infinite, token);
            }
            throw original;
        }, timeout: TimeSpan.Zero));

        Assert.Same(original, error);
        Assert.Equal(2, calls);
    }

    [Fact]
    public void ExtensionLaunchMarkers_Redacts_Stderr_While_Reporting_Error_Category()
    {
        var home = Path.Join(Path.GetTempPath(), $"extension-log-test-{Guid.NewGuid():N}");
        var logs = Path.Join(home, "logs");
        Directory.CreateDirectory(logs);
        try
        {
            File.WriteAllLines(Path.Join(logs, "process-1-42.log"),
            [
                "=== launch pid=42 module=/tmp/extensions/failed-extension/extension.mjs ===",
                "=== initializing ===",
                "=== ready ===",
                "ignored old stderr",
                .. Enumerable.Range(0, 7).Select(index => $"context {index}"),
                "Error: extension startup failed",
                "=== exit code=1 disposition=crash ===",
            ]);

            var summary = TestHelper.ExtensionLaunchMarkers(home, "user:failed-extension");
            Assert.Contains("=== ready ===", summary);
            Assert.Contains("=== exit code=1 disposition=crash ===", summary);
            Assert.Contains("error categories: Error", summary);
            Assert.DoesNotContain("Error: extension startup failed", summary);
            Assert.DoesNotContain("ignored old stderr", summary);
        }
        finally
        {
            Directory.Delete(home, recursive: true);
        }
    }
}
