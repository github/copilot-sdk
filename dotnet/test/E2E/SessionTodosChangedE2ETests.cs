/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class SessionTodosChangedE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session_todos_changed", output)
{
    private static readonly string[] ExpectedTodoIds = ["alpha", "beta"];

    [Fact]
    public async Task Fires_Session_Todos_Changed_And_Exposes_Rows_And_Dependencies()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var todosChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionTodosChangedEvent>(
            session,
            TimeSpan.FromSeconds(30));

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt =
                "Use the sql tool exactly once to execute all three of the following statements " +
                "together, in this exact order, in a single sql tool call (a single query string " +
                "containing all three statements):\n" +
                "1. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');\n" +
                "2. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');\n" +
                "3. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');\n" +
                "Then stop. Do not insert any other rows or create any other tables.",
        });

        await todosChangedTask;

        var result = await session.Rpc.Plan.ReadSqlTodosWithDependenciesAsync();

        var ids = result.Rows
            .Select(row => row.Id)
            .OfType<string>()
            .OrderBy(id => id, StringComparer.Ordinal)
            .ToArray();

        Assert.Equal(ExpectedTodoIds, ids);

        Assert.Contains(result.Dependencies, dependency =>
            dependency.TodoId == "beta" &&
            dependency.DependsOn == "alpha");
    }
}
