import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var parameters = Map.<String, Object>of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "Search query")),
            "required", List.of("query"));

        var customGrep = ToolDefinition.createOverride("grep",
            "A custom grep implementation that overrides the built-in", parameters,
            invocation -> {
                String query = (String) invocation.getArguments().get("query");
                return CompletableFuture.completedFuture("CUSTOM_GREP_RESULT: " + query);
            });

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setTools(List.of(customGrep)))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("Use grep to search for the word 'hello'"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
