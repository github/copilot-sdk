import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.AzureOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import com.github.copilot.sdk.SystemMessageMode;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        var endpoint = System.getenv("AZURE_OPENAI_ENDPOINT");
        var apiKey = System.getenv("AZURE_OPENAI_API_KEY");
        var model = System.getenv("AZURE_OPENAI_MODEL");
        if (model == null) model = "claude-haiku-4.5";
        var apiVersion = System.getenv("AZURE_API_VERSION");
        if (apiVersion == null) apiVersion = "2024-10-21";

        if (endpoint == null || endpoint.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            System.err.println("Required: AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY");
            System.exit(1);
        }

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel(model)
                    .setProvider(new ProviderConfig()
                        .setType("azure")
                        .setBaseUrl(endpoint)
                        .setApiKey(apiKey)
                        .setAzure(new AzureOptions()
                            .setApiVersion(apiVersion)))
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
