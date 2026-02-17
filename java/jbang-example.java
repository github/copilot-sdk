
//DEPS io.github.copilot-community-sdk:copilot-sdk:1.0.9
import com.github.copilot.sdk.*;
import com.github.copilot.sdk.events.*;
import com.github.copilot.sdk.json.*;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.out;

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
                out.println(msg.getData().content());
            });

            // Handle session usage info events
            session.on(SessionUsageInfoEvent.class, usage -> {
                var data = usage.getData();
                out.println("\n--- Usage Metrics ---");
                out.println("Current tokens: " + (int) data.currentTokens());
                out.println("Token limit: " + (int) data.tokenLimit());
                out.println("Messages count: " + (int) data.messagesLength());
            });

            // Send a message
            var completable = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?"));
            // and wait for completion
            completable.get();
        }
    }
}
