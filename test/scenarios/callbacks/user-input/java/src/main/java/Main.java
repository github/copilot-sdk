import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.UserInputHandler;
import com.github.copilot.sdk.json.UserInputResponse;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var inputLog = new ArrayList<String>();

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setOnUserInputRequest((request, invocation) -> {
                        inputLog.add("question: " + request.getQuestion());
                        return CompletableFuture.completedFuture(
                            new UserInputResponse().setAnswer("Paris").setWasFreeform(true));
                    })
                    .setHooks(new SessionHooks()
                        .setOnPreToolUse((input, invocation) ->
                            CompletableFuture.completedFuture(PreToolUseHookOutput.allow()))))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt(
                    "I want to learn about a city. Use the ask_user tool to ask me which city "
                    + "I'm interested in. Then tell me about that city."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            System.out.println("\n--- User input log ---");
            for (var entry : inputLog) {
                System.out.println("  " + entry);
            }
            System.out.println("\nTotal user input requests: " + inputLog.size());
            client.stop().get();
        }
    }
}
