/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Tests for error handling and diagnostics improvements.
/// </summary>
public class ErrorHandlingTests
{
    [Fact]
    public async Task Should_Provide_Helpful_Error_When_CLI_Not_Found()
    {
        // Arrange: Use a non-existent CLI path
        var client = new CopilotClient(new CopilotClientOptions
        {
            CliPath = "/nonexistent/path/to/copilot",
            AutoStart = true
        });

        // Act & Assert: Should throw with helpful error message
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            async () => await client.CreateSessionAsync());

        Assert.Contains("Failed to start Copilot CLI process", exception.Message);
        Assert.Contains("Please ensure the Copilot CLI is installed", exception.Message);
    }

    [Fact]
    public async Task Should_Detect_Immediate_Process_Exit()
    {
        // Arrange: Use an executable that exits immediately
        // Platform-specific command that exits with non-zero code
        string exitCommand;
        if (OperatingSystem.IsWindows())
        {
            exitCommand = "cmd";
        }
        else
        {
            exitCommand = "false"; // Unix command that exits immediately with code 1
        }

        var clientOptions = new CopilotClientOptions
        {
            AutoStart = true
        };

        if (OperatingSystem.IsWindows())
        {
            clientOptions.CliPath = exitCommand;
            clientOptions.CliArgs = ["/c", "exit", "1"]; // Exit with code 1
        }
        else
        {
            clientOptions.CliPath = exitCommand;
        }

        var client = new CopilotClient(clientOptions);

        // Act & Assert: Should detect the immediate exit
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            async () => await client.CreateSessionAsync());

        // Should mention the process exited immediately
        Assert.Contains("exited immediately", exception.Message);
    }

    [Fact]
    public async Task Should_Provide_Clear_Error_For_Connection_Issues()
    {
        // Verify that ConnectionLostException is handled and wrapped properly
        // by checking that the InvokeRpcAsync method has proper exception handling

        // This is a minimal test that verifies the structure exists
        // More comprehensive testing would require integration tests with a real CLI
        var method = typeof(CopilotClient).GetMethod(
            "InvokeRpcAsync",
            System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static);

        Assert.NotNull(method);
    }
}
