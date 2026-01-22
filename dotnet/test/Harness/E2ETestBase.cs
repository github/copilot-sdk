/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Data;
using System.Reflection;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

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
    }

    private static string GetTestName(ITestOutputHelper output)
    {
        // xUnit doesn't provide a public API to get the current test name.
        var type = output.GetType();
        var testField = type.GetField("test", BindingFlags.Instance | BindingFlags.NonPublic);
        var test = (ITest?)testField?.GetValue(output);
        return test?.TestCase.TestMethod.Method.Name ?? throw new InvalidOperationException("Couldn't find test name");
    }

    public async Task InitializeAsync()
    {
        await Ctx.ConfigureForTestAsync(_snapshotCategory, _testName);
    }

    public Task DisposeAsync() => Task.CompletedTask;

    protected static string GetSystemMessage(ParsedHttpExchange exchange) =>
        exchange.Request.Messages.FirstOrDefault(m => m.Role == "system")?.Content ?? string.Empty;

    protected static List<string> GetToolNames(ParsedHttpExchange exchange) =>
        exchange.Request.Tools?.Select(t => t.Function.Name).ToList() ?? new();
}
