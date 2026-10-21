/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class CapiProxyTest {

    @Test
    void replayMissFailsShutdownAfterProcessCleanup() throws Exception {
        var directory = Files.createTempDirectory("sdk-replay-failure-");
        var proxy = new CapiProxy();
        try {
            var url = proxy.start();
            var client = HttpClient.newHttpClient();
            var configuration = new ObjectMapper().writeValueAsString(Map.of("filePath",
                    directory.resolve("missing.yaml").toString(), "workDir", directory.toString(), "replayOnly", true));
            var configured = client.send(
                    HttpRequest.newBuilder(URI.create(url + "/config")).header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(configuration)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, configured.statusCode());
            var response = client.send(HttpRequest.newBuilder(URI.create(url + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"model\":\"gpt-4.1\",\"messages\":[{\"role\":\"user\",\"content\":\"UNMATCHED_INFERENCE\"}]}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
            var error = assertThrows(IOException.class, () -> proxy.stop(true));
            assertTrue(error.getMessage().contains("No cached response found"), error.getMessage());
            assertThrows(IOException.class, () -> client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()));
        } finally {
            try {
                proxy.stop(true);
            } finally {
                Files.deleteIfExists(directory);
            }
        }
    }

    @Test
    void proxyInstancesShareHttpClientResources() throws Exception {
        var field = CapiProxy.class.getDeclaredField("HTTP_CLIENT");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "E2E contexts must not retain one HttpClient selector manager per test class");
    }
}
