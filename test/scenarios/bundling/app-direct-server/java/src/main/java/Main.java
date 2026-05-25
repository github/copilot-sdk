import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        var cliUrl = System.getenv("COPILOT_CLI_URL");
        if (cliUrl == null) {
            cliUrl = "localhost:3000";
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliUrl(cliUrl))) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5"))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What is the capital of France?"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            } else {
                System.err.println("No response content received");
                System.exit(1);
            }
            client.stop().get();
        }
    }
}
