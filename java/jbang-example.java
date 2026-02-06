
//DEPS io.github.copilot-community-sdk:copilot-sdk:1.0.7
import com.github.copilot.sdk.*;
import com.github.copilot.sdk.events.*;
import com.github.copilot.sdk.json.*;
import java.util.concurrent.CompletableFuture;

class CopilotSDK {
    public static void main(String[] args) throws Exception {
        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session
            var session = client.createSession(
                new SessionConfig().setModel("claude-sonnet-4.5")).get();

            // Handle assistant message events
            session.on(AssistantMessageEvent.class, msg -> {
                System.out.println(msg.getData().getContent());
            });

            // Handle session usage info events
            session.on(SessionUsageInfoEvent.class, usage -> {
                var data = usage.getData();
                System.out.println("\n--- Usage Metrics ---");
                System.out.println("Current tokens: " + (int) data.getCurrentTokens());
                System.out.println("Token limit: " + (int) data.getTokenLimit());
                System.out.println("Messages count: " + (int) data.getMessagesLength());
            });

            // Send a message
            var completable = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?"));
            // and wait for completion
            completable.get();
        }
    }
}
