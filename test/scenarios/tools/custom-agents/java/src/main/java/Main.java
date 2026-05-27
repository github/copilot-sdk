import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CustomAgentConfig;
import com.github.copilot.rpc.DefaultAgentConfig;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var parameters = Map.<String, Object>of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "Analysis query")),
            "required", List.of("query"));

        var analyzeTool = ToolDefinition.create("analyze-codebase",
            "Performs deep analysis of the codebase", parameters,
            invocation -> {
                String query = (String) invocation.getArguments().get("query");
                return CompletableFuture.completedFuture("Analysis result for: " + query);
            });

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setTools(List.of(analyzeTool))
                    .setDefaultAgent(new DefaultAgentConfig()
                        .setExcludedTools(List.of("analyze-codebase")))
                    .setCustomAgents(List.of(
                        new CustomAgentConfig()
                            .setName("researcher")
                            .setDisplayName("Research Agent")
                            .setDescription("A research agent that can only read and search files, not modify them")
                            .setTools(List.of("grep", "glob", "view", "analyze-codebase"))
                            .setPrompt("You are a research assistant. You can search and read files "
                                + "but cannot modify anything. When asked about your capabilities, "
                                + "list the tools you have access to."))))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt(
                    "What custom agents are available? Describe the researcher agent and its capabilities."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
