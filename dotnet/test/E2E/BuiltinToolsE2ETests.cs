/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Smoke coverage for the Copilot CLI built-in tools (bash, view, edit, create_file,
/// grep, glob). Each test asks the model to use one tool and then verifies the model's
/// final response reflects the tool's result. Mirrors
/// <c>nodejs/test/e2e/builtin_tools.e2e.test.ts</c>.
/// </summary>
public class BuiltinToolsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "builtin_tools", output)
{
    [Fact]
    public async Task Should_Capture_Exit_Code_In_Output()
    {
        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Run 'echo hello && echo world'. Tell me the exact output.",
        });
        var content = msg?.Data.Content ?? string.Empty;
        Assert.Contains("hello", content);
        Assert.Contains("world", content);
    }

    [Fact]
    public async Task Should_Capture_Stderr_Output()
    {
        // The Copilot CLI runs commands through a shell tool that resolves to bash on
        // Linux/macOS and PowerShell on Windows. The TS prompt only works on bash, so
        // skip this test on Windows to mirror the TS `it.skipIf(process.platform === "win32")`.
        if (OperatingSystem.IsWindows())
        {
            return;
        }

        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Run 'echo error_msg >&2; echo ok' and tell me what stderr said. Reply with just the stderr content.",
        });
        Assert.Contains("error_msg", msg?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Read_File_With_Line_Range()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "lines.txt"), "line1\nline2\nline3\nline4\nline5\n");
        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read lines 2 through 4 of the file 'lines.txt' in this directory. Tell me what those lines contain.",
        });
        var content = msg?.Data.Content ?? string.Empty;
        Assert.Contains("line2", content);
        Assert.Contains("line4", content);
    }

    [Fact]
    public async Task Should_Handle_Nonexistent_File_Gracefully()
    {
        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Try to read the file 'does_not_exist.txt'. If it doesn't exist, say 'FILE_NOT_FOUND'.",
        });
        var content = (msg?.Data.Content ?? string.Empty).ToUpperInvariant();
        // Match any of the common phrasings for a missing-file response.
        Assert.True(
            content.Contains("NOT FOUND")
            || content.Contains("NOT EXIST")
            || content.Contains("NO SUCH")
            || content.Contains("FILE_NOT_FOUND")
            || content.Contains("DOES NOT EXIST")
            || content.Contains("ERROR"),
            $"Expected a 'not found'-style response, got: {msg?.Data.Content}");
    }

    [Fact]
    public async Task Should_Edit_A_File_Successfully()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "edit_me.txt"), "Hello World\nGoodbye World\n");
        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Edit the file 'edit_me.txt': replace 'Hello World' with 'Hi Universe'. Then read it back and tell me its contents.",
        });
        Assert.Contains("Hi Universe", msg?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Create_A_New_File()
    {
        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Create a file called 'new_file.txt' with the content 'Created by test'. Then read it back to confirm.",
        });
        Assert.Contains("Created by test", msg?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Search_For_Patterns_In_Files()
    {
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "data.txt"), "apple\nbanana\napricot\ncherry\n");
        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Search for lines starting with 'ap' in the file 'data.txt'. Tell me which lines matched.",
        });
        var content = msg?.Data.Content ?? string.Empty;
        Assert.Contains("apple", content);
        Assert.Contains("apricot", content);
    }

    [Fact]
    public async Task Should_Find_Files_By_Pattern()
    {
        Directory.CreateDirectory(Path.Join(Ctx.WorkDir, "src"));
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "src", "index.ts"), "export const index = 1;");
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "README.md"), "# Readme");

        var session = await CreateSessionAsync();
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Find all .ts files in this directory (recursively). List the filenames you found.",
        });
        Assert.Contains("index.ts", msg?.Data.Content ?? string.Empty);
    }
}
