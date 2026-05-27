import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SessionConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        var cliPath = System.getenv("COPILOT_CLI_PATH");
        var options = new CopilotClientOptions();
        if (cliPath != null) {
            options.setCliPath(cliPath);
        }

        try (var client = new CopilotClient(options)) {
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
            }
            client.stop().get();
        }
    }
}
