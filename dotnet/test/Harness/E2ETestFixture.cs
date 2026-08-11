/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test;

public class E2ETestFixture : IAsyncLifetime
{
    internal const string SharedTcpConnectionToken = "e2e-shared-token";

    public E2ETestContext Ctx { get; private set; } = null!;
    public CopilotClient Client { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        Ctx = await E2ETestContext.CreateAsync();
        Client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = CreateSharedConnection(E2ETestContext.UsesInProcessTransport),
        }, persistent: true);
    }

    internal static RuntimeConnection CreateSharedConnection(bool useInProcessTransport) =>
        useInProcessTransport
            ? RuntimeConnection.ForInProcess()
            : RuntimeConnection.ForTcp(connectionToken: SharedTcpConnectionToken);

    public async Task DisposeAsync()
    {
        await Ctx.DisposeAsync();
    }
}
