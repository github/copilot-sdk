/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtemp, readFile, rm, writeFile } from "fs/promises";
import http from "http";
import type {
  ChatCompletion,
  ChatCompletionChunk,
  ChatCompletionMessageFunctionToolCall,
} from "openai/resources/chat/completions";
import os from "os";
import path from "path";
import { afterEach, beforeEach, describe, expect, test } from "vitest";
import yaml from "yaml";
import {
  NormalizedData,
  ReplayingCapiProxy,
  ToolResultNormalizer,
  workingDirPlaceholder,
} from "./replayingCapiProxy";
import { ShellConfig } from "./util";

describe("ReplayingCapiProxy", () => {
  let tempDir: string;
  let workDir: string;

  beforeEach(async () => {
    tempDir = await mkdtemp(path.join(os.tmpdir(), "capi-proxy-test-"));
    workDir = path.join(tempDir, "work");
  });

  afterEach(async () => {
    await rm(tempDir, { recursive: true, force: true });
  });

  async function createProxy(
    httpExchanges: Array<{
      url: string;
      requestBody: string;
      responseBody: string;
    }>,
    options?: { toolResultNormalizers?: ToolResultNormalizer[] },
  ) {
    const outputPath = path.join(tempDir, "output.yaml");
    const proxy = new ReplayingCapiProxy(
      "http://localhost",
      outputPath,
      workDir,
    );

    for (const normalizer of options?.toolResultNormalizers ?? []) {
      proxy.addToolResultNormalizer(normalizer.toolName, normalizer.normalizer);
    }

    for (const exchange of httpExchanges) {
      (proxy.exchanges as Array<unknown>).push({
        request: {
          url: exchange.url,
          method: "POST",
          body: exchange.requestBody,
        },
        response: { statusCode: 200, body: exchange.responseBody },
      });
    }

    await proxy.stop();
    return outputPath;
  }

  async function readYamlOutput(outputPath: string): Promise<NormalizedData> {
    const content = await readFile(outputPath, "utf-8");
    return yaml.parse(content) as NormalizedData;
  }

  test("does not write file when no chat completion exchanges", async () => {
    const outputPath = path.join(tempDir, "output.yaml");
    const proxy = new ReplayingCapiProxy(
      "http://localhost",
      outputPath,
      workDir,
    );
    await proxy.stop();

    await expect(readFile(outputPath)).rejects.toThrow(/ENOENT/);
  });

  test("captures chat completion request and response", async () => {
    const requestBody = JSON.stringify({
      messages: [
        { role: "system", content: "You are helpful" },
        { role: "user", content: "Hello" },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Hi there!" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations).toHaveLength(1);
    expect(result.conversations[0].messages).toEqual([
      { role: "system", content: "${system}" },
      { role: "user", content: "Hello" },
      { role: "assistant", content: "Hi there!" },
    ]);
  });

  test("normalizes tool call IDs to sequential values", async () => {
    const requestBody = JSON.stringify({
      messages: [{ role: "user", content: "Do something" }],
    });
    const responseBody = JSON.stringify({
      choices: [
        {
          message: {
            role: "assistant",
            tool_calls: [
              {
                id: "toolu_abc123xyz",
                type: "function",
                function: { name: "view", arguments: "{}" },
              },
            ],
          },
        },
      ],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[1].tool_calls![0].id).toBe(
      "toolcall_0",
    );
  });

  test("normalizes shell tool names to platform-agnostic placeholders", async () => {
    const originalShellConfig =
      process.platform === "win32" ? ShellConfig.powerShell : ShellConfig.bash;
    const requestBody = JSON.stringify({
      messages: [{ role: "user", content: "Do something" }],
    });
    const responseBody = JSON.stringify({
      choices: [
        {
          message: {
            role: "assistant",
            tool_calls: [
              {
                id: "t0",
                type: "function",
                function: {
                  name: originalShellConfig.shellToolName,
                  arguments: "{}",
                },
              },
              {
                id: "t1",
                type: "function",
                function: {
                  name: originalShellConfig.readShellToolName,
                  arguments: "{}",
                },
              },
              {
                id: "t2",
                type: "function",
                function: {
                  name: originalShellConfig.writeShellToolName,
                  arguments: "{}",
                },
              },
              {
                id: "t3",
                type: "function",
                function: { name: "someOtherName", arguments: "{}" },
              },
            ],
          },
        },
      ],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(
      result.conversations[0].messages[1].tool_calls![0].function?.name,
    ).toBe("${shell}");
    expect(
      result.conversations[0].messages[1].tool_calls![1].function?.name,
    ).toBe("${read_shell}");
    expect(
      result.conversations[0].messages[1].tool_calls![2].function?.name,
    ).toBe("${write_shell}");
    expect(
      result.conversations[0].messages[1].tool_calls![3].function?.name,
    ).toBe("someOtherName");
  });

  test("normalizes workDir paths to placeholder with forward slashes", async () => {
    const requestBody = JSON.stringify({
      messages: [{ role: "user", content: "Read file" }],
    });
    const responseBody = JSON.stringify({
      choices: [
        {
          message: {
            role: "assistant",
            tool_calls: [
              {
                id: "tc1",
                type: "function",
                function: {
                  name: "view",
                  arguments: JSON.stringify({
                    path: workDir + "\\subdir\\file.txt",
                  }),
                },
              },
            ],
          },
        },
      ],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    const args =
      result.conversations[0].messages[1].tool_calls![0].function!.arguments;
    expect(args).toBe(`{"path":"${workingDirPlaceholder}/subdir/file.txt"}`);
  });

  test("removes prefix exchanges keeping only the longest conversation", async () => {
    const turn1Request = JSON.stringify({
      messages: [{ role: "user", content: "Hello" }],
    });
    const turn1Response = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Hi" } }],
    });
    const turn2Request = JSON.stringify({
      messages: [
        { role: "user", content: "Hello" },
        { role: "assistant", content: "Hi" },
        { role: "user", content: "How are you?" },
      ],
    });
    const turn2Response = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Good!" } }],
    });

    const outputPath = await createProxy([
      {
        url: "/chat/completions",
        requestBody: turn1Request,
        responseBody: turn1Response,
      },
      {
        url: "/chat/completions",
        requestBody: turn2Request,
        responseBody: turn2Response,
      },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations).toHaveLength(1);
    expect(result.conversations[0].messages).toHaveLength(4);
  });

  test("strips current_datetime from user messages", async () => {
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content:
            "<current_datetime>2025-12-09</current_datetime> What time is it?",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "It's now" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe(
      "What time is it?",
    );
  });

  test("strips agent_instructions from user messages", async () => {
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content:
            "<agent_instructions>\nYou are a helpful test agent.\n</agent_instructions>\n\n\n\nSay hello briefly.",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Hello!" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe(
      "Say hello briefly.",
    );
  });

  test("strips agent_instructions containing skill-context from user messages", async () => {
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content:
            '<agent_instructions>\n<skill-context name="test-skill">\nSkill content here\n</skill-context>\nYou are a helpful agent.\n</agent_instructions>\n\nSay hello.',
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Hi!" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe("Say hello.");
  });

  test("applies tool result normalizers to tool response content", async () => {
    const requestBody = JSON.stringify({
      messages: [
        { role: "user", content: "Help me" },
        {
          role: "assistant",
          tool_calls: [
            {
              id: "tc1",
              type: "function",
              function: { name: "tool_alpha", arguments: "{}" },
            },
            {
              id: "tc2",
              type: "function",
              function: { name: "tool_beta", arguments: "{}" },
            },
          ],
        },
        { role: "tool", tool_call_id: "tc1", content: "alpha result" },
        { role: "tool", tool_call_id: "tc2", content: "beta result" },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy(
      [{ url: "/chat/completions", requestBody, responseBody }],
      {
        toolResultNormalizers: [
          { toolName: "tool_alpha", normalizer: (r) => r.toUpperCase() },
          { toolName: "tool_beta", normalizer: (r) => `[${r}]` },
        ],
      },
    );

    const result = await readYamlOutput(outputPath);
    const toolMessages = result.conversations[0].messages.filter(
      (m) => m.role === "tool",
    );
    expect(toolMessages[0].content).toBe("ALPHA RESULT");
    expect(toolMessages[1].content).toBe("[beta result]");
  });

  test("ignores non-chat-completion endpoints", async () => {
    const outputPath = await createProxy([
      { url: "/models", requestBody: "{}", responseBody: "{}" },
      { url: "/embeddings", requestBody: "{}", responseBody: "{}" },
    ]);

    await expect(readFile(outputPath)).rejects.toThrow(/ENOENT/);
  });

  describe("cache replay", () => {
    async function makeRequest(
      proxyUrl: string,
      requestPath: string,
      options?: { method?: string; body?: object },
    ): Promise<{ status: number; body: string }> {
      return new Promise((resolve, reject) => {
        const url = new URL(proxyUrl);
        const req = http.request(
          {
            hostname: url.hostname,
            port: url.port,
            path: requestPath,
            method: options?.method ?? "POST",
            headers: { "content-type": "application/json" },
          },
          (res) => {
            const chunks: Buffer[] = [];
            res.on("data", (chunk: Buffer) => chunks.push(chunk));
            res.on("end", () => {
              resolve({
                status: res.statusCode || 500,
                body: Buffer.concat(chunks).toString("utf-8"),
              });
            });
          },
        );
        req.on("error", reject);
        if (options?.body) {
          req.write(JSON.stringify(options.body));
        }
        req.end();
      });
    }

    test("returns cached response when request matches prefix", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi there!" },
            ],
          },
        ],
      } satisfies NormalizedData);
      await writeFile(cachePath, cacheContent);

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "You are helpful" },
              { role: "user", content: "Hello" },
            ],
          },
        });

        expect(response.status).toBe(200);
        const parsed = JSON.parse(response.body) as ChatCompletion;
        expect(parsed.choices[0].message.content).toBe("Hi there!");
      } finally {
        await proxy.stop();
      }
    });

    test("returns cached response with tool calls", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "List files" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: { name: "list_files", arguments: '{"path":"."}' },
                  },
                ],
              },
            ],
          },
        ],
      } satisfies NormalizedData);
      await writeFile(cachePath, cacheContent);

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "System prompt" },
              { role: "user", content: "List files" },
            ],
          },
        });

        expect(response.status).toBe(200);
        const parsed = JSON.parse(response.body) as ChatCompletion;
        expect(parsed.choices[0].message.tool_calls).toHaveLength(1);
        const toolCall = parsed.choices[0].message
          .tool_calls![0] as ChatCompletionMessageFunctionToolCall;
        expect(toolCall.function.name).toBe("list_files");
      } finally {
        await proxy.stop();
      }
    });

    test("expands workdir placeholder in cached response", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Read file" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: {
                      name: "read_file",
                      arguments: `{"path":"${workingDirPlaceholder}/test.txt"}`,
                    },
                  },
                ],
              },
            ],
          },
        ],
      } satisfies NormalizedData);
      await writeFile(cachePath, cacheContent);

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "System" },
              { role: "user", content: "Read file" },
            ],
          },
        });

        expect(response.status).toBe(200);
        const parsed = JSON.parse(response.body) as ChatCompletion;
        const toolCall = parsed.choices[0].message
          .tool_calls![0] as ChatCompletionMessageFunctionToolCall;
        const args = JSON.parse(toolCall.function.arguments) as {
          path: string;
        };
        expect(args.path).toBe(workDir + "/test.txt");
      } finally {
        await proxy.stop();
      }
    });

    test("matches multi-turn conversation", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi!" },
              { role: "user", content: "How are you?" },
              { role: "assistant", content: "I am fine!" },
            ],
          },
        ],
      } satisfies NormalizedData);
      await writeFile(cachePath, cacheContent);

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        // First turn
        const response1 = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "Be helpful" },
              { role: "user", content: "Hello" },
            ],
          },
        });
        expect(
          (JSON.parse(response1.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("Hi!");

        // Second turn
        const response2 = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "Be helpful" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi!" },
              { role: "user", content: "How are you?" },
            ],
          },
        });
        expect(
          (JSON.parse(response2.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("I am fine!");
      } finally {
        await proxy.stop();
      }
    });

    test("returns streaming response when stream: true", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi there!" },
            ],
          },
        ],
      } satisfies NormalizedData);
      await writeFile(cachePath, cacheContent);

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "You are helpful" },
              { role: "user", content: "Hello" },
            ],
            stream: true,
          },
        });

        expect(response.status).toBe(200);
        expect(response.body).toContain("data: ");
        expect(response.body).toContain("[DONE]");

        // Parse the SSE chunk
        const dataLine = response.body
          .split("\n")
          .find((line) => line.startsWith("data: {"));
        expect(dataLine).toBeDefined();
        const chunk = JSON.parse(dataLine!.slice(6)) as ChatCompletionChunk;
        expect(chunk.object).toBe("chat.completion.chunk");
        expect(chunk.choices[0].delta.content).toBe("Hi there!");
      } finally {
        await proxy.stop();
      }
    });

    test("returns cached models for /models endpoint", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["gpt-4o", "claude-sonnet-4"],
        conversations: [],
      } satisfies NormalizedData);
      await writeFile(cachePath, cacheContent);

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/models", {
          method: "GET",
        });

        expect(response.status).toBe(200);
        const parsed = JSON.parse(response.body) as {
          data: Array<{ id: string; name: string }>;
        };
        expect(parsed.data).toHaveLength(2);
        expect(parsed.data[0].id).toBe("gpt-4o");
        expect(parsed.data[1].id).toBe("claude-sonnet-4");
      } finally {
        await proxy.stop();
      }
    });
  });
});
