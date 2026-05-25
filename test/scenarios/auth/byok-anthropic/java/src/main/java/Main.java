import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import com.github.copilot.sdk.SystemMessageMode;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        var apiKey = System.getenv("ANTHROPIC_API_KEY");
        var model = System.getenv("ANTHROPIC_MODEL");
        if (model == null) model = "claude-sonnet-4-20250514";
        var baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl == null) baseUrl = "https://api.anthropic.com";

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Missing ANTHROPIC_API_KEY.");
            System.exit(1);
        }

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel(model)
                    .setProvider(new ProviderConfig()
                        .setType("anthropic")
                        .setBaseUrl(baseUrl)
                        .setApiKey(apiKey))
                    .setAvailableTools(List.of())
                    .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.REPLACE)
                        .setContent("You are a helpful assistant. Answer concisely.")))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What is the capital of France?"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
