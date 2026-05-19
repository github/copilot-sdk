/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class SessionFsSqliteE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session_fs_sqlite", output)
{
    private static readonly SessionFsConfig SessionFsConfig = new()
    {
        InitialCwd = "/",
        SessionStatePath = "/session-state",
        Conventions = SessionFsSetProviderConventions.Posix,
        Capabilities = new SessionFsSetProviderCapabilities { Sqlite = true },
    };

    private readonly List<SqliteCall> _sqliteCalls = [];

    [Fact]
    public async Task Should_Route_Sql_Queries_Through_The_Sessionfs_Sqlite_Handler()
    {
        await using var client = CreateSessionFsClient();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s => new InMemorySessionFsSqliteHandler(s.SessionId, _sqliteCalls),
        });

        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt =
                "Use the sql tool to create a table called \"items\" with columns id (TEXT PRIMARY KEY) and name (TEXT). " +
                "Then insert a row with id \"a1\" and name \"Widget\". " +
                "Then select all rows from items and tell me what you find.",
        });

        Assert.Contains("Widget", msg?.Data.Content ?? string.Empty);

        var sessionCalls = _sqliteCalls.Where(c => c.SessionId == session.SessionId).ToList();
        Assert.NotEmpty(sessionCalls);
        Assert.Contains(sessionCalls, c => c.Query.Contains("CREATE TABLE", StringComparison.OrdinalIgnoreCase));
        Assert.Contains(sessionCalls, c => c.Query.Contains("INSERT", StringComparison.OrdinalIgnoreCase));
        Assert.Contains(sessionCalls, c => c.Query.Contains("SELECT", StringComparison.OrdinalIgnoreCase));

        Assert.Contains(sessionCalls, c => c.QueryType == "exec");
        Assert.Contains(sessionCalls, c => c.QueryType == "query");
        Assert.Contains(sessionCalls, c => c.QueryType == "run");

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Allow_Subagents_To_Use_Sql_Tool_Via_Inherited_Sessionfs()
    {
        await using var client = CreateSessionFsClient();

        var handler = (InMemorySessionFsSqliteHandler?)null;
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s =>
            {
                handler = new InMemorySessionFsSqliteHandler(s.SessionId, _sqliteCalls);
                return handler;
            },
        });

        var events = new List<SessionEvent>();
        using var _ = session.On(evt => events.Add(evt));

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt =
                "Use the task tool to ask a task agent to do the following: " +
                "Use the sql tool to run this query: INSERT INTO todos (id, title, status) VALUES ('subagent-test', 'Created by subagent', 'done')",
        });

        await session.DisposeAsync();

        var sessionCalls = _sqliteCalls.Where(c => c.SessionId == session.SessionId).ToList();
        var insertCalls = sessionCalls.Where(c => c.Query.Contains("INSERT", StringComparison.OrdinalIgnoreCase)).ToList();
        Assert.NotEmpty(insertCalls);

        // Verify that the sql tool execution in events.jsonl came from the subagent (has agentId)
        Assert.NotNull(handler);
        var eventsKey = $"/{session.SessionId}/session-state/events.jsonl";
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(handler!.Files.ContainsKey(eventsKey)),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed out waiting for events.jsonl to be written.");
        Assert.True(handler!.Files.TryGetValue(eventsKey, out var content));
        var lines = content.Split('\n', StringSplitOptions.RemoveEmptyEntries);
        var sqlToolEvents = lines
            .Select(line => System.Text.Json.JsonDocument.Parse(line))
            .Where(doc =>
                doc.RootElement.TryGetProperty("type", out var type) && type.GetString() == "tool.execution_start" &&
                doc.RootElement.TryGetProperty("data", out var data) && data.TryGetProperty("toolName", out var toolName) && toolName.GetString() == "sql")
            .ToList();
        Assert.NotEmpty(sqlToolEvents);
        Assert.All(sqlToolEvents, evt =>
        {
            Assert.True(evt.RootElement.TryGetProperty("agentId", out var agentId));
            Assert.False(string.IsNullOrEmpty(agentId.GetString()));
        });
    }

    private CopilotClient CreateSessionFsClient()
    {
        return Ctx.CreateClient(
            useStdio: true,
            options: new CopilotClientOptions
            {
                SessionFs = SessionFsConfig,
            });
    }
}
