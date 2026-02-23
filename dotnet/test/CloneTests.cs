/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class CloneTests
{
    [Fact]
    public void CopilotClientOptions_Clone_CopiesAllProperties()
    {
        var original = new CopilotClientOptions
        {
            CliPath = "/usr/bin/copilot",
            CliArgs = ["--verbose", "--debug"],
            Cwd = "/home/user",
            Port = 8080,
            UseStdio = false,
            CliUrl = "http://localhost:8080",
            LogLevel = "debug",
            AutoStart = false,
            AutoRestart = false,
            Environment = new Dictionary<string, string> { ["KEY"] = "value" },
            GitHubToken = "ghp_test",
            UseLoggedInUser = false,
        };

        var clone = original.Clone();

        Assert.Equal(original.CliPath, clone.CliPath);
        Assert.Equal(original.CliArgs, clone.CliArgs);
        Assert.Equal(original.Cwd, clone.Cwd);
        Assert.Equal(original.Port, clone.Port);
        Assert.Equal(original.UseStdio, clone.UseStdio);
        Assert.Equal(original.CliUrl, clone.CliUrl);
        Assert.Equal(original.LogLevel, clone.LogLevel);
        Assert.Equal(original.AutoStart, clone.AutoStart);
        Assert.Equal(original.AutoRestart, clone.AutoRestart);
        Assert.Equal(original.Environment, clone.Environment);
        Assert.Equal(original.GitHubToken, clone.GitHubToken);
        Assert.Equal(original.UseLoggedInUser, clone.UseLoggedInUser);
    }

    [Fact]
    public void CopilotClientOptions_Clone_CollectionsAreIndependent()
    {
        var original = new CopilotClientOptions
        {
            CliArgs = ["--verbose"],
        };

        var clone = original.Clone();

        // Mutate clone array
        clone.CliArgs![0] = "--quiet";

        // Original is unaffected
        Assert.Equal("--verbose", original.CliArgs![0]);
    }

    [Fact]
    public void CopilotClientOptions_Clone_EnvironmentIsShared()
    {
        var env = new Dictionary<string, string> { ["key"] = "value" };
        var original = new CopilotClientOptions { Environment = env };

        var clone = original.Clone();

        Assert.Same(original.Environment, clone.Environment);
    }

    [Fact]
    public void SessionConfig_Clone_CopiesAllProperties()
    {
        var original = new SessionConfig
        {
            SessionId = "test-session",
            ClientName = "my-app",
            Model = "gpt-4",
            ReasoningEffort = "high",
            ConfigDir = "/config",
            AvailableTools = ["tool1", "tool2"],
            ExcludedTools = ["tool3"],
            WorkingDirectory = "/workspace",
            Streaming = true,
            McpServers = new Dictionary<string, object> { ["server1"] = new object() },
            CustomAgents = [new CustomAgentConfig { Name = "agent1" }],
            SkillDirectories = ["/skills"],
            DisabledSkills = ["skill1"],
        };

        var clone = original.Clone();

        Assert.Equal(original.SessionId, clone.SessionId);
        Assert.Equal(original.ClientName, clone.ClientName);
        Assert.Equal(original.Model, clone.Model);
        Assert.Equal(original.ReasoningEffort, clone.ReasoningEffort);
        Assert.Equal(original.ConfigDir, clone.ConfigDir);
        Assert.Equal(original.AvailableTools, clone.AvailableTools);
        Assert.Equal(original.ExcludedTools, clone.ExcludedTools);
        Assert.Equal(original.WorkingDirectory, clone.WorkingDirectory);
        Assert.Equal(original.Streaming, clone.Streaming);
        Assert.Equal(original.McpServers.Count, clone.McpServers!.Count);
        Assert.Equal(original.CustomAgents.Count, clone.CustomAgents!.Count);
        Assert.Equal(original.SkillDirectories, clone.SkillDirectories);
        Assert.Equal(original.DisabledSkills, clone.DisabledSkills);
    }

    [Fact]
    public void SessionConfig_Clone_CollectionsAreIndependent()
    {
        var original = new SessionConfig
        {
            AvailableTools = ["tool1"],
            ExcludedTools = ["tool2"],
            McpServers = new Dictionary<string, object> { ["s1"] = new object() },
            CustomAgents = [new CustomAgentConfig { Name = "a1" }],
            SkillDirectories = ["/skills"],
            DisabledSkills = ["skill1"],
        };

        var clone = original.Clone();

        // Mutate clone collections
        clone.AvailableTools!.Add("tool99");
        clone.ExcludedTools!.Add("tool99");
        clone.McpServers!["s2"] = new object();
        clone.CustomAgents!.Add(new CustomAgentConfig { Name = "a2" });
        clone.SkillDirectories!.Add("/more");
        clone.DisabledSkills!.Add("skill99");

        // Original is unaffected
        Assert.Single(original.AvailableTools!);
        Assert.Single(original.ExcludedTools!);
        Assert.Single(original.McpServers!);
        Assert.Single(original.CustomAgents!);
        Assert.Single(original.SkillDirectories!);
        Assert.Single(original.DisabledSkills!);
    }

    [Fact]
    public void SessionConfig_Clone_PreservesMcpServersComparer()
    {
        var servers = new Dictionary<string, object>(StringComparer.OrdinalIgnoreCase) { ["server"] = new object() };
        var original = new SessionConfig { McpServers = servers };

        var clone = original.Clone();

        Assert.True(clone.McpServers!.ContainsKey("SERVER")); // case-insensitive lookup works
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CollectionsAreIndependent()
    {
        var original = new ResumeSessionConfig
        {
            AvailableTools = ["tool1"],
            ExcludedTools = ["tool2"],
            McpServers = new Dictionary<string, object> { ["s1"] = new object() },
            CustomAgents = [new CustomAgentConfig { Name = "a1" }],
            SkillDirectories = ["/skills"],
            DisabledSkills = ["skill1"],
        };

        var clone = original.Clone();

        // Mutate clone collections
        clone.AvailableTools!.Add("tool99");
        clone.ExcludedTools!.Add("tool99");
        clone.McpServers!["s2"] = new object();
        clone.CustomAgents!.Add(new CustomAgentConfig { Name = "a2" });
        clone.SkillDirectories!.Add("/more");
        clone.DisabledSkills!.Add("skill99");

        // Original is unaffected
        Assert.Single(original.AvailableTools!);
        Assert.Single(original.ExcludedTools!);
        Assert.Single(original.McpServers!);
        Assert.Single(original.CustomAgents!);
        Assert.Single(original.SkillDirectories!);
        Assert.Single(original.DisabledSkills!);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesMcpServersComparer()
    {
        var servers = new Dictionary<string, object>(StringComparer.OrdinalIgnoreCase) { ["server"] = new object() };
        var original = new ResumeSessionConfig { McpServers = servers };

        var clone = original.Clone();

        Assert.True(clone.McpServers!.ContainsKey("SERVER"));
    }

    [Fact]
    public void MessageOptions_Clone_CopiesAllProperties()
    {
        var original = new MessageOptions
        {
            Prompt = "Hello",
            Attachments = [new UserMessageDataAttachmentsItemFile { Path = "/test.txt", DisplayName = "test.txt" }],
            Mode = "chat",
        };

        var clone = original.Clone();

        Assert.Equal(original.Prompt, clone.Prompt);
        Assert.Equal(original.Mode, clone.Mode);
        Assert.Single(clone.Attachments!);
    }

    [Fact]
    public void MessageOptions_Clone_AttachmentsAreIndependent()
    {
        var original = new MessageOptions
        {
            Attachments = [new UserMessageDataAttachmentsItemFile { Path = "/test.txt", DisplayName = "test.txt" }],
        };

        var clone = original.Clone();

        clone.Attachments!.Add(new UserMessageDataAttachmentsItemFile { Path = "/other.txt", DisplayName = "other.txt" });

        Assert.Single(original.Attachments!);
    }

    [Fact]
    public void Clone_WithNullCollections_ReturnsNullCollections()
    {
        var original = new SessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.AvailableTools);
        Assert.Null(clone.ExcludedTools);
        Assert.Null(clone.McpServers);
        Assert.Null(clone.CustomAgents);
        Assert.Null(clone.SkillDirectories);
        Assert.Null(clone.DisabledSkills);
        Assert.Null(clone.Tools);
    }
}
