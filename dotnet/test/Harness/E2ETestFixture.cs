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
            Connection = RuntimeConnection.ForTcp(connectionToken: SharedTcpConnectionToken),
        }, persistent: true);
    }

    public async Task DisposeAsync()
    {
        await Ctx.DisposeAsync();
    }
}
