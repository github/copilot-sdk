import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SessionConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5"))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt(
                    "Use the grep tool to search for the word 'SDK' in README.md and show the matching lines."))
                .get();
            if (response != null) {
                System.out.println("Response: " + response.getData().content());
            }
            System.out.println("Default mode test complete");
            client.stop().get();
        }
    }
}
