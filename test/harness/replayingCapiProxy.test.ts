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
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import yaml from "yaml";
import {
  NormalizedData,
  type ReplayBackend,
  ParsedHttpExchange,
  ReplayingCapiProxy,
  ToolResultNormalizer,
  workingDirPlaceholder,
} from "./replayingCapiProxy";
import { ShellConfig } from "./util";

describe("ReplayingCapiProxy", () => {
  let tempDir: string;
  let workDir: string;
  let githubActions: string | undefined;

  beforeEach(async () => {
    githubActions = process.env.GITHUB_ACTIONS;
    delete process.env.GITHUB_ACTIONS;
    tempDir = await mkdtemp(path.join(os.tmpdir(), "capi-proxy-test-"));
    workDir = path.join(tempDir, "work");
  });

  afterEach(async () => {
    if (githubActions === undefined) {
      delete process.env.GITHUB_ACTIONS;
    } else {
      process.env.GITHUB_ACTIONS = githubActions;
    }
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

  test("does not impose idle expiry on pooled control connections", async () => {
    const proxy = new ReplayingCapiProxy("http://localhost");
    const address = await proxy.start();
    const agent = new http.Agent({ keepAlive: true, maxSockets: 1 });
    const getExchanges = () =>
      new Promise<{
        headers: http.IncomingHttpHeaders;
        reusedSocket: boolean;
      }>((resolve, reject) => {
        const request = http.get(
          `${address}/exchanges`,
          { agent },
          (response) => {
            response.on("error", reject);
            response.on("end", () =>
              resolve({
                headers: response.headers,
                reusedSocket: request.reusedSocket,
              }),
            );
            response.resume();
          },
        );
        request.on("error", reject);
      });

    try {
      const first = await getExchanges();
      expect(first.headers.connection).toBe("keep-alive");
      expect(first.headers["keep-alive"]).toBeUndefined();
      expect((await getExchanges()).reusedSocket).toBe(true);
    } finally {
      agent.destroy();
      await proxy.stop();
    }
  });

  test("validates registered GitHub identities before replay configuration", async () => {
    const proxy = new ReplayingCapiProxy("http://localhost");
    proxy.setCopilotUserByToken("owner-token", { login: "owner", id: 42 });
    proxy.setCopilotUserByToken("other-token", { login: "other", id: 99 });
    proxy.setCopilotUserByToken("unresolved-token", { login: "unresolved" });
    const address = await proxy.start();
    try {
      for (const [token, id, login] of [
        ["owner-token", 42, "owner"],
        ["other-token", 99, "other"],
      ] as const) {
        const response = await fetch(`${address}/user`, {
          headers: { authorization: `Bearer ${token}` },
        });
        expect(response.status).toBe(200);
        expect(await response.json()).toEqual({ id, login, type: "User" });
      }
      for (const token of ["unknown-token", "unresolved-token"]) {
        const response = await fetch(`${address}/user`, {
          headers: { authorization: `Bearer ${token}` },
        });
        expect(response.status).toBe(401);
      }
    } finally {
      await proxy.stop();
    }
  });

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

  test("uses configured model display names and resets them between tests", async () => {
    const proxy = new ReplayingCapiProxy("http://localhost");
    const address = await proxy.start();
    const config = { filePath: path.join(tempDir, "models.yaml"), workDir };
    try {
      await proxy.updateConfig({
        ...config,
        modelNames: { "claude-sonnet-5": "Claude Sonnet 5" },
      });
      const named = await fetch(`${address}/models`);
      expect(named.status).toBe(200);
      expect(await named.json()).toMatchObject({
        data: [{ id: "claude-sonnet-5", name: "Claude Sonnet 5" }],
      });

      await proxy.updateConfig(config);
      const reset = await fetch(`${address}/models`);
      expect(reset.status).toBe(200);
      expect(await reset.json()).toMatchObject({
        data: [{ id: "claude-sonnet-5", name: "claude-sonnet-5" }],
      });
    } finally {
      await proxy.stop();
    }
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

  test.each([
    [false, 37],
    [true, 37],
    [false, 0],
    [true, 0],
    [false, undefined],
    [true, undefined],
    [false, null],
    [true, null],
  ] as const)(
    "exposes provider input usage for compaction assertions (streaming: %s, input: %s)",
    async (streaming, inputTokens) => {
      const summary = "<overview>Completed summary</overview>";
      const usage = {
        prompt_tokens: inputTokens,
        completion_tokens: 5,
        total_tokens: (inputTokens ?? 0) + 5,
      };
      const response = {
        id: "compaction-response",
        object: streaming ? "chat.completion.chunk" : "chat.completion",
        created: 1,
        model: "test-model",
        choices: [
          {
            index: 0,
            ...(streaming
              ? { delta: { role: "assistant", content: summary } }
              : { message: { role: "assistant", content: summary } }),
            finish_reason: "stop",
            logprobs: null,
          },
        ],
        usage,
      };
      const proxy = new ReplayingCapiProxy(
        "http://localhost",
        path.join(tempDir, "compaction.yaml"),
        workDir,
      );
      (proxy.exchanges as Array<unknown>).push({
        request: {
          url: "/chat/completions",
          method: "POST",
          body: JSON.stringify({ messages: [], model: "test-model" }),
          headers: {
            "x-interaction-type": "conversation-compaction",
            "x-interaction-id": "compaction-interaction",
          },
        },
        response: {
          statusCode: 200,
          body: streaming
            ? `data: ${JSON.stringify(response)}\n\ndata: [DONE]\n\n`
            : JSON.stringify(response),
        },
      });
      const address = await proxy.start();
      try {
        const result = await fetch(`${address}/exchanges`);
        expect(result.ok).toBe(true);
        const exchanges = (await result.json()) as ParsedHttpExchange[];
        const compactionResponses = exchanges.filter(
          (exchange) =>
            exchange.response?.choices &&
            exchange.requestHeaders?.["x-interaction-type"] ===
              "conversation-compaction",
        );
        expect(compactionResponses).toHaveLength(1);
        expect(
          compactionResponses[0].response?.choices
            .map((choice) => choice.message.content ?? "")
            .join(""),
        ).toBe(summary);
        expect(
          compactionResponses[0].response?.usage?.prompt_tokens ?? undefined,
        ).toBe(inputTokens ?? undefined);
        expect(compactionResponses[0].compactionUsage).toEqual({
          interactionId: "compaction-interaction",
          summary,
          responseCount: 1,
          ...(inputTokens == null ? {} : { inputTokens }),
        });
      } finally {
        await proxy.stop(true);
      }
    },
  );

  test.each([
    [
      "reasoning-only",
      null,
      11,
      "<overview>Completed summary</overview>",
      37,
      true,
      48,
    ],
    [
      "split text",
      "<overview>",
      11,
      "Completed summary</overview>",
      37,
      true,
      48,
      true,
    ],
    [
      "missing first input",
      null,
      undefined,
      "<overview>Completed summary</overview>",
      37,
      true,
      undefined,
    ],
    [
      "null final input",
      null,
      11,
      "<overview>Completed summary</overview>",
      null,
      true,
      undefined,
    ],
    [
      "explicit zero",
      null,
      0,
      "<overview>Completed summary</overview>",
      0,
      true,
      0,
    ],
    [
      "new attempt",
      null,
      11,
      "<overview>Completed summary</overview>",
      37,
      false,
      37,
    ],
  ] as const)(
    "correlates compaction provider usage across %s",
    async (
      _name,
      firstContent,
      firstInput,
      finalContent,
      finalInput,
      continuation,
      expectedInput,
      multipartContinuation: boolean = false,
    ) => {
      const proxy = new ReplayingCapiProxy(
        "http://localhost",
        path.join(tempDir, "compaction.yaml"),
        workDir,
      );
      const responses = [
        { content: firstContent, inputTokens: firstInput },
        { content: finalContent, inputTokens: finalInput },
      ];
      for (const [index, { content, inputTokens }] of responses.entries()) {
        (proxy.exchanges as Array<unknown>).push({
          request: {
            url: "/chat/completions",
            method: "POST",
            body: JSON.stringify({
              messages: [
                {
                  role: "user",
                  content:
                    index === 1 && continuation
                      ? multipartContinuation
                        ? [
                            {
                              type: "text",
                              text: "<current_datetime>clock</current_datetime>\n",
                            },
                            {
                              type: "text",
                              text: "Please continue from where you left off.",
                            },
                          ]
                        : "Please continue from where you left off."
                      : "Summarize the conversation.",
                },
              ],
              model: "test-model",
            }),
            headers: {
              "x-interaction-type": "conversation-compaction",
              "x-interaction-id": "compaction-interaction",
            },
          },
          response: {
            statusCode: 200,
            body: JSON.stringify({
              choices: [
                {
                  message: {
                    role: "assistant",
                    content,
                    reasoning_content:
                      content === null ? "Reasoning before summary" : undefined,
                  },
                  finish_reason:
                    index === 0 && content !== null ? "length" : "stop",
                },
              ],
              usage: { prompt_tokens: inputTokens, completion_tokens: 5 },
            }),
          },
        });
        if (index === 0) {
          for (const [interactionType, interactionId, statusCode] of [
            ["conversation", "compaction-interaction", 200],
            ["conversation-compaction", "other-interaction", 200],
            ["conversation-compaction", "compaction-interaction", 500],
          ]) {
            (proxy.exchanges as Array<unknown>).push({
              request: {
                url: "/chat/completions",
                method: "POST",
                body: JSON.stringify({ messages: [], model: "test-model" }),
                headers: {
                  "x-interaction-type": interactionType,
                  "x-interaction-id": interactionId,
                },
              },
              response: {
                statusCode,
                body: JSON.stringify({
                  choices: [
                    {
                      message: {
                        role: "assistant",
                        content: "Unrelated response",
                      },
                    },
                  ],
                  usage: { prompt_tokens: 999 },
                }),
              },
            });
          }
        }
      }
      const address = await proxy.start();
      try {
        const result = await fetch(`${address}/exchanges`);
        expect(result.ok).toBe(true);
        const exchanges = (await result.json()) as ParsedHttpExchange[];
        const compactions = exchanges.filter(
          (exchange) =>
            exchange.compactionUsage?.summary ===
            "<overview>Completed summary</overview>",
        );
        expect(compactions).toHaveLength(1);
        expect(compactions[0].compactionUsage).toEqual({
          interactionId: "compaction-interaction",
          summary: "<overview>Completed summary</overview>",
          responseCount: continuation ? 2 : 1,
          ...(expectedInput === undefined
            ? {}
            : { inputTokens: expectedInput }),
        });
      } finally {
        await proxy.stop(true);
      }
    },
  );

  test.each(["object", "scalar", "freeform"] as const)(
    "preserves opaque tool text while normalizing and replaying paths (%s)",
    async (argumentKind) => {
      const opaqueText = String.raw`word\b before\nafter ${workingDirPlaceholder}`;
      const patchBody = [
        String.raw`+const pattern = /word\b/;`,
        String.raw`+const text = "before\nafter";`,
        String.raw`+const escapes = "\r\t\b\u0041";`,
        `+const root = "${workDir}\\literal\\b";`,
        `+const placeholder = "${workingDirPlaceholder}";`,
        String.raw`+*** Add File: body\must\stay.txt`,
      ].join("\n");
      const patch = [
        "*** Begin Patch",
        `*** Add File: ${workDir}\\src\\marker.txt`,
        patchBody,
        String.raw`*** Update File: src\before.txt`,
        String.raw`*** Move to: src\after.txt`,
        "@@",
        "-before",
        "+after",
        String.raw`*** Delete File: src\obsolete.txt`,
        "*** End Patch",
      ].join("\n");
      const expectedPatch = (root: string) =>
        [
          "*** Begin Patch",
          `*** Add File: ${root}/src/marker.txt`,
          patchBody,
          "*** Update File: src/before.txt",
          "*** Move to: src/after.txt",
          "@@",
          "-before",
          "+after",
          "*** Delete File: src/obsolete.txt",
          "*** End Patch",
        ].join("\n");
      const patchArguments = (input: string, filename: string) =>
        argumentKind === "object"
          ? JSON.stringify({ input, path: filename, content: opaqueText })
          : argumentKind === "scalar"
            ? JSON.stringify(input)
            : input;
      const textArguments = (root: string, separator: string) =>
        JSON.stringify({
          input: opaqueText,
          content: opaqueText,
          prompt: opaqueText,
          paths: [
            `src${separator}one.txt`,
            `${root}${separator}src${separator}two.txt`,
          ],
        });
      const request = {
        model: "test-model",
        messages: [{ role: "user", content: "Apply the patch" }],
      };
      const outputPath = await createProxy([
        {
          url: "/chat/completions",
          requestBody: JSON.stringify(request),
          responseBody: JSON.stringify({
            choices: [
              {
                message: {
                  role: "assistant",
                  tool_calls: [
                    {
                      id: "patch-call",
                      type: "function",
                      function: {
                        name: "apply_patch",
                        arguments: patchArguments(patch, "src\\marker.txt"),
                      },
                    },
                    {
                      id: "text-call",
                      type: "function",
                      function: {
                        name: "literal_tool",
                        arguments: textArguments(workDir, "\\"),
                      },
                    },
                    {
                      id: "scalar-call",
                      type: "function",
                      function: {
                        name: "scalar_tool",
                        arguments: JSON.stringify(opaqueText),
                      },
                    },
                  ],
                },
              },
            ],
          }),
        },
      ]);

      const result = await readYamlOutput(outputPath);
      expect(
        result.conversations[0].messages[1].tool_calls?.map(
          (call) => call.function?.arguments,
        ),
      ).toEqual([
        patchArguments(expectedPatch(workingDirPlaceholder), "src/marker.txt"),
        textArguments(workingDirPlaceholder, "/"),
        JSON.stringify(opaqueText),
      ]);

      const proxy = new ReplayingCapiProxy("http://localhost:1");
      await proxy.updateConfig({
        filePath: outputPath,
        workDir,
        backend: "capi",
        replayOnly: true,
      });
      const proxyUrl = await proxy.start();
      try {
        const response = await fetch(`${proxyUrl}/chat/completions`, {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ ...request, stream: false }),
        });
        expect(response.status).toBe(200);
        const completion = (await response.json()) as ChatCompletion;
        expect(
          completion.choices[0].message.tool_calls?.map((call) =>
            call.type === "function" ? call.function.arguments : undefined,
          ),
        ).toEqual([
          patchArguments(expectedPatch(workDir), "src/marker.txt"),
          textArguments(workDir, "/"),
          JSON.stringify(opaqueText),
        ]);
      } finally {
        await proxy.stop(true);
      }
    },
  );

  test.each(["/", "\\"])(
    "normalizes and replays delegated prompts across working directories (%s)",
    async (separator) => {
      const promptForDir = (root: string, pathSeparator: string) =>
        `Read ${root}${pathSeparator}src${pathSeparator}marker.txt`;
      const taskArguments = (prompt: string) =>
        JSON.stringify({
          agent_type: "explore",
          description: "Read marker.txt",
          prompt,
        });
      const parentRequest = {
        model: "test-model",
        messages: [{ role: "user", content: "Delegate the file read" }],
      };
      const outputPath = await createProxy([
        {
          url: "/chat/completions",
          requestBody: JSON.stringify(parentRequest),
          responseBody: JSON.stringify({
            choices: [
              {
                message: {
                  role: "assistant",
                  tool_calls: [
                    {
                      id: "task-call",
                      type: "function",
                      function: {
                        name: "task",
                        arguments: taskArguments(
                          promptForDir(workDir, separator),
                        ),
                      },
                    },
                  ],
                },
              },
            ],
          }),
        },
        {
          url: "/chat/completions",
          requestBody: JSON.stringify({
            model: "test-model",
            messages: [
              { role: "user", content: promptForDir(workDir, separator) },
            ],
          }),
          responseBody: JSON.stringify({
            choices: [
              {
                message: {
                  role: "assistant",
                  content: "Delegated file read completed.",
                },
              },
            ],
          }),
        },
      ]);
      const result = await readYamlOutput(outputPath);
      const normalizedPrompt = promptForDir(workingDirPlaceholder, "/");
      expect(result.conversations).toHaveLength(2);
      expect(
        result.conversations[0].messages[1].tool_calls?.[0].function?.arguments,
      ).toBe(taskArguments(normalizedPrompt));
      expect(result.conversations[1].messages[0].content).toBe(
        normalizedPrompt,
      );

      const replayWorkDir = path.join(tempDir, "replay-work");
      const proxy = new ReplayingCapiProxy("http://localhost:1");
      await proxy.updateConfig({
        filePath: outputPath,
        workDir: replayWorkDir,
        backend: "capi",
        replayOnly: true,
      });
      const proxyUrl = await proxy.start();
      try {
        const parentResponse = await fetch(`${proxyUrl}/chat/completions`, {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ ...parentRequest, stream: false }),
        });
        expect(parentResponse.status).toBe(200);
        const parentCompletion =
          (await parentResponse.json()) as ChatCompletion;
        const taskCall = parentCompletion.choices[0].message.tool_calls?.[0];
        expect(taskCall?.type).toBe("function");
        if (taskCall?.type !== "function") {
          throw new Error("Expected a delegated task tool call");
        }
        expect(taskCall.function.name).toBe("task");
        expect(taskCall.function.arguments).toBe(
          taskArguments(promptForDir(replayWorkDir, "/")),
        );
        const { prompt } = JSON.parse(taskCall.function.arguments) as {
          prompt: string;
        };
        const childResponse = await fetch(`${proxyUrl}/chat/completions`, {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            model: "test-model",
            messages: [{ role: "user", content: prompt }],
            stream: false,
          }),
        });
        expect(childResponse.status).toBe(200);
        const childCompletion = (await childResponse.json()) as ChatCompletion;
        expect(childCompletion.choices[0].message.content).toBe(
          "Delegated file read completed.",
        );
      } finally {
        await proxy.stop(true);
      }
    },
  );

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

  test("strips system_reminder from user messages", async () => {
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content:
            "What is 2+2?\n\n<system_reminder>\n<sql_tables>No tables currently exist.</sql_tables>\n</system_reminder>",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "4" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe("What is 2+2?");
  });

  test("strips mode_changed_notice from user messages", async () => {
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content:
            "Context before.\n\n<mode_changed_notice>\n<plan_mode>Write a plan only.</plan_mode>\n</mode_changed_notice>\n\nCreate a brief implementation plan.",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Plan ready" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe(
      "Context before.\n\nCreate a brief implementation plan.",
    );
  });

  test("drops notice-only user turns while preserving genuinely empty input", async () => {
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Ready" } }],
    });
    const noticeOutputPath = await createProxy([
      {
        url: "/chat/completions",
        requestBody: JSON.stringify({
          messages: [
            {
              role: "user",
              content:
                "<mode_changed_notice>\nPlan mode is no longer active.\n</mode_changed_notice>",
            },
          ],
        }),
        responseBody,
      },
    ]);
    const noticeResult = await readYamlOutput(noticeOutputPath);
    expect(noticeResult.conversations[0].messages).toEqual([
      { role: "assistant", content: "Ready" },
    ]);

    const emptyOutputPath = await createProxy([
      {
        url: "/chat/completions",
        requestBody: JSON.stringify({
          messages: [{ role: "user", content: "" }],
        }),
        responseBody,
      },
    ]);
    const emptyResult = await readYamlOutput(emptyOutputPath);
    expect(emptyResult.conversations[0].messages).toEqual([
      { role: "user" },
      { role: "assistant", content: "Ready" },
    ]);
  });

  test("strips plan mode prefix from user messages", async () => {
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content: "[[PLAN]] Create a brief implementation plan.",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Plan" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe(
      "Create a brief implementation plan.",
    );
  });

  test("normalizes task completion notification wording", async () => {
    const idleNotification = [
      "<system_notification>",
      'Agent "sdk-background-agent" (general-purpose) has finished processing and is now idle. Use read_agent with agent_id "sdk-background-agent" to read the results, or write_agent to send follow-up messages.',
      "</system_notification>",
    ].join("\n");
    const fullNotification = [
      "<system_notification>",
      'Agent "sdk-background-agent" (general-purpose) has completed successfully. Use read_agent with agent_id "sdk-background-agent" to retrieve the full results.',
      "</system_notification>",
    ].join("\n");

    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content: idleNotification,
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content).toBe(fullNotification);
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

  test("strips skill metadata frontmatter from skill-context user messages", async () => {
    const skillDir = path.join(workDir, ".test_skills", "test-skill");
    const requestBody = JSON.stringify({
      messages: [
        {
          role: "user",
          content: `<skill-context name="test-skill">
Base directory for this skill: ${skillDir}

---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

Always include PINEAPPLE_COCONUT_42.
</skill-context>`,
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "OK!" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    expect(result.conversations[0].messages[0].content)
      .toBe(`<skill-context name="test-skill">
Base directory for this skill: ${workingDirPlaceholder}/.test_skills/test-skill

# Test Skill Instructions

Always include PINEAPPLE_COCONUT_42.
</skill-context>`);
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

  test("removes the runtime-specific available-tools list", async () => {
    const requestBody = JSON.stringify({
      messages: [
        { role: "user", content: "Help me" },
        {
          role: "assistant",
          tool_calls: [
            {
              id: "tc1",
              type: "function",
              function: { name: "report_intent", arguments: "{}" },
            },
          ],
        },
        {
          role: "tool",
          tool_call_id: "tc1",
          content:
            "Tool 'report_intent' does not exist. Available tools that can be called are bash, read_bash, view, read_agent, list_agents, write_agent, grep, glob, task.",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    const toolMessage = result.conversations[0].messages.find(
      (m) => m.role === "tool",
    );
    expect(toolMessage?.content).toBe("Tool 'report_intent' does not exist.");
  });

  test("normalizes interrupted tool execution results", async () => {
    const requestBody = JSON.stringify({
      messages: [
        { role: "user", content: "Run a slow analysis" },
        {
          role: "assistant",
          tool_calls: [
            {
              id: "tc1",
              type: "function",
              function: {
                name: "slow_analysis",
                arguments: '{"value":"test_abort"}',
              },
            },
            {
              id: "tc2",
              type: "function",
              function: {
                name: "powershell",
                arguments: '{"command":"sleep 100"}',
              },
            },
            {
              id: "tc3",
              type: "function",
              function: {
                name: "bash",
                arguments: '{"command":"sleep 100"}',
              },
            },
          ],
        },
        {
          role: "tool",
          tool_call_id: "tc1",
          content:
            'Failed to execute `slow_analysis` tool with arguments: {"value":"test_abort"} due to error: Error: Session aborted',
        },
        {
          role: "tool",
          tool_call_id: "tc2",
          content: "<shell context is being reconfigured; retry the command>",
        },
        {
          role: "tool",
          tool_call_id: "tc3",
          content: "unknown attachedShellSession handle 9",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    const toolMessages = result.conversations[0].messages.filter(
      (m) => m.role === "tool",
    );
    expect(toolMessages.map((message) => message.content)).toEqual([
      "The execution of this tool, or a previous tool was interrupted.",
      "The execution of this tool, or a previous tool was interrupted.",
      "The execution of this tool, or a previous tool was interrupted.",
    ]);
  });

  test("normalizes background agent IDs and removes runtime advisories", async () => {
    const stableResult =
      "Agent started in background with agent_id: background-agent. You'll be notified when it completes. Tell the user you're waiting and end your response, or continue unrelated work until notified.";
    const requestBody = JSON.stringify({
      messages: [
        { role: "user", content: "Help me" },
        {
          role: "assistant",
          tool_calls: [
            {
              id: "tc1",
              type: "function",
              function: { name: "task", arguments: "{}" },
            },
          ],
        },
        {
          role: "tool",
          tool_call_id: "tc1",
          content:
            "Agent started in background with agent_id: 3e0c7565-6091-58cb-85bb-6cb14db23ef7. You'll be notified when it completes. Tell the user you're waiting and end your response, or continue unrelated work until notified. The agent supports multi-turn conversations.",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    const toolMessage = result.conversations[0].messages.find(
      (m) => m.role === "tool",
    );
    expect(toolMessage?.content).toBe(stableResult);
  });

  test.each(["", ", model: claude-sonnet-5"])(
    "normalizes read_agent result metadata%s",
    async (model) => {
      const requestBody = JSON.stringify({
        messages: [
          { role: "user", content: "Help me" },
          {
            role: "assistant",
            tool_calls: [
              {
                id: "tc1",
                type: "function",
                function: {
                  name: "read_agent",
                  arguments: '{"agent_id":"read-file","wait":true}',
                },
              },
            ],
          },
          {
            role: "tool",
            tool_call_id: "tc1",
            content: `Agent is idle (waiting for messages). agent_id: read-file, agent_type: explore, status: idle, description: Reading subagent-test.txt, elapsed: 1.25s, total_turns: 1${model}\n\n[Turn 0]\nDone.`,
          },
        ],
      });
      const responseBody = JSON.stringify({
        choices: [{ message: { role: "assistant", content: "Done" } }],
      });

      const outputPath = await createProxy([
        { url: "/chat/completions", requestBody, responseBody },
      ]);

      const result = await readYamlOutput(outputPath);
      const toolMessage = result.conversations[0].messages.find(
        (m) => m.role === "tool",
      );
      expect(toolMessage?.content).toBe(
        "Agent completed. agent_id: read-file, agent_type: explore, status: completed, description: Reading subagent-test.txt, elapsed: 0s, total_turns: 0, duration: 0s\n\nDone.",
      );
    },
  );

  test("names runtime agent IDs that no task call introduced", async () => {
    const runtimeAgentId = "3e0c7565-6091-58cb-85bb-6cb14db23ef7";
    const agentResult = (agentId: string) =>
      `Agent completed. agent_id: ${agentId}, agent_type: general-purpose, status: completed, description: Probe, elapsed: 0s, total_turns: 0, duration: 0s\n\nDone.`;
    const requestBody = JSON.stringify({
      messages: [
        { role: "user", content: "Check the agent" },
        {
          role: "assistant",
          tool_calls: [
            {
              id: "tc1",
              type: "function",
              function: {
                name: "read_agent",
                arguments: JSON.stringify({
                  agent_id: runtimeAgentId,
                  since_turn: 0,
                }),
              },
            },
          ],
        },
        {
          role: "tool",
          tool_call_id: "tc1",
          content: agentResult(runtimeAgentId),
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    const [, readCall, readResult] = result.conversations[0].messages;
    expect(JSON.parse(readCall.tool_calls![0].function!.arguments!)).toEqual({
      agent_id: "api-agent",
      since_turn: 0,
    });
    expect(readResult.content).toBe(agentResult("api-agent"));
  });

  test("normalizes GitHub CLI proxy auth failures", async () => {
    const requestBody = JSON.stringify({
      messages: [
        { role: "user", content: "Summarize this issue" },
        {
          role: "assistant",
          tool_calls: [
            {
              id: "tc1",
              type: "function",
              function: { name: "web_fetch", arguments: "{}" },
            },
          ],
        },
        {
          role: "tool",
          tool_call_id: "tc1",
          content:
            'Post "https://api.github.com/graphql": tls: failed to verify certificate: x509: certificate signed by unknown authority\n<exited with exit code 1>',
        },
        {
          role: "tool",
          tool_call_id: "tc1",
          content:
            "\u28fe\u28fdHTTP 401: Requires authentication (https://api.github.com/graphql)\nTry authenticating with:  gh auth login\n<exited with exit code 1>",
        },
      ],
    });
    const responseBody = JSON.stringify({
      choices: [{ message: { role: "assistant", content: "Done" } }],
    });

    const outputPath = await createProxy([
      { url: "/chat/completions", requestBody, responseBody },
    ]);

    const result = await readYamlOutput(outputPath);
    const toolMessages = result.conversations[0].messages.filter(
      (m) => m.role === "tool",
    );
    expect(toolMessages).toEqual([
      {
        role: "tool",
        tool_call_id: "toolcall_0",
        content: "${gh_auth_required}\n<exited with exit code 4>",
      },
      {
        role: "tool",
        tool_call_id: "toolcall_0",
        content: "${gh_auth_required}\n<exited with exit code 4>",
      },
    ]);
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

    test.each([
      ["scripts/check.cjs", "references/policy.txt"],
      ["references/policy.txt", "scripts/check.cjs"],
    ])(
      "replays OTel skill resource fixtures independently of enumeration order %s %s",
      async (first, second) => {
        const cachePath = path.join(tempDir, "skill-order.yaml");
        const context = (root: string, files: string[]) =>
          `<skill-context name="review">
Base directory for this skill: ${root}

Related files (use view tool to read):
${files.map((file) => `  - ${root}/${file}`).join("\n")}

Follow the user's explicit instructions.
Related files (use view tool to read):
  - authored-second
  - authored-first
</skill-context>`;
        await writeFile(
          cachePath,
          yaml.stringify({
            models: ["test-model"],
            conversations: [
              {
                messages: [
                  {
                    role: "user",
                    content:
                      "<mode_changed_notice>\n<plan_mode>Write a plan only.</plan_mode>\n</mode_changed_notice>\n\n" +
                      context(`${workingDirPlaceholder}/skills/review`, [
                        first,
                        second,
                      ]),
                  },
                  { role: "assistant", content: "OTEL_RESOURCE_REPLAY_DONE" },
                ],
              },
            ],
          } satisfies NormalizedData),
        );
        const proxy = new ReplayingCapiProxy("http://localhost");
        await proxy.updateConfig({
          filePath: cachePath,
          workDir,
          replayOnly: true,
        });
        const proxyUrl = await proxy.start();
        try {
          const request = {
            model: "test-model",
            messages: [
              {
                role: "user",
                content:
                  context(path.join(workDir, "skills", "review"), [
                    second,
                    first,
                  ]) +
                  "\n\n<mode_changed_notice>\nPlan mode is no longer active.\n</mode_changed_notice>",
              },
            ],
          };
          const response = await makeRequest(proxyUrl, "/chat/completions", {
            body: request,
          });
          expect(response.status).toBe(200);
          expect(JSON.parse(response.body).choices[0].message.content).toBe(
            "OTEL_RESOURCE_REPLAY_DONE",
          );

          for (const content of [
            context(path.join(workDir, "skills", "review"), [first]),
            context(path.join(workDir, "skills", "review"), [
              first,
              second,
              second,
            ]),
            request.messages[0].content.replace(
              "  - authored-second\n  - authored-first",
              "  - authored-first\n  - authored-second",
            ),
          ]) {
            const mismatch = await makeRequest(proxyUrl, "/chat/completions", {
              body: {
                model: "test-model",
                messages: [{ role: "user", content }],
              },
            });
            expect(mismatch.status).toBe(500);
          }
        } finally {
          await proxy.stop(true);
        }
      },
    );

    test("replay-only mode rejects cache misses without contacting the upstream", async () => {
      let upstreamRequests = 0;
      const upstream = http.createServer((_request, response) => {
        upstreamRequests++;
        response.writeHead(200, { "content-type": "application/json" });
        response.end(JSON.stringify({ choices: [] }));
      });
      await new Promise<void>((resolve) =>
        upstream.listen(0, "127.0.0.1", resolve),
      );
      const address = upstream.address();
      if (!address || typeof address === "string") {
        throw new Error("Upstream test server did not expose a TCP port.");
      }

      const cachePath = path.join(tempDir, "cache.yaml");
      await writeFile(
        cachePath,
        yaml.stringify({
          models: ["test-model"],
          conversations: [],
        } satisfies NormalizedData),
      );
      const proxy = new ReplayingCapiProxy(`http://127.0.0.1:${address.port}`);
      await proxy.updateConfig({
        filePath: cachePath,
        workDir,
        backend: "capi",
        replayOnly: true,
      });
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [{ role: "user", content: "cache miss" }],
          },
        });

        expect(response.status).toBe(500);
        expect(response.body).toBe("Proxy error");
        expect(upstreamRequests).toBe(0);
      } finally {
        await proxy.stop(true);
        await new Promise<void>((resolve, reject) =>
          upstream.close((error) => (error ? reject(error) : resolve())),
        );
      }
    });

    test.each<ReplayBackend>([
      "capi",
      "openai-completions",
      "openai-responses",
      "anthropic-messages",
    ])(
      "replays a notice-only stored turn while preserving empty user input through %s",
      async (backend) => {
      const cachePath = path.join(tempDir, `mode-notice-${backend}.yaml`);
      await writeFile(
        cachePath,
        yaml.stringify({
          models: ["test-model"],
          conversations: [
            {
              messages: [
                {
                  role: "user",
                  content:
                    "<mode_changed_notice>\nPlan mode is no longer active.\n</mode_changed_notice>",
                },
                { role: "assistant", content: "Notice ready" },
              ],
            },
            {
              messages: [
                { role: "user" },
                { role: "assistant", content: "Empty ready" },
              ],
            },
          ],
        } satisfies NormalizedData),
      );
      const proxy = new ReplayingCapiProxy("http://localhost:9999");
      await proxy.updateConfig({
        filePath: cachePath,
        workDir,
        backend,
        replayOnly: true,
      });
      const proxyUrl = await proxy.start();
      const request = (content: string) => {
        switch (backend) {
          case "capi":
          case "openai-completions":
            return {
              endpoint: "/chat/completions",
              body: {
                model: "test-model",
                messages: [{ role: "user", content }],
              },
            };
          case "openai-responses":
            return {
              endpoint: "/responses",
              body: {
                model: "test-model",
                input: [
                  {
                    type: "message",
                    role: "user",
                    content: [{ type: "input_text", text: content }],
                  },
                ],
              },
            };
          case "anthropic-messages":
            return {
              endpoint: "/v1/messages",
              body: {
                model: "test-model",
                max_tokens: 100,
                messages: [{ role: "user", content }],
              },
            };
        }
      };

      try {
        const noticeRequest = request(
          "<mode_changed_notice>\nPlan mode is no longer active.\n</mode_changed_notice>",
        );
        const response = await makeRequest(proxyUrl, noticeRequest.endpoint, {
          body: noticeRequest.body,
        });

        expect(response.status).toBe(200);
        expect(response.body).toContain("Notice ready");

        const emptyRequest = request("");
        const emptyResponse = await makeRequest(proxyUrl, emptyRequest.endpoint, {
          body: emptyRequest.body,
        });
        expect(emptyResponse.status).toBe(200);
        expect(emptyResponse.body).toContain("Empty ready");
      } finally {
        await proxy.stop(true);
      }
      },
    );

    test.each([
      ["should_accept_blob_attachments", "pixel.png"],
      ["vision_disabled_then_enabled_via_setmodel", "test.png"],
    ])(
      "replays only the recorded image histories for %s",
      async (snapshot, filename) => {
        process.env.GITHUB_ACTIONS = "true";
        const cachePath = path.join(
          import.meta.dirname,
          "..",
          "snapshots",
          "session_config",
          `${snapshot}.yaml`,
        );
        const stored = await readYamlOutput(cachePath);
        const messages = stored.conversations.at(-1)!.messages;
        const finalResponse = messages.at(-1)!;
        expect(finalResponse.role).toBe("assistant");
        expect(finalResponse.content).toBeTruthy();
        const imageDescription = `Image file at path ${workDir}/${filename}`;
        const limitMessage = (limit: number) =>
          `You've reached the maximum number of images you can view (${limit}) so I can't provide the image for you to see.`;
        const proxy = new ReplayingCapiProxy(
          "http://localhost:1",
          cachePath,
          workDir,
        );
        const proxyUrl = await proxy.start();

        try {
          for (const imagePart of [
            {
              type: "image_url",
              image_url: {
                url: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
              },
            },
            { type: "text", text: limitMessage(1) },
          ]) {
            const response = await makeRequest(proxyUrl, "/chat/completions", {
              body: {
                model: stored.models[0],
                messages: [
                  ...messages.slice(0, -2),
                  {
                    role: "user",
                    content: [
                      { type: "text", text: imageDescription },
                      imagePart,
                    ],
                  },
                ],
              },
            });
            expect(response.status).toBe(200);
            const completion = JSON.parse(response.body) as ChatCompletion;
            expect(completion.choices[0].message.content).toBe(
              finalResponse.content,
            );
            expect(completion.choices[0].finish_reason).toBe("stop");
          }

          const stderr = vi
            .spyOn(process.stderr, "write")
            .mockReturnValue(true);
          const consoleError = vi
            .spyOn(console, "error")
            .mockImplementation(() => {});
          try {
            for (const content of [
              imageDescription,
              `${imageDescription}\n${limitMessage(2)}`,
            ]) {
              const response = await makeRequest(
                proxyUrl,
                "/chat/completions",
                {
                  body: {
                    model: stored.models[0],
                    messages: [
                      ...messages.slice(0, -2),
                      { role: "user", content },
                    ],
                  },
                },
              );
              expect(response.status).toBe(500);
              expect(proxy.exchanges.at(-1)?.response?.body).toContain(
                "No cached response found for POST /chat/completions.",
              );
            }
          } finally {
            stderr.mockRestore();
            consoleError.mockRestore();
          }
        } finally {
          await proxy.stop(true);
        }
      },
    );

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

    test("replays a CAPI view-image capture for BYOK tool-result and image-turn continuations", async () => {
      const cachePath = path.join(tempDir, "view-image.yaml");
      await writeFile(
        cachePath,
        yaml.stringify({
          models: ["test-model"],
          conversations: [
            {
              messages: [
                { role: "system", content: "${system}" },
                { role: "user", content: "View the image" },
                {
                  role: "assistant",
                  tool_calls: [
                    {
                      id: "toolcall_0",
                      type: "function",
                      function: {
                        name: "view",
                        arguments: '{"path":"/image.png"}',
                      },
                    },
                  ],
                },
                {
                  role: "tool",
                  tool_call_id: "toolcall_0",
                  content: "Viewed image file successfully.",
                },
                { role: "user", content: "Image file at path /image.png\n[image]" },
                { role: "assistant", content: "Image viewed" },
              ],
            },
          ],
        } satisfies NormalizedData),
      );
      const proxy = new ReplayingCapiProxy("http://localhost:9999");
      await proxy.updateConfig({
        filePath: cachePath,
        workDir,
        backend: "openai-completions",
        replayOnly: true,
      });
      const proxyUrl = await proxy.start();

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "System prompt" },
              { role: "user", content: "View the image" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "runtime-call-id",
                    type: "function",
                    function: { name: "view", arguments: '{"path":"/image.png"}' },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "runtime-call-id",
                content: "Viewed image file successfully.",
              },
            ],
          },
        });

        expect(response.status).toBe(200);
        expect((JSON.parse(response.body) as ChatCompletion).choices[0].message.content).toBe(
          "Image viewed",
        );

        const visionResponse = await makeRequest(
          proxyUrl,
          "/chat/completions",
          {
            body: {
              model: "test-model",
              messages: [
                { role: "system", content: "System prompt" },
                { role: "user", content: "View the image" },
                {
                  role: "assistant",
                  tool_calls: [
                    {
                      id: "runtime-call-id",
                      type: "function",
                      function: {
                        name: "view",
                        arguments: '{"path":"/image.png"}',
                      },
                    },
                  ],
                },
                {
                  role: "tool",
                  tool_call_id: "runtime-call-id",
                  content: "Viewed image file successfully.",
                },
                {
                  role: "user",
                  content: "Image file at path /image.png\n[image]",
                },
              ],
            },
          },
        );
        expect(visionResponse.status).toBe(200);
        expect(
          (JSON.parse(visionResponse.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("Image viewed");
      } finally {
        await proxy.stop(true);
      }
    });

    test("matches shell tool results with shell ID completion markers", async () => {
      const originalShellConfig =
        process.platform === "win32"
          ? ShellConfig.powerShell
          : ShellConfig.bash;
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Run command" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: {
                      name: "${shell}",
                      arguments: '{"command":"echo ok"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "toolcall_0",
                content: "ok\n<exited with exit code 0>",
              },
              { role: "assistant", content: "Done" },
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
              { role: "user", content: "Run command" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "runtime-call-id",
                    type: "function",
                    function: {
                      name: originalShellConfig.shellToolName,
                      arguments: '{"command":"echo ok"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "runtime-call-id",
                content: "ok\n<shellId: 42 completed with exit code 0>",
              },
            ],
          },
        });

        expect(response.status).toBe(200);
        expect(
          (JSON.parse(response.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("Done");
      } finally {
        await proxy.stop();
      }
    });

    test("matches semantically equivalent interrupted tool results", async () => {
      const originalShellConfig =
        process.platform === "win32"
          ? ShellConfig.powerShell
          : ShellConfig.bash;
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Run command" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: {
                      name: "${shell}",
                      arguments: '{"command":"sleep 100"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "toolcall_0",
                content:
                  "The execution of this tool, or a previous tool was interrupted.",
              },
              { role: "assistant", content: "Ready for another request." },
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
        const messages = [
          { role: "system", content: "System prompt" },
          { role: "user", content: "Run command" },
          {
            role: "assistant",
            tool_calls: [
              {
                id: "runtime-call-id",
                type: "function",
                function: {
                  name: originalShellConfig.shellToolName,
                  arguments: '{"command":"sleep 100"}',
                },
              },
            ],
          },
        ];
        const interruptedResponse = await makeRequest(
          proxyUrl,
          "/chat/completions",
          {
            body: {
              model: "test-model",
              messages: [
                ...messages,
                {
                  role: "tool",
                  tool_call_id: "runtime-call-id",
                  content: "Session aborted",
                },
              ],
            },
          },
        );

        expect(interruptedResponse.status).toBe(200);
        expect(
          (JSON.parse(interruptedResponse.body) as ChatCompletion).choices[0]
            .message.content,
        ).toBe("Ready for another request.");

        const unknownHandleResponse = await makeRequest(
          proxyUrl,
          "/chat/completions",
          {
            body: {
              model: "test-model",
              messages: [
                ...messages,
                {
                  role: "tool",
                  tool_call_id: "runtime-call-id",
                  content: "unknown attachedShellSession handle 9",
                },
              ],
            },
          },
        );
        expect(unknownHandleResponse.status).toBe(200);

        const meaningfulErrorResponse = await makeRequest(
          proxyUrl,
          "/chat/completions",
          {
            body: {
              model: "test-model",
              messages: [
                ...messages,
                {
                  role: "tool",
                  tool_call_id: "runtime-call-id",
                  content:
                    "The command failed because the executable was missing.",
                },
              ],
            },
          },
        );
        expect(meaningfulErrorResponse.status).toBe(500);
      } finally {
        await proxy.stop();
      }
    });

    test("matches available-tools results after the built-in tool set changes", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      // Legacy snapshot recorded before write_agent was a built-in tool: the
      // enumeration frozen on disk still contains the older tool list.
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Report intent" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: { name: "report_intent", arguments: "{}" },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "toolcall_0",
                content:
                  "Tool 'report_intent' does not exist. Available tools that can be called are ${shell}, view, read_agent, list_agents, grep, glob, task.",
              },
              { role: "assistant", content: "Done" },
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
              { role: "user", content: "Report intent" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "runtime-call-id",
                    type: "function",
                    function: { name: "report_intent", arguments: "{}" },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "runtime-call-id",
                // Newer runtime added write_agent to the built-in tool set.
                content:
                  "Tool 'report_intent' does not exist. Available tools that can be called are bash, read_bash, view, read_agent, list_agents, write_agent, grep, glob, task.",
              },
            ],
          },
        });

        expect(response.status).toBe(200);
        expect(
          (JSON.parse(response.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("Done");
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

    test("matches cached task completion notification wording variants", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const unreadNotification = [
        "<system_notification>",
        'Agent "read-file" (explore) has completed successfully. Use read_agent with agent_id "read-file" to retrieve unread results.',
        "</system_notification>",
      ].join("\n");
      const idleNotification = [
        "<system_notification>",
        'Agent "read-file" (explore) has finished processing and is now idle. Use read_agent with agent_id "read-file" to read the results, or write_agent to send follow-up messages.',
        "</system_notification>",
      ].join("\n");

      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi!" },
              { role: "user", content: unreadNotification },
              { role: "assistant", content: "Read agent completed." },
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
              { role: "system", content: "Be helpful" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi!" },
              { role: "user", content: idleNotification },
            ],
          },
        });

        expect(response.status).toBe(200);
        expect(
          (JSON.parse(response.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("Read agent completed.");
      } finally {
        await proxy.stop();
      }
    });

    test("replays background agent calls with the runtime-generated ID", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const startResult =
        "Agent started in background with agent_id: read-file. You'll be notified when it completes. Tell the user you're waiting and end your response, or continue unrelated work until notified.";
      const notification =
        '<system_notification>\nAgent "read-file" (explore) has completed successfully. Use read_agent with agent_id "read-file" to retrieve the full results.\n</system_notification>';
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Read the file" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: {
                      name: "task",
                      arguments:
                        '{"agent_type":"explore","name":"read-file","prompt":"Read it","mode":"background"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "toolcall_0",
                content: startResult,
              },
              { role: "assistant", content: "Waiting." },
              { role: "user", content: notification },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_1",
                    type: "function",
                    function: {
                      name: "read_agent",
                      arguments: '{"agent_id":"read-file","wait":true}',
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
      const runtimeAgentId = "3e0c7565-6091-58cb-85bb-6cb14db23ef7";

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "Be helpful" },
              { role: "user", content: "Read the file" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "runtime-call-id",
                    type: "function",
                    function: {
                      name: "task",
                      arguments:
                        '{"agent_type":"explore","name":"read-file","prompt":"Read it","mode":"background"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "runtime-call-id",
                content: startResult.replace("read-file", runtimeAgentId),
              },
              { role: "assistant", content: "Waiting." },
              { role: "user", content: notification },
            ],
          },
        });

        expect(response.status).toBe(200);
        const parsed = JSON.parse(response.body) as ChatCompletion;
        const toolCall = parsed.choices[0].message
          .tool_calls![0] as ChatCompletionMessageFunctionToolCall;
        expect(JSON.parse(toolCall.function.arguments)).toEqual({
          agent_id: runtimeAgentId,
          wait: true,
        });
      } finally {
        await proxy.stop();
      }
    });

    test("replays reads of an agent no task call started with its runtime ID", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const agentResult = (agentId: string) =>
        `Agent completed. agent_id: ${agentId}, agent_type: general-purpose, status: completed, description: Probe, elapsed: 0s, total_turns: 0, duration: 0s\n\nDone.`;
      const readCall = (id: string, agentId: string) => ({
        role: "assistant" as const,
        tool_calls: [
          {
            id,
            type: "function" as const,
            function: {
              name: "read_agent",
              arguments: JSON.stringify({ agent_id: agentId, since_turn: 0 }),
            },
          },
        ],
      });
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Check the agent" },
              readCall("toolcall_0", "api-agent"),
              {
                role: "tool",
                tool_call_id: "toolcall_0",
                content: agentResult("api-agent"),
              },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_1",
                    type: "function",
                    function: {
                      name: "write_agent",
                      arguments: '{"agent_id":"api-agent","message":"Continue"}',
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
      // A different ID than any recording saw, as each run generates its own.
      const runtimeAgentId = "8d0a3f62-1b4e-4c9a-9f57-2e6b0c1d7a45";

      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "test-model",
            messages: [
              { role: "system", content: "Be helpful" },
              { role: "user", content: "Check the agent" },
              readCall("runtime-call-id", runtimeAgentId),
              {
                role: "tool",
                tool_call_id: "runtime-call-id",
                content: agentResult(runtimeAgentId),
              },
            ],
          },
        });

        expect(response.status).toBe(200);
        const parsed = JSON.parse(response.body) as ChatCompletion;
        const toolCall = parsed.choices[0].message
          .tool_calls![0] as ChatCompletionMessageFunctionToolCall;
        expect(JSON.parse(toolCall.function.arguments)).toEqual({
          agent_id: runtimeAgentId,
          message: "Continue",
        });
      } finally {
        await proxy.stop();
      }
    });

    test("matches parallel tool results regardless of arrival order", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const cacheContent = yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Lookup city and country" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "toolcall_0",
                    type: "function",
                    function: {
                      name: "lookup_city",
                      arguments: '{"city":"Paris"}',
                    },
                  },
                  {
                    id: "toolcall_1",
                    type: "function",
                    function: {
                      name: "lookup_country",
                      arguments: '{"country":"France"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "toolcall_1",
                content: "COUNTRY_FRANCE",
              },
              {
                role: "tool",
                tool_call_id: "toolcall_0",
                content: "CITY_PARIS",
              },
              { role: "assistant", content: "Paris is in France." },
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
              { role: "system", content: "Be helpful" },
              { role: "user", content: "Lookup city and country" },
              {
                role: "assistant",
                tool_calls: [
                  {
                    id: "city-id",
                    type: "function",
                    function: {
                      name: "lookup_city",
                      arguments: '{"city":"Paris"}',
                    },
                  },
                  {
                    id: "country-id",
                    type: "function",
                    function: {
                      name: "lookup_country",
                      arguments: '{"country":"France"}',
                    },
                  },
                ],
              },
              {
                role: "tool",
                tool_call_id: "country-id",
                content: "COUNTRY_FRANCE",
              },
              { role: "tool", tool_call_id: "city-id", content: "CITY_PARIS" },
            ],
          },
        });

        expect(response.status).toBe(200);
        expect(
          (JSON.parse(response.body) as ChatCompletion).choices[0].message
            .content,
        ).toBe("Paris is in France.");
      } finally {
        await proxy.stop();
      }
    });

    test("streaming preserves tool choices from recorded multi-client traffic", async () => {
      const proxy = new ReplayingCapiProxy("http://localhost:9999");
      await proxy.updateConfig({
        filePath: path.resolve(
          __dirname,
          "../snapshots/multi_client/both_clients_see_tool_request_and_completion_events.yaml",
        ),
        workDir,
        backend: "capi",
        replayOnly: true,
      });
      const proxyUrl = await proxy.start();
      try {
        const response = await makeRequest(proxyUrl, "/chat/completions", {
          body: {
            model: "claude-sonnet-5",
            messages: [
              { role: "system", content: "Application prompt" },
              {
                role: "user",
                content:
                  "Use the magic_number tool with seed 'hello' and tell me the result",
              },
            ],
            stream: true,
          },
        });
        expect(response.status).toBe(200);
        const chunks = response.body
          .split("\n")
          .filter((line) => line.startsWith("data: {"))
          .map((line) => JSON.parse(line.slice(6)) as ChatCompletionChunk);
        const calls = chunks.flatMap(
          (chunk) => chunk.choices[0].delta.tool_calls ?? [],
        );
        expect(calls.map((call) => call.function?.name)).toEqual([
          "report_intent",
          "magic_number",
        ]);
        expect(calls.map((call) => call.index)).toEqual([0, 1]);
        expect(JSON.parse(calls[1].function!.arguments!)).toEqual({
          seed: "hello",
        });
        expect(chunks.at(-1)?.choices[0].finish_reason).toBe("tool_calls");
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

    test.each([false, true])(
      "defaults to Sonnet 5 without stored models (capture exists: %s)",
      async (captureExists) => {
        const cachePath = path.join(tempDir, "cache.yaml");
        if (captureExists) {
          await writeFile(
            cachePath,
            yaml.stringify({
              models: [],
              conversations: [],
            } satisfies NormalizedData),
          );
        }

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
            data: Array<{ id: string }>;
          };
          expect(parsed.data.map((model) => model.id)).toEqual([
            "claude-sonnet-5",
          ]);
        } finally {
          await proxy.stop();
        }
      },
    );

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

    test("returns cached Auto responses in order", async () => {
      const cachePath = path.join(tempDir, "cache.yaml");
      const autoResponses = [
        {
          body: {
            session_token: "first-token",
            selected_model: { id: "test-model" },
          },
        },
        {
          statusCode: 500,
          body: {
            session_token: "unused-token",
            selected_model: { id: "unused-model" },
          },
        },
      ];
      await writeFile(
        cachePath,
        yaml.stringify({
          models: ["test-model"],
          autoResponses,
          conversations: [],
        } satisfies NormalizedData),
      );

      const proxy = new ReplayingCapiProxy(
        "http://localhost:9999",
        cachePath,
        workDir,
      );
      const proxyUrl = await proxy.start();

      try {
        const success = await makeRequest(proxyUrl, "/auto", {
          body: { prompt: "first" },
        });
        expect(success.status).toBe(200);
        expect(JSON.parse(success.body)).toEqual(autoResponses[0].body);

        const failure = await makeRequest(proxyUrl, "/auto", {
          body: { prompt: "second" },
        });
        expect(failure.status).toBe(500);
        expect(JSON.parse(failure.body)).toEqual(autoResponses[1].body);
      } finally {
        await proxy.stop();
      }
    });
  });
});
