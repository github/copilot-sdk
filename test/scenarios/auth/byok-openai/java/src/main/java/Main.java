import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.SessionConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        var apiKey = System.getenv("OPENAI_API_KEY");
        var model = System.getenv("OPENAI_MODEL");
        if (model == null) model = "claude-haiku-4.5";
        var baseUrl = System.getenv("OPENAI_BASE_URL");
        if (baseUrl == null) baseUrl = "https://api.openai.com/v1";

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Missing OPENAI_API_KEY.");
            System.exit(1);
        }

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel(model)
                    .setProvider(new ProviderConfig()
                        .setType("openai")
                        .setBaseUrl(baseUrl)
                        .setApiKey(apiKey)))
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
