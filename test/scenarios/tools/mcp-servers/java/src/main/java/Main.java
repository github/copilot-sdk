import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.McpStdioServerConfig;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import com.github.copilot.SystemMessageMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        var mcpServers = new HashMap<String, com.github.copilot.rpc.McpServerConfig>();
        var mcpServerCmd = System.getenv("MCP_SERVER_CMD");
        if (mcpServerCmd != null && !mcpServerCmd.isEmpty()) {
            var mcpArgs = System.getenv("MCP_SERVER_ARGS");
            var serverConfig = new McpStdioServerConfig()
                .setCommand(mcpServerCmd)
                .setTools(List.of("*"));
            if (mcpArgs != null && !mcpArgs.isEmpty()) {
                serverConfig.setArgs(List.of(mcpArgs.split(" ")));
            }
            mcpServers.put("example", serverConfig);
        }

        var config = new SessionConfig()
            .setModel("claude-haiku-4.5")
            .setAvailableTools(List.of())
            .setSystemMessage(new SystemMessageConfig()
                .setMode(SystemMessageMode.REPLACE)
                .setContent("You are a helpful assistant. Answer questions concisely."));

        if (!mcpServers.isEmpty()) {
            config.setMcpServers(mcpServers);
        }

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(config).get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What is the capital of France?"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            if (!mcpServers.isEmpty()) {
                System.out.println("\nMCP servers configured: " + String.join(", ", mcpServers.keySet()));
            } else {
                System.out.println("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)");
            }
            client.stop().get();
        }
    }
}
