import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setStreaming(true))
                .get();
            int[] chunkCount = {0};
            session.on(AssistantMessageDeltaEvent.class, evt -> chunkCount[0]++);
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What is the capital of France?"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            System.out.println("\nStreaming chunks received: " + chunkCount[0]);
            client.stop().get();
        }
    }
}
