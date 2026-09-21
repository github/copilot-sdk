/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingSkillsAndAgentsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_skills_and_agents", output)
{
    [Fact]
    public async Task Should_Reload_Atomically_Replaced_Skill_And_Replay_It_On_Resume()
    {
        const string skillName = "scenario-reloadable-skill";
        var skillsDirectory = Path.Join(Ctx.WorkDir, "scenario-skills", Guid.NewGuid().ToString("N"));
        var skillFile = WriteSkill(
            skillsDirectory,
            skillName,
            "Scenario skill version one.",
            "Use SCENARIO_SKILL_VERSION_ONE.");

        await using var session1 = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDirectory],
        });

        AssertSkill(
            await session1.Rpc.Skills.ListAsync(),
            skillName,
            "Scenario skill version one.",
            skillFile);

        var replacement = Path.Join(Path.GetDirectoryName(skillFile)!, "SKILL.replacement.md");
        File.WriteAllText(
            replacement,
            CreateSkillContent(
                skillName,
                "Scenario skill version two.",
                "Use SCENARIO_SKILL_VERSION_TWO."));
        File.Replace(replacement, skillFile, destinationBackupFileName: null);

        await session1.Rpc.Skills.ReloadAsync();
        AssertSkill(
            await session1.Rpc.Skills.ListAsync(),
            skillName,
            "Scenario skill version two.",
            skillFile);

        var sessionId = session1.SessionId;
        await SuspendAndUntrackSessionForResumeAsync(session1);

        await using var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            ContinuePendingWork = false,
            SkillDirectories = [skillsDirectory],
        });

        AssertSkill(
            await session2.Rpc.Skills.ListAsync(),
            skillName,
            "Scenario skill version two.",
            skillFile);
    }

    [Fact]
    public async Task Should_Classify_Agent_Method_Not_Found_As_Remote_Protocol_Error()
    {
        var scriptPath = Path.Join(
            Path.GetTempPath(),
            $"copilot-agent-method-not-found-{Guid.NewGuid():N}.cjs");
        File.WriteAllText(scriptPath, FakeAgentMethodNotFoundCliScript);

        try
        {
            await using var client = Ctx.CreateClient(options: new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(path: "node", args: [scriptPath]),
            });
            await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            var exception = await Assert.ThrowsAsync<IOException>(() => session.Rpc.Agent.ReloadAsync());

            Assert.Contains("Method not found: session.agent.reload", exception.Message, StringComparison.Ordinal);
            Assert.NotNull(exception.InnerException);
            Assert.Equal("RemoteRpcException", exception.InnerException!.GetType().Name);
            var errorCode = exception.InnerException.GetType().GetProperty("ErrorCode")!.GetValue(exception.InnerException);
            Assert.Equal(-32601, Assert.IsType<int>(errorCode));
            Assert.DoesNotContain("Unhandled method", exception.ToString(), StringComparison.OrdinalIgnoreCase);
        }
        finally
        {
            File.Delete(scriptPath);
        }
    }

    private static string WriteSkill(
        string skillsDirectory,
        string skillName,
        string description,
        string body)
    {
        var skillDirectory = Path.Join(skillsDirectory, skillName);
        Directory.CreateDirectory(skillDirectory);
        var skillFile = Path.Join(skillDirectory, "SKILL.md");
        File.WriteAllText(skillFile, CreateSkillContent(skillName, description, body));
        return skillFile;
    }

    private static string CreateSkillContent(string skillName, string description, string body) =>
        $"""
        ---
        name: {skillName}
        description: {description}
        ---

        # Scenario Reloadable Skill

        {body}
        """.ReplaceLineEndings("\n");

    private static void AssertSkill(
        SkillList list,
        string skillName,
        string description,
        string expectedPath)
    {
        var skill = Assert.Single(
            list.Skills,
            skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));
        Assert.True(skill.Enabled);
        Assert.Equal(description, skill.Description);
        Assert.Equal(expectedPath, skill.Path);
    }

    private const string FakeAgentMethodNotFoundCliScript = """
        let buffer = Buffer.alloc(0);

        process.stdin.on("data", chunk => {
          buffer = Buffer.concat([buffer, chunk]);
          processBuffer();
        });
        process.stdin.resume();

        function processBuffer() {
          while (true) {
            const headerEnd = buffer.indexOf("\r\n\r\n");
            if (headerEnd < 0) return;
            const header = buffer.subarray(0, headerEnd).toString("utf8");
            const match = /Content-Length:\s*(\d+)/i.exec(header);
            if (!match) throw new Error("Missing Content-Length header");
            const length = Number(match[1]);
            const bodyStart = headerEnd + 4;
            const bodyEnd = bodyStart + length;
            if (buffer.length < bodyEnd) return;
            const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
            buffer = buffer.subarray(bodyEnd);
            handleMessage(JSON.parse(body));
          }
        }

        function handleMessage(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "id")) return;
          if (message.method === "connect") {
            writeResult(message.id, { ok: true, protocolVersion: 4, version: "fake" });
            return;
          }
          if (message.method === "ping") {
            writeResult(message.id, { message: "pong", protocolVersion: 4 });
            return;
          }
          if (message.method === "session.create") {
            const params = Array.isArray(message.params) ? message.params[0] : message.params;
            writeResult(message.id, {
              sessionId: params?.sessionId ?? "fake-agent-session",
              workspacePath: null,
              capabilities: null,
              openCanvases: []
            });
            return;
          }
          if (message.method === "session.agent.reload") {
            writeError(message.id, -32601, "Method not found: session.agent.reload");
            return;
          }
          writeResult(message.id, {});
        }

        function writeResult(id, result) {
          writeMessage({ jsonrpc: "2.0", id, result });
        }

        function writeError(id, code, message) {
          writeMessage({ jsonrpc: "2.0", id, error: { code, message } });
        }

        function writeMessage(message) {
          const body = JSON.stringify(message);
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }
        """;
}
