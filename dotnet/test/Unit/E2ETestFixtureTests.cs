/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class E2ETestFixtureTests
{
    [Fact]
    public void Shared_Client_Uses_InProcess_Connection_For_InProcess_Tests()
    {
        var connection = E2ETestFixture.CreateSharedConnection(useInProcessTransport: true);

        Assert.IsType<InProcessRuntimeConnection>(connection);
    }

    [Fact]
    public void Shared_Client_Preserves_Tcp_Connection_For_OutOfProcess_Tests()
    {
        var connection = Assert.IsType<TcpRuntimeConnection>(
            E2ETestFixture.CreateSharedConnection(useInProcessTransport: false));

        Assert.Equal(E2ETestFixture.SharedTcpConnectionToken, connection.ConnectionToken);
    }
}
