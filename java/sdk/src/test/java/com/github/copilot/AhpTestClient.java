/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

final class AhpTestClient implements AutoCloseable {
    private final Process process;
    private final BufferedReader output;
    private final BufferedWriter input;

    AhpTestClient(Path sdkRoot) throws Exception {
        process = new ProcessBuilder("node", "--import", "tsx", "test/e2e/harness/ahpTestDriver.ts")
                .directory(sdkRoot.resolve("nodejs").toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        try {
            if (!read().path("ready").asBoolean()) {
                throw new IOException("Standard AHP test client did not become ready");
            }
        } catch (Exception error) {
            try {
                close();
            } catch (Exception cleanup) {
                error.addSuppressed(cleanup);
            }
            throw error;
        }
    }

    synchronized JsonNode request(Map<String, Object> command) throws Exception {
        input.write(JsonRpcClient.getObjectMapper().writeValueAsString(command));
        input.newLine();
        input.flush();
        JsonNode response = read();
        if (response.has("error")) {
            throw new IOException(response.get("error").asText());
        }
        return response.required("result");
    }

    private JsonNode read() throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String line = output.readLine();
                if (line == null) {
                    throw new IOException("Standard AHP test client closed output unexpectedly");
                }
                return JsonRpcClient.getObjectMapper().readTree(line);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }).get(60, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        try {
            input.close();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                throw new IOException("Standard AHP test client did not stop");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Standard AHP test client exited with code " + process.exitValue());
            }
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    throw new IOException("Could not reap standard AHP test client");
                }
            }
            output.close();
        }
    }
}
