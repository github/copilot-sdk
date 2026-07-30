/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class PermissionHandlerTests
{
    private static readonly JsonSerializerOptions SerializerOptions = new()
    {
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    [Fact]
    public void PermissionEventExposesManagedApprovalRequired()
    {
        const string json = """
            {
              "permissionRequest": {
                "kind": "read",
                "intention": "Read managed content",
                "path": "/workspace/file.txt",
                "managedApprovalRequired": true
              },
              "requestId": "permission-1"
            }
            """;

        var data = JsonSerializer.Deserialize<PermissionRequestedData>(
            json,
            SerializerOptions);

        Assert.NotNull(data);
        var request = Assert.IsType<PermissionRequestRead>(data.PermissionRequest);
        Assert.True(request.ManagedApprovalRequired);
    }

    [Fact]
    public async Task ApproveAllLeavesManagedRequestPending()
    {
        var request = new PermissionRequest
        {
            Kind = "read",
            ManagedApprovalRequired = true,
        };

        var decision = await PermissionHandler.ApproveAll(request, new PermissionInvocation());

        Assert.IsType<PermissionDecisionNoResult>(decision);
    }

    [Fact]
    public async Task ApproveAllApprovesOrdinaryRequest()
    {
        var request = new PermissionRequest { Kind = "read" };

        var decision = await PermissionHandler.ApproveAll(request, new PermissionInvocation());

        Assert.IsType<PermissionDecisionApproveOnce>(decision);
    }
}
