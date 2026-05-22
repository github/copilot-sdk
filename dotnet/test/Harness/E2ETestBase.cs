/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.Logging;
using System.Data;
using System.Reflection;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test;

public abstract class E2ETestBase : IClassFixture<E2ETestFixture>, IAsyncLifetime
{
    private readonly E2ETestFixture _fixture;
    private readonly string _snapshotCategory;
    private readonly string _testName;

    protected E2ETestContext Ctx => _fixture.Ctx;
    protected CopilotClient Client => _fixture.Client;

    protected E2ETestBase(E2ETestFixture fixture, string snapshotCategory, ITestOutputHelper output)
    {
        _fixture = fixture;
        _snapshotCategory = snapshotCategory;
        _testName = GetTestName(output);
        Logger = new XunitLogger(output);

        // Wire logger into the shared context so all clients created via Ctx.CreateClient get it.
        Ctx.Logger = Logger;
    }

    /// <summary>Logger that forwards warnings and above to xunit test output.</summary>
    protected ILogger Logger { get; }

    /// <summary>Bridges <see cref="ILogger"/> to xunit's <see cref="ITestOutputHelper"/>.</summary>
    private sealed class XunitLogger(ITestOutputHelper output) : ILogger
    {
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;
        public bool IsEnabled(LogLevel logLevel) => logLevel >= LogLevel.Warning;
        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter)
        {
            if (!IsEnabled(logLevel)) return;
            try { output.WriteLine($"[{logLevel}] {formatter(state, exception)}"); }
            catch (InvalidOperationException) { /* test already finished */ }
        }
    }

    internal static string GetTestName(ITestOutputHelper output)
    {
        // xUnit doesn't provide a public API to get the current test name.
        var type = output.GetType();
        var testField = type.GetField("test", BindingFlags.Instance | BindingFlags.NonPublic);
        var test = (ITest?)testField?.GetValue(output);
        return test?.TestCase.TestMethod.Method.Name ?? throw new InvalidOperationException("Couldn't find test name");
    }

    public async Task InitializeAsync()
    {
        await Ctx.CleanupAfterTestAsync();
        await Ctx.ConfigureForTestAsync(_snapshotCategory, _testName);
    }

    public Task DisposeAsync()
    {
        return Ctx.CleanupAfterTestAsync();
    }

    /// <summary>
    /// Creates a session with a default config that approves all permissions.
    /// Convenience wrapper for E2E tests.
    /// </summary>
    protected Task<CopilotSession> CreateSessionAsync(SessionConfig? config = null)
    {
        config ??= new SessionConfig();
        config.OnPermissionRequest ??= PermissionHandler.ApproveAll;
        return Client.CreateSessionAsync(config);
    }

    /// <summary>
    /// Resumes a session with a default config that approves all permissions.
    /// Convenience wrapper for E2E tests.
    /// </summary>
    protected async Task<CopilotSession> ResumeSessionAsync(string sessionId, ResumeSessionConfig? config = null)
    {
        config ??= new ResumeSessionConfig();
        config.OnPermissionRequest ??= PermissionHandler.ApproveAll;

        await Client.StartAsync();
        var port = Client.RuntimePort
            ?? throw new InvalidOperationException("The shared E2E client must use TCP transport to support multi-client resume.");

        var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri($"localhost:{port}", connectionToken: E2ETestFixture.SharedTcpConnectionToken),
        });
        return await client.ResumeSessionAsync(sessionId, config);
    }

    protected static string GetSystemMessage(ParsedHttpExchange exchange)
    {
        return exchange.Request.Messages.FirstOrDefault(m => m.Role == "system")?.StringContent ?? string.Empty;
    }

    protected static List<string> GetToolNames(ParsedHttpExchange exchange)
    {
        return exchange.Request.Tools?.Select(t => t.Function.Name).ToList() ?? [];
    }

    protected async Task<List<ParsedHttpExchange>> WaitForExchangesAsync(int minimumCount = 1)
    {
        List<ParsedHttpExchange> exchanges = [];
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                exchanges = await Ctx.GetExchangesAsync();
                return exchanges.Count >= minimumCount;
            },
            timeoutMessage: $"Timed out waiting for {minimumCount} chat completion request(s)");
        return exchanges;
    }

    protected async Task<List<ParsedHttpExchange>> SendAndWaitForExchangesAsync(
        CopilotSession session,
        MessageOptions options,
        int minimumCount = 1)
    {
        using var cts = new CancellationTokenSource();
        var sendTask = session.SendAndWaitAsync(options, TimeSpan.FromMinutes(3), cts.Token);
        var exchangesTask = WaitForExchangesAsync(minimumCount);

        try
        {
            var completedTask = await Task.WhenAny(exchangesTask, sendTask);
            if (completedTask == sendTask)
            {
                await sendTask;
            }

            return await exchangesTask;
        }
        finally
        {
            if (!sendTask.IsCompleted)
            {
                cts.Cancel();
                try
                {
                    await sendTask;
                }
                catch (OperationCanceledException) when (cts.IsCancellationRequested)
                {
                }
            }
        }
    }

    protected static Dictionary<string, McpServerConfig> CreateTestMcpServers(params string[] serverNames)
    {
        var testHarnessDir = FindTestHarnessDir();
        return serverNames.ToDictionary(
            name => name,
            _ => (McpServerConfig)new McpStdioServerConfig
            {
                Command = "node",
                Args = [Path.Combine(testHarnessDir, "test-mcp-server.mjs")],
                WorkingDirectory = testHarnessDir,
                Tools = ["*"]
            });
    }

    protected static string FindTestHarnessDir()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            var candidate = Path.Combine(dir.FullName, "test", "harness", "test-mcp-server.mjs");
            if (File.Exists(candidate))
                return Path.GetDirectoryName(candidate)!;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("Could not find test/harness/test-mcp-server.mjs");
    }

    protected static async Task WaitForMcpServerStatusAsync(
        CopilotSession session,
        string serverName,
        McpServerStatus expectedStatus)
    {
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var result = await session.Rpc.Mcp.ListAsync();
                return result.Servers.Any(server =>
                    string.Equals(server.Name, serverName, StringComparison.Ordinal)
                    && server.Status == expectedStatus);
            },
            timeout: TimeSpan.FromSeconds(60),
            pollInterval: TimeSpan.FromMilliseconds(200),
            timeoutMessage: $"{serverName} reaching {expectedStatus}");
    }
}
