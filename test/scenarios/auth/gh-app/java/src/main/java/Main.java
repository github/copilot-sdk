import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) throws Exception {
        var clientId = System.getenv("GITHUB_OAUTH_CLIENT_ID");
        if (clientId == null || clientId.isEmpty()) {
            System.err.println("Missing GITHUB_OAUTH_CLIENT_ID");
            System.exit(1);
        }

        var mapper = new ObjectMapper();
        var httpClient = HttpClient.newHttpClient();

        // Step 1: Request device code
        var deviceCodeReq = HttpRequest.newBuilder()
            .uri(URI.create("https://github.com/login/device/code"))
            .header("Accept", "application/json")
            .header("User-Agent", "copilot-sdk-java")
            .POST(HttpRequest.BodyPublishers.ofString("client_id=" + clientId))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();
        var deviceCodeResp = httpClient.send(deviceCodeReq, HttpResponse.BodyHandlers.ofString());
        var deviceCode = mapper.readTree(deviceCodeResp.body());

        var userCode = deviceCode.get("user_code").asText();
        var verificationUri = deviceCode.get("verification_uri").asText();
        var code = deviceCode.get("device_code").asText();
        var interval = deviceCode.get("interval").asInt();

        System.out.println("Please visit: " + verificationUri);
        System.out.println("Enter code: " + userCode);

        // Step 2: Poll for access token
        String accessToken = null;
        while (accessToken == null) {
            Thread.sleep(interval * 1000L);
            var tokenReq = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "client_id=" + clientId
                    + "&device_code=" + code
                    + "&grant_type=urn:ietf:params:oauth:grant-type:device_code"))
                .build();
            var tokenResp = httpClient.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            var tokenData = mapper.readTree(tokenResp.body());

            if (tokenData.has("access_token")) {
                accessToken = tokenData.get("access_token").asText();
            } else if (tokenData.has("error")) {
                var err = tokenData.get("error").asText();
                if ("authorization_pending".equals(err)) continue;
                if ("slow_down".equals(err)) { interval += 5; continue; }
                throw new RuntimeException("OAuth error: " + err);
            }
        }

        // Step 3: Verify authentication
        var userReq = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/user"))
            .header("Authorization", "Bearer " + accessToken)
            .header("User-Agent", "copilot-sdk-java")
            .GET()
            .build();
        var userResp = httpClient.send(userReq, HttpResponse.BodyHandlers.ofString());
        var userData = mapper.readTree(userResp.body());
        System.out.println("Authenticated as: " + userData.get("login").asText());

        // Step 4: Use the token with Copilot
        try (var client = new CopilotClient(new CopilotClientOptions()
                .setGitHubToken(accessToken))) {
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
