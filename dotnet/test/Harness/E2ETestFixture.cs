/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class E2ETestFixture : IAsyncLifetime
{
    public E2ETestContext Ctx { get; private set; } = null!;
    public CopilotClient Client { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        Ctx = await E2ETestContext.CreateAsync();
        Client = Ctx.CreateClient();
    }

    public async Task DisposeAsync()
    {
        if (Client is not null)
        {
            await Client.ForceStopAsync();
        }

        await Ctx.DisposeAsync();
    }
}
